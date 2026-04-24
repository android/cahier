@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.fields.ALL_OUT_OF_RANGE
import com.example.cahier.ui.brushgraph.ui.fields.ALL_PROGRESS_DOMAINS
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun IntegralNodeFields(
  integralNode: ProtoBrushBehavior.IntegralNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val limits = integralNode.integrateOver.getNumericLimits(ProgressDomainContext.INTEGRAL)
  Row(modifier = modifier.fillMaxWidth()) {
    EnumDropdown(
      label = stringResource(R.string.bg_integrate_over),
      currentValue = integralNode.integrateOver,
      values = ALL_PROGRESS_DOMAINS.toList(),
      modifier = Modifier.weight(1f),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { domain ->
        val newLimits = domain.getNumericLimits(ProgressDomainContext.INTEGRAL)
        val clampedStart = integralNode.integralValueRangeStart.coerceIn(newLimits.min, newLimits.max)
        val clampedEnd = integralNode.integralValueRangeEnd.coerceIn(newLimits.min, newLimits.max)

        onUpdate(
          NodeData.Behavior(
            behaviorNode.safeCopy(
              integralNode = integralNode.safeCopy(
                integrateOver = domain,
                integralValueRangeStart = clampedStart,
                integralValueRangeEnd = clampedEnd
              )
            )
          )
        )
        onDropdownEditComplete()
      }
    )
  }
  
  NumericField(
    title = stringResource(R.string.bg_label_range_start),
    value = integralNode.integralValueRangeStart,
    limits = limits,
    onValueChangeFinished = onFieldEditComplete
  ) {
    onUpdate(
      NodeData.Behavior(
        behaviorNode.safeCopy(
          integralNode = integralNode.safeCopy(integralValueRangeStart = it)
        )
      )
    )
  }
  NumericField(
    title = stringResource(R.string.bg_label_range_end),
    value = integralNode.integralValueRangeEnd,
    limits = limits,
    onValueChangeFinished = onFieldEditComplete
  ) {
    onUpdate(
      NodeData.Behavior(
        behaviorNode.safeCopy(
          integralNode = integralNode.safeCopy(integralValueRangeEnd = it)
        )
      )
    )
  }
  
  Row(modifier = Modifier.fillMaxWidth()) {
    EnumDropdown(
      label = stringResource(R.string.bg_out_of_range_behavior),
      currentValue = integralNode.integralOutOfRangeBehavior,
      values = ALL_OUT_OF_RANGE.toList(),
      modifier = Modifier.weight(1f),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { oor ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.safeCopy(
              integralNode = integralNode.safeCopy(integralOutOfRangeBehavior = oor)
            )
          )
        )
      }
    )
  }
}
