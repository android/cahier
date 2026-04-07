@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.converters

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.toBrushFamily
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import com.example.cahier.ui.brushgraph.model.safeCopy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction

/** Utility to convert a [BrushGraph] data model into a functional [BrushFamily] object. */
object BrushFamilyConverter {

  /**
   * Converts a [BrushGraph] into a [BrushFamily].
   *
   * @throws IllegalStateException if the graph is invalid.
   */
  fun convert(graph: BrushGraph): BrushFamily {
    val issues = validateAll(graph)
    val criticalErrors = issues.filter { it.severity == ValidationSeverity.ERROR }
    if (criticalErrors.isNotEmpty()) {
      throw criticalErrors.first()
    }

    val familyNode = graph.nodes.first { it.data is NodeData.Family }
    val familyData = familyNode.data as NodeData.Family

    val coatEdges = graph.edges.filter { it.toNodeId == familyNode.id }.sortedBy { it.toInputIndex }
    if (coatEdges.isEmpty()) {
      throw GraphValidationException(
        "Brush Family must be connected to at least one coat.",
        familyNode.id,
      )
    }

    val behaviorCache = mutableMapOf<String, ProtoBrushBehavior.Node>()
    val coats = coatEdges.map { edge ->
      val coatNode =
        graph.nodes.find { it.id == edge.fromNodeId }
          ?: throw GraphValidationException("Coat node ${edge.fromNodeId} not found")
      createCoat(coatNode, graph, behaviorCache)
    }

    val proto = ProtoBrushFamily.newBuilder()
      .addAllCoats(coats)
      .setInputModel(familyData.inputModel)
      .setClientBrushFamilyId(familyData.clientBrushFamilyId)
      .setDeveloperComment(familyData.developerComment)
      .build()

    return proto.toBrushFamily()
  }

  private fun createCoat(
    coatNode: GraphNode,
    graph: BrushGraph,
    behaviorCache: MutableMap<String, ProtoBrushBehavior.Node>,
  ): ProtoBrushCoat {
    val inputs = graph.edges.filter { it.toNodeId == coatNode.id }
    val tipEdge =
      inputs.find { it.toInputIndex == 0 }
        ?: throw GraphValidationException(
          "Coat node ${coatNode.id} missing Tip input.",
          coatNode.id,
        )
    val paintEdge =
      inputs.find { it.toInputIndex == 1 }
        ?: throw GraphValidationException(
          "Coat node ${coatNode.id} missing Paint input.",
          coatNode.id,
        )

    val tip = createTip(tipEdge.fromNodeId, graph, behaviorCache, mutableSetOf())
    val paint = createPaint(paintEdge.fromNodeId, graph)

    return ProtoBrushCoat.newBuilder()
      .setTip(tip)
      .addPaintPreferences(paint)
      .build()
  }

  private fun createTip(
    nodeId: String,
    graph: BrushGraph,
    behaviorCache: MutableMap<String, ProtoBrushBehavior.Node>,
    path: MutableSet<String>,
  ): ProtoBrushTip {
    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException("Node $nodeId not found")
    val data =
      graphNode.data as? NodeData.Tip
        ?: throw GraphValidationException(
          "Expected Tip node, found ${graphNode.data::class.simpleName}",
          nodeId,
        )

    val builder = data.tip.toBuilder()
    builder.clearBehaviors()

    val behaviorEdges = graph.edges.filter { it.toNodeId == nodeId && it.toInputIndex == 0 }
    for (edge in behaviorEdges) {
      val nodes = mutableListOf<ProtoBrushBehavior.Node>()
      collectBehaviorNodes(edge.fromNodeId, graph, behaviorCache, path, nodes)
      builder.addBehaviors(ProtoBrushBehavior.newBuilder().addAllNodes(nodes).build())
    }

    return builder.build()
  }

  private fun collectBehaviorNodes(
    nodeId: String,
    graph: BrushGraph,
    cache: MutableMap<String, ProtoBrushBehavior.Node>,
    path: MutableSet<String>,
    results: MutableList<ProtoBrushBehavior.Node>,
  ) {
    val node = getOrCreateBehaviorNode(nodeId, graph, cache, path)
    // We need to add nodes in post-order. getOrCreateBehaviorNode handles children.
    // However, the proto format expects a list of nodes that form the tree.
    // Let's refine this to properly collect the tree in post-order.
    val graphNode = graph.nodes.find { it.id == nodeId } ?: return
    val inputEdges = graph.edges.filter { it.toNodeId == nodeId }.sortedBy { it.toInputIndex }
    for (edge in inputEdges) {
      collectBehaviorNodes(edge.fromNodeId, graph, cache, path, results)
    }
    results.add(node)
  }

  private fun createPaint(nodeId: String, graph: BrushGraph): ProtoBrushPaint {
    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException("Node $nodeId not found")
    val data =
      graphNode.data as? NodeData.Paint
        ?: throw GraphValidationException(
          "Expected Paint node, found ${graphNode.data::class.simpleName}",
          nodeId,
        )

    val textureEdges = graph.edges.filter { it.toNodeId == nodeId && it.toInputIndex == 0 }
    val colorEdges = graph.edges.filter { it.toNodeId == nodeId && it.toInputIndex == 1 }

    val builder = data.paint.toBuilder()
    builder.clearTextureLayers()
    builder.clearColorFunctions()

    for (edge in textureEdges) {
      builder.addTextureLayers(createTextureLayer(edge.fromNodeId, graph))
    }
    for (edge in colorEdges) {
      builder.addColorFunctions(createColorFunction(edge.fromNodeId, graph))
    }

    return builder.build()
  }

  private fun createTextureLayer(nodeId: String, graph: BrushGraph): ProtoBrushPaint.TextureLayer {
    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException("Node $nodeId not found")
    val data =
      graphNode.data as? NodeData.TextureLayer
        ?: throw GraphValidationException(
          "Expected TextureLayer node, found ${graphNode.data::class.simpleName}",
          nodeId,
        )
    return data.layer
  }

  private fun createColorFunction(nodeId: String, graph: BrushGraph): ProtoColorFunction {
    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException("Node $nodeId not found")
    val data =
      graphNode.data as? NodeData.ColorFunc
        ?: throw GraphValidationException(
          "Expected ColorFunc node, found ${graphNode.data::class.simpleName}",
          nodeId,
        )
    return data.function
  }

  private fun getOrCreateBehaviorNode(
    nodeId: String,
    graph: BrushGraph,
    cache: MutableMap<String, ProtoBrushBehavior.Node>,
    path: MutableSet<String>,
  ): ProtoBrushBehavior.Node {
    if (path.contains(nodeId)) {
      throw GraphValidationException("Cycle detected involving node $nodeId", nodeId)
    }
    cache[nodeId]?.let {
      return it
    }

    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException("Node $nodeId not found in graph.", nodeId)

    path.add(nodeId)
    val inputEdges = graph.edges.filter { it.toNodeId == nodeId }

    fun getValueInput(index: Int = 0): ProtoBrushBehavior.Node {
      val edge =
        inputEdges.find { it.toInputIndex == index }
          ?: throw GraphValidationException(
            "${graphNode.data.title()} missing input at index $index",
            graphNode.id,
          )
      return getOrCreateBehaviorNode(edge.fromNodeId, graph, cache, path)
    }

    val data =
      graphNode.data as? NodeData.Behavior
        ?: throw GraphValidationException(
          "Non-behavior node ${graphNode.data.title()} found in behavior graph path",
          nodeId,
        )

    val originalNode = data.node
    val nodeBuilder = originalNode.toBuilder()
    
    // We don't need to manually link inputs here because the Proto format 
    // uses a flat list of nodes in post-order. The children are added to the list 
    // before the parent in 'collectBehaviorNodes'.
    val node = nodeBuilder.build()

    path.remove(nodeId)
    cache[nodeId] = node
    return node
  }

  /** Validates the entire graph and returns all found errors and warnings. */
  fun validateAll(graph: BrushGraph): List<GraphValidationException> {
    val issues = mutableListOf<GraphValidationException>()
    val activeNodeIds = findActiveNodes(graph)

    val nodesById = graph.nodes.associateBy { it.id }

    // Check for dangling edges. Shouldn't be possible due to checks in addEdge.
    // This is an annoying error to encounter because the edge may not render in the UI, making it
    // impossible to delete -- best work around is to delete the node.
    for (edge in graph.edges) {
      if (!nodesById.containsKey(edge.fromNodeId)) {
        issues.add(GraphValidationException("Edge refers to missing source node", edge.toNodeId))
      }
      if (!nodesById.containsKey(edge.toNodeId)) {
        issues.add(GraphValidationException("Edge refers to missing target node", edge.fromNodeId))
      }
    }

    // Input labels and required connections.
    val familyNodes = graph.nodes.filter { it.data is NodeData.Family }
    if (familyNodes.size != 1) {
      for (node in familyNodes) {
        issues.add(
          GraphValidationException(
            "Graph must have exactly one Brush Family node. Found ${familyNodes.size}.",
            node.id,
            ValidationSeverity.ERROR,
          )
        )
      }
      if (familyNodes.isEmpty()) {
        issues.add(
          GraphValidationException(
            "Graph must have exactly one Brush Family node. Found 0.",
            severity = ValidationSeverity.ERROR,
          )
        )
      }
    }

    for (node in graph.nodes) {
      val isActive = activeNodeIds.contains(node.id)
      val labels = node.data.inputLabels()
      val incomingEdges = graph.edges.filter { it.toNodeId == node.id }

      val isOptional = node.data is NodeData.Tip || node.data is NodeData.Paint

      for (index in labels.indices) {
        val label = labels[index]
        if (label != "Add Coat...") {
          val edge = incomingEdges.find { it.toInputIndex == index }
          if (edge == null) {
            if (!isOptional) {
              issues.add(
                GraphValidationException(
                  "${node.data.title()} missing input for \"$label\".",
                  node.id,
                  if (isActive) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
                )
              )
            }
          } else {
            val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
            if (fromNode == null) {
              issues.add(
                GraphValidationException(
                  "Invalid connection: from node not found.",
                  node.id,
                  if (isActive) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
                )
              )
            } else {
              BrushGraph.isValidConnection(fromNode.data, node.data, index)?.let { message ->
                issues.add(
                  GraphValidationException(
                    "Invalid connection: $message",
                    node.id,
                    if (isActive) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
                  )
                )
              }
            }
          }
        }
      }

      if (node.data is NodeData.Family) {
        if (graph.edges.none { it.toNodeId == node.id }) {
          issues.add(
            GraphValidationException(
              "Brush Family must be connected to at least one coat.",
              node.id,
              ValidationSeverity.ERROR,
            )
          )
        }
      }

      if (node.data !is NodeData.Family && node.data.hasOutput()) {
        if (graph.edges.none { it.fromNodeId == node.id }) {
          issues.add(
            GraphValidationException(
              "${node.data.title()} output is not used.",
              node.id,
              ValidationSeverity.WARNING,
            )
          )
        }
      }
      // Check for self overlap discard vs opacity multiplier.
      if (node.data is NodeData.Coat) {
        val incomingEdges = graph.edges.filter { it.toNodeId == node.id }
        val tipEdge = incomingEdges.find { it.toInputIndex == 0 }
        val paintEdge = incomingEdges.find { it.toInputIndex == 1 }

        if (tipEdge != null && paintEdge != null) {
          val paintNode = graph.nodes.find { it.id == paintEdge.fromNodeId }
          if (
            paintNode?.data is NodeData.Paint &&
              paintNode.data.paint.selfOverlap == ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD
          ) {
            val opacityTargetNodes = mutableListOf<GraphNode>()
            findOpacityTargetNodes(tipEdge.fromNodeId, graph, mutableSetOf(), opacityTargetNodes)

            if (opacityTargetNodes.isNotEmpty()) {
              issues.add(
                GraphValidationException(
                  "Self overlap discard is incompatible with an opacity multiplier target on the coat tip.",
                  paintNode.id,
                  ValidationSeverity.WARNING,
                )
              )
              opacityTargetNodes.forEach { targetNode ->
                issues.add(
                  GraphValidationException(
                    "Targeting opacity multiplier is incompatible with self overlap discard on the coat paint.",
                    targetNode.id,
                    ValidationSeverity.WARNING,
                  )
                )
              }
            }
          }
        }
      }

      // Property validation is unnecessary since properties are edited through the UI, and we
      // control the ranges for the properties.
    }

    val visited = mutableSetOf<String>()
    for (node in graph.nodes) {
      if (!visited.contains(node.id)) {
        try {
          checkCycle(node.id, graph, visited, mutableSetOf())
        } catch (e: GraphValidationException) {
          issues.add(
            if (activeNodeIds.contains(e.nodeId)) {
              e
            } else {
              e.copy(severity = ValidationSeverity.WARNING)
            }
          )
        }
      }
    }

    return issues.distinctBy { (it.message ?: "") + (it.nodeId ?: "") + it.severity.name }
  }

  /** Returns the set of node IDs that are connected to the family node. */
  private fun findActiveNodes(graph: BrushGraph): Set<String> {
    val familyNode = graph.nodes.find { it.data is NodeData.Family } ?: return emptySet()
    val active = mutableSetOf<String>(familyNode.id)
    val queue = mutableListOf(familyNode.id)
    while (queue.isNotEmpty()) {
      val currentId = queue.removeAt(0)
      for (edge in graph.edges.filter { it.toNodeId == currentId }) {
        if (active.add(edge.fromNodeId)) queue.add(edge.fromNodeId)
      }
    }
    return active
  }

  private fun GraphValidationException.copy(
    message: String = this.message ?: "",
    nodeId: String? = this.nodeId,
    severity: ValidationSeverity = this.severity,
  ) = GraphValidationException(message, nodeId, severity)

  private fun checkCycle(
    nodeId: String,
    graph: BrushGraph,
    visited: MutableSet<String>,
    path: MutableSet<String>,
  ) {
    if (!path.add(nodeId)) {
      throw GraphValidationException("Cycle detected involving node $nodeId.", nodeId)
    }
    visited.add(nodeId)
    for (edge in graph.edges.filter { it.fromNodeId == nodeId }) {
      checkCycle(edge.toNodeId, graph, visited, path)
    }
    path.remove(nodeId)
  }

  private fun findOpacityTargetNodes(
    nodeId: String,
    graph: BrushGraph,
    visited: MutableSet<String>,
    results: MutableList<GraphNode>,
  ) {
    if (!visited.add(nodeId)) return
    val node = graph.nodes.find { it.id == nodeId } ?: return

    if (node.data is NodeData.Behavior) {
      val brushNode = node.data.node
      if (
        brushNode.hasTargetNode() &&
          brushNode.targetNode.target == ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER
      ) {
        results.add(node)
      }
    }

    val incomingEdges = graph.edges.filter { it.toNodeId == nodeId }
    for (edge in incomingEdges) {
      findOpacityTargetNodes(edge.fromNodeId, graph, visited, results)
    }
  }
}
