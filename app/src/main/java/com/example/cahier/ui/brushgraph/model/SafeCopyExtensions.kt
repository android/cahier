@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.model

import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import ink.proto.CubicBezierEasingFunction as ProtoCubicBezier
import ink.proto.StepsEasingFunction as ProtoSteps

private inline fun <T> T.safe(block: () -> T): T =
  try {
    block()
  } catch (e: Exception) {
    this
  }

/** Safe-copy extension for [ProtoBrushTip]. */
fun ProtoBrushTip.safeCopy(
  scaleX: Float = this.scaleX,
  scaleY: Float = this.scaleY,
  cornerRounding: Float = this.cornerRounding,
  slantRadians: Float = this.slantRadians,
  pinch: Float = this.pinch,
  rotationRadians: Float = this.rotationRadians,
  particleGapDistanceScale: Float = this.particleGapDistanceScale,
  particleGapDurationSeconds: Float = this.particleGapDurationSeconds,
  behaviors: List<ProtoBrushBehavior> = this.behaviorsList,
) = safe {
  this.toBuilder()
    .setScaleX(scaleX)
    .setScaleY(scaleY)
    .setCornerRounding(cornerRounding)
    .setSlantRadians(slantRadians)
    .setPinch(pinch)
    .setRotationRadians(rotationRadians)
    .setParticleGapDistanceScale(particleGapDistanceScale)
    .setParticleGapDurationSeconds(particleGapDurationSeconds)
    .clearBehaviors()
    .addAllBehaviors(behaviors)
    .build()
}

/** Safe-copy extension for [ProtoBrushPaint]. */
fun ProtoBrushPaint.safeCopy(
  textureLayers: List<ProtoBrushPaint.TextureLayer> = this.textureLayersList,
  colorFunctions: List<ProtoColorFunction> = this.colorFunctionsList,
  selfOverlap: ProtoBrushPaint.SelfOverlap = this.selfOverlap,
) = safe {
  this.toBuilder()
    .clearTextureLayers()
    .addAllTextureLayers(textureLayers)
    .clearColorFunctions()
    .addAllColorFunctions(colorFunctions)
    .setSelfOverlap(selfOverlap)
    .build()
}

/** Safe-copy extension for [ProtoBrushPaint.TextureLayer]. */
fun ProtoBrushPaint.TextureLayer.safeCopy(
  clientTextureId: String = this.clientTextureId,
  sizeX: Float = this.sizeX,
  sizeY: Float = this.sizeY,
  offsetX: Float = this.offsetX,
  offsetY: Float = this.offsetY,
  rotationInRadians: Float = this.rotationInRadians,
  animationFrames: Int = this.animationFrames,
  animationRows: Int = this.animationRows,
  animationColumns: Int = this.animationColumns,
  animationDurationSeconds: Float = this.animationDurationSeconds,
  sizeUnit: ProtoBrushPaint.TextureLayer.SizeUnit = this.sizeUnit,
  origin: ProtoBrushPaint.TextureLayer.Origin = this.origin,
  mapping: ProtoBrushPaint.TextureLayer.Mapping = this.mapping,
  wrapX: ProtoBrushPaint.TextureLayer.Wrap = this.wrapX,
  wrapY: ProtoBrushPaint.TextureLayer.Wrap = this.wrapY,
  blendMode: ProtoBrushPaint.TextureLayer.BlendMode = this.blendMode,
) = safe {
  this.toBuilder()
    .setClientTextureId(clientTextureId)
    .setSizeX(sizeX)
    .setSizeY(sizeY)
    .setOffsetX(offsetX)
    .setOffsetY(offsetY)
    .setRotationInRadians(rotationInRadians)
    .setAnimationFrames(animationFrames)
    .setAnimationRows(animationRows)
    .setAnimationColumns(animationColumns)
    .setAnimationDurationSeconds(animationDurationSeconds)
    .setSizeUnit(sizeUnit)
    .setOrigin(origin)
    .setMapping(mapping)
    .setWrapX(wrapX)
    .setWrapY(wrapY)
    .setBlendMode(blendMode)
    .build()
}

/** Safe-copy extension for [ProtoColorFunction]. */
fun ProtoColorFunction.safeCopy(
  opacityMultiplier: Float = this.opacityMultiplier,
  replaceColor: ink.proto.Color = this.replaceColor,
) = safe {
  val builder = this.toBuilder()
  if (this.hasOpacityMultiplier()) {
    builder.setOpacityMultiplier(opacityMultiplier)
  } else if (this.hasReplaceColor()) {
    builder.setReplaceColor(replaceColor)
  }
  builder.build()
}

/** Safe-copy extension for [ProtoCubicBezier]. */
fun ProtoCubicBezier.safeCopy(
  x1: Float = this.x1,
  y1: Float = this.y1,
  x2: Float = this.x2,
  y2: Float = this.y2,
) = safe {
  this.toBuilder()
    .setX1(x1)
    .setY1(y1)
    .setX2(x2)
    .setY2(y2)
    .build()
}

/** Safe-copy extension for [ProtoSteps]. */
fun ProtoSteps.safeCopy(
  stepCount: Int = this.stepCount,
  stepPosition: ink.proto.StepPosition = this.stepPosition,
) = safe {
  this.toBuilder()
    .setStepCount(stepCount)
    .setStepPosition(stepPosition)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.Node]. */
fun ProtoBrushBehavior.Node.safeCopy(
  sourceNode: ProtoBrushBehavior.SourceNode = this.sourceNode,
  constantNode: ProtoBrushBehavior.ConstantNode = this.constantNode,
  noiseNode: ProtoBrushBehavior.NoiseNode = this.noiseNode,
  toolTypeFilterNode: ProtoBrushBehavior.ToolTypeFilterNode = this.toolTypeFilterNode,
  dampingNode: ProtoBrushBehavior.DampingNode = this.dampingNode,
  responseNode: ProtoBrushBehavior.ResponseNode = this.responseNode,
  integralNode: ProtoBrushBehavior.IntegralNode = this.integralNode,
  binaryOpNode: ProtoBrushBehavior.BinaryOpNode = this.binaryOpNode,
  interpolationNode: ProtoBrushBehavior.InterpolationNode = this.interpolationNode,
  targetNode: ProtoBrushBehavior.TargetNode = this.targetNode,
  polarTargetNode: ProtoBrushBehavior.PolarTargetNode = this.polarTargetNode,
) = safe {
  val builder = this.toBuilder()
  when (this.nodeCase) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> builder.setSourceNode(sourceNode)
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> builder.setConstantNode(constantNode)
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> builder.setNoiseNode(noiseNode)
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> builder.setToolTypeFilterNode(toolTypeFilterNode)
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> builder.setDampingNode(dampingNode)
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> builder.setResponseNode(responseNode)
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> builder.setIntegralNode(integralNode)
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> builder.setBinaryOpNode(binaryOpNode)
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> builder.setInterpolationNode(interpolationNode)
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> builder.setTargetNode(targetNode)
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> builder.setPolarTargetNode(polarTargetNode)
    else -> {}
  }
  builder.build()
}

/** Safe-copy extension for [ProtoBrushBehavior.SourceNode]. */
fun ProtoBrushBehavior.SourceNode.safeCopy(
  source: ProtoBrushBehavior.Source = this.source,
  sourceValueRangeStart: Float = this.sourceValueRangeStart,
  sourceValueRangeEnd: Float = this.sourceValueRangeEnd,
  sourceOutOfRangeBehavior: ProtoBrushBehavior.OutOfRange = this.sourceOutOfRangeBehavior,
) = safe {
  this.toBuilder()
    .setSource(source)
    .setSourceValueRangeStart(sourceValueRangeStart)
    .setSourceValueRangeEnd(sourceValueRangeEnd)
    .setSourceOutOfRangeBehavior(sourceOutOfRangeBehavior)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.ConstantNode]. */
fun ProtoBrushBehavior.ConstantNode.safeCopy(value: Float = this.value) = safe {
  this.toBuilder().setValue(value).build()
}

/** Safe-copy extension for [ProtoBrushBehavior.NoiseNode]. */
fun ProtoBrushBehavior.NoiseNode.safeCopy(
  seed: Int = this.seed.toInt(),
  varyOver: ProtoBrushBehavior.ProgressDomain = this.varyOver,
  basePeriod: Float = this.basePeriod,
) = safe {
  this.toBuilder()
    .setSeed(seed.toLong().toUInt().toInt())
    .setVaryOver(varyOver)
    .setBasePeriod(basePeriod)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.ToolTypeFilterNode]. */
fun ProtoBrushBehavior.ToolTypeFilterNode.safeCopy(
  enabledToolTypes: Int = this.enabledToolTypes,
) = safe {
  this.toBuilder().setEnabledToolTypes(enabledToolTypes).build()
}

/** Safe-copy extension for [ProtoBrushBehavior.DampingNode]. */
fun ProtoBrushBehavior.DampingNode.safeCopy(
  dampingSource: ProtoBrushBehavior.ProgressDomain = this.dampingSource,
  dampingGap: Float = this.dampingGap,
) = safe {
  this.toBuilder()
    .setDampingSource(dampingSource)
    .setDampingGap(dampingGap)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.ResponseNode]. */
fun ProtoBrushBehavior.ResponseNode.safeCopy(
  curve: Any,
) = safe {
  val builder = this.toBuilder()
  when (curve) {
    is ink.proto.PredefinedEasingFunction -> builder.setPredefinedResponseCurve(curve)
    is ProtoCubicBezier -> builder.setCubicBezierResponseCurve(curve)
    is ink.proto.LinearEasingFunction -> builder.setLinearResponseCurve(curve)
    is ProtoSteps -> builder.setStepsResponseCurve(curve)
  }
  builder.build()
}

/** Safe-copy extension for [ProtoBrushBehavior.IntegralNode]. */
fun ProtoBrushBehavior.IntegralNode.safeCopy(
  integrateOver: ProtoBrushBehavior.ProgressDomain = this.integrateOver,
  integralValueRangeStart: Float = this.integralValueRangeStart,
  integralValueRangeEnd: Float = this.integralValueRangeEnd,
  integralOutOfRangeBehavior: ProtoBrushBehavior.OutOfRange = this.integralOutOfRangeBehavior,
) = safe {
  this.toBuilder()
    .setIntegrateOver(integrateOver)
    .setIntegralValueRangeStart(integralValueRangeStart)
    .setIntegralValueRangeEnd(integralValueRangeEnd)
    .setIntegralOutOfRangeBehavior(integralOutOfRangeBehavior)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.BinaryOpNode]. */
fun ProtoBrushBehavior.BinaryOpNode.safeCopy(
  operation: ProtoBrushBehavior.BinaryOp = this.operation,
) = safe {
  this.toBuilder().setOperation(operation).build()
}

/** Safe-copy extension for [ProtoBrushBehavior.InterpolationNode]. */
fun ProtoBrushBehavior.InterpolationNode.safeCopy(
  interpolation: ProtoBrushBehavior.Interpolation = this.interpolation,
) = safe {
  this.toBuilder().setInterpolation(interpolation).build()
}

/** Safe-copy extension for [ProtoBrushBehavior.TargetNode]. */
fun ProtoBrushBehavior.TargetNode.safeCopy(
  target: ProtoBrushBehavior.Target = this.target,
  targetModifierRangeStart: Float = this.targetModifierRangeStart,
  targetModifierRangeEnd: Float = this.targetModifierRangeEnd,
) = safe {
  this.toBuilder()
    .setTarget(target)
    .setTargetModifierRangeStart(targetModifierRangeStart)
    .setTargetModifierRangeEnd(targetModifierRangeEnd)
    .build()
}

/** Safe-copy extension for [ProtoBrushBehavior.PolarTargetNode]. */
fun ProtoBrushBehavior.PolarTargetNode.safeCopy(
  target: ProtoBrushBehavior.PolarTarget = this.target,
  angleRangeStart: Float = this.angleRangeStart,
  angleRangeEnd: Float = this.angleRangeEnd,
  magnitudeRangeStart: Float = this.magnitudeRangeStart,
  magnitudeRangeEnd: Float = this.magnitudeRangeEnd,
) = safe {
  this.toBuilder()
    .setTarget(target)
    .setAngleRangeStart(angleRangeStart)
    .setAngleRangeEnd(angleRangeEnd)
    .setMagnitudeRangeStart(magnitudeRangeStart)
    .setMagnitudeRangeEnd(magnitudeRangeEnd)
    .build()
}

/** Safe-copy extension for [ProtoBrushCoat]. */
fun ProtoBrushCoat.safeCopy(
  tip: ProtoBrushTip = this.tip,
  paintPreferences: List<ProtoBrushPaint> = this.paintPreferencesList,
) = safe {
  this.toBuilder()
    .setTip(tip)
    .clearPaintPreferences()
    .addAllPaintPreferences(paintPreferences)
    .build()
}

/** Safe-copy extension for [ProtoBrushFamily]. */
fun ProtoBrushFamily.safeCopy(
  coats: List<ProtoBrushCoat> = this.coatsList,
  inputModel: ProtoBrushFamily.InputModel = this.inputModel,
  clientBrushFamilyId: String = this.clientBrushFamilyId,
  developerComment: String = this.developerComment,
) = safe {
  this.toBuilder()
    .clearCoats()
    .addAllCoats(coats)
    .setInputModel(inputModel)
    .setClientBrushFamilyId(clientBrushFamilyId)
    .setDeveloperComment(developerComment)
    .build()
}
