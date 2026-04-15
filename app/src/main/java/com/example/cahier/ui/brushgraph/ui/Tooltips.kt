package com.example.cahier.ui.brushgraph.ui

import com.example.cahier.ui.brushgraph.model.NodeData
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint

/** Extension functions to provide tooltips for nodes and enums. */

fun NodeData.getTooltip(): String = when (this) {
  is NodeData.Tip ->
    "Defines the geometric shape (e.g., circle, stadium, or parallelogram) " +
    "and size of the brush tip. This shape acts as a cross-section that is " +
    "repeated or extruded along the path to create the stroke mesh."
  is NodeData.Paint ->
    "Controls the shading, color, and texturing applied to the stroke " +
    "geometry. A Coat can have a list of Paint preferences for compatibility " +
    "fallback; the first one compatible with the device and renderer is used."
  is NodeData.Behavior -> when (this.node.nodeCase) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE ->
      "Retrieves data from the device input (such as pressure, tilt, or " +
      "speed) and maps it to a normalized 0 to 1 range. This allows you to " +
      "use physical input to drive brush behavior dynamically."
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE ->
      "Produces a single, fixed numeric value that does not change. This is " +
      "useful for providing a steady baseline or default value to other nodes in the graph."
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
      "Generates a continuous random noise function with values between 0 and 1. " +
      "It creates organic, non-repeating variations based on a domain like time " +
      "or distance, allowing for natural-looking brush effects."
    ProtoBrushBehavior.Node.NodeCase.FALLBACK_FILTER_NODE ->
      "Filters out a branch of the behavior graph unless a specific input property " +
      "(like pressure or tilt) is missing from the device. This node is deprecated " +
      "in favor of more flexible fallback operations."
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE ->
      "Passes or blocks values based on the type of tool being used (e.g., stylus, " +
      "finger, or mouse). This allows you to create brush behaviors that only " +
      "apply to specific input methods."
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE ->
      "Smoothes out rapid changes in an input value, causing the output to " +
      "gradually follow the input over time or distance. This is useful for " +
      "reducing jitter and creating smoother brush strokes."
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE ->
      "Maps an input value through a custom response curve or easing function. " +
      "This allows you to reshape the input data, for example, to make a brush " +
      "more or less sensitive to pressure."
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE ->
      "Combines two input values using a standard binary operation such as addition, " +
      "multiplication, minimum, or maximum. This allows you to mix different " +
      "behavior branches together."
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE ->
      "Performs interpolation between two values based on a third control value. " +
      "For example, you can use this to blend between two different brush sizes " +
      "based on pressure."
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE ->
      "Accumulates or integrates an input value over time or distance since the " +
      "start of the stroke. This is useful for effects that build up as you draw, " +
      "like ink bleeding or texture accumulation."
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ->
      "Applies the final calculated value to a specific property of the brush tip, " +
      "such as width, height, or color. This is a terminal node, meaning it does " +
      "not pass values further but effects the actual rendering."
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE ->
      "Applies a vector modifier (angle and magnitude) to a property of the brush tip, " +
      "such as position offset. This allows for directional effects, like offsetting " +
      "the tip based on the direction of travel."
    else -> "A behavior node that modifies brush properties based on inputs and operations."
  }
  is NodeData.TextureLayer ->
    "Configures a texture layer for the paint, specifying the image, size, mapping " +
    "mode, and how it blends with the base color."
  is NodeData.ColorFunc ->
    "Configures a color function for the paint, allowing for dynamic color shifts " +
    "based on behavior graph outputs."
  is NodeData.Coat ->
    "Configures a layer of the brush, which can have its own tip and paint settings, " +
    "allowing for complex multi-layered brushes."
  is NodeData.Family ->
    "The root node representing the complete brush family. It contains one or more " +
    "coats that define the final appearance of the brush."
}

fun ProtoBrushBehavior.Source.getTooltip(): String = when (this) {
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE ->
    "Measures the stylus or touch pressure, reported in a normalized range from " +
    "0 to 1. This is the most common source for controlling brush thickness or " +
    "opacity based on how hard the user presses."
  ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS ->
    "Measures the tilt of the stylus relative to the screen, reported in radians " +
    "from 0 (perpendicular) to π/2 (parallel). This can be used to simulate " +
    "calligraphy or airbrush effects by changing tip shape based on angle."
  ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
    "Absolute speed of the modeled stroke input in multiples of the brush size per " +
    "second. This allows you to make the brush respond to how fast the user is " +
    "drawing, for example, making it thinner at high speeds."
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
    "Signed X component of the velocity of the modeled stroke input in multiples " +
    "of the brush size per second. Positive values indicate movement toward the right."
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND ->
    "Signed Y component of the velocity of the modeled stroke input in multiples " +
    "of the brush size per second. Positive values indicate movement downward."
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X ->
    "Signed X component of the modeled stroke input's current direction of travel, " +
    "normalized to the range [-1, 1]. It indicates the horizontal direction the stroke is moving."
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y ->
    "Signed Y component of the modeled stroke input's current direction of travel, " +
    "normalized to the range [-1, 1]. It indicates the vertical direction the stroke is moving."
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Distance traveled by the inputs of the current stroke, starting at 0 at the " +
    "first input, where one distance unit is equal to the brush size. This is useful " +
    "for effects that change progressively along the stroke."
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS ->
    "The time elapsed (in seconds) from when the stroke started to when this part " +
    "of the stroke was drawn. The value remains fixed for any given part of the stroke once drawn."
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS ->
    "The time elapsed (in milliseconds) from when the stroke started. This is " +
    "deprecated; use SOURCE_TIME_OF_INPUT_IN_SECONDS instead."
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Distance traveled by the inputs of the current prediction, starting at 0 at the " +
    "last non-predicted input, in multiples of the brush size."
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS ->
    "Elapsed time (in seconds) of the prediction, starting at 0 at the last non-predicted input."
  ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS ->
    "Stylus tilt along the horizontal axis, in radians. Positive values correspond " +
    "to tilt toward the right."
  ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS ->
    "Stylus tilt along the vertical axis, in radians. Positive values correspond " +
    "to tilt downward."
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS ->
    "Stylus orientation or angle relative to the screen, in radians from 0 to 2π."
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS ->
    "Stylus orientation centered around zero, in radians from -π to π."
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Estimated distance remaining to the end of the stroke, in multiples of brush " +
    "size. This changes dynamically as you draw."
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS ->
    "Time elapsed since this specific input was recorded, in seconds. This continues " +
    "to increase and can drive post-stroke animations."
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS ->
    "Angle of the direction of travel, in radians from 0 to 2π."
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS ->
    "Angle of the direction of travel, centered around zero, in radians from -π to π."
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
    "Absolute acceleration of the stroke input, in multiples of brush size per second squared."
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
    "Horizontal component of acceleration, in multiples of brush size per second squared."
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
    "Vertical component of acceleration, in multiples of brush size per second squared."
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
    "Acceleration in the direction of travel, in multiples of brush size per second squared."
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED ->
    "Acceleration perpendicular to the direction of travel, in multiples of brush size per second squared."
  ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND ->
    "Absolute speed of the pointer on the screen, in centimeters per second."
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND ->
    "Horizontal velocity of the pointer, in centimeters per second."
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND ->
    "Vertical velocity of the pointer, in centimeters per second."
  ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS ->
    "Distance traveled by the pointer on the screen, in centimeters."
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS ->
    "Predicted distance to be traveled by the pointer, in centimeters."
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED ->
    "Absolute acceleration of the pointer, in centimeters per second squared."
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED ->
    "Horizontal acceleration of the pointer, in centimeters per second squared."
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED ->
    "Vertical acceleration of the pointer, in centimeters per second squared."
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED ->
    "Acceleration of the pointer in the direction of travel, in centimeters per second squared."
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED ->
    "Acceleration of the pointer perpendicular to the direction of travel, in centimeters per second squared."
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH ->
    "Distance remaining to the end of the stroke, as a fraction of the total stroke length."
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS ->
    "Time elapsed since the stroke ended, in seconds. Useful for post-stroke effects."
  ProtoBrushBehavior.Source.SOURCE_UNSPECIFIED -> "Unspecified input source."
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS ->
    "Elapsed time of the prediction in milliseconds. This is deprecated; use " +
    "SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS instead."
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS ->
    "Time elapsed since input was recorded in milliseconds. This is deprecated; " +
    "use SOURCE_TIME_SINCE_INPUT_IN_SECONDS instead."
}

fun ProtoBrushBehavior.Target.getTooltip(): String = when (this) {
  ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER ->
    "Scales the brush tip width. If multiple behaviors target this or size multiplier, " +
    "they combine multiplicatively. The final width is clamped to a maximum of twice the baseline width."
  ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER ->
    "Scales the brush tip height. If multiple behaviors target this or size multiplier, " +
    "they combine multiplicatively. The final height is clamped to a maximum of twice the baseline height."
  ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER ->
    "A convenience target that scales both width and height of the brush tip " +
    "simultaneously, maintaining the aspect ratio."
  ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ->
    "Adds an offset to the brush tip rotation. The final rotation angle is normalized " +
    "modulo 2π. If multiple behaviors have this target, they stack additively."
  ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET ->
    "Adds an offset to the brush tip corner rounding. The final value is clamped " +
    "to the range [0, 1]. If multiple behaviors have this target, they stack additively."
  ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ->
    "Shifts the hue of the brush color. A positive offset shifts around the hue wheel " +
    "from red towards orange. If multiple behaviors have this target, they stack additively."
  ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER ->
    "Scales the saturation of the brush color. If multiple behaviors have this target, " +
    "they stack multiplicatively. The final multiplier is clamped to [0, 2]."
  ProtoBrushBehavior.Target.TARGET_LUMINOSITY ->
    "Modifies the luminosity of the brush color. An offset of +/-100% corresponds " +
    "to changing the luminosity by up to +/-100%."
  ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS ->
    "Adds an offset to the brush tip slant. This tilts the shape of the tip. If " +
    "multiple behaviors have this target, they stack additively."
  ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET ->
    "Adds an offset to the brush tip pinch. This brings the upper corners of the tip " +
    "closer together. If multiple behaviors have this target, they stack additively."
  ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER ->
    "Scales the opacity of the brush color. If multiple behaviors have this target, " +
    "they stack multiplicatively. The final multiplier is clamped to [0, 2]."
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Adds an offset to the brush tip position along the horizontal axis. The offset " +
    "is measured in multiples of the brush size."
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Adds an offset to the brush tip position along the vertical axis. The offset " +
    "is measured in multiples of the brush size."
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Moves the brush tip forward or backward along the direction of travel. The " +
    "distance is measured in multiples of the brush size."
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Moves the brush tip sideways relative to the direction of travel. The distance " +
    "is measured in multiples of the brush size."
  ProtoBrushBehavior.Target.TARGET_UNSPECIFIED -> "Unspecified target."
}

fun ProtoBrushPaint.TextureLayer.Wrap.getTooltip(): String = when (this) {
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT ->
    "Repeats the texture image horizontally and vertically, creating a tiled pattern. " +
    "This is useful for seamless textures like paper or canvas."
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR ->
    "Repeats the texture image while alternating mirror images, ensuring that " +
    "adjacent edges always match. This helps to avoid visible seams in the pattern."
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP ->
    "Clamps the texture to its edges. Points outside the texture take the color of " +
    "the nearest edge pixel, stretching the border colors outward."
  ProtoBrushPaint.TextureLayer.Wrap.WRAP_UNSPECIFIED -> "Unspecified texture wrap mode."
}

fun ProtoBrushPaint.TextureLayer.SizeUnit.getTooltip(): String = when (this) {
  ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES ->
    "Specifies the texture size in the same absolute units as the stroke input " +
    "position. This means the texture scale remains constant regardless of brush size."
  ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE ->
    "Specifies the texture size as a multiple of the brush size. This means the " +
    "texture scales up or down along with the brush width."
  else -> "Units for specifying the size of the texture on the stroke."
}

fun ProtoBrushPaint.TextureLayer.Origin.getTooltip(): String = when (this) {
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN ->
    "The texture origin is fixed at the origin of the stroke space. This ensures " +
    "that the texture remains aligned across different strokes."
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT ->
    "The texture origin is anchored to the very first input point of the stroke. " +
    "This means the pattern starts predictably at the beginning of each stroke."
  ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT ->
    "The texture origin is anchored to the last input position of the stroke. " +
    "This means the texture pattern will move along with the brush as you draw."
  else -> "Specification of the origin point to use for the texture mapping."
}

fun ProtoBrushPaint.TextureLayer.Mapping.getTooltip(): String = when (this) {
  ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING ->
    "The texture repeats as tiles across the stroke mesh. Each copy of the texture " +
    "has the same size and shape, creating a continuous pattern."
  ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING ->
    "The texture is 'stamped' onto each individual particle of the stroke. This is " +
    "intended for use with particle brushes to apply a shape to each dot."
  else -> "How the texture should be applied and mapped to the stroke geometry."
}

fun ProtoBrushPaint.TextureLayer.BlendMode.getTooltip(): String = when (this) {
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE ->
    "Multiplies the source and destination colors. This results in a darker color, " +
    "similar to multiplying layers in image editors."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN ->
    "Keeps the destination pixels that overlap with the source pixels, acting as a mask. " +
    "The source color itself is not drawn."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT ->
    "Keeps the destination pixels that do NOT overlap with the source pixels, " +
    "effectively erasing parts of the destination."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP ->
    "Draws the source color only where the destination exists, masking it to the existing shape."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN ->
    "Keeps the source pixels only where they overlap with the destination pixels, " +
    "masking the source to the destination shape."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER ->
    "Draws the source color over the destination color. This is the standard blending " +
    "mode most commonly used."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER ->
    "Draws the destination color over the source color. The source color is drawn " +
    "behind the existing content."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC ->
    "Keeps the source color and discards the destination color. This effectively " +
    "ignores the background or previous layers."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST ->
    "Keeps the destination color and discards the source color. This effectively " +
    "ignores the current layer."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT ->
    "Keeps the source color only where it does NOT overlap with the destination. " +
    "This can be used to cut out shapes."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP ->
    "Draws the destination color only where the source exists, masking it to the new shape."
  ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR ->
    "Combines source and destination but clears areas where they intersect. This " +
    "creates a visual effect where overlapping parts disappear."
  else -> "How the texture color combines with the paint color."
}

fun ProtoBrushPaint.SelfOverlap.getTooltip(): String = when (this) {
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY ->
    "The default behavior, which may vary by implementation. It allows the renderer " +
    "to choose the most efficient method."
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE ->
    "Overlapping parts of a single stroke build up opacity and color " +
    "density, mimicking physical ink buildup like a highlighter or " +
    "watercolor where the path crosses itself."
  ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD ->
    "The stroke maintains uniform opacity and color even where it crosses " +
    "over itself, rendering as a flat, continuous shape without darkening " +
    "at intersections."
  else -> "How overlapping parts of the same stroke are rendered."
}

fun ProtoBrushBehavior.PolarTarget.getTooltip(): String = when (this) {
  ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
    "Adds a position offset in absolute stroke space. The angle determines the " +
    "direction (0 is positive X-axis) and magnitude determines the distance in " +
    "multiples of the brush size."
  ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ->
    "Adds a position offset relative to the current direction of travel. An angle " +
    "of zero is forward, allowing you to create trailing or leading effects."
  ProtoBrushBehavior.PolarTarget.POLAR_UNSPECIFIED -> "Unspecified polar target."
}

fun ProtoBrushBehavior.OutOfRange.getTooltip(): String = when (this) {
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP ->
    "Values outside the specified range are clamped to the minimum or maximum " +
    "boundary value. This prevents the output from exceeding the desired limits."
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_REPEAT ->
    "Values outside the range repeat by wrapping around to the other side, creating " +
    "a repeating pattern or cycle."
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_MIRROR ->
    "Values outside the range repeat in a mirrored fashion, bouncing back and forth " +
    "between the boundaries. This creates a smooth, oscillating effect."
  ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_UNSPECIFIED -> "Unspecified out of range behavior."
}

fun ProtoBrushBehavior.OptionalInputProperty.getTooltip(): String = when (this) {
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_PRESSURE -> "Stylus or touch pressure (Deprecated)."
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_TILT -> "Stylus tilt (Deprecated)."
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_ORIENTATION -> "Stylus orientation (Deprecated)."
  ProtoBrushBehavior.OptionalInputProperty.OPTIONAL_INPUT_TILT_X_AND_Y -> "Stylus tilt along X and Y axes (Deprecated)."
  else -> "Optional input property (Deprecated)."
}

fun ProtoBrushBehavior.BinaryOp.getTooltip(): String = when (this) {
  ProtoBrushBehavior.BinaryOp.BINARY_OP_PRODUCT ->
    "Multiplies value A by value B. If either value is missing (null), the result " +
    "is also missing."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM ->
    "Adds value A to value B. If either value is missing (null), the result is also missing."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_MIN ->
    "Returns the smaller of the two values, A and B. If either value is missing, " +
    "the result is missing."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_MAX ->
    "Returns the larger of the two values, A and B. If either value is missing, " +
    "the result is missing."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_AND_THEN ->
    "Returns value B only if value A is present (not null). If A is missing, it " +
    "returns missing, allowing for conditional execution."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_OR_ELSE ->
    "Returns value A if it is present. If A is missing, it falls back and returns " +
    "value B instead."
  ProtoBrushBehavior.BinaryOp.BINARY_OP_XOR_ELSE ->
    "Returns A if B is missing, or B if A is missing. If both are present or both " +
    "are missing, it returns missing."
  else -> "Binary operation to combine two values."
}

fun ProtoBrushBehavior.ProgressDomain.getTooltip(): String = when (this) {
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS ->
    "Damping or noise variation occurs over time, measured in seconds. This creates " +
    "time-dependent effects that continue even if you stop moving."
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS ->
    "Damping or noise variation occurs over physical distance traveled on the screen, " +
    "measured in centimeters. This requires screen calibration data to be accurate."
  ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE ->
    "Damping or noise variation occurs over distance traveled, measured in multiples " +
    "of the brush size. This scales the effect naturally with the brush width."
  else -> "Domain for measuring damping or noise progress."
}

fun ProtoBrushBehavior.Interpolation.getTooltip(): String = when (this) {
  ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP ->
    "Linear interpolation (LERP). Uses the first input value as a percentage (0 to 1) " +
    "to blend between the second value and the third value."
  ProtoBrushBehavior.Interpolation.INTERPOLATION_INVERSE_LERP ->
    "Inverse linear interpolation. Calculates where the first value lies within the " +
    "range defined by the second and third values, returning a 0 to 1 percentage."
  else -> "Interpolation function to combine values."
}

fun ink.proto.PredefinedEasingFunction.getTooltip(): String = when (this) {
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR ->
    "Linear easing function. It returns the input value unchanged, creating a " +
    "direct, proportional response."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_EASE ->
    "Standard 'ease' function. It starts slowly, accelerates in the middle, and " +
    "slows down again at the end."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN ->
    "Standard 'ease-in' function. It starts slowly and accelerates towards the end."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_EASE_OUT ->
    "Standard 'ease-out' function. It starts quickly and decelerates towards the end."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN_OUT ->
    "Standard 'ease-in-out' function. It combines ease-in and ease-out, starting " +
    "and ending slowly."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_STEP_START ->
    "Step-start function. The value jumps immediately to the final value at the " +
    "very beginning of the interval."
  ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_STEP_END ->
    "Step-end function. The value remains at the initial value until the very end " +
    "of the interval, then jumps to the final value."
  else -> "Predefined easing function to shape the response curve."
}

fun ink.proto.StepPosition.getTooltip(): String = when (this) {
  ink.proto.StepPosition.STEP_POSITION_JUMP_START ->
    "The step function jumps at the start of each interval, meaning the value " +
    "changes at the beginning of the step."
  ink.proto.StepPosition.STEP_POSITION_JUMP_END ->
    "The step function jumps at the end of each interval, meaning the value changes " +
    "at the end of the step."
  ink.proto.StepPosition.STEP_POSITION_JUMP_NONE ->
    "The step function does not jump at either boundary, maintaining a smoother " +
    "transition between steps."
  ink.proto.StepPosition.STEP_POSITION_JUMP_BOTH ->
    "The step function jumps at both the start and the end of the interval."
  else -> "Step position behavior for step easing functions."
}

fun getInputModelTooltip(modelName: String): String = when (modelName) {
  "Spring Model" ->
    "A model that simulates a physical spring to smooth inputs. It creates a natural, " +
    "fluid feeling by adding slight inertia to the stroke."
  "Sliding Window Model" ->
    "Averages nearby inputs together within a sliding time window. This creates smooth " +
    "strokes by blending recent inputs."
  "Naive Experimental Model" ->
    "A simple model that passes through raw inputs mostly unchanged. This is experimental " +
    "and may result in less smooth strokes."
  else -> "Selects the model used to smooth raw hardware inputs."
}

fun getColorFunctionTooltip(option: String): String = when (option) {
  "Opacity Multiplier" ->
    "Multiplies the opacity of the stroke by a calculated value, allowing for " +
    "dynamic transparency effects."
  "Replace Color" ->
    "Replaces the brush color with a specified color, ignoring the baseline color " +
    "for this function's output."
  else -> "Selects the type of color function to apply."
}
