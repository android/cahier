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

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.encode
import com.google.common.truth.Truth.assertThat
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HarnessTest {

  @get:Rule val tempFolder = TemporaryFolder()

  class FakeImageConverter : ImagePayloadConverter<ByteArray> {
    override fun convert(payload: ByteArray): Part {
      return Part.builder().text("asdf").build()
    }
  }

  class FakeDeployer : BrushFamilyDeployer {
    override fun deploy(
      format: BrushFamilyInputFormat,
      targetPath: String,
      log: (String) -> Unit,
    ) {}
  }

  @Test
  fun sendText_returnsExpectedResponseAndUpdatesTitle() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root")
    val storage = DesktopStorage(testBaseDir)

    val fakeMainResponseJson =
      """{
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "client_brush_family_id: \"Concise Title\""
                  }
                ],
                "role": "model"
              }
            }
          ]
        }"""

    val fakeMainResponse =
      GenerateContentResponse.fromJsonString(
        fakeMainResponseJson,
        GenerateContentResponse::class.java,
      )

    val fakeChat = FakeChat(scriptedResponses = listOf(fakeMainResponse))
    val fakeClient = FakeClient(FakeChats(fakeChat))

    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    val response = harness.sendText("Hi")

    assertThat(response).isEqualTo("client_brush_family_id: \"Concise Title\"")

    assertThat(fakeChat.sentRequests).hasSize(1)
    assertThat(fakeChat.sentRequests[0].toString()).contains("Hi")
  }

  @Test
  fun sendText_followUpTurnsIncludeHistoryAndSkipSystemPrompt() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_followup")
    val storage = DesktopStorage(testBaseDir)

    val fakeResponse1Json =
      """{
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "client_brush_family_id: 'Title 1'"
                  }
                ],
                "role": "model"
              }
            }
          ]
        }"""
    val fakeResponse2Json =
      """{
          "candidates": [
            {
              "content": {
                "parts": [
                  {
                    "text": "client_brush_family_id: 'Title 2'"
                  }
                ],
                "role": "model"
              }
            }
          ]
        }"""

    val fakes =
      listOf(fakeResponse1Json, fakeResponse2Json).map {
        GenerateContentResponse.fromJsonString(it, GenerateContentResponse::class.java)
      }

    val fakeChat = FakeChat(scriptedResponses = fakes)
    val fakeClient = FakeClient(FakeChats(fakeChat))

    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    // Turn 1
    val resp1 = harness.sendText("Prompt 1")
    assertThat(resp1).isEqualTo("client_brush_family_id: 'Title 1'")
    assertThat(fakeChat.sentRequests).hasSize(1)

    // Turn 2
    val resp2 = harness.sendText("Prompt 2")
    assertThat(resp2).isEqualTo("client_brush_family_id: 'Title 2'")
    assertThat(fakeChat.sentRequests).hasSize(2)
    assertThat(fakeChat.sentRequests[1].toString()).isEqualTo("Prompt 2")
  }

  @Test
  fun forkConversation_createsForkedConversation() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_fork_harness")
    val storage = DesktopStorage(testBaseDir)

    val fakeResponse =
      GenerateContentResponse.fromJsonString(
        """{
            "candidates": [
              {
                "content": {
                  "parts": [
                    {
                      "text": "Sample"
                    }
                  ],
                  "role": "model"
                }
              }
            ]
          }""",
        GenerateContentResponse::class.java,
      )

    val fakeChat = FakeChat(scriptedResponses = listOf(fakeResponse))
    val fakeClient = FakeClient(FakeChats(fakeChat))

    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    val src = harness.activeConversation
    harness.forkConversation("Forked Chat")

    val destUuid = harness.activeConversation.uuid
    assertThat(destUuid).isNotEqualTo(src.uuid)
    assertThat(harness.activeConversation.manifest.timestamp).isGreaterThan(src.manifest.timestamp)
    assertThat(harness.activeConversation.manifest.title).isEqualTo("Forked Chat")
  }

  @Test
  fun deployWithRetries_successOnFirstTry() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_deploy_success")
    val storage = DesktopStorage(testBaseDir)

    var deployCount = 0
    var retryCount = 0

    class SuccessDeployer : BrushFamilyDeployer {
      override fun deploy(
        format: BrushFamilyInputFormat,
        targetPath: String,
        log: (String) -> Unit,
      ) {
        deployCount++
      }
    }

    val fakeClient = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList())))
    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = SuccessDeployer(),
      )

    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")
    // Not actually running deploy_brush in this test, so we need to manually create the
    // .brushfamily file to simulate the result of a successful deployment, which would trigger the
    // promotion.
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03))

    harness.deployWithRetries { _ ->
      retryCount++
      "Retry reply"
    }

    assertThat(deployCount).isEqualTo(1)
    assertThat(retryCount).isEqualTo(0)
    assertThat(storage.getBrushFamilyCount(harness.activeConversation.uuid)).isEqualTo(1)
  }

  @Test
  fun deployWithRetries_failsValidationOnceThenSucceeds() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_deploy_retry")
    val storage = DesktopStorage(testBaseDir)

    var deployCount = 0
    var retryCount = 0

    class FailOnceDeployer : BrushFamilyDeployer {
      override fun deploy(
        format: BrushFamilyInputFormat,
        targetPath: String,
        log: (String) -> Unit,
      ) {
        deployCount++
        if (deployCount == 1) {
          throw ValidationException("Validation failed asdf")
        }
      }
    }

    val fakeClient = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList())))
    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FailOnceDeployer(),
      )

    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")
    // Not actually running deploy_brush in this test, so we need to manually create the
    // .brushfamily file to simulate the result of a successful deployment, which would trigger the
    // promotion.
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(byteArrayOf(0x01, 0x02, 0x03))

    harness.deployWithRetries { _ ->
      retryCount++
      "Retry reply"
    }

    assertThat(deployCount).isEqualTo(2)
    assertThat(retryCount).isEqualTo(1)
    assertThat(storage.getBrushFamilyCount(harness.activeConversation.uuid)).isEqualTo(1)
  }

  @Test
  fun deployWithRetries_failsValidationRepeatedlyAndGivesUp() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_deploy_giveup")
    val storage = DesktopStorage(testBaseDir)

    var deployCount = 0
    var retryCount = 0

    val fakeClient = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList())))

    class FailDeployer : BrushFamilyDeployer {
      override fun deploy(
        format: BrushFamilyInputFormat,
        targetPath: String,
        log: (String) -> Unit,
      ) {
        deployCount++
        throw ValidationException("Validation failed asdf")
      }
    }

    val harness =
      Harness(
        client = fakeClient,
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FailDeployer(),
      )

    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")

    harness.deployWithRetries { _ ->
      retryCount++
      "Retry reply"
    }

    assertThat(deployCount).isEqualTo(4)
    assertThat(retryCount).isEqualTo(3)
    assertThat(storage.getBrushFamilyCount(harness.activeConversation.uuid)).isEqualTo(0)
  }

  @Test
  fun createAndReloadConversation_manifestSaved() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    harness.createNewConversation(title = "Test Brush")

    val reloaded = harness.loadConversation(harness.activeConversation.uuid)
    assertThat(reloaded).isNotNull()
    assertThat(reloaded!!.manifest.title).isEqualTo("Test Brush")
  }

  @Test
  fun initializeActiveConversation_recoversFromCorruptedManifest() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_corrupt")
    val storage = DesktopStorage(testBaseDir)

    // Fake an active session with garbage in manifest
    storage.setActiveConversationId("corrupted-uuid")
    val manifestFile = File(testBaseDir, "conversations/corrupted-uuid/manifest.json")
    manifestFile.parentFile?.mkdirs()
    manifestFile.writeText("{ invalid json: [")

    // History file exists
    val historyFile = File(testBaseDir, "conversations/corrupted-uuid/history.json")
    historyFile.writeText("[]")

    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    // Harness should recover and create a new conversation, leaving the corrupted one not active.
    assertThat(harness.activeConversation.uuid).isNotEqualTo("corrupted-uuid")
  }

  @Test
  fun listPersistedBrushes_extractsDescriptionsCorrectly() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_summary")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    val customPrompt = "This is my custom brush description that should be extracted."
    val simplePrompt = "Basic prompt"

    val part1 = Part.builder().text(customPrompt).build()
    val part2 = Part.builder().text(simplePrompt).build()

    val userTurn1 = Content.builder().parts(listOf(part1)).role("user").build()
    val userTurn2 = Content.builder().parts(listOf(part2)).role("user").build()

    harness.storage.saveHistory(harness.activeConversation.uuid, listOf(userTurn1, userTurn2))
    // Load the conversation to get the history saved on disk into memory in the Chat.
    val unused = harness.loadConversation(harness.activeConversation.uuid)

    // Simulate deployed brushes with expected descriptions
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile1 =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/1.brushfamily")
    binaryFile1.parentFile?.mkdirs()
    binaryFile1.writeBytes(brushFamilyBytes)
    val binaryFile2 =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/2.brushfamily")
    binaryFile2.parentFile?.mkdirs()
    binaryFile2.writeBytes(brushFamilyBytes)

    val brushes = harness.listPersistedBrushes()

    assertThat(brushes).hasSize(2)
    // Results are reversed (newest first)
    assertThat(brushes[0].title).isEqualTo("Basic prompt")
    assertThat(brushes[1].title)
      .isEqualTo("This is my custom brush description that") // Clipped to 40
  }

  @Test
  fun listAvailableConversations_sortsDescendingAndHandlesEmptyTitles() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_list")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    harness.createNewConversation(title = "Second")

    val list = harness.listAvailableConversations()
    assertThat(list).hasSize(2)
    // Newest first
    assertThat(list[0].second.title).isEqualTo("Second")
    assertThat(list[1].second.title).isEqualTo("Untitled Conversation")
  }

  @Test
  fun promoteStagedBrushFamily_titleSourceUnspecified_setsAutoFallback() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_promotion")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    harness.createNewConversation(title = "")
    val promptContent =
      Content.builder()
        .parts(listOf(Part.builder().text("Draw a fluffy cat").build()))
        .role("user")
        .build()
    harness.storage.saveHistory(harness.activeConversation.uuid, listOf(promptContent))
    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")
    // Load the conversation to get the history saved on disk into memory in the Chat.
    val unused = harness.loadConversation(harness.activeConversation.uuid)

    // Not actually running deploy_brush in this test, so we need to manually create the
    // .brushfamily file to simulate the result of a successful deployment, which would trigger the
    // promotion.
    val brushFamilyBytes = BrushFamily.builder().build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    harness.deploy(null)

    assertThat(harness.activeConversation.manifest.title).isEqualTo("Draw a fluffy cat")
    assertThat(harness.activeConversation.manifest.titleSource)
      .isEqualTo(TitleSource.AUTO_PROMPT_FALLBACK)
  }

  @Test
  fun promoteStagedBrushFamily_titleSourceUnspecified_setsClientBrushFamilyId() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_promotion")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    harness.createNewConversation(title = "")
    val promptContent =
      Content.builder()
        .parts(listOf(Part.builder().text("Draw a fluffy cat").build()))
        .role("user")
        .build()
    harness.storage.saveHistory(harness.activeConversation.uuid, listOf(promptContent))
    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")
    // Load the conversation to get the history saved on disk into memory in the Chat.
    val unused = harness.loadConversation(harness.activeConversation.uuid)

    // Not actually running deploy_brush in this test, so we need to manually create the
    // .brushfamily file to simulate the result of a successful deployment, which would trigger the
    // promotion.
    @OptIn(ExperimentalInkCustomBrushApi::class)
    val brushFamilyBytes =
      BrushFamily.builder().setClientBrushFamilyId("cat-brush").build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    harness.deploy(null)

    assertThat(harness.activeConversation.manifest.title).isEqualTo("cat-brush")
    assertThat(harness.activeConversation.manifest.titleSource).isEqualTo(TitleSource.AUTO_GEMINI)
  }

  @Test
  fun promoteStagedBrushFamily_titleSourceManual_retainsManualTitle() {
    val testBaseDir: File = tempFolder.newFolder("ink_test_root_promotion_manual")
    val storage = DesktopStorage(testBaseDir)
    val harness =
      Harness(
        client = FakeClient(FakeChats(FakeChat(scriptedResponses = emptyList()))),
        modelName = "fake-model",
        storage = storage,
        imageConverter = FakeImageConverter(),
        deployer = FakeDeployer(),
      )

    harness.createNewConversation(title = "My Manual Title")
    val promptContent =
      Content.builder()
        .parts(listOf(Part.builder().text("Should be ignored").build()))
        .role("user")
        .build()
    harness.storage.saveHistory(harness.activeConversation.uuid, listOf(promptContent))
    harness.storage.stageBrushFamilyContent(harness.activeConversation.uuid, "asdf")
    // Load the conversation to get the history saved on disk into memory in the Chat.
    val unused = harness.loadConversation(harness.activeConversation.uuid)

    // Not actually running deploy_brush in this test, so we need to manually create the
    // .brushfamily file to simulate the result of a successful deployment, which would trigger the
    // promotion.
    @OptIn(ExperimentalInkCustomBrushApi::class)
    val brushFamilyBytes =
      BrushFamily.builder().setClientBrushFamilyId("should-be-ignored").build().encode()
    val binaryFile =
      File(testBaseDir, "conversations/${harness.activeConversation.uuid}/new.brushfamily")
    binaryFile.parentFile?.mkdirs()
    binaryFile.writeBytes(brushFamilyBytes)

    harness.deploy(null)

    assertThat(harness.activeConversation.manifest.title).isEqualTo("My Manual Title")
    assertThat(harness.activeConversation.manifest.titleSource)
      .isEqualTo(TitleSource.USER_SPECIFIED)
  }
}
