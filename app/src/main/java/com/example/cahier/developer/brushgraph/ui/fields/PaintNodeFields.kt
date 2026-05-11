/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.ui.getTooltip
import ink.proto.BrushPaint as ProtoBrushPaint

@Composable
fun PaintNodeFields(
  data: NodeData.Paint,
  onUpdate: (NodeData) -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
    val paint = data.paint
    FieldWithTooltip(
        modifier = modifier.padding(vertical = 4.dp),
        tooltipTitle = stringResource(
            R.string.bg_title_self_overlap_format,
            stringResource(paint.selfOverlap.displayStringRId())
        ),
        tooltipText = stringResource(paint.selfOverlap.getTooltip()),
    ) {
        EnumDropdown(
            label = stringResource(R.string.bg_self_overlap),
            currentValue = paint.selfOverlap,
            values = listOf(
                ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY,
                ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE,
                ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD,
            ),
            displayName = { stringResource(it.displayStringRId()) },
            onSelected = { so ->
                onUpdate(
                    NodeData.Paint(
                        paint.toBuilder().setSelfOverlap(so).build(),
                        texturePortIds = data.texturePortIds,
                        colorPortIds = data.colorPortIds
                    )
                )
                onDropdownEditComplete()
            }
        )
    }
}
