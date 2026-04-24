@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import ink.proto.PredefinedEasingFunction
import ink.proto.StepPosition
import ink.proto.LinearEasingFunction
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.CubicBezierEasingFunction as ProtoCubicBezier
import ink.proto.StepsEasingFunction as ProtoSteps
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun ResponseNodeFields(
  responseNode: ProtoBrushBehavior.ResponseNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier) {
    CurvePreviewWidget(
      responseNode = responseNode,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    ResponseCurveWidget(
      responseNode = responseNode,
      onResponseNodeChanged = {
        onUpdate(
          NodeData.Behavior(
            behaviorNode.toBuilder().setResponseNode(it).build()
          )
        )
      }
    )
  }
}

@Composable
fun CurvePreviewWidget(
  responseNode: ProtoBrushBehavior.ResponseNode,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier
      .height(120.dp)
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    val widthPx = constraints.maxWidth.toFloat()
    val heightPx = constraints.maxHeight.toFloat()

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
      // Background and Grid
      drawRect(backgroundColor)
      val centerY = heightPx * 0.8f
      val scaleY = heightPx * 0.6f

      // Zero line
      drawLine(outlineColor, Offset(0f, centerY), Offset(widthPx, centerY))
      // One line
      drawLine(outlineVariantColor, Offset(0f, centerY - scaleY), Offset(widthPx, centerY - scaleY))

      val path = androidx.compose.ui.graphics.Path()
      when (responseNode.responseCurveCase) {
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE -> {
          val c = responseNode.cubicBezierResponseCurve
          path.moveTo(0f, centerY)
          path.cubicTo(
            c.x1 * widthPx,
            centerY - c.y1 * scaleY,
            c.x2 * widthPx,
            centerY - c.y2 * scaleY,
            widthPx,
            centerY - scaleY
          )
        }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE -> {
          val s = responseNode.stepsResponseCurve
          val steps = s.stepCount.coerceAtLeast(1)
          val position = s.stepPosition
          path.moveTo(0f, centerY)
          val numSamples = 200
          for (i in 0..numSamples) {
            val x = i.toFloat() / numSamples
            val yVal = evaluateSteps(x, steps, position)
            val px = x * widthPx
            val py = centerY - yVal * scaleY
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
          }
        }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE -> {
          val l = responseNode.linearResponseCurve
          val xs = l.xList
          val ys = l.yList
          path.moveTo(0f, centerY)
          if (xs.isEmpty()) {
            path.lineTo(widthPx, centerY - scaleY)
          } else {
            for (i in xs.indices) {
              path.lineTo(xs[i] * widthPx, centerY - ys[i] * scaleY)
            }
            path.lineTo(widthPx, centerY - scaleY)
          }
        }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE -> {
          val func = responseNode.predefinedResponseCurve
          when (func) {
            PredefinedEasingFunction.PREDEFINED_EASING_LINEAR -> {
              path.moveTo(0f, centerY)
              path.lineTo(widthPx, centerY - scaleY)
            }
            PredefinedEasingFunction.PREDEFINED_EASING_STEP_START -> {
              path.moveTo(0f, centerY - scaleY)
              path.lineTo(widthPx, centerY - scaleY)
            }
            PredefinedEasingFunction.PREDEFINED_EASING_STEP_END -> {
              path.moveTo(0f, centerY)
              path.lineTo(widthPx, centerY)
              path.lineTo(widthPx, centerY - scaleY)
            }
            else -> {
              val (x1, y1, x2, y2) =
                when (func) {
                  PredefinedEasingFunction.PREDEFINED_EASING_EASE ->
                    listOf(0.25f, 0.1f, 0.25f, 1f)
                  PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN ->
                    listOf(0.42f, 0f, 1f, 1f)
                  PredefinedEasingFunction.PREDEFINED_EASING_EASE_OUT ->
                    listOf(0f, 0f, 0.58f, 1f)
                  PredefinedEasingFunction.PREDEFINED_EASING_EASE_IN_OUT ->
                    listOf(0.42f, 0f, 0.58f, 1f)
                  else -> listOf(0f, 0f, 1f, 1f)
                }
              path.moveTo(0f, centerY)
              path.cubicTo(
                x1 * widthPx,
                centerY - y1 * scaleY,
                x2 * widthPx,
                centerY - y2 * scaleY,
                widthPx,
                centerY - scaleY
              )
            }
          }
        }
        else -> {
          path.moveTo(0f, centerY)
          path.lineTo(widthPx, centerY - scaleY)
        }
      }
      drawPath(
        path,
        primaryColor,
        style = DrawStroke(width = 3.dp.toPx())
      )
    }
  }
}

private fun evaluateSteps(x: Float, n: Int, position: StepPosition): Float {
  val xClamped = x.coerceIn(0f, 1f)
  return when (position) {
    StepPosition.STEP_POSITION_JUMP_START -> {
      ceil(xClamped * n).coerceAtLeast(1f) / n
    }
    StepPosition.STEP_POSITION_JUMP_BOTH -> {
      floor(xClamped * (n + 1) + 1f) / (n + 1)
    }
    StepPosition.STEP_POSITION_JUMP_NONE -> {
      if (n <= 1) xClamped else floor(xClamped * (n - 1)) / (n - 1)
    }
    else -> {
      floor(xClamped * n) / n
    }
  }
}

@Composable
fun ResponseCurveWidget(
  responseNode: ProtoBrushBehavior.ResponseNode,
  onResponseNodeChanged: (ProtoBrushBehavior.ResponseNode) -> Unit,
) {
  val currentCase = responseNode.responseCurveCase
  val tabs =
    listOf(
      ProtoBrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE,
      ProtoBrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE,
      ProtoBrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE,
      ProtoBrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE,
    )

  val selectedTabIndex = tabs.indexOf(currentCase).coerceAtLeast(0)

  Column {
    TabRow(selectedTabIndex = selectedTabIndex) {
      tabs.forEachIndexed { index, case ->
        Tab(
          selected = selectedTabIndex == index,
          onClick = {
            if (currentCase != case) {
              val builder = responseNode.toBuilder()
              when (case) {
                ProtoBrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE ->
                  builder.setPredefinedResponseCurve(
                    PredefinedEasingFunction.PREDEFINED_EASING_LINEAR
                  )
                ProtoBrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE ->
                  builder.setCubicBezierResponseCurve(
                    ProtoCubicBezier.newBuilder()
                      .setX1(0.5f)
                      .setY1(0f)
                      .setX2(0.5f)
                      .setY2(1f)
                      .build()
                  )
                ProtoBrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE ->
                  builder.setStepsResponseCurve(
                    ProtoSteps.newBuilder()
                      .setStepCount(3)
                      .setStepPosition(StepPosition.STEP_POSITION_JUMP_START)
                      .build()
                  )
                ProtoBrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE ->
                  builder.setLinearResponseCurve(
                    LinearEasingFunction.getDefaultInstance()
                  )
                else -> {}
              }
              onResponseNodeChanged(builder.build())
            }
          },
          text = { Text(stringResource(case.displayStringRId())) }
        )
      }
    }

    Box(
      modifier =
        Modifier.padding(vertical = 8.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      when (currentCase) {
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE ->
          CubicBezierWidget(responseNode.cubicBezierResponseCurve) {
            onResponseNodeChanged(responseNode.toBuilder().setCubicBezierResponseCurve(it).build())
          }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE ->
          StepsWidget(responseNode.stepsResponseCurve) {
            onResponseNodeChanged(responseNode.toBuilder().setStepsResponseCurve(it).build())
          }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.PREDEFINED_RESPONSE_CURVE ->
          PredefinedFunctionWidget(responseNode.predefinedResponseCurve) {
            onResponseNodeChanged(responseNode.toBuilder().setPredefinedResponseCurve(it).build())
          }
        ProtoBrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE ->
          LinearWidget(responseNode.linearResponseCurve) {
            onResponseNodeChanged(responseNode.toBuilder().setLinearResponseCurve(it).build())
          }
        else -> Text(stringResource(R.string.bg_unknown_curve_type), modifier = Modifier.padding(16.dp))
      }
    }
  }
}

@Composable
fun LinearWidget(
  curve: LinearEasingFunction,
  onCurveChanged: (LinearEasingFunction) -> Unit,
) {
  Column(modifier = Modifier.padding(8.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(text = stringResource(R.string.bg_points), style = MaterialTheme.typography.titleSmall)
      IconButton(
        onClick = {
          val builder = curve.toBuilder()
          val newX = if (curve.xCount > 0) (curve.getX(curve.xCount - 1) + 1f) / 2f else 0.5f
          val newY = if (curve.yCount > 0) (curve.getY(curve.yCount - 1) + 1f) / 2f else 0.5f
          builder.addX(newX).addY(newY).build()
          onCurveChanged(builder.build())
        }
      ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.bg_cd_add_point))
      }
    }

    for (i in 0 until curve.xCount) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          NumericField(
            title = "X",
            value = curve.getX(i),
            limits = NumericLimits.standard(0f, 1f, 0.01f),
            onValueChanged = { newValue ->
              val builder = curve.toBuilder()
              builder.setX(i, newValue)
              onCurveChanged(builder.build())
            }
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
          NumericField(
            title = "Y",
            value = curve.getY(i),
            limits = NumericLimits.standard(-2f, 2f, 0.01f),
            onValueChanged = { newValue ->
              val builder = curve.toBuilder()
              builder.setY(i, newValue)
              onCurveChanged(builder.build())
            }
          )
        }
        IconButton(
          onClick = {
            val newXs = curve.xList.toMutableList()
            val newYs = curve.yList.toMutableList()
            if (i < newXs.size && i < newYs.size) {
              newXs.removeAt(i)
              newYs.removeAt(i)
            }
            onCurveChanged(
              curve.toBuilder()
                .clearX().addAllX(newXs)
                .clearY().addAllY(newYs)
                .build()
            )
          }
        ) {
          Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.bg_cd_remove_point))
        }
      }
    }
  }
}

@Composable
fun CubicBezierWidget(curve: ProtoCubicBezier, onCurveChanged: (ProtoCubicBezier) -> Unit) {
  Column(modifier = Modifier.padding(8.dp)) {
    NumericField(title = "x1", value = curve.x1, limits = NumericLimits.standard(0f, 1f, 0.01f), onValueChanged = { onCurveChanged(curve.toBuilder().setX1(it).build()) })
    NumericField(title = "y1", value = curve.y1, limits = NumericLimits.standard(-2f, 2f, 0.01f), onValueChanged = { onCurveChanged(curve.toBuilder().setY1(it).build()) })
    NumericField(title = "x2", value = curve.x2, limits = NumericLimits.standard(0f, 1f, 0.01f), onValueChanged = { onCurveChanged(curve.toBuilder().setX2(it).build()) })
    NumericField(title = "y2", value = curve.y2, limits = NumericLimits.standard(-2f, 2f, 0.01f), onValueChanged = { onCurveChanged(curve.toBuilder().setY2(it).build()) })
  }
}

@Composable
fun StepsWidget(curve: ProtoSteps, onCurveChanged: (ProtoSteps) -> Unit) {
  Column(modifier = Modifier.padding(8.dp)) {
    NumericField(
      title = stringResource(R.string.bg_label_step_count),
      value = curve.stepCount.toFloat(),
      limits = NumericLimits.standard(1f, 20f, 1f),
      onValueChanged = {
        onCurveChanged(curve.toBuilder().setStepCount(it.toInt()).build())
      }
    )
    EnumDropdown(
      label = stringResource(R.string.bg_step_position),
      currentValue = curve.stepPosition,
      values = StepPosition.values().filter {
        it != StepPosition.STEP_POSITION_UNSPECIFIED && it.ordinal >= 0
      }.toList(),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { position ->
        onCurveChanged(curve.toBuilder().setStepPosition(position).build())
      }
    )
  }
}

@Composable
fun PredefinedFunctionWidget(
  current: PredefinedEasingFunction,
  onChanged: (PredefinedEasingFunction) -> Unit,
) {
  EnumDropdown(
    label = stringResource(R.string.bg_predefined_function),
    currentValue = current,
    values = PredefinedEasingFunction.values().filter {
      it != PredefinedEasingFunction.PREDEFINED_EASING_UNSPECIFIED && it.ordinal >= 0
    }.toList(),
    modifier = Modifier.padding(8.dp),
    displayName = { stringResource(it.displayStringRId()) },
    onSelected = { func ->
      onChanged(func)
    }
  )
}
