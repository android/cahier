/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
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
import com.example.cahier.developer.brushgraph.data.StarredField
import com.example.cahier.developer.brushgraph.data.StarredFieldType
import com.example.cahier.developer.brushgraph.ui.fields.StarrableNumericField
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
  nodeId: String,
  starredFields: Set<StarredField>,
  onToggleStar: (String, StarredFieldType) -> Unit,
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
  
  val isRangeStartStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.INTEGRAL_RANGE_START))
  
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.INTEGRAL_RANGE_START,
    value = integralNode.integralValueRangeStart,
    limits = limits,
    isStarred = isRangeStartStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.INTEGRAL_RANGE_START) },
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.toBuilder()
            .setIntegralNode(integralNode.toBuilder().setIntegralValueRangeStart(it).build())
            .build()
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  val isRangeEndStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.INTEGRAL_RANGE_END))

  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.INTEGRAL_RANGE_END,
    value = integralNode.integralValueRangeEnd,
    limits = limits,
    isStarred = isRangeEndStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.INTEGRAL_RANGE_END) },
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.toBuilder()
            .setIntegralNode(integralNode.toBuilder().setIntegralValueRangeEnd(it).build())
            .build()
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
  
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
