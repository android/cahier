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
import com.example.cahier.ui.brushdesigner.BrushSliderControl
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
  var expandedOver by remember { mutableStateOf(false) }
  
  Row(modifier = modifier.fillMaxWidth()) {
    ExposedDropdownMenuBox(
      expanded = expandedOver,
      onExpandedChange = { expandedOver = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(integralNode.integrateOver.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_integrate_over)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOver) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedOver,
        onDismissRequest = { expandedOver = false }
      ) {
        ALL_PROGRESS_DOMAINS.forEach { domain ->
          DropdownMenuItem(
            text = { Text(stringResource(domain.displayStringRId())) },
            onClick = {
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
              expandedOver = false
            }
          )
        }
      }
    }
  }
  
  BrushSliderControl(
    label = stringResource(R.string.bg_label_range_start),
    value = integralNode.integralValueRangeStart,
    valueRange = limits.min..limits.max,
    onValueChange = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(
            integralNode = integralNode.safeCopy(integralValueRangeStart = it)
          )
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_range_end),
    value = integralNode.integralValueRangeEnd,
    valueRange = limits.min..limits.max,
    onValueChange = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.safeCopy(
            integralNode = integralNode.safeCopy(integralValueRangeEnd = it)
          )
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
  var expandedIntegralOor by remember { mutableStateOf(false) }
  
  Row(modifier = Modifier.fillMaxWidth()) {
    ExposedDropdownMenuBox(
      expanded = expandedIntegralOor,
      onExpandedChange = { expandedIntegralOor = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(integralNode.integralOutOfRangeBehavior.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_out_of_range_behavior)) },
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIntegralOor)
        },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedIntegralOor,
        onDismissRequest = { expandedIntegralOor = false }
      ) {
        ALL_OUT_OF_RANGE.forEach { oor ->
          DropdownMenuItem(
            text = { Text(stringResource(oor.displayStringRId())) },
            onClick = {
              onUpdate(
                NodeData.Behavior(
                  behaviorNode.safeCopy(
                    integralNode = integralNode.safeCopy(integralOutOfRangeBehavior = oor)
                  )
                )
              )
              expandedIntegralOor = false
            }
          )
        }
      }
    }
  }
}
