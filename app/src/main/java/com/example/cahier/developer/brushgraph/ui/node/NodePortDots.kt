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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.ui.INPUT_ROW_HEIGHT
import com.example.cahier.developer.brushgraph.ui.NODE_PADDING_VERTICAL
import com.example.cahier.developer.brushgraph.ui.titleHeight
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.isPortReorderable
import kotlin.math.roundToInt

@Composable
fun BoxScope.NodePortDots(
  node: GraphNode,
  position: Offset,
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
    key(portKey) {
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
        portPosition = getPortPosition(port.id, true) - position,
        isReorderable = node.data.isPortReorderable(port, index, hasAddPort),
        isLargeHandle = node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE && index % 2 == 0,
        onReorderUpdate = { deltaY ->
          if (activeReorderPortIndex != index) {
            activeReorderPortIndex = index
            cumulativeDeltaY = 0f
          }
          cumulativeDeltaY += deltaY
          
          val originalY = getPortPosition(port.id, true).y - position.y
          var maxValidIndex = visiblePorts.size - 2 // Exclude add port
          var minValidIndex = if (node.data is NodeData.Coat) 1 else 0
          
          if (node.data is NodeData.Paint) {
              val textureEdges = graph.edges.filter { edge ->
                  val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
                  fromNode?.data is NodeData.TextureLayer && edge.toNodeId == node.id
              }
              val T = textureEdges.size
              
              val colorEdges = graph.edges.filter { edge ->
                  val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
                  fromNode?.data is NodeData.ColorFunction && edge.toNodeId == node.id
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
          
          val minDragY = NODE_PADDING_VERTICAL + node.data.titleHeight() + (0 + 0.5f) * INPUT_ROW_HEIGHT
          val maxDragY = NODE_PADDING_VERTICAL + node.data.titleHeight() + (visiblePorts.size - 1 + 0.5f) * INPUT_ROW_HEIGHT
          
          val requestedY = originalY + cumulativeDeltaY
          
          val isPolarTarget = node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
          
          val targetIndex = if (isPolarTarget) {
              val setSize = 2
              val currentSet = ((requestedY - NODE_PADDING_VERTICAL - node.data.titleHeight()) / (INPUT_ROW_HEIGHT * setSize) - 0.5f).roundToInt()
              currentSet * setSize
          } else {
              ((requestedY - NODE_PADDING_VERTICAL - node.data.titleHeight()) / INPUT_ROW_HEIGHT - 0.5f).roundToInt()
          }
          
          val currentY = requestedY.coerceIn(minDragY, maxDragY)
          cumulativeDeltaY = currentY - originalY
          
          if (targetIndex in minValidIndex..maxValidIndex && targetIndex != index) {
            onReorderPorts(node.id, index, targetIndex)
            cumulativeDeltaY -= (targetIndex - index) * INPUT_ROW_HEIGHT
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
      portPosition = getPortPosition("output", true) - position,
    )
  }
}
