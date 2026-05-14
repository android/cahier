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

package com.example.cahier.features.drawing

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import coil3.compose.AsyncImage
import com.example.cahier.core.ui.LocalTextureStore

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingDetailThumbnail(
    strokes: List<Stroke>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundImageUri: String? = null,
) {
    val textureStore = LocalTextureStore.current
    val cacheGen by textureStore.generation.collectAsState()
    val canvasStrokeRenderer = remember(cacheGen) {
        CanvasStrokeRenderer.create(textureStore)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                onClick = onClick,
                role = Role.Button,
            ),
    ) {
        backgroundImageUri?.let {
            AsyncImage(
                model = it,
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            val canvas = drawContext.canvas.nativeCanvas
            strokes.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = Matrix(),
                )
            }
        }
    }
}

@Preview
@Composable
fun DrawingDetailThumbnailPreview(
    modifier: Modifier = Modifier,
) {
    DrawingDetailThumbnail(
        modifier = modifier
            .fillMaxSize(),
        strokes = emptyList(),
        onClick = {},
        backgroundImageUri = null,
    )
}