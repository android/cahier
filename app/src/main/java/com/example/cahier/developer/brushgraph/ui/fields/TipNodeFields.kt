/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.StarredField
import com.example.cahier.developer.brushgraph.data.StarredFieldType
import com.example.cahier.developer.brushgraph.ui.TipPreviewWidget
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import ink.proto.BrushTip as ProtoBrushTip

@Composable
fun TipNodeFields(
  data: NodeData.Tip,
  onUpdate: (NodeData) -> Unit,
  onFieldEditComplete: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  nodeId: String,
  starredFields: Set<StarredField>,
  onToggleStar: (String, StarredFieldType) -> Unit,
  modifier: Modifier = Modifier
) {
  val tip = data.tip
  TipPreviewWidget(tip, strokeRenderer)

  val isScaleXStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_SCALE_X))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_SCALE_X,
    value = tip.scaleX,
    limits = NumericLimits.standard(0f, 2f, 0.01f),
    isStarred = isScaleXStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_SCALE_X) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setScaleX(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isScaleYStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_SCALE_Y))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_SCALE_Y,
    value = tip.scaleY,
    limits = NumericLimits.standard(0f, 2f, 0.01f),
    isStarred = isScaleYStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_SCALE_Y) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setScaleY(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isCornerStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_CORNER_ROUNDING))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_CORNER_ROUNDING,
    value = tip.cornerRounding,
    limits = NumericLimits.standard(0f, 1f, 0.01f),
    isStarred = isCornerStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_CORNER_ROUNDING) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setCornerRounding(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isSlantStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_SLANT))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_SLANT,
    value = tip.slantRadians,
    limits = NumericLimits.radiansShownAsDegrees(-90f, 90f),
    isStarred = isSlantStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_SLANT) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setSlantRadians(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isPinchStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_PINCH))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_PINCH,
    value = tip.pinch,
    limits = NumericLimits.standard(0f, 1f, 0.01f),
    isStarred = isPinchStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_PINCH) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setPinch(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isRotationStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_ROTATION))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_ROTATION,
    value = tip.rotationRadians,
    limits = NumericLimits.radiansShownAsDegrees(0f, 360f),
    isStarred = isRotationStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_ROTATION) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setRotationRadians(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isGapDistStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_GAP_DISTANCE))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_GAP_DISTANCE,
    value = tip.particleGapDistanceScale,
    limits = NumericLimits.standard(0f, 5f, 0.01f),
    isStarred = isGapDistStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_GAP_DISTANCE) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setParticleGapDistanceScale(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
  val isGapDurStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.TIP_GAP_DURATION))
  StarrableNumericField(
    nodeId = nodeId,
    fieldType = StarredFieldType.TIP_GAP_DURATION,
    value = tip.particleGapDurationSeconds,
    limits = NumericLimits(0f, 1000f, 1f, "ms", unitScale = 1000f),
    isStarred = isGapDurStarred,
    onToggleStar = { onToggleStar(nodeId, StarredFieldType.TIP_GAP_DURATION) },
    onValueChanged = { onUpdate(NodeData.Tip(tip.toBuilder().setParticleGapDurationSeconds(it).build(), behaviorPortIds = data.behaviorPortIds)) },
    onValueChangeFinished = onFieldEditComplete
  )
}
