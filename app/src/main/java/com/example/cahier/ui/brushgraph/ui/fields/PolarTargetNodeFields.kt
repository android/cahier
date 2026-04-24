@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.getNumericLimits
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.ui.FieldWithTooltip
import com.example.cahier.ui.brushgraph.ui.fields.ALL_POLAR_TARGETS
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun PolarTargetNodeFields(
  polarNode: ProtoBrushBehavior.PolarTargetNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_polar_target_format, stringResource(polarNode.target.displayStringRId())),
    tooltipText = stringResource(polarNode.target.getTooltip()),
    modifier = modifier
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_polar_target),
      currentValue = polarNode.target,
      values = ALL_POLAR_TARGETS.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { target ->
        val newMagLimits = NumericLimits.standard(-10.0f, 10.0f, 0.01f)
        val clampedMagStart = polarNode.magnitudeRangeStart.coerceIn(newMagLimits.min, newMagLimits.max)
        val clampedMagEnd = polarNode.magnitudeRangeEnd.coerceIn(newMagLimits.min, newMagLimits.max)

        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder()
              .setPolarTargetNode(
                polarNode.toBuilder()
                  .setTarget(target)
                  .setMagnitudeRangeStart(clampedMagStart)
                  .setMagnitudeRangeEnd(clampedMagEnd)
                  .build()
              )
              .build()
          )
        )
        onDropdownEditComplete()
      }
    )
  }
  
  NumericField(
    title = stringResource(R.string.bg_label_angle_start),
    value = polarNode.angleRangeStart,
    limits = NumericLimits.radiansShownAsDegrees(-360f, 360f),
    onValueChanged = {
      onUpdate(NodeData.Behavior(behaviorNode.toBuilder().setPolarTargetNode(polarNode.toBuilder().setAngleRangeStart(it).build()).build()))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  NumericField(
    title = stringResource(R.string.bg_label_angle_end),
    value = polarNode.angleRangeEnd,
    limits = NumericLimits.radiansShownAsDegrees(-360f, 360f),
    onValueChanged = {
      onUpdate(NodeData.Behavior(behaviorNode.toBuilder().setPolarTargetNode(polarNode.toBuilder().setAngleRangeEnd(it).build()).build()))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  val magLimits = NumericLimits.standard(-10.0f, 10.0f, 0.01f)
  
  NumericField(
    title = stringResource(R.string.bg_label_mag_start),
    value = polarNode.magnitudeRangeStart,
    limits = magLimits,
    onValueChanged = {
      onUpdate(NodeData.Behavior(behaviorNode.toBuilder().setPolarTargetNode(polarNode.toBuilder().setMagnitudeRangeStart(it).build()).build()))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  NumericField(
    title = stringResource(R.string.bg_label_mag_end),
    value = polarNode.magnitudeRangeEnd,
    limits = magLimits,
    onValueChanged = {
      onUpdate(NodeData.Behavior(behaviorNode.toBuilder().setPolarTargetNode(polarNode.toBuilder().setMagnitudeRangeEnd(it).build()).build()))
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
