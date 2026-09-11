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

class FakeAdb(
  override var device: String? = null,
  val installedApps: Set<String> = emptySet(),
  val logcatOutput: String = "",
  val nextResult: CommandResult = CommandResult(0, "", ""),
) : Adb {
  val commandsRun = mutableListOf<List<String>>()

  override fun run(args: List<String>, redirectOutput: File?): CommandResult {
    val deviceArgs = device?.let { listOf("-s", it) } ?: emptyList()
    commandsRun.add(deviceArgs + args)

    if (redirectOutput != null) {
      redirectOutput.writeText("fake screenshot content")
    }
    if (args.isNotEmpty()) {
      if (args.contains("logcat")) {
        return CommandResult(0, logcatOutput, "")
      }
      if (args.contains("date")) {
        return CommandResult(0, "06-29 06:31:41.000\n", "")
      }
    }
    return nextResult
  }

  override fun findDevice() {
    if (device == null) {
      throw IllegalStateException("No connected Android devices found via ADB.")
    }
  }

  override fun isInstalled(pkg: String): Boolean {
    return pkg in installedApps
  }
}
