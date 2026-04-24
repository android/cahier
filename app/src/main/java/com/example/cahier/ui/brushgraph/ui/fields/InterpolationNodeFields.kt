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
import com.example.cahier.ui.brushgraph.ui.fields.ALL_INTERPOLATIONS
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun InterpolationNodeFields(
  interpNode: ProtoBrushBehavior.InterpolationNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier
) {
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_interpolation_format, stringResource(interpNode.interpolation.displayStringRId())),
    tooltipText = stringResource(interpNode.interpolation.getTooltip()),
    modifier = modifier
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_interpolation),
      currentValue = interpNode.interpolation,
      values = ALL_INTERPOLATIONS.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { interp ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder()
              .setInterpolationNode(interpNode.toBuilder().setInterpolation(interp).build())
              .build()
          )
        )
      }
    )
  }
}
