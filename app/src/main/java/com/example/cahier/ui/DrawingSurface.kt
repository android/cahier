/*
 *
 *  * Copyright 2025 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.example.cahier.ui

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.withSave
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import coil3.compose.AsyncImage
import com.example.cahier.utils.pointerInputWithSiblingFallthrough

@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onErase: (offsetX: Float, offsetY: Float) -> Unit,
    onEraseStart: () -> Unit,
    onEraseEnd: () -> Unit,
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    isEraserMode: Boolean,
    backgroundImageUri: String?,
    onStartDrag: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        backgroundImageUri?.let {
            AsyncImage(
                model = it,
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            strokes.forEach { stroke ->
                canvas.withSave {
                    canvasStrokeRenderer.draw(
                        stroke = stroke,
                        canvas = this,
                        strokeToScreenTransform = Matrix()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInputWithSiblingFallthrough {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onStartDrag()
                        },
                        onDrag = { _, _ ->
                            // Consume events.
                        },
                        onDragEnd = {
                            // Do nothing.
                        }
                    )
                }
        )


        if (isEraserMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onEraseStart() },
                            onDragEnd = { onEraseEnd() }
                        ) { change, _ ->
                            onErase(change.position.x, change.position.y)
                            change.consume()
                        }
                    }
            )
        } else {
            InProgressStrokes(
                defaultBrush = currentBrush,
                nextBrush = onGetNextBrush,
                onStrokesFinished = onStrokesFinished,
            )
        }
    }
}