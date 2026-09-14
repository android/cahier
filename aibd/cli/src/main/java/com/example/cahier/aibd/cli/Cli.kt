/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cahier.aibd.cli

import com.example.cahier.aibd.core.Harness
import com.example.cahier.aibd.core.ImagePayloadConverter
import com.example.cahier.tools.Adb
import com.example.cahier.tools.DesktopDeployer
import com.example.cahier.tools.RealAdb
import com.google.genai.errors.GenAiIOException
import com.google.genai.types.Part
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.LineReaderImpl
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.Signal
import org.jline.terminal.TerminalBuilder
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters

class ByteArrayImageConverter(private val defaultMimeType: String = "image/jpeg") :
  ImagePayloadConverter<ByteArray> {
  override fun convert(payload: ByteArray): Part {
    return Part.fromBytes(payload, defaultMimeType)
  }
}

class LoadingSpinner(
  private val dispatcher: CoroutineDispatcher,
  private val terminal: Terminal,
  private val waitingMsg: String = "Waiting for Gemini...",
  private val stoppedMsg: String = "Gemini responded in %.1fs! Deploying to device...\n",
) {
  private var job: Job? = null
  private val startTime = AtomicLong(0)

  // We access `terminal` across a thread boundary, but its Java type (`Terminal`) lacks thread-safe
  // annotations. In the event that `terminal` is mutated elsewhere (e.g., by the user dragging the
  // window to resize their terminal), the worst that can happen is that the spinner line is not
  // displayed correctly.
  //
  // In the event of the size growing, this manifests as a flicker of the old, needlessly
  // truncated line before the new line is printed. In the event of the size shrinking, this can
  // cause the line to wrap, which causes the spinner to only erase part of the printed line (the
  // part which wrapped), leaving the rest of the previous spinner line visible.

  fun start() {
    startTime.set(System.currentTimeMillis())
    val scope = CoroutineScope(dispatcher)
    job = scope.launch {
      val frames = listOf("[|]", "[/]", "[-]", "[\\]")
      var idx = 0
      while (isActive) {
        val elapsed = (System.currentTimeMillis() - startTime.get()) / 1000.0
        val frame = frames[idx++ % frames.size]
        val fullMsg =
          "$frame $waitingMsg (${String.format(Locale.US, "%.1fs", elapsed)}) " +
            "(Press Ctrl+C or ESC to cancel)"

        // Query available columns (default to 80 if unknown).
        // Subtract 1 to prevent auto-wrap on the rightmost column in some terminals.
        val cols = terminal.size.columns
        val maxCols = if (cols > 0) cols - 1 else 79
        val displayMsg =
          if (fullMsg.length > maxCols) {
            fullMsg.take(maxCols - 3) + "..."
          } else {
            fullMsg
          }

        // \r moves to start of line; \u001B[K erases anything remaining to the right.
        print("\r$displayMsg\u001B[K")
        delay(100)
      }
    }
  }

  fun stop(success: Boolean = true) {
    job?.cancel()
    val elapsed = (System.currentTimeMillis() - startTime.get()) / 1000.0
    // \r moves to column 0 and \u001B[K erases the entire spinner line.
    print("\r\u001B[K")
    if (success) {
      print(String.format(Locale.US, stoppedMsg, elapsed))
    }
  }
}

fun <R> runWithSpinnerAndCancel(
  harness: Harness<ByteArray>,
  terminal: Terminal,
  spinner: LoadingSpinner,
  dispatcher: CoroutineDispatcher,
  block: () -> R,
): R? {
  spinner.start()

  val scope = CoroutineScope(dispatcher)
  val deferred = scope.async { runInterruptible { block() } }

  val oldAttributes = terminal.enterRawMode()
  val oldIntHandler = terminal.handle(Signal.INT) { _ -> deferred.cancel() }
  try {
    val reader = terminal.reader()
    while (deferred.isActive) {
      if (reader.peek(50) >= 0) {
        val char = reader.read()
        // \u001B is ESC, \u0003 is CTRL+C
        if (char == '\u001B'.code || char == '\u0003'.code) {
          deferred.cancel()
          spinner.stop(false)
          harness.revertChatState()
          println("Active Gemini request cancelled. Chat history reverted.")
          return null
        }
      }
    }
    val result = runBlocking { deferred.await() }
    spinner.stop(true)
    return result
  } catch (e: GenAiIOException) {
    spinner.stop(false)
    harness.revertChatState()
    println("Active Gemini request cancelled. Chat history reverted.")
    harness.log(
      "Gemini request failed (this may have been due to cancellation with CTRL+C): ${e.message}"
    )
    return null
  } catch (e: CancellationException) {
    spinner.stop(false)
    return null
  } catch (e: Exception) {
    spinner.stop(false)
    throw e
  } finally {
    terminal.attributes = oldAttributes
    terminal.handle(Signal.INT, oldIntHandler)
  }
}

fun deployWithRetriesWithSpinner(
  harness: Harness<ByteArray>,
  terminal: Terminal,
  dispatcher: CoroutineDispatcher,
) {
  harness.deployWithRetries { msg ->
    var resp: String? = null
    val spinner =
      LoadingSpinner(
        dispatcher,
        terminal,
        waitingMsg = "Auto-retry with Gemini...",
        stoppedMsg = "Gemini responded! Retrying deploy...\n",
      )
    runWithSpinnerAndCancel(harness, terminal, spinner, dispatcher) { resp = harness.sendText(msg) }
    return@deployWithRetries resp
  }
}

@Command(name = ">", description = ["AI Brush Designer CLI"]) class CliRootCommand

@Command(name = "/exit", aliases = ["/quit"], description = ["Exit the CLI"])
class ExitCommand : Runnable {
  override fun run() {
    println("Goodbye!")
    System.exit(0)
  }
}

@Command(
  name = "/new",
  description =
    [
      "Start a new conversation. Optionally provide a title for the conversation; if omitted, " +
        "one will be generated based on the first prompt. You can always return to the current " +
        "conversation later using /resume."
    ],
)
class NewCommand(private val harness: Harness<ByteArray>) : Runnable {
  @Parameters(
    index = "0..*",
    paramLabel = "[title]",
    description = ["Optional title for the new conversation."],
    defaultValue = "",
  )
  lateinit var titleWords: List<String>

  override fun run() {
    println("Starting a new conversation...")
    harness.createNewConversation(titleWords.joinToString(" "))
  }
}

@Command(
  name = "/fork",
  description =
    [
      "Fork the current conversation, creating a new conversation which is a copy of the " +
        "current conversation. Optionally provide a title for the new conversation."
    ],
)
class ForkCommand(private val harness: Harness<ByteArray>) : Runnable {
  @Parameters(
    index = "0..*",
    paramLabel = "[title]",
    description = ["Optional title for the new conversation."],
    defaultValue = "",
  )
  lateinit var titleWords: List<String>

  override fun run() {
    println("Forking conversation...")
    harness.forkConversation(titleWords.joinToString(" "))
  }
}

@Command(
  name = "/deploy",
  description = ["Deploy any valid brush generated from the current conversation to your device"],
)
class DeployCommand(
  private val harness: Harness<ByteArray>,
  private val lineReader: LineReader,
  private val terminal: Terminal,
  private val dispatcher: CoroutineDispatcher,
) : Runnable {
  override fun run() {
    val stagedBrushExists =
      File(harness.storage.getBrushFamilyPath(harness.activeConversation.uuid, null)).exists()
    val brushes = harness.listPersistedBrushes()

    if (brushes.isEmpty() && !stagedBrushExists) {
      println("No staged brush found to deploy. Prompt Gemini first!")
    } else {
      println("Available Brushes for Deployment:")

      for (b in brushes) {
        println("  [${b.index}] ${b.title}")
      }

      if (stagedBrushExists) {
        println("  [0] Current staged brush (new.textproto)")
      }

      val selection =
        lineReader.readLine("Enter brush number to deploy (or c to cancel): > ").trim().lowercase()
      if (selection == "c") {
        println("Deployment cancelled.")
        return
      }
      selection.toIntOrNull()?.let { index ->
        if (index in 0..brushes.size) {
          println("Deploying brush...")
          if (index == 0) { // Staged brush
            deployWithRetriesWithSpinner(harness, terminal, dispatcher)
          } else {
            try {
              harness.deploy(index)
            } catch (e: Exception) {
              harness.logAndPrint("Deploy failed: ${e.message}")
            }
          }
        } else {
          println("Invalid selection. Deployment cancelled.")
        }
      } ?: println("Invalid selection. Deployment cancelled.")
    }
  }
}

@Command(name = "/resume", description = ["Resume a previous conversation."])
class ResumeCommand(private val harness: Harness<ByteArray>, private val lineReader: LineReader) :
  Runnable {
  override fun run() {
    val sessions = harness.listAvailableConversations()
    if (sessions.isEmpty()) {
      println("No prior conversations found.")
    } else {
      println("Available Conversations:")
      sessions.forEachIndexed { index, (uuid, manifest) ->
        val timestamp =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(manifest.timestamp)
        println(
          "  [${index + 1}] ${manifest.title} (${uuid.take(6)}) " +
            "($timestamp) - ${harness.storage.getBrushFamilyCount(uuid)} valid brushes"
        )
      }
      val selection =
        lineReader
          .readLine("Enter conversation number to resume (1-${sessions.size}, or c to cancel): > ")
          .trim()
          .lowercase()
      if (selection == "c") {
        println("Resumption cancelled.")
      } else {
        selection
          .toIntOrNull()
          ?.takeIf { it in 1..sessions.size }
          ?.let {
            harness.loadConversation(sessions[it - 1].first)?.let { conversation ->
              println(
                "Successfully resumed conversation: " +
                  "\"${conversation.manifest.withNonEmptyTitle().title}\""
              )
            } ?: println("ERROR: Failed to load selected conversation.")
          } ?: println("Invalid selection. Resumption cancelled.")
      }
    }
  }
}

@Command(
  name = "/image",
  description =
    [
      "Send an image from the local filesystem to Gemini for context. The optional prompt will " +
        "be sent along with the image. If omitted, a default prompt will be used indicating " +
        "that the image should be used as a reference for brush design."
    ],
)
class ImageCommand(
  private val harness: Harness<ByteArray>,
  private val terminal: Terminal,
  private val dispatcher: CoroutineDispatcher,
) : Runnable {
  @Parameters(
    index = "0",
    paramLabel = "<path>",
    description = ["Local filesystem path to the image."],
  )
  lateinit var path: String

  @Parameters(
    index = "1..*",
    paramLabel = "[prompt]",
    description = ["Optional prompt string."],
    defaultValue = "The image provided is a reference for the kind of brush I want to create.",
  )
  lateinit var promptWords: List<String>

  override fun run() {
    val file = File(path)
    if (!file.exists()) {
      println("File not found: $path")
    } else {
      try {
        runWithSpinnerAndCancel(
            harness,
            terminal,
            LoadingSpinner(dispatcher, terminal, waitingMsg = "Sending image to Gemini..."),
            dispatcher,
          ) {
            harness.sendImage(promptWords.joinToString(" "), file.readBytes())
          }
          ?.let { response ->
            deployWithRetriesWithSpinner(harness, terminal, dispatcher)

            harness.log("Gemini: $response")
          } ?: harness.logAndPrint("Gemini did not respond.")
      } catch (e: Exception) {
        harness.logAndPrint("ERROR: ${e.message}")
      }
    }
  }
}

@Command(
  name = "/screenshot",
  description =
    [
      "Capture the screen and send it to Gemini for context. The optional prompt will be sent " +
        "along with the screenshot. If omitted, a default prompt will be used indicating that " +
        "the screenshot should be used as a reference for brush design."
    ],
)
class ScreenshotCommand(
  private val harness: Harness<ByteArray>,
  private val terminal: Terminal,
  private val dispatcher: CoroutineDispatcher,
  private val adb: Adb,
) : Runnable {
  @Parameters(
    index = "0..*",
    paramLabel = "[prompt]",
    description = ["Optional prompt string."],
    defaultValue =
      "The image provided is a screenshot from my test device. Please use it as a reference for " +
        "the kind of brush I want to create.",
  )
  lateinit var promptWords: List<String>

  override fun run() {
    val uuid = harness.activeConversation.uuid

    val screenshotFile: File =
      try {
        harness.storage.createScreenshotFile(uuid)
      } catch (e: Exception) {
        harness.logAndPrint("ERROR: Failed to create screenshot file: ${e.message}")
        null
      } ?: return

    adb
      .run(listOf("exec-out", "screencap", "-p"), screenshotFile)
      .takeIf { it.exitCode != 0 }
      ?.let {
        harness.logAndPrint(
          "ADB screenshot capture failed (exit code ${it.exitCode}): ${it.stderr.trim()}"
        )
        return
      }

    if (!screenshotFile.exists() || screenshotFile.length() == 0L) {
      if (screenshotFile.exists()) screenshotFile.delete()
      harness.logAndPrint("ADB screenshot capture failed: received empty file.")
      return
    }

    try {
      runWithSpinnerAndCancel(
          harness,
          terminal,
          LoadingSpinner(dispatcher, terminal, waitingMsg = "Sending screenshot to Gemini..."),
          dispatcher,
        ) {
          harness.sendImage(promptWords.joinToString(" "), screenshotFile.readBytes())
        }
        ?.let { response ->
          deployWithRetriesWithSpinner(harness, terminal, dispatcher)

          harness.log("Gemini: $response")
        } ?: harness.logAndPrint("Gemini did not respond.")
    } catch (e: Exception) {
      harness.logAndPrint("ERROR: ${e.message}")
    }
  }
}

fun main() {
  val banner =
    """
 █████╗ ██╗    ██████╗ ██████╗ ██╗   ██╗███████╗██╗  ██╗
██╔══██╗██║    ██╔══██╗██╔══██╗██║   ██║██╔════╝██║  ██║
███████║██║    ██████╔╝██████╔╝██║   ██║███████╗███████║
██╔══██║██║    ██╔══██╗██╔══██╗██║   ██║╚════██║██╔══██║
██║  ██║██║    ██████╔╝██║  ██║╚██████╔╝███████║██║  ██║
╚═╝  ╚═╝╚═╝    ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝

██████╗ ███████╗███████╗██╗ ██████╗ ███╗   ██╗███████╗██████╗ 
██╔══██╗██╔════╝██╔════╝██║██╔════╝ ████╗  ██║██╔════╝██╔══██╗
██║  ██║█████╗  ███████╗██║██║  ███╗██╔██╗ ██║█████╗  ██████╔╝
██║  ██║██╔══╝  ╚════██║██║██║   ██║██║╚██╗██║██╔══╝  ██╔══██╗
██████╔╝███████╗███████║██║╚██████╔╝██║ ╚████║███████╗██║  ██║
╚═════╝ ╚══════╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝
"""

  println(banner)
  println("Type your prompt and press Enter.")
  println("Type /help for a list of commands.")
  println("=========================================")

  val threadPool = Executors.newFixedThreadPool(4)
  val dispatcher = threadPool.asCoroutineDispatcher()

  try {
    val terminal = TerminalBuilder.terminal()
    val lineReader = LineReaderImpl(terminal)
    val adb = RealAdb()
    val harness =
      Harness(imageConverter = ByteArrayImageConverter(), deployer = DesktopDeployer(adb = adb))

    val manifest = harness.activeConversation.manifest
    val uuid = harness.activeConversation.uuid
    if (harness.storage.loadHistory(uuid)?.isNotEmpty() ?: false) {
      val title = manifest.withNonEmptyTitle().title
      println("Resumed Active Conversation: \"$title\"")
      println("(ID: ${uuid} | ${harness.storage.getBrushFamilyCount(uuid)} valid brushes deployed)")
      println("=========================================")
    }

    val commandLine =
      CommandLine(CliRootCommand())
        .addSubcommand("/help", CommandLine.HelpCommand())
        .addSubcommand("/new", NewCommand(harness))
        .addSubcommand("/fork", ForkCommand(harness))
        .addSubcommand("/deploy", DeployCommand(harness, lineReader, terminal, dispatcher))
        .addSubcommand("/resume", ResumeCommand(harness, lineReader))
        .addSubcommand("/image", ImageCommand(harness, terminal, dispatcher))
        .addSubcommand("/screenshot", ScreenshotCommand(harness, terminal, dispatcher, adb))
        .addSubcommand("/exit", ExitCommand())

    commandLine.setParameterExceptionHandler { _, args ->
      println("Unknown command: ${args.firstOrNull()}. Type /help for options.")
      2
    }

    while (true) {
      val line =
        try {
          lineReader.readLine("> ").trim()
        } catch (e: UserInterruptException) {
          println("Goodbye!")
          break
        } catch (e: EndOfFileException) {
          println("Goodbye!")
          break
        }

      if (line.isEmpty()) continue

      // Preprocess the line to convert to a command if one can be inferred. While a single word can
      // be a prompt, certain words are reserved and will be interpreted as commands.
      val words = lineReader.parsedLine.words().toMutableList()
      if (words.size == 1 && (words[0] == "exit" || words[0] == "quit")) {
        words[0] = "/exit"
      }
      // In the case of the help command, the user need not prefix the command they are requesting
      // help with a "/", e.g. "help deploy" is equivalent to "/help /deploy", and "help" is
      // equivalent to "/help".
      if (words.size <= 2 && (words[0] == "/help" || words[0] == "help")) {
        words[0] = "/help"
        if (words.size == 2 && !words[1].startsWith("/")) {
          words[1] = "/" + words[1]
        }
      }

      if (words.firstOrNull()?.startsWith("/") ?: false) {
        commandLine.execute(*words.toTypedArray())
      } else {
        val spinner = LoadingSpinner(dispatcher, terminal)
        runWithSpinnerAndCancel(harness, terminal, spinner, dispatcher) { harness.sendText(line) }
          ?.let { response ->
            harness.log("Gemini: $response")
            deployWithRetriesWithSpinner(harness, terminal, dispatcher)
          } ?: harness.logAndPrint("Gemini did not respond.")
      }
    }
  } finally {
    threadPool.shutdownNow()
  }
}
