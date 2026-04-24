@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.ui.FieldWithTooltip
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
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_operation_format, stringResource(binaryNode.operation.displayStringRId())),
    tooltipText = stringResource(binaryNode.operation.getTooltip()),
    modifier = modifier
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_operation),
      currentValue = binaryNode.operation,
      values = ALL_BINARY_OPS.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { op ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder()
              .setBinaryOpNode(binaryNode.toBuilder().setOperation(op).build())
              .build()
          )
        )
        onDropdownEditComplete()
      }
    )
  }
}
