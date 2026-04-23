@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.ui.brushdesigner.BrushSliderControl
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.NumericLimits
import com.example.cahier.ui.brushgraph.model.getNumericLimits
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
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
  var expandedPolar by remember { mutableStateOf(false) }
  var showPolarTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    ExposedDropdownMenuBox(
      expanded = expandedPolar,
      onExpandedChange = { expandedPolar = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(polarNode.target.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_polar_target)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPolar) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedPolar,
        onDismissRequest = { expandedPolar = false }
      ) {
        ALL_POLAR_TARGETS.forEach { target ->
          DropdownMenuItem(
            text = { Text(stringResource(target.displayStringRId())) },
            onClick = {
              val newMagLimits = if (target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ||
                                  target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE) {
                  NumericLimits(-10.0f, 10.0f, 0.01f)
              } else {
                  NumericLimits(0.0f, 1.0f, 0.1f)
              }
              val clampedMagStart = polarNode.magnitudeRangeStart.coerceIn(newMagLimits.min, newMagLimits.max)
              val clampedMagEnd = polarNode.magnitudeRangeEnd.coerceIn(newMagLimits.min, newMagLimits.max)

              onUpdate(
                NodeData.Behavior(
                  behaviorNode.safeCopy(
                    polarTargetNode = polarNode.safeCopy(
                      target = target,
                      magnitudeRangeStart = clampedMagStart,
                      magnitudeRangeEnd = clampedMagEnd
                    )
                  )
                )
              )
              onDropdownEditComplete()
              expandedPolar = false
            }
          )
        }
      }
    }
    IconButton(onClick = { showPolarTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  if (showPolarTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_polar_target_format, stringResource(polarNode.target.displayStringRId())),
      text = stringResource(polarNode.target.getTooltip()),
      onDismiss = { showPolarTooltip = false }
    )
  }
  
  val displayAngleStart = Math.toDegrees(polarNode.angleRangeStart.toDouble()).toFloat()
  val displayAngleEnd = Math.toDegrees(polarNode.angleRangeEnd.toDouble()).toFloat()

  BrushSliderControl(
    label = stringResource(R.string.bg_label_angle_start),
    value = displayAngleStart,
    valueRange = -360f..360f,
    unit = "°",
    onValueChange = {
      val newValue = Math.toRadians(it.toDouble()).toFloat()
      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeStart = newValue))))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  BrushSliderControl(
    label = stringResource(R.string.bg_label_angle_end),
    value = displayAngleEnd,
    valueRange = -360f..360f,
    unit = "°",
    onValueChange = {
      val newValue = Math.toRadians(it.toDouble()).toFloat()
      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeEnd = newValue))))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  val magLimits = if (polarNode.target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ||
                      polarNode.target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE) {
      NumericLimits(-10.0f, 10.0f, 0.01f)
  } else {
      NumericLimits(0.0f, 1.0f, 0.1f)
  }
  
  BrushSliderControl(
    label = stringResource(R.string.bg_label_mag_start),
    value = polarNode.magnitudeRangeStart,
    valueRange = magLimits.min..magLimits.max,
    onValueChange = {
      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(magnitudeRangeStart = it))))
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  BrushSliderControl(
    label = stringResource(R.string.bg_label_mag_end),
    value = polarNode.magnitudeRangeEnd,
    valueRange = magLimits.min..magLimits.max,
    onValueChange = {
      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(magnitudeRangeEnd = it))))
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
