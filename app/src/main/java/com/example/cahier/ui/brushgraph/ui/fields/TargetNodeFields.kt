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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.cahier.ui.brushgraph.model.getNumericLimits
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun TargetNodeFields(
  targetNode: ProtoBrushBehavior.TargetNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val limits = targetNode.target.getNumericLimits()
  var expandedTarget by remember { mutableStateOf(false) }
  var showTargetTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    ExposedDropdownMenuBox(
      expanded = expandedTarget,
      onExpandedChange = { expandedTarget = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(targetNode.target.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_target)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedTarget,
        onDismissRequest = { expandedTarget = false }
      ) {
        @Composable
        fun TargetSection(label: String, targets: List<ProtoBrushBehavior.Target>) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
          ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline,
              modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
          }
          targets.forEach { target ->
            DropdownMenuItem(
              text = { Text(stringResource(target.displayStringRId())) },
              onClick = {
                val currentDisplayStart = if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble()).toFloat() else targetNode.targetModifierRangeStart
                val currentDisplayEnd = if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble()).toFloat() else targetNode.targetModifierRangeEnd

                val newLimits = target.getNumericLimits()
                val clampedDisplayStart = currentDisplayStart.coerceIn(newLimits.min, newLimits.max)
                val clampedDisplayEnd = currentDisplayEnd.coerceIn(newLimits.min, newLimits.max)

                val newProtoStart = if (target.isAngle()) Math.toRadians(clampedDisplayStart.toDouble()).toFloat() else clampedDisplayStart
                val newProtoEnd = if (target.isAngle()) Math.toRadians(clampedDisplayEnd.toDouble()).toFloat() else clampedDisplayEnd

                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      targetNode = targetNode.safeCopy(
                        target = target,
                        targetModifierRangeStart = newProtoStart,
                        targetModifierRangeEnd = newProtoEnd
                      )
                    )
                  )
                )
                onDropdownEditComplete()
                expandedTarget = false
              }
            )
          }
        }

        TargetSection(stringResource(R.string.bg_section_size_shape), TARGETS_SIZE_SHAPE)
        TargetSection(stringResource(R.string.bg_section_position), TARGETS_POSITION)
        TargetSection(stringResource(R.string.bg_section_color_opacity), TARGETS_COLOR_OPACITY)
      }
    }
    IconButton(onClick = { showTargetTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  if (showTargetTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_target_format, stringResource(targetNode.target.displayStringRId())),
      text = stringResource(targetNode.target.getTooltip()),
      onDismiss = { showTargetTooltip = false }
    )
  }
  val isAngleTarget = targetNode.target == ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ||
                      targetNode.target == ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ||
                      targetNode.target == ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS

  val displayValueStart = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble()).toFloat() else targetNode.targetModifierRangeStart
  val displayValueEnd = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble()).toFloat() else targetNode.targetModifierRangeEnd

  BrushSliderControl(
    label = stringResource(R.string.bg_label_range_start),
    value = displayValueStart,
    valueRange = limits.min..limits.max,
    unit = limits.displayUnit,
    onValueChange = {
      val newValue = if (isAngleTarget) Math.toRadians(it.toDouble()).toFloat() else it
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(
            targetNode = targetNode.safeCopy(targetModifierRangeStart = newValue)
          )
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_range_end),
    value = displayValueEnd,
    valueRange = limits.min..limits.max,
    unit = limits.displayUnit,
    onValueChange = {
      val newValue = if (isAngleTarget) Math.toRadians(it.toDouble()).toFloat() else it
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(
            targetNode = targetNode.safeCopy(targetModifierRangeEnd = newValue)
          )
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
