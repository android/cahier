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

package com.example.cahier.developer.brushgraph.data

import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.ColorFunction as ProtoColorFunction
import ink.proto.BrushFamily as ProtoBrushFamily

/** Enum representing specific starrable numeric fields. */
enum class StarredFieldType(val id: Int, val displayNameRes: Int) {
    SOURCE_RANGE_START(1, R.string.bg_label_range_start),
    SOURCE_RANGE_END(2, R.string.bg_label_range_end),
    NOISE_SEED(3, R.string.bg_label_seed),
    NOISE_BASE_PERIOD(4, R.string.bg_label_base_period),
    INTEGRAL_RANGE_START(5, R.string.bg_label_range_start),
    INTEGRAL_RANGE_END(6, R.string.bg_label_range_end),
    POLAR_ANGLE_START(7, R.string.bg_label_angle_start),
    POLAR_ANGLE_END(8, R.string.bg_label_angle_end),
    POLAR_MAG_START(9, R.string.bg_label_mag_start),
    POLAR_MAG_END(10, R.string.bg_label_mag_end),
    TARGET_RANGE_START(11, R.string.bg_label_range_start),
    TARGET_RANGE_END(12, R.string.bg_label_range_end),
    TEXTURE_SIZE_X(13, R.string.bg_label_size_x),
    TEXTURE_SIZE_Y(14, R.string.bg_label_size_y),
    TEXTURE_ANIM_ROWS(15, R.string.bg_label_animation_rows),
    TEXTURE_ANIM_COLS(16, R.string.bg_label_animation_columns),
    TEXTURE_ANIM_FRAMES(17, R.string.bg_label_animation_frames),
    TEXTURE_ANIM_DURATION(18, R.string.bg_label_animation_duration_ms),
    TEXTURE_OFFSET_X(19, R.string.bg_label_offset_x),
    TEXTURE_OFFSET_Y(20, R.string.bg_label_offset_y),
    TEXTURE_ROTATION(21, R.string.bg_label_rotation_degrees),
    TIP_SCALE_X(22, R.string.bg_label_scale_x),
    TIP_SCALE_Y(23, R.string.bg_label_scale_y),
    TIP_CORNER_ROUNDING(24, R.string.bg_label_corner_rounding),
    TIP_SLANT(25, R.string.bg_label_slant_degrees),
    TIP_PINCH(26, R.string.bg_label_pinch),
    TIP_ROTATION(27, R.string.bg_label_rotation_degrees),
    TIP_GAP_DISTANCE(28, R.string.bg_label_particle_gap_distance_scale),
    TIP_GAP_DURATION(29, R.string.bg_label_particle_gap_duration_ms),
    DAMPING_GAP(30, R.string.bg_label_damping_gap),
    CONSTANT_VALUE(31, R.string.bg_port_value),
    COLOR_OPACITY_MULTIPLIER(32, R.string.bg_label_opacity_multiplier),
    FAMILY_WINDOW_SIZE(33, R.string.brush_designer_window_size_ms),
    FAMILY_UPSAMPLING_FREQ(34, R.string.brush_designer_upsampling_frequency_hz);
    
    companion object {
        fun fromId(id: Int): StarredFieldType? = values().find { it.id == id }
    }
}

/** Data class to represent a starred field instance. */
data class StarredField(
    val nodeId: String,
    val fieldType: StarredFieldType
)

/** Get the current value of a starred field from NodeData. */
fun NodeData.getNumericFieldValue(fieldType: StarredFieldType): Float {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START -> node.sourceNode.sourceValueRangeStart
                StarredFieldType.SOURCE_RANGE_END -> node.sourceNode.sourceValueRangeEnd
                StarredFieldType.NOISE_SEED -> node.noiseNode.seed.toFloat()
                StarredFieldType.NOISE_BASE_PERIOD -> node.noiseNode.basePeriod
                StarredFieldType.INTEGRAL_RANGE_START -> node.integralNode.integralValueRangeStart
                StarredFieldType.INTEGRAL_RANGE_END -> node.integralNode.integralValueRangeEnd
                StarredFieldType.POLAR_ANGLE_START -> node.polarTargetNode.angleRangeStart
                StarredFieldType.POLAR_ANGLE_END -> node.polarTargetNode.angleRangeEnd
                StarredFieldType.POLAR_MAG_START -> node.polarTargetNode.magnitudeRangeStart
                StarredFieldType.POLAR_MAG_END -> node.polarTargetNode.magnitudeRangeEnd
                StarredFieldType.TARGET_RANGE_START -> node.targetNode.targetModifierRangeStart
                StarredFieldType.TARGET_RANGE_END -> node.targetNode.targetModifierRangeEnd
                StarredFieldType.DAMPING_GAP -> node.dampingNode.dampingGap
                StarredFieldType.CONSTANT_VALUE -> node.constantNode.value
                else -> 0f
            }
        }
        is NodeData.TextureLayer -> {
            val layer = this.layer
            when (fieldType) {
                StarredFieldType.TEXTURE_SIZE_X -> layer.sizeX
                StarredFieldType.TEXTURE_SIZE_Y -> layer.sizeY
                StarredFieldType.TEXTURE_ANIM_ROWS -> layer.animationRows.toFloat()
                StarredFieldType.TEXTURE_ANIM_COLS -> layer.animationColumns.toFloat()
                StarredFieldType.TEXTURE_ANIM_FRAMES -> layer.animationFrames.toFloat()
                StarredFieldType.TEXTURE_ANIM_DURATION -> layer.animationDurationSeconds
                StarredFieldType.TEXTURE_OFFSET_X -> layer.offsetX
                StarredFieldType.TEXTURE_OFFSET_Y -> layer.offsetY
                StarredFieldType.TEXTURE_ROTATION -> layer.rotationInRadians
                else -> 0f
            }
        }
        is NodeData.Tip -> {
            val tip = this.tip
            when (fieldType) {
                StarredFieldType.TIP_SCALE_X -> tip.scaleX
                StarredFieldType.TIP_SCALE_Y -> tip.scaleY
                StarredFieldType.TIP_CORNER_ROUNDING -> tip.cornerRounding
                StarredFieldType.TIP_SLANT -> tip.slantRadians
                StarredFieldType.TIP_PINCH -> tip.pinch
                StarredFieldType.TIP_ROTATION -> tip.rotationRadians
                StarredFieldType.TIP_GAP_DISTANCE -> tip.particleGapDistanceScale
                StarredFieldType.TIP_GAP_DURATION -> tip.particleGapDurationSeconds
                else -> 0f
            }
        }
        is NodeData.ColorFunction -> {
            val function = this.function
            when (fieldType) {
                StarredFieldType.COLOR_OPACITY_MULTIPLIER -> function.opacityMultiplier
                else -> 0f
            }
        }
        is NodeData.Family -> {
            val inputModel = this.inputModel
            val swModel = inputModel.slidingWindowModel
            when (fieldType) {
                StarredFieldType.FAMILY_WINDOW_SIZE -> swModel.windowSizeSeconds * 1000f
                StarredFieldType.FAMILY_UPSAMPLING_FREQ -> {
                    val period = swModel.experimentalUpsamplingPeriodSeconds
                    if (period == Float.POSITIVE_INFINITY || period == 0f) 0f else 1f / period
                }
                else -> 0f
            }
        }
        else -> 0f
    }
}

/** Get the limits of a starred field from NodeData. */
fun NodeData.getNumericFieldLimits(fieldType: StarredFieldType): NumericLimits {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START, StarredFieldType.SOURCE_RANGE_END -> node.sourceNode.source.getNumericLimits()
                StarredFieldType.NOISE_SEED -> NumericLimits.standard(0f, 100f, 1f)
                StarredFieldType.NOISE_BASE_PERIOD -> node.noiseNode.varyOver.getNumericLimits(ProgressDomainContext.NOISE)
                StarredFieldType.INTEGRAL_RANGE_START, StarredFieldType.INTEGRAL_RANGE_END -> node.integralNode.integrateOver.getNumericLimits(ProgressDomainContext.INTEGRAL)
                StarredFieldType.POLAR_ANGLE_START, StarredFieldType.POLAR_ANGLE_END -> NumericLimits.radiansShownAsDegrees(-360f, 360f)
                StarredFieldType.POLAR_MAG_START, StarredFieldType.POLAR_MAG_END -> node.polarTargetNode.target.getMagnitudeLimits()
                StarredFieldType.TARGET_RANGE_START, StarredFieldType.TARGET_RANGE_END -> node.targetNode.target.getNumericLimits()
                StarredFieldType.DAMPING_GAP -> node.dampingNode.dampingSource.getNumericLimits(ProgressDomainContext.DAMPING)
                StarredFieldType.CONSTANT_VALUE -> NumericLimits.standard(-100f, 100f, 0.01f)
                else -> NumericLimits.standard(0f, 1f, 0.01f)
            }
        }
        is NodeData.TextureLayer -> {
            when (fieldType) {
                StarredFieldType.TEXTURE_SIZE_X, StarredFieldType.TEXTURE_SIZE_Y -> NumericLimits.standard(0.1f, 1000f, 0.1f)
                StarredFieldType.TEXTURE_ANIM_ROWS, StarredFieldType.TEXTURE_ANIM_COLS, StarredFieldType.TEXTURE_ANIM_FRAMES -> NumericLimits.standard(1f, 100f, 1f)
                StarredFieldType.TEXTURE_ANIM_DURATION -> NumericLimits(1f, 10000f, 1f, "ms", unitScale = 1000f)
                StarredFieldType.TEXTURE_OFFSET_X, StarredFieldType.TEXTURE_OFFSET_Y -> NumericLimits.standard(-1f, 1f, 0.01f)
                StarredFieldType.TEXTURE_ROTATION -> NumericLimits.radiansShownAsDegrees(0f, 360f)
                else -> NumericLimits.standard(0f, 1f, 0.01f)
            }
        }
        is NodeData.Tip -> {
            when (fieldType) {
                StarredFieldType.TIP_SCALE_X, StarredFieldType.TIP_SCALE_Y -> NumericLimits.standard(0f, 2f, 0.01f)
                StarredFieldType.TIP_CORNER_ROUNDING -> NumericLimits.standard(0f, 1f, 0.01f)
                StarredFieldType.TIP_SLANT -> NumericLimits.radiansShownAsDegrees(-90f, 90f)
                StarredFieldType.TIP_PINCH -> NumericLimits.standard(0f, 1f, 0.01f)
                StarredFieldType.TIP_ROTATION -> NumericLimits.radiansShownAsDegrees(0f, 360f)
                StarredFieldType.TIP_GAP_DISTANCE -> NumericLimits.standard(0f, 5f, 0.01f)
                StarredFieldType.TIP_GAP_DURATION -> NumericLimits(0f, 1000f, 1f, "ms", unitScale = 1000f)
                else -> NumericLimits.standard(0f, 1f, 0.01f)
            }
        }
        is NodeData.ColorFunction -> {
            when (fieldType) {
                StarredFieldType.COLOR_OPACITY_MULTIPLIER -> NumericLimits.standard(0f, 2f, 0.01f)
                else -> NumericLimits.standard(0f, 1f, 0.01f)
            }
        }
        is NodeData.Family -> {
            when (fieldType) {
                StarredFieldType.FAMILY_WINDOW_SIZE -> NumericLimits.standard(1f, 100f, 1f)
                StarredFieldType.FAMILY_UPSAMPLING_FREQ -> NumericLimits.standard(0f, 500f, 1f)
                else -> NumericLimits.standard(0f, 1f, 0.01f)
            }
        }
        else -> NumericLimits.standard(0f, 1f, 0.01f)
    }
}

/** Update NodeData with a new value for a starred field. */
fun NodeData.updateWithNumericFieldValue(fieldType: StarredFieldType, value: Float): NodeData {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            val updatedNode = when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START -> 
                    node.toBuilder().setSourceNode(node.sourceNode.toBuilder().setSourceValueRangeStart(value).build()).build()
                StarredFieldType.SOURCE_RANGE_END -> 
                    node.toBuilder().setSourceNode(node.sourceNode.toBuilder().setSourceValueRangeEnd(value).build()).build()
                StarredFieldType.NOISE_SEED -> 
                    node.toBuilder().setNoiseNode(node.noiseNode.toBuilder().setSeed(value.toInt()).build()).build()
                StarredFieldType.NOISE_BASE_PERIOD -> 
                    node.toBuilder().setNoiseNode(node.noiseNode.toBuilder().setBasePeriod(value).build()).build()
                StarredFieldType.INTEGRAL_RANGE_START -> 
                    node.toBuilder().setIntegralNode(node.integralNode.toBuilder().setIntegralValueRangeStart(value).build()).build()
                StarredFieldType.INTEGRAL_RANGE_END -> 
                    node.toBuilder().setIntegralNode(node.integralNode.toBuilder().setIntegralValueRangeEnd(value).build()).build()
                StarredFieldType.POLAR_ANGLE_START -> 
                    node.toBuilder().setPolarTargetNode(node.polarTargetNode.toBuilder().setAngleRangeStart(value).build()).build()
                StarredFieldType.POLAR_ANGLE_END -> 
                    node.toBuilder().setPolarTargetNode(node.polarTargetNode.toBuilder().setAngleRangeEnd(value).build()).build()
                StarredFieldType.POLAR_MAG_START -> 
                    node.toBuilder().setPolarTargetNode(node.polarTargetNode.toBuilder().setMagnitudeRangeStart(value).build()).build()
                StarredFieldType.POLAR_MAG_END -> 
                    node.toBuilder().setPolarTargetNode(node.polarTargetNode.toBuilder().setMagnitudeRangeEnd(value).build()).build()
                StarredFieldType.TARGET_RANGE_START -> 
                    node.toBuilder().setTargetNode(node.targetNode.toBuilder().setTargetModifierRangeStart(value).build()).build()
                StarredFieldType.TARGET_RANGE_END -> 
                    node.toBuilder().setTargetNode(node.targetNode.toBuilder().setTargetModifierRangeEnd(value).build()).build()
                StarredFieldType.DAMPING_GAP -> 
                    node.toBuilder().setDampingNode(node.dampingNode.toBuilder().setDampingGap(value).build()).build()
                StarredFieldType.CONSTANT_VALUE -> 
                    node.toBuilder().setConstantNode(node.constantNode.toBuilder().setValue(value).build()).build()
                else -> node
            }
            this.copy(node = updatedNode)
        }
        is NodeData.TextureLayer -> {
            val layer = this.layer
            val updatedLayer = when (fieldType) {
                StarredFieldType.TEXTURE_SIZE_X -> layer.toBuilder().setSizeX(value).build()
                StarredFieldType.TEXTURE_SIZE_Y -> layer.toBuilder().setSizeY(value).build()
                StarredFieldType.TEXTURE_ANIM_ROWS -> layer.toBuilder().setAnimationRows(value.toInt()).build()
                StarredFieldType.TEXTURE_ANIM_COLS -> layer.toBuilder().setAnimationColumns(value.toInt()).build()
                StarredFieldType.TEXTURE_ANIM_FRAMES -> layer.toBuilder().setAnimationFrames(value.toInt()).build()
                StarredFieldType.TEXTURE_ANIM_DURATION -> layer.toBuilder().setAnimationDurationSeconds(value).build()
                StarredFieldType.TEXTURE_OFFSET_X -> layer.toBuilder().setOffsetX(value).build()
                StarredFieldType.TEXTURE_OFFSET_Y -> layer.toBuilder().setOffsetY(value).build()
                StarredFieldType.TEXTURE_ROTATION -> layer.toBuilder().setRotationInRadians(value).build()
                else -> layer
            }
            this.copy(layer = updatedLayer)
        }
        is NodeData.Tip -> {
            val tip = this.tip
            val updatedTip = when (fieldType) {
                StarredFieldType.TIP_SCALE_X -> tip.toBuilder().setScaleX(value).build()
                StarredFieldType.TIP_SCALE_Y -> tip.toBuilder().setScaleY(value).build()
                StarredFieldType.TIP_CORNER_ROUNDING -> tip.toBuilder().setCornerRounding(value).build()
                StarredFieldType.TIP_SLANT -> tip.toBuilder().setSlantRadians(value).build()
                StarredFieldType.TIP_PINCH -> tip.toBuilder().setPinch(value).build()
                StarredFieldType.TIP_ROTATION -> tip.toBuilder().setRotationRadians(value).build()
                StarredFieldType.TIP_GAP_DISTANCE -> tip.toBuilder().setParticleGapDistanceScale(value).build()
                StarredFieldType.TIP_GAP_DURATION -> tip.toBuilder().setParticleGapDurationSeconds(value).build()
                else -> tip
            }
            this.copy(tip = updatedTip)
        }
        is NodeData.ColorFunction -> {
            val function = this.function
            val updatedFunction = when (fieldType) {
                StarredFieldType.COLOR_OPACITY_MULTIPLIER -> function.toBuilder().setOpacityMultiplier(value).build()
                else -> function
            }
            this.copy(function = updatedFunction)
        }
        is NodeData.Family -> {
            val inputModel = this.inputModel
            val swModel = inputModel.slidingWindowModel
            val updatedModel = when (fieldType) {
                StarredFieldType.FAMILY_WINDOW_SIZE -> 
                    inputModel.toBuilder().setSlidingWindowModel(swModel.toBuilder().setWindowSizeSeconds(value / 1000f)).build()
                StarredFieldType.FAMILY_UPSAMPLING_FREQ -> {
                    val newPeriod = if (value == 0f) Float.POSITIVE_INFINITY else 1f / value
                    inputModel.toBuilder().setSlidingWindowModel(swModel.toBuilder().setExperimentalUpsamplingPeriodSeconds(newPeriod)).build()
                }
                else -> inputModel
            }
            this.copy(inputModel = updatedModel)
        }
        else -> this
    }
}
