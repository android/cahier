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

/** Registry to track the actual position of ports and sizes of nodes on the screen. */
class NodeRegistry {
  private val portPositions = mutableStateMapOf<Port, Offset>()
  private val nodeSizes = mutableStateMapOf<String, Size>()

  fun updatePort(nodeId: String, side: PortSide, index: Int, position: Offset) {
    portPositions[Port(nodeId, side, index)] = position
  }

  fun getPort(nodeId: String, side: PortSide, index: Int): Offset? {
    return portPositions[Port(nodeId, side, index)]
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
      for (port in visiblePorts) {
        // Only snap to input ports.
        if (port.side == PortSide.INPUT) {
          // Ignore occupied ports!
          val isOccupied = graph.edges.any { it.toPort.nodeId == port.nodeId && it.toPort.index == port.index }

          if (isOccupied) {
            continue
          }
          
          val portPos = getPort(port.nodeId, port.side, port.index)
            ?: (node.position + node.data.getPortPosition(port.side, port.index))
            
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

  fun deletePort(port: Port) {
    portPositions.remove(port)
  }

  fun deletePortAndShift(port: Port) {
    portPositions.remove(port)
    
    val keysToMove = portPositions.keys.filter { it.nodeId == port.nodeId && it.side == port.side && it.index > port.index }
    val sortedKeys = keysToMove.sortedBy { it.index }
    
    for (key in sortedKeys) {
      val pos = portPositions.remove(key)
      if (pos != null) {
        portPositions[key.copy(index = key.index - 1)] = pos
      }
    }
  }

  fun shiftPorts(nodeId: String, side: PortSide, fromIndex: Int, delta: Int) {
    val keysToMove = portPositions.keys.filter { it.nodeId == nodeId && it.side == side && it.index >= fromIndex }
    val sortedKeys = if (delta < 0) keysToMove.sortedBy { it.index } else keysToMove.sortedByDescending { it.index }
    
    for (key in sortedKeys) {
      val pos = portPositions.remove(key)
      if (pos != null) {
        portPositions[key.copy(index = key.index + delta)] = pos
      }
    }
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
