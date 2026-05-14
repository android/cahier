/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushdesigner.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import com.example.cahier.R
import ink.proto.BrushTip as ProtoBrushTip

/**
 * Tab 0: Tip geometry controls — scale (with lock toggle), corner rounding,
 * slant, rotation, pinch, and particle (stamp) settings.
 *
 * Uses [NumericField] for professional ±button input with degree/percent
 * unit conversions and click-to-edit exact-value entry.
 *
 * Stateless: receives data and callbacks, does not access ViewModel.
 */
@OptIn(ExperimentalInkCustomBrushApi::class)
@Composable
internal fun TipShapeTabContent(
    currentTip: ProtoBrushTip,
    activeBrush: Brush?,
    onUpdateTip: (ProtoBrushTip.Builder.() -> Unit) -> Unit,
) {
    var isScaleLocked by remember { mutableStateOf(false) }

    TipPreview(
        brush = activeBrush,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    Text(
        stringResource(R.string.brush_designer_tip_geometry),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.brush_designer_lock_scale_ratio),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = isScaleLocked, onCheckedChange = { isScaleLocked = it })
    }

    NumericField(
        title = stringResource(R.string.brush_designer_tip_scale_x),
        value = if (currentTip.hasScaleX()) currentTip.scaleX else 1f,
        limits = NumericLimits.floatShownAsPercent(10f, 200f),
        onValueChanged = { newX ->
            onUpdateTip {
                val oldX = if (hasScaleX()) scaleX else 1f
                val oldY = if (hasScaleY()) scaleY else 1f
                setScaleX(newX)
                if (isScaleLocked && oldX > 0f) {
                    setScaleY(oldY * (newX / oldX))
                }
            }
        }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_tip_scale_y),
        value = if (currentTip.hasScaleY()) currentTip.scaleY else 1f,
        limits = NumericLimits.floatShownAsPercent(10f, 200f),
        onValueChanged = { newY ->
            onUpdateTip {
                val oldX = if (hasScaleX()) scaleX else 1f
                val oldY = if (hasScaleY()) scaleY else 1f
                setScaleY(newY)
                if (isScaleLocked && oldY > 0f) {
                    setScaleX(oldX * (newY / oldY))
                }
            }
        }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_corner_rounding),
        value = if (currentTip.hasCornerRounding()) currentTip.cornerRounding else 1f,
        limits = NumericLimits.floatShownAsPercent(0f, 100f),
        onValueChanged = { newValue -> onUpdateTip { setCornerRounding(newValue) } }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_slant),
        value = if (currentTip.hasSlantRadians()) currentTip.slantRadians else 0f,
        limits = NumericLimits.radiansShownAsDegrees(-90f, 90f),
        onValueChanged = { newValue -> onUpdateTip { setSlantRadians(newValue) } }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_tip_rotation),
        value = if (currentTip.hasRotationRadians()) currentTip.rotationRadians else 0f,
        limits = NumericLimits.radiansShownAsDegrees(0f, 360f),
        onValueChanged = { newValue -> onUpdateTip { setRotationRadians(newValue) } }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_pinch),
        value = if (currentTip.hasPinch()) currentTip.pinch else 0f,
        limits = NumericLimits.floatShownAsPercent(0f, 100f),
        onValueChanged = { newValue -> onUpdateTip { setPinch(newValue) } }
    )

    HorizontalDivider()
    Text(
        stringResource(R.string.brush_designer_particle_settings),
        style = MaterialTheme.typography.titleSmall
    )

    NumericField(
        title = stringResource(R.string.brush_designer_gap_distance_scale),
        value = if (currentTip.hasParticleGapDistanceScale()) currentTip
            .particleGapDistanceScale else 0f,
        limits = NumericLimits(0f, 5f, 0.1f),
        onValueChanged = { newValue ->
            onUpdateTip { setParticleGapDistanceScale(newValue) }
        }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_gap_duration_ms),
        value = if (currentTip.hasParticleGapDurationSeconds()) currentTip
            .particleGapDurationSeconds * 1000f else 0f,
        limits = NumericLimits(0f, 250f, 5f),
        onValueChanged = { newValue ->
            onUpdateTip { setParticleGapDurationSeconds(newValue / 1000f) }
        }
    )
}
