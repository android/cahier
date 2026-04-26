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

    val familyNodeId = UUID.randomUUID().toString()
    val coatPortIds = (0 until family.coatsCount).map { UUID.randomUUID().toString() }
    val familyData =
      NodeData.Family(
        clientBrushFamilyId = family.clientBrushFamilyId,
        developerComment = family.developerComment,
        inputModel = family.inputModel,
        coatPortIds = coatPortIds,
      )
    nodes.add(GraphNode(id = familyNodeId, data = familyData))

    val behaviorDeduplicationMap = mutableMapOf<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>()
    val assignedNodeIds = mutableSetOf<String>()
    val textureDeduplicationMap = mutableMapOf<ProtoBrushPaint.TextureLayer, String>()
    val colorDeduplicationMap = mutableMapOf<ProtoColorFunction, String>()

    for (index in 0 until family.coatsCount) {
      val coat = family.getCoats(index)
      val coatId = UUID.randomUUID().toString()
      val paintPortIds = (0 until coat.paintPreferencesCount).map { UUID.randomUUID().toString() }
      val coatData = NodeData.Coat(paintPortIds = paintPortIds)
      val coatNode = GraphNode(id = coatId, data = coatData)
      nodes.add(coatNode)
      edges.add(
        GraphEdge(
          fromNodeId = coatId,
          toNodeId = familyNodeId,
          toPortId = coatPortIds[index]
        )
      )

      val (tipId, tipOutputPortId) = convertTip(coat.tip, nodes, edges, behaviorDeduplicationMap, assignedNodeIds)
      edges.add(
        GraphEdge(
          fromNodeId = tipId,
          toNodeId = coatId,
          toPortId = coatData.tipPortId
        )
      )

      var paintIndex = 0
      for (paint in coat.paintPreferencesList) {
        val paintData = NodeData.Paint(paint)
        val (paintId, paintOutputPortId) = convertPaint(paint, nodes, edges, textureDeduplicationMap, colorDeduplicationMap)
        edges.add(
          GraphEdge(
            fromNodeId = paintId,
            toNodeId = coatId,
            toPortId = paintPortIds[paintIndex++]
          )
        )
      }
    }

    val initialGraph = BrushGraph(nodes = nodes, edges = edges)
    return deduplicateDownstream(initialGraph)
  }

  private fun convertTip(
    tip: ProtoBrushTip,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    deduplicationMap: MutableMap<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>,
    assignedNodeIds: MutableSet<String>,
  ): Pair<String, String> {
    val tipId = UUID.randomUUID().toString()
    val tempBehaviorPortIds = (0 until tip.behaviorsCount).map { UUID.randomUUID().toString() }
    val usedPortIds = mutableListOf<String>()

    var behaviorIndex = 0
    for (behavior in tip.behaviorsList) {
      val terminalNodes = convertBehaviorGraph(behavior, nodes, edges, deduplicationMap, assignedNodeIds)
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
    }

    val tipData = NodeData.Tip(tip, behaviorPortIds = usedPortIds)
    nodes.add(GraphNode(id = tipId, data = tipData))

    return Pair(tipId, "output")
  }

  private fun convertPaint(
    paint: ProtoBrushPaint,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    textureDeduplicationMap: MutableMap<ProtoBrushPaint.TextureLayer, String>,
    colorDeduplicationMap: MutableMap<ProtoColorFunction, String>,
  ): Pair<String, String> {
    val paintId = UUID.randomUUID().toString()
    val texturePortIds = (0 until paint.textureLayersCount).map { UUID.randomUUID().toString() }
    val colorPortIds = (0 until paint.colorFunctionsCount).map { UUID.randomUUID().toString() }
    val paintData = NodeData.Paint(paint, texturePortIds = texturePortIds, colorPortIds = colorPortIds)
    nodes.add(GraphNode(id = paintId, data = paintData))

    val tempTexturePortIds = texturePortIds
    val tempColorPortIds = colorPortIds
    val usedTexturePortIds = mutableListOf<String>()
    val usedColorPortIds = mutableListOf<String>()

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
              data = layerData
            )
          )
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
              data = cfData
            )
          )
      }
      colorIndex++
    }

    val finalPaintData = NodeData.Paint(paint, texturePortIds = usedTexturePortIds, colorPortIds = usedColorPortIds)
    nodes.removeIf { it.id == paintId }
    nodes.add(GraphNode(id = paintId, data = finalPaintData))

    return Pair(paintId, "output")
  }

  private fun convertBehaviorGraph(
    behavior: ProtoBrushBehavior,
    nodes: MutableList<GraphNode>,
    edges: MutableList<GraphEdge>,
    deduplicationMap: MutableMap<Pair<ProtoBrushBehavior.Node, List<String>>, InternalNodeInfo>,
    assignedNodeIds: MutableSet<String>,
  ): List<Pair<String, String>> {
    val behaviorId = UUID.randomUUID().toString()
    val nodeStack = mutableListOf<InternalNodeInfo>()
    val behaviorNodes = mutableListOf<InternalNodeInfo>()

    for (protoNode in behavior.nodesList) {
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
    
    fun buildGraphNode(info: InternalNodeInfo, depth: Int) {
        if (assignedNodeIds.contains(info.id)) {
            return
        }
        
        nodes.add(GraphNode(id = info.id, data = info.data))
        
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
        
        info.children.forEach { buildGraphNode(it, depth + 1) }
    }

    for (root in terminalNodeInfos) {
        buildGraphNode(root, 0)
    }
    
    return terminalNodeInfos.map { it.id to "output" }
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
