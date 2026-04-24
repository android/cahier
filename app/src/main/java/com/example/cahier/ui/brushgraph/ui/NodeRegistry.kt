@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cahier.ui.brushgraph.data.PortSide
import com.example.cahier.ui.brushgraph.data.Port
import com.example.cahier.ui.brushgraph.data.BrushGraph
import com.example.cahier.ui.brushgraph.data.getVisiblePorts
import com.example.cahier.ui.brushgraph.data.NODE_PADDING_VERTICAL
import com.example.cahier.ui.brushgraph.data.INPUT_ROW_HEIGHT

/** Registry to track the actual position of ports and sizes of nodes on the screen. */
data class PortKey(val nodeId: String, val portId: String)

class NodeRegistry {
  private val portPositions = mutableStateMapOf<PortKey, Offset>()
  private val nodeSizes = mutableStateMapOf<String, Size>()

  fun updatePort(nodeId: String, portId: String, position: Offset) {
    portPositions[PortKey(nodeId, portId)] = position
  }

  fun getPortPosition(nodeId: String, portId: String, graph: BrushGraph, useFallbackOnly: Boolean = false): Offset {
    val stored = portPositions[PortKey(nodeId, portId)]
    if (stored != null && !useFallbackOnly) return stored
    
    val node = graph.nodes.find { it.id == nodeId } ?: return Offset.Zero
    
    // Special handling for output port which is not in visiblePorts
    if (portId == "output") {
      val w = node.data.width()
      val yOffset = NODE_PADDING_VERTICAL + node.data.titleHeight() + 0.5f * INPUT_ROW_HEIGHT
      return Offset(node.position.x + w, node.position.y + yOffset)
    }
    
    val visiblePorts = node.getVisiblePorts(graph)
    val port = visiblePorts.find { it.id == portId } ?: return Offset.Zero
    val sameSidePorts = visiblePorts.filter { it.side == port.side }
    val index = sameSidePorts.indexOf(port)
    
    val w = node.data.width()
    val yOffset = NODE_PADDING_VERTICAL + 
                  node.data.titleHeight() + 
                  (index + 0.5f) * INPUT_ROW_HEIGHT

    val relativeOffset = when (port.side) {
        PortSide.INPUT -> Offset(0f, yOffset)
        PortSide.OUTPUT -> Offset(w, yOffset)
    }
    
    val nodeOffset = Offset(node.position.x, node.position.y)
    return nodeOffset + relativeOffset
  }

  fun updateNodeSize(nodeId: String, size: Size) {
    nodeSizes[nodeId] = size
  }

  fun getNodeSize(nodeId: String): Size? {
    return nodeSizes[nodeId]
  }

  fun findNearestPort(pos: Offset, fromNodeId: String, graph: BrushGraph): Port? {
    val thresholdSq = 3000f
    var nearestPort: Port? = null
    var minDistanceSq = thresholdSq

    for (node in graph.nodes) {
      val visiblePorts = node.getVisiblePorts(graph)
      visiblePorts.forEachIndexed { index, port ->
        // Only snap to input ports.
        if (port.side == PortSide.INPUT) {
          // Ignore occupied ports (unless it's the same edge being edited).
          val existingEdge = graph.edges.find { it.toNodeId == port.nodeId && it.toPortId == port.id }
          if (existingEdge != null && existingEdge.fromNodeId != fromNodeId) {
            return@forEachIndexed // Occupied by another node's edge!
          }
          
          val portPos = getPortPosition(port.nodeId, port.id, graph)
            
          val distSq = (pos - portPos).getDistanceSquared()

          if (distSq < minDistanceSq) {
            minDistanceSq = distSq
            nearestPort = port
          }
        }
      }
    }

    return nearestPort
  }

  fun deletePort(nodeId: String, portId: String) {
    portPositions.remove(PortKey(nodeId, portId))
  }

  fun clearNode(nodeId: String) {
    val keysToRemove = portPositions.keys.filter { it.nodeId == nodeId }
    keysToRemove.forEach { portPositions.remove(it) }
    nodeSizes.remove(nodeId)
  }

  fun clear() {
    portPositions.clear()
    nodeSizes.clear()
  }
}
