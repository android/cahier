@file:OptIn(
  androidx.ink.brush.ExperimentalInkCustomBrushApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.cahier.ui.brushgraph.ui

import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cahier.R
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import androidx.ink.brush.InputToolType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth

import com.example.cahier.ui.brushgraph.model.*
import com.example.cahier.ui.brushgraph.ui.TipPreviewWidget
import com.example.cahier.ui.brushgraph.ui.fields.*

/** Renders the editable fields for a node. */
@Composable
fun NodeFields(
  node: GraphNode,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onUpdate: (NodeData) -> Unit,
  textFieldsLocked: Boolean,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: androidx.ink.rendering.android.canvas.CanvasStrokeRenderer,
  onFieldEditComplete: () -> Unit = {},
  onDropdownEditComplete: () -> Unit = {},
) {
  Column(
    modifier =
      Modifier.padding(top = 8.dp).heightIn(max = 600.dp).verticalScroll(rememberScrollState())
  ) {
    when (val data = node.data) {
      is NodeData.Behavior -> {
        BehaviorNodeFields(
          data = data,
          onUpdate = onUpdate,
          onDropdownEditComplete = onDropdownEditComplete,
          onFieldEditComplete = onFieldEditComplete,
          textFieldsLocked = textFieldsLocked
        )
      }
      is NodeData.ColorFunc -> {
        ColorFuncNodeFields(
          function = data.function,
          onUpdate = onUpdate,
          onChooseColor = onChooseColor,
          onDropdownEditComplete = onDropdownEditComplete,
          onFieldEditComplete = onFieldEditComplete
        )
      }
      is NodeData.Family -> {
        FamilyNodeFields(
          data = data,
          onUpdate = onUpdate,
          onDropdownEditComplete = onDropdownEditComplete,
          textFieldsLocked = textFieldsLocked
        )
      }
      is NodeData.Tip -> {
        TipNodeFields(
          data = data,
          onUpdate = onUpdate,
          onFieldEditComplete = onFieldEditComplete,
          strokeRenderer = strokeRenderer
        )
      }
      is NodeData.Coat -> {
        CoatNodeFields()
      }
      is NodeData.Paint -> {
        PaintNodeFields(
          data = data,
          onUpdate = onUpdate,
          onDropdownEditComplete = onDropdownEditComplete
        )
      }
      is NodeData.TextureLayer -> {
        TextureLayerNodeFields(
          layer = data.layer,
          allTextureIds = allTextureIds,
          onLoadTexture = onLoadTexture,
          onUpdate = { onUpdate(it) },
          strokeRenderer = strokeRenderer
        )
      }
    }
  }
}




internal fun createDefaultNode(nodeCase: ProtoBrushBehavior.Node.NodeCase): NodeData {
  return when (nodeCase) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setSourceNode(
            ProtoBrushBehavior.SourceNode.newBuilder()
              .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
              .setSourceValueRangeStart(0f)
              .setSourceValueRangeEnd(1f)
              .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setConstantNode(ProtoBrushBehavior.ConstantNode.newBuilder().setValue(0f))
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setNoiseNode(
            ProtoBrushBehavior.NoiseNode.newBuilder()
              .setSeed(0)
              .setVaryOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
              .setBasePeriod(1f)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setToolTypeFilterNode(
            ProtoBrushBehavior.ToolTypeFilterNode.newBuilder()
              .setEnabledToolTypes(1 shl 3) // Stylus
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setDampingNode(
            ProtoBrushBehavior.DampingNode.newBuilder()
              .setDampingSource(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
              .setDampingGap(0.1f)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setResponseNode(
            ProtoBrushBehavior.ResponseNode.newBuilder()
              .setPredefinedResponseCurve(ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setIntegralNode(
            ProtoBrushBehavior.IntegralNode.newBuilder()
              .setIntegrateOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS)
              .setIntegralValueRangeStart(0f)
              .setIntegralValueRangeEnd(1f)
              .setIntegralOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setBinaryOpNode(
            ProtoBrushBehavior.BinaryOpNode.newBuilder()
              .setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setInterpolationNode(
            ProtoBrushBehavior.InterpolationNode.newBuilder()
              .setInterpolation(ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setTargetNode(
            ProtoBrushBehavior.TargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER)
              .setTargetModifierRangeStart(0f)
              .setTargetModifierRangeEnd(1f)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setPolarTargetNode(
            ProtoBrushBehavior.PolarTargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE)
              .setAngleRangeStart(0f)
              .setAngleRangeEnd(6.28f)
              .setMagnitudeRangeStart(0f)
              .setMagnitudeRangeEnd(1f)
          )
          .build()
      )
    else -> throw IllegalArgumentException("Unsupported node case: $nodeCase")
  }
}





