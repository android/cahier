package com.example.cahier.ui.brushgraph.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R

@Composable
fun NameTextureDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    textureNameInput: String,
    onTextureNameInputChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
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
    onDismiss: () -> Unit,
    paletteBrushNameInput: String,
    onPaletteBrushNameInputChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_save_to_palette)) },
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
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
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
    onDismiss: () -> Unit,
    onKeepChanges: () -> Unit,
    onRestoreOriginal: () -> Unit
) {
    if (show) {
        AlertDialog(
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
    onDismiss: () -> Unit,
    textFieldsLocked: Boolean,
    onToggleTextFieldsLocked: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.bg_options)) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.bg_lock_text_fields), modifier = Modifier.weight(1f))
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
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AlertDialog(
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
