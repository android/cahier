@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.ui.asString
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.INPUT_ROW_HEIGHT
import com.example.cahier.developer.brushgraph.data.Port

@Composable
fun NodePortLabels(
  node: GraphNode,
  graph: BrushGraph,
  visiblePorts: List<Port>,
  isSelectionMode: Boolean,
  onPortClick: (String, Port) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  Box(modifier = modifier.fillMaxWidth()) {
    Column {
      for ((index, port) in visiblePorts.withIndex()) {
        with(density) {
          val isPortEmpty = graph.edges.none { it.toNodeId == node.id && it.toPortId == port.id }
          Box(
            modifier =
              Modifier.height(INPUT_ROW_HEIGHT.toDp())
                .fillMaxWidth()
                .padding(start = 8.dp, end = if (index == 0 && node.data.hasOutput()) 48.dp else 8.dp)
                .let {
                  if (isPortEmpty) {
                    it.clickable(enabled = !isSelectionMode) { onPortClick(node.id, port) }
                      .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                  } else {
                    it
                  }
                },
            contentAlignment = Alignment.CenterStart,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp)
            ) {
              if (isPortEmpty) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = stringResource(R.string.bg_cd_add),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = port.label?.asString() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPortEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
    // Output label on the right, aligned with the first input row.
    if (node.data.hasOutput()) {
      with(density) {
        Box(
          modifier =
            Modifier.height(INPUT_ROW_HEIGHT.toDp())
              .align(Alignment.TopEnd)
              .padding(horizontal = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.bg_label_out),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterEnd),
          )
        }
      }
    }
  }
}
