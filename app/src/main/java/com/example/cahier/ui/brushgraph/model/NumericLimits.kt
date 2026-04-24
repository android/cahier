package com.example.cahier.ui.brushgraph.model

import ink.proto.BrushBehavior as ProtoBrushBehavior
import com.example.cahier.developer.brushdesigner.ui.NumericLimits

/** Returns the numeric limits for a given [ProtoBrushBehavior.Source]. */
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

/** Returns the numeric limits for a given [ProtoBrushBehavior.Target]. */
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

/** Returns the magnitude limits for a given [ProtoBrushBehavior.PolarTarget]. */
fun ProtoBrushBehavior.PolarTarget.getMagnitudeLimits(): NumericLimits {
  return when (this) {
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
    ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE -> 
      NumericLimits(-10.0f, 10.0f, 0.01f)
    else -> NumericLimits(0.0f, 1.0f, 0.1f)
  }
}

/** Enum for progress domain context. */
enum class ProgressDomainContext {
  DAMPING,
  INTEGRAL,
  NOISE
}

/** Returns the numeric limits for a given [ProtoBrushBehavior.ProgressDomain] based on [context]. */
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
