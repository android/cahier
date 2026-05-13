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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withSave
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import com.example.cahier.core.ui.LocalTextureStore

/**
 * Renders a live stroke preview of the current brush — a Z-shaped squiggle
 * that shows how the brush actually paints (tip shape, texture, behaviors).
 *
 * Re-renders whenever the [brush] or [textureStore] changes.
 */
@Composable
internal fun TipPreview(
    brush: Brush?,
    modifier: Modifier = Modifier,
) {
    val textureStore = LocalTextureStore.current
    val cacheGen by textureStore.generation.collectAsState()
    val strokeRenderer = remember(cacheGen) {
        CanvasStrokeRenderer.create(textureStore)
    }

    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (brush == null) return@Canvas

            val w = size.width
            val h = size.height

            drawLine(
                color = gridColor,
                start = Offset(w / 2, 0f),
                end = Offset(w / 2, h),
                strokeWidth = 0.5f
            )
            drawLine(
                color = gridColor,
                start = Offset(0f, h / 2),
                end = Offset(w, h / 2),
                strokeWidth = 0.5f
            )

            val inputBatch = MutableStrokeInputBatch()
            val points = listOf(
                Triple(w * 0.1f, h * 0.3f, 0L),
                Triple(w * 0.25f, h * 0.25f, 10L),
                Triple(w * 0.45f, h * 0.2f, 20L),
                Triple(w * 0.55f, h * 0.35f, 30L),
                Triple(w * 0.5f, h * 0.5f, 40L),
                Triple(w * 0.45f, h * 0.65f, 50L),
                Triple(w * 0.55f, h * 0.8f, 60L),
                Triple(w * 0.75f, h * 0.75f, 70L),
                Triple(w * 0.9f, h * 0.7f, 80L),
            )

            points.forEachIndexed { index, (x, y, timeMs) ->
                inputBatch.add(
                    type = InputToolType.STYLUS,
                    x = x,
                    y = y,
                    elapsedTimeMillis = timeMs,
                    pressure = 0.5f + (index.toFloat() / points.size) * 0.3f,
                    tiltRadians = 0.2f + (index.toFloat() / points.size) * 0.4f,
                    orientationRadians = (index.toFloat() / points.size) * 1.5f
                )
            }

            val stroke = Stroke(brush = brush, inputs = inputBatch)

            val nativeCanvas = drawContext.canvas.nativeCanvas
            nativeCanvas.withSave {
                strokeRenderer.draw(
                    stroke = stroke,
                    canvas = this,
                    strokeToScreenTransform = android.graphics.Matrix()
                )
            }
        }
    }
}
