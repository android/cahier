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

package com.example.cahier.developer.brushgraph.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.NumericLimits

/**
 * A professional numeric input with a slider, ± buttons, and a click-to-edit dialog for exact value
 * entry.
 *
 * Stateless: all state is passed in via [value] and changes are emitted via [onValueChanged] in
 * real units as described by [limits].
 *
 * @param title label displayed above the slider
 * @param value current value in real units as described by [limits]
 * @param limits display range, step, and unit conversion
 * @param onValueChanged callback with the new value in real units as described by [limits]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NumericField(
  modifier: Modifier = Modifier,
  title: String,
  value: Float,
  limits: NumericLimits,
  onValueChangeFinished: (() -> Unit)? = null,
  onValueChanged: (Float) -> Unit,
) {
  val displayValue = limits.fromRealValue(value)
  var showTextInput by remember { mutableStateOf(false) }
  var textInputValue by remember { mutableStateOf("") }

  Column(modifier = modifier.padding(vertical = 4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        modifier =
          Modifier.combinedClickable(
            onClick = {
              textInputValue = displayValue.toString()
              showTextInput = true
            },
            onLongClick = {
              textInputValue = displayValue.toString()
              showTextInput = true
            },
          ),
      )
      Text(
        text = limits.format(displayValue),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
          Modifier.combinedClickable(
            onClick = {
              textInputValue = displayValue.toString()
              showTextInput = true
            },
            onLongClick = {
              textInputValue = displayValue.toString()
              showTextInput = true
            },
          ),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = {
          val newVal = (displayValue - limits.step).coerceAtLeast(limits.min)
          onValueChanged(limits.toRealValue(newVal))
        }
      ) {
        Icon(
          painterResource(R.drawable.remove_24px),
          contentDescription = stringResource(R.string.brush_designer_decrease, title),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
      // TODO(b/573585033)
      @Suppress("DEPRECATION")
      Slider(
        value = displayValue.coerceIn(limits.min, limits.max),
        onValueChange = { onValueChanged(limits.toRealValue(it)) },
        valueRange = limits.min..limits.max,
        modifier = Modifier.weight(1f),
        onValueChangeFinished = onValueChangeFinished,
      )

      IconButton(
        onClick = {
          val newVal = (displayValue + limits.step).coerceAtMost(limits.max)
          onValueChanged(limits.toRealValue(newVal))
        }
      ) {
        Icon(
          painterResource(R.drawable.add_24px),
          contentDescription = stringResource(R.string.brush_designer_increase, title),
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }

    if (showTextInput) {
      AlertDialog(
        onDismissRequest = { showTextInput = false },
        title = { Text(stringResource(R.string.brush_designer_enter_value)) },
        text = {
          // TODO(b/573149579)
          @Suppress("DEPRECATION")
          OutlinedTextField(
            value = textInputValue,
            onValueChange = { textInputValue = it },
            label = {
              Text(
                "$title (${
                                    limits.displayUnit.ifEmpty {
                                        stringResource(R.string.brush_designer_value_label)
                                    }
                                })"
              )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        },
        confirmButton = {
          TextButton(
            onClick = {
              textInputValue.toFloatOrNull()?.let { parsed ->
                val clamped = parsed.coerceIn(limits.min, limits.max)
                onValueChanged(limits.toRealValue(clamped))
              }
              showTextInput = false
            }
          ) {
            Text(stringResource(R.string.done))
          }
        },
        dismissButton = {
          TextButton(onClick = { showTextInput = false }) {
            Text(stringResource(R.string.bg_cancel))
          }
        },
      )
    }
  }
}
