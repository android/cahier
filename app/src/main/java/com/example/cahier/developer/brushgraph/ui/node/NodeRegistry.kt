@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.developer.brushgraph.ui.NODE_PADDING_VERTICAL
import com.example.cahier.developer.brushgraph.ui.INPUT_ROW_HEIGHT
import com.example.cahier.developer.brushgraph.ui.width
import com.example.cahier.developer.brushgraph.ui.titleHeight

/** Registry to track the actual position of ports and sizes of nodes on the screen. */
data class PortKey(val nodeId: String, val portId: String)

class NodeRegistry {
  private val portPositions = mutableStateMapOf<PortKey, Offset>()
  private val nodePositions = mutableStateMapOf<String, Offset>()

  fun updatePort(nodeId: String, portId: String, position: Offset) {
    portPositions[PortKey(nodeId, portId)] = position
  }

  fun updateNodePosition(nodeId: String, position: Offset) {
    nodePositions[nodeId] = position
  }

  fun getNodePosition(nodeId: String): Offset? {
    return nodePositions[nodeId]
  }

  fun getPortPosition(nodeId: String, portId: String, graph: BrushGraph, useFallbackOnly: Boolean = false): Offset {
    val stored = portPositions[PortKey(nodeId, portId)]
    if (stored != null && !useFallbackOnly) return stored
    
    val node = graph.nodes.find { it.id == nodeId } ?: return Offset.Zero
    
    // Special handling for output port which is not in visiblePorts
    val nodePos = nodePositions[nodeId] ?: Offset.Zero
    if (portId == "output") {
      val w = node.data.width()
      val yOffset = NODE_PADDING_VERTICAL + node.data.titleHeight() + 0.5f * INPUT_ROW_HEIGHT
      return Offset(nodePos.x + w, nodePos.y + yOffset)
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
    
    val nodeOffset = nodePos
    return nodeOffset + relativeOffset
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
    nodePositions.remove(nodeId)
  }

  fun clear() {
    portPositions.clear()
    nodePositions.clear()
  }
}
