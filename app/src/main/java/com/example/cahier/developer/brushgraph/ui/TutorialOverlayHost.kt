@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.developer.brushgraph.data.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.developer.brushgraph.data.TutorialAction
import com.example.cahier.developer.brushgraph.data.TutorialAnchor
import com.example.cahier.developer.brushgraph.data.TutorialStep

@Composable
fun BoxScope.TutorialOverlayHost(
  tutorialStep: TutorialStep?,
  graph: BrushGraph,
  zoom: Float,
  offset: androidx.compose.ui.geometry.Offset,
  selectedNodeId: String?,
  selectedEdge: GraphEdge?,
  currentStepIndex: Int,
  isLandscape: Boolean,
  viewportSize: androidx.compose.ui.geometry.Size,
  isPreviewExpanded: Boolean,
  onAdvanceTutorial: (TutorialAction) -> Unit,
  onRegressTutorial: () -> Unit,
  onCloseTutorial: () -> Unit,
  modifier: Modifier = Modifier
) {
  tutorialStep?.let { step ->
    val density = LocalDensity.current
    val isInspectorOpen = (selectedNodeId != null || selectedEdge != null)
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    
    val tutorialModifier = when (step.anchor) {
      TutorialAnchor.SCREEN_CENTER -> Modifier.align(Alignment.Center)
      
      TutorialAnchor.FAB -> {
        if (isInspectorOpen) {
          if (isLandscape) {
            Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = (INSPECTOR_WIDTH_LANDSCAPE + 80).dp)
          } else {
            Modifier.align(Alignment.BottomEnd).padding(bottom = (INSPECTOR_HEIGHT_PORTRAIT + 16).dp, end = 80.dp)
          }
        } else {
          Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 80.dp)
        }
      }
      
      TutorialAnchor.NODE_CANVAS -> {
        val node = step.getTargetNode(graph)
        if (node != null) {
          val nodeCenterX = node.position.x + node.data.width() / 2f
          val nodeTopY = node.position.y
          
          val screenX = nodeCenterX * zoom + offset.x
          val screenY = nodeTopY * zoom + offset.y
          
          val paddingPx = with(density) { 16.dp.toPx() }
          Modifier.offset { 
            IntOffset(
              (screenX - overlaySize.width / 2).toInt(),
              (screenY - overlaySize.height - paddingPx).toInt()
            )
          }
        } else {
          Modifier.align(Alignment.Center)
        }
      }
      
      TutorialAnchor.INSPECTOR -> {
        if (isLandscape) {
          Modifier.align(Alignment.CenterEnd).padding(end = (INSPECTOR_WIDTH_LANDSCAPE + 16).dp)
        } else {
          Modifier.align(Alignment.BottomCenter).padding(bottom = (INSPECTOR_HEIGHT_PORTRAIT + 16).dp)
        }
      }
      
      TutorialAnchor.TEST_CANVAS -> {
        val basePadding = if (isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
        if (isInspectorOpen && !isLandscape) {
          Modifier.align(Alignment.BottomCenter).padding(bottom = (maxOf(INSPECTOR_HEIGHT_PORTRAIT, basePadding) + 16).dp)
        } else {
          Modifier.align(Alignment.BottomCenter).padding(bottom = (basePadding + 16).dp)
        }
      }
      
      TutorialAnchor.ACTION_BAR -> Modifier.align(Alignment.TopStart).padding(top = 80.dp, start = 16.dp)
      
      TutorialAnchor.NOTIFICATION_ICON -> {
        val indicatorPaddingEnd = if (isLandscape && isInspectorOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp
        Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = indicatorPaddingEnd)
      }
    }.zIndex(20f)

    TutorialOverlay(
      step = step,
      onNext = { onAdvanceTutorial(step.actionRequired) },
      onBack = if (currentStepIndex > 0) { { onRegressTutorial() } } else null,
      onClose = onCloseTutorial,
      modifier = modifier.then(tutorialModifier).onGloballyPositioned { coordinates ->
        overlaySize = coordinates.size
      }
    )
  }
}
