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

import com.google.genai.types.Content
import java.io.File
import java.time.Instant
import java.time.InstantSource

enum class TitleSource {
  /** The title was not set by any source. */
  DEFAULT,
  /** The title was set manually by the user, and should not be overwritten except by the user. */
  USER_SPECIFIED,
  /** The title was set automatically by Gemini. Gemini may overwrite this title later. */
  AUTO_GEMINI,
  /**
   * The title was not set by Gemini nor manually by the user, so a snippet from the conversation
   * log was used as a fallback title. This title may be overwritten by Gemini or the user.
   */
  AUTO_PROMPT_FALLBACK,
}

data class Manifest(
  val title: String = "",
  val timestamp: Instant = InstantSource.system().instant(),
  val titleSource: TitleSource = TitleSource.DEFAULT,
) {
  fun withNonEmptyTitle(): Manifest {
    return if (title.isNotEmpty()) this else copy(title = "Untitled Conversation")
  }
}

/**
 * To be passed as the conversation UUID to Storage.log() in order to log only to the session log.
 */
val LOG_SESSION_ONLY: String? = null

interface Storage {
  /**
   * Returns the UUID of the currently active conversation, or null if there is no active
   * conversation.
   */
  fun getActiveConversationId(): String?

  /** Stores the UUID of the currently active conversation. */
  fun setActiveConversationId(uuid: String)

  /** Loads the manifest for the given conversation UUID, or null if it does not exist. */
  fun loadManifest(uuid: String): Manifest?

  /** Saves the given manifest to the storage. */
  fun saveManifest(uuid: String, manifest: Manifest)

  /** Loads the history for the given conversation UUID, or null if it does not exist. */
  fun loadHistory(uuid: String): List<Content>?

  /** Saves the given history to the storage for the given conversation UUID. */
  fun saveHistory(uuid: String, history: List<Content>)

  /**
   * Stages the given unvalidated brush family content for the given conversation UUID. Staged
   * content will either be promoted (and stored permanently in the conversation storage) or
   * overwritten by Gemini if they fail to deploy.
   */
  fun stageBrushFamilyContent(uuid: String, content: String)

  /**
   * Promotes the staged .brushfamily file for the given conversation UUID to the next available
   * index. Deletes the staged brush family content and .brushfamily file. If promotion fails, the
   * staged brush family content and .brushfamily file are not deleted. Returns true if the
   * promotion and deletion were successful, false otherwise.
   */
  fun promoteStagedBrushFamily(uuid: String): Boolean

  /**
   * Returns the path to the brush family file for the given conversation UUID and index, or null if
   * it does not exist. A null index indicates the staged brush family content.
   */
  fun getBrushFamilyPath(uuid: String, index: Int?): String

  /**
   * Returns the bytes of the .brushfamily file (gzipped binary BrushFamily proto) for the given
   * conversation UUID and index, or null if it does not exist. A null index indicates the staged
   * .brushfamily file.
   */
  fun getBrushFamilyBytes(uuid: String, index: Int?): ByteArray?

  /** Returns the number of valid brush families stored for the given conversation UUID. */
  fun getBrushFamilyCount(uuid: String): Int

  /** Returns a list of all conversation UUIDs in the storage. */
  fun listConversationUuids(): List<String>

  /**
   * Creates a copy of the conversation with the given source UUID to storage under the given target
   * UUID. Returns true if the copy was successful, false otherwise.
   */
  fun copyConversation(sourceUuid: String, targetUuid: String): Boolean

  /**
   * Creates a new screenshot file for the given conversation UUID. Returns the file or throws if
   * the file could not be created.
   */
  fun createScreenshotFile(uuid: String): File

  /**
   * Logs the given message to the session log and the conversation log for the given UUID, if
   * provided and valid. The session log is specific to the current runtime instance of AIBD, while
   * the conversation log is specific to the conversation, and the same file is used across
   * sessions.
   */
  fun log(conversationUuid: String?, message: String)
}
