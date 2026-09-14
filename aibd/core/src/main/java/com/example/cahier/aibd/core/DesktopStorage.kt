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

import com.google.genai.JsonSerializable
import com.google.genai.types.Content
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.io.copyRecursively
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DesktopStorage(baseDir: File) : Storage {
  private val basePath: Path = baseDir.toPath().normalize()
  private val sessionLogFile: Path

  companion object {
    private val objectMapper = JsonSerializable.objectMapper()
  }

  init {
    val logDir = basePath.resolve("log")
    logDir.createDirectories()
    val fileTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
    val now = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime()
    sessionLogFile = logDir.resolve("${now.format(fileTimeFormat)}.log")
    log(LOG_SESSION_ONLY, "Session initialized.")
  }

  private fun resolve(relativePath: String): Path {
    return basePath.resolve(relativePath).normalize()
  }

  private fun readTextFile(relativePath: String): String? {
    val path = resolve(relativePath)
    return if (path.exists()) {
      path.readText()
    } else {
      log(LOG_SESSION_ONLY, "WARNING: File not found: $path")
      null
    }
  }

  private fun writeTextFile(relativePath: String, content: String) {
    val path = resolve(relativePath)
    path.parent?.createDirectories()
    path.writeText(content)
  }

  private fun appendText(path: Path, content: String) {
    path.parent?.createDirectories()
    path.toFile().appendText(content)
  }

  private fun copyFile(sourceRelativePath: String, targetRelativePath: String): Boolean {
    val source = resolve(sourceRelativePath)
    val target = resolve(targetRelativePath)
    if (!source.exists()) return false
    target.parent?.createDirectories()
    source.copyTo(target, overwrite = true)
    return true
  }

  override fun getActiveConversationId(): String? {
    val linkPath = resolve("active_conversation")
    if (!Files.isSymbolicLink(linkPath)) return null
    return Files.readSymbolicLink(linkPath).fileName.toString()
  }

  override fun setActiveConversationId(uuid: String) {
    val linkPath = resolve("active_conversation")
    Files.deleteIfExists(linkPath)
    Files.createSymbolicLink(linkPath, Paths.get("conversations/$uuid"))
  }

  override fun loadManifest(uuid: String): Manifest? {
    val text = readTextFile("conversations/$uuid/manifest.json") ?: return null
    return objectMapper.readValue(text, Manifest::class.java)
  }

  override fun saveManifest(uuid: String, manifest: Manifest) {
    val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest)
    writeTextFile("conversations/${uuid}/manifest.json", json)
  }

  override fun loadHistory(uuid: String): List<Content>? {
    val text = readTextFile("conversations/$uuid/history.json") ?: return null
    val historyType =
      objectMapper.typeFactory.constructCollectionType(List::class.java, Content::class.java)
    return objectMapper.readValue(text, historyType)
  }

  override fun saveHistory(uuid: String, history: List<Content>) {
    val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(history)
    writeTextFile("conversations/$uuid/history.json", json)
  }

  override fun stageBrushFamilyContent(uuid: String, content: String) {
    writeTextFile("conversations/$uuid/new.textproto", content)
  }

  override fun promoteStagedBrushFamily(uuid: String): Boolean {
    return resolve("conversations/$uuid/new.brushfamily").exists() &&
      copyFile(
        "conversations/$uuid/new.brushfamily",
        "conversations/$uuid/${getBrushFamilyCount(uuid) + 1}.brushfamily",
      ) &&
      resolve("conversations/$uuid/new.textproto").deleteIfExists() &&
      resolve("conversations/$uuid/new.brushfamily").deleteIfExists()
  }

  override fun getBrushFamilyPath(uuid: String, index: Int?): String {
    return index?.let { resolve("conversations/$uuid/${index}.brushfamily").absolutePathString() }
      ?: resolve("conversations/$uuid/new.textproto").absolutePathString()
  }

  override fun getBrushFamilyBytes(uuid: String, index: Int?): ByteArray? {
    val path = resolve("conversations/$uuid/${index ?: "new"}.brushfamily")
    return if (path.exists()) path.readBytes() else null
  }

  override fun getBrushFamilyCount(uuid: String): Int {
    val conversationsDir = resolve("conversations/$uuid")
    if (!conversationsDir.exists()) return 0
    return conversationsDir.listDirectoryEntries().count {
      it.name.matches(Regex("^\\d+\\.brushfamily$"))
    }
  }

  override fun listConversationUuids(): List<String> {
    val conversationsDir = resolve("conversations")
    if (!conversationsDir.exists()) return emptyList()
    return conversationsDir.listDirectoryEntries().filter { it.isDirectory() }.map { it.name }
  }

  override fun copyConversation(sourceUuid: String, targetUuid: String): Boolean {
    val sourceDir = resolve("conversations/$sourceUuid")
    val targetDir = resolve("conversations/$targetUuid")
    if (!sourceDir.isDirectory()) return false
    sourceDir.toFile().copyRecursively(targetDir.toFile(), overwrite = true)
    return true
  }

  override fun createScreenshotFile(uuid: String): File {
    val timeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)
    val now = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime()
    val path = resolve("conversations/$uuid/screenshot/screenshot_${now.format(timeFormat)}.png")
    path.parent?.createDirectories()
    return path.toFile()
  }

  override fun log(conversationUuid: String?, message: String) {
    val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
    val now = OffsetDateTime.now(ZoneOffset.UTC).toLocalDateTime()
    val timestamp = "[${now.format(timeFormat)}]"
    val convPrefix = "[${conversationUuid?.take(8) ?: ""}]"

    appendText(sessionLogFile, "$timestamp $convPrefix $message\n")

    if (conversationUuid?.isNotEmpty() ?: false) {
      val convLogFile = resolve("conversations/$conversationUuid/execution.log")
      appendText(convLogFile, "$timestamp $message\n")
    }
  }
}
