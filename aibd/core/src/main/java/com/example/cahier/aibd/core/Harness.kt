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
@file:OptIn(ExperimentalInkCustomBrushApi::class)

package com.example.cahier.aibd.core

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.decode
import com.google.genai.Client as SdkClient
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.HttpOptions
import com.google.genai.types.Part
import java.io.File
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

data class Conversation(val uuid: String, val manifest: Manifest, val chat: Chat) {

  constructor(
    title: String,
    chat: Chat,
  ) : this(
    UUID.randomUUID().toString(),
    Manifest(
      title = title,
      titleSource =
        if (title.isNotEmpty()) {
          TitleSource.USER_SPECIFIED
        } else {
          TitleSource.DEFAULT
        },
    ),
    chat,
  )
}

data class PersistedBrushInfo(val index: Int, val title: String)

class ValidationException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface ImagePayloadConverter<T> {
  fun convert(payload: T): Part
}

enum class BrushFamilyInputFormat {
  TEXTPROTO,
  BRUSHFAMILY,
}

interface BrushFamilyDeployer {
  /**
   * Deploys a staged or saved brush family to the device.
   *
   * @param format The format of the brush family input.
   * @param targetPath The path to the brush family file.
   * @param log A callback to log messages.
   * @throws ValidationException if the brush family content fails to be validated when converting
   *   to a BrushFamily binary proto.
   * @throws Exception if the brush family fails to deploy for any other reason.
   */
  fun deploy(format: BrushFamilyInputFormat, targetPath: String, log: (String) -> Unit)
}

/**
 * Platform-agnostic harness for interacting with Gemini and managing brush design conversations.
 *
 * @param modelName The model code for the Gemini model to use. Find model codes under the model
 *   descriptions available at https://ai.google.dev/gemini-api/docs/models, e.g. gemini-3.5-flash
 *   for https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash#gemini-35-flash
 * @param imageConverter Converts the image payload to a Part.
 * @param deployer Platform-specific logic to deploy a brush to the device. See [Deployer] above.
 * @param storage Repository to persist conversation data between sessions. Responsible for storing
 *   conversation chat history, as well as brush family artifacts and logs.
 */
class Harness<T>(
  private val imageConverter: ImagePayloadConverter<T>,
  private val modelName: String = "gemini-3.5-flash",
  val storage: Storage = DesktopStorage(File(System.getProperty("user.home"), ".ink-aibd")),
  private val client: Client =
    try {
      val httpOptions = HttpOptions.builder().timeout(300000).build()
      val sdkClient = SdkClient.builder().httpOptions(httpOptions).build()
      RealClient(sdkClient)
    } catch (e: Exception) {
      throw IllegalStateException(
        """
        Failed to create Gemini client. Did you set API key environment variable?
        If not, please visit https://ai.google.dev/gemini-api/docs/api-key and create one.
        Then set the environment variable: export GOOGLE_API_KEY='<key>'
          """,
        e,
      )
    },
  private val deployer: BrushFamilyDeployer,
) {
  private val systemPrompt: String
  private val chatConfig: GenerateContentConfig by lazy {
    GenerateContentConfig.builder()
      .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
      .build()
  }
  lateinit var activeConversation: Conversation
    private set

  init {
    systemPrompt = loadSystemPrompt()
    if (!initializeFromExistingConversation()) {
      createNewConversation()
    }
  }

  private fun createChat(): Chat = client.chats.create(modelName, chatConfig)

  /**
   * Starts a new conversation with the given title. If the title is empty, the title will be
   * generated based on the first user prompt.
   */
  fun createNewConversation(title: String = "") {
    try {
      Conversation(title = title, createChat()).saveAsActive()?.let {
        log("Conversation initialized.")
      } ?: logAndPrint("Failed to start new conversation.")
    } catch (e: Exception) {
      logAndPrint("Failed to start new conversation: ${e.message}")
    }
  }

  /**
   * Attempts to create a copy of the active conversation in storage, with the given title. If the
   * title is empty, the title and title source are copied from the source conversation. If
   * successful, the active conversation will be the newly created conversation.
   */
  fun forkConversation(title: String = "") {
    try {
      val sourceConversation = activeConversation
      val destConversation =
        Conversation(
          title = if (title.isEmpty()) sourceConversation.manifest.title else title,
          sourceConversation.chat,
        )

      if (!storage.copyConversation(sourceConversation.uuid, destConversation.uuid)) {
        throw IllegalStateException("Failed to copy conversation storage")
      }

      destConversation.saveAsActive()?.let {
        log("Conversation forked from ${sourceConversation.uuid} to ${destConversation.uuid}.")
      } ?: logAndPrint("Failed to fork conversation.")
    } catch (e: Exception) {
      logAndPrint("Failed to fork conversation: ${e.message}")
    }
  }

  /**
   * Loads the Conversation with the given UUID from storage, and sets it as the active
   * conversation. Returns the loaded Conversation if successful, and null otherwise.
   */
  fun loadConversation(uuid: String): Conversation? {
    try {
      val manifest = storage.loadManifest(uuid) ?: return null

      return Conversation(uuid, manifest, createChat()).setActive()?.also {
        log("Conversation resumed successfully.")
      }
    } catch (e: Exception) {
      logAndPrint("Conversation files corrupted: ${e.message ?: ""}")
      return null
    }
  }

  private fun initializeFromExistingConversation(): Boolean {
    try {
      val existingId = storage.getActiveConversationId() ?: return false
      log("Attempting to initialize from existing conversation $existingId...")

      return loadConversation(existingId) != null
    } catch (e: Exception) {
      log(
        "WARNING: Active conversation corrupted on boot. Starting fresh: ${e.stackTraceToString()}"
      )
      return false
    }
  }

  /** Saves the given conversation to storage and sets it as the active conversation. */
  private fun Conversation.saveAsActive(): Conversation? {
    return this.save()?.setActive()
  }

  /**
   * Sets the given conversation as the active conversation in the harness, and saves the UUID to
   * storage.
   */
  private fun Conversation.setActive(): Conversation? {
    try {
      val history =
        storage.loadHistory(this.uuid)
          ?: throw IllegalStateException("Failed to load history for conversation ${this.uuid}")
      this.chat.restoreHistory(history, history)

      storage.setActiveConversationId(this.uuid)
      activeConversation = this
      return this
    } catch (e: Exception) {
      logAndPrint("Failed to make conversation active: ${e.message}")
      return null
    }
  }

  /** Saves the given conversation to storage. */
  private fun Conversation.save(): Conversation? {
    try {
      storage.saveHistory(this.uuid, this.chat.getHistory(true))
      storage.saveManifest(this.uuid, this.manifest)
      return this
    } catch (e: Exception) {
      logAndPrint("Failed to save conversation ${this.uuid}: ${e.message}")
      return null
    }
  }

  /**
   * Sends a text prompt to Gemini. Returns the response text and stages it as brush family content
   * if successful, and returns null otherwise.
   */
  fun sendText(prompt: String): String? {
    log("User prompt: $prompt")

    return activeConversation.chat.sendMessage(prompt).text()?.let { stageBrushFamilyContent(it) }
  }

  /**
   * Sends an image and text prompt to Gemini. Returns the response text and stages it as brush
   * family content if successful, and returns null otherwise.
   */
  fun sendImage(prompt: String, imagePayload: T): String? {
    log("User image prompt: $prompt")

    return activeConversation.chat
      .sendMessage(Content.fromParts(Part.fromText(prompt), imageConverter.convert(imagePayload)))
      .text()
      ?.let { stageBrushFamilyContent(it) }
  }

  /** Attempts to restore the active chat session to the last state saved to storage. */
  fun revertChatState() {
    if (activeConversation.copy(chat = createChat()).setActive() != null) {
      log("Request cancelled by user. Restored last saved history.")
    } else {
      logAndPrint(
        "Failed to update active conversation to last saved history, state may be corrupted."
      )
    }
  }

  private fun parseResponse(responseText: String): String {
    val pattern = """```(?:textproto|proto)?\s*(.*?)\s*```""".toRegex(RegexOption.DOT_MATCHES_ALL)
    return pattern.find(responseText)?.groupValues?.get(1) ?: responseText
  }

  private fun stageBrushFamilyContent(content: String): String {
    try {
      storage.stageBrushFamilyContent(activeConversation.uuid, parseResponse(content))
      log("Gemini response staged.")

      activeConversation.save()?.let { log("Conversation history saved.") }
        ?: logAndPrint("Failed to save conversation history.")
    } catch (e: Exception) {
      logAndPrint("Failed to stage brush family content/save history: ${e.message}")
    }
    return content
  }

  /**
   * Deploys a staged or saved brush family to the device. Will throw an exception via
   * deployer.deploy() if the brush family fails to deploy.
   */
  fun deploy(index: Int? = null) {
    val brushFamilyPath = storage.getBrushFamilyPath(activeConversation.uuid, index)
    log("Deploying to device...")
    deployer.deploy(
      index?.let { BrushFamilyInputFormat.BRUSHFAMILY } ?: BrushFamilyInputFormat.TEXTPROTO,
      brushFamilyPath,
      this::log,
    )
    log("Successfully deployed to device.")

    getBrushFamily(index)?.let {
      if (it.developerComment.isNotEmpty()) {
        println("\n=== Brush Summary ===")
        println(it.developerComment)
        println("=====================\n")
      }
    }

    index ?: promoteStagedBrushFamily()
  }

  /**
   * Attempts to deploy the brush family at the given index to the device. If the deployment fails
   * due to a validation error, the brush family is sent to Gemini for retrying.
   *
   * @param maxAttempts The number of times to try to deploy the brush family before giving up.
   * @param validationRetryCallback The callback to use when a validation error is encountered.
   *   Takes in the error message from the failed deployment and returns the response from Gemini,
   *   or null if the retry should not be attempted.
   */
  fun deployWithRetries(maxAttempts: Int = 4, validationRetryCallback: (String) -> String?) {
    for (attempt in 1..maxAttempts) {
      try {
        deploy()
        break
      } catch (e: ValidationException) {
        if (attempt < maxAttempts) {
          logAndPrint("Found a validation error! Sending to Gemini.")
          validationRetryCallback(e.message ?: "Validation failed.")?.let {
            log("Gemini (retry $attempt): $it")
          } ?: logAndPrint("Gemini (retry $attempt) failed to respond.")
        } else {
          logAndPrint("Deploy failed after $maxAttempts attempts: ${e.message}")
        }
      } catch (e: Exception) {
        logAndPrint("Deploy failed on attempt $attempt: ${e.message}")
        break
      }
    }
  }

  /**
   * Attempts to promote the staged brush family to the next available index in the conversation. If
   * successful, the manifest is updated with the new title and title source, and the conversation
   * is updated with the new manifest.
   */
  private fun promoteStagedBrushFamily() {
    if (storage.promoteStagedBrushFamily(activeConversation.uuid)) {
      val manifest = activeConversation.manifest
      val index = storage.getBrushFamilyCount(activeConversation.uuid)
      val newFamilyId = getBrushFamily(index)?.clientBrushFamilyId?.takeIf { it.isNotEmpty() }

      val (newTitle, newSource) =
        when {
          manifest.titleSource == TitleSource.USER_SPECIFIED ->
            manifest.title to TitleSource.USER_SPECIFIED
          newFamilyId != null -> newFamilyId to TitleSource.AUTO_GEMINI
          manifest.titleSource != TitleSource.AUTO_GEMINI ->
            extractPromptFallback(index) to TitleSource.AUTO_PROMPT_FALLBACK
          else -> manifest.title to manifest.titleSource
        }

      activeConversation
        .copy(manifest = manifest.copy(title = newTitle, titleSource = newSource))
        .saveAsActive()
        ?.let {
          log("Gemini persisted ${index}.brushfamily")
          activeConversation = activeConversation.copy(manifest = it.manifest)
          log("Valid brush count: ${index}")
        } ?: logAndPrint("Failed to update manifest after promotion.")
    } else {
      logAndPrint("Failed to promote staged brush content.")
    }
  }

  /**
   * Returns a list of the UUIDs and manifests of the available conversations, with the most
   * recently created conversation first.
   */
  fun listAvailableConversations(): List<Pair<String, Manifest>> {
    return storage
      .listConversationUuids()
      .mapNotNull { uuid ->
        storage.loadManifest(uuid)?.let { manifest -> uuid to manifest.withNonEmptyTitle() }
      }
      .sortedByDescending { (_, m) -> m.timestamp }
  }

  /**
   * Returns a list of the indices and titles of the valid brushes in the current conversation, with
   * the latest brush first.
   */
  fun listPersistedBrushes(): List<PersistedBrushInfo> {
    val validBrushFamilyCount = storage.getBrushFamilyCount(activeConversation.uuid)
    if (validBrushFamilyCount == 0) return emptyList()
    val results = mutableListOf<PersistedBrushInfo>()

    for (i in 1..validBrushFamilyCount) {
      val decoded = getBrushFamily(i)
      val displayTitle =
        decoded?.clientBrushFamilyId?.takeIf { it.isNotEmpty() } ?: extractPromptFallback(i)
      results.add(PersistedBrushInfo(i, displayTitle))
    }
    return results.reversed() // So they print with the latest brush first.
  }

  fun log(message: String) {
    val uuid = if (::activeConversation.isInitialized) activeConversation.uuid else LOG_SESSION_ONLY
    storage.log(uuid, message)
  }

  fun logAndPrint(message: String) {
    log(message)
    println(message)
  }

  fun getBrushFamily(index: Int?): BrushFamily? {
    val bytes = storage.getBrushFamilyBytes(activeConversation.uuid, index) ?: return null
    return try {
      BrushFamily.decode(bytes)
    } catch (e: Exception) {
      logAndPrint("WARNING: Failed to decode brush family $index: ${e.message}")
      null
    }
  }

  private fun loadSystemPrompt(): String {
    return Harness::class
      .java
      .getResourceAsStream("/com/example/cahier/aibd/prompt/prompt.md")
      ?.let { resourceStream -> resourceStream.bufferedReader().use { it.readText() } }
      ?: run {
        logAndPrint(
          "WARNING: system prompt resource not found on classpath, no system instructions will " +
            "be used."
        )
        ""
      }
  }

  /**
   * Extracts the user prompt which led to the creation of a brush from the conversation history, as
   * a fallback for when Gemini fails to provide a description of the brush created.
   */
  private fun extractPromptFallback(turnIndex: Int): String {
    val userPrompts =
      activeConversation.chat
        .getHistory(true)
        .asSequence()
        .filter { it.role().orElse("") == "user" }
        .map { content ->
          content.parts().getOrNull()?.firstNotNullOfOrNull { it.text().getOrNull() } ?: ""
        }
        .filter { !it.trim().startsWith("Validation failed:") }
        .toList()
    return userPrompts.getOrNull(turnIndex - 1)?.lines()?.firstOrNull()?.take(40)
      ?: return "Brush design prompt $turnIndex"
  }
}
