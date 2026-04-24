package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
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

  NumericField(
    title = stringResource(R.string.bg_label_scale_x),
    value = tip.scaleX,
    limits = NumericLimits.standard(0f, 2f, 0.01f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(scaleX = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_scale_y),
    value = tip.scaleY,
    limits = NumericLimits.standard(0f, 2f, 0.01f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(scaleY = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_corner_rounding),
    value = tip.cornerRounding,
    limits = NumericLimits.standard(0f, 1f, 0.01f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(cornerRounding = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_slant_degrees),
    value = tip.slantRadians,
    limits = NumericLimits.radiansShownAsDegrees(-90f, 90f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(slantRadians = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_pinch),
    value = tip.pinch,
    limits = NumericLimits.standard(0f, 1f, 0.01f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(pinch = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_rotation_degrees),
    value = tip.rotationRadians,
    limits = NumericLimits.radiansShownAsDegrees(0f, 360f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(rotationRadians = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_particle_gap_distance_scale),
    value = tip.particleGapDistanceScale,
    limits = NumericLimits.standard(0f, 5f, 0.01f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDistanceScale = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  NumericField(
    title = stringResource(R.string.bg_label_particle_gap_duration_ms),
    value = tip.particleGapDurationSeconds,
    limits = NumericLimits(0f, 1000f, 1f, "ms", unitScale = 1000f),
    onValueChanged = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDurationSeconds = it), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
}
