package com.example.cahier.developer.brushgraph.ui

import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior
import ink.proto.BrushPaint
import ink.proto.PredefinedEasingFunction
import ink.proto.StepPosition
import com.example.cahier.R
import org.junit.Assert.assertTrue
import org.junit.Test

class TooltipsTest {

    @Test
    fun testNodeDataTooltipsAreUnique() {
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
            NodeData.Behavior(BrushBehavior.Node.newBuilder().setFallbackFilterNode(BrushBehavior.FallbackFilterNode.getDefaultInstance()).build()),
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
    fun testSourceEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Source.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Source.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testTargetEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Target.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Target.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testWrapEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Wrap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Wrap.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testSizeUnitEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.SizeUnit.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for SizeUnit.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testOriginEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Origin.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Origin.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testMappingEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.Mapping.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Mapping.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testBlendModeEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.TextureLayer.BlendMode.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for BlendMode.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testSelfOverlapEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushPaint.SelfOverlap.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for SelfOverlap.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testPolarTargetEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.PolarTarget.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for PolarTarget.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testOutOfRangeEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.OutOfRange.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for OutOfRange.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testOptionalInputPropertyEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.OptionalInputProperty.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for OptionalInputProperty.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testBinaryOpEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.BinaryOp.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for BinaryOp.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testProgressDomainEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.ProgressDomain.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for ProgressDomain.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testInterpolationEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in BrushBehavior.Interpolation.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for Interpolation.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testPredefinedEasingFunctionEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in PredefinedEasingFunction.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for PredefinedEasingFunction.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testStepPositionEnumsTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        for (value in StepPosition.values()) {
            if (value.name == "UNRECOGNIZED") continue
            val tooltip = value.getTooltip()
            assertTrue("Tooltip should be unique for StepPosition.$value: $tooltip", tooltips.add(tooltip))
        }
    }

    @Test
    fun testInputModelTooltipsAreUnique() {
        val tooltips = mutableSetOf<Int>()
        val models = arrayOf(
            R.string.bg_model_sliding_window,
            R.string.bg_model_spring,
            R.string.bg_model_naive_experimental
        )
        for (modelResId in models) {
            val tooltip = getInputModelTooltip(modelResId)
            assertTrue("Tooltip should be unique for InputModel.$modelResId: $tooltip", tooltips.add(tooltip))
        }
    }
    @Test
    fun testColorFunctionTooltipsAreUnique() {
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
