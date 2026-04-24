@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.ProgressDomainContext
import com.example.cahier.developer.brushgraph.data.getNumericLimits

import com.example.cahier.developer.brushgraph.ui.fields.ALL_OUT_OF_RANGE
import com.example.cahier.developer.brushgraph.ui.fields.ALL_PROGRESS_DOMAINS
import com.example.cahier.developer.brushgraph.data.displayStringRId
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
            behaviorNode.toBuilder()
              .setIntegralNode(
                integralNode.toBuilder()
                  .setIntegrateOver(domain)
                  .setIntegralValueRangeStart(clampedStart)
                  .setIntegralValueRangeEnd(clampedEnd)
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
    title = stringResource(R.string.bg_label_range_start),
    value = integralNode.integralValueRangeStart,
    limits = limits,
    onValueChangeFinished = onFieldEditComplete
  ) {
    onUpdate(
      NodeData.Behavior(
        behaviorNode.toBuilder()
          .setIntegralNode(integralNode.toBuilder().setIntegralValueRangeStart(it).build())
          .build()
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
        behaviorNode.toBuilder()
          .setIntegralNode(integralNode.toBuilder().setIntegralValueRangeEnd(it).build())
          .build()
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
            behaviorNode.toBuilder()
              .setIntegralNode(integralNode.toBuilder().setIntegralOutOfRangeBehavior(oor).build())
              .build()
          )
        )
      }
    )
  }
}
