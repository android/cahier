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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.PredefinedEasingFunction as ProtoPredefinedEasingFunction

/** Renders the editable fields for a node. */
@Composable
fun NodeFields(
    node: GraphNode,
    textFieldsLocked: Boolean,
    strokeRenderer: CanvasStrokeRenderer,
    allTextureIds: Set<String>,
    onChooseColor: (Color, (Color) -> Unit) -> Unit,
    onUpdate: (NodeData) -> Unit,
    onLoadTexture: () -> Unit,
    onFieldEditComplete: () -> Unit = {},
    onDropdownEditComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
              .padding(top = 8.dp)
              .heightIn(max = 600.dp)
              .verticalScroll(rememberScrollState())
    ) {
        when (val data = node.data) {
            is NodeData.Behavior -> {
                BehaviorNodeFields(
                    data = data,
                    onUpdate = onUpdate,
                    onDropdownEditComplete = onDropdownEditComplete,
                    onFieldEditComplete = onFieldEditComplete,
                    textFieldsLocked = textFieldsLocked
                )
            }

            is NodeData.ColorFunction -> {
                ColorFunctionNodeFields(
                    function = data.function,
                    onUpdate = onUpdate,
                    onChooseColor = onChooseColor,
                    onDropdownEditComplete = onDropdownEditComplete,
                    onFieldEditComplete = onFieldEditComplete
                )
            }

            is NodeData.Family -> {
                FamilyNodeFields(
                    data = data,
                    onUpdate = onUpdate,
                    onDropdownEditComplete = onDropdownEditComplete,
                    textFieldsLocked = textFieldsLocked
                )
            }

            is NodeData.Tip -> {
                TipNodeFields(
                    data = data,
                    onUpdate = onUpdate,
                    onFieldEditComplete = onFieldEditComplete,
                    strokeRenderer = strokeRenderer
                )
            }

            is NodeData.Coat -> {
                CoatNodeFields()
            }

            is NodeData.Paint -> {
                PaintNodeFields(
                    data = data,
                    onUpdate = onUpdate,
                    onDropdownEditComplete = onDropdownEditComplete
                )
            }

            is NodeData.TextureLayer -> {
                TextureLayerNodeFields(
                    layer = data.layer,
                    allTextureIds = allTextureIds,
                    onLoadTexture = onLoadTexture,
                    onUpdate = { onUpdate(it) },
                    strokeRenderer = strokeRenderer
                )
            }
        }
    }
}

internal fun createDefaultNode(nodeCase: ProtoBrushBehavior.Node.NodeCase): NodeData {
    return when (nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setSourceNode(
                        ProtoBrushBehavior.SourceNode.newBuilder()
                            .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                            .setSourceValueRangeStart(0f)
                            .setSourceValueRangeEnd(1f)
                            .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setConstantNode(ProtoBrushBehavior.ConstantNode.newBuilder().setValue(0f))
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setNoiseNode(
                        ProtoBrushBehavior.NoiseNode.newBuilder()
                            .setSeed(0)
                            .setVaryOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
                            .setBasePeriod(1f)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setToolTypeFilterNode(
                        ProtoBrushBehavior.ToolTypeFilterNode.newBuilder()
                            .setEnabledToolTypes(1 shl 3) // Stylus
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setDampingNode(
                        ProtoBrushBehavior.DampingNode.newBuilder()
                            .setDampingSource(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
                            .setDampingGap(0.1f)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setResponseNode(
                        ProtoBrushBehavior.ResponseNode.newBuilder()
                            .setPredefinedResponseCurve(ProtoPredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setIntegralNode(
                        ProtoBrushBehavior.IntegralNode.newBuilder()
                            .setIntegrateOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS)
                            .setIntegralValueRangeStart(0f)
                            .setIntegralValueRangeEnd(1f)
                            .setIntegralOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(
                        ProtoBrushBehavior.BinaryOpNode.newBuilder()
                            .setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setInterpolationNode(
                        ProtoBrushBehavior.InterpolationNode.newBuilder()
                            .setInterpolation(ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setTargetNode(
                        ProtoBrushBehavior.TargetNode.newBuilder()
                            .setTarget(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER)
                            .setTargetModifierRangeStart(0f)
                            .setTargetModifierRangeEnd(1f)
                    )
                    .build()
            )

        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE ->
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setPolarTargetNode(
                        ProtoBrushBehavior.PolarTargetNode.newBuilder()
                            .setTarget(ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE)
                            .setAngleRangeStart(0f)
                            .setAngleRangeEnd(6.28f)
                            .setMagnitudeRangeStart(0f)
                            .setMagnitudeRangeEnd(1f)
                    )
                    .build()
            )

        else -> throw IllegalArgumentException("Unsupported node case: $nodeCase")
    }
}
