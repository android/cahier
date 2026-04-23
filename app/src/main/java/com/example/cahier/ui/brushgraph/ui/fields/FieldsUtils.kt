package com.example.cahier.ui.brushgraph.ui.fields

import androidx.ink.brush.InputToolType
import ink.proto.BrushBehavior as ProtoBrushBehavior

internal val SOURCES_INPUT = listOf(
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE,
  ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS
)

internal val SOURCES_MOVEMENT = listOf(
  ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X,
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y
)

internal val SOURCES_DISTANCE = listOf(
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH
)

internal val SOURCES_TIME = listOf(
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS
)

internal val SOURCES_ACCELERATION = listOf(
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED
)

internal val TARGETS_SIZE_SHAPE = listOf(
  ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET,
  ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET
)

internal val TARGETS_POSITION = listOf(
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE
)

internal val TARGETS_COLOR_OPACITY = listOf(
  ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_LUMINOSITY,
  ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER
)

internal val ALL_POLAR_TARGETS =
  ProtoBrushBehavior.PolarTarget.values()
    .filter { it != ProtoBrushBehavior.PolarTarget.POLAR_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

internal val ALL_BINARY_OPS =
  ProtoBrushBehavior.BinaryOp.values()
    .filter { it != ProtoBrushBehavior.BinaryOp.BINARY_OP_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

internal val ALL_OUT_OF_RANGE =
  ProtoBrushBehavior.OutOfRange.values()
    .filter { it != ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

internal val ALL_PROGRESS_DOMAINS =
  ProtoBrushBehavior.ProgressDomain.values()
    .filter {
      it != ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_UNSPECIFIED && it.ordinal >= 0
    }
    .toTypedArray()

internal val ALL_INTERPOLATIONS =
  ProtoBrushBehavior.Interpolation.values()
    .filter { it != ProtoBrushBehavior.Interpolation.INTERPOLATION_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

internal val ALL_TOOL_TYPES =
  arrayOf(InputToolType.STYLUS, InputToolType.TOUCH, InputToolType.MOUSE, InputToolType.UNKNOWN)

internal fun ProtoBrushBehavior.Source.isAngle(): Boolean {
  return this == ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS
}

internal fun ProtoBrushBehavior.Target.isAngle(): Boolean {
  return this == ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ||
         this == ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ||
         this == ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS
}

internal val NODE_TYPES_START = listOf(
  ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE,
  ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE,
  ProtoBrushBehavior.Node.NodeCase.NOISE_NODE
)

internal val NODE_TYPES_OPERATOR = listOf(
  ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE,
  ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE,
  ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE,
  ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE,
  ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE,
  ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE
)

internal val NODE_TYPES_TERMINAL = listOf(
  ProtoBrushBehavior.Node.NodeCase.TARGET_NODE,
  ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
)
