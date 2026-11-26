package com.example.cahier.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.cahier.R
import com.example.cahier.core.ui.theme.CahierAppTheme

@Composable
fun ConfirmationDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  title: String,
  text: String,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.confirm)) } },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) } },
    modifier = modifier,
  )
}

// Dialogs are drawn in their own window, so the preview needs an explicit size to show one.
@Preview(widthDp = 360, heightDp = 280)
@Composable
fun ConfirmationDialogPreview() {
  CahierAppTheme {
    ConfirmationDialog(
      onConfirm = {},
      onDismiss = {},
      title = "Confirm",
      text = "Confirm you want to perform this action.",
    )
  }
}
