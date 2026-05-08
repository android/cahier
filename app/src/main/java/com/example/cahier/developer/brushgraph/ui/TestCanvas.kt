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
@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.ink.brush.Brush
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.cahier.core.ui.DrawingSurface
import com.example.cahier.developer.brushgraph.viewmodel.BrushGraphViewModel
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.developer.brushgraph.data.TutorialAction
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.core.ui.theme.extendedColorScheme
import androidx.compose.ui.res.stringResource
import com.example.cahier.R

@Composable
fun TestCanvas(
  isInvertedCanvas: Boolean,
  strokeList: List<Stroke>,
  strokeRenderer: CanvasStrokeRenderer,
  brush: Brush,
  modifier: Modifier = Modifier,
  onGetNextBrush: () -> Brush,
  onStrokesAdded: (List<Stroke>) -> Unit,
) {
  val textureStore = LocalTextureStore.current
  Box(modifier = modifier.fillMaxSize()) {
    Text(
      stringResource(R.string.bg_test_canvas_draw_prompt),
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isInvertedCanvas) {
          MaterialTheme.colorScheme.inverseOnSurface
        } else {
          MaterialTheme.colorScheme.onSurface
        },
    )
    DrawingSurface(
      strokes = strokeList,
      canvasStrokeRenderer = strokeRenderer,
      textureStore = textureStore,
      onStrokesFinished = onStrokesAdded,
      onErase = { _, _ -> },
      onEraseStart = {},
      onEraseEnd = {},
      currentBrush = brush,
      onGetNextBrush = onGetNextBrush,
      isEraserMode = false,
      backgroundImageUri = null,
      onStartDrag = {},
    )
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollapsiblePreviewPane(
  isPreviewExpanded: Boolean,
  isInvertedCanvas: Boolean,
  testAutoUpdateStrokes: Boolean,
  brushColor: Color,
  brushSize: Float,
  brush: Brush,
  strokeList: List<Stroke>,
  strokeRenderer: CanvasStrokeRenderer,
  topIssue: GraphValidationException?,
  modifier: Modifier = Modifier,
  onGetNextBrush: () -> Brush,
  onTogglePreviewExpanded: () -> Unit,
  onClearStrokes: () -> Unit,
  onToggleCanvasTheme: () -> Unit,
  onSetTestAutoUpdateStrokes: (Boolean) -> Unit,
  onUpdateTestBrushColor: (Color) -> Unit,
  onUpdateTestBrushSize: (Float) -> Unit,
  onStrokesAdded: (List<Stroke>) -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onToggleNotificationPane: () -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    // Toggle Tab (always visible)
    Surface(
      modifier =
        Modifier.fillMaxWidth().height(40.dp).clickable { onTogglePreviewExpanded() },
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 4.dp,
      shadowElevation = 8.dp,
    ) {
      Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            if (isPreviewExpanded) {
              Icons.Default.KeyboardArrowDown
            } else {
              Icons.Default.KeyboardArrowUp
            },
            contentDescription = if (isPreviewExpanded) stringResource(R.string.bg_test_canvas_collapse) else stringResource(R.string.bg_test_canvas_expand),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(R.string.bg_test_canvas_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          if (isPreviewExpanded) {
            Spacer(Modifier.width(16.dp))
            Text(
              stringResource(R.string.bg_test_canvas_reset),
              modifier = Modifier.clickable { onClearStrokes() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Text(
              stringResource(R.string.bg_test_canvas_invert),
              modifier = Modifier.clickable { onToggleCanvasTheme() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            
            // Auto-update toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = testAutoUpdateStrokes,
                onCheckedChange = { onSetTestAutoUpdateStrokes(it) }
              )
              Spacer(Modifier.width(4.dp))
              Text(stringResource(R.string.bg_auto_update), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(16.dp))
            
            // Color picker
            Box(
              modifier = Modifier
                .size(20.dp)
                .background(brushColor)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clickable {
                  onChooseColor(brushColor) { newColor ->
                    onUpdateTestBrushColor(newColor)
                  }
                }
            )
            Spacer(Modifier.width(16.dp))
            
            // Size selector
            var sizeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = sizeExpanded,
              onExpandedChange = { sizeExpanded = it },
              modifier = Modifier.width(80.dp)
            ) {
              Text(
                text = "${brushSize.toInt()}px",
                modifier = Modifier.menuAnchor().clickable { sizeExpanded = true },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              ExposedDropdownMenu(
                expanded = sizeExpanded,
                onDismissRequest = { sizeExpanded = false }
              ) {
                for (size in (2..4 step 1) + (6..10 step 2) + (20..40 step 10) + (50..100 step 25)) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.bg_size_px, size)) },
                    onClick = {
                      onUpdateTestBrushSize(size.toFloat())
                      sizeExpanded = false
                    }
                  )
                }
              }
            }

            // Top issue, if one is present
            if (topIssue != null) {
              val isError = topIssue.severity == ValidationSeverity.ERROR
              Surface(
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.extendedColorScheme.warning,
                tonalElevation = 2.dp,
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onToggleNotificationPane() }
              ) {
                Box(
                  modifier = Modifier.fillMaxSize().padding(start = 16.dp),
                  contentAlignment = Alignment.CenterStart
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (isError) Icons.Default.Error else Icons.Default.Warning,
                      contentDescription = null,
                      tint = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColorScheme.onWarning,
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                      text = stringResource(if (isError) R.string.bg_error else R.string.bg_warning, topIssue.displayMessage.asString()),
                      style = MaterialTheme.typography.labelLarge,
                      color = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColorScheme.onWarning,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Expanding Drawer Content
    AnimatedVisibility(
      visible = isPreviewExpanded,
      enter = expandVertically(),
      exit = shrinkVertically(),
    ) {
      Surface(
        modifier =
          Modifier.fillMaxWidth().height((PREVIEW_HEIGHT_EXPANDED - PREVIEW_HEIGHT_COLLAPSED).dp),
        tonalElevation = 8.dp,
        color =
          if (isInvertedCanvas) {
            MaterialTheme.colorScheme.inverseSurface
          } else {
            MaterialTheme.colorScheme.surface
          },
      ) {
        TestCanvas(
          strokeList = strokeList,
          strokeRenderer = strokeRenderer,
          brush = brush,
          isInvertedCanvas = isInvertedCanvas,
          onGetNextBrush = onGetNextBrush,
          onStrokesAdded = onStrokesAdded,
        )
      }
    }
  }
}
