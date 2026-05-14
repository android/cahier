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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import com.example.cahier.R
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.developer.brushdesigner.data.CustomBrushEntity
import com.example.cahier.features.drawing.CustomBrushes

/**
 * Top app bar for the Brush Designer screen with stock brush, palette,
 * save/import/export actions.
 *
 * Stateless: receives data and callbacks, does not access ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrushDesignerTopBar(
    isCompact: Boolean,
    onNavigateUp: () -> Unit,
    savedBrushes: List<CustomBrushEntity>,
    onLoadBrush: (BrushFamily) -> Unit,
    onLoadFromPalette: (CustomBrushEntity) -> Unit,
    onDeleteFromPalette: (String) -> Unit,
    onClearCanvas: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.brush_designer_title),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.brush_designer_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            BrushLibraryMenu(
                onLoadBrush = onLoadBrush
            )
            PaletteMenu(
                savedBrushes = savedBrushes,
                onLoadFromPalette = onLoadFromPalette,
                onDeleteFromPalette = onDeleteFromPalette
            )
            if (isCompact) {
                OverflowMenu(
                    onShowSaveDialog = onShowSaveDialog,
                    onClearCanvas = onClearCanvas,
                    onImport = onImport,
                    onExport = onExport
                )
            } else {
                TextButton(onClick = onShowSaveDialog) {
                    Text(stringResource(R.string.brush_designer_save_to_palette))
                }
                TextButton(onClick = onClearCanvas) {
                    Text(stringResource(R.string.clear))
                }
                TextButton(onClick = onImport) {
                    Text(stringResource(R.string.brush_designer_import))
                }
                TextButton(onClick = onExport) {
                    Text(stringResource(R.string.brush_designer_export))
                }
            }
        }
    )
}

@Composable
private fun BrushLibraryMenu(
    onLoadBrush: (BrushFamily) -> Unit,
) {
    val textureStore = LocalTextureStore.current
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val cahierBrushes = remember { CustomBrushes.getBrushes(context, textureStore) }

    val stockBrushes = remember {
        listOf(
            R.string.highlighter to StockBrushes.highlighter(),
            R.string.marker to StockBrushes.marker(),
            R.string.pressure_pen to StockBrushes.pressurePen(),
            R.string.dashed_line to StockBrushes.dashedLine(),
            R.string.emoji_highlighter to StockBrushes.emojiHighlighter("emoji-heart", showMiniEmojiTrail = true),
        )
    }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.brush_designer_brushes))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.brush_designer_section_stock),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {},
                enabled = false
            )
            stockBrushes.filter { it.first != R.string.emoji_highlighter }.forEach { (nameResId, brushFamily) ->
                DropdownMenuItem(
                    text = { Text(stringResource(nameResId)) },
                    onClick = {
                        onLoadBrush(brushFamily)
                        expanded = false
                    }
                )
            }
            
            var showEmojiSubMenu by remember { mutableStateOf(false) }
            var itemWidth by remember { mutableStateOf(0) }
            var itemHeight by remember { mutableStateOf(0) }
            val density = LocalDensity.current
            
            Box(modifier = Modifier.onSizeChanged { 
                itemWidth = it.width
                itemHeight = it.height
            }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.emoji_highlighter)) },
                    trailingIcon = {
                        Text(text = if (showEmojiSubMenu) "▶" else "▼")
                    },
                    onClick = { showEmojiSubMenu = true }
                )
                DropdownMenu(
                    expanded = showEmojiSubMenu,
                    onDismissRequest = { showEmojiSubMenu = false },
                    offset = DpOffset(x = density.run { itemWidth.toDp() }, y = density.run { -itemHeight.toDp() }),
                    properties = PopupProperties(focusable = true)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.emoji_heart)) },
                        onClick = {
                            onLoadBrush(StockBrushes.emojiHighlighter("emoji-heart", showMiniEmojiTrail = true))
                            showEmojiSubMenu = false
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.emoji_star)) },
                        onClick = {
                            onLoadBrush(StockBrushes.emojiHighlighter("emoji-star", showMiniEmojiTrail = true))
                            showEmojiSubMenu = false
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.emoji_poop)) },
                        onClick = {
                            onLoadBrush(StockBrushes.emojiHighlighter("emoji-poop", showMiniEmojiTrail = true))
                            showEmojiSubMenu = false
                            expanded = false
                        }
                    )
                }
            }

            if (cahierBrushes.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.brush_designer_section_cahier),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {},
                    enabled = false
                )
                cahierBrushes.forEach { customBrush ->
                    DropdownMenuItem(
                        text = { Text(customBrush.name) },
                        onClick = {
                            onLoadBrush(customBrush.brushFamily)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteMenu(
    savedBrushes: List<CustomBrushEntity>,
    onLoadFromPalette: (CustomBrushEntity) -> Unit,
    onDeleteFromPalette: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(R.string.brush_designer_my_palette))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (savedBrushes.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.brush_designer_no_saved_brushes)) },
                    onClick = { expanded = false }
                )
            } else {
                savedBrushes.forEach { entity ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(entity.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    onDeleteFromPalette(entity.name)
                                }) {
                                    Icon(
                                        painterResource(R.drawable.delete_24px),
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onLoadFromPalette(entity)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(
    onShowSaveDialog: () -> Unit,
    onClearCanvas: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painterResource(R.drawable.menu_24px),
                contentDescription = stringResource(R.string.brush_designer_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_save_to_palette)) },
                onClick = { onShowSaveDialog(); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clear)) },
                onClick = { onClearCanvas(); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_import)) },
                onClick = { onImport(); expanded = false }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_export)) },
                onClick = { onExport(); expanded = false }
            )
        }
    }
}

/**
 * Dialog for saving the current brush to the local palette with a name.
 *
 * Stateless: receives callbacks only.
 */
@Composable
internal fun SaveToPaletteDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var brushNameInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.brush_designer_save_dialog_title)) },
        text = {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.info_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.brush_designer_save_dialog_info),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                OutlinedTextField(
                    value = brushNameInput,
                    onValueChange = { brushNameInput = it },
                    label = { Text(stringResource(R.string.brush_designer_brush_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (brushNameInput.isNotBlank()) {
                    onSave(brushNameInput)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.brush_designer_cancel))
            }
        }
    )
}
