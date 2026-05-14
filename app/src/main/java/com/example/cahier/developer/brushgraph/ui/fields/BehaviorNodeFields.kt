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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BehaviorNodeFields(
    data: NodeData.Behavior,
    onUpdate: (NodeData) -> Unit,
    onDropdownEditComplete: () -> Unit,
    onFieldEditComplete: () -> Unit,
    textFieldsLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val behaviorNode = data.node
    val nodeCase = behaviorNode.nodeCase
    var expandedNodeTypes by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Node Type Selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedNodeTypes,
                onExpandedChange = { expandedNodeTypes = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = stringResource(nodeCase.displayStringRId()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.bg_node_type)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNodeTypes) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedNodeTypes,
                    onDismissRequest = { expandedNodeTypes = false }
                ) {
                    @Composable
                    fun DropdownSection(
                        label: String,
                        types: List<ProtoBrushBehavior.Node.NodeCase>,
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
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.displayStringRId())) },
                                onClick = {
                                    if (type != nodeCase) {
                                        onUpdate(createDefaultNode(type))
                                        onDropdownEditComplete()
                                    }
                                    expandedNodeTypes = false
                                }
                            )
                        }
                    }

                    DropdownSection(
                        stringResource(R.string.bg_section_start_nodes),
                        NODE_TYPES_START
                    )
                    DropdownSection(
                        stringResource(R.string.bg_section_operator_nodes),
                        NODE_TYPES_OPERATOR
                    )
                    DropdownSection(
                        stringResource(R.string.bg_section_terminal_nodes),
                        NODE_TYPES_TERMINAL
                    )
                }
            }
        }

        // Developer Comment
        if (nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ||
            nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
        ) {
            OutlinedTextField(
                value = data.developerComment,
                onValueChange = {
                    onUpdate(data.copy(developerComment = it))
                },
                label = { Text(stringResource(R.string.bg_developer_comment)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                minLines = 2,
                enabled = !textFieldsLocked,
            )
        }

        // Dispatch to specific node fields
        when (nodeCase) {
            ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> {
                SourceNodeFields(
                    sourceNode = behaviorNode.sourceNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onDropdownEditComplete = onDropdownEditComplete,
                    onFieldEditComplete = onFieldEditComplete,
                )
            }

            ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> {
                ConstantNodeFields(
                    constantNode = behaviorNode.constantNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete
                )
            }

            ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> {
                NoiseNodeFields(
                    noiseNode = behaviorNode.noiseNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    onDropdownEditComplete = onDropdownEditComplete,
                    textFieldsLocked = textFieldsLocked
                )
            }

            ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> {
                ToolTypeFilterNodeFields(
                    filterNode = behaviorNode.toolTypeFilterNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate
                )
            }

            ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> {
                DampingNodeFields(
                    dampingNode = behaviorNode.dampingNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> {
                ResponseNodeFields(
                    responseNode = behaviorNode.responseNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate
                )
            }

            ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> {
                IntegralNodeFields(
                    integralNode = behaviorNode.integralNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
                BinaryOpNodeFields(
                    binaryNode = behaviorNode.binaryOpNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
                InterpolationNodeFields(
                    interpNode = behaviorNode.interpolationNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate
                )
            }

            ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> {
                TargetNodeFields(
                    targetNode = behaviorNode.targetNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
                PolarTargetNodeFields(
                    polarNode = behaviorNode.polarTargetNode,
                    behaviorNode = behaviorNode,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            else -> {
                // Fallback or empty view for unsupported nodes
                Text(
                    stringResource(
                        R.string.bg_err_unsupported_behavior_node_type,
                        nodeCase.toString()
                    )
                )
            }
        }
    }
}
