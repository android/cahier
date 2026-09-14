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

import com.google.genai.Chat as SdkChat
import com.google.genai.ChatResumer
import com.google.genai.Chats as SdkChats
import com.google.genai.Client as SdkClient
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse

interface Chat {
  fun sendMessage(text: String): GenerateContentResponse

  fun sendMessage(content: Content): GenerateContentResponse

  fun getHistory(curated: Boolean): List<Content>

  fun restoreHistory(comprehensive: List<Content>, curated: List<Content>)
}

interface Chats {
  fun create(model: String, config: GenerateContentConfig): Chat
}

interface Client {
  val chats: Chats
}

class RealChat(private val sdkChat: SdkChat) : Chat {
  override fun sendMessage(text: String) = sdkChat.sendMessage(text)

  override fun sendMessage(content: Content) = sdkChat.sendMessage(content)

  override fun getHistory(curated: Boolean) = sdkChat.getHistory(curated)

  override fun restoreHistory(comprehensive: List<Content>, curated: List<Content>) {
    ChatResumer.restoreHistory(sdkChat, comprehensive, curated)
  }
}

class RealChats(private val sdkChats: SdkChats) : Chats {
  override fun create(model: String, config: GenerateContentConfig): Chat =
    RealChat(sdkChats.create(model, config))
}

class RealClient(private val sdkClient: SdkClient) : Client {
  override val chats: Chats = RealChats(sdkClient.chats)
}
