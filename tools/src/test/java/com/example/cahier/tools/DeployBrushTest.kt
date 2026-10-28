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

import com.example.cahier.aibd.core.BrushFamilyInputFormat
import com.example.cahier.aibd.core.ValidationException
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DeployBrushTest {

  @get:Rule val tempFolder = TemporaryFolder()

  private lateinit var testdataDir: File
  private lateinit var tempDir: File
  private lateinit var inputFile: File

  @Before
  fun setUp() {
    val candidates =
      listOf(
        File(
          System.getenv("TEST_SRCDIR") ?: "",
          "tools/src/test/java/com/example/cahier/tools/testdata",
        ),
        File(
          System.getenv("BUILD_WORKSPACE_DIRECTORY") ?: "",
          "tools/src/test/java/com/example/cahier/tools/testdata",
        ),
        File("src/test/java/com/example/cahier/tools/testdata"),
        File("tools/src/test/java/com/example/cahier/tools/testdata"),
        File("tools/src/test/java/com/example/cahier/tools/testdata"),
      )
    testdataDir =
      candidates.firstOrNull { it.exists() }
        ?: error("Could not find testdata directory in any location: $candidates")
    tempDir = tempFolder.newFolder("deploy_test")
    inputFile = File(tempDir, "output.textproto")
    val validFile = File(testdataDir, "valid_brush.textproto")
    inputFile.writeText(validFile.readText())
  }

  @Test
  fun validateProto_validFile_succeeds() {
    val validFile = File(testdataDir, "valid_brush.textproto")
    val bytes = validateAndCompressBrushFamily(validFile.readText())
    assertThat(bytes).isNotEmpty()
  }

  @Test
  fun validateProto_invalidFile_throwsException() {
    val invalidFile = File(testdataDir, "invalid_brush.textproto")
    assertThrows(ValidationException::class.java) {
      validateAndCompressBrushFamily(invalidFile.readText())
    }
  }

  @Test
  fun deploy_noDevice_throwsException() {
    val fakeAdb = FakeAdb(device = null)
    val deployer = DesktopDeployer(adb = fakeAdb)

    val error =
      assertThrows(IllegalStateException::class.java) {
        deployer.deploy(BrushFamilyInputFormat.TEXTPROTO, inputFile.absolutePath) {}
      }
    assertThat(error).hasMessageThat().contains("No connected Android devices found")
  }

  @Test
  fun deploy_targetAppNotInstalled_throwsException() {
    val fakeAdb = FakeAdb(installedApps = emptySet(), device = "device1")
    val deployer = DesktopDeployer(adb = fakeAdb)

    val error =
      assertThrows(IllegalStateException::class.java) {
        deployer.deploy(BrushFamilyInputFormat.TEXTPROTO, inputFile.absolutePath) {}
      }
    assertThat(error).hasMessageThat().contains("not detected as installed on device")
  }

  @Test
  fun deploy_logcatError_throwsExceptionCitingLogcat() {
    val fakeAdb =
      FakeAdb(
        installedApps = setOf("com.example.cahier"),
        device = "device1",
        logcatOutput = "E MainActivity: java.lang.NullPointerException: test error",
      )
    val deployer = DesktopDeployer(adb = fakeAdb)

    val error =
      assertThrows(IllegalStateException::class.java) {
        deployer.deploy(BrushFamilyInputFormat.TEXTPROTO, inputFile.absolutePath) {}
      }
    assertThat(error).hasMessageThat().contains("Import failed with logcat errors")
    assertThat(fakeAdb.commandsRun)
      .contains(listOf("-s", "device1", "shell", "date", """+"%m-%d %H:%M:%S.000""""))
    assertThat(fakeAdb.commandsRun)
      .contains(
        listOf(
          "-s",
          "device1",
          "logcat",
          "-d",
          "-v",
          "threadtime",
          "-t",
          "06-29 06:31:41.000",
          "-e",
          "BrushGraph|MainActivity",
          "*:E",
        )
      )
  }

  @Test
  fun deploy_brushfamilyInputFormat_skipsValidation() {
    val binaryFile = File(tempDir, "sample.brushfamily")
    binaryFile.writeBytes(byteArrayOf(1, 2, 3)) // This would fail validation.

    val fakeAdb = FakeAdb(installedApps = setOf("com.example.cahier"), device = "device1")
    val deployer = DesktopDeployer(adb = fakeAdb)

    deployer.deploy(BrushFamilyInputFormat.BRUSHFAMILY, binaryFile.absolutePath) {}

    assertThat(fakeAdb.commandsRun)
      .contains(
        listOf(
          "-s",
          "device1",
          "push",
          binaryFile.absolutePath,
          "/sdcard/Android/data/com.example.cahier/files/sample.brushfamily",
        )
      )
  }

  @Test
  fun deploy_multiUser_runsExpectedCommands() {
    val fakeAdb =
      FakeAdb(installedApps = setOf("com.example.cahier"), device = "device1", currentUser = "1")
    val deployer = DesktopDeployer(adb = fakeAdb)

    deployer.deploy(BrushFamilyInputFormat.TEXTPROTO, inputFile.absolutePath) {}

    assertThat(fakeAdb.commandsRun)
      .contains(
        listOf(
          "-s",
          "device1",
          "shell",
          "cat",
          "/sdcard/Android/data/com.example.cahier/files/output.brushfamily",
          "|",
          "run-as",
          "com.example.cahier",
          "--user",
          "1",
          "sh",
          "-c",
          "'mkdir -p cache/shares && cat > cache/shares/output.brushfamily'",
        )
      )
    assertThat(fakeAdb.commandsRun)
      .contains(
        listOf(
          "-s",
          "device1",
          "shell",
          "am",
          "start",
          "--user",
          "1",
          "-n",
          "com.example.cahier/.MainActivity",
          "-a",
          "com.example.cahier.intent.action.IMPORT_BRUSH",
          "-d",
          "content://com.example.cahier.provider/shared_files/output.brushfamily",
        )
      )
  }
}
