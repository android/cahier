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
package com.example.cahier.developer.brushgraph.ui

import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.ColorFunction as ProtoColorFunction
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.PredefinedEasingFunction as ProtoPredefinedEasingFunction
import ink.proto.StepPosition as ProtoStepPosition
import org.junit.Assert.assertTrue
import org.junit.Test

class TooltipsTest {

    /** Set of tests that simply verifies that all the values for various types have tooltips written for them. */

    @Test
    fun nodeDataTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        val nodes = listOf(
            NodeData.Tip(ProtoBrushTip.getDefaultInstance()),
            NodeData.Paint(ProtoBrushPaint.getDefaultInstance()),
            NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()),
            NodeData.ColorFunction(ProtoColorFunction.getDefaultInstance()),
            NodeData.Coat(),
            NodeData.Family(),

            // Behavior nodes
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setSourceNode(ProtoBrushBehavior.SourceNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setConstantNode(ProtoBrushBehavior.ConstantNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setNoiseNode(ProtoBrushBehavior.NoiseNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setToolTypeFilterNode(ProtoBrushBehavior.ToolTypeFilterNode.getDefaultInstance())
                    .build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setDampingNode(ProtoBrushBehavior.DampingNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setResponseNode(ProtoBrushBehavior.ResponseNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setInterpolationNode(ProtoBrushBehavior.InterpolationNode.getDefaultInstance())
                    .build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setIntegralNode(ProtoBrushBehavior.IntegralNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setTargetNode(ProtoBrushBehavior.TargetNode.getDefaultInstance()).build()
            ),
            NodeData.Behavior(
                ProtoBrushBehavior.Node.newBuilder()
                    .setPolarTargetNode(ProtoBrushBehavior.PolarTargetNode.getDefaultInstance()).build()
            )
        )

        for (node in nodes) {
            val tooltip = node.getTooltip()
            assertTrue("Tooltip should be unique: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun sourceEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.Source.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for Source.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun targetEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.Target.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for Target.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun wrapEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.TextureLayer.Wrap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Wrap.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun sizeUnitEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.TextureLayer.SizeUnit.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for SizeUnit.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun originEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.TextureLayer.Origin.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for Origin.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun mappingEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.TextureLayer.Mapping.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for Mapping.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun blendModeEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.TextureLayer.BlendMode.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for BlendMode.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun selfOverlapEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushPaint.SelfOverlap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for SelfOverlap.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun polarTargetEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.PolarTarget.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for PolarTarget.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun outOfRangeEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.OutOfRange.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for OutOfRange.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun binaryOpEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.BinaryOp.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for BinaryOp.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun progressDomainEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.ProgressDomain.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for ProgressDomain.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun interpolationEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoBrushBehavior.Interpolation.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for Interpolation.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun predefinedEasingFunctionEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoPredefinedEasingFunction.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for ProtoPredefinedEasingFunction.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun stepPositionEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoStepPosition.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue(
                "Tooltip should be unique for ProtoStepPosition.$value: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun inputModelTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        val models = arrayOf(
            R.string.bg_model_sliding_window,
            R.string.bg_model_passthrough
        )
        for (modelResId in models) {
            val tooltip = getInputModelTooltip(modelResId)
            assertTrue(
                "Tooltip should be unique for InputModel.$modelResId: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }

    @Test
    fun colorFunctionTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        val options = arrayOf(
            R.string.bg_target_opacity_multiplier,
            R.string.bg_replace_color,
            R.string.bg_target_chroma_multiplier,
            R.string.bg_target_hue_offset,
            R.string.bg_target_lightness_offset
        )
        for (optionResId in options) {
            val tooltip = getColorFunctionTooltip(optionResId)
            assertTrue(
                "Tooltip should be unique for ColorFunction.$optionResId: $tooltip",
                tooltips.add(tooltip)
            )
        }
    }
}
