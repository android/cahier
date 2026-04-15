@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.inspectors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.NodeData

/** Shows connection details between two nodes and allows deletion. */
@Composable
fun EdgeInspector(
  edge: GraphEdge,
  fromNode: GraphNode,
  toNode: GraphNode,
  onNodeFocus: (String) -> Unit,
  onDisableChange: (Boolean) -> Unit,
  onDelete: () -> Unit,
  onAddNodeBetween: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text("Delete Edge") },
      text = { Text("Are you sure you want to delete this edge?") },
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
    // From Node Section
    EdgeNodeInfo(label = "From", node = fromNode, onClick = { onNodeFocus(fromNode.id) })
    if (fromNode.data is NodeData.Behavior && toNode.data is NodeData.Behavior) {
      Spacer(Modifier.height(8.dp))
      Button(
        onClick = { onAddNodeBetween() },
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text("Add Node Between")
      }
      Spacer(Modifier.height(8.dp))
    } else {
      Spacer(Modifier.height(16.dp))
    }

    // To Node Section
    EdgeNodeInfo(
      label = "To",
      node = toNode,
      inputIndex = edge.toPort.index,
      onClick = { onNodeFocus(toNode.id) },
    )

    Spacer(modifier = Modifier.weight(1f))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
      Checkbox(
        checked = edge.isDisabled,
        onCheckedChange = { checked ->
          onDisableChange(checked)
        }
      )
      Spacer(Modifier.width(8.dp))
      Text("Disable edge")
    }
    Spacer(Modifier.height(8.dp))

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

@Composable
private fun EdgeNodeInfo(
  label: String,
  node: GraphNode,
  inputIndex: Int? = null,
  onClick: () -> Unit,
) {
  val title = node.data.title()
  val subtitles = node.data.subtitles()
  val inputLabel = if (inputIndex != null) node.data.inputLabels().getOrNull(inputIndex) else null

  Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    for (subtitle in subtitles) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (inputLabel != null) {
      Text(
        text = "Input: $inputLabel",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
