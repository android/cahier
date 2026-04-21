@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.Port
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import com.example.cahier.ui.brushgraph.model.getPortPosition

/** Registry to track the actual position of ports and sizes of nodes on the screen. */
data class PortKey(val nodeId: String, val portId: String)

class NodeRegistry {
  private val portPositions = mutableStateMapOf<PortKey, Offset>()
  private val nodeSizes = mutableStateMapOf<String, Size>()

  fun updatePort(nodeId: String, portId: String, position: Offset) {
    portPositions[PortKey(nodeId, portId)] = position
  }

  fun getPort(nodeId: String, portId: String): Offset? {
    return portPositions[PortKey(nodeId, portId)]
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
          
          val portPos = getPort(port.nodeId, port.id)
            ?: (node.position + node.getPortPosition(port.id, graph))
            
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
