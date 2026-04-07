@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.model

import androidx.ink.brush.EasingFunction
import androidx.ink.brush.InputToolType
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint

fun ProtoBrushBehavior.Source.displayString(): String =
  when (this) {
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE -> "normalized pressure"
    ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS -> "tilt"
    ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS -> "tilt X"
    ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS -> "tilt Y"
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS -> "orientation"
    ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS -> "orientation about zero"
    ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> "speed"
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> "velocity X"
    ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND -> "velocity Y"
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS -> "direction"
    ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS -> "direction about zero"
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X -> "normalized direction X"
    ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y -> "normalized direction Y"
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE -> "distance traveled"
    ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS -> "time of input"
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
      "predicted distance traveled"
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS -> "predicted time elapsed"
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE -> "distance remaining"
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS -> "time since input"
    ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS -> "time since stroke end"
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      "acceleration"
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      "acceleration X"
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      "acceleration Y"
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      "acceleration forward"
    ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
      "acceleration lateral"
    ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND -> "speed (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND -> "velocity X (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND -> "velocity Y (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS -> "distance traveled (absolute)"
    ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS ->
      "predicted distance traveled (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED -> "acceleration (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      "acceleration X (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      "acceleration Y (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      "acceleration forward (absolute)"
    ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED ->
      "acceleration lateral (absolute)"
    ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH ->
      "distance remaining (fraction)"
    else -> this.toString()
  }

fun ProtoBrushBehavior.Target.displayString(): String =
  when (this) {
    ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER -> "width multiplier"
    ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER -> "height multiplier"
    ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER -> "size multiplier"
    ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS -> "slant offset"
    ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET -> "pinch offset"
    ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS -> "rotation offset"
    ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET -> "corner rounding offset"
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE -> "position offset X"
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE -> "position offset Y"
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE ->
      "position offset forward"
    ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE ->
      "position offset lateral"
    ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS -> "hue offset"
    ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER -> "saturation multiplier"
    ProtoBrushBehavior.Target.TARGET_LUMINOSITY -> "luminosity offset"
    ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER -> "opacity multiplier"
    else -> this.toString()
  }

fun ProtoBrushBehavior.PolarTarget.displayString(): String =
  when (this) {
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
      "position offset absolute"
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
      "position offset relative"
    else -> this.toString()
  }

fun ProtoBrushBehavior.BinaryOp.displayString(): String =
  when (this) {
    ProtoBrushBehavior.BinaryOp.BINARY_OP_PRODUCT -> "product"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM -> "sum"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_MIN -> "min"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_MAX -> "max"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_AND_THEN -> "and then"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_OR_ELSE -> "or else"
    ProtoBrushBehavior.BinaryOp.BINARY_OP_XOR_ELSE -> "xor else"
    else -> this.toString()
  }

fun ProtoBrushBehavior.OutOfRange.displayString(): String =
  when (this) {
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP -> "clamp"
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_REPEAT -> "repeat"
    ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_MIRROR -> "mirror"
    else -> this.toString()
  }

fun ProtoBrushBehavior.ProgressDomain.displayString(): String =
  when (this) {
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> "distance (absolute)"
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> "distance (size-relative)"
    ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> "time (seconds)"
    else -> this.toString()
  }

fun ProtoBrushBehavior.Interpolation.displayString(): String =
  when (this) {
    ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP -> "lerp"
    ProtoBrushBehavior.Interpolation.INTERPOLATION_INVERSE_LERP -> "inverse lerp"
    else -> this.toString()
  }

fun EasingFunction.displayString(): String =
  when (this) {
    is EasingFunction.CubicBezier -> "Cubic(${this.x1}, ${this.y1}, ${this.x2}, ${this.y2})"
    is EasingFunction.Steps -> "Steps(${this.stepCount}, ${this.stepPosition.displayString()})"
    is EasingFunction.Linear ->
      "Linear(${this.points.joinToString(transform={"(${it.x}, ${it.y})"})})"
    EasingFunction.Predefined.LINEAR -> "linear"
    EasingFunction.Predefined.EASE -> "ease"
    EasingFunction.Predefined.EASE_IN -> "ease in"
    EasingFunction.Predefined.EASE_OUT -> "ease out"
    EasingFunction.Predefined.EASE_IN_OUT -> "ease in out"
    EasingFunction.Predefined.STEP_START -> "step start"
    EasingFunction.Predefined.STEP_END -> "step end"
    else -> this.toString()
  }

fun EasingFunction.StepPosition.displayString(): String =
  when (this) {
    EasingFunction.StepPosition.JUMP_START -> "jump start"
    EasingFunction.StepPosition.JUMP_END -> "jump end"
    EasingFunction.StepPosition.JUMP_BOTH -> "jump both"
    EasingFunction.StepPosition.JUMP_NONE -> "jump none"
    else -> this.toString()
  }

fun InputToolType.displayString(): String =
  when (this) {
    InputToolType.UNKNOWN -> "unknown"
    InputToolType.MOUSE -> "mouse"
    InputToolType.TOUCH -> "touch"
    InputToolType.STYLUS -> "stylus"
    else -> this.toString()
  }

fun ProtoBrushPaint.SelfOverlap.displayString(): String =
  when (this) {
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY -> "any"
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE -> "accumulate"
    ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD -> "discard"
    else -> this.toString()
  }

fun ProtoBrushPaint.TextureLayer.SizeUnit.displayString(): String =
  when (this) {
    ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE -> "brush size"
    ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES -> "stroke coordinates"
    else -> this.toString()
  }

fun ProtoBrushPaint.TextureLayer.Origin.displayString(): String =
  when (this) {
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN -> "stroke space origin"
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT -> "first stroke input"
    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT -> "last stroke input"
    else -> this.toString()
  }

fun ProtoBrushPaint.TextureLayer.Mapping.displayString(): String =
  when (this) {
    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING -> "tiling"
    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING -> "stamping"
    else -> this.toString()
  }

fun ProtoBrushPaint.TextureLayer.Wrap.displayString(): String =
  when (this) {
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT -> "repeat"
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR -> "mirror"
    ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP -> "clamp"
    else -> this.toString()
  }

fun ProtoBrushPaint.TextureLayer.BlendMode.displayString(): String =
  when (this) {
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC -> "source"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER -> "source over"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP -> "source atop"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN -> "source in"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT -> "source out"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST -> "destination"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER -> "destination over"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP -> "destination atop"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN -> "destination in"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT -> "destination out"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE -> "modulate"
    ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR -> "xor"
    else -> this.toString()
  }
