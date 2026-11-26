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

package com.example.cahier.core.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.cahier.core.data.CahierTextureBitmapStore

/**
 * Provides the [CahierTextureBitmapStore] for brush textures.
 *
 * In Compose previews ([LocalInspectionMode]), this falls back to a default store so `@Preview`
 * composables can render without wrapping in a provider; at runtime, the app still fails fast with
 * `"No TextureStore provided"` if nothing provides a store.
 */
val LocalTextureStore: ProvidableCompositionLocal<CahierTextureBitmapStore> =
  compositionLocalWithComputedDefaultOf<CahierTextureBitmapStore> {
    if (LocalInspectionMode.currentValue) {
      CahierTextureBitmapStore(LocalContext.currentValue)
    } else {
      error("No TextureStore provided")
    }
  }
