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

package com.example.cahier.developer.brushgraph.ui

import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.NodeData
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushBehaviorKt
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import ink.proto.PredefinedEasingFunction as ProtoPredefinedEasingFunction
import ink.proto.StepPosition as ProtoStepPosition
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class TooltipsTest {

  /**
   * Set of tests that simply verifies that all the values for various types have tooltips written
   * for them.
   */
  @Test
  fun nodeDataTooltips_checked_areUnique() {
    val nodes =
      listOf(
        NodeData.Tip(ProtoBrushTip.getDefaultInstance()),
        NodeData.Paint(ProtoBrushPaint.getDefaultInstance()),
        NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()),
        NodeData.ColorFunction(ProtoColorFunction.getDefaultInstance()),
        NodeData.Coat(),
        NodeData.Family(),

        // Behavior nodes
        NodeData.Behavior(
          BrushBehaviorKt.node { sourceNode = ProtoBrushBehavior.SourceNode.getDefaultInstance() }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            constantNode = ProtoBrushBehavior.ConstantNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node { noiseNode = ProtoBrushBehavior.NoiseNode.getDefaultInstance() }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            toolTypeFilterNode = ProtoBrushBehavior.ToolTypeFilterNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node { dampingNode = ProtoBrushBehavior.DampingNode.getDefaultInstance() }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            responseNode = ProtoBrushBehavior.ResponseNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            binaryOpNode = ProtoBrushBehavior.BinaryOpNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            interpolationNode = ProtoBrushBehavior.InterpolationNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            integralNode = ProtoBrushBehavior.IntegralNode.getDefaultInstance()
          }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node { targetNode = ProtoBrushBehavior.TargetNode.getDefaultInstance() }
        ),
        NodeData.Behavior(
          BrushBehaviorKt.node {
            polarTargetNode = ProtoBrushBehavior.PolarTargetNode.getDefaultInstance()
          }
        ),
      )

    for (node in nodes) {
      assertTooltipIsUnique(node, nodes) { it.getTooltip() }
    }
  }

  @Test
  fun sourceEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushBehavior.Source) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.Source.entries) { it.getTooltip() }
  }

  @Test
  fun targetEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushBehavior.Target) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.Target.entries) { it.getTooltip() }
  }

  @Test
  fun wrapEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushPaint.TextureLayer.Wrap) {
    assertTooltipIsUnique(value, ProtoBrushPaint.TextureLayer.Wrap.entries) { it.getTooltip() }
  }

  @Test
  fun sizeUnitEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushPaint.TextureLayer.SizeUnit
  ) {
    assertTooltipIsUnique(value, ProtoBrushPaint.TextureLayer.SizeUnit.entries) { it.getTooltip() }
  }

  @Test
  fun originEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushPaint.TextureLayer.Origin
  ) {
    assertTooltipIsUnique(value, ProtoBrushPaint.TextureLayer.Origin.entries) { it.getTooltip() }
  }

  @Test
  fun mappingEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushPaint.TextureLayer.Mapping
  ) {
    assertTooltipIsUnique(value, ProtoBrushPaint.TextureLayer.Mapping.entries) { it.getTooltip() }
  }

  @Test
  fun blendModeEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushPaint.TextureLayer.BlendMode
  ) {
    assertTooltipIsUnique(value, ProtoBrushPaint.TextureLayer.BlendMode.entries) { it.getTooltip() }
  }

  @Test
  fun selfOverlapEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushPaint.SelfOverlap) {
    assertTooltipIsUnique(value, ProtoBrushPaint.SelfOverlap.entries) { it.getTooltip() }
  }

  @Test
  fun polarTargetEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushBehavior.PolarTarget
  ) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.PolarTarget.entries) { it.getTooltip() }
  }

  @Test
  fun outOfRangeEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushBehavior.OutOfRange) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.OutOfRange.entries) { it.getTooltip() }
  }

  @Test
  fun binaryOpEnumTooltip_checked_isUnique(@TestParameter value: ProtoBrushBehavior.BinaryOp) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.BinaryOp.entries) { it.getTooltip() }
  }

  @Test
  fun progressDomainEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushBehavior.ProgressDomain
  ) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.ProgressDomain.entries) { it.getTooltip() }
  }

  @Test
  fun interpolationEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoBrushBehavior.Interpolation
  ) {
    assertTooltipIsUnique(value, ProtoBrushBehavior.Interpolation.entries) { it.getTooltip() }
  }

  @Test
  fun predefinedEasingFunctionEnumTooltip_checked_isUnique(
    @TestParameter value: ProtoPredefinedEasingFunction
  ) {
    assertTooltipIsUnique(value, ProtoPredefinedEasingFunction.entries) { it.getTooltip() }
  }

  @Test
  fun stepPositionEnumTooltip_checked_isUnique(@TestParameter value: ProtoStepPosition) {
    assertTooltipIsUnique(value, ProtoStepPosition.entries) { it.getTooltip() }
  }

  @Test
  fun inputModelTooltips_checked_areUnique() {
    val models = listOf(R.string.bg_model_sliding_window, R.string.bg_model_passthrough)
    for (modelResId in models) {
      assertTooltipIsUnique(modelResId, models, ::getInputModelTooltip)
    }
  }

  @Test
  fun colorFunctionTooltips_checked_areUnique() {
    val options =
      listOf(
        R.string.bg_target_opacity_multiplier,
        R.string.bg_replace_color,
        R.string.bg_target_chroma_multiplier,
        R.string.bg_target_hue_offset,
        R.string.bg_target_lightness_offset,
      )
    for (optionResId in options) {
      assertTooltipIsUnique(optionResId, options, ::getColorFunctionTooltip)
    }
  }

  /** Asserts that no value in [allValues] other than [value] has the same tooltip as [value]. */
  private fun <T> assertTooltipIsUnique(value: T, allValues: List<T>, getTooltip: (T) -> Int) {
    val tooltip = getTooltip(value)
    val valuesWithSameTooltip = allValues.filter { it != value && getTooltip(it) == tooltip }
    assertTrue(
      "Tooltip for $value is also used by $valuesWithSameTooltip",
      valuesWithSameTooltip.isEmpty(),
    )
  }
}
