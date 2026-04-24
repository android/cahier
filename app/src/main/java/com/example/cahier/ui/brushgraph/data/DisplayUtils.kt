@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.data

import androidx.ink.brush.InputToolType
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushFamily as ProtoBrushFamily
import com.example.cahier.R
import ink.proto.StepPosition
import ink.proto.PredefinedEasingFunction
import com.example.cahier.developer.brushdesigner.ui.NumericLimits

fun ProtoBrushBehavior.Source.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE -> R.string.bg_source_normalized_pressure
    ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS -> R.string.bg_source_tilt
    ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS -> R.string.bg_source_tilt_x
    ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS -> R.string.bg_source_tilt_y
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS -> R.string.bg_source_orientation
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS -> R.string.bg_source_orientation_about_zero
    ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_source_speed
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_source_velocity_x
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_source_velocity_y
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS -> R.string.bg_source_direction
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS -> R.string.bg_source_direction_about_zero
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X -> R.string.bg_source_normalized_direction_x
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y -> R.string.bg_source_normalized_direction_y
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_source_distance_traveled
    ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS -> R.string.bg_source_time_of_input_s
    ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS -> R.string.bg_source_time_of_input_ms
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
      R.string.bg_source_predicted_distance_traveled
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS -> R.string.bg_source_predicted_time_elapsed_s
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS -> R.string.bg_source_predicted_time_elapsed_ms
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_source_distance_remaining
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS -> R.string.bg_source_time_since_input_s
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS -> R.string.bg_source_time_since_input_ms
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS -> R.string.bg_source_time_since_stroke_end
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_x
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_y
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_forward
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_lateral
    ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND -> R.string.bg_source_speed_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND -> R.string.bg_source_velocity_x_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND -> R.string.bg_source_velocity_y_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> R.string.bg_source_distance_traveled_absolute
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS ->
      R.string.bg_source_predicted_distance_traveled_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_source_acceleration_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_x_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_y_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_forward_absolute
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      R.string.bg_source_acceleration_lateral_absolute
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH ->
      R.string.bg_source_distance_remaining_fraction
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.Source.getNumericLimits(): NumericLimits {
  return when (this) {
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE -> NumericLimits(0f, 1f, 0.01f)
    ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(0f, 90f)
    ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS,
    ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(-90f, 90f)
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS,
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(0f, 360f)
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS,
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(-180f, 180f)
    ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> NumericLimits(0f, 1000f, 0.01f)
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> NumericLimits(-1000f, 1000f, 0.01f, "/s")
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X,
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y -> NumericLimits(-1f, 1f, 0.01f)
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(0f, 100f, 0.01f)
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(0f, 100f, 0.01f)
    ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS,
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS,
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS,
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS -> NumericLimits(0f, 10f, 0.001f, "s")
    ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS,
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS,
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS -> NumericLimits(0f, 10000f, 1f, "ms")
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> NumericLimits(0f, 100000f, 1f, "/s²")
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> NumericLimits(-100000f, 100000f, 1f, "/s²")
    ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND -> NumericLimits(0f, 100f, 0.1f, "cm/s")
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND,
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND -> NumericLimits(-100f, 100f, 0.1f, "cm/s")
    ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> NumericLimits(0f, 100f, 0.01f, "cm")
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> NumericLimits(0f, 10f, 0.01f, "cm")
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED -> NumericLimits(0f, 5000f, 0.1f, "cm/s²")
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED,
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED -> NumericLimits(-5000f, 5000f, 0.1f, "cm/s²")
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH -> NumericLimits.floatShownAsPercent(0f, 100f)
    else -> NumericLimits(-100f, 100f, 0.01f)
  }
}

fun ProtoBrushBehavior.Target.getNumericLimits(): NumericLimits {
  return when (this) {
    ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS,
    ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(-360f, 360f)
    ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS -> NumericLimits.radiansShownAsDegrees(-90f, 90f)
    ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET,
    ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET,
    ProtoBrushBehavior.Target.TARGET_LUMINOSITY -> NumericLimits.floatShownAsPercent(-100f, 100f)
    ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
    ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER,
    ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER,
    ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER,
    ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER -> NumericLimits(0f, 2f, 0.01f, "x")
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(-10.0f, 10.0f, 0.01f)
    else -> NumericLimits(-100f, 100f, 0.01f)
  }
}

fun ProtoBrushBehavior.PolarTarget.getMagnitudeLimits(): NumericLimits {
  return when (this) {
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE -> 
      NumericLimits(-10.0f, 10.0f, 0.01f)
    else -> NumericLimits(0.0f, 1.0f, 0.1f)
  }
}

enum class ProgressDomainContext {
  DAMPING,
  INTEGRAL,
  NOISE
}

fun ProtoBrushBehavior.ProgressDomain.getNumericLimits(context: ProgressDomainContext): NumericLimits {
  return when (context) {
    ProgressDomainContext.DAMPING -> when (this) {
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> NumericLimits(0f, 10f, 0.001f, "s")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> NumericLimits(0f, 100f, 0.1f, "mm")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(0f, 10f, 0.01f)
      else -> NumericLimits(0f, 100f, 1f)
    }
    ProgressDomainContext.INTEGRAL -> when (this) {
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> NumericLimits(0f, 10f, 0.001f, "s ⋅ input")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> NumericLimits(0f, 10f, 0.01f, "cm ⋅ input")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(0f, 10f, 0.01f, "⋅ input")
      else -> NumericLimits(0f, 100f, 1f)
    }
    ProgressDomainContext.NOISE -> when (this) {
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> NumericLimits(0f, 10f, 0.01f, "s")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> NumericLimits(0f, 10f, 0.1f, "cm")
      ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> NumericLimits(0f, 10f, 0.01f)
      else -> NumericLimits(0f, 100f, 1f)
    }
  }
}

fun ProtoBrushBehavior.Target.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER -> R.string.bg_target_width_multiplier
    ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER -> R.string.bg_target_height_multiplier
    ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER -> R.string.bg_target_size_multiplier
    ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS -> R.string.bg_target_slant_offset
    ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET -> R.string.bg_target_pinch_offset
    ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS -> R.string.bg_target_rotation_offset
    ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET -> R.string.bg_target_corner_rounding_offset
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_target_position_offset_x
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_target_position_offset_y
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE ->
      R.string.bg_target_position_offset_forward
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE ->
      R.string.bg_target_position_offset_lateral
    ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS -> R.string.bg_target_hue_offset
    ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER -> R.string.bg_target_saturation_multiplier
    ProtoBrushBehavior.Target.TARGET_LUMINOSITY -> R.string.bg_target_luminosity_offset
    ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER -> R.string.bg_target_opacity_multiplier
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.PolarTarget.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
      R.string.bg_polar_target_position_offset_absolute
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
      R.string.bg_polar_target_position_offset_relative
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.BinaryOp.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.BinaryOp.BINARY_OP_PRODUCT -> R.string.bg_binary_op_product
    ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM -> R.string.bg_binary_op_sum
    ProtoBrushBehavior.BinaryOp.BINARY_OP_MIN -> R.string.bg_binary_op_min
    ProtoBrushBehavior.BinaryOp.BINARY_OP_MAX -> R.string.bg_binary_op_max
    ProtoBrushBehavior.BinaryOp.BINARY_OP_AND_THEN -> R.string.bg_binary_op_and_then
    ProtoBrushBehavior.BinaryOp.BINARY_OP_OR_ELSE -> R.string.bg_binary_op_or_else
    ProtoBrushBehavior.BinaryOp.BINARY_OP_XOR_ELSE -> R.string.bg_binary_op_xor_else
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.OutOfRange.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP -> R.string.bg_out_of_range_clamp
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_REPEAT -> R.string.bg_out_of_range_repeat
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_MIRROR -> R.string.bg_out_of_range_mirror
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.ProgressDomain.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> R.string.bg_progress_domain_distance_absolute
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_progress_domain_distance_size_relative
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> R.string.bg_progress_domain_time_seconds
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.Interpolation.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP -> R.string.bg_interpolation_lerp
    ProtoBrushBehavior.Interpolation.INTERPOLATION_INVERSE_LERP -> R.string.bg_interpolation_inverse_lerp
    else -> R.string.bg_node_unknown
  }

fun StepPosition.displayStringRId(): Int =
  when (this) {
    StepPosition.STEP_POSITION_JUMP_START -> R.string.bg_step_position_jump_start
    StepPosition.STEP_POSITION_JUMP_END -> R.string.bg_step_position_jump_end
    StepPosition.STEP_POSITION_JUMP_BOTH -> R.string.bg_step_position_jump_both
    StepPosition.STEP_POSITION_JUMP_NONE -> R.string.bg_step_position_jump_none
    else -> R.string.bg_node_unknown
  }

fun PredefinedEasingFunction.displayStringRId(): Int =
  when (this) {
    PredefinedEasingFunction.PREDEFINED_EASING_LINEAR -> R.string.bg_easing_linear
    PredefinedEasingFunction.PREDEFINED_EASING_EASE -> R.string.bg_easing_ease
    PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN -> R.string.bg_easing_ease_in
    PredefinedEasingFunction.PREDEFINED_EASING_EASE_OUT -> R.string.bg_easing_ease_out
    PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN_OUT -> R.string.bg_easing_ease_in_out
    PredefinedEasingFunction.PREDEFINED_EASING_STEP_START -> R.string.bg_easing_step_start
    PredefinedEasingFunction.PREDEFINED_EASING_STEP_END -> R.string.bg_easing_step_end
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.ResponseNode.ResponseCurveCase.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE -> R.string.bg_tab_predefined
    ProtoBrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE -> R.string.bg_tab_cubic_bezier
    ProtoBrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE -> R.string.bg_tab_linear
    ProtoBrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE -> R.string.bg_tab_steps
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.ResponseNode.displayStringRId(): Int =
  this.responseCurveCase.displayStringRId()

fun InputToolType.displayStringRId(): Int =
  when (this) {
    InputToolType.UNKNOWN -> R.string.bg_tool_type_unknown
    InputToolType.MOUSE -> R.string.bg_tool_type_mouse
    InputToolType.TOUCH -> R.string.bg_tool_type_touch
    InputToolType.STYLUS -> R.string.bg_tool_type_stylus
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.SelfOverlap.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY -> R.string.bg_self_overlap_any
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE -> R.string.bg_self_overlap_accumulate
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD -> R.string.bg_self_overlap_discard
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.TextureLayer.SizeUnit.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE -> R.string.bg_size_unit_brush_size
    ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES -> R.string.bg_size_unit_stroke_coordinates
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.TextureLayer.Origin.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN -> R.string.bg_origin_stroke_space_origin
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT -> R.string.bg_origin_first_stroke_input
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT -> R.string.bg_origin_last_stroke_input
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.TextureLayer.Mapping.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING -> R.string.bg_mapping_tiling
    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING -> R.string.bg_mapping_stamping
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.TextureLayer.Wrap.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT -> R.string.bg_wrap_repeat
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR -> R.string.bg_wrap_mirror
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP -> R.string.bg_wrap_clamp
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushPaint.TextureLayer.BlendMode.displayStringRId(): Int =
  when (this) {
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC -> R.string.bg_blend_mode_src
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER -> R.string.bg_blend_mode_src_over
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP -> R.string.bg_blend_mode_src_atop
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN -> R.string.bg_blend_mode_src_in
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT -> R.string.bg_blend_mode_src_out
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST -> R.string.bg_blend_mode_dst
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER -> R.string.bg_blend_mode_dst_over
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP -> R.string.bg_blend_mode_dst_atop
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN -> R.string.bg_blend_mode_dst_in
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT -> R.string.bg_blend_mode_dst_out
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE -> R.string.bg_blend_mode_modulate
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR -> R.string.bg_blend_mode_xor
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushBehavior.Node.NodeCase.displayStringRId(): Int =
  when (this) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> R.string.bg_node_source
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> R.string.bg_node_constant
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> R.string.bg_node_noise
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> R.string.bg_node_tool_type_filter
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> R.string.bg_node_damping
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> R.string.bg_node_response
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> R.string.bg_node_integral
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> R.string.bg_node_binary_op
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> R.string.bg_node_interpolation
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> R.string.bg_node_target
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> R.string.bg_node_polar_target
    else -> R.string.bg_node_unknown
  }

fun ProtoBrushFamily.InputModel.displayStringRId(): Int =
  when {
    hasSlidingWindowModel() -> R.string.bg_model_sliding_window
    hasSpringModel() -> R.string.bg_model_spring
    hasExperimentalNaiveModel() -> R.string.bg_model_naive_experimental
    else -> R.string.bg_unknown_model
  }
