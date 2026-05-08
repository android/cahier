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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.cahier.R
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.core.ui.DrawingSurface
import com.example.cahier.core.ui.theme.BrushBlack
import com.example.cahier.core.ui.theme.BrushBlue
import com.example.cahier.core.ui.theme.BrushGreen
import com.example.cahier.core.ui.theme.BrushRed
import com.example.cahier.core.ui.theme.BrushYellow
import ink.proto.BrushFamily as ProtoBrushFamily

/**
 * The drawing preview pane where users can test/preview their custom brush.
 * Includes the canvas, size selector, and color picker controls.
 *
 * Stateless: receives all state and callbacks, does not access ViewModel.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@Composable
internal fun PreviewPane(
    modifier: Modifier = Modifier,
    activeBrush: Brush?,
    activeProto: ProtoBrushFamily,
    strokes: List<Stroke>,
    brushColor: Color,
    brushSize: Float,
    onReplaceStrokes: (List<Stroke>) -> Unit,
    onStrokesFinished: (List<Stroke>) -> Unit,
    onGetNextBrush: () -> Brush,
    onSetBrushColor: (Color) -> Unit,
    onSetBrushSize: (Float) -> Unit
) {
    val context = LocalContext.current
    val textureStore = LocalTextureStore.current

    val canvasStrokeRenderer = remember(textureStore) {
        CanvasStrokeRenderer.create(textureStore = textureStore)
    }
    val localStrokes = remember { mutableStateListOf<Stroke>() }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(strokes) {
        if (localStrokes != strokes) {
            localStrokes.clear()
            localStrokes.addAll(strokes)
        }
    }

    LaunchedEffect(activeBrush) {
        if (activeBrush != null && localStrokes.isNotEmpty()) {
            val updatedStrokes = localStrokes.map { it.copy(brush = activeBrush) }
            localStrokes.clear()
            localStrokes.addAll(updatedStrokes)
            onReplaceStrokes(updatedStrokes)
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp, MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (showCustomColorPicker) {
            CustomColorPickerDialog(
                initialColor = brushColor,
                onColorSelected = onSetBrushColor,
                onDismissRequest = { showCustomColorPicker = false }
            )
        }

        if (activeBrush != null) {
            DrawingSurface(
                strokes = localStrokes,
                canvasStrokeRenderer = canvasStrokeRenderer,
                textureStore = textureStore,
                onStrokesFinished = { newStrokes ->
                    localStrokes.addAll(newStrokes)
                    onStrokesFinished(newStrokes)
                },
                onErase = { _, _ -> },
                onEraseStart = { },
                onEraseEnd = { },
                currentBrush = activeBrush,
                onGetNextBrush = onGetNextBrush,
                isEraserMode = false,
                backgroundImageUri = null,
                onStartDrag = {},
                modifier = Modifier.fillMaxSize()
            )

            PreviewToolbar(
                brushSize = brushSize,
                brushColor = brushColor,
                onSizeSelected = onSetBrushSize,
                onColorSelected = onSetBrushColor,
                onShowCustomColorPicker = { showCustomColorPicker = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        } else {
            if (activeProto.coatsCount == 0) {
                Text(
                    text = stringResource(R.string.brush_designer_invalid_brush),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Toolbar overlay with brush size and color selectors.
 */
@Composable
private fun PreviewToolbar(
    brushSize: Float,
    brushColor: Color,
    onSizeSelected: (Float) -> Unit,
    onColorSelected: (Color) -> Unit,
    onShowCustomColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SizeSelector(
            brushSize = brushSize,
            onSizeSelected = onSizeSelected
        )

        VerticalDivider(modifier = Modifier.height(24.dp))

        ColorSelector(
            brushColor = brushColor,
            onColorSelected = onColorSelected,
            onShowCustomColorPicker = onShowCustomColorPicker
        )
    }
}

@Composable
private fun SizeSelector(
    brushSize: Float,
    onSizeSelected: (Float) -> Unit
) {
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { sizeMenuExpanded = true }) {
            Text(
                stringResource(
                    id = R.string.brush_designer_size_px, brushSize.toInt()
                ),
                fontWeight = FontWeight.Bold
            )
        }
        DropdownMenu(
            expanded = sizeMenuExpanded,
            onDismissRequest = { sizeMenuExpanded = false }
        ) {
            listOf(2f, 5f, 10f, 15f, 25f, 50f, 100f).forEach { size ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.brush_designer_size_px, size.toInt())) },
                    onClick = {
                        onSizeSelected(size)
                        sizeMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSelector(
    brushColor: Color,
    onColorSelected: (Color) -> Unit,
    onShowCustomColorPicker: () -> Unit
) {
    var colorMenuExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { colorMenuExpanded = true }) {
            Icon(
                painterResource(R.drawable.circle_24px),
                contentDescription = stringResource(R.string.color),
                tint = brushColor
            )
        }
        DropdownMenu(
            expanded = colorMenuExpanded,
            onDismissRequest = { colorMenuExpanded = false }
        ) {
            val colors = mapOf(
                stringResource(R.string.brush_designer_color_black) to BrushBlack,
                stringResource(R.string.brush_designer_color_red) to BrushRed,
                stringResource(R.string.brush_designer_color_blue) to BrushBlue,
                stringResource(R.string.brush_designer_color_green) to BrushGreen,
                stringResource(R.string.brush_designer_color_yellow) to BrushYellow
            )
            colors.forEach { (name, color) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.circle_24px),
                            contentDescription = name,
                            tint = color
                        )
                    },
                    onClick = {
                        onColorSelected(color)
                        colorMenuExpanded = false
                    }
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_custom_color)) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.circle_24px),
                        contentDescription = stringResource(R.string.brush_designer_custom_color),
                        tint = brushColor
                    )
                },
                onClick = {
                    colorMenuExpanded = false
                    onShowCustomColorPicker()
                }
            )
        }
    }
}
