@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.cahier.ui.brushgraph.model.PortSide

/** Registry to track the actual position of ports and sizes of nodes on the screen. */
class NodeRegistry {
  private val portPositions = mutableStateMapOf<Triple<String, PortSide, Int>, Offset>()
  private val nodeSizes = mutableStateMapOf<String, Size>()

  fun updatePort(nodeId: String, side: PortSide, index: Int, position: Offset) {
    portPositions[Triple(nodeId, side, index)] = position
  }

  fun getPort(nodeId: String, side: PortSide, index: Int): Offset? {
    return portPositions[Triple(nodeId, side, index)]
  }

  fun updateNodeSize(nodeId: String, size: Size) {
    nodeSizes[nodeId] = size
  }

  fun getNodeSize(nodeId: String): Size? {
    return nodeSizes[nodeId]
  }

  fun findNearestPort(pos: Offset, fromNodeId: String): Triple<String, PortSide, Int>? {
    val thresholdSq = 3000f
    var nearestPort: Triple<String, PortSide, Int>? = null
    var minDistanceSq = thresholdSq

    for ((key, portPos) in portPositions) {
      val (_, side, _) = key
      // Only snap to input ports.
      if (side == PortSide.INPUT) {
        val distSq = (pos - portPos).getDistanceSquared()
        if (distSq < minDistanceSq) {
          minDistanceSq = distSq
          nearestPort = key
        }
      }
    }
    return nearestPort
  }

  fun clearNode(nodeId: String) {
    val keysToRemove = portPositions.keys.filter { it.first == nodeId }
    keysToRemove.forEach { portPositions.remove(it) }
    nodeSizes.remove(nodeId)
  }

  fun clear() {
    portPositions.clear()
    nodeSizes.clear()
  }
}
