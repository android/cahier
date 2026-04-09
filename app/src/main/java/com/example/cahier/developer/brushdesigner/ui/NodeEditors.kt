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

package com.example.cahier.developer.brushdesigner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import ink.proto.BrushBehavior

/**
 * Dispatches to the correct editor based on the [BrushBehavior.Node.NodeCase].
 *
 * Each editor is stateless: it receives the current node and emits
 * the updated node via [onNodeChanged].
 */
@Composable
internal fun NodeEditor(
    node: BrushBehavior.Node,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    when (node.nodeCase) {
        BrushBehavior.Node.NodeCase.SOURCE_NODE ->
            SourceNodeEditor(node.sourceNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.RESPONSE_NODE ->
            ResponseNodeEditor(node.responseNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.DAMPING_NODE ->
            DampingNodeEditor(node.dampingNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.NOISE_NODE ->
            NoiseNodeEditor(node.noiseNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.TARGET_NODE ->
            TargetNodeEditor(node.targetNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.CONSTANT_NODE ->
            ConstantNodeEditor(node.constantNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.BINARY_OP_NODE ->
            BinaryOpNodeEditor(node.binaryOpNode, onNodeChanged)

        BrushBehavior.Node.NodeCase.INTERPOLATION_NODE ->
            InterpolationNodeEditor(node.interpolationNode, onNodeChanged)

        else -> Text(
            "Unsupported node type: ${node.nodeCase}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun SourceNodeEditor(
    source: BrushBehavior.SourceNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_source), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        val sources = BrushBehavior.Source.entries.filter {
            it != BrushBehavior.Source.SOURCE_UNSPECIFIED
        }
        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_source_input),
            currentValue = source.source,
            values = sources,
            displayName = { it.name.replace("SOURCE_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setSourceNode(
                        source.toBuilder().setSource(selected)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_range_start),
            value = source.sourceValueRangeStart,
            limits = NumericLimits.standard(-100f, 100f, 0.1f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setSourceNode(
                        source.toBuilder().setSourceValueRangeStart(it)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_range_end),
            value = source.sourceValueRangeEnd,
            limits = NumericLimits.standard(-100f, 100f, 0.1f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setSourceNode(
                        source.toBuilder().setSourceValueRangeEnd(it)
                    ).build()
                )
            }
        )

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_out_of_range),
            currentValue = source.sourceOutOfRangeBehavior,
            values = BrushBehavior.OutOfRange.entries.filter {
                it != BrushBehavior.OutOfRange.OUT_OF_RANGE_UNSPECIFIED
            },
            displayName = { it.name.replace("OUT_OF_RANGE_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setSourceNode(
                        source.toBuilder().setSourceOutOfRangeBehavior(selected)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun ResponseNodeEditor(
    response: BrushBehavior.ResponseNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_response_curve), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        MathCurvePreview(
            responseNode = response,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        val currentType = when (response.responseCurveCase) {
            BrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE ->
                ResponseCurveType.CubicBezier
            BrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE ->
                ResponseCurveType.Linear
            BrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE ->
                ResponseCurveType.Steps
            else -> ResponseCurveType.Predefined
        }

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_curve_type),
            currentValue = currentType,
            values = ResponseCurveType.entries.toList(),
            displayName = { it.displayName },
            onSelected = { selected ->
                val newResponseBuilder = response.toBuilder()
                when (selected) {
                    ResponseCurveType.Predefined ->
                        newResponseBuilder.setPredefinedResponseCurve(
                            ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_EASE
                        )
                    ResponseCurveType.CubicBezier ->
                        newResponseBuilder.setCubicBezierResponseCurve(
                            ink.proto.CubicBezierEasingFunction.newBuilder()
                                .setX1(0.25f).setY1(0.1f).setX2(0.25f).setY2(1f)
                        )
                    ResponseCurveType.Linear ->
                        newResponseBuilder.setLinearResponseCurve(
                            ink.proto.LinearEasingFunction.newBuilder()
                        )
                    ResponseCurveType.Steps ->
                        newResponseBuilder.setStepsResponseCurve(
                            ink.proto.StepsEasingFunction.newBuilder().setStepCount(4)
                        )
                }
                onNodeChanged(
                    BrushBehavior.Node.newBuilder()
                        .setResponseNode(newResponseBuilder).build()
                )
            }
        )

        when (response.responseCurveCase) {
            BrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE -> {
                val cb = response.cubicBezierResponseCurve
                NumericField(
                    title = stringResource(R.string.brush_designer_node_control_x1),
                    value = cb.x1,
                    limits = NumericLimits.standard(0f, 1f, 0.05f)
                ) {
                    onNodeChanged(
                        BrushBehavior.Node.newBuilder().setResponseNode(
                            response.toBuilder().setCubicBezierResponseCurve(
                                cb.toBuilder().setX1(it)
                            )
                        ).build()
                    )
                }
                NumericField(
                    title = stringResource(R.string.brush_designer_node_control_y1),
                    value = cb.y1,
                    limits = NumericLimits.standard(-1f, 2f, 0.05f)
                ) {
                    onNodeChanged(
                        BrushBehavior.Node.newBuilder().setResponseNode(
                            response.toBuilder().setCubicBezierResponseCurve(
                                cb.toBuilder().setY1(it)
                            )
                        ).build()
                    )
                }
                NumericField(
                    title = stringResource(R.string.brush_designer_node_control_x2),
                    value = cb.x2,
                    limits = NumericLimits.standard(0f, 1f, 0.05f)
                ) {
                    onNodeChanged(
                        BrushBehavior.Node.newBuilder().setResponseNode(
                            response.toBuilder().setCubicBezierResponseCurve(
                                cb.toBuilder().setX2(it)
                            )
                        ).build()
                    )
                }
                NumericField(
                    title = stringResource(R.string.brush_designer_node_control_y2),
                    value = cb.y2,
                    limits = NumericLimits.standard(-1f, 2f, 0.05f)
                ) {
                    onNodeChanged(
                        BrushBehavior.Node.newBuilder().setResponseNode(
                            response.toBuilder().setCubicBezierResponseCurve(
                                cb.toBuilder().setY2(it)
                            )
                        ).build()
                    )
                }
            }

            BrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE -> {
                EnumDropdown(
                    label = stringResource(R.string.brush_designer_node_predefined_curve),
                    currentValue = response.predefinedResponseCurve,
                    values = ink.proto.PredefinedEasingFunction.entries.toList(),
                    displayName = {
                        it.name.replace("PREDEFINED_EASING_FUNCTION_", "")
                    },
                    onSelected = { selected ->
                        onNodeChanged(
                            BrushBehavior.Node.newBuilder().setResponseNode(
                                response.toBuilder().setPredefinedResponseCurve(selected)
                            ).build()
                        )
                    }
                )
            }

            BrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE -> {
                val steps = response.stepsResponseCurve
                NumericField(
                    title = stringResource(R.string.brush_designer_node_step_count),
                    value = steps.stepCount.toFloat(),
                    limits = NumericLimits.standard(1f, 20f, 1f)
                ) {
                    onNodeChanged(
                        BrushBehavior.Node.newBuilder().setResponseNode(
                            response.toBuilder().setStepsResponseCurve(
                                steps.toBuilder().setStepCount(it.toInt())
                            )
                        ).build()
                    )
                }
            }

            else -> {
                Text(
                    "Linear curves: edit points via export/import.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
internal fun DampingNodeEditor(
    damping: BrushBehavior.DampingNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_damping), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_damping_source),
            currentValue = damping.dampingSource,
            values = BrushBehavior.ProgressDomain.entries.filter {
                it != BrushBehavior.ProgressDomain.PROGRESS_DOMAIN_UNSPECIFIED
            },
            displayName = { it.name.replace("PROGRESS_DOMAIN_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setDampingNode(
                        damping.toBuilder().setDampingSource(selected)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_gap_seconds),
            value = damping.dampingGap,
            limits = NumericLimits.standard(0.01f, 2f, 0.01f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setDampingNode(
                        damping.toBuilder().setDampingGap(it)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun NoiseNodeEditor(
    noise: BrushBehavior.NoiseNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.brush_designer_node_noise),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_seed),
            value = noise.seed.toFloat(),
            limits = NumericLimits.standard(-10000f, 10000f, 1f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setNoiseNode(
                        noise.toBuilder().setSeed(it.toInt())
                    ).build()
                )
            }
        )

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_vary_over),
            currentValue = noise.varyOver,
            values = BrushBehavior.ProgressDomain.entries.filter {
                it != BrushBehavior.ProgressDomain.PROGRESS_DOMAIN_UNSPECIFIED
            },
            displayName = { it.name.replace("PROGRESS_DOMAIN_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setNoiseNode(
                        noise.toBuilder().setVaryOver(selected)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_base_period),
            value = noise.basePeriod,
            limits = NumericLimits.standard(0.01f, 5f, 0.01f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setNoiseNode(
                        noise.toBuilder().setBasePeriod(it)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun TargetNodeEditor(
    target: BrushBehavior.TargetNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_target), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_target_output),
            currentValue = target.target,
            values = BrushBehavior.Target.entries.filter {
                it != BrushBehavior.Target.TARGET_UNSPECIFIED
            },
            displayName = { it.name.replace("TARGET_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setTargetNode(
                        target.toBuilder().setTarget(selected)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_modifier_range_start),
            value = target.targetModifierRangeStart,
            limits = NumericLimits.standard(-10f, 10f, 0.05f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setTargetNode(
                        target.toBuilder().setTargetModifierRangeStart(it)
                    ).build()
                )
            }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_node_modifier_range_end),
            value = target.targetModifierRangeEnd,
            limits = NumericLimits.standard(-10f, 10f, 0.05f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setTargetNode(
                        target.toBuilder().setTargetModifierRangeEnd(it)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun ConstantNodeEditor(
    constant: BrushBehavior.ConstantNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_constant_value), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        NumericField(
            title = stringResource(R.string.brush_designer_node_value),
            value = constant.value,
            limits = NumericLimits.standard(-100f, 100f, 0.1f),
            onValueChanged = {
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setConstantNode(
                        constant.toBuilder().setValue(it)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun BinaryOpNodeEditor(
    binaryOp: BrushBehavior.BinaryOpNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_binary_operation), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_operation),
            currentValue = binaryOp.operation,
            values = BrushBehavior.BinaryOp.entries.filter {
                it != BrushBehavior.BinaryOp.BINARY_OP_UNSPECIFIED
            },
            displayName = { it.name.replace("BINARY_OP_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setBinaryOpNode(
                        binaryOp.toBuilder().setOperation(selected)
                    ).build()
                )
            }
        )
    }
}

@Composable
internal fun InterpolationNodeEditor(
    interpolation: BrushBehavior.InterpolationNode,
    onNodeChanged: (BrushBehavior.Node) -> Unit
) {
    Column {
        Text(stringResource(R.string.brush_designer_node_interpolation), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary)

        EnumDropdown(
            label = stringResource(R.string.brush_designer_node_interpolation),
            currentValue = interpolation.interpolation,
            values = BrushBehavior.Interpolation.entries.filter {
                it != BrushBehavior.Interpolation.INTERPOLATION_UNSPECIFIED
            },
            displayName = { it.name.replace("INTERPOLATION_", "") },
            onSelected = { selected ->
                onNodeChanged(
                    BrushBehavior.Node.newBuilder().setInterpolationNode(
                        interpolation.toBuilder().setInterpolation(selected)
                    ).build()
                )
            }
        )
    }
}

/** Response curve types available in the curve type selector dropdown. */
private enum class ResponseCurveType(val displayName: String) {
    Predefined("Predefined"),
    CubicBezier("Cubic Bézier"),
    Linear("Linear"),
    Steps("Steps");
}
