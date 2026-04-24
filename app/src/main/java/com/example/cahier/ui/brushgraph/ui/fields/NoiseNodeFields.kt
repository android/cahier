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
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.ProgressDomainContext
import com.example.cahier.ui.brushgraph.model.getNumericLimits
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun NoiseNodeFields(
  noiseNode: ProtoBrushBehavior.NoiseNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  textFieldsLocked: Boolean,
  modifier: Modifier = Modifier
) {
  val limits = noiseNode.varyOver.getNumericLimits(ProgressDomainContext.NOISE)
  
  NumericField(
    title = stringResource(R.string.bg_label_seed),
    value = noiseNode.seed.toFloat(),
    limits = NumericLimits.standard(0f, 100f, 1f),
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(seed = it.toInt()))
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  var showVaryTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_vary_over),
      currentValue = noiseNode.varyOver,
      values = ALL_PROGRESS_DOMAINS.toList(),
      modifier = Modifier.weight(1f),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { domain ->
        val newLimits = domain.getNumericLimits(ProgressDomainContext.NOISE)
        val clampedBasePeriod = noiseNode.basePeriod.coerceIn(newLimits.min, newLimits.max)

        onUpdate(
          NodeData.Behavior(
            behaviorNode.safeCopy(
              noiseNode = noiseNode.safeCopy(
                varyOver = domain,
                basePeriod = clampedBasePeriod
              )
            )
          )
        )
        onDropdownEditComplete()
      }
    )
    IconButton(onClick = { showVaryTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  
  if (showVaryTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_vary_over_format, stringResource(noiseNode.varyOver.displayStringRId())),
      text = stringResource(noiseNode.varyOver.getTooltip()),
      onDismiss = { showVaryTooltip = false }
    )
  }
  
  NumericField(
    title = stringResource(R.string.bg_label_base_period),
    value = noiseNode.basePeriod,
    limits = limits,
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(basePeriod = it))
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
