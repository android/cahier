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
import androidx.compose.ui.unit.dp

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
            title = { Text("Name Texture") },
            text = {
                OutlinedTextField(
                    value = textureNameInput,
                    onValueChange = onTextureNameInputChange,
                    label = { Text("Texture ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
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
            title = { Text("Save to Cahier Palette") },
            text = {
                OutlinedTextField(
                    value = paletteBrushNameInput,
                    onValueChange = onPaletteBrushNameInputChange,
                    label = { Text("Brush Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
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
            title = { Text("Clear Graph") },
            text = {
                Text("Are you sure you want to clear the entire brush graph? This action cannot be undone.")
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
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
            title = { Text("Start Tutorial") },
            text = {
                Text("Starting the tutorial will clear your current brush graph to start from scratch. Your current brush will be saved and restored when you exit the tutorial.")
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
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
            title = { Text("Exit Tutorial") },
            text = {
                Text("Do you want to keep the brush you built in the tutorial, or restore your original brush?")
            },
            confirmButton = {
                Button(onClick = onKeepChanges) {
                    Text("Keep Tutorial Brush")
                }
            },
            dismissButton = {
                Button(onClick = onRestoreOriginal) {
                    Text("Restore Original Brush")
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
            title = { Text("Options") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Lock text fields", modifier = Modifier.weight(1f))
                        Switch(
                            checked = textFieldsLocked,
                            onCheckedChange = { onToggleTextFieldsLocked() }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
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
            title = { Text("Reorganize Graph") },
            text = {
                Text("Are you sure you want to reorganize the graph? This will reset all node positions and expansion states. This action cannot be undone.")
            },
            confirmButton = {
                Button(onClick = onConfirm) {
                    Text("Reorganize")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
