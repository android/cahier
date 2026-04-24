@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.ui.brushgraph.data.NodeData
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun ConstantNodeFields(
  constantNode: ProtoBrushBehavior.ConstantNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  NumericField(
    title = stringResource(R.string.bg_port_value),
    value = constantNode.value,
    limits = NumericLimits.standard(-100f, 100f, 0.01f),
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(behaviorNode.toBuilder().setConstantNode(constantNode.toBuilder().setValue(it).build()).build())
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
