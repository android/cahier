@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.converters

import androidx.compose.ui.geometry.Offset
import androidx.ink.brush.BrushFamily
import androidx.ink.storage.encode
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.NodeData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction

/** Utility to convert a functional [BrushFamily] into a [BrushGraph] data model. */
object BrushGraphConverter {

  private const val HORIZONTAL_GAP = 100f
  private const val VERTICAL_GAP = 40f

  /** Converts a [BrushFamily] into a [BrushGraph]. */
  fun fromBrushFamily(family: BrushFamily): BrushGraph {
    val baos = ByteArrayOutputStream()
    family.encode(baos)
    val compressedBytes = baos.toByteArray()
    val bais = ByteArrayInputStream(compressedBytes)
    val proto = GZIPInputStream(bais).use { ProtoBrushFamily.parseFrom(it) }
    return fromProtoBrushFamily(proto)
  }

  /** Converts a [ProtoBrushFamily] into a [BrushGraph]. */
  fun fromProtoBrushFamily(family: ProtoBrushFamily): BrushGraph {
    val nodes = mutableListOf<GraphNode>()
    val edges = mutableListOf<GraphEdge>()

    var nextY = 0f
    val familyNodeId = UUID.randomUUID().toString()

    val familyData =
      NodeData.Family(
        clientBrushFamilyId = family.clientBrushFamilyId,
        developerComment = family.developerComment,
        inputModel = family.inputModel,
        numCoats = family.coatsCount,
      )
    val familyNodeX = 1600f
    nodes.add(GraphNode(id = familyNodeId, data = familyData, position = Offset(familyNodeX, 0f)))

    for (index in 0 until family.coatsCount) {
      val coat = family.getCoats(index)
      val coatId = UUID.randomUUID().toString()
      val coatData = NodeData.Coat
      val coatX = familyNodeX - coatData.width() - HORIZONTAL_GAP
      val coatY = nextY
      val coatNode = GraphNode(id = coatId, data = coatData, position = Offset(coatX, coatY))
      nodes.add(coatNode)
      edges.add(GraphEdge(fromNodeId = coatId, toNodeId = familyNodeId, toInputIndex = index))

      val tipX = coatX - NodeData.Tip(coat.tip).width() - HORIZONTAL_GAP
      val (tipId, tipSubtreeMaxY) = convertTip(coat.tip, nodes, edges, tipX, coatY)
      edges.add(GraphEdge(fromNodeId = tipId, toNodeId = coatId, toInputIndex = 0))

      val paint = coat.paintPreferencesList.firstOrNull() ?: ProtoBrushPaint.getDefaultInstance()
      val paintData = NodeData.Paint(paint)
      val paintX = coatX - paintData.width() - HORIZONTAL_GAP
      val (paintId, paintSubtreeMaxY) =
        convertPaint(paint, nodes, edges, paintX, tipSubtreeMaxY + VERTICAL_GAP)
      edges.add(GraphEdge(fromNodeId = paintId, toNodeId = coatId, toInputIndex = 1))

      nextY = maxOf(coatY + coatData.height(), paintSubtreeMaxY) + VERTICAL_GAP * 4
    }

    return BrushGraph(nodes = nodes, edges = edges)
  }

  private fun convertTip(
    tip: ProtoBrushTip,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
  ): Pair<String, Float> {
    val tipId = UUID.randomUUID().toString()
    val tipData = NodeData.Tip(tip)
    nodes.add(GraphNode(id = tipId, data = tipData, position = Offset(x, y)))

    var currentY = y
    for (behavior in tip.behaviorsList) {
      // Reconstruct the graph from the post-order list of nodes.
      val (terminalNodeIds, behaviorMaxY) = convertBehaviorGraph(behavior, nodes, edges, x, currentY)
      for (terminalId in terminalNodeIds) {
        edges.add(GraphEdge(fromNodeId = terminalId, toNodeId = tipId, toInputIndex = 0))
      }
      currentY = behaviorMaxY + VERTICAL_GAP
    }

    return tipId to maxOf(y + tipData.height(), currentY - VERTICAL_GAP)
  }

  private fun convertPaint(
    paint: ProtoBrushPaint,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
  ): Pair<String, Float> {
    val paintId = UUID.randomUUID().toString()
    val paintData = NodeData.Paint(paint)
    nodes.add(GraphNode(id = paintId, data = paintData, position = Offset(x, y)))

    var currentY = y
    for (layer in paint.textureLayersList) {
      val layerId = UUID.randomUUID().toString()
      val layerData = NodeData.TextureLayer(layer)
      nodes.add(
        GraphNode(
          id = layerId,
          data = layerData,
          position = Offset(x - layerData.width() - HORIZONTAL_GAP, currentY),
        )
      )
      edges.add(GraphEdge(fromNodeId = layerId, toNodeId = paintId, toInputIndex = 0))
      currentY += layerData.height() + VERTICAL_GAP
    }

    for (cf in paint.colorFunctionsList) {
      val cfId = UUID.randomUUID().toString()
      val cfData = NodeData.ColorFunc(cf)
      nodes.add(
        GraphNode(
          id = cfId,
          data = cfData,
          position = Offset(x - cfData.width() - HORIZONTAL_GAP, currentY),
        )
      )
      edges.add(GraphEdge(fromNodeId = cfId, toNodeId = paintId, toInputIndex = 1))
      currentY += cfData.height() + VERTICAL_GAP
    }

    return paintId to maxOf(y + paintData.height(), currentY - VERTICAL_GAP)
  }

  private fun convertBehaviorGraph(
    behavior: ProtoBrushBehavior,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    tipX: Float,
    startY: Float,
  ): Pair<List<String>, Float> {
    val nodeStack = mutableListOf<InternalNodeInfo>()
    val behaviorNodes = mutableListOf<InternalNodeInfo>()

    for (protoNode in behavior.nodesList) {
      val nodeId = UUID.randomUUID().toString()
      val nodeData = NodeData.Behavior(protoNode)
      val inputCount = nodeData.inputLabels().size
      
      val children = mutableListOf<InternalNodeInfo>()
      for (i in 0 until inputCount) {
        if (nodeStack.isNotEmpty()) {
          children.add(0, nodeStack.removeAt(nodeStack.size - 1))
        }
      }
      
      val info = InternalNodeInfo(nodeId, nodeData, children)
      behaviorNodes.add(info)
      
      if (protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
          protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
        nodeStack.add(info)
      }
    }

    val terminalNodeInfos = behaviorNodes.filter { it.data.node.let { n -> 
        n.nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE || 
        n.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE 
    }}
    
    var currentY = startY
    val assignedNodes = mutableSetOf<String>()
    var maxYReached = startY

    fun layoutNode(info: InternalNodeInfo, depth: Int): Float {
        if (assignedNodes.contains(info.id)) {
            // Find existing position if already assigned
            return nodes.find { it.id == info.id }?.position?.y?.plus(info.data.height() / 2f) ?: 0f
        }
        
        val x = tipX - (depth + 1) * (info.data.width() + HORIZONTAL_GAP)
        
        val centerY = if (info.children.isEmpty()) {
            val y = currentY + info.data.height() / 2f
            currentY += info.data.height() + VERTICAL_GAP
            y
        } else {
            val childYCenters = info.children.map { layoutNode(it, depth + 1) }
            childYCenters.average().toFloat()
        }
        
        val finalY = centerY - info.data.height() / 2f
        nodes.add(GraphNode(id = info.id, data = info.data, position = Offset(x, finalY)))
        maxYReached = maxOf(maxYReached, finalY + info.data.height())
        
        info.children.forEachIndexed { index, child ->
            edges.add(GraphEdge(fromNodeId = child.id, toNodeId = info.id, toInputIndex = index))
        }
        assignedNodes.add(info.id)
        return centerY
    }

    for (root in terminalNodeInfos) {
        layoutNode(root, 0)
    }
    
    return terminalNodeInfos.map { it.id } to maxYReached
  }

  private data class InternalNodeInfo(
    val id: String, 
    val data: NodeData.Behavior, 
    val children: List<InternalNodeInfo>
  )
}
