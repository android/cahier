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

import androidx.compose.material3.MaterialTheme
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
import com.example.cahier.developer.brushgraph.ui.getTooltip
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.data.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun DampingNodeFields(
  dampingNode: ProtoBrushBehavior.DampingNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  nodeId: String,
  starredFields: Set<StarredField>,
  onToggleStar: (String, StarredFieldType) -> Unit,
  modifier: Modifier = Modifier
) {
  val limits = dampingNode.dampingSource.getNumericLimits(ProgressDomainContext.DAMPING)
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_damping_source_format, stringResource(dampingNode.dampingSource.displayStringRId())),
    tooltipText = stringResource(dampingNode.dampingSource.getTooltip()),
    modifier = modifier
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_damping_source),
      currentValue = dampingNode.dampingSource,
      values = ALL_PROGRESS_DOMAINS.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { domain ->
        val newLimits = domain.getNumericLimits(ProgressDomainContext.DAMPING)
        val clampedGap = dampingNode.dampingGap.coerceIn(newLimits.min, newLimits.max)

        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder()
              .setDampingNode(
                dampingNode.toBuilder()
                  .setDampingSource(domain)
                  .setDampingGap(clampedGap)
                  .build()
              )
              .build()
          )
        )
        onDropdownEditComplete()
      }
    )
  }
  
  val isGapStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.DAMPING_GAP))
  
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.DAMPING_GAP,
    value = dampingNode.dampingGap,
    limits = limits,
    isStarred = isGapStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.DAMPING_GAP) },
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(
          behaviorNode.toBuilder()
            .setDampingNode(dampingNode.toBuilder().setDampingGap(it).build())
            .build()
        )
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
