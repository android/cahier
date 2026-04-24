@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.cahier.ui.brushgraph.BrushGraphViewModel
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.ui.brushgraph.model.TutorialAction
import androidx.compose.ui.res.stringResource
import com.example.cahier.R

@Composable
fun TestCanvas(
  viewModel: BrushGraphViewModel,
  strokeList: List<Stroke>,
  strokeRenderer: CanvasStrokeRenderer,
  textureStore: TextureBitmapStore,
  brush: Brush,
  onStrokesAdded: (List<Stroke>) -> Unit,
  isDark: Boolean = false,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      stringResource(R.string.bg_test_canvas_draw_prompt),
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isDark) {
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
      onGetNextBrush = { viewModel.brush.value },
      isEraserMode = false,
      backgroundImageUri = null,
      onStartDrag = {},
    )
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollapsiblePreviewPane(
  viewModel: BrushGraphViewModel,
  strokeRenderer: CanvasStrokeRenderer,
  textureStore: TextureBitmapStore,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Toggle Tab (always visible)
    Surface(
      modifier =
        Modifier.fillMaxWidth().height(40.dp).clickable { viewModel.togglePreviewExpanded() },
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
            if (viewModel.isPreviewExpanded) {
              Icons.Default.KeyboardArrowDown
            } else {
              Icons.Default.KeyboardArrowUp
            },
            contentDescription = if (viewModel.isPreviewExpanded) stringResource(R.string.bg_test_canvas_collapse) else stringResource(R.string.bg_test_canvas_expand),
          )
          Spacer(Modifier.width(8.dp))
          Text(
            stringResource(R.string.bg_test_canvas_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          if (viewModel.isPreviewExpanded) {
            Spacer(Modifier.width(16.dp))
            Text(
              stringResource(R.string.bg_test_canvas_reset),
              modifier = Modifier.clickable { viewModel.clearStrokes() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Text(
              stringResource(R.string.bg_test_canvas_invert),
              modifier = Modifier.clickable { viewModel.toggleCanvasTheme() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            
            // Auto-update toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = viewModel.testAutoUpdateStrokes,
                onCheckedChange = { viewModel.testAutoUpdateStrokes = it }
              )
              Spacer(Modifier.width(4.dp))
              Text(stringResource(R.string.bg_auto_update), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(16.dp))
            
            // Color picker
            Box(
              modifier = Modifier
                .size(20.dp)
                .background(Color(viewModel.testBrushColor ?: 0))
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .clickable {
                  onChooseColor(Color(viewModel.testBrushColor ?: 0)) { newColor ->
                    viewModel.updateTestBrushColor(newColor.toArgb())
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
                text = "${viewModel.testBrushSize.toInt()}px",
                modifier = Modifier.menuAnchor().clickable { sizeExpanded = true },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              ExposedDropdownMenu(
                expanded = sizeExpanded,
                onDismissRequest = { sizeExpanded = false }
              ) {
                for (size in 10..50 step 10) {
                  DropdownMenuItem(
                    text = { Text(stringResource(R.string.bg_size_px, size)) },
                    onClick = {
                      viewModel.updateTestBrushSize(size.toFloat())
                      sizeExpanded = false
                    }
                  )
                }
              }
            }
          }
        }
      }
    }

    // Expanding Drawer Content
    AnimatedVisibility(
      visible = viewModel.isPreviewExpanded,
      enter = expandVertically(),
      exit = shrinkVertically(),
    ) {
      Surface(
        modifier =
          Modifier.fillMaxWidth().height((PREVIEW_HEIGHT_EXPANDED - PREVIEW_HEIGHT_COLLAPSED).dp),
        tonalElevation = 8.dp,
        color =
          if (viewModel.isDarkCanvas) {
            MaterialTheme.colorScheme.inverseSurface
          } else {
            MaterialTheme.colorScheme.surface
          },
      ) {
        TestCanvas(
          viewModel = viewModel,
          strokeList = viewModel.strokeList,
          strokeRenderer = strokeRenderer,
          textureStore = textureStore,
          brush = viewModel.brush.collectAsState().value,
          onStrokesAdded = { 
            viewModel.strokeList.addAll(it)
            viewModel.advanceTutorial(TutorialAction.DRAW_ON_CANVAS)
          },
          isDark = viewModel.isDarkCanvas,
        )
      }
    }
  }
}
