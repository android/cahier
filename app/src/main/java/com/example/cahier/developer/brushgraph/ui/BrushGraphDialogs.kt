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
package com.example.cahier.developer.brushgraph.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R

@Composable
fun NameTextureDialog(
    show: Boolean,
    textureNameInput: String,
    onTextureNameInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_name_texture)) },
            text = {
                OutlinedTextField(
                    value = textureNameInput,
                    onValueChange = onTextureNameInputChange,
                    label = { Text(stringResource(R.string.bg_texture_id)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.bg_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_cancel))
                }
            }
        )
    }
}

@Composable
fun SaveToPaletteDialog(
    show: Boolean,
    paletteBrushNameInput: String,
    onPaletteBrushNameInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_save_to_cahier)) },
            text = {
                OutlinedTextField(
                    value = paletteBrushNameInput,
                    onValueChange = onPaletteBrushNameInputChange,
                    label = { Text(stringResource(R.string.bg_brush_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_cancel))
                }
            }
        )
    }
}

@Composable
fun ClearGraphConfirmationDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_clear_graph)) },
            text = {
                Text(stringResource(R.string.bg_clear_graph_confirmation))
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_cancel))
                }
            }
        )
    }
}

@Composable
fun TutorialWarningDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_start_tutorial)) },
            text = {
                Text(stringResource(R.string.bg_start_tutorial_message))
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.bg_start))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_cancel))
                }
            }
        )
    }
}

@Composable
fun TutorialFinishDialog(
    show: Boolean,
    onKeepChanges: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_exit_tutorial)) },
            text = {
                Text(stringResource(R.string.bg_exit_tutorial_message))
            },
            confirmButton = {
                Button(onClick = onKeepChanges) {
                    Text(stringResource(R.string.bg_keep_tutorial_brush))
                }
            },
            dismissButton = {
                Button(onClick = onRestoreOriginal) {
                    Text(stringResource(R.string.bg_restore_original_brush))
                }
            }
        )
    }
}

@Composable
fun OptionsDialog(
    show: Boolean,
    textFieldsLocked: Boolean,
    onToggleTextFieldsLocked: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_options)) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            stringResource(R.string.bg_lock_text_fields),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = textFieldsLocked,
                            onCheckedChange = { onToggleTextFieldsLocked() }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_ok))
                }
            }
        )
    }
}

@Composable
fun ReorganizeConfirmationDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (show) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_reorganize_graph)) },
            text = {
                Text(stringResource(R.string.bg_reorganize_graph_confirmation))
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text(stringResource(R.string.bg_reorganize))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.bg_cancel))
                }
            }
        )
    }
}

@Composable
internal fun TooltipDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.bg_ok))
            }
        }
    )
}

@Composable
internal fun FieldWithTooltip(
    tooltipTitle: String,
    tooltipText: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var showTooltip by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
        IconButton(onClick = { showTooltip = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Help,
                contentDescription = stringResource(R.string.bg_cd_help)
            )
        }
    }
    if (showTooltip) {
        TooltipDialog(
            title = tooltipTitle,
            text = tooltipText,
            onDismiss = { showTooltip = false }
        )
    }
}
