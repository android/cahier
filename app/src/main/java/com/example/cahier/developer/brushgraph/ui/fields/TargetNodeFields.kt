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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.data.getNumericLimits
import com.example.cahier.developer.brushgraph.ui.TooltipDialog
import com.example.cahier.developer.brushgraph.ui.getTooltip
import ink.proto.BrushBehavior as ProtoBrushBehavior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetNodeFields(
    targetNode: ProtoBrushBehavior.TargetNode,
    behaviorNode: ProtoBrushBehavior.Node,
    onUpdate: (NodeData) -> Unit,
    onFieldEditComplete: () -> Unit,
    onDropdownEditComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limits = targetNode.target.getNumericLimits()
    var expandedTarget by remember { mutableStateOf(false) }
    var showTargetTooltip by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedTarget,
                onExpandedChange = { expandedTarget = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = stringResource(targetNode.target.displayStringRId()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.bg_target)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) },
                    modifier = Modifier
                      .menuAnchor()
                      .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedTarget,
                    onDismissRequest = { expandedTarget = false }
                ) {
                    @Composable
                    fun TargetSection(
                        label: String,
                        targets: List<ProtoBrushBehavior.Target>,
                        modifier: Modifier = Modifier,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f))
                        }
                        targets.forEach { target ->
                            DropdownMenuItem(
                                text = { Text(stringResource(target.displayStringRId())) },
                                onClick = {
                                    val currentDisplayStart =
                                        if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble())
                                            .toFloat() else targetNode.targetModifierRangeStart
                                    val currentDisplayEnd =
                                        if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble())
                                            .toFloat() else targetNode.targetModifierRangeEnd

                                    val newLimits = target.getNumericLimits()
                                    val clampedDisplayStart =
                                        currentDisplayStart.coerceIn(newLimits.min, newLimits.max)
                                    val clampedDisplayEnd =
                                        currentDisplayEnd.coerceIn(newLimits.min, newLimits.max)

                                    val newProtoStart =
                                        if (target.isAngle()) Math.toRadians(clampedDisplayStart.toDouble())
                                            .toFloat() else clampedDisplayStart
                                    val newProtoEnd =
                                        if (target.isAngle()) Math.toRadians(clampedDisplayEnd.toDouble())
                                            .toFloat() else clampedDisplayEnd

                                    onUpdate(
                                        NodeData.Behavior(
                                            behaviorNode.toBuilder()
                                                .setTargetNode(
                                                    targetNode.toBuilder()
                                                        .setTarget(target)
                                                        .setTargetModifierRangeStart(newProtoStart)
                                                        .setTargetModifierRangeEnd(newProtoEnd)
                                                        .build()
                                                )
                                                .build()
                                        )
                                    )
                                    onDropdownEditComplete()
                                    expandedTarget = false
                                }
                            )
                        }
                    }

                    TargetSection(
                        stringResource(R.string.bg_section_size_shape),
                        TARGETS_SIZE_SHAPE
                    )
                    TargetSection(stringResource(R.string.bg_section_position), TARGETS_POSITION)
                    TargetSection(
                        stringResource(R.string.bg_section_color_opacity),
                        TARGETS_COLOR_OPACITY
                    )
                }
            }
            IconButton(onClick = { showTargetTooltip = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = stringResource(R.string.bg_cd_help)
                )
            }
        }
        if (showTargetTooltip) {
            TooltipDialog(
                title = stringResource(
                    R.string.bg_title_target_format,
                    stringResource(targetNode.target.displayStringRId())
                ),
                text = stringResource(targetNode.target.getTooltip()),
                onDismiss = { showTargetTooltip = false }
            )
        }
        NumericField(
            title = stringResource(R.string.bg_label_range_start),
            value = targetNode.targetModifierRangeStart,
            limits = limits,
            onValueChanged = {
                onUpdate(
                    NodeData.Behavior(
                        behaviorNode.toBuilder()
                            .setTargetNode(
                                targetNode.toBuilder().setTargetModifierRangeStart(it).build()
                            )
                            .build()
                    )
                )
            },
            onValueChangeFinished = onFieldEditComplete
        )
        NumericField(
            title = stringResource(R.string.bg_label_range_end),
            value = targetNode.targetModifierRangeEnd,
            limits = limits,
            onValueChanged = {
                onUpdate(
                    NodeData.Behavior(
                        behaviorNode.toBuilder()
                            .setTargetNode(
                                targetNode.toBuilder().setTargetModifierRangeEnd(it).build()
                            )
                            .build()
                    )
                )
            },
            onValueChangeFinished = onFieldEditComplete
        )
    }
}
