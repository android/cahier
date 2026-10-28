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

package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.NumericLimits
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.ui.EnumDropdown
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.ui.NumericField
import com.example.cahier.developer.brushgraph.ui.getColorFunctionTooltip
import ink.proto.Color as ProtoColor
import ink.proto.ColorFunction as ProtoColorFunction

@Composable
fun ColorFunctionNodeFields(
  function: ProtoColorFunction,
  onUpdate: (NodeData) -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onDropdownEditComplete: () -> Unit,
  onFieldEditComplete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentTypeResId = function.displayStringRId()

  Column(modifier = modifier) {
    FieldWithTooltip(
      tooltipTitle =
        stringResource(R.string.bg_title_function_type_format, stringResource(currentTypeResId)),
      tooltipText = stringResource(getColorFunctionTooltip(currentTypeResId)),
    ) {
      EnumDropdown(
        label = stringResource(R.string.bg_function_type),
        currentValue = currentTypeResId,
        values =
          listOf(
            R.string.bg_target_opacity_multiplier,
            R.string.bg_replace_color,
            R.string.bg_target_hue_offset,
            R.string.bg_target_chroma_multiplier,
            R.string.bg_target_lightness_offset,
          ),
        displayName = { stringResource(it) },
        onSelected = { resId ->
          if (resId != currentTypeResId) {
            onUpdate(
              when (resId) {
                R.string.bg_target_opacity_multiplier -> {
                  NodeData.ColorFunction(
                    ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()
                  )
                }

                R.string.bg_replace_color -> {
                  NodeData.ColorFunction(
                    ProtoColorFunction.newBuilder()
                      .setReplaceColor(
                        ProtoColor.newBuilder().setR(0f).setG(0f).setB(0f).setA(1f).build()
                      )
                      .build()
                  )
                }

                R.string.bg_target_hue_offset -> {
                  NodeData.ColorFunction(
                    ProtoColorFunction.newBuilder().setHueOffsetRadians(0f).build()
                  )
                }

                R.string.bg_target_chroma_multiplier -> {
                  NodeData.ColorFunction(
                    ProtoColorFunction.newBuilder().setChromaMultiplier(1f).build()
                  )
                }

                else -> { // bg_target_lightness_offset
                  NodeData.ColorFunction(
                    ProtoColorFunction.newBuilder().setLightnessOffset(0f).build()
                  )
                }
              }
            )
          }
          onDropdownEditComplete()
        },
      )
    }

    if (function.hasOpacityMultiplier()) {
      NumericField(
        title = stringResource(R.string.bg_label_opacity_multiplier),
        value = function.opacityMultiplier,
        limits = NumericLimits(0f, 2f, 0.01f, "x"),
        onValueChanged = {
          onUpdate(NodeData.ColorFunction(function.toBuilder().setOpacityMultiplier(it).build()))
        },
        onValueChangeFinished = onFieldEditComplete,
      )
    } else if (function.hasChromaMultiplier()) {
      NumericField(
        title = stringResource(R.string.bg_label_chroma_multiplier),
        value = function.chromaMultiplier,
        limits = NumericLimits(0f, 2f, 0.01f, "x"),
        onValueChanged = {
          onUpdate(NodeData.ColorFunction(function.toBuilder().setChromaMultiplier(it).build()))
        },
        onValueChangeFinished = onFieldEditComplete,
      )
    } else if (function.hasHueOffsetRadians()) {
      NumericField(
        title = stringResource(R.string.bg_label_hue_offset),
        value = function.hueOffsetRadians,
        limits = NumericLimits.radiansShownAsDegrees(-360f, 360f),
        onValueChanged = {
          onUpdate(NodeData.ColorFunction(function.toBuilder().setHueOffsetRadians(it).build()))
        },
        onValueChangeFinished = onFieldEditComplete,
      )
    } else if (function.hasLightnessOffset()) {
      NumericField(
        title = stringResource(R.string.bg_label_lightness_offset),
        value = function.lightnessOffset,
        limits = NumericLimits.floatShownAsPercent(-100f, 100f),
        onValueChanged = {
          onUpdate(NodeData.ColorFunction(function.toBuilder().setLightnessOffset(it).build()))
        },
        onValueChangeFinished = onFieldEditComplete,
      )
    } else if (function.hasReplaceColor()) {
      val color = function.replaceColor
      val composeColor = Color(red = color.r, green = color.g, blue = color.b, alpha = color.a)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
      ) {
        Text(stringResource(R.string.bg_color_label), style = MaterialTheme.typography.bodyMedium)
        Surface(
          onClick = {
            onChooseColor(composeColor) { newColor ->
              onUpdate(
                NodeData.ColorFunction(
                  function
                    .toBuilder()
                    .setReplaceColor(
                      ProtoColor.newBuilder()
                        .setR(newColor.red)
                        .setG(newColor.green)
                        .setB(newColor.blue)
                        .setA(newColor.alpha)
                        .build()
                    )
                    .build()
                )
              )
            }
          },
          shape = RoundedCornerShape(4.dp),
          color = composeColor,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.size(40.dp),
        ) {}
        Spacer(Modifier.width(8.dp))
        Text(
          text = String.format("ARGB #%08X", (composeColor.toArgb())),
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}
