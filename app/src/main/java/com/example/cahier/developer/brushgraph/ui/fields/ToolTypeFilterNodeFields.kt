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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import androidx.ink.brush.InputToolType
import com.example.cahier.R
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun ToolTypeFilterNodeFields(
  filterNode: ProtoBrushBehavior.ToolTypeFilterNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    Text(stringResource(R.string.bg_enabled_tool_types), style = MaterialTheme.typography.bodySmall)
    ALL_TOOL_TYPES.forEach { toolType ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        val bitIndex = toolTypeBitIndex(toolType)
        Checkbox(
          checked = (filterNode.enabledToolTypes and (1 shl bitIndex)) != 0,
          onCheckedChange = { checked ->
            val newMask =
              if (checked) {
                filterNode.enabledToolTypes or (1 shl bitIndex)
              } else {
                filterNode.enabledToolTypes and (1 shl bitIndex).inv()
              }
            onUpdate(
              NodeData.Behavior(
                behaviorNode.toBuilder()
                  .setToolTypeFilterNode(filterNode.toBuilder().setEnabledToolTypes(newMask).build())
                  .build()
              )
            )
          }
        )
        Text(stringResource(toolType.displayStringRId()))
      }
    }
  }
}

private fun toolTypeBitIndex(toolType: InputToolType): Int =
  when (toolType) {
    InputToolType.UNKNOWN -> 0
    InputToolType.MOUSE -> 1
    InputToolType.TOUCH -> 2
    InputToolType.STYLUS -> 3
    else -> 0
  }
