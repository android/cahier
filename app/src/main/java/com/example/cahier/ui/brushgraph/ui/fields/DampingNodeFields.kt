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
import com.example.cahier.ui.brushgraph.model.ProgressDomainContext
import com.example.cahier.ui.brushgraph.model.getNumericLimits
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun DampingNodeFields(
  dampingNode: ProtoBrushBehavior.DampingNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val limits = dampingNode.dampingSource.getNumericLimits(ProgressDomainContext.DAMPING)
  var expandedSource by remember { mutableStateOf(false) }
  var showDampingTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    ExposedDropdownMenuBox(
      expanded = expandedSource,
      onExpandedChange = { expandedSource = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(dampingNode.dampingSource.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_damping_source)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedSource,
        onDismissRequest = { expandedSource = false }
      ) {
        ALL_PROGRESS_DOMAINS.forEach { domain ->
          DropdownMenuItem(
            text = { Text(stringResource(domain.displayStringRId())) },
            onClick = {
              val newLimits = domain.getNumericLimits(ProgressDomainContext.DAMPING)
              val clampedGap = dampingNode.dampingGap.coerceIn(newLimits.min, newLimits.max)

              onUpdate(
                NodeData.Behavior(
                  behaviorNode.safeCopy(
                    dampingNode = dampingNode.safeCopy(
                      dampingSource = domain,
                      dampingGap = clampedGap
                    )
                  )
                )
              )
              onDropdownEditComplete()
              expandedSource = false
            }
          )
        }
      }
    }
    IconButton(onClick = { showDampingTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  
  if (showDampingTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_damping_source_format, stringResource(dampingNode.dampingSource.displayStringRId())),
      text = stringResource(dampingNode.dampingSource.getTooltip()),
      onDismiss = { showDampingTooltip = false }
    )
  }
  
  BrushSliderControl(
    label = stringResource(R.string.bg_label_damping_gap),
    value = dampingNode.dampingGap,
    valueRange = limits.min..limits.max,
    onValueChange = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(dampingNode = dampingNode.safeCopy(dampingGap = it))
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
