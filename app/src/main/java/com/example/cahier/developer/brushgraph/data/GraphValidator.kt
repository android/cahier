/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
package com.example.cahier.developer.brushgraph.data

import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushPaint as ProtoBrushPaint

/** The severity of a validation issue. Errors and Warnings are generally associated
 *  with specific issues with nodes, and link to them, while Debug messages are general
 *  information. Errors represent blocking issues which cause the [BrushGraph] to fail
 *  validation, while Warnings are non-blocking, but should be fixed, and can help diagnose
 *  issues with the [BrushGraph]. Errors are downgraded to Warnings when the affected node
 *  is orphaned from the graph, so a node not included in the graph doesn't block validation.
 */
enum class ValidationSeverity {
  ERROR,
  WARNING,
  DEBUG,
}

/** Exception thrown when the brush graph fails validation. */
data class GraphValidationException(
  val displayMessage: DisplayText,
  val nodeId: String? = null,
  val severity: ValidationSeverity = ValidationSeverity.ERROR,
) : IllegalStateException(
    when (displayMessage) {
        is DisplayText.Literal -> displayMessage.text
        is DisplayText.Resource -> "Resource ${displayMessage.resId}"
    }
)

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
                  ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> listOf("input_0", "input_1")
                  ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> listOf("angle_0", "mag_0")
                  ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf("value", "start", "end")
                  else -> if (labels.size == 1) listOf("Input") else emptyList()
              }
          } else data.inputPortIds

          if (nodeCase == ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE) {
            val labels = listOf(R.string.bg_port_value, R.string.bg_port_start, R.string.bg_port_end)
            for (i in 0 until minOf(ids.size, labels.size)) {
              if (!connectedPortIds.contains(ids[i])) {
                issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_interp_missing_input, listOf(DisplayText.Resource(labels[i]))), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
              }
            }
          } else if (nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val chunkedIds = ids.chunked(2)
            val hasValidSet = chunkedIds.any { set -> set.size == 2 && set.all { connectedPortIds.contains(it) } }
            if (!hasValidSet) {
              issues.add(GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_polar_missing_inputs), nodeId = node.id, severity = if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else if (nodeCase == ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
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
              isValidConnection(actualSourceNode, node, edge.toPortId, graph)?.let { displayText ->
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
                displayMessage = DisplayText.Resource(R.string.bg_err_source_range_equal, node.data.subtitles()),
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

    return issues.distinct()
  }

  /** Returns a failure message when a connection from [from] to [to] at [toPortId] is invalid. */
  fun isValidConnection(from: GraphNode, to: GraphNode, toPortId: String, graph: BrushGraph = BrushGraph()): DisplayText? {
    val fromData = from.data
    val toData = to.data
    val fromIsStructural =
      fromData is NodeData.Tip ||
        fromData is NodeData.Coat ||
        fromData is NodeData.Paint ||
        fromData is NodeData.TextureLayer ||
        fromData is NodeData.ColorFunction ||
        fromData is NodeData.Family
    val toIsStructural =
      toData is NodeData.Tip ||
        toData is NodeData.Coat ||
        toData is NodeData.Paint ||
        toData is NodeData.TextureLayer ||
        toData is NodeData.ColorFunction ||
        toData is NodeData.Family

    val toPort = to.getVisiblePorts(graph).find { it.id == toPortId }

    return when (toData) {
      is NodeData.Coat -> {
        val coatData = toData
        if (toPortId == coatData.tipPortId) {
          if (fromData is NodeData.Tip) {
            null
          } else {
            DisplayText.Resource(R.string.bg_err_coat_only_accepts_tip)
          }
        } else if (coatData.paintPortIds.contains(toPortId) || toPort is Port.AddPaint) {
          if (fromData is NodeData.Paint) {
            null
          } else {
            DisplayText.Resource(R.string.bg_err_coat_only_accepts_paint)
          }
        } else {
          DisplayText.Resource(R.string.bg_err_invalid_port_coat)
        }
      }
      is NodeData.Family -> {
        val familyData = toData
        if (familyData.coatPortIds.contains(toPortId) || toPort is Port.AddCoat) {
          if (fromData is NodeData.Coat) {
            null
          } else {
            DisplayText.Resource(R.string.bg_err_family_only_accepts_coat)
          }
        } else {
          DisplayText.Resource(R.string.bg_err_invalid_port_family)
        }
      }
      is NodeData.Tip -> {
        if (
          !(fromData is NodeData.Behavior) ||
            (fromData.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
              fromData.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
        ) {
          DisplayText.Resource(R.string.bg_err_tip_only_accepts_target)
        } else {
          null
        }
      }
      is NodeData.Paint -> {
          if (toData.texturePortIds.contains(toPortId) || toPort is Port.AddTexture) {
              if (fromData is NodeData.TextureLayer) {
                null
              } else {
                DisplayText.Resource(R.string.bg_err_paint_only_accepts_texture)
              }
          } else if (toData.colorPortIds.contains(toPortId) || toPort is Port.AddColor) {
              if (fromData is NodeData.ColorFunction) {
                null
              } else {
                DisplayText.Resource(R.string.bg_err_paint_only_accepts_color)
              }
          } else {
              DisplayText.Resource(R.string.bg_err_invalid_port_paint)
          }
      }
      is NodeData.TextureLayer -> DisplayText.Resource(R.string.bg_err_texture_cannot_accept_inputs)
      is NodeData.ColorFunction -> DisplayText.Resource(R.string.bg_err_color_cannot_accept_inputs)
      else -> {
        // 'to' is a behavior node.
        if (
          fromData is NodeData.Behavior &&
            (fromData.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ||
              fromData.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
        ) {
          // Targets can only connect to Tip.
          DisplayText.Resource(
            R.string.bg_err_behavior_cannot_accept,
            listOf(DisplayText.Resource(toData.title()), DisplayText.Resource(fromData.title()))
          )
        } else if (!fromIsStructural && !toIsStructural) {
          null
        } else {
          DisplayText.Resource(
            R.string.bg_err_behavior_cannot_accept_structural,
            listOf(DisplayText.Resource(toData.title()), DisplayText.Resource(fromData.title()))
          )
        }
      }
    }
  }

  /** Returns the set of node IDs for active (not disabled) nodes in the [BrushGraph] */
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

  /** Returns input nodes to disabled node, or returns the node itself. This logic enables data
   *  incoming to disabled nodes to "pass through" to where the disabled node is going.
   */
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
