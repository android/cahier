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
package com.google.genai;

import com.google.genai.types.Content;
import java.util.List;

/**
 * Helper bridge to restore protected history fields of ChatBase.
 *
 * <p>This class lives in the com.google.genai package rather than its true package location to gain
 * access to protected fields of the GenAI SDK's Chat class, comprehensiveHistory and
 * curatedHistory. This is necessary because the current version of the Google GenAI Java SDK (as of
 * this writing) lacks a public API or constructor parameter to initialize a new Chat instance with
 * pre-existing conversation history. If the internal field names are renamed or refactored in a
 * future version of the SDK, this bridge class will need to be updated accordingly.
 */
public class ChatResumer {
  private ChatResumer() {}

  public static void restoreHistory(
      Chat chat, List<Content> comprehensiveHistory, List<Content> curatedHistory) {
    chat.comprehensiveHistory.clear();
    chat.comprehensiveHistory.addAll(comprehensiveHistory);
    chat.curatedHistory.clear();
    chat.curatedHistory.addAll(curatedHistory);
  }
}
