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
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part

class FakeChat(
  var scriptedResponses: List<GenerateContentResponse> = emptyList(),
  var history: MutableList<Content> = mutableListOf(),
  var shouldThrowError: Boolean = false,
) : Chat {
  var responseIndex = 0
  val sentRequests = mutableListOf<Any>()

  override fun sendMessage(text: String): GenerateContentResponse {
    sentRequests.add(text)
    // Simulate SDK adding this to chat history
    val part = Part.builder().text(text).build()
    val userTurn = Content.builder().parts(listOf(part)).role("user").build()
    history.add(userTurn)

    if (responseIndex < scriptedResponses.size) {
      return scriptedResponses[responseIndex++]
    }
    throw IllegalStateException("Unexpected prompt: $text (No more scripted responses)")
  }

  override fun sendMessage(content: Content): GenerateContentResponse {
    sentRequests.add(content)
    if (shouldThrowError) {
      throw RuntimeException("Simulated GenAI Error")
    }
    history.add(content)
    if (responseIndex < scriptedResponses.size) {
      return scriptedResponses[responseIndex++]
    }
    throw IllegalStateException("Unexpected prompt: $content (No more scripted responses)")
  }

  override fun getHistory(curated: Boolean): List<Content> = history

  override fun restoreHistory(comprehensive: List<Content>, curated: List<Content>) {
    history = comprehensive.toMutableList()
  }
}

class FakeChats(val fakeChat: FakeChat) : Chats {
  override fun create(model: String, config: GenerateContentConfig): Chat = fakeChat
}

class FakeClient(val fakeChats: FakeChats) : Client {
  override val chats: Chats = fakeChats
}
