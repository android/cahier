package com.example.cahier.ui.brushgraph.converters

import com.example.cahier.R
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.DisplayText
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint

/** Utility to validate a [BrushGraph] for correctness. */
object GraphValidator {

  /** Validates the entire graph and returns all found errors and warnings. */
  fun validateAll(graph: BrushGraph): List<GraphValidationException> {
    val issues = mutableListOf<GraphValidationException>()
    val activeNodeIds = findActiveNodes(graph)

    val nodesById = graph.nodes.associateBy { it.id }

    // Check for dangling edges.
    for (edge in graph.edges) {
      if (edge.isDisabled) continue
      if (!nodesById.containsKey(edge.fromNodeId)) {
        issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_edge_missing_source), nodeId = edge.toNodeId))
      }
      if (!nodesById.containsKey(edge.toNodeId)) {
        issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_edge_missing_target), nodeId = edge.fromNodeId))
      }
    }

    // Input labels and required connections.
    val familyNodes = graph.nodes.filter { it.data is NodeData.Family }
    if (familyNodes.size != 1) {
      for (node in familyNodes) {
        issues.add(
          GraphValidationException(
            displayMessage = DisplayText.Resource(R.string.bg_err_family_count, listOf(familyNodes.size)),
            nodeId = node.id,
            severity = ValidationSeverity.ERROR,
          )
        )
      }
      if (familyNodes.isEmpty()) {
        issues.add(
          GraphValidationException(
            displayMessage = DisplayText.Resource(R.string.bg_err_family_count, listOf(0)),
            severity = ValidationSeverity.ERROR,
          )
        )
      }
    }

    for (node in graph.nodes) {
      if (node.isDisabled) continue
      val isActive = activeNodeIds.contains(node.id)
      val ports = node.getVisiblePorts(graph)
      val isOptionalInput = node.data is NodeData.Tip || node.data is NodeData.Paint
      val incomingEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == node.id && activeNodeIds.contains(it.fromNodeId) }

      val connectedPortIds = incomingEdges.map { it.toPortId }.toSet()
      val active = isActive

      when (val data = node.data) {
        is NodeData.Coat -> {
          val hasTip = connectedPortIds.contains(data.tipPortId)
          val hasPaint = data.paintPortIds.any { connectedPortIds.contains(it) }
          if (!hasTip) {
            issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_coat_missing_tip), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
          if (!hasPaint) {
            issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_coat_missing_paint), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
        is NodeData.Behavior -> {
          val nodeCase = data.node.nodeCase
          val labels = data.inputLabels()
          val ids = if (data.inputPortIds.isEmpty()) {
              when (nodeCase) {
                  ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE -> listOf("input_0", "input_1")
                  ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> listOf("angle_0", "mag_0")
                  ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf("Value", "Start", "End")
                  else -> if (labels.size == 1) listOf("Input") else emptyList()
              }
          } else data.inputPortIds

          if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE) {
            val labels = listOf("Value", "Start", "End")
            for (label in labels) {
              if (!connectedPortIds.contains(label)) {
                issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_interp_missing_input, listOf(label)), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
              }
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val chunkedIds = ids.chunked(2)
            val hasValidSet = chunkedIds.any { set -> set.size == 2 && set.all { connectedPortIds.contains(it) } }
            if (!hasValidSet) {
              issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_polar_missing_inputs), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
            val numInputs = ids.count { connectedPortIds.contains(it) }
            if (numInputs < 2) {
              issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_binary_min_inputs), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            } else if (numInputs > 26) {
              issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_binary_max_inputs), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else {
            if (connectedPortIds.isEmpty() && data.inputLabels().isNotEmpty()) {
              issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_missing_input, listOf(DisplayText.Resource(data.title()))), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          }
        }

        is NodeData.Family -> {
          if (connectedPortIds.isEmpty()) {
            issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_family_missing_coat), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
        else -> {
          if (!isOptionalInput && data.inputLabels().isNotEmpty() && connectedPortIds.isEmpty()) {
             issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_missing_input, listOf(DisplayText.Resource(data.title()))), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
      }

      for (edge in incomingEdges) {
        val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
        if (fromNode == null) {
          issues.add(
            GraphValidationException(
              displayMessage = DisplayText.Resource(R.string.bg_err_invalid_conn_no_source),
              nodeId = node.id,
              severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
            )
          )
        } else {
          val actualSources = findActualSourceNode(graph, edge.fromNodeId)
          if (actualSources.isEmpty()) {
            issues.add(
              GraphValidationException(
                displayMessage = DisplayText.Resource(R.string.bg_err_missing_source_passthrough),
                nodeId = node.id,
                severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
              )
            )
          } else {
            for (actualSourceNode in actualSources) {
              BrushGraph.isValidConnection(actualSourceNode, node, edge.toPortId, graph)?.let { displayText ->
                issues.add(
                  GraphValidationException(
                    displayMessage = DisplayText.Resource(R.string.bg_err_invalid_connection_detail, listOf(DisplayText.Resource(actualSourceNode.data.title()), DisplayText.Resource(node.data.title()), edge.toPortId, displayText)),
                    nodeId = node.id,
                    severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
                  )
                )
              }
            }
          }
        }
      }

      if (node.data is NodeData.Family) {
        if (graph.edges.none { !it.isDisabled && it.toNodeId == node.id && activeNodeIds.contains(it.fromNodeId) }) {
          issues.add(
            GraphValidationException(
              displayMessage = DisplayText.Resource(R.string.bg_err_family_no_coat),
              nodeId = node.id,
              ValidationSeverity.ERROR,
            )
          )
        }
      }

      if (node.data !is NodeData.Family && node.data.hasOutput()) {
        if (graph.edges.none { !it.isDisabled && it.fromNodeId == node.id && activeNodeIds.contains(it.toNodeId) }) {
          issues.add(
            GraphValidationException(
              displayMessage = DisplayText.Resource(R.string.bg_err_unused_output, listOf(DisplayText.Resource(node.data.title()))),
              nodeId = node.id,
              severity = ValidationSeverity.WARNING,
            )
          )
        }
      }

      if (node.data is NodeData.Coat) {
        val incomingEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == node.id && activeNodeIds.contains(it.fromNodeId) }
        val tipEdge = incomingEdges.find { it.toPortId == node.data.tipPortId }
        
        val connectedPaints = node.data.paintPortIds.mapNotNull { portId ->
            incomingEdges.find { it.toPortId == portId }
        }.mapNotNull { edge ->
            graph.nodes.find { it.id == edge.fromNodeId }
        }

        if (tipEdge != null && connectedPaints.isNotEmpty()) {
          val discardPaints = connectedPaints.filter { 
              it.data is NodeData.Paint && 
              it.data.paint.selfOverlap == ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD 
          }
          
          if (discardPaints.isNotEmpty()) {
            val opacityTargetNodes = mutableListOf<GraphNode>()
            findOpacityTargetNodes(tipEdge.fromNodeId, graph, mutableSetOf(), opacityTargetNodes)

            if (opacityTargetNodes.isNotEmpty()) {
              for (paintNode in discardPaints) {
                  issues.add(
                    GraphValidationException(
                      displayMessage = DisplayText.Resource(R.string.bg_err_self_overlap_incompatible_op),
                      nodeId = paintNode.id,
                      severity = ValidationSeverity.WARNING,
                    )
                  )
              }
              opacityTargetNodes.forEach { targetNode ->
                issues.add(
                  GraphValidationException(
                    displayMessage = DisplayText.Resource(R.string.bg_err_op_incompatible_self_overlap),
                    nodeId = targetNode.id,
                    severity = ValidationSeverity.WARNING,
                  )
                )
              }
            }
          }
        }
      }

      if (node.data is NodeData.Behavior) {
        val behaviorNode = node.data.node
        if (behaviorNode.nodeCase == ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE) {
          val sourceNode = behaviorNode.sourceNode
          if (sourceNode.sourceValueRangeStart == sourceNode.sourceValueRangeEnd) {
            issues.add(
              GraphValidationException(
                displayMessage = DisplayText.Resource(R.string.bg_err_source_range_equal, listOf(node.data.subtitles().joinToString())),
                nodeId = node.id,
                severity = if (isActive) ValidationSeverity.ERROR else ValidationSeverity.WARNING,
              )
            )
          }
        }
      }
    }

    val visited = mutableSetOf<String>()
    for (node in graph.nodes) {
      if (!visited.contains(node.id)) {
        try {
          checkCycle(node.id, graph, visited, mutableSetOf())
        } catch (e: GraphValidationException) {
          issues.add(
            if (activeNodeIds.contains(e.nodeId)) e else e.copy(severity = ValidationSeverity.WARNING)
          )
        }
      }
    }

    return issues.distinctBy { (it.message ?: "") + (it.nodeId ?: "") + it.severity.name }
  }

  private fun findActiveNodes(graph: BrushGraph): Set<String> {
    val familyNode = graph.nodes.find { it.data is NodeData.Family } ?: return emptySet()
    if (familyNode.isDisabled) return emptySet()
    val active = mutableSetOf<String>(familyNode.id)
    val queue = mutableListOf(familyNode.id)
    while (queue.isNotEmpty()) {
      val currentId = queue.removeAt(0)
      val currentNode = graph.nodes.find { it.id == currentId }
      val isPassThrough = currentNode != null && currentNode.isDisabled && 
                          currentNode.data is NodeData.Behavior && currentNode.data.isOperator
      
      val currentNodeData = currentNode?.data as? NodeData.Behavior
      val firstPortId = currentNodeData?.inputPortIds?.firstOrNull()

      for (edge in graph.edges.filter { !it.isDisabled && it.toNodeId == currentId }) {
        if (isPassThrough && edge.toPortId != firstPortId) continue
        
        val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
        if (fromNode != null) {
          val isFromPassThrough = fromNode.isDisabled && 
                                  fromNode.data is NodeData.Behavior && fromNode.data.isOperator
          
          if (!fromNode.isDisabled || isFromPassThrough) {
            if (active.add(edge.fromNodeId)) queue.add(edge.fromNodeId)
          }
        }
      }
    }
    return active
  }

  fun findActualSourceNode(graph: BrushGraph, nodeId: String): List<GraphNode> {
    val node = graph.nodes.find { it.id == nodeId } ?: return emptyList()
    if (!node.isDisabled) return listOf(node)
    if (node.data is NodeData.Behavior && node.data.isOperator) {
      val incomingEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == nodeId }
      val sources = mutableListOf<GraphNode>()
      for (edge in incomingEdges) {
        sources.addAll(findActualSourceNode(graph, edge.fromNodeId))
      }
      return sources
    }
    return emptyList()
  }

  private fun checkCycle(
    nodeId: String,
    graph: BrushGraph,
    visited: MutableSet<String>,
    path: MutableSet<String>,
  ) {
    if (!path.add(nodeId)) {
      throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_cycle_detected, listOf(nodeId)), nodeId = nodeId)
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
    if (node.isDisabled) return

    if (node.data is NodeData.Behavior) {
      val brushNode = node.data.node
      if (
        brushNode.hasTargetNode() &&
          brushNode.targetNode.target == ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER
      ) {
        results.add(node)
      }
    }

    val incomingEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == nodeId }
    for (edge in incomingEdges) {
      findOpacityTargetNodes(edge.fromNodeId, graph, visited, results)
    }
  }
}
