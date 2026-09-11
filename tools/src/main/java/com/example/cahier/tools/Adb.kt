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

import java.io.File
import java.util.concurrent.TimeUnit

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

interface Adb {
  /** The device ID to use for ADB commands, or null if no device is set. */
  var device: String?

  /**
   * Runs an ADB command, formatted with `device` if set.
   *
   * @param args The arguments to pass to the ADB command.
   * @param redirectOutput The file to redirect the output to, or null if the output should be
   *   captured and returned in the result.
   * @return The result of the ADB command.
   */
  fun run(args: List<String>, redirectOutput: File? = null): CommandResult

  /**
   * Finds the first connected Android device and sets it as `device`, if not already set.
   *
   * @throws IllegalStateException if no connected Android devices are found.
   */
  fun findDevice()

  /**
   * Checks if the given package is installed on the device.
   *
   * @param pkg The package name to check.
   * @return True if the package is installed, false otherwise.
   */
  fun isInstalled(pkg: String): Boolean
}

class RealAdb(override var device: String? = null) : Adb {
  override fun run(args: List<String>, redirectOutput: File?): CommandResult {
    val deviceArgs = device?.let { listOf("-s", it) } ?: emptyList()
    val pb =
      ProcessBuilder(
        mutableListOf("adb").apply {
          addAll(deviceArgs)
          addAll(args)
        }
      )
    redirectOutput?.let { pb.redirectOutput(it) }
    var exitValue = -1
    return try {
      val process = pb.start()
      val completedInTime = process.waitFor(15, TimeUnit.SECONDS)
      if (!completedInTime) {
        process.destroyForcibly()
        CommandResult(-1, "", "Process timed out")
      } else {
        val stdOut =
          if (redirectOutput != null) "" else process.inputStream.bufferedReader().readText()
        val stdErr = process.errorStream.bufferedReader().readText()
        exitValue = process.exitValue()
        CommandResult(exitValue, stdOut, stdErr)
      }
    } catch (e: Exception) {
      CommandResult(-1, "", e.message ?: "Unknown error")
    } finally {
      if (exitValue != 0 && redirectOutput?.exists() ?: false) {
        redirectOutput.delete()
      }
    }
  }

  override fun findDevice() {
    device =
      device
        ?: run(listOf("devices"))
          .takeIf { it.exitCode == 0 }
          ?.let { result ->
            result.stdout.lineSequence().drop(1).firstNotNullOfOrNull { line ->
              val parts = line.trim().split("\\s+".toRegex())
              if (parts.size >= 2 && parts[1] == "device") parts[0] else null
            }
          }
        ?: throw IllegalStateException("No connected Android devices found via ADB.")
  }

  override fun isInstalled(pkg: String): Boolean =
    run(listOf("shell", "pm", "path", pkg)).let { result ->
      result.exitCode == 0 && result.stdout.contains("package:")
    }
}
