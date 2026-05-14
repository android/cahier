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

import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior
import ink.proto.BrushPaint
import ink.proto.PredefinedEasingFunction as ProtoPredefinedEasingFunction
import ink.proto.StepPosition as ProtoStepPosition
import com.example.cahier.R
import org.junit.Assert.assertTrue
import org.junit.Test

class TooltipsTest {

    /** Set of tests that simply verifies that all the values for various types have tooltips written for them. */

    @Test
    fun nodeDataTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        val nodes = listOf(
            NodeData.Tip(ink.proto.BrushTip.getDefaultInstance()),
            NodeData.Paint(ink.proto.BrushPaint.getDefaultInstance()),
            NodeData.TextureLayer(ink.proto.BrushPaint.TextureLayer.getDefaultInstance()),
            NodeData.ColorFunction(ink.proto.ColorFunction.getDefaultInstance()),
            NodeData.Coat(),
            NodeData.Family(),
            
            // Behavior nodes
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setSourceNode(BrushBehavior.SourceNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setConstantNode(BrushBehavior.ConstantNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setNoiseNode(BrushBehavior.NoiseNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setToolTypeFilterNode(BrushBehavior.ToolTypeFilterNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setDampingNode(BrushBehavior.DampingNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setResponseNode(BrushBehavior.ResponseNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setBinaryOpNode(BrushBehavior.BinaryOpNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setInterpolationNode(BrushBehavior.InterpolationNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setIntegralNode(BrushBehavior.IntegralNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setTargetNode(BrushBehavior.TargetNode.getDefaultInstance()).build()),
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setPolarTargetNode(BrushBehavior.PolarTargetNode.getDefaultInstance()).build())
        )

        for (node in nodes) {
            val tooltip = node.getTooltip()
            assertTrue("Tooltip should be unique: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun sourceEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Source.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Source.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun targetEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Target.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Target.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun wrapEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Wrap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Wrap.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun sizeUnitEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.SizeUnit.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for SizeUnit.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun originEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Origin.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Origin.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun mappingEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Mapping.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Mapping.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun blendModeEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.BlendMode.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for BlendMode.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun selfOverlapEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.SelfOverlap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for SelfOverlap.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun polarTargetEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.PolarTarget.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for PolarTarget.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun outOfRangeEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.OutOfRange.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for OutOfRange.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun binaryOpEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.BinaryOp.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for BinaryOp.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun progressDomainEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.ProgressDomain.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for ProgressDomain.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun interpolationEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Interpolation.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Interpolation.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun predefinedEasingFunctionEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoPredefinedEasingFunction.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for ProtoPredefinedEasingFunction.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun stepPositionEnumsTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in ProtoStepPosition.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for ProtoStepPosition.$value: $tooltip", tooltips.add(tooltip))
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
            assertTrue("Tooltip should be unique for InputModel.$modelResId: $tooltip", tooltips.add(tooltip))
        }
    }
    @Test
    fun colorFunctionTooltips_checked_areUnique() {
        val tooltips = mutableSetOf<Int>()
        val options = arrayOf(
            R.string.bg_opacity_multiplier,
            R.string.bg_replace_color
        )
        for (optionResId in options) {
            val tooltip = getColorFunctionTooltip(optionResId)
            assertTrue("Tooltip should be unique for ColorFunction.$optionResId: $tooltip", tooltips.add(tooltip))
        }
    }
}
