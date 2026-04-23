package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.ui.brushdesigner.BrushSliderControl
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TipPreviewWidget
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import ink.proto.BrushTip as ProtoBrushTip

@Composable
fun TipNodeFields(
  data: NodeData.Tip,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier
) {
  val tip = data.tip
  TipPreviewWidget(tip, strokeRenderer)

  BrushSliderControl(
    label = stringResource(R.string.bg_label_scale_x),
    value = tip.scaleX,
    valueRange = 0f..2f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleX = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_scale_y),
    value = tip.scaleY,
    valueRange = 0f..2f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleY = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_corner_rounding),
    value = tip.cornerRounding,
    valueRange = 0f..1f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(cornerRounding = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_slant_degrees),
    value = Math.toDegrees(tip.slantRadians.toDouble()).toFloat(),
    valueRange = -90f..90f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(slantRadians = Math.toRadians(it.toDouble()).toFloat()), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_pinch),
    value = tip.pinch,
    valueRange = 0f..1f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(pinch = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_rotation_degrees),
    value = Math.toDegrees(tip.rotationRadians.toDouble()).toFloat(),
    valueRange = 0f..360f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(rotationRadians = Math.toRadians(it.toDouble()).toFloat()), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_particle_gap_distance_scale),
    value = tip.particleGapDistanceScale,
    valueRange = 0f..5f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDistanceScale = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  BrushSliderControl(
    label = stringResource(R.string.bg_label_particle_gap_duration_ms),
    value = tip.particleGapDurationSeconds * 1000f,
    valueRange = 0f..1000f,
    onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDurationSeconds = it / 1000f), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
}
