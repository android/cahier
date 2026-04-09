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

package com.example.cahier.developer.brushdesigner.ui

import androidx.annotation.StringRes
import com.example.cahier.R

/**
 * Represents the three tabs in the Brush Designer controls' pane.
 */
enum class BrushDesignerTab(@param:StringRes val labelResId: Int) {
    TipShape(R.string.brush_designer_tab_tip_shape),
    Paint(R.string.brush_designer_tab_paint),
    Behaviors(R.string.brush_designer_tab_behaviors);
}
