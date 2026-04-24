@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.converters

import androidx.ink.brush.BrushFamily
import androidx.ink.storage.encode
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphPoint
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

    val coatPortIds = (0 until family.coatsCount).map { UUID.randomUUID().toString() }
    val familyData =
      NodeData.Family(
        clientBrushFamilyId = family.clientBrushFamilyId,
        developerComment = family.developerComment,
        inputModel = family.inputModel,
        coatPortIds = coatPortIds,
      )
    val familyNodeX = 1600f
    nodes.add(GraphNode(id = familyNodeId, data = familyData, position = GraphPoint(familyNodeX, 0f)))

    for (index in 0 until family.coatsCount) {
      val coat = family.getCoats(index)
      val coatId = UUID.randomUUID().toString()
      val paintPortIds = (0 until coat.paintPreferencesCount).map { UUID.randomUUID().toString() }
      val coatData = NodeData.Coat(paintPortIds = paintPortIds)
      val coatX = familyNodeX - coatData.width() - HORIZONTAL_GAP
      val coatY = nextY
      val coatNode = GraphNode(id = coatId, data = coatData, position = GraphPoint(coatX, coatY))
      nodes.add(coatNode)
      edges.add(
        GraphEdge(
          fromNodeId = coatId,
          toNodeId = familyNodeId,
          toPortId = coatPortIds[index]
        )
      )

      val tipX = coatX - NodeData.Tip(coat.tip).width() - HORIZONTAL_GAP
      val (tipId, tipOutputPortId, tipSubtreeMaxY) = convertTip(coat.tip, nodes, edges, tipX, coatY)
      edges.add(
        GraphEdge(
          fromNodeId = tipId,
          toNodeId = coatId,
          toPortId = coatData.tipPortId
        )
      )

      var paintIndex = 0
      var currentPaintY = tipSubtreeMaxY + VERTICAL_GAP
      var maxPaintSubtreeY = currentPaintY
      for (paint in coat.paintPreferencesList) {
        val paintData = NodeData.Paint(paint)
        val paintX = coatX - paintData.width() - HORIZONTAL_GAP
        val (paintId, paintOutputPortId, paintSubtreeMaxY) = convertPaint(paint, nodes, edges, paintX, currentPaintY)
        edges.add(
          GraphEdge(
            fromNodeId = paintId,
            toNodeId = coatId,
            toPortId = paintPortIds[paintIndex++]
          )
        )
        currentPaintY = paintSubtreeMaxY + VERTICAL_GAP
        maxPaintSubtreeY = maxOf(maxPaintSubtreeY, paintSubtreeMaxY)
      }

      val coatHeight = coatData.height(coat.paintPreferencesCount + 2)
      nextY = maxOf(coatY + coatHeight, maxPaintSubtreeY) + VERTICAL_GAP * 4
    }

    return BrushGraph(nodes = nodes, edges = edges)
  }

  private fun convertTip(
    tip: ProtoBrushTip,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
  ): Triple<String, String, Float> {
    val tipId = UUID.randomUUID().toString()
    val behaviorPortIds = (0 until tip.behaviorsCount).map { UUID.randomUUID().toString() }
    val tipData = NodeData.Tip(tip, behaviorPortIds = behaviorPortIds)
    nodes.add(GraphNode(id = tipId, data = tipData, position = GraphPoint(x, y)))

    var currentY = y
    var behaviorIndex = 0
    for (behavior in tip.behaviorsList) {
      // Reconstruct the graph from the post-order list of nodes.
      val (terminalNodes, behaviorMaxY) = convertBehaviorGraph(behavior, nodes, edges, x, currentY)
      for ((terminalId, _) in terminalNodes) {
        edges.add(
          GraphEdge(
            fromNodeId = terminalId,
            toNodeId = tipId,
            toPortId = behaviorPortIds[behaviorIndex]
          )
        )
      }
      behaviorIndex++
      currentY = behaviorMaxY + VERTICAL_GAP
    }

    return Triple(tipId, "output", maxOf(y + tipData.height(tip.behaviorsCount + 1), currentY - VERTICAL_GAP))
  }

  private fun convertPaint(
    paint: ProtoBrushPaint,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
  ): Triple<String, String, Float> {
    val paintId = UUID.randomUUID().toString()
    val texturePortIds = (0 until paint.textureLayersCount).map { UUID.randomUUID().toString() }
    val colorPortIds = (0 until paint.colorFunctionsCount).map { UUID.randomUUID().toString() }
    val paintData = NodeData.Paint(paint, texturePortIds = texturePortIds, colorPortIds = colorPortIds)
    nodes.add(GraphNode(id = paintId, data = paintData, position = GraphPoint(x, y)))

    var currentY = y
    var layerIndex = 0
    for (layer in paint.textureLayersList) {
      val layerId = UUID.randomUUID().toString()
      val layerData = NodeData.TextureLayer(layer)
      nodes.add(
        GraphNode(
          id = layerId,
          data = layerData,
          position = GraphPoint(x - layerData.width() - HORIZONTAL_GAP, currentY),
        )
      )
      edges.add(
        GraphEdge(
          fromNodeId = layerId,
          toNodeId = paintId,
          toPortId = texturePortIds[layerIndex++]
        )
      )
      currentY += layerData.height() + VERTICAL_GAP
    }

    var colorIndex = 0
    for (cf in paint.colorFunctionsList) {
      val cfId = UUID.randomUUID().toString()
      val cfData = NodeData.ColorFunction(cf)
      nodes.add(
        GraphNode(
          id = cfId,
          data = cfData,
          position = GraphPoint(x - cfData.width() - HORIZONTAL_GAP, currentY),
        )
      )
      edges.add(
        GraphEdge(
          fromNodeId = cfId,
          toNodeId = paintId,
          toPortId = colorPortIds[colorIndex++]
        )
      )
      currentY += cfData.height() + VERTICAL_GAP
    }

    val paintHeight = paintData.height(paint.textureLayersCount + 1 + paint.colorFunctionsCount + 1)
    return Triple(paintId, "output", maxOf(y + paintHeight, currentY - VERTICAL_GAP))
  }

  private fun convertBehaviorGraph(
    behavior: ProtoBrushBehavior,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    tipX: Float,
    startY: Float,
  ): Pair<List<Pair<String, String>>, Float> {
    val behaviorId = UUID.randomUUID().toString()
    val nodeStack = mutableListOf<InternalNodeInfo>()
    val behaviorNodes = mutableListOf<InternalNodeInfo>()

    for (protoNode in behavior.nodesList) {
      val nodeId = UUID.randomUUID().toString()
      // Temporary NodeData to get inputCount
      val tempNodeData = NodeData.Behavior(
        node = protoNode,
        developerComment = behavior.developerComment,
        behaviorId = behaviorId
      )
      val inputCount = tempNodeData.inputLabels().size
      
      val children = mutableListOf<InternalNodeInfo>()
      for (i in 0 until inputCount) {
        if (nodeStack.isNotEmpty()) {
          children.add(0, nodeStack.removeAt(nodeStack.size - 1))
        }
      }
      
      val inputPortIds = (0 until children.size).map { UUID.randomUUID().toString() }
      val nodeData = NodeData.Behavior(
        node = protoNode,
        developerComment = behavior.developerComment,
        behaviorId = behaviorId,
        inputPortIds = inputPortIds
      )
      
      val info = InternalNodeInfo(nodeId, nodeData, children)
      behaviorNodes.add(info)
      
      if (protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
          protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
        nodeStack.add(info)
      }
    }

    val terminalNodeInfos = listOfNotNull(behaviorNodes.lastOrNull())
    
    var currentY = startY
    val assignedNodes = mutableSetOf<String>()
    var maxYReached = startY
    val maxYPerDepth = mutableMapOf<Int, Float>()

    fun layoutNode(info: InternalNodeInfo, depth: Int): Float {
        if (assignedNodes.contains(info.id)) {
            // Find existing position if already assigned
            return nodes.find { it.id == info.id }?.position?.y?.plus(info.data.height() / 2f) ?: 0f
        }
        
        val x = tipX - (depth + 1) * (info.data.width() + HORIZONTAL_GAP)
        val portCount = when (info.data.node.nodeCase) {
            ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> info.children.size + 1
            ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
                val numSets = maxOf(1, info.children.size / 2)
                numSets * 2 + 1
            }
            else -> {
                val labels = info.data.inputLabels()
                if (labels.size == 1) {
                    maxOf(1, info.children.size) + 1
                } else {
                    labels.size
                }
            }
        }
        val nodeHeight = info.data.height(portCount)
        
        val centerY = if (info.children.isEmpty()) {
            val y = currentY + nodeHeight / 2f
            currentY += nodeHeight + VERTICAL_GAP
            y
        } else {
            val childYCenters = info.children.map { layoutNode(it, depth + 1) }
            childYCenters.average().toFloat()
        }
        
        val minY = maxYPerDepth[depth] ?: startY
        val finalY = maxOf(centerY - nodeHeight / 2f, minY)
        maxYPerDepth[depth] = finalY + nodeHeight + VERTICAL_GAP
        
        nodes.add(GraphNode(id = info.id, data = info.data, position = GraphPoint(x, finalY)))
        maxYReached = maxOf(maxYReached, finalY + nodeHeight)
        
        info.children.forEachIndexed { index, child ->
            edges.add(
              GraphEdge(
                fromNodeId = child.id,
                toNodeId = info.id,
                toPortId = info.data.inputPortIds[index]
              )
            )
        }
        assignedNodes.add(info.id)
        return finalY + nodeHeight / 2f
    }

    for (root in terminalNodeInfos) {
        layoutNode(root, 0)
    }
    
    return terminalNodeInfos.map { it.id to "output" } to maxYReached
  }

  private data class InternalNodeInfo(
    val id: String, 
    val data: NodeData.Behavior, 
    val children: List<InternalNodeInfo>
  )
}
