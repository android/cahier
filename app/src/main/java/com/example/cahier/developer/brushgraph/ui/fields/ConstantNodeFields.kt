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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.StarredField
import com.example.cahier.developer.brushgraph.data.StarredFieldType
import com.example.cahier.developer.brushgraph.ui.fields.StarrableNumericField
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun ConstantNodeFields(
  constantNode: ProtoBrushBehavior.ConstantNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  nodeId: String,
  starredFields: Set<StarredField>,
  onToggleStar: (String, StarredFieldType) -> Unit,
  modifier: Modifier = Modifier
) {
  val isValueStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.CONSTANT_VALUE))
  
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.CONSTANT_VALUE,
    value = constantNode.value,
    limits = NumericLimits.standard(-100f, 100f, 0.01f),
    isStarred = isValueStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.CONSTANT_VALUE) },
    onValueChanged = {
      onUpdate(
        NodeData.Behavior(behaviorNode.toBuilder().setConstantNode(constantNode.toBuilder().setValue(it).build()).build())
      )
    },
    onValueChangeFinished = onFieldEditComplete
  )
}
