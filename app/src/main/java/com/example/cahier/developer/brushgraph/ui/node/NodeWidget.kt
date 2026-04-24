@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.ui.SineWavePreview
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.INPUT_ROW_HEIGHT
import com.example.cahier.developer.brushgraph.data.NODE_PADDING_BOTTOM
import com.example.cahier.developer.brushgraph.data.NODE_PADDING_VERTICAL
import com.example.cahier.developer.brushgraph.data.NODE_WIDTH
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.developer.brushgraph.data.isPortReorderable
import com.example.cahier.core.ui.theme.extendedColorScheme
import ink.proto.BrushCoat as ProtoBrushCoat
import kotlin.math.roundToInt

@Composable
fun NodeWidget(
  node: GraphNode,
  graph: BrushGraph,
  isActiveSource: Boolean,
  zoom: Float,
  onMove: (Offset) -> Unit,
  onClick: () -> Unit,
  onUpdate: (NodeData) -> Unit,
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
  onUpdateNodeSize: (androidx.compose.ui.geometry.Size) -> Unit,
  canvasCoordinates: LayoutCoordinates? = null,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  brush: Brush,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  isInSelectedSet: Boolean = false,
  onLongPress: () -> Unit = {},
) {
  var isPressed by remember { mutableStateOf(false) }
  var boxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
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

  Box(
    modifier =
      Modifier.zIndex(if (isSelected) 1f else 0f)
        .offset { IntOffset(node.position.x.roundToInt(), node.position.y.roundToInt()) }
        .onGloballyPositioned { coordinates ->
          if (canvasCoordinates != null && coordinates.isAttached) {
            val size = coordinates.size
            val topLeft = canvasCoordinates.localPositionOf(coordinates, Offset.Zero)
            val bottomRight =
              canvasCoordinates.localPositionOf(
                coordinates,
                Offset(size.width.toFloat(), size.height.toFloat()),
              )
            onUpdateNodeSize(
              androidx.compose.ui.geometry.Size(
                bottomRight.x - topLeft.x,
                bottomRight.y - topLeft.y,
              )
            )
          }
        }
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
            val backgroundColor =
              if (node.isDisabled) {
                MaterialTheme.colorScheme.surfaceDim
              } else {
                when (node.data) {
                  is NodeData.Coat -> MaterialTheme.colorScheme.secondaryContainer
                  is NodeData.Tip,
                  is NodeData.Paint -> MaterialTheme.colorScheme.secondaryContainer
                  is NodeData.Family -> MaterialTheme.colorScheme.tertiaryContainer
                  is NodeData.TextureLayer,
                  is NodeData.ColorFunction -> MaterialTheme.colorScheme.surfaceVariant
                  else -> MaterialTheme.colorScheme.surfaceDim
                }
              }
            Modifier.width(w.toDp())
              .height(h.toDp())
              .background(
                if (node.isDisabled) {
                  MaterialTheme.colorScheme.surfaceDim
                } else if (isActiveSource || isPressed || isSelected || isInSelectedSet) {
                  MaterialTheme.colorScheme.primaryContainer
                } else if (node.hasError) {
                  MaterialTheme.colorScheme.errorContainer
                } else if (node.hasWarning) {
                  MaterialTheme.extendedColorScheme.warningContainer
                } else {
                  backgroundColor
                },
                RoundedCornerShape(8.dp),
              )
              .border(
                if (isActiveSource || isPressed || isSelected || isInSelectedSet) {
                  2.dp
                } else if (node.isDisabled) {
                  1.dp
                } else if (node.hasError || node.hasWarning) {
                  2.dp
                } else {
                  1.dp
                },
                if (isActiveSource || isPressed || isSelected || isInSelectedSet) {
                  MaterialTheme.colorScheme.primary
                } else if (node.isDisabled) {
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
                } else if (node.hasError) {
                  MaterialTheme.colorScheme.error
                } else if (node.hasWarning) {
                  MaterialTheme.extendedColorScheme.warning
                } else {
                  MaterialTheme.colorScheme.outline
                },
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
        val borderWidth = 2.dp
        val borderColor =
          when {
            node.hasError -> MaterialTheme.colorScheme.error
            node.hasWarning -> MaterialTheme.extendedColorScheme.warning
            isActiveSource || isPressed || isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
          }

        Box(modifier = Modifier.fillMaxHeight().width(borderWidth).background(borderColor))

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

