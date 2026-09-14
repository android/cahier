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
package com.example.cahier.aibd.core

import com.google.common.truth.Truth.assertThat
import com.google.genai.types.Content
import com.google.genai.types.Part
import java.io.File
import java.nio.file.Files
import kotlin.jvm.optionals.getOrNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DesktopStorageTest {

  @get:Rule val tempFolder = TemporaryFolder()

  @Test
  fun activeConversationId_saveAndGet_createsSymlink() {
    val baseDir = tempFolder.newFolder("storage_test")
    val repo = DesktopStorage(baseDir)

    assertThat(repo.getActiveConversationId()).isNull()

    repo.setActiveConversationId("uuid")
    assertThat(repo.getActiveConversationId()).isEqualTo("uuid")

    val symlinkPath = baseDir.toPath().resolve("active_conversation")
    assertThat(Files.isSymbolicLink(symlinkPath)).isTrue()
    assertThat(Files.readSymbolicLink(symlinkPath).toString()).isEqualTo("conversations/uuid")
  }

  @Test
  fun getActiveConversationId_returnsNullIfSymlinkInvalid() {
    val baseDir = tempFolder.newFolder("storage_test_invalid_symlink")
    val repo = DesktopStorage(baseDir)

    val symlinkPath = baseDir.toPath().resolve("conversations/active_conversation")
    symlinkPath.parent?.toFile()?.mkdirs()
    // Create a regular file instead of a symlink
    symlinkPath.toFile().writeText("not a symlink")

    assertThat(repo.getActiveConversationId()).isNull()
  }

  @Test
  fun saveAndLoadManifest_preservesFields() {
    val baseDir = tempFolder.newFolder("storage_test_manifest")
    val repo = DesktopStorage(baseDir)

    val manifest = Manifest(title = "My Title")
    repo.saveManifest("uuid", manifest)

    val loaded = repo.loadManifest("uuid")
    assertThat(loaded).isEqualTo(manifest)
  }

  @Test
  fun saveAndLoadHistory_preservesContent() {
    val baseDir = tempFolder.newFolder("storage_test_history")
    val repo = DesktopStorage(baseDir)

    val part = Part.builder().text("Hello world").build()
    val content = Content.builder().parts(listOf(part)).role("user").build()

    repo.saveHistory("history-uuid", listOf(content))

    val loaded = repo.loadHistory("history-uuid")
    assertThat(loaded).isNotNull()
    assertThat(loaded).hasSize(1)
    assertThat(loaded!![0].role().getOrNull()).isEqualTo("user")
  }

  @Test
  fun promoteStagedBrushFamily_copiesBinaryFileIfPresent() {
    val baseDir = tempFolder.newFolder("storage_test_promote_binary")
    val repo = DesktopStorage(baseDir)

    repo.stageBrushFamilyContent("uuid", "asdf")

    val binaryFile = File(baseDir, "conversations/uuid/new.brushfamily")
    binaryFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03))
    assertThat(repo.getBrushFamilyBytes("uuid", null)).isEqualTo(byteArrayOf(0x01, 0x02, 0x03))

    assertThat(repo.promoteStagedBrushFamily("uuid")).isTrue()
    assertThat(repo.getBrushFamilyBytes("uuid", 1)).isEqualTo(byteArrayOf(0x01, 0x02, 0x03))
    assertThat(repo.getBrushFamilyBytes("uuid", null)).isNull()
    assertThat(File(repo.getBrushFamilyPath("uuid", null)).exists()).isFalse()
  }

  @Test
  fun getBrushFamilyPath_returnsCorrectRelativePath() {
    val baseDir = tempFolder.newFolder("storage_test_paths")
    val repo = DesktopStorage(baseDir)

    val stagedTextprotoPath = repo.getBrushFamilyPath("uuid", null)
    assertThat(stagedTextprotoPath).endsWith("conversations/uuid/new.textproto")

    val promotedPath = repo.getBrushFamilyPath("uuid", 5)
    assertThat(promotedPath).endsWith("conversations/uuid/5.brushfamily")
  }

  @Test
  fun getBrushFamilyBytes_returnsBytes() {
    val baseDir = tempFolder.newFolder("storage_test_bytes")
    val repo = DesktopStorage(baseDir)

    val bytes = byteArrayOf(0x01, 0x02, 0x03)
    repo.stageBrushFamilyContent("uuid", "asdf")
    val binaryFile = File(baseDir, "conversations/uuid/new.brushfamily")
    binaryFile.writeBytes(bytes)

    val result = repo.getBrushFamilyBytes("uuid", null)
    assertThat(result).isEqualTo(bytes)

    assertThat(repo.promoteStagedBrushFamily("uuid")).isTrue()
    val result2 = repo.getBrushFamilyBytes("uuid", 1)
    assertThat(result2).isEqualTo(bytes)
  }

  @Test
  fun getBrushFamilyBytes_returnsNullIfMissing() {
    val baseDir = tempFolder.newFolder("storage_test_bytes_missing")
    val repo = DesktopStorage(baseDir)

    val result = repo.getBrushFamilyBytes("uuid", null)
    assertThat(result).isNull()
  }

  @Test
  fun getBrushFamilyCount_returnsCorrectCount() {
    val baseDir = tempFolder.newFolder("storage_test_count")
    val repo = DesktopStorage(baseDir)

    assertThat(repo.getBrushFamilyCount("uuid")).isEqualTo(0)

    repo.stageBrushFamilyContent("uuid", "asdf")
    assertThat(repo.getBrushFamilyCount("uuid")).isEqualTo(0)

    val bytes = byteArrayOf(0x01, 0x02, 0x03)
    val binaryFile = File(baseDir, "conversations/uuid/new.brushfamily")
    binaryFile.writeBytes(bytes)
    assertThat(repo.promoteStagedBrushFamily("uuid")).isTrue()
    assertThat(repo.getBrushFamilyCount("uuid")).isEqualTo(1)
  }

  @Test
  fun listConversationUuids_returnsExtantConversationUuids() {
    val baseDir = tempFolder.newFolder("storage_test_list")
    val repo = DesktopStorage(baseDir)

    assertThat(repo.listConversationUuids()).isEmpty()
    assertThat(repo.loadManifest("uuid")).isNull()

    repo.saveManifest("uuid", Manifest())
    assertThat(repo.listConversationUuids()).containsExactly("uuid")
    assertThat(repo.loadManifest("uuid")).isNotNull()
  }

  @Test
  fun log_writesToSessionAndConversationLog() {
    val baseDir = tempFolder.newFolder("storage_test_log")
    val repo = DesktopStorage(baseDir)

    repo.log("uuid", "Test log message")

    val convLogFile = File(baseDir, "conversations/uuid/execution.log")
    assertThat(convLogFile.exists()).isTrue()
    assertThat(convLogFile.readText()).contains("Test log message")
    val sessionLogFile = File(baseDir, "log").listFiles()?.firstOrNull()
    assertThat(sessionLogFile?.exists()).isTrue()
    assertThat(sessionLogFile?.readText()).contains("Test log message")
  }

  @Test
  fun createScreenshotFile_createsFormattedFileHandle() {
    val baseDir = tempFolder.newFolder("storage_test_screenshot")
    val repo = DesktopStorage(baseDir)

    val file = repo.createScreenshotFile("uuid")
    assertThat(file.parentFile?.exists()).isTrue()
    assertThat(file.name).matches("screenshot_\\d{8}_\\d{6}\\.png")
  }

  @Test
  fun copyConversation_copiesAllFiles() {
    val baseDir = tempFolder.newFolder("storage_test_copy")
    val repo = DesktopStorage(baseDir)

    repo.saveManifest("uuid-src", Manifest(title = "Source"))
    repo.stageBrushFamilyContent("uuid-src", "asdf")
    val bytes = byteArrayOf(0x01, 0x02, 0x03)
    val binaryFile = File(baseDir, "conversations/uuid-src/new.brushfamily")
    binaryFile.writeBytes(bytes)
    assertThat(repo.promoteStagedBrushFamily("uuid-src")).isTrue()

    val copied = repo.copyConversation("uuid-src", "uuid-dest")
    assertThat(copied).isTrue()
    assertThat(repo.listConversationUuids()).containsExactly("uuid-src", "uuid-dest")
    assertThat(repo.loadManifest("uuid-dest")).isEqualTo(repo.loadManifest("uuid-src"))
    assertThat(repo.getBrushFamilyBytes("uuid-dest", 1)).isEqualTo(bytes)
    assertThat(repo.getBrushFamilyCount("uuid-dest"))
      .isEqualTo(repo.getBrushFamilyCount("uuid-src"))
  }

  @Test
  fun withNonEmptyTitle_returnsTitleIfPresent() {
    val manifest = Manifest(title = "Title")
    assertThat(manifest.withNonEmptyTitle().title).isEqualTo("Title")
    val emptyTitle = Manifest(title = "")
    assertThat(emptyTitle.withNonEmptyTitle().title).isEqualTo("Untitled Conversation")
  }
}
