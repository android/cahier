@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.inspectors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.ui.NodeFields

/** Renders the content of the node inspector. */
@Composable
fun NodeInspector(
  node: GraphNode,
  onUpdate: (NodeData) -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: androidx.ink.rendering.android.canvas.CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text("Delete Node") },
      text = { Text("Are you sure you want to delete this node and all its connections?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete()
            showDeleteConfirmation = false
          }
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
      },
    )
  }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    Box(modifier = Modifier.weight(1f)) {
      NodeFields(
        node = node,
        onUpdate = onUpdate,
        onChooseColor = onChooseColor,
        allTextureIds = allTextureIds,
        onLoadTexture = onLoadTexture,
        strokeRenderer = strokeRenderer,
        textFieldsLocked = textFieldsLocked,
      )
    }

    Spacer(Modifier.height(16.dp))

    Button(
      onClick = { showDeleteConfirmation = true },
      modifier = Modifier.fillMaxWidth().height(48.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
      Text("Delete")
    }
  }
}
