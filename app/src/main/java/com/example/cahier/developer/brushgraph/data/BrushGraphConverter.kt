@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.data

import androidx.ink.brush.BrushFamily
import androidx.ink.storage.encode
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphPoint
import com.example.cahier.developer.brushgraph.data.NodeData
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

    val behaviorDeduplicationMap = mutableMapOf<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>()
    val assignedNodeIds = mutableSetOf<String>()
    val textureDeduplicationMap = mutableMapOf<ProtoBrushPaint.TextureLayer, String>()
    val colorDeduplicationMap = mutableMapOf<ProtoColorFunction, String>()

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
      val (tipId, tipOutputPortId, tipSubtreeMaxY) = convertTip(coat.tip, nodes, edges, tipX, coatY, behaviorDeduplicationMap, assignedNodeIds)
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
        val (paintId, paintOutputPortId, paintSubtreeMaxY) = convertPaint(paint, nodes, edges, paintX, currentPaintY, textureDeduplicationMap, colorDeduplicationMap)
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

    val initialGraph = BrushGraph(nodes = nodes, edges = edges)
    return deduplicateDownstream(initialGraph)
  }

  private fun convertTip(
    tip: ProtoBrushTip,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
    deduplicationMap: MutableMap<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>,
    assignedNodeIds: MutableSet<String>,
  ): Triple<String, String, Float> {
    val tipId = UUID.randomUUID().toString()
    val tempBehaviorPortIds = (0 until tip.behaviorsCount).map { UUID.randomUUID().toString() }
    val usedPortIds = mutableListOf<String>()

    var currentY = y
    var behaviorIndex = 0
    for (behavior in tip.behaviorsList) {
      // Reconstruct the graph from the post-order list of nodes.
      val (terminalNodes, behaviorMaxY) = convertBehaviorGraph(behavior, nodes, edges, x, currentY, deduplicationMap, assignedNodeIds)
      for ((terminalId, _) in terminalNodes) {
        val alreadyConnected = edges.any { it.toNodeId == tipId && it.fromNodeId == terminalId }
        if (!alreadyConnected) {
          val portId = tempBehaviorPortIds[behaviorIndex]
          edges.add(
            GraphEdge(
              fromNodeId = terminalId,
              toNodeId = tipId,
              toPortId = portId
            )
          )
          usedPortIds.add(portId)
        }
      }
      behaviorIndex++
      currentY = behaviorMaxY + VERTICAL_GAP
    }

    val tipData = NodeData.Tip(tip, behaviorPortIds = usedPortIds)
    nodes.add(GraphNode(id = tipId, data = tipData, position = GraphPoint(x, y)))

    return Triple(tipId, "output", maxOf(y + tipData.height(usedPortIds.size + 1), currentY - VERTICAL_GAP))
  }

  private fun convertPaint(
    paint: ProtoBrushPaint,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    x: Float,
    y: Float,
    textureDeduplicationMap: MutableMap<ProtoBrushPaint.TextureLayer, String>,
    colorDeduplicationMap: MutableMap<ProtoColorFunction, String>,
  ): Triple<String, String, Float> {
    val paintId = UUID.randomUUID().toString()
    val texturePortIds = (0 until paint.textureLayersCount).map { UUID.randomUUID().toString() }
    val colorPortIds = (0 until paint.colorFunctionsCount).map { UUID.randomUUID().toString() }
    val paintData = NodeData.Paint(paint, texturePortIds = texturePortIds, colorPortIds = colorPortIds)
    nodes.add(GraphNode(id = paintId, data = paintData, position = GraphPoint(x, y)))

    val tempTexturePortIds = texturePortIds
    val tempColorPortIds = colorPortIds
    val usedTexturePortIds = mutableListOf<String>()
    val usedColorPortIds = mutableListOf<String>()

    var currentY = y
    var layerIndex = 0
    for (layer in paint.textureLayersList) {
      val isNew = !textureDeduplicationMap.containsKey(layer)
      val layerId = textureDeduplicationMap.getOrPut(layer) { UUID.randomUUID().toString() }
      val layerData = NodeData.TextureLayer(layer)
      
      val alreadyConnected = edges.any { it.toNodeId == paintId && it.fromNodeId == layerId }
      if (!alreadyConnected) {
          val portId = tempTexturePortIds[layerIndex]
          edges.add(
            GraphEdge(
              fromNodeId = layerId,
              toNodeId = paintId,
              toPortId = portId
            )
          )
          usedTexturePortIds.add(portId)
      }
      
      if (isNew) {
          nodes.add(
            GraphNode(
              id = layerId,
              data = layerData,
              position = GraphPoint(x - layerData.width() - HORIZONTAL_GAP, currentY),
            )
          )
          currentY += layerData.height() + VERTICAL_GAP
      }
      layerIndex++
    }

    var colorIndex = 0
    for (cf in paint.colorFunctionsList) {
      val isNew = !colorDeduplicationMap.containsKey(cf)
      val cfId = colorDeduplicationMap.getOrPut(cf) { UUID.randomUUID().toString() }
      val cfData = NodeData.ColorFunction(cf)
      
      val alreadyConnected = edges.any { it.toNodeId == paintId && it.fromNodeId == cfId }
      if (!alreadyConnected) {
          val portId = tempColorPortIds[colorIndex]
          edges.add(
            GraphEdge(
              fromNodeId = cfId,
              toNodeId = paintId,
              toPortId = portId
            )
          )
          usedColorPortIds.add(portId)
      }
      
      if (isNew) {
          nodes.add(
            GraphNode(
              id = cfId,
              data = cfData,
              position = GraphPoint(x - cfData.width() - HORIZONTAL_GAP, currentY),
            )
          )
          currentY += cfData.height() + VERTICAL_GAP
      }
      colorIndex++
    }

    val finalPaintData = NodeData.Paint(paint, texturePortIds = usedTexturePortIds, colorPortIds = usedColorPortIds)
    nodes.removeIf { it.id == paintId }
    nodes.add(GraphNode(id = paintId, data = finalPaintData, position = GraphPoint(x, y)))

    val paintHeight = finalPaintData.height(usedTexturePortIds.size + 1 + usedColorPortIds.size + 1)
    return Triple(paintId, "output", maxOf(y + paintHeight, currentY - VERTICAL_GAP))
  }

  private fun convertBehaviorGraph(
    behavior: ProtoBrushBehavior,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    tipX: Float,
    startY: Float,
    deduplicationMap: MutableMap<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>,
    assignedNodeIds: MutableSet<String>,
  ): Pair<List<Pair<String, String>>, Float> {
    val behaviorId = UUID.randomUUID().toString()
    val nodeStack = mutableListOf<InternalNodeInfo>()
    val behaviorNodes = mutableListOf<InternalNodeInfo>()

    for (protoNode in behavior.nodesList) {
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
      
      val childrenIds = children.map { it.id }
      val key = Pair(protoNode, childrenIds)
            
      val existingInfo = deduplicationMap[key]
      if (existingInfo != null) {
          behaviorNodes.add(existingInfo)
          if (protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
              protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            nodeStack.add(existingInfo)
          }
          continue
      }
      
      val nodeId = UUID.randomUUID().toString()
      val inputPortIds = (0 until children.size).map { UUID.randomUUID().toString() }
      val nodeData = NodeData.Behavior(
        node = protoNode,
        developerComment = behavior.developerComment,
        behaviorId = behaviorId,
        inputPortIds = inputPortIds
      )
      
      val info = InternalNodeInfo(nodeId, nodeData, children)
      behaviorNodes.add(info)
      
      deduplicationMap[key] = info
      
      if (protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
          protoNode.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
        nodeStack.add(info)
      }
    }

    val terminalNodeInfos = listOfNotNull(behaviorNodes.lastOrNull())
    
    var currentY = startY
    var maxYReached = startY
    val maxYPerDepth = mutableMapOf<Int, Float>()

    fun layoutNode(info: InternalNodeInfo, depth: Int): Float {
        if (assignedNodeIds.contains(info.id)) {
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
        assignedNodeIds.add(info.id)
        return finalY + nodeHeight / 2f
    }

    for (root in terminalNodeInfos) {
        layoutNode(root, 0)
    }
    
    return terminalNodeInfos.map { it.id to "output" } to maxYReached
  }

  /**
   * Performs a top-down deduplication pass on behavior nodes.
   * 
   * NOTE: This method assumes that the `nodes` list is ordered bottom-up (sources first, 
   * then operators, then targets) as a result of the post-order traversal during construction. 
   * By processing the reversed list, we achieve top-down processing in a single pass. 
   * If the graph construction order changes in the future, this may need to be updated 
   * to perform a full topological sort first.
   */
  private fun deduplicateDownstream(graph: BrushGraph): BrushGraph {
      val nodes = graph.nodes.toMutableList()
      val edges = graph.edges.toMutableList()
      
      // Filter and reverse behavior nodes to process top-down
      val behaviorNodes = nodes.filter { it.data is NodeData.Behavior }.reversed()
      
      val removedNodeIds = mutableSetOf<String>()
      
      for (node in behaviorNodes) {
          if (removedNodeIds.contains(node.id)) continue
          
          val nodeData = node.data as NodeData.Behavior
          val isInterpolation = nodeData.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE
          val isBinaryOp = nodeData.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE
          if (isInterpolation || isBinaryOp) continue
          
          val nodeOutputSet = edges.filter { it.fromNodeId == node.id && !it.isDisabled }
                                   .map { it.toNodeId }
                                   .sorted()
                                   
          if (nodeOutputSet.isEmpty()) continue
          
          // Find another node to merge with
          val otherNode = nodes.find { other ->
              if (other.id == node.id || removedNodeIds.contains(other.id)) return@find false
              val otherData = other.data as? NodeData.Behavior ?: return@find false
              if (otherData.node != nodeData.node) return@find false
              
              val otherOutputSet = edges.filter { it.fromNodeId == other.id && !it.isDisabled }
                                       .map { it.toNodeId }
                                       .sorted()
              otherOutputSet == nodeOutputSet
          }
          
          if (otherNode != null) {
              val keptNode = otherNode
              val nodeToRemove = node
              
              val keptData = keptNode.data as NodeData.Behavior
              val dataToRemove = nodeToRemove.data as NodeData.Behavior
              val newData = keptData.copy(inputPortIds = keptData.inputPortIds + dataToRemove.inputPortIds)
              
              nodes.remove(keptNode)
              val updatedKeptNode = keptNode.copy(data = newData)
              nodes.add(updatedKeptNode)
              
              // Redirect incoming edges
              val incomingEdges = edges.filter { it.toNodeId == nodeToRemove.id }
              for (edge in incomingEdges) {
                  edges.remove(edge)
                  edges.add(edge.copy(toNodeId = keptNode.id))
              }
              
              // Remove outgoing edges of removed node and cleanup ports!
              val outgoingEdges = edges.filter { it.fromNodeId == nodeToRemove.id }
              for (edge in outgoingEdges) {
                  val parentNode = nodes.find { it.id == edge.toNodeId }
                  if (parentNode != null) {
                      val updatedParent = removePortFromNode(parentNode, edge.toPortId)
                      nodes.remove(parentNode)
                      nodes.add(updatedParent)
                  }
              }
              edges.removeAll(outgoingEdges)
              
              // Remove node from list
              nodes.remove(nodeToRemove)
              removedNodeIds.add(nodeToRemove.id)
          }
      }
      

      
      return BrushGraph(nodes = nodes, edges = edges)
  }

  private fun removePortFromNode(node: GraphNode, portId: String): GraphNode {
      val data = node.data
      val newData = when (data) {
          is NodeData.Behavior -> {
              data.copy(inputPortIds = data.inputPortIds.filter { it != portId })
          }
          is NodeData.Tip -> {
              data.copy(behaviorPortIds = data.behaviorPortIds.filter { it != portId })
          }
          is NodeData.Paint -> {
              data.copy(
                  texturePortIds = data.texturePortIds.filter { it != portId },
                  colorPortIds = data.colorPortIds.filter { it != portId }
              )
          }
          else -> data
      }
      return node.copy(data = newData)
  }

  private data class InternalNodeInfo(
    val id: String, 
    val data: NodeData.Behavior, 
    val children: List<InternalNodeInfo>
  )
}
