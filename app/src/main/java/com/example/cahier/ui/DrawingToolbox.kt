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

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cahier.R
import com.example.cahier.data.CustomBrush
import com.example.cahier.ui.viewmodels.DrawingCanvasViewModel
import kotlinx.coroutines.launch

@Composable
fun DrawingToolbox(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExit: () -> Unit,
    isVertical: Boolean,
    onColorPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isVertical) {
            VerticalToolbox(
                drawingCanvasViewModel = drawingCanvasViewModel,
                imagePickerLauncher = imagePickerLauncher,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onExit = onExit,
                onColorPickerClick = onColorPickerClick
            )
        } else {
            HorizontalToolbox(
                drawingCanvasViewModel = drawingCanvasViewModel,
                imagePickerLauncher = imagePickerLauncher,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onExit = onExit,
                onColorPickerClick = onColorPickerClick
            )
        }
    }
}

@Composable
internal fun ToolboxBrushControls(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    isVertical: Boolean,
    onColorPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brushesMenuExpanded = rememberSaveable { mutableStateOf(false) }
    val sizeMenuExpanded = rememberSaveable { mutableStateOf(false) }
    val customBrushes by drawingCanvasViewModel.customBrushes.collectAsStateWithLifecycle()
    val isEraserMode by drawingCanvasViewModel.isEraserMode.collectAsStateWithLifecycle()

    if (isVertical) {
        Column(modifier = modifier) {
            ToolBoxContent(
                drawingCanvasViewModel = drawingCanvasViewModel,
                brushesMenuExpanded = brushesMenuExpanded,
                sizeMenuExpanded = sizeMenuExpanded,
                customBrushes = customBrushes,
                onColorPickerClick = onColorPickerClick,
                isEraserMode = isEraserMode,
            )
        }
    } else {
        Row(modifier = modifier) {
            ToolBoxContent(
                drawingCanvasViewModel = drawingCanvasViewModel,
                brushesMenuExpanded = brushesMenuExpanded,
                sizeMenuExpanded = sizeMenuExpanded,
                customBrushes = customBrushes,
                onColorPickerClick = onColorPickerClick,
                isEraserMode = isEraserMode,
            )
        }
    }
}

@Composable
private fun ToolBoxContent(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    brushesMenuExpanded: MutableState<Boolean>,
    sizeMenuExpanded: MutableState<Boolean>,
    customBrushes: List<CustomBrush>,
    onColorPickerClick: () -> Unit,
    isEraserMode: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        IconButton(
            onClick = { brushesMenuExpanded.value = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.brush_24px),
                contentDescription = stringResource(R.string.brush),
                modifier = Modifier.background(
                    color = if (isEraserMode)
                        Color.Transparent else MaterialTheme.colorScheme.inversePrimary,
                    shape = CircleShape
                )
            )
        }
        BrushesDropdownMenu(
            expanded = brushesMenuExpanded.value,
            onDismissRequest = { brushesMenuExpanded.value = false },
            onBrushChange = { newBrush ->
                drawingCanvasViewModel.changeBrush(newBrush)
                brushesMenuExpanded.value = false
            },
            customBrushes = customBrushes
        )
    }
    IconButton(
        onClick = {
            onColorPickerClick()
            drawingCanvasViewModel.setEraserMode(false)
        },
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.palette_24px),
            contentDescription = stringResource(R.string.color),
        )
    }
    Box {
        IconButton(
            onClick = { sizeMenuExpanded.value = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.line_weight_24px),
                contentDescription = stringResource(R.string.brush_size),
            )
        }
        SizeDropdownMenu(
            expanded = sizeMenuExpanded.value,
            onDismissRequest = { sizeMenuExpanded.value = false },
            onSizeChange = { newSize ->
                drawingCanvasViewModel.changeBrushSize(newSize)
                sizeMenuExpanded.value = false
            }
        )
    }
}

@Composable
internal fun ToolboxHistoryControls(
    onUndo: () -> Unit,
    canUndo: Boolean,
    onRedo: () -> Unit,
    canRedo: Boolean,
    drawingCanvasViewModel: DrawingCanvasViewModel,
    onClear: () -> Unit,
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    val content = @Composable {
        val isEraserMode by drawingCanvasViewModel.isEraserMode.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.undo_24px),
                contentDescription = stringResource(R.string.undo)
            )
        }
        IconButton(
            onClick = onRedo,
            enabled = canRedo,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.redo_24px),
                contentDescription = stringResource(R.string.redo)
            )
        }
        IconButton(
            onClick = {
                drawingCanvasViewModel.setEraserMode(true)
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ink_eraser_24px),
                contentDescription = stringResource(R.string.eraser),
                modifier = Modifier.background(
                    color = if (isEraserMode)
                        MaterialTheme.colorScheme.inversePrimary else Color.Transparent,
                )
            )
        }
        IconButton(
            onClick = { coroutineScope.launch { onClear() } },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.clear_all_24px),
                contentDescription = stringResource(R.string.clear)
            )
        }
    }
    if (isVertical) {
        Column(modifier = modifier) {
            content()
        }
    } else {
        Row(modifier = modifier) {
            content()
        }
    }
}

@Composable
internal fun ToolboxNoteActions(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onExit: () -> Unit,
    isVertical: Boolean,
    modifier: Modifier = Modifier
) {
    val content = @Composable {
        val uiState by drawingCanvasViewModel.uiState.collectAsStateWithLifecycle()
        var optionsMenuExpanded by rememberSaveable { mutableStateOf(false) }
        IconButton(
            onClick = { drawingCanvasViewModel.toggleFavorite() },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = if (uiState.note.isFavorite)
                    painterResource(R.drawable.favorite_24px_filled) else
                    painterResource(R.drawable.favorite_24px),
                contentDescription = if (uiState.note.isFavorite)
                    stringResource(R.string.unfavorite) else
                    stringResource(R.string.favorite),
                tint = if (uiState.note.isFavorite)
                    MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
        Spacer(modifier = Modifier.size(4.dp))
        IconButton(
            onClick = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.image_24px),
                contentDescription = stringResource(R.string.add_image)
            )
        }
        Box {
            IconButton(
                onClick = { optionsMenuExpanded = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.menu_24px),
                    contentDescription = stringResource(R.string.more_options)
                )
            }
            DropdownMenu(
                expanded = optionsMenuExpanded,
                onDismissRequest = { optionsMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.exit)) },
                    onClick = {
                        optionsMenuExpanded = false
                        onExit()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.exit_to_app_24px),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
    if (isVertical) {
        Column(modifier = modifier) {
            content()
        }
    } else {
        Row(modifier = modifier) {
            content()
        }
    }
}

@Composable
fun SizeDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizes = listOf(5f, 10f, 15f, 20f, 25f)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        modifier = modifier
    ) {
        sizes.forEach { size ->
            DropdownMenuItem(
                text = { Text(text = "${size.toInt()}px") },
                onClick = { onSizeChange(size) }
            )
        }
    }
}

@Composable
fun BrushesDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onBrushChange: (BrushFamily) -> Unit,
    customBrushes: List<CustomBrush>,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.pressure_pen)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.stylus_pen_24px),
                    contentDescription = stringResource(R.string.pressure_pen)
                )
            },
            onClick = { onBrushChange(StockBrushes.pressurePenLatest) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.marker)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ink_marker_24px),
                    contentDescription = stringResource(R.string.marker)
                )
            },
            onClick = { onBrushChange(StockBrushes.markerLatest) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.highlighter)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ink_highlighter_24px),
                    contentDescription = stringResource(R.string.highlighter)
                )
            },
            onClick = { onBrushChange(StockBrushes.highlighterLatest) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.dashed_line)) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.road_24px),
                    contentDescription = stringResource(R.string.dashed_line)
                )
            },
            onClick = { onBrushChange(StockBrushes.dashedLineLatest) }
        )
        if (customBrushes.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = stringResource(R.string.custom_brush),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            customBrushes.forEach { customBrush ->
                DropdownMenuItem(
                    text = { Text(text = customBrush.name) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(customBrush.icon),
                            contentDescription = customBrush.name
                        )
                    },
                    onClick = { onBrushChange(customBrush.brushFamily) }
                )
            }
        }
    }
}