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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.ProgressDomainContext
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.data.getNumericLimits
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun IntegralNodeFields(
  integralNode: ProtoBrushBehavior.IntegralNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
    val limits = integralNode.integrateOver.getNumericLimits(ProgressDomainContext.INTEGRAL)
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            EnumDropdown(
                label = stringResource(R.string.bg_integrate_over),
                currentValue = integralNode.integrateOver,
                values = ALL_PROGRESS_DOMAINS.toList(),
                modifier = Modifier.weight(1f),
                displayName = { stringResource(it.displayStringRId()) },
                onSelected = { domain ->
                    val newLimits = domain.getNumericLimits(ProgressDomainContext.INTEGRAL)
                    val clampedStart =
                        integralNode.integralValueRangeStart.coerceIn(newLimits.min, newLimits.max)
                    val clampedEnd =
                        integralNode.integralValueRangeEnd.coerceIn(newLimits.min, newLimits.max)

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
                        .setIntegralNode(
                            integralNode.toBuilder().setIntegralValueRangeStart(it).build()
                        )
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
                        .setIntegralNode(
                            integralNode.toBuilder().setIntegralValueRangeEnd(it).build()
                        )
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
                                .setIntegralNode(
                                    integralNode.toBuilder().setIntegralOutOfRangeBehavior(oor)
                                        .build()
                                )
                                .build()
                        )
                    )
                }
            )
        }
    }
}
