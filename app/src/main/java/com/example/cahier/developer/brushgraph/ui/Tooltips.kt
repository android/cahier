package com.example.cahier.developer.brushgraph.ui

import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint
import com.example.cahier.R
import ink.proto.PredefinedEasingFunction
import ink.proto.StepPosition

/** Extension functions to provide tooltips for nodes and enums. */

fun NodeData.getTooltip(): Int = when (this) {
  is NodeData.Tip -> R.string.bg_tooltip_node_tip
  is NodeData.Paint -> R.string.bg_tooltip_node_paint
  is NodeData.Behavior -> when (this.node.nodeCase) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> R.string.bg_tooltip_node_source
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> R.string.bg_tooltip_node_constant
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> R.string.bg_tooltip_node_noise
    ProtoBrushBehavior.Node.NodeCase.FALLBACK_FILTER_NODE -> R.string.bg_tooltip_node_fallback_filter
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> R.string.bg_tooltip_node_tool_type_filter
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> R.string.bg_tooltip_node_damping
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> R.string.bg_tooltip_node_response
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> R.string.bg_tooltip_node_binary_op
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> R.string.bg_tooltip_node_interpolation
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> R.string.bg_tooltip_node_integral
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> R.string.bg_tooltip_node_target
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> R.string.bg_tooltip_node_polar_target
    else -> R.string.bg_tooltip_node_unknown
  }
  is NodeData.TextureLayer -> R.string.bg_tooltip_node_texture_layer
  is NodeData.ColorFunction -> R.string.bg_tooltip_node_color_func
  is NodeData.Coat -> R.string.bg_tooltip_node_coat
  is NodeData.Family -> R.string.bg_tooltip_node_family
}

fun ProtoBrushBehavior.Source.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE -> R.string.bg_tooltip_source_pressure
  ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS -> R.string.bg_tooltip_source_tilt
  ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_tooltip_source_speed
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_tooltip_source_velocity_x
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> R.string.bg_tooltip_source_velocity_y
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X -> R.string.bg_tooltip_source_direction_x
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y -> R.string.bg_tooltip_source_direction_y
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_source_distance_traveled
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS -> R.string.bg_tooltip_source_time_of_input_s
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS -> R.string.bg_tooltip_source_time_of_input_ms
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_source_predicted_distance_traveled
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS -> R.string.bg_tooltip_source_predicted_time_elapsed_s
  ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS -> R.string.bg_tooltip_source_tilt_x
  ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS -> R.string.bg_tooltip_source_tilt_y
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS -> R.string.bg_tooltip_source_orientation
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS -> R.string.bg_tooltip_source_orientation_about_zero
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_source_distance_remaining
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS -> R.string.bg_tooltip_source_time_since_input_s
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS -> R.string.bg_tooltip_source_direction
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS -> R.string.bg_tooltip_source_direction_about_zero
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_x
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_y
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_forward
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_lateral
  ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND -> R.string.bg_tooltip_source_speed_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND -> R.string.bg_tooltip_source_velocity_x_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND -> R.string.bg_tooltip_source_velocity_y_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> R.string.bg_tooltip_source_distance_traveled_absolute
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> R.string.bg_tooltip_source_predicted_distance_traveled_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_x_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_y_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_forward_absolute
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED -> R.string.bg_tooltip_source_acceleration_lateral_absolute
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH -> R.string.bg_tooltip_source_distance_remaining_fraction
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS -> R.string.bg_tooltip_source_time_since_stroke_end
  ProtoBrushBehavior.Source.SOURCE_UNSPECIFIED -> R.string.bg_tooltip_source_unspecified
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS -> R.string.bg_tooltip_source_predicted_time_elapsed_ms
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS -> R.string.bg_tooltip_source_time_since_input_ms
}

fun ProtoBrushBehavior.Target.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER -> R.string.bg_tooltip_target_width_multiplier
  ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER -> R.string.bg_tooltip_target_height_multiplier
  ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER -> R.string.bg_tooltip_target_size_multiplier
  ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS -> R.string.bg_tooltip_target_rotation_offset
  ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET -> R.string.bg_tooltip_target_corner_rounding_offset
  ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS -> R.string.bg_tooltip_target_hue_offset
  ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER -> R.string.bg_tooltip_target_saturation_multiplier
  ProtoBrushBehavior.Target.TARGET_LUMINOSITY -> R.string.bg_tooltip_target_luminosity
  ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS -> R.string.bg_tooltip_target_slant_offset
  ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET -> R.string.bg_tooltip_target_pinch_offset
  ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER -> R.string.bg_tooltip_target_opacity_multiplier
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_target_position_offset_x
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_target_position_offset_y
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_target_position_offset_forward
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_target_position_offset_lateral
  ProtoBrushBehavior.Target.TARGET_UNSPECIFIED -> R.string.bg_tooltip_target_unspecified
}

fun ProtoBrushPaint.TextureLayer.Wrap.getTooltip(): Int = when (this) {
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT -> R.string.bg_tooltip_wrap_repeat
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR -> R.string.bg_tooltip_wrap_mirror
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP -> R.string.bg_tooltip_wrap_clamp
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_UNSPECIFIED -> R.string.bg_tooltip_wrap_unspecified
}

fun ProtoBrushPaint.TextureLayer.SizeUnit.getTooltip(): Int = when (this) {
  ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES -> R.string.bg_tooltip_size_unit_stroke_coordinates
  ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE -> R.string.bg_tooltip_size_unit_brush_size
  else -> R.string.bg_tooltip_size_unit_default
}

fun ProtoBrushPaint.TextureLayer.Origin.getTooltip(): Int = when (this) {
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN -> R.string.bg_tooltip_origin_stroke_space_origin
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT -> R.string.bg_tooltip_origin_first_stroke_input
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT -> R.string.bg_tooltip_origin_last_stroke_input
  else -> R.string.bg_tooltip_origin_default
}

fun ProtoBrushPaint.TextureLayer.Mapping.getTooltip(): Int = when (this) {
  ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING -> R.string.bg_tooltip_mapping_tiling
  ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING -> R.string.bg_tooltip_mapping_stamping
  else -> R.string.bg_tooltip_mapping_default
}

fun ProtoBrushPaint.TextureLayer.BlendMode.getTooltip(): Int = when (this) {
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE -> R.string.bg_tooltip_blend_mode_modulate
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN -> R.string.bg_tooltip_blend_mode_dst_in
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT -> R.string.bg_tooltip_blend_mode_dst_out
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP -> R.string.bg_tooltip_blend_mode_src_atop
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN -> R.string.bg_tooltip_blend_mode_src_in
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER -> R.string.bg_tooltip_blend_mode_src_over
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER -> R.string.bg_tooltip_blend_mode_dst_over
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC -> R.string.bg_tooltip_blend_mode_src
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST -> R.string.bg_tooltip_blend_mode_dst
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT -> R.string.bg_tooltip_blend_mode_src_out
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP -> R.string.bg_tooltip_blend_mode_dst_atop
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR -> R.string.bg_tooltip_blend_mode_xor
  else -> R.string.bg_tooltip_blend_mode_default
}

fun ProtoBrushPaint.SelfOverlap.getTooltip(): Int = when (this) {
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY -> R.string.bg_tooltip_self_overlap_any
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE -> R.string.bg_tooltip_self_overlap_accumulate
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD -> R.string.bg_tooltip_self_overlap_discard
  else -> R.string.bg_tooltip_self_overlap_default
}

fun ProtoBrushBehavior.PolarTarget.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_polar_target_absolute
  ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_polar_target_relative
  ProtoBrushBehavior.PolarTarget.POLAR_UNSPECIFIED -> R.string.bg_tooltip_polar_target_unspecified
}

fun ProtoBrushBehavior.OutOfRange.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP -> R.string.bg_tooltip_out_of_range_clamp
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_REPEAT -> R.string.bg_tooltip_out_of_range_repeat
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_MIRROR -> R.string.bg_tooltip_out_of_range_mirror
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_UNSPECIFIED -> R.string.bg_tooltip_out_of_range_unspecified
}

fun ProtoBrushBehavior.OptionalInputProperty.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_PRESSURE -> R.string.bg_tooltip_optional_input_pressure
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_TILT -> R.string.bg_tooltip_optional_input_tilt
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_ORIENTATION -> R.string.bg_tooltip_optional_input_orientation
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_TILT_X_AND_Y -> R.string.bg_tooltip_optional_input_tilt_x_y
  else -> R.string.bg_tooltip_optional_input_default
}

fun ProtoBrushBehavior.BinaryOp.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.BinaryOp.BINARY_OP_PRODUCT -> R.string.bg_tooltip_binary_op_product
  ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM -> R.string.bg_tooltip_binary_op_sum
  ProtoBrushBehavior.BinaryOp.BINARY_OP_MIN -> R.string.bg_tooltip_binary_op_min
  ProtoBrushBehavior.BinaryOp.BINARY_OP_MAX -> R.string.bg_tooltip_binary_op_max
  ProtoBrushBehavior.BinaryOp.BINARY_OP_AND_THEN -> R.string.bg_tooltip_binary_op_and_then
  ProtoBrushBehavior.BinaryOp.BINARY_OP_OR_ELSE -> R.string.bg_tooltip_binary_op_or_else
  ProtoBrushBehavior.BinaryOp.BINARY_OP_XOR_ELSE -> R.string.bg_tooltip_binary_op_xor_else
  else -> R.string.bg_tooltip_binary_op_default
}

fun ProtoBrushBehavior.ProgressDomain.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> R.string.bg_tooltip_progress_domain_time
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> R.string.bg_tooltip_progress_domain_distance_cm
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> R.string.bg_tooltip_progress_domain_distance_size
  else -> R.string.bg_tooltip_progress_domain_default
}

fun ProtoBrushBehavior.Interpolation.getTooltip(): Int = when (this) {
  ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP -> R.string.bg_tooltip_interpolation_lerp
  ProtoBrushBehavior.Interpolation.INTERPOLATION_INVERSE_LERP -> R.string.bg_tooltip_interpolation_inverse_lerp
  else -> R.string.bg_tooltip_interpolation_default
}

fun PredefinedEasingFunction.getTooltip(): Int = when (this) {
  PredefinedEasingFunction.PREDEFINED_EASING_LINEAR -> R.string.bg_tooltip_easing_linear
  PredefinedEasingFunction.PREDEFINED_EASING_EASE -> R.string.bg_tooltip_easing_ease
  PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN -> R.string.bg_tooltip_easing_ease_in
  PredefinedEasingFunction.PREDEFINED_EASING_EASE_OUT -> R.string.bg_tooltip_easing_ease_out
  PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN_OUT -> R.string.bg_tooltip_easing_ease_in_out
  PredefinedEasingFunction.PREDEFINED_EASING_STEP_START -> R.string.bg_tooltip_easing_step_start
  PredefinedEasingFunction.PREDEFINED_EASING_STEP_END -> R.string.bg_tooltip_easing_step_end
  else -> R.string.bg_tooltip_easing_default
}

fun StepPosition.getTooltip(): Int = when (this) {
  StepPosition.STEP_POSITION_JUMP_START -> R.string.bg_tooltip_step_position_jump_start
  StepPosition.STEP_POSITION_JUMP_END -> R.string.bg_tooltip_step_position_jump_end
  StepPosition.STEP_POSITION_JUMP_NONE -> R.string.bg_tooltip_step_position_jump_none
  StepPosition.STEP_POSITION_JUMP_BOTH -> R.string.bg_tooltip_step_position_jump_both
  else -> R.string.bg_tooltip_step_position_default
}

fun getInputModelTooltip(resId: Int): Int = when (resId) {
  R.string.bg_model_spring -> R.string.bg_tooltip_model_spring
  R.string.bg_model_sliding_window -> R.string.bg_tooltip_model_sliding_window
  R.string.bg_model_naive_experimental -> R.string.bg_tooltip_model_naive_experimental
  else -> R.string.bg_tooltip_model_default
}

fun getColorFunctionTooltip(resId: Int): Int = when (resId) {
  R.string.bg_opacity_multiplier -> R.string.bg_tooltip_color_func_opacity
  R.string.bg_replace_color -> R.string.bg_tooltip_color_func_replace
  else -> R.string.bg_tooltip_color_func_default
}
