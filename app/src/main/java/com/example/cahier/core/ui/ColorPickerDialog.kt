/*
 * Copyright 2025 Google LLC. All rights reserved.
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

package com.example.cahier.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.core.ui.theme.BrushBlack
import com.example.cahier.core.ui.theme.BrushBlue
import com.example.cahier.core.ui.theme.BrushGreen
import com.example.cahier.core.ui.theme.BrushRed
import com.example.cahier.core.ui.theme.BrushYellow
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import java.util.Locale

/** Dropdown menu offering preset brush colors and an option to open [CustomColorPickerDialog]. */
@Composable
fun ColorPickerDropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  onColorSelected: (Color) -> Unit,
  onCustomColorClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = remember {
    listOf(
      R.string.brush_designer_color_black to BrushBlack,
      R.string.brush_designer_color_red to BrushRed,
      R.string.brush_designer_color_blue to BrushBlue,
      R.string.brush_designer_color_green to BrushGreen,
      R.string.brush_designer_color_yellow to BrushYellow,
    )
  }

  DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier) {
    for ((nameRes, color) in colors) {
      val name = stringResource(nameRes)
      DropdownMenuItem(
        text = { Text(name) },
        leadingIcon = {
          Icon(
            painter = painterResource(R.drawable.circle_24px),
            contentDescription = name,
            tint = color,
          )
        },
        onClick = { onColorSelected(color) },
      )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

    DropdownMenuItem(
      text = { Text(stringResource(R.string.brush_designer_custom_color)) },
      leadingIcon = {
        Icon(
          painter = painterResource(R.drawable.palette_24px),
          contentDescription = stringResource(R.string.brush_designer_custom_color),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      },
      onClick = onCustomColorClick,
    )
  }
}

/** A dialog for picking a custom color using a classic HSV color picker. */
@Composable
fun CustomColorPickerDialog(
  initialColor: Color,
  onColorSelected: (Color) -> Unit,
  onDismissRequest: () -> Unit,
) {
  var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }

  AlertDialog(
    onDismissRequest = {
      onColorSelected(currentColor.toColor())
      onDismissRequest()
    },
    title = { Text(stringResource(R.string.brush_designer_pick_color)) },
    text = {
      Column(horizontalAlignment = Alignment.End) {
        ClassicColorPicker(
          color = currentColor,
          modifier = Modifier.fillMaxWidth().height(300.dp),
          onColorChanged = { hsvColor: HsvColor -> currentColor = hsvColor },
        )
        Spacer(modifier = Modifier.height(16.dp))

        val hexString = String.format(Locale.US, "%08x", currentColor.toColor().toArgb())
        Text(
          text = hexString,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onColorSelected(currentColor.toColor())
          onDismissRequest()
        }
      ) {
        Text(stringResource(R.string.done))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.bg_cancel)) }
    },
  )
}
