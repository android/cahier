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

package com.example.cahier.tools

import com.example.cahier.aibd.core.BrushFamilyDeployer
import com.example.cahier.aibd.core.BrushFamilyInputFormat
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

data class InkApp(
  val key: String,
  val packageName: String,
  val primaryUserProviderPath: String,
  val secondaryUserDir: String,
  val secondaryUserProviderPath: String,
  val intent: String,
  val activity: String,
  val name: String,
)

val APPS: List<InkApp> =
  listOf(
    InkApp(
      key = "cahier",
      packageName = "com.example.cahier",
      primaryUserProviderPath = "provider/external_files/",
      secondaryUserDir = "cache/shares",
      secondaryUserProviderPath = "provider/shared_files/",
      intent = "com.example.cahier.intent.action.IMPORT_BRUSH",
      activity = ".MainActivity",
      name = "Cahier",
    )
  )

val KEY_TO_APPS: Map<String, List<InkApp>> =
  APPS.associateBy { it.key }.mapValues { listOf(it.value) } + ("auto" to APPS)

const val PRIMARY_USER_ID = "0"

class DesktopDeployer(val adb: Adb = RealAdb(), val targetAppKey: String = "auto") :
  BrushFamilyDeployer {
  override fun deploy(format: BrushFamilyInputFormat, targetPath: String, log: (String) -> Unit) {
    adb.findDevice()
    val targetFile = File(targetPath)
    val outputBinaryFile =
      when (format) {
        BrushFamilyInputFormat.TEXTPROTO -> {
          val binaryContents = validateAndCompressBrushFamily(targetFile.readText())
          val outputFile =
            File(targetFile.parentFile, "${targetFile.nameWithoutExtension}.brushfamily")
          outputFile.writeBytes(binaryContents)
          outputFile
        }
        else -> targetFile
      }

    val chosenApps = KEY_TO_APPS[targetAppKey] ?: KEY_TO_APPS["auto"]!!
    val targetApp =
      chosenApps.firstOrNull { adb.isInstalled(it.packageName) }
        ?: run {
          val installedApps =
            KEY_TO_APPS["auto"]!!.filter { it !in chosenApps && adb.isInstalled(it.packageName) }
          val msg =
            "Selected app(s) ${chosenApps.joinToString { it.name }} are not detected as installed" +
              " on device. Installed apps: ${installedApps.joinToString { it.name }}"
          error(msg)
        }

    val userResult = adb.run(listOf("shell", "am", "get-current-user"))
    val currentUser =
      userResult.takeIf { userResult.exitCode == 0 }?.stdout?.trim()?.takeIf { it.isNotEmpty() }
        ?: PRIMARY_USER_ID

    // Push directly to external files storage so that both debug and release builds work.
    val appExternalDir = "/sdcard/Android/data/${targetApp.packageName}/files/"
    val stagedFilePath = "$appExternalDir${outputBinaryFile.name}"

    adb.run(listOf("shell", "mkdir", "-p", appExternalDir))
    log("Pushing directly to app external storage: $stagedFilePath")
    val pushResult = adb.run(listOf("push", outputBinaryFile.absolutePath, stagedFilePath))
    if (pushResult.exitCode != 0) {
      error("ADB push failed: ${pushResult.stderr}")
    }
    log("Successfully pushed brush family to app storage.")

    val contentUri =
      if (currentUser == PRIMARY_USER_ID) {
        "content://${targetApp.packageName}.${targetApp.primaryUserProviderPath}" +
          outputBinaryFile.name
      } else {
        log("Detected multi-user profile (user $currentUser); copying brush to app cache...")
        val secondaryUserFilePath = "${targetApp.secondaryUserDir}/${outputBinaryFile.name}"
        val copyResult =
          adb.run(
            listOf(
              "shell",
              "cat",
              stagedFilePath,
              "|",
              "run-as",
              targetApp.packageName,
              "--user",
              currentUser,
              "sh",
              "-c",
              "'mkdir -p ${targetApp.secondaryUserDir} && cat > $secondaryUserFilePath'",
            )
          )
        if (copyResult.exitCode != 0) {
          error("Failed to copy brush to user $currentUser cache: ${copyResult.stderr}")
        }
        "content://${targetApp.packageName}.${targetApp.secondaryUserProviderPath}" +
          outputBinaryFile.name
      }
    log("Importing from URI: $contentUri")
    log("Launching ${targetApp.name} and importing brush...")

    val dateResult = adb.run(listOf("shell", "date", """+"%m-%d %H:%M:%S.000""""))
    val startTime = dateResult.stdout.trim()

    val startResult =
      adb.run(
        listOfNotNull(
          "shell",
          "am",
          "start",
          "--user".takeIf { currentUser != PRIMARY_USER_ID },
          currentUser.takeIf { currentUser != PRIMARY_USER_ID },
          "-n",
          "${targetApp.packageName}/${targetApp.activity}",
          "-a",
          targetApp.intent,
          "-d",
          contentUri,
        )
      )
    if (startResult.exitCode != 0) {
      error("ADB am start failed: ${startResult.stderr}")
    }
    log("Successfully launched ${targetApp.name}.")

    checkLogcatForImportFailure(startTime)
  }

  private fun checkLogcatForImportFailure(startTime: String) {
    Thread.sleep(1000) // Wait for the import to have a chance to fail and log.
    val dumpArgs =
      listOfNotNull(
        "logcat",
        "-d",
        "-v",
        "threadtime",
        "-t".takeIf { startTime.isNotEmpty() },
        startTime.takeIf { startTime.isNotEmpty() },
        "-e",
        "BrushGraph|MainActivity",
        "*:E",
      )

    val result = adb.run(dumpArgs)
    if (result.exitCode == 0 && result.stdout.trim().isNotEmpty()) {
      error("Import failed with logcat errors:\n${result.stdout.trim()}")
    }
  }
}

/**
 * Validates and deploys a BrushFamily proto to a connected Android device.
 *
 * This tool takes a BrushFamily textproto, validates it, compresses it, pushes it to the external
 * storage of a target application on an Android device via ADB, and triggers an import intent.
 *
 * Typical usage:
 *
 * 1. Deploy the default output.textproto to an auto-detected app:
 *
 *    blaze run //tools/src/main/java/com/example/cahier/tools:deploy_brush
 *
 * 2. Deploy a specific textproto file:
 *
 *    blaze run //tools/src/main/java/com/example/cahier/tools:deploy_brush -- \
 *    --input=//tools/src/main/java/com/example/cahier/tools/my_brush.textproto
 *
 * 3. Deploy to a specific device and app:
 *
 *    blaze run //tools/src/main/java/com/example/cahier/tools:deploy_brush -- \
 *    --device=emulator-5554 --app=cahier
 */
@Command(
  name = "deploy_brush",
  description = ["Validates and deploys a BrushFamily proto to a connected Android device."],
  mixinStandardHelpOptions = true,
)
class DeployBrushCommand(val adb: Adb = RealAdb()) : Callable<Int> {
  @Option(
    names = ["-a", "--app"],
    description =
      ["Target app to deploy to and launch. If auto, will consider the other options in order."],
    defaultValue = "auto",
  )
  var targetAppKey: String = "auto"

  @Option(names = ["-d", "--device"], description = ["Specific device ID to use (optional)"])
  var device: String? = null

  @Option(names = ["-i", "--input"], description = ["Path to brush textproto or .brushfamily file"])
  var inputFile: File? = null

  @Option(
    names = ["-f", "--input_format"],
    description =
      [
        "Input format. Valid values are textproto (unvalidated BrushFamily textproto) or ",
        "brushfamily (validated gzipped BrushFamily binary-proto).",
      ],
    defaultValue = "textproto",
  )
  var inputFormat: String = "textproto"

  override fun call(): Int {
    device?.let { adb.device = device }

    val workspaceDir = File(System.getenv("BUILD_WORKSPACE_DIRECTORY") ?: ".")
    val targetFile =
      inputFile
        ?: File(workspaceDir, "tools/src/main/java/com/example/cahier/tools/output.textproto")

    val deployer = DesktopDeployer(adb = adb, targetAppKey = targetAppKey)
    val format =
      when (inputFormat) {
        "brushfamily" -> BrushFamilyInputFormat.BRUSHFAMILY
        else -> BrushFamilyInputFormat.TEXTPROTO
      }

    return try {
      deployer.deploy(format, targetFile.absolutePath) { line -> println(line) }
      0
    } catch (e: Exception) {
      System.err.println("Deployment failed: ${e.message}")
      1
    }
  }
}

fun main(args: Array<String>) {
  exitProcess(CommandLine(DeployBrushCommand()).execute(*args))
}
