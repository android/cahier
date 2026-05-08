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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.ui.node.NodeRegistry
import com.example.cahier.developer.brushgraph.ui.node.NodeWidget
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import kotlin.math.roundToInt

/**
 * A composable that renders an infinite canvas for the node graph. Handles panning, zooming, and
 * node interaction.
 */
@Composable
fun GraphCanvas(
  graph: BrushGraph,
  zoom: Float,
  offset: Offset,
  onZoomChange: (Float) -> Unit,
  onOffsetChange: (Offset) -> Unit,
  onNodeClick: (String, Offset) -> Unit,
  onNodeDelete: (String) -> Unit,
  onAddEdge: (String, String, String) -> Unit,
  onEdgeClick: (GraphEdge) -> Unit,
  onEdgeDelete: (GraphEdge) -> Unit,
  nodeRegistry: NodeRegistry,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  brush: Brush,
  modifier: Modifier = Modifier,
  onNodeMoveFinished: () -> Unit = {},
  onNodeLongPress: (String) -> Unit = {},
  onEdgeDetach: (GraphEdge) -> Unit = {},
  onFinalizeEdgeEdit: (GraphEdge, String, String, String) -> Unit = { _, _, _, _ -> },
  onCanvasClick: () -> Unit = {},
  onPortClick: (String, Port) -> Unit = { _, _ -> },
  onReorderPorts: (String, Int, Int) -> Unit = { _, _, _ -> },
  activeEdgeSourceId: String? = null,
  selectedNodeId: String? = null,
  selectedEdge: GraphEdge? = null,
  detachedEdge: GraphEdge? = null,
  onNodeDataUpdate: (String, NodeData) -> Unit = { _, _ -> },
  bottomPadding: Dp = 16.dp,
  rightPadding: Dp = 0.dp,
  isSelectionMode: Boolean = false,
  selectedNodeIds: Set<String> = emptySet(),
  onSelectAll: () -> Unit = {},
  onDuplicateSelected: () -> Unit = {},
  onDeleteSelected: () -> Unit = {},
  onDoneSelection: () -> Unit = {},
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
                    val fromNode = currentGraph.nodes.find { it.id == edge.fromNodeId }
                    val toNode = currentGraph.nodes.find { it.id == edge.toNodeId }
                    
                    val start = if (fromNode != null) {
                        nodeRegistry.getPortPosition(edge.fromNodeId, Port.OUTPUT_PORT_ID, currentGraph)
                    } else Offset.Zero
                    val end = if (toNode != null) {
                        nodeRegistry.getPortPosition(edge.toNodeId, edge.toPortId, currentGraph)
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
          EdgeRenderer(
            graph = graph,
            detachedEdge = detachedEdge,
            selectedEdge = selectedEdge,
            activeSourcePort = activeSourcePort,
            pointerPos = pointerPos,
            nodeRegistry = nodeRegistry,
            selectedEdgeColor = selectedEdgeColor,
            outlineColor = outlineColor,
            activeEdgeColor = activeEdgeColor
          )

          for (node in graph.nodes) {
            key(node.id) {
              NodeWidget(
                node = node,
                position = nodeRegistry.getNodePosition(node.id) ?: Offset.Zero,
                graph = graph,
                isActiveSource = node.id == activeSourcePort?.nodeId,
                isSelected = node.id == selectedNodeId,
                zoom = zoom,
                onMove = { delta ->
                  if (isSelectionMode && selectedNodeIds.contains(node.id)) {
                    selectedNodeIds.forEach { selId ->
                      val currentPos = nodeRegistry.getNodePosition(selId) ?: Offset.Zero
                      nodeRegistry.updateNodePosition(selId, currentPos + delta)
                    }
                  } else {
                    val currentPos = nodeRegistry.getNodePosition(node.id) ?: Offset.Zero
                    nodeRegistry.updateNodePosition(node.id, currentPos + delta)
                  }
                },
                onClick = { onNodeClick(node.id, nodeRegistry.getNodePosition(node.id) ?: Offset.Zero) },
                onUpdate = { onNodeDataUpdate(node.id, it) },
                onPortClick = onPortClick,
                onReorderPorts = onReorderPorts,
                onDragStart = { draggingNodeId = node.id },
                isSelectionMode = isSelectionMode,
                isInSelectedSet = selectedNodeIds.contains(node.id),
                onLongPress = { onNodeLongPress(node.id) },
                onDrag = { change ->
                  val nodePos = nodeRegistry.getNodePosition(node.id) ?: Offset.Zero
                  val nodePosInParent = Offset(nodePos.x * zoom, nodePos.y * zoom) + offset
                  draggingPointerPos = nodePosInParent + change.position * zoom
                },
                onDragEnd = {
                  draggingPointerPos?.let { pos ->
                    val rightPaddingPx = with(density) { rightPadding.toPx() }
                    val trashCenterX = (parentWidth - rightPaddingPx) / 2f
                    val trashCenter = Offset(trashCenterX, parentHeight - trashCenterPaddingPx)
                    if ((pos - trashCenter).getDistance() < 100f) {
                      onNodeDelete(node.id)
                    }
                  }
                  draggingNodeId = null
                  draggingPointerPos = null
                  onNodeMoveFinished()
                },
                onPortDrag = { side, portId, isStart ->
                  if (!isSelectionMode) {
                    if (isStart) {
                      if (side == PortSide.OUTPUT) {
                        activeSourcePort = Port.Output(node.id, portId)
                      } else if (side == PortSide.INPUT) {
                        val edge = graph.edges.find { it.toNodeId == node.id && it.toPortId == portId && !it.isDisabled }

                        if (edge != null) {
                          activeSourcePort = Port.Output(edge.fromNodeId)
                          onEdgeDetach(edge)
                        }
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
                          nodeRegistry.getPortPosition(
                            snappedPort.nodeId,
                            snappedPort.id,
                            graph
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
                            onFinalizeEdgeEdit(currentDetached, fromNodeId, target.nodeId, target.id)
                          } else {
                            onAddEdge(fromNodeId, target.nodeId, target.id)
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
                getPortPosition = { portId, fallback -> nodeRegistry.getPortPosition(node.id, portId, graph, fallback) },
                onPortPositioned = { portId, pos -> nodeRegistry.updatePort(node.id, portId, pos) },
                onClearNodeCache = { nodeRegistry.clearNode(node.id) },
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

      TrashCanArea(
        graph = graph,
        draggingNodeId = draggingNodeId,
        draggingPointerPos = draggingPointerPos,
        parentWidth = parentWidth,
        parentHeight = parentHeight,
        trashCenterPaddingPx = trashCenterPaddingPx,
        bottomPadding = bottomPadding,
        rightPadding = rightPadding,
        onNodeDelete = onNodeDelete,
        modifier = Modifier.padding(end = rightPadding).align(Alignment.BottomCenter)
      )
      
      SelectionActionMenu(
        isSelectionMode = isSelectionMode,
        onSelectAll = onSelectAll,
        onDuplicateSelected = onDuplicateSelected,
        onDeleteSelected = onDeleteSelected,
        onDoneSelection = onDoneSelection,
        modifier = Modifier.align(Alignment.TopStart)
      )
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
fun EdgeRenderer(
  graph: BrushGraph,
  detachedEdge: GraphEdge?,
  selectedEdge: GraphEdge?,
  activeSourcePort: Port?,
  pointerPos: Offset?,
  nodeRegistry: NodeRegistry,
  selectedEdgeColor: Color,
  outlineColor: Color,
  activeEdgeColor: Color,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    for (edge in graph.edges) {
      if (edge == detachedEdge) continue
      val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
      val toNode = graph.nodes.find { it.id == edge.toNodeId }
      
      val start = if (fromNode != null) {
          nodeRegistry.getPortPosition(edge.fromNodeId, Port.OUTPUT_PORT_ID, graph)
      } else Offset.Zero
      val end = if (toNode != null) {
          nodeRegistry.getPortPosition(edge.toNodeId, edge.toPortId, graph)
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
            nodeRegistry.getPortPosition(sourcePort.nodeId, sourcePort.id, graph)
          drawPath(
            path = createSplinePath(start, pointerPos),
            color = activeEdgeColor,
            style = DrawStroke(width = 3f),
          )
        }
    }
  }
}

@Composable
fun SelectionActionMenu(
  isSelectionMode: Boolean,
  onSelectAll: () -> Unit,
  onDuplicateSelected: () -> Unit,
  onDeleteSelected: () -> Unit,
  onDoneSelection: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (isSelectionMode) {
    if (showDeleteConfirmation) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        title = { Text(stringResource(R.string.bg_delete_nodes)) },
        text = { Text(stringResource(R.string.bg_delete_nodes_confirmation)) },
        confirmButton = {
          TextButton(
            onClick = {
              onDeleteSelected()
              showDeleteConfirmation = false
            }
          ) {
            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.bg_cancel)) }
        },
      )
    }

    Surface(
      modifier = modifier
        .padding(start = 16.dp, top = 80.dp)
        .wrapContentSize(),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 4.dp
    ) {
      Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(onClick = onSelectAll) {
          Text(stringResource(R.string.bg_select_all))
        }
        Button(onClick = onDuplicateSelected) {
          Text(stringResource(R.string.bg_duplicate))
        }
        Button(onClick = { showDeleteConfirmation = true }) {
          Text(stringResource(R.string.delete))
        }
        Button(onClick = onDoneSelection) {
          Text(stringResource(R.string.done))
        }
      }
    }
  }
}

@Composable
fun TrashCanArea(
  graph: BrushGraph,
  draggingNodeId: String?,
  draggingPointerPos: Offset?,
  parentWidth: Float,
  parentHeight: Float,
  trashCenterPaddingPx: Float,
  bottomPadding: Dp,
  rightPadding: Dp = 0.dp,
  onNodeDelete: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val draggingNode = graph.nodes.find { it.id == draggingNodeId }
  val density = LocalDensity.current
  val rightPaddingPx = with(density) { rightPadding.toPx() }
  val trashCenterX = (parentWidth - rightPaddingPx) / 2f

  if (draggingNode != null && draggingNode.data !is NodeData.Family) {
    Surface(
      modifier =
        modifier.padding(bottom = bottomPadding).graphicsLayer {
          val isOver =
            draggingPointerPos?.let { pos ->
              val trashCenter = Offset(trashCenterX, parentHeight - trashCenterPaddingPx)
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
          contentDescription = stringResource(R.string.bg_cd_delete),
          tint = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }
}
