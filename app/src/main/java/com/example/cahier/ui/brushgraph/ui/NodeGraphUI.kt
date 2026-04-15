@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushTip
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.ui.brushgraph.ui.CoatPreviewWidget
import com.example.cahier.ui.brushgraph.ui.TipPreviewWidget
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.Port
import com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
import com.example.cahier.ui.brushgraph.model.NODE_PADDING_BOTTOM
import com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL
import com.example.cahier.ui.brushgraph.model.NODE_WIDTH
import com.example.cahier.ui.brushgraph.model.PREVIEW_AREA_HEIGHT
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import com.example.cahier.ui.theme.extendedColorScheme
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import kotlin.math.roundToInt

import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.ui.CahierTextureBitmapStore

/**
 * A composable that renders an infinite canvas for the node graph. Handles panning, zooming, and
 * node interaction.
 */
@Composable
fun NodeGraphCanvas(
  graph: BrushGraph,
  zoom: Float,
  offset: Offset,
  onZoomChange: (Float) -> Unit,
  onOffsetChange: (Offset) -> Unit,
  onNodeMove: (String, Offset) -> Unit,
  onNodeClick: (String, Offset) -> Unit,
  onNodeDelete: (String) -> Unit,
  onAddEdge: (String, String, Int) -> Unit,
  onEdgeClick: (GraphEdge) -> Unit,
  onEdgeDelete: (GraphEdge) -> Unit,
  onEdgeDetach: (GraphEdge) -> Unit = {},
  onFinalizeEdgeEdit: (GraphEdge, String, String, Int) -> Unit = { _, _, _, _ -> },
  onCanvasClick: () -> Unit = {},
  onPortClick: (String, Port) -> Unit = { _, _ -> },
  onReorderPorts: (String, Int, Int) -> Unit = { _, _, _ -> },
  nodeRegistry: NodeRegistry,
  modifier: Modifier = Modifier,
  activeEdgeSourceId: String? = null,
  selectedNodeId: String? = null,
  selectedEdge: GraphEdge? = null,
  detachedEdge: GraphEdge? = null,
  onNodeDataUpdate: (String, NodeData) -> Unit = { _, _ -> },
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  textureStore: TextureBitmapStore,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  brush: Brush,
  bottomPadding: Dp = 16.dp,
) {
  var pointerPos by remember { mutableStateOf<Offset?>(null) }
  var draggingNodeId by remember { mutableStateOf<String?>(null) }
  var draggingPointerPos by remember { mutableStateOf<Offset?>(null) } // In parent Box space
  var activeSourcePort by remember { mutableStateOf<Port?>(null) }
  var canvasCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  val currentZoom by androidx.compose.runtime.rememberUpdatedState(zoom)
  val currentOffset by androidx.compose.runtime.rememberUpdatedState(offset)
  val currentOnZoomChange by androidx.compose.runtime.rememberUpdatedState(onZoomChange)
  val currentOnOffsetChange by androidx.compose.runtime.rememberUpdatedState(onOffsetChange)
  val currentOnCanvasClick by androidx.compose.runtime.rememberUpdatedState(onCanvasClick)

  val currentGraph by androidx.compose.runtime.rememberUpdatedState(graph)
  val currentOnEdgeClick by androidx.compose.runtime.rememberUpdatedState(onEdgeClick)

  val density = LocalDensity.current
  val trashCenterPaddingPx = with(density) { (bottomPadding + 32.dp).toPx() }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val parentWidth = constraints.maxWidth.toFloat()
    val parentHeight = constraints.maxHeight.toFloat()

    Box(
      modifier =
        Modifier.fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { centroid, pan, gestureZoom, _ ->
              val newZoom = currentZoom * gestureZoom
              currentOnZoomChange(newZoom)
              // Ensure we zoom relative to the centroid.
              val newOffset = (currentOffset - centroid) * gestureZoom + centroid + pan
              currentOnOffsetChange(newOffset)
            }
          }
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = { tapOffset ->
                val graphTap = (tapOffset - currentOffset) / currentZoom
                currentGraph.edges
                  .find { edge ->
                    val fromNode = currentGraph.nodes.find { it.id == edge.fromPort.nodeId }
                    val toNode = currentGraph.nodes.find { it.id == edge.toPort.nodeId }
                    
                    val start = if (fromNode != null) {
                        nodeRegistry.getPort(edge.fromPort.nodeId, edge.fromPort.side, edge.fromPort.index)
                          ?: (fromNode.position + fromNode.data.getPortPosition(edge.fromPort.side, edge.fromPort.index))
                    } else Offset.Zero
                    val end = if (toNode != null) {
                        nodeRegistry.getPort(edge.toPort.nodeId, edge.toPort.side, edge.toPort.index)
                          ?: (toNode.position +
                            toNode.data.getPortPosition(edge.toPort.side, edge.toPort.index))
                    } else Offset.Zero
                    val threshold = 24f / currentZoom
                    val distance = distanceToSpline(graphTap, start, end)
                    distance < threshold
                  }
                  .let { edge ->
                    if (edge != null) {
                      currentOnEdgeClick(edge)
                    } else {
                      currentOnCanvasClick()
                    }
                  }
              }
            )
          }
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .graphicsLayer(
              scaleX = zoom,
              scaleY = zoom,
              translationX = offset.x,
              translationY = offset.y,
              transformOrigin = TransformOrigin(0f, 0f),
            )
      ) {
        Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { canvasCoordinates = it }) {
          val outlineColor = MaterialTheme.colorScheme.outline
          val activeEdgeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
          val selectedEdgeColor = MaterialTheme.colorScheme.primary
          Canvas(modifier = Modifier.fillMaxSize()) {
            for (edge in graph.edges) {
              if (edge == detachedEdge) continue
              val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
              val toNode = graph.nodes.find { it.id == edge.toPort.nodeId }
              
              val start = if (fromNode != null) {
                  nodeRegistry.getPort(edge.fromPort.nodeId, edge.fromPort.side, edge.fromPort.index)
                    ?: (fromNode.position + fromNode.data.getPortPosition(edge.fromPort.side, edge.fromPort.index))
              } else Offset.Zero
              val end = if (toNode != null) {
                  nodeRegistry.getPort(edge.toPort.nodeId, edge.toPort.side, edge.toPort.index)
                    ?: (toNode.position +
                      toNode.data.getPortPosition(edge.toPort.side, edge.toPort.index))
              } else Offset.Zero
              val isSelected = edge == selectedEdge
              drawPath(
                path = createSplinePath(start, end),
                color = if (isSelected) selectedEdgeColor else outlineColor,
                style = DrawStroke(width = if (isSelected) 6f else 3f),
                alpha = if (edge.isDisabled) 0.38f else 1f,
              )
            }

            // Draw temporary edge
            val sourcePort = activeSourcePort
            if (sourcePort != null && pointerPos != null) {
              graph.nodes
                .find { it.id == sourcePort.nodeId }
                ?.let { node ->
                  val start =
                    nodeRegistry.getPort(sourcePort.nodeId, sourcePort.side, sourcePort.index)
                      ?: (node.position + node.data.getPortPosition(sourcePort.side, sourcePort.index))
                  drawPath(
                    path = createSplinePath(start, pointerPos!!),
                    color = activeEdgeColor,
                    style = DrawStroke(width = 3f),
                  )
                }
            }
          }

          for (node in graph.nodes) {
            key(node.id) {
              NodeWidget(
                node = node,
                graph = graph,
                isActiveSource = node.id == activeSourcePort?.nodeId,
                isSelected = node.id == selectedNodeId,
                zoom = zoom,
                onMove = { delta -> onNodeMove(node.id, node.position + delta) },
                onClick = { onNodeClick(node.id, node.position) },
                onUpdate = { onNodeDataUpdate(node.id, it) },
                onPortClick = onPortClick,
                onReorderPorts = onReorderPorts,
                onDragStart = { draggingNodeId = node.id },
                onDrag = { change ->
                  // change.position is relative to node top-left.
                  // Convert to parent Box space.
                  val nodePosInParent = node.position * zoom + offset
                  draggingPointerPos = nodePosInParent + change.position * zoom
                },
                onDragEnd = {
                  // Check if released over trash can.
                  draggingPointerPos?.let { pos ->
                    val trashCenter = Offset(parentWidth / 2f, parentHeight - trashCenterPaddingPx)
                    if ((pos - trashCenter).getDistance() < 100f) {
                      onNodeDelete(node.id)
                    }
                  }
                  draggingNodeId = null
                  draggingPointerPos = null
                },
                onPortDrag = { side, index, isStart ->

                  if (isStart) {
                    if (side == PortSide.OUTPUT) {
                      activeSourcePort = Port(node.id, side, index)

                    } else if (side == PortSide.INPUT) {
                      val edge = graph.edges.find { it.toPort.nodeId == node.id && it.toPort.index == index && !it.isDisabled }

                      if (edge != null) {
                        activeSourcePort = edge.fromPort
                        onEdgeDetach(edge)
                      }
                    }
                  }
                },
                onPortDragUpdate = { pos ->
                  activeSourcePort?.let { sourcePort ->
                    val fromNodeId = sourcePort.nodeId
                    val fromNode = graph.nodes.find { it.id == fromNodeId }
                    if (fromNode != null) {
                      val snappedPort = nodeRegistry.findNearestPort(pos, fromNodeId, graph)
                      if (snappedPort != null) {
                        pointerPos =
                          nodeRegistry.getPort(
                            snappedPort.nodeId,
                            snappedPort.side,
                            snappedPort.index,
                          )
                      } else {
                        pointerPos = pos
                      }
                    } else {
                      pointerPos = pos
                    }
                  }
                },
                onPortDragEnd = {

                  val sourcePort = activeSourcePort
                  if (sourcePort != null) {
                    pointerPos?.let { pos ->
                      val fromNodeId = sourcePort.nodeId
                      val fromNode = graph.nodes.find { it.id == fromNodeId }
                      if (fromNode != null) {
                        val target = nodeRegistry.findNearestPort(pos, fromNodeId, graph)

                        if (target != null) {
                          val currentDetached = detachedEdge
                          if (currentDetached != null) {
                            onFinalizeEdgeEdit(currentDetached, fromNodeId, target.nodeId, target.index)
                          } else {
                            onAddEdge(fromNodeId, target.nodeId, target.index)
                          }
                        } else {
                          detachedEdge?.let {
                            onEdgeDelete(it)
                          }
                        }
                      }
                    }
                  }
                  activeSourcePort = null
                  pointerPos = null
                },
                nodeRegistry = nodeRegistry,
                canvasCoordinates = canvasCoordinates,
                onChooseColor = onChooseColor,
                allTextureIds = allTextureIds,
                onLoadTexture = onLoadTexture,
                strokeRenderer = strokeRenderer,
                textFieldsLocked = textFieldsLocked,
                brush = brush,
              )
            }
          }
        }
      }

      // Trash can UI.
      val draggingNode = graph.nodes.find { it.id == draggingNodeId }
      if (draggingNode != null && draggingNode.data !is NodeData.Family) {
        Surface(
          modifier =
            Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding).graphicsLayer {
              val isOver =
                draggingPointerPos?.let { pos ->
                  val trashCenter = Offset(parentWidth / 2f, parentHeight - trashCenterPaddingPx)
                  (pos - trashCenter).getDistance() < 100f
                } ?: false
              scaleX = if (isOver) 1.2f else 1.0f
              scaleY = if (isOver) 1.2f else 1.0f
            },
          shape = CircleShape,
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
          tonalElevation = 8.dp,
        ) {
          Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Delete",
              tint = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
        }
      }
    }
  }
}

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
  onPortDrag: (PortSide, Int, Boolean) -> Unit = { _, _, _ -> },
  onPortDragUpdate: (Offset) -> Unit = {},
  onPortDragEnd: () -> Unit = {},
  onReorderPorts: (String, Int, Int) -> Unit = { _, _, _ -> },
  onPortClick: (String, Port) -> Unit = { _, _ -> },
  nodeRegistry: NodeRegistry,
  canvasCoordinates: LayoutCoordinates? = null,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  brush: Brush,
  isSelected: Boolean = false,
) {
  var isPressed by remember { mutableStateOf(false) }
  var activeReorderPortIndex by remember { mutableStateOf<Int?>(null) }
  var boxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
  var cumulativeDeltaY by remember { mutableStateOf(0f) }
  val density = LocalDensity.current
  val visiblePorts = node.getVisiblePorts(graph)

  androidx.compose.runtime.DisposableEffect(visiblePorts, node.data.hasOutput()) {
    nodeRegistry.clearNode(node.id)
    onDispose {
      nodeRegistry.clearNode(node.id)
    }
  }

  // Fix jank by using updated callbacks.
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
            nodeRegistry.updateNodeSize(
              node.id,
              androidx.compose.ui.geometry.Size(
                bottomRight.x - topLeft.x,
                bottomRight.y - topLeft.y,
              ),
            )
          }
        }
        .pointerInput(node.id, isSelected) {
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
                } else if (isActiveSource || isPressed || isSelected) {
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
                if (isActiveSource || isPressed || isSelected) {
                  2.dp
                } else if (node.isDisabled) {
                  1.dp
                } else if (node.hasError || node.hasWarning) {
                  2.dp
                } else {
                  1.dp
                },
                if (isActiveSource || isPressed || isSelected) {
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
          with(density) {
            Row(
              modifier =
                Modifier.height(node.data.titleHeight().toDp())
                  .padding(horizontal = 8.dp, vertical = 4.dp)
                  .fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(
                  text = node.data.title(),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                for (subtitle in node.data.subtitles()) {
                  Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              // Previews for Tip and Coat nodes.
              val data = node.data
              if (data is NodeData.Tip) {
                Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
                  TipPreviewWidget(data.tip, strokeRenderer)
                }
              } else if (data is NodeData.Coat) {
                // Resolve connected tip and paint.
                val nodesById = graph.nodes.associateBy { it.id }
                val inputs = graph.edges.filter { it.toPort.nodeId == node.id }
                val tipEdge = inputs.find { it.toPort.index == 0 }
                val paintEdge = inputs.find { it.toPort.index == 1 }
                val tipNode = tipEdge?.let { edge -> nodesById[edge.fromPort.nodeId] }
                val paintNode = paintEdge?.let { edge -> nodesById[edge.fromPort.nodeId] }

                val tip = (tipNode?.data as? NodeData.Tip)?.tip ?: ProtoBrushTip.getDefaultInstance()
                val paint = (paintNode?.data as? NodeData.Paint)?.paint ?: ProtoBrushPaint.getDefaultInstance()

                Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
                  CoatPreviewWidget(
                    ProtoBrushCoat.newBuilder()
                      .setTip(tip)
                      .addPaintPreferences(paint)
                      .build(),
                    strokeRenderer
                  )
                }
              }
            }
          }

          if (node.data is NodeData.ColorFunc) {
            Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
              ColorFunctionPreviewWidget(node.data.function, strokeRenderer)
            }
          }
          if (node.data is NodeData.TextureLayer) {
            Box(modifier = Modifier.size(60.dp).padding(4.dp)) {
              TextureLayerPreviewWidget(node.data.layer, strokeRenderer)
            }
          }

          // Always show input labels row by row below the title.
          Box(modifier = Modifier.fillMaxWidth()) {
            Column {
              for (port in visiblePorts) {
                with(density) {
                  val isPortEmpty = graph.edges.none { it.toPort.nodeId == node.id && it.toPort.index == port.index }
                  Box(
                    modifier =
                      Modifier.height(com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT.toDp())
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = if (port.index == 0 && node.data.hasOutput()) 48.dp else 8.dp)
                        .let {
                          if (isPortEmpty) {
                            it.clickable { onPortClick(node.id, port) }
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
                          contentDescription = "Add",
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      Text(
                        text = port.label ?: "",
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
                    text = "Out",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterEnd),
                  )
                }
              }
            }
          }
        }

        // Ports (Inputs & Output for this part of the node)
        val hasAddPort = visiblePorts.any { it.isAddPort }
        for (port in visiblePorts) {
          val edge = graph.edges.find { it.toPort.nodeId == node.id && it.toPort.index == port.index }
          val portKey = edge?.let { "${it.fromPort.nodeId}_${it.fromPort.index}" } ?: "port_${port.index}"
          androidx.compose.runtime.key(portKey) {
            PortDot(
              port = port,
              count = visiblePorts.size,
              modifier = Modifier.align(Alignment.TopStart),
              zoom = zoom,
              onDrag = onPortDrag,
              onDragUpdate = onPortDragUpdate,
              onDragEnd = onPortDragEnd,
              nodeRegistry = nodeRegistry,
              canvasCoordinates = canvasCoordinates,
              portPosition = node.data.getPortPosition(PortSide.INPUT, port.index),
              isReorderable = !port.isAddPort && hasAddPort && 
                              !(node.data is com.example.cahier.ui.brushgraph.model.NodeData.Coat && port.index == 0) &&
                              !(node.data is com.example.cahier.ui.brushgraph.model.NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE && port.index % 2 != 0),
              isLargeHandle = node.data is com.example.cahier.ui.brushgraph.model.NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE && port.index % 2 == 0,
              onReorderUpdate = { deltaY ->
                if (activeReorderPortIndex != port.index) {
                  activeReorderPortIndex = port.index
                  cumulativeDeltaY = 0f
                }
                cumulativeDeltaY += deltaY
                
                val originalY = com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL + node.data.titleHeight() + (port.index + 0.5f) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
                var maxValidIndex = visiblePorts.size - 2 // Exclude add port
                var minValidIndex = if (node.data is com.example.cahier.ui.brushgraph.model.NodeData.Coat) 1 else 0
                
                if (node.data is com.example.cahier.ui.brushgraph.model.NodeData.Paint) {
                    val textureEdges = graph.edges.filter { edge ->
                        val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
                        fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.TextureLayer && edge.toPort.nodeId == node.id
                    }
                    val T = textureEdges.size
                    
                    val colorEdges = graph.edges.filter { edge ->
                        val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
                        fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.ColorFunc && edge.toPort.nodeId == node.id
                    }
                    val C = colorEdges.size
                    
                    if (port.index in 0 until T) {
                        minValidIndex = 0
                        maxValidIndex = T - 1
                    } else if (port.index in (T + 1) until (T + 1 + C)) {
                        minValidIndex = T + 1
                        maxValidIndex = T + 1 + C - 1
                    }
                }
                
                val minValidY = com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL + node.data.titleHeight() + (minValidIndex + 0.5f) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
                val maxValidY = com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL + node.data.titleHeight() + (maxValidIndex + 0.5f) * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
                
                val requestedY = originalY + cumulativeDeltaY
                val currentY = requestedY.coerceIn(minValidY, maxValidY)
                cumulativeDeltaY = currentY - originalY
                
                val isPolarTarget = node.data is com.example.cahier.ui.brushgraph.model.NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
                
                val targetIndex = if (isPolarTarget) {
                    val setSize = 2
                    val currentSet = ((currentY - com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL - node.data.titleHeight()) / (com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT * setSize) - 0.5f).roundToInt()
                    currentSet * setSize
                } else {
                    ((currentY - com.example.cahier.ui.brushgraph.model.NODE_PADDING_VERTICAL - node.data.titleHeight()) / com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT - 0.5f).roundToInt()
                }
                
                if (targetIndex in minValidIndex..maxValidIndex && targetIndex != port.index) {
                  onReorderPorts(node.id, port.index, targetIndex)
                  val direction = if (targetIndex > port.index) 1 else -1
                  val step = if (isPolarTarget) 2 else 1
                  cumulativeDeltaY -= direction * step * com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT
                  activeReorderPortIndex = targetIndex
                }
              },
              onReorderEnd = {
                activeReorderPortIndex = null
                cumulativeDeltaY = 0f
              },
              isDragging = port.index == activeReorderPortIndex,
              dragOffset = if (port.index == activeReorderPortIndex) cumulativeDeltaY else 0f
            )
          }
        }
        if (node.data.hasOutput()) {
          PortDot(
            port = Port(node.id, PortSide.OUTPUT, 0),
            count = 1,
            modifier = Modifier.align(Alignment.TopEnd),
            zoom = zoom,
            onDrag = onPortDrag,
            onDragUpdate = onPortDragUpdate,
            onDragEnd = onPortDragEnd,
            nodeRegistry = nodeRegistry,
            canvasCoordinates = canvasCoordinates,
            portPosition = node.data.getPortPosition(PortSide.OUTPUT, 0),
          )
        }
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
  }
}
}

/**
 * Custom drag gesture detector that allows for a zoom-adjusted touch slop. This ensures that
 * dragging to create an edge starts reliably even when the canvas is significantly zoomed in.
 */
suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectPortDragGestures(
  zoom: Float,
  onDragStart: (Offset) -> Unit = {},
  onDragEnd: () -> Unit = {},
  onDragCancel: () -> Unit = {},
  onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
  val touchSlop = viewConfiguration.touchSlop / zoom

  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var drag: PointerInputChange? = null
    var dragStartedCalled = false

    // Wait for the pointer to move beyond the (zoom-adjusted) touch slop.
    val pointerId = down.id
    var totalMainPositionChange = Offset.Zero
    while (true) {
      val event = awaitPointerEvent()
      val dragEvent = event.changes.firstOrNull { it.id == pointerId } ?: break
      if (dragEvent.isConsumed) break
      if (dragEvent.changedToUpIgnoreConsumed()) break

      val positionChange = dragEvent.positionChange()
      totalMainPositionChange += positionChange
      val distance = totalMainPositionChange.getDistance()
      if (distance >= touchSlop) {
        onDragStart(dragEvent.position)
        dragStartedCalled = true
        onDrag(dragEvent, totalMainPositionChange)
        if (dragEvent.isConsumed) {
          drag = dragEvent
        }
        break
      }
      if (event.changes.all { it.changedToUpIgnoreConsumed() }) break
    }

    if (drag != null || totalMainPositionChange.getDistance() >= touchSlop) {
      val dragSuccessful =
        drag(pointerId) {
          onDrag(it, it.positionChange())
          it.consume()
        }
      if (!dragSuccessful) {
        if (dragStartedCalled) onDragCancel()
      } else {
        if (dragStartedCalled) onDragEnd()
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
  onDrag: (PortSide, Int, Boolean) -> Unit,
  onDragUpdate: (Offset) -> Unit = {},
  onDragEnd: () -> Unit = {},
  nodeRegistry: NodeRegistry,
  canvasCoordinates: LayoutCoordinates? = null,
  portPosition: Offset,
  isReorderable: Boolean = false,
  onReorderUpdate: (Float) -> Unit = {},
  onReorderEnd: () -> Unit = {},
  isDragging: Boolean = false,
  dragOffset: Float = 0f,
  isLargeHandle: Boolean = false,
) {
  var portCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  val density = LocalDensity.current

  val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
  val currentOnDragUpdate by androidx.compose.runtime.rememberUpdatedState(onDragUpdate)
  val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
  val currentOnReorderUpdate by androidx.compose.runtime.rememberUpdatedState(onReorderUpdate)
  val currentOnReorderEnd by androidx.compose.runtime.rememberUpdatedState(onReorderEnd)

  val currentPortIndex by androidx.compose.runtime.rememberUpdatedState(port.index)
  val currentPortSide by androidx.compose.runtime.rememberUpdatedState(port.side)

  with(density) {
    val outerX = if (port.side == PortSide.INPUT) (-24).dp else 14.dp
    
    val animatedY by androidx.compose.animation.core.animateDpAsState(
        targetValue = portPosition.y.toDp() - 6.dp,
        label = "portY"
    )
    val finalY = if (isDragging) portPosition.y.toDp() - 6.dp else animatedY

    Box(
      modifier =
        modifier
          .offset(
            x = outerX,
            y = finalY,
          )
          .size(width = if (port.side == PortSide.INPUT) 24.dp else 12.dp, height = 12.dp)
          .graphicsLayer {
            if (isDragging) {
                translationY = dragOffset
            }
          }
    ) {
      Box(
        modifier =
          Modifier
            .align(if (port.side == PortSide.INPUT) Alignment.TopStart else Alignment.TopEnd)
            .size(12.dp)
            .background(
                if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                CircleShape
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .onGloballyPositioned { coordinates ->
              portCoordinates = coordinates
              val canvasCo = canvasCoordinates
              if (canvasCo != null && coordinates.isAttached) {
                val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                val graphSpacePos = canvasCo.localPositionOf(coordinates, center)
                nodeRegistry.updatePort(port.nodeId, port.side, port.index, graphSpacePos)
              }
            }
            .pointerInput(port.nodeId, port.side, canvasCoordinates, zoom) {
                detectPortDragGestures(
                  zoom = zoom,
                  onDragStart = { currentOnDrag(currentPortSide, currentPortIndex, true) },
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

      if (port.side == PortSide.INPUT && isReorderable) {
        Icon(
          imageVector = androidx.compose.material.icons.Icons.Default.DragHandle,
          contentDescription = "Reorder",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier
              .align(Alignment.TopEnd)
              .size(width = 10.dp, height = if (isLargeHandle) with(density) { (com.example.cahier.ui.brushgraph.model.INPUT_ROW_HEIGHT * 2).toDp() } else 10.dp)
              .pointerInput(port.nodeId, port.side) {
                detectDragGestures(
                  onDrag = { change, dragAmount ->
                    change.consume()
                    currentOnReorderUpdate(dragAmount.y)
                  },
                  onDragEnd = { currentOnReorderEnd() },
                  onDragCancel = { currentOnReorderEnd() }
                )
              }
        )
      }
    }
  }
}


