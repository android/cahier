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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.ui.fields.ALL_INTERPOLATIONS
import com.example.cahier.developer.brushgraph.ui.getTooltip
import com.example.cahier.developer.brushgraph.data.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun InterpolationNodeFields(
  interpNode: ProtoBrushBehavior.InterpolationNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier
) {
  FieldWithTooltip(
    modifier = modifier,
    tooltipTitle = stringResource(R.string.bg_title_interpolation_format, stringResource(interpNode.interpolation.displayStringRId())),
    tooltipText = stringResource(interpNode.interpolation.getTooltip()),
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_interpolation),
      currentValue = interpNode.interpolation,
      values = ALL_INTERPOLATIONS.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { interp ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder()
              .setInterpolationNode(interpNode.toBuilder().setInterpolation(interp).build())
              .build()
          )
        )
      }
    )
  }
}
