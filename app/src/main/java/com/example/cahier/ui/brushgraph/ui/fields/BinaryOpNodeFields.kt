@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.ui.fields.ALL_BINARY_OPS
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun BinaryOpNodeFields(
  binaryNode: ProtoBrushBehavior.BinaryOpNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showOpTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_operation),
      currentValue = binaryNode.operation,
      values = ALL_BINARY_OPS.toList(),
      modifier = Modifier.weight(1f),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { op ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.safeCopy(binaryOpNode = binaryNode.safeCopy(operation = op))
          )
        )
        onDropdownEditComplete()
      }
    )
    IconButton(onClick = { showOpTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  if (showOpTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_operation_format, stringResource(binaryNode.operation.displayStringRId())),
      text = stringResource(binaryNode.operation.getTooltip()),
      onDismiss = { showOpTooltip = false }
    )
  }
}
