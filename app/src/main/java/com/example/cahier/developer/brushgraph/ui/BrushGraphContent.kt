@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.cahier.developer.brushgraph.data.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.developer.brushgraph.data.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_EXPANDED

@Composable
fun BrushGraphContent(
  isLandscape: Boolean,
  isNodeSelected: Boolean,
  isEdgeSelected: Boolean,
  isErrorPaneOpen: Boolean,
  isPreviewExpanded: Boolean,
  viewportSize: Size,
  onViewportSizeChange: (Size) -> Unit,
  canvasSlot: @Composable (trashPaddingBottom: Dp) -> Unit,
  inspectorSlot: @Composable () -> Unit,
  notificationPaneSlot: @Composable () -> Unit,
  notificationIconSlot: @Composable (indicatorPaddingEnd: Dp) -> Unit,
  previewSlot: @Composable () -> Unit,
  menuSlot: @Composable () -> Unit,
  fabSlot: @Composable (viewportSize: Size) -> Unit,
  tutorialSlot: @Composable (viewportSize: Size) -> Unit,
  dialogSlot: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  dialogSlot()

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val currentIsLandscape = maxWidth > maxHeight

    val isSidePaneOpen = currentIsLandscape && (isNodeSelected || isErrorPaneOpen)
    val indicatorPaddingEnd by animateDpAsState(
      targetValue = if (isSidePaneOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp,
      label = "indicatorPaddingEnd",
    )
    val previewHeight = if (isPreviewExpanded) {
      PREVIEW_HEIGHT_EXPANDED
    } else {
      PREVIEW_HEIGHT_COLLAPSED
    }
    val isAnySidePaneOpen = isNodeSelected || isEdgeSelected || isErrorPaneOpen

    val trashPaddingBottom by animateDpAsState(
      targetValue =
        if (!currentIsLandscape && isAnySidePaneOpen) {
          (maxOf(previewHeight, INSPECTOR_HEIGHT_PORTRAIT) + 16).dp
        } else {
          (previewHeight + 16).dp
        },
      label = "trashPaddingBottom",
    )

    Scaffold { paddingValues ->
      Box(
        modifier =
          Modifier.fillMaxSize().padding(paddingValues).onGloballyPositioned { coordinates ->
            onViewportSizeChange(coordinates.size.toSize())
          }
      ) {
        canvasSlot(trashPaddingBottom)
        inspectorSlot()
        notificationPaneSlot()
        notificationIconSlot(indicatorPaddingEnd)
        previewSlot()
        menuSlot()
        fabSlot(viewportSize)
        tutorialSlot(viewportSize)
      }
    }
  }
}
