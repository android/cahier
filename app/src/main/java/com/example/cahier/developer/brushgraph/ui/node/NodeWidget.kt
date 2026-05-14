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

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.developer.brushgraph.ui.SineWavePreview
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.ui.NODE_PADDING_BOTTOM
import com.example.cahier.developer.brushgraph.ui.NODE_PADDING_VERTICAL
import com.example.cahier.developer.brushgraph.ui.NODE_WIDTH
import com.example.cahier.developer.brushgraph.ui.width
import com.example.cahier.developer.brushgraph.ui.height
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.core.ui.theme.extendedColorScheme
import kotlin.math.roundToInt

@Composable
fun NodeWidget(
  node: GraphNode,
  position: Offset,
  graph: BrushGraph,
  isActiveSource: Boolean,
  zoom: Float,
  allTextureIds: Set<String>,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  brush: Brush,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onLoadTexture: () -> Unit,
  onMove: (Offset) -> Unit,
  onClick: () -> Unit,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier,
  canvasCoordinates: LayoutCoordinates? = null,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  isInSelectedSet: Boolean = false,
  onDragStart: () -> Unit = {},
  onDrag: (PointerInputChange) -> Unit = {},
  onDragEnd: () -> Unit = {},
  onPortDrag: (PortSide, String, Boolean) -> Unit = { _, _, _ -> },
  onPortDragUpdate: (Offset) -> Unit = {},
  onPortDragEnd: () -> Unit = {},
  onReorderPorts: (String, Int, Int) -> Unit = { _, _, _ -> },
  onPortClick: (String, Port) -> Unit = { _, _ -> },
  getPortPosition: (String, Boolean) -> Offset,
  onPortPositioned: (String, Offset) -> Unit,
  onClearNodeCache: () -> Unit,
  onLongPress: () -> Unit = {},
) {
  var isPressed by remember { mutableStateOf(false) }
  val density = LocalDensity.current
  val visiblePorts = remember(node.data, graph) { node.getVisiblePorts(graph) }

  androidx.compose.runtime.DisposableEffect(node.id) {
    onDispose {
      onClearNodeCache()
    }
  }

  val currentOnMove by androidx.compose.runtime.rememberUpdatedState(onMove)
  val currentOnDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
  val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
  val currentOnDragUpdate by androidx.compose.runtime.rememberUpdatedState(onDrag)

  val w = node.data.width()
  val h = node.data.height(visiblePorts.size)

  val backgroundColorByNodeData =
    when (node.data) {
      is NodeData.Coat -> MaterialTheme.colorScheme.secondaryContainer
      is NodeData.Tip,
      is NodeData.Paint -> MaterialTheme.colorScheme.secondaryContainer
      is NodeData.Family -> MaterialTheme.colorScheme.tertiaryContainer
      is NodeData.TextureLayer,
      is NodeData.ColorFunction -> MaterialTheme.colorScheme.surfaceVariant
      else -> MaterialTheme.colorScheme.surfaceDim
    }
  val (backgroundColor, outlineWeight, outlineColor) = when {
    node.isDisabled -> 
      Triple(MaterialTheme.colorScheme.surfaceDim, 
              1.dp,
              MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
    isActiveSource || isPressed || isSelected || isInSelectedSet -> 
      Triple(MaterialTheme.colorScheme.primaryContainer,
              2.dp,
              MaterialTheme.colorScheme.primary)
     node.hasError -> 
      Triple(MaterialTheme.colorScheme.errorContainer,
              2.dp,
              MaterialTheme.colorScheme.error)
    node.hasWarning -> 
      Triple(MaterialTheme.extendedColorScheme.warningContainer,
              2.dp,
              MaterialTheme.extendedColorScheme.warning)
    else -> 
      Triple(backgroundColorByNodeData,
              1.dp,
              MaterialTheme.colorScheme.outline)
  }
  Box(
    modifier =
      modifier.zIndex(if (isSelected) 1f else 0f)
        .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
        .pointerInput(node.id, isSelected, isSelectionMode) {
          detectTapGestures(
            onPress = {
              isPressed = true
              try {
                awaitRelease()
              } finally {
                isPressed = false
              }
            },
            onTap = { onClick() },
            onLongPress = { onLongPress() }
          )
        }
        .pointerInput(node.id, isSelected) {
          detectDragGestures(
            onDragStart = { currentOnDragStart() },
            onDragEnd = { currentOnDragEnd() },
            onDragCancel = { currentOnDragEnd() },
            onDrag = { change, dragAmount ->
              change.consume()
              currentOnMove(dragAmount)
              currentOnDragUpdate(change)
            },
          )
        }
        .then(
          with(density) {
            Modifier.width(w.toDp())
              .height(h.toDp())
              .background(
                backgroundColor,
                RoundedCornerShape(8.dp),
              )
              .border(
                outlineWeight,
                outlineColor,
                RoundedCornerShape(8.dp),
              )
          }
        )
  ) {
    Box(modifier = Modifier.fillMaxSize().alpha(if (node.isDisabled) 0.38f else 1f)) {
      Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
      val nodeWidthDp = with(density) { NODE_WIDTH.toDp() }
      val topPaddingDp = with(density) { NODE_PADDING_VERTICAL.toDp() }
      val bottomPaddingDp = with(density) { NODE_PADDING_BOTTOM.toDp() }
      Box(
        modifier = Modifier.width(nodeWidthDp).fillMaxHeight().padding(bottom = bottomPaddingDp)
      ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = topPaddingDp)) {
          NodeHeader(
            node = node,
            graph = graph,
            strokeRenderer = strokeRenderer
          )

          NodePortLabels(
            node = node,
            graph = graph,
            visiblePorts = visiblePorts,
            isSelectionMode = isSelectionMode,
            onPortClick = onPortClick
          )
        }

        NodePortDots(
          node = node,
          position = position,
          graph = graph,
          visiblePorts = visiblePorts,
          zoom = zoom,
          onPortDrag = onPortDrag,
          onPortDragUpdate = onPortDragUpdate,
          onPortDragEnd = onPortDragEnd,
          getPortPosition = getPortPosition,
          onPortPositioned = onPortPositioned,
          canvasCoordinates = canvasCoordinates,
          onReorderPorts = onReorderPorts
        )
      }

      if (node.data is NodeData.Family) {
        // Division Line
        Box(modifier = Modifier.fillMaxHeight().width(2.dp).background(outlineColor))

        SineWavePreview(
          brush = brush,
          strokeRenderer = strokeRenderer,
          modifier =
            Modifier.weight(1f)
              .fillMaxHeight()
              .background(MaterialTheme.colorScheme.surface)
              .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)),
        )
      }
    }
    
    if (isSelectionMode && node.data !is NodeData.Family) {
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = 6.dp, y = (-6).dp)
          .size(16.dp)
          .background(
            if (isInSelectedSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            CircleShape
          )
          .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
      )
    }
  }
}
}
