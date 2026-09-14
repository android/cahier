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
@file:Suppress("GlobalCoroutineDispatchers")

package com.example.cahier.aibd.cli

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.encode
import com.example.cahier.aibd.core.BrushFamilyDeployer
import com.example.cahier.aibd.core.BrushFamilyInputFormat
import com.example.cahier.aibd.core.DesktopStorage
import com.example.cahier.aibd.core.FakeChat
import com.example.cahier.aibd.core.FakeChats
import com.example.cahier.aibd.core.FakeClient
import com.example.cahier.aibd.core.Harness
import com.example.cahier.aibd.core.ImagePayloadConverter
import com.example.cahier.tools.CommandResult
import com.example.cahier.tools.FakeAdb
import com.google.common.truth.Truth.assertThat
import com.google.genai.types.GenerateContentResponse
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class CliTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private lateinit var testBaseDir: File
  private lateinit var storage: DesktopStorage
  private lateinit var fakeChat: FakeChat
  private lateinit var fakeClient: FakeClient
  private lateinit var harness: Harness<ByteArray>

  // Capture standard output print streams
  private lateinit var systemOut: PrintStream
  private val capturedOut = ByteArrayOutputStream()

  class FakeImageConverter : ImagePayloadConverter<ByteArray> {
    override fun convert(payload: ByteArray): com.google.genai.types.Part {
      return com.google.genai.types.Part.builder().text("fake-image").build()
    }
  }

  class FakeDeployer : BrushFamilyDeployer {
    override fun deploy(
      format: BrushFamilyInputFormat,
      targetPath: String,
      log: (String) -> Unit,
    ) {}
  }

  class CountDeployer(private val expectedFormat: BrushFamilyInputFormat) : BrushFamilyDeployer {
    var deployCount = 0

    override fun deploy(format: BrushFamilyInputFormat, targetPath: String, log: (String) -> Unit) {
      deployCount++
      assertThat(format).isEqualTo(expectedFormat)
    }
  }

  @Before
  fun setUp() {
    testBaseDir = tempFolder.newFolder("cli_test_root")
    storage = DesktopStorage(testBaseDir)
    fakeChat = FakeChat()
    fakeClient = FakeClient(FakeChats(fakeChat))
    harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    systemOut = System.out
    System.setOut(PrintStream(capturedOut))
  }

  @After
  fun tearDown() {
    System.setOut(systemOut)
  }

  private fun createMockTerminal(): org.jline.terminal.Terminal {
    val terminal = mock<org.jline.terminal.Terminal>()
    val attributes = mock<org.jline.terminal.Attributes>()
    val reader = mock<org.jline.utils.NonBlockingReader>()
    whenever(terminal.enterRawMode()).thenReturn(attributes)
    whenever(terminal.reader()).thenReturn(reader)
    whenever(reader.peek(any())).thenReturn(-1)
    whenever(terminal.handle(any(), any())).thenReturn(null)
    return terminal
  }

  @Test
  fun newCommand_runsCreateNewConversation_withCorrectTitle() {
    val command = NewCommand(harness)
    command.titleWords = listOf("hello", "brush")
    command.run()

    assertThat(harness.activeConversation.manifest.title).isEqualTo("hello brush")
  }

  @Test
  fun forkCommand_runsForkConversation_withCorrectTitle() {
    val command = ForkCommand(harness)
    command.titleWords = listOf("forked", "brush")
    command.run()

    assertThat(harness.activeConversation.manifest.title).isEqualTo("forked brush")
  }

  @Test
  fun deployCommand_noBrushesOrStagedContent_printsErrorMsg() {
    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    val command = DeployCommand(harness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(capturedOut.toString())
      .contains("No staged brush found to deploy. Prompt Gemini first!")
  }

  @Test
  fun deployCommand_cancelledByUser_doesNotDeploy() {
    // Populate fake persisted brushes list
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/1.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("c")

    val command = DeployCommand(harness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(capturedOut.toString()).contains("Deployment cancelled.")
  }

  @Test
  fun deployCommand_textproto_deploysStagedBrush() {
    // Stage brush family
    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "brush content")
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(byteArrayOf(0x01))

    val countDeployer = CountDeployer(BrushFamilyInputFormat.TEXTPROTO)
    val localHarness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = countDeployer,
      )

    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("0")

    val command = DeployCommand(localHarness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(countDeployer.deployCount).isEqualTo(1)
  }

  @Test
  fun deployCommand_brushfamily_deploysSpecificBrush() {
    // Create one persisted brush family
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/1.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    val countDeployer = CountDeployer(BrushFamilyInputFormat.BRUSHFAMILY)
    val localHarness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = countDeployer,
      )

    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("1")

    val command = DeployCommand(localHarness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(countDeployer.deployCount).isEqualTo(1)
  }

  @Test
  fun deployCommand_invalidIndexInput_printsWarningAndCancels() {
    // Create one persisted brush family
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/1.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("5")

    val command = DeployCommand(harness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(capturedOut.toString()).contains("Invalid selection. Deployment cancelled.")
  }

  @Test
  fun deployCommand_nonIntegerInput_printsWarningAndCancels() {
    // Create one persisted brush family
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/1.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    val terminal = createMockTerminal()
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("asdf")

    val command = DeployCommand(harness, lineReader, terminal, Dispatchers.Unconfined)
    command.run()

    assertThat(capturedOut.toString()).contains("Invalid selection. Deployment cancelled.")
  }

  @Test
  fun resumeCommand_emptySessionsList_printsNoSessions() {
    val tempDir = tempFolder.newFolder("cli_test_resume_empty")
    val cleanStorage = DesktopStorage(tempDir)
    val localHarness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = cleanStorage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )
    // Clear out the conversation created automatically by the Harness constructor
    File(tempDir, "conversations/${localHarness.activeConversation.uuid}").deleteRecursively()
    File(tempDir, "active_conversation").delete()

    val lineReader = mock<org.jline.reader.LineReader>()
    val command = ResumeCommand(localHarness, lineReader)
    command.run()

    assertThat(capturedOut.toString()).contains("No prior conversations found.")
  }

  @Test
  fun resumeCommand_userCancels_abortsResumption() {
    harness.createNewConversation(title = "Convo 1")
    harness.createNewConversation(title = "Convo 2")

    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("c")

    val command = ResumeCommand(harness, lineReader)
    command.run()

    assertThat(capturedOut.toString()).contains("Resumption cancelled.")
  }

  @Test
  fun resumeCommand_validIdx_loadsConversation() {
    harness.createNewConversation(title = "Convo 1")
    val uuid1 = harness.activeConversation.uuid
    harness.createNewConversation(title = "Convo 2")

    // Inputs "2" (the second conversation list element)
    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("2")

    val command = ResumeCommand(harness, lineReader)
    command.run()

    assertThat(harness.activeConversation.uuid).isEqualTo(uuid1)
    assertThat(capturedOut.toString()).contains("Successfully resumed conversation: \"Convo 1\"")
  }

  @Test
  fun resumeCommand_invalidIndexInput_printsWarningAndCancels() {
    harness.createNewConversation(title = "Convo 1")

    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("99")

    val command = ResumeCommand(harness, lineReader)
    command.run()

    assertThat(capturedOut.toString()).contains("Invalid selection. Resumption cancelled.")
  }

  @Test
  fun resumeCommand_nonIntegerInput_printsWarningAndCancels() {
    harness.createNewConversation(title = "Convo 1")

    val lineReader = mock<org.jline.reader.LineReader>()
    whenever(lineReader.readLine(any<String>())).thenReturn("asdf")

    val command = ResumeCommand(harness, lineReader)
    command.run()

    assertThat(capturedOut.toString()).contains("Invalid selection. Resumption cancelled.")
  }

  @Test
  fun imageCommand_invalidFilePath_displaysError() {
    val terminal = createMockTerminal()
    val command = ImageCommand(harness, terminal, Dispatchers.Unconfined)
    command.path = "non_existent_file.png"
    command.promptWords = listOf("brush", "design")
    command.run()

    assertThat(capturedOut.toString()).contains("File not found: non_existent_file.png")
  }

  @Test
  fun imageCommand_validFileAndPrompt_sendsPayloadToGemini() {
    val imageFile = tempFolder.newFile("test_image.png")
    imageFile.writeBytes(byteArrayOf(0x0a, 0x0b))

    val fakeResponseJson =
      """{"candidates": [{"content": {"parts": [{"text": "Gemini response text"}], "role": "model"}}]}"""
    fakeChat.scriptedResponses =
      listOf(
        GenerateContentResponse.fromJsonString(
          fakeResponseJson,
          GenerateContentResponse::class.java,
        )
      )

    val terminal = createMockTerminal()
    val command = ImageCommand(harness, terminal, Dispatchers.Unconfined)
    command.path = imageFile.absolutePath
    command.promptWords = listOf("looks", "good")
    command.run()

    assertThat(fakeChat.history).isNotEmpty()
    val promptSent = fakeChat.sentRequests.firstOrNull()
    assertThat(promptSent).isNotNull()
  }

  @Test
  fun screenshotCommand_adbCaptureFails_cleansUpFileAndPrintsError() {
    val fakeAdb =
      FakeAdb(nextResult = CommandResult(exitCode = 1, stdout = "", stderr = "device offline"))

    val terminal = createMockTerminal()
    val command = ScreenshotCommand(harness, terminal, Dispatchers.Unconfined, fakeAdb)
    command.promptWords = listOf("draw", "this")
    command.run()

    assertThat(capturedOut.toString())
      .contains("ADB screenshot capture failed (exit code 1): device offline")
  }

  @Test
  fun screenshotCommand_success_capturesAndSendsImage() {
    val fakeAdb = FakeAdb(nextResult = CommandResult(exitCode = 0, stdout = "success", stderr = ""))

    val fakeResponseJson =
      """{"candidates": [{"content": {"parts": [{"text": "Visual design looks excellent"}], "role": "model"}}]}"""
    fakeChat.scriptedResponses =
      listOf(
        GenerateContentResponse.fromJsonString(
          fakeResponseJson,
          GenerateContentResponse::class.java,
        )
      )

    val terminal = createMockTerminal()
    val command = ScreenshotCommand(harness, terminal, Dispatchers.Unconfined, fakeAdb)
    command.promptWords = listOf("use", "screenshot")
    command.run()

    assertThat(fakeAdb.commandsRun).isNotEmpty()
    assertThat(fakeAdb.commandsRun[0]).containsExactly("exec-out", "screencap", "-p")
    assertThat(fakeChat.sentRequests).isNotEmpty()
  }
}
