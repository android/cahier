@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import android.util.Log
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.R
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.DisplayText
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
import com.example.cahier.ui.brushgraph.model.NODE_PADDING_BOTTOM
import com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL
import com.example.cahier.ui.brushgraph.model.NODE_WIDTH
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.Port
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import com.example.cahier.ui.brushgraph.model.isPortReorderable
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
                  is NodeData.ColorFunc -> MaterialTheme.colorScheme.surfaceVariant
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

@Composable
fun PortDot(
  port: Port,
  count: Int,
  modifier: Modifier,
  zoom: Float,
  onDrag: (PortSide, String, Boolean) -> Unit,
  onDragUpdate: (Offset) -> Unit = {},
  onDragEnd: () -> Unit = {},
  onPortPositioned: (Offset) -> Unit,
  canvasCoordinates: LayoutCoordinates? = null,
  portPosition: Offset,
  isReorderable: Boolean = false,
  onReorderUpdate: (Float) -> Unit = {},
  onReorderEnd: () -> Unit = {},
  isDragging: Boolean = false,
  dragOffset: Float = 0f,
  isLargeHandle: Boolean = false,
) {
  var portCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
  val density = LocalDensity.current

  val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
  val currentOnDragUpdate by androidx.compose.runtime.rememberUpdatedState(onDragUpdate)
  val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
  val currentOnReorderUpdate by androidx.compose.runtime.rememberUpdatedState(onReorderUpdate)
  val currentOnReorderEnd by androidx.compose.runtime.rememberUpdatedState(onReorderEnd)
  val currentPortId by androidx.compose.runtime.rememberUpdatedState(port.id)
  val currentPortSide by androidx.compose.runtime.rememberUpdatedState(port.side)

  val outerWidth = if (port.side == PortSide.INPUT) 24.dp else 12.dp
  val outerHeight = if (port.side == PortSide.INPUT) 32.dp else 12.dp
  val outerX = if (port.side == PortSide.INPUT) (-24).dp else 14.dp
  
  val animatedY by androidx.compose.animation.core.animateDpAsState(
      targetValue = with(density) { portPosition.y.toDp() } - (if (port.side == PortSide.INPUT) 16.dp else 6.dp),
      label = "portY"
  )
  val finalY = if (isDragging) with(density) { portPosition.y.toDp() } - (if (port.side == PortSide.INPUT) 16.dp else 6.dp) else animatedY

  Box(
    modifier =
      modifier
        .offset {
          IntOffset(outerX.roundToPx(), finalY.roundToPx())
        }
        .size(width = outerWidth, height = outerHeight)
        .graphicsLayer {
          if (isDragging) {
              translationY = dragOffset
          }
        }
        .zIndex(if (isDragging) 1f else 0f)
  ) {
    if (port.side == PortSide.INPUT) {
      // Input Port (Left Half)
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .size(width = 12.dp, height = 32.dp)
          .pointerInput(port.nodeId, port.side, canvasCoordinates, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDragStart = { currentOnDrag(currentPortSide, currentPortId, true) },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
              ) { change, _ ->
              change.consume()
              val canvasCo = canvasCoordinates
              val portCo = portCoordinates
              if (canvasCo != null && portCo != null && canvasCo.isAttached && portCo.isAttached) {
                val graphSpacePos = canvasCo.localPositionOf(portCo, change.position)
                currentOnDragUpdate(graphSpacePos)
              }
            }
          }
      ) {
          Box(
            modifier = Modifier
              .align(Alignment.Center)
              .size(12.dp)
              .background(
                  if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  CircleShape
              )
              .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
              .onGloballyPositioned { coordinates: androidx.compose.ui.layout.LayoutCoordinates ->
                portCoordinates = coordinates
                val canvasCo = canvasCoordinates
                if (canvasCo != null && coordinates.isAttached) {
                  val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                  val graphSpacePos = canvasCo.localPositionOf(coordinates, center)
                  onPortPositioned(graphSpacePos)
                }
              }
          )
      }

      // Reorder Handle (Right Half)
      if (isReorderable) {
        val handleHeight = if (isLargeHandle) with(density) { (com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT * 2).toDp() } else 32.dp
        Box(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(width = 12.dp, height = handleHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            .pointerInput(port.nodeId, port.side, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDrag = { change, dragAmount ->
                  change.consume()
                  currentOnReorderUpdate(dragAmount.y)
                },
                onDragEnd = { currentOnReorderEnd() },
                onDragCancel = { currentOnReorderEnd() }
              )
            }
        ) {
            Icon(
              painter = painterResource(R.drawable.gs_drag_indicator_vd_theme_24),
              contentDescription = stringResource(R.string.bg_cd_reorder),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.Center).size(12.dp)
            )
        }
      }
    } else {
      // Output Port
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(12.dp)
          .background(
              if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              CircleShape
          )
          .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
          .onGloballyPositioned { coordinates: androidx.compose.ui.layout.LayoutCoordinates ->
            portCoordinates = coordinates
            val canvasCo = canvasCoordinates
            if (canvasCo != null && coordinates.isAttached) {
              val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
              val graphSpacePos = canvasCo.localPositionOf(coordinates, center)
              onPortPositioned(graphSpacePos)
            }
          }
          .pointerInput(port.nodeId, port.side, canvasCoordinates, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDragStart = { currentOnDrag(currentPortSide, currentPortId, true) },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
              ) { change, _ ->
              change.consume()
              val canvasCo = canvasCoordinates
              val portCo = portCoordinates
              if (canvasCo != null && portCo != null && canvasCo.isAttached && portCo.isAttached) {
                val graphSpacePos = canvasCo.localPositionOf(portCo, change.position)
                currentOnDragUpdate(graphSpacePos)
              }
            }
          }
      )
    }
  }
}

@Composable
fun NodeHeader(
  node: GraphNode,
  graph: BrushGraph,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier,
) {
  val data = node.data
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .height(with(LocalDensity.current) { data.titleHeight().toDp() })
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
        Text(
          text = stringResource(data.title()),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        for (subtitle in data.subtitles()) {
          val subtitleText = subtitle.asString()
          Text(
            text = subtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Previews for Tip and Coat nodes.
      if (data is NodeData.Tip) {
        Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
          TipPreviewWidget(data.tip, strokeRenderer)
        }
      } else if (data is NodeData.Coat) {
        val coat = try {
            com.example.cahier.ui.brushgraph.converters.BrushFamilyConverter.createCoat(node, graph, mutableMapOf())
        } catch (e: Exception) {
            ProtoBrushCoat.getDefaultInstance()
        }

        Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
          CoatPreviewWidget(coat, strokeRenderer)
        }
      }
    }

    if (data is NodeData.ColorFunc) {
      Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
        ColorFunctionPreviewWidget(data.function, strokeRenderer)
      }
    }
    if (data is NodeData.TextureLayer) {
      Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
        TextureLayerPreviewWidget(data.layer, strokeRenderer)
      }
    }
  }
}

@Composable
fun NodePortLabels(
  node: GraphNode,
  graph: BrushGraph,
  visiblePorts: List<Port>,
  isSelectionMode: Boolean,
  onPortClick: (String, Port) -> Unit,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current
  Box(modifier = modifier.fillMaxWidth()) {
    Column {
      for ((index, port) in visiblePorts.withIndex()) {
        with(density) {
          val isPortEmpty = graph.edges.none { it.toNodeId == node.id && it.toPortId == port.id }
          Box(
            modifier =
              Modifier.height(com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT.toDp())
                .fillMaxWidth()
                .padding(start = 8.dp, end = if (index == 0 && node.data.hasOutput()) 48.dp else 8.dp)
                .let {
                  if (isPortEmpty) {
                    it.clickable(enabled = !isSelectionMode) { onPortClick(node.id, port) }
                      .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                  } else {
                    it
                  }
                },
            contentAlignment = Alignment.CenterStart,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp)
            ) {
              if (isPortEmpty) {
                Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = stringResource(R.string.bg_cd_add),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
              }
              Text(
                text = port.label?.asString() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPortEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
    // Output label on the right, aligned with the first input row.
    if (node.data.hasOutput()) {
      with(density) {
        Box(
          modifier =
            Modifier.height(INPUT_ROW_HEIGHT.toDp())
              .align(Alignment.TopEnd)
              .padding(horizontal = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.bg_label_out),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterEnd),
          )
        }
      }
    }
  }
}

@Composable
fun BoxScope.NodePortDots(
  node: GraphNode,
  graph: BrushGraph,
  visiblePorts: List<Port>,
  zoom: Float,
  onPortDrag: (PortSide, String, Boolean) -> Unit,
  onPortDragUpdate: (Offset) -> Unit,
  onPortDragEnd: () -> Unit,
  getPortPosition: (String, Boolean) -> Offset,
  onPortPositioned: (String, Offset) -> Unit,
  canvasCoordinates: LayoutCoordinates?,
  onReorderPorts: (String, Int, Int) -> Unit,
) {
  var activeReorderPortIndex by remember { mutableStateOf<Int?>(null) }
  var cumulativeDeltaY by remember { mutableFloatStateOf(0f) }
  val hasAddPort = visiblePorts.any { it.isAddPort }

  for ((index, port) in visiblePorts.withIndex()) {
    val edge = graph.edges.find { it.toNodeId == node.id && it.toPortId == port.id }
    val portKey = edge?.let { "${it.fromNodeId}_${port.id}" } ?: "port_${port.id}"
    androidx.compose.runtime.key(portKey) {
      PortDot(
        port = port,
        count = visiblePorts.size,
        modifier = Modifier.align(Alignment.TopStart),
        zoom = zoom,
        onDrag = onPortDrag,
        onDragUpdate = onPortDragUpdate,
        onDragEnd = onPortDragEnd,
        onPortPositioned = { pos -> onPortPositioned(port.id, pos) },
        canvasCoordinates = canvasCoordinates,
        portPosition = getPortPosition(port.id, true) - Offset(node.position.x, node.position.y),
        isReorderable = node.data.isPortReorderable(port, index, hasAddPort),
        isLargeHandle = node.data is com.example.cahier.ui.brushgraph.model.NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE && index % 2 == 0,
        onReorderUpdate = { deltaY ->
          if (activeReorderPortIndex != index) {
            activeReorderPortIndex = index
            cumulativeDeltaY = 0f
          }
          cumulativeDeltaY += deltaY
          
          val originalY = getPortPosition(port.id, true).y - node.position.y
          var maxValidIndex = visiblePorts.size - 2 // Exclude add port
          var minValidIndex = if (node.data is NodeData.Coat) 1 else 0
          
          if (node.data is com.example.cahier.ui.brushgraph.model.NodeData.Paint) {
              val textureEdges = graph.edges.filter { edge ->
                  val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
                  fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.TextureLayer && edge.toNodeId == node.id
              }
              val T = textureEdges.size
              
              val colorEdges = graph.edges.filter { edge ->
                  val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
                  fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.ColorFunc && edge.toNodeId == node.id
              }
              val C = colorEdges.size
              
              if (index in 0 until T) {
                  minValidIndex = 0
                  maxValidIndex = T - 1
              } else if (index in (T + 1) until (T + 1 + C)) {
                  minValidIndex = T + 1
                  maxValidIndex = T + 1 + C - 1
              }
          }
          
          val minDragY = com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL + node.data.titleHeight() + (0 + 0.5f) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
          val maxDragY = com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL + node.data.titleHeight() + (visiblePorts.size - 1 + 0.5f) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
          
          val requestedY = originalY + cumulativeDeltaY
          
          val isPolarTarget = node.data is com.example.cahier.ui.brushgraph.model.NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
          
          val targetIndex = if (isPolarTarget) {
              val setSize = 2
              val currentSet = ((requestedY - com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL - node.data.titleHeight()) / (com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT * setSize) - 0.5f).roundToInt()
              currentSet * setSize
          } else {
              ((requestedY - com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL - node.data.titleHeight()) / com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT - 0.5f).roundToInt()
          }
          
          val currentY = requestedY.coerceIn(minDragY, maxDragY)
          cumulativeDeltaY = currentY - originalY
          
          if (targetIndex in minValidIndex..maxValidIndex && targetIndex != index) {
            onReorderPorts(node.id, index, targetIndex)
            cumulativeDeltaY -= (targetIndex - index) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
            activeReorderPortIndex = targetIndex
          }
        },
        onReorderEnd = {
          activeReorderPortIndex = null
          cumulativeDeltaY = 0f
        },
        isDragging = index == activeReorderPortIndex,
        dragOffset = if (index == activeReorderPortIndex) cumulativeDeltaY else 0f
      )
    }
  }
  if (node.data.hasOutput()) {
    PortDot(
      port = Port.Output(node.id, "output"),
      count = 1,
      modifier = Modifier.align(Alignment.TopEnd),
      zoom = zoom,
      onDrag = onPortDrag,
      onDragUpdate = onPortDragUpdate,
      onDragEnd = onPortDragEnd,
      onPortPositioned = { pos -> onPortPositioned("output", pos) },
      canvasCoordinates = canvasCoordinates,
      portPosition = getPortPosition("output", true) - Offset(node.position.x, node.position.y),
    )
  }
}
