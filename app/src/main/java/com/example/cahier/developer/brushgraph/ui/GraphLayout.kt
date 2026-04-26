package com.example.cahier.developer.brushgraph.ui

import androidx.compose.ui.geometry.Offset
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import java.util.UUID

const val NODE_WIDTH = 300f
const val NODE_PADDING_VERTICAL = 8f
const val NODE_PADDING_BOTTOM = 12f
const val TITLE_AREA_HEIGHT = 64f
const val SUBTITLE_LINE_HEIGHT = 32f
const val PREVIEW_AREA_HEIGHT = 64f
const val INPUT_ROW_HEIGHT = 60f

const val INSPECTOR_WIDTH_LANDSCAPE = 320f
const val INSPECTOR_HEIGHT_PORTRAIT = 400f
const val PREVIEW_HEIGHT_EXPANDED = 200f
const val PREVIEW_HEIGHT_COLLAPSED = 40f

fun NodeData.width(): Float = when (this) {
    is NodeData.Family -> 3 * NODE_WIDTH
    else -> NODE_WIDTH
}

fun NodeData.height(portCount: Int = inputLabels().size): Float {
    val previewH = if (this is NodeData.ColorFunction || this is NodeData.TextureLayer) PREVIEW_AREA_HEIGHT else 0f
    return NODE_PADDING_VERTICAL +
      titleHeight() +
      previewH +
      maxOf(portCount, 1) * INPUT_ROW_HEIGHT +
      NODE_PADDING_BOTTOM
}

fun NodeData.titleHeight(): Float {
    val subs = subtitles()
    val subtitleHeight = subs.size * SUBTITLE_LINE_HEIGHT
    if (subtitleHeight > 0f) {
      return TITLE_AREA_HEIGHT + subtitleHeight
    }
    if (this is NodeData.Tip || this is NodeData.Coat) {
      return TITLE_AREA_HEIGHT + PREVIEW_AREA_HEIGHT
    }
    return TITLE_AREA_HEIGHT
}

object GraphLayout {
  private const val HORIZONTAL_GAP = 200f
  private const val VERTICAL_GAP = 80f
  private const val FAMILY_NODE_X = 1600f
  fun calculateLayout(graph: BrushGraph): Map<String, Offset> {
    val positions = mutableMapOf<String, Offset>()
    val familyNode = graph.nodes.find { it.data is NodeData.Family } ?: return positions
    
    positions[familyNode.id] = Offset(FAMILY_NODE_X, 0f)
    
    val coatEdges = graph.edges.filter { it.toNodeId == familyNode.id }
    val familyData = familyNode.data as NodeData.Family
    
    // Sort coats by port order if possible, or just as they come
    val coatNodes = familyData.coatPortIds.mapNotNull { portId ->
      coatEdges.find { it.toPortId == portId }?.fromNodeId
    }.mapNotNull { id -> graph.nodes.find { it.id == id } }

    var nextY = 0f
    
    for (coatNode in coatNodes) {
      val coatData = coatNode.data as NodeData.Coat
      val coatX = FAMILY_NODE_X - coatData.width() - HORIZONTAL_GAP
      val coatY = nextY
      positions[coatNode.id] = Offset(coatX, coatY)
      
      // Layout Tip
      val tipEdge = graph.edges.find { it.toNodeId == coatNode.id && it.toPortId == coatData.tipPortId }
      val tipNode = tipEdge?.fromNodeId?.let { id -> graph.nodes.find { it.id == id } }
      
      var tipSubtreeMaxY = coatY
      if (tipNode != null) {
        val tipData = tipNode.data as NodeData.Tip
        val tipX = coatX - tipData.width() - HORIZONTAL_GAP
        positions[tipNode.id] = Offset(tipX, coatY)
        
        // Layout behavior graph connected to tip
        val behaviorEdges = graph.edges.filter { it.toNodeId == tipNode.id }
        val behaviorRootIds = tipData.behaviorPortIds.mapNotNull { portId ->
          behaviorEdges.find { it.toPortId == portId }?.fromNodeId
        }
        
        var currentY = coatY
        val maxYPerDepth = mutableMapOf<Int, Float>()
        val assignedNodeIds = mutableSetOf<String>()
        val nodeSubtreeMaxY = mutableMapOf<String, Float>()
        
        for (rootId in behaviorRootIds) {
          val maxY = layoutBehaviorNode(rootId, graph, positions, tipX, currentY, 0, maxYPerDepth, assignedNodeIds, nodeSubtreeMaxY)
          currentY = maxY + VERTICAL_GAP
        }
        tipSubtreeMaxY = maxOf(coatY + tipData.height(tipData.behaviorPortIds.size + 1), currentY - VERTICAL_GAP)
      }
      
      // Layout Paints
      var currentPaintY = tipSubtreeMaxY + VERTICAL_GAP
      var maxPaintSubtreeY = currentPaintY
      
      val paintEdges = coatData.paintPortIds.mapNotNull { portId ->
        graph.edges.find { it.toNodeId == coatNode.id && it.toPortId == portId }
      }
      
      for (paintEdge in paintEdges) {
        val paintNode = graph.nodes.find { it.id == paintEdge.fromNodeId }
        if (paintNode != null) {
          val paintData = paintNode.data as NodeData.Paint
          val paintX = coatX - paintData.width() - HORIZONTAL_GAP
          positions[paintNode.id] = Offset(paintX, currentPaintY)
          
          // Layout texture layers and color functions
          var subY = currentPaintY
          
          val textureEdges = graph.edges.filter { it.toNodeId == paintNode.id && paintData.texturePortIds.contains(it.toPortId) }
          for (te in textureEdges) {
            val texNode = graph.nodes.find { it.id == te.fromNodeId }
            if (texNode != null) {
              val texX = paintX - texNode.data.width() - HORIZONTAL_GAP
              positions[texNode.id] = Offset(texX, subY)
              subY += texNode.data.height() + VERTICAL_GAP
            }
          }
          
          val colorEdges = graph.edges.filter { it.toNodeId == paintNode.id && paintData.colorPortIds.contains(it.toPortId) }
          for (ce in colorEdges) {
            val colNode = graph.nodes.find { it.id == ce.fromNodeId }
            if (colNode != null) {
              val colX = paintX - colNode.data.width() - HORIZONTAL_GAP
              positions[colNode.id] = Offset(colX, subY)
              subY += colNode.data.height() + VERTICAL_GAP
            }
          }
          
          val paintHeight = paintData.height(paintData.texturePortIds.size + 1 + paintData.colorPortIds.size + 1)
          currentPaintY = maxOf(currentPaintY + paintHeight, subY) + VERTICAL_GAP
          maxPaintSubtreeY = maxOf(maxPaintSubtreeY, currentPaintY)
        }
      }
      
      val coatHeight = coatData.height(coatData.paintPortIds.size + 2)
      nextY = maxOf(coatY + coatHeight, maxPaintSubtreeY) + VERTICAL_GAP * 4
    }
    
    return positions
  }

  private fun layoutBehaviorNode(
    nodeId: String,
    graph: BrushGraph,
    positions: MutableMap<String, Offset>,
    parentX: Float,
    desiredY: Float,
    depth: Int,
    maxYPerDepth: MutableMap<Int, Float>,
    assignedNodeIds: MutableSet<String>,
    nodeSubtreeMaxY: MutableMap<String, Float>
  ): Float {
    val node = graph.nodes.find { it.id == nodeId } ?: return desiredY
    val data = node.data as? NodeData.Behavior ?: return desiredY
    
    if (assignedNodeIds.contains(nodeId)) {
      return nodeSubtreeMaxY[nodeId] ?: desiredY
    }
    
    val childEdges = graph.edges.filter { it.toNodeId == nodeId }
    val childNodes = data.inputPortIds.mapNotNull { portId ->
      childEdges.find { it.toPortId == portId }?.fromNodeId?.let { id -> graph.nodes.find { it.id == id } }
    }
    
    val x = parentX - (data.width() + HORIZONTAL_GAP)
    val nodeHeight = data.height(childNodes.size + 1)
    
    val minY = maxYPerDepth[depth] ?: desiredY
    val finalY = maxOf(desiredY, minY)
    positions[nodeId] = Offset(x, finalY)
    assignedNodeIds.add(nodeId)
    
    val nextParentX = x
    var currentChildY = finalY
    var maxChildYReached = finalY + nodeHeight
    
    for (i in childNodes.indices) {
      val child = childNodes[i]
      val targetY = if (i == 0) finalY else currentChildY + VERTICAL_GAP
      val cY = layoutBehaviorNode(child.id, graph, positions, nextParentX, targetY, depth + 1, maxYPerDepth, assignedNodeIds, nodeSubtreeMaxY)
      currentChildY = cY
      maxChildYReached = maxOf(maxChildYReached, cY)
    }
    
    maxYPerDepth[depth] = maxChildYReached + VERTICAL_GAP
    nodeSubtreeMaxY[nodeId] = maxChildYReached
    
    return maxChildYReached
  }
}
