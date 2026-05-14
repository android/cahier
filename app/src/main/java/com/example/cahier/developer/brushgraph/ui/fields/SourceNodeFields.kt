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
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.data.getNumericLimits
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.ui.TooltipDialog
import com.example.cahier.developer.brushgraph.ui.getTooltip
import ink.proto.BrushBehavior as ProtoBrushBehavior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceNodeFields(
    sourceNode: ProtoBrushBehavior.SourceNode,
    behaviorNode: ProtoBrushBehavior.Node,
    onUpdate: (NodeData) -> Unit,
    onDropdownEditComplete: () -> Unit,
    onFieldEditComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limits = sourceNode.source.getNumericLimits()
    var expandedSource by remember { mutableStateOf(false) }
    var showSourceTooltip by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedSource,
                onExpandedChange = { expandedSource = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = stringResource(sourceNode.source.displayStringRId()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.bg_source)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                    modifier = Modifier
                      .menuAnchor()
                      .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedSource,
                    onDismissRequest = { expandedSource = false }
                ) {
                    @Composable
                    fun SourceSection(
                        label: String,
                        sources: List<ProtoBrushBehavior.Source>,
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
                        sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(stringResource(source.displayStringRId())) },
                                onClick = {
                                    val currentDisplayStart =
                                        if (sourceNode.source.isAngle()) Math.toDegrees(sourceNode.sourceValueRangeStart.toDouble())
                                            .toFloat() else sourceNode.sourceValueRangeStart
                                    val currentDisplayEnd =
                                        if (sourceNode.source.isAngle()) Math.toDegrees(sourceNode.sourceValueRangeEnd.toDouble())
                                            .toFloat() else sourceNode.sourceValueRangeEnd

                                    val newLimits = source.getNumericLimits()
                                    val clampedDisplayStart =
                                        currentDisplayStart.coerceIn(newLimits.min, newLimits.max)
                                    val clampedDisplayEnd =
                                        currentDisplayEnd.coerceIn(newLimits.min, newLimits.max)

                                    val newProtoStart =
                                        if (source.isAngle()) Math.toRadians(clampedDisplayStart.toDouble())
                                            .toFloat() else clampedDisplayStart
                                    val newProtoEnd =
                                        if (source.isAngle()) Math.toRadians(clampedDisplayEnd.toDouble())
                                            .toFloat() else clampedDisplayEnd

                                    val needsClamp = source.isTimeSince()
                                    val newOor =
                                        if (needsClamp) ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP else sourceNode.sourceOutOfRangeBehavior

                                    onUpdate(
                                        NodeData.Behavior(
                                            behaviorNode.toBuilder()
                                                .setSourceNode(
                                                    sourceNode.toBuilder()
                                                        .setSource(source)
                                                        .setSourceOutOfRangeBehavior(newOor)
                                                        .setSourceValueRangeStart(newProtoStart)
                                                        .setSourceValueRangeEnd(newProtoEnd)
                                                        .build()
                                                )
                                                .build()
                                        )
                                    )
                                    onDropdownEditComplete()
                                    expandedSource = false
                                }
                            )
                        }
                    }

                    SourceSection(stringResource(R.string.bg_section_input), SOURCES_INPUT)
                    SourceSection(stringResource(R.string.bg_section_movement), SOURCES_MOVEMENT)
                    SourceSection(stringResource(R.string.bg_section_distance), SOURCES_DISTANCE)
                    SourceSection(stringResource(R.string.bg_section_time), SOURCES_TIME)
                    SourceSection(
                        stringResource(R.string.bg_section_acceleration),
                        SOURCES_ACCELERATION
                    )
                }
            }
            IconButton(onClick = { showSourceTooltip = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = stringResource(R.string.bg_cd_help)
                )
            }
        }

        if (showSourceTooltip) {
            TooltipDialog(
                title = stringResource(
                    R.string.bg_title_source_format,
                    stringResource(sourceNode.source.displayStringRId())
                ),
                text = stringResource(sourceNode.source.getTooltip()),
                onDismiss = { showSourceTooltip = false }
            )
        }
        NumericField(
            title = stringResource(R.string.bg_label_range_start),
            value = sourceNode.sourceValueRangeStart,
            limits = limits,
            onValueChanged = {
                onUpdate(
                    NodeData.Behavior(
                        behaviorNode.toBuilder().setSourceNode(
                            sourceNode.toBuilder().setSourceValueRangeStart(it).build()
                        ).build()
                    )
                )
            },
            onValueChangeFinished = onFieldEditComplete
        )
        NumericField(
            title = stringResource(R.string.bg_label_range_end),
            value = sourceNode.sourceValueRangeEnd,
            limits = limits,
            onValueChanged = {
                onUpdate(
                    NodeData.Behavior(
                        behaviorNode.toBuilder().setSourceNode(
                            sourceNode.toBuilder().setSourceValueRangeEnd(it).build()
                        ).build()
                    )
                )
            },
            onValueChangeFinished = onFieldEditComplete
        )
        val isTimeSinceSource = sourceNode.source.isTimeSince()
        FieldWithTooltip(
            tooltipTitle = stringResource(
                R.string.bg_title_out_of_range_behavior_format,
                stringResource(sourceNode.sourceOutOfRangeBehavior.displayStringRId())
            ),
            tooltipText = stringResource(sourceNode.sourceOutOfRangeBehavior.getTooltip())
        ) {
            EnumDropdown(
                label = stringResource(R.string.bg_out_of_range_behavior),
                currentValue = sourceNode.sourceOutOfRangeBehavior,
                values = if (isTimeSinceSource) {
                    listOf(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                } else {
                    ALL_OUT_OF_RANGE.toList()
                },
                displayName = { stringResource(it.displayStringRId()) },
                onSelected = { oor ->
                    onUpdate(
                        NodeData.Behavior(
                            behaviorNode.toBuilder().setSourceNode(
                                sourceNode.toBuilder().setSourceOutOfRangeBehavior(oor).build()
                            ).build()
                        )
                    )
                    onDropdownEditComplete()
                }
            )
        }
        if (isTimeSinceSource) {
            Text(
                text = stringResource(R.string.bg_msg_source_clamp_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun ProtoBrushBehavior.Source.isTimeSince(): Boolean {
    return this == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS ||
            this == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS
}
