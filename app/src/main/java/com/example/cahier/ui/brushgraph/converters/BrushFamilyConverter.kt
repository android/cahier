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
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
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
    return convertIntoProto(graph).toBrushFamily()
  }

  /** Converts a [BrushGraph] into a [ProtoBrushFamily]. */
  fun convertIntoProto(graph: BrushGraph): ProtoBrushFamily {
    val issues = validateAll(graph)
    val criticalErrors = issues.filter { it.severity == ValidationSeverity.ERROR }
    if (criticalErrors.isNotEmpty()) {
      throw criticalErrors.first()
    }

    val familyNode = graph.nodes.first { it.data is NodeData.Family }
    val familyData = familyNode.data as NodeData.Family

    val coatEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == familyNode.id }.sortedBy { it.toPort.index }
    if (coatEdges.isEmpty()) {
      throw GraphValidationException(
        "Brush Family must be connected to at least one coat.",
        familyNode.id,
      )
    }

    val behaviorCache = mutableMapOf<String, List<List<ink.proto.BrushBehavior.Node>>>()
    val coats = coatEdges.map { edge ->
      val coatNode =
        graph.nodes.find { it.id == edge.fromPort.nodeId }
          ?: throw GraphValidationException("Coat node ${edge.fromPort.nodeId} not found")
      createCoat(coatNode, graph, behaviorCache)
    }

    return ProtoBrushFamily.newBuilder()
      .addAllCoats(coats)
      .setInputModel(familyData.inputModel)
      .setClientBrushFamilyId(familyData.clientBrushFamilyId)
      .setDeveloperComment(familyData.developerComment)
      .build()
  }

  private fun createCoat(
    coatNode: GraphNode,
    graph: BrushGraph,
    behaviorCache: MutableMap<String, List<List<ink.proto.BrushBehavior.Node>>>,
  ): ProtoBrushCoat {
    val inputs = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == coatNode.id }
    val tipEdge =
      inputs.find { it.toPort.index == 0 }
        ?: throw GraphValidationException(
          "Coat node ${coatNode.id} missing Tip input.",
          coatNode.id,
        )
        
    val paintEdges = inputs.filter { it.toPort.index >= 1 }.sortedBy { it.toPort.index }
    if (paintEdges.isEmpty()) {
        throw GraphValidationException(
          "Coat node ${coatNode.id} missing Paint input.",
          coatNode.id,
        )
    }

    val tip = createTip(tipEdge.fromPort.nodeId, graph, behaviorCache, mutableSetOf())
    
    val builder = ProtoBrushCoat.newBuilder()
      .setTip(tip)
      
    for (edge in paintEdges) {
        val paint = createPaint(edge.fromPort.nodeId, graph)
        builder.addPaintPreferences(paint)
    }

    return builder.build()
  }

  private fun createTip(
    nodeId: String,
    graph: BrushGraph,
    behaviorCache: MutableMap<String, List<List<ProtoBrushBehavior.Node>>>,
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

    val behaviorEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == nodeId }.sortedBy { it.toPort.index }
    for (edge in behaviorEdges) {
      val actualSources = findActualSourceNode(graph, edge.fromPort.nodeId)
      for (actualSourceNode in actualSources) {
        val behaviorLists = collectBehaviorNodes(actualSourceNode.id, graph, behaviorCache, path)
        for (nodeList in behaviorLists) {
            val comment = (actualSourceNode.data as? NodeData.Behavior)?.developerComment ?: ""
            builder.addBehaviors(
              ProtoBrushBehavior.newBuilder()
                .addAllNodes(nodeList)
                .setDeveloperComment(comment)
                .build()
            )
        }
      }
    }

    return builder.build()
  }

  private fun collectBehaviorNodes(
    nodeId: String,
    graph: BrushGraph,
    cache: MutableMap<String, List<List<ProtoBrushBehavior.Node>>>,
    path: MutableSet<String>,
  ): List<List<ProtoBrushBehavior.Node>> {
    if (path.contains(nodeId)) {
      throw GraphValidationException("Cycle detected involving node $nodeId", nodeId)
    }
    cache[nodeId]?.let { return it }

    val graphNode = graph.nodes.find { it.id == nodeId } ?: return emptyList()
    val data = graphNode.data as? NodeData.Behavior ?: return emptyList()
    val inputEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == nodeId }

    path.add(nodeId)
    val resultLists = mutableListOf<List<ProtoBrushBehavior.Node>>()

    fun createDefaultNode(): ProtoBrushBehavior.Node {
        return ProtoBrushBehavior.Node.newBuilder()
            .setSourceNode(ProtoBrushBehavior.SourceNode.newBuilder().setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE))
            .build()
    }

    val labels = data.inputLabels()
    val nodeCase = data.node.nodeCase
    if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
        val sortedEdges = inputEdges.sortedBy { it.toPort.index }
        val setLists = mutableListOf<List<List<ProtoBrushBehavior.Node>>>()
        
        for (edge in sortedEdges) {
            val sources = findActualSourceNode(graph, edge.fromPort.nodeId)
            val lists = mutableListOf<List<ProtoBrushBehavior.Node>>()
            if (sources.isEmpty()) {
                lists.add(listOf(createDefaultNode()))
            } else {
                for (source in sources) {
                    lists.addAll(collectBehaviorNodes(source.id, graph, cache, path))
                }
            }
            setLists.add(lists)
        }
        
        if (setLists.size >= 2) {
            var currentCombinedLists = setLists[0]
            
            for (i in 1 until setLists.size) {
                val nextLists = setLists[i]
                val numInstances = maxOf(currentCombinedLists.size, nextLists.size)
                val newCombinedLists = mutableListOf<List<ProtoBrushBehavior.Node>>()
                
                for (j in 0 until numInstances) {
                    val list1 = currentCombinedLists.getOrNull(j) ?: currentCombinedLists.last()
                    val list2 = nextLists.getOrNull(j) ?: nextLists.last()
                    
                    val combinedList = mutableListOf<ProtoBrushBehavior.Node>()
                    combinedList.addAll(list1)
                    combinedList.addAll(list2)
                    combinedList.add(data.node)
                    
                    newCombinedLists.add(combinedList)
                }
                currentCombinedLists = newCombinedLists
            }
            resultLists.addAll(currentCombinedLists)
        } else {
            // Fallback if less than 2 inputs
            resultLists.add(listOf(data.node))
        }
    } else if (labels.size > 1) {
        // Multi-input behavior node
        val maxIndex = inputEdges.map { it.toPort.index }.maxOrNull() ?: -1
        val numSets = maxOf(1, (maxIndex + labels.size) / labels.size)

        for (i in 0 until numSets) {
            val setLists = mutableListOf<List<List<ProtoBrushBehavior.Node>>>()
            for (k in labels.indices) {
                val edge = inputEdges.find { it.toPort.index == i * labels.size + k }
                val sources = edge?.let { findActualSourceNode(graph, it.fromPort.nodeId) } ?: emptyList()
                val lists = mutableListOf<List<ProtoBrushBehavior.Node>>()
                if (sources.isEmpty()) {
                    lists.add(listOf(createDefaultNode()))
                } else {
                    for (src in sources) {
                        lists.addAll(collectBehaviorNodes(src.id, graph, cache, path))
                    }
                }
                setLists.add(lists)
            }

            // Parallel mapping (zip) across all inputs in the set
            val numInstances = setLists.map { it.size }.maxOrNull() ?: 0
            for (j in 0 until numInstances) {
                val combinedList = mutableListOf<ProtoBrushBehavior.Node>()
                for (lists in setLists) {
                    val list = lists.getOrNull(j) ?: lists.last()
                    combinedList.addAll(list)
                }
                combinedList.add(data.node) // Add Op node at the end (post-order)
                resultLists.add(combinedList)
            }
        }
    } else {
        // Single input node or Source node
        val connectedEdges = inputEdges.sortedBy { it.toPort.index }
        
        if (connectedEdges.isEmpty()) {
            // Source node
            resultLists.add(listOf(data.node))
        } else {
            for (edge in connectedEdges) {
                val sources = findActualSourceNode(graph, edge.fromPort.nodeId)
                if (sources.isNotEmpty()) {
                    for (source in sources) {
                        val childLists = collectBehaviorNodes(source.id, graph, cache, path)
                        for (childList in childLists) {
                            val newList = mutableListOf<ProtoBrushBehavior.Node>()
                            newList.addAll(childList)
                            newList.add(data.node) // Add current node at the end
                            resultLists.add(newList)
                        }
                    }
                } else {
                    // Pass-through or invalid source
                    resultLists.add(listOf(data.node))
                }
            }
        }
    }

    path.remove(nodeId)
    cache[nodeId] = resultLists
    return resultLists
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

    val textureEdges = graph.edges.filter { edge ->
      if (edge.isDisabled) return@filter false
      if (edge.toPort.nodeId != nodeId) return@filter false
      val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
      fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.TextureLayer
    }.sortedBy { it.toPort.index }

    val colorEdges = graph.edges.filter { edge ->
      if (edge.isDisabled) return@filter false
      if (edge.toPort.nodeId != nodeId) return@filter false
      val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
      fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.ColorFunc
    }.sortedBy { it.toPort.index }

    val builder = data.paint.toBuilder()
    builder.clearTextureLayers()
    builder.clearColorFunctions()

    for (edge in textureEdges) {
      builder.addTextureLayers(createTextureLayer(edge.fromPort.nodeId, graph))
    }
    for (edge in colorEdges) {
      builder.addColorFunctions(createColorFunction(edge.fromPort.nodeId, graph))
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

  /** Validates the entire graph and returns all found errors and warnings. */
  fun validateAll(graph: BrushGraph): List<GraphValidationException> {
    val issues = mutableListOf<GraphValidationException>()
    val activeNodeIds = findActiveNodes(graph)

    val nodesById = graph.nodes.associateBy { it.id }

    // Check for dangling edges. Shouldn't be possible due to checks in addEdge.
    // This is an annoying error to encounter because the edge may not render in the UI, making it
    // impossible to delete -- best work around is to delete the node.
    for (edge in graph.edges) {
      if (edge.isDisabled) continue
      if (!nodesById.containsKey(edge.fromPort.nodeId)) {
        issues.add(GraphValidationException("Edge refers to missing source node", edge.toPort.nodeId))
      }
      if (!nodesById.containsKey(edge.toPort.nodeId)) {
        issues.add(GraphValidationException("Edge refers to missing target node", edge.fromPort.nodeId))
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
      if (node.isDisabled) continue
      val isActive = activeNodeIds.contains(node.id)
      val ports = node.getVisiblePorts(graph)
      val isOptionalInput = node.data is NodeData.Tip || node.data is NodeData.Paint
      val incomingEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == node.id && activeNodeIds.contains(it.fromPort.nodeId) }

      val connectedIndices = incomingEdges.map { it.toPort.index }.toSet()
      val active = isActive

      when (val data = node.data) {
        is NodeData.Coat -> {
          val hasTip = connectedIndices.contains(0)
          val hasPaint = connectedIndices.any { it >= 1 }
          if (!hasTip) {
            issues.add(GraphValidationException("Coat missing Tip input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
          if (!hasPaint) {
            issues.add(GraphValidationException("Coat missing at least one Paint input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
        is NodeData.Behavior -> {
          val nodeCase = data.node.nodeCase
          if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE) {
            val labels = listOf("Value", "Start", "End")
            for (i in 0..2) {
              if (!connectedIndices.contains(i)) {
                issues.add(GraphValidationException("Interpolation missing input for \"${labels[i]}\".", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
              }
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val maxIndex = connectedIndices.maxOrNull() ?: -1
            val numSets = (maxIndex + 2) / 2
            var hasValidSet = false
            for (i in 0 until numSets) {
              val hasAngle = connectedIndices.contains(i * 2)
              val hasMag = connectedIndices.contains(i * 2 + 1)
              if (hasAngle && hasMag) {
                hasValidSet = true
                break
              }
            }
            if (!hasValidSet) {
              issues.add(GraphValidationException("Polar Target needs at least one complete set of inputs (Angle and Magnitude).", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
            val numInputs = connectedIndices.size
            if (numInputs < 2) {
              issues.add(GraphValidationException("Binary Op requires at least 2 inputs.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            } else if (numInputs > 26) {
              issues.add(GraphValidationException("Binary Op cannot have more than 26 inputs.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else {
            if (connectedIndices.isEmpty() && data.inputLabels().isNotEmpty()) {
              issues.add(GraphValidationException("${data.title()} missing input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          }
        }

        is NodeData.Family -> {
          if (connectedIndices.isEmpty()) {
            issues.add(GraphValidationException("Family missing coat input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
        else -> {
          if (!isOptionalInput && data.inputLabels().isNotEmpty() && connectedIndices.isEmpty()) {
             issues.add(GraphValidationException("${data.title()} missing input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
      }

      for (edge in incomingEdges) {
        val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
        if (fromNode == null) {
          issues.add(
            GraphValidationException(
              "Invalid connection: from node not found.",
              node.id,
              if (active) {
                ValidationSeverity.ERROR
              } else {
                ValidationSeverity.WARNING
              },
            )
          )
        } else {
          val actualSources = findActualSourceNode(graph, edge.fromPort.nodeId)
          if (actualSources.isEmpty()) {
            issues.add(
              GraphValidationException(
                "Missing source for pass-through connection",
                node.id,
                if (active) {
                  ValidationSeverity.ERROR
                } else {
                  ValidationSeverity.WARNING
                },
              )
            )
          } else {
            for (actualSourceNode in actualSources) {
              BrushGraph.isValidConnection(actualSourceNode.data, node.data, edge.toPort.index, graph, node.id)?.let { message ->
                issues.add(
                  GraphValidationException(
                    "Invalid connection from ${actualSourceNode.data.title()} to ${node.data.title()} at index ${edge.toPort.index}: $message",
                    node.id,
                    if (active) {
                      ValidationSeverity.ERROR
                    } else {
                      ValidationSeverity.WARNING
                    },
                  )
                )
              }
            }
          }
        }
      }


      if (node.data is NodeData.Family) {
        if (graph.edges.none { !it.isDisabled && it.toPort.nodeId == node.id && activeNodeIds.contains(it.fromPort.nodeId) }) {
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
        if (graph.edges.none { !it.isDisabled && it.fromPort.nodeId == node.id && activeNodeIds.contains(it.toPort.nodeId) }) {
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
        val incomingEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == node.id && activeNodeIds.contains(it.fromPort.nodeId) }
        val tipEdge = incomingEdges.find { it.toPort.index == 0 }
        val paintEdge = incomingEdges.find { it.toPort.index == 1 }

        if (tipEdge != null && paintEdge != null) {
          val paintNode = graph.nodes.find { it.id == paintEdge.fromPort.nodeId }
          if (
            paintNode?.data is NodeData.Paint &&
              paintNode.data.paint.selfOverlap == ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD
          ) {
            val opacityTargetNodes = mutableListOf<GraphNode>()
            findOpacityTargetNodes(tipEdge.fromPort.nodeId, graph, mutableSetOf(), opacityTargetNodes)

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

      if (node.data is NodeData.Behavior) {
        val behaviorNode = node.data.node
        if (behaviorNode.nodeCase == ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE) {
          val sourceNode = behaviorNode.sourceNode
          if (sourceNode.sourceValueRangeStart == sourceNode.sourceValueRangeEnd) {
            issues.add(
              GraphValidationException(
                "Source node \"${node.data.subtitles().joinToString()}\" cannot have equal range start and end values.",
                node.id,
                if (isActive) {
                  ValidationSeverity.ERROR
                } else {
                  ValidationSeverity.WARNING
                },
              )
            )
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
    if (familyNode.isDisabled) return emptySet()
    val active = mutableSetOf<String>(familyNode.id)
    val queue = mutableListOf(familyNode.id)
    while (queue.isNotEmpty()) {
      val currentId = queue.removeAt(0)
      val currentNode = graph.nodes.find { it.id == currentId }
      val isPassThrough = currentNode != null && currentNode.isDisabled && 
                          currentNode.data is NodeData.Behavior && currentNode.data.isOperator
      
      for (edge in graph.edges.filter { !it.isDisabled && it.toPort.nodeId == currentId }) {
        if (isPassThrough && edge.toPort.index != 0) continue
        
        val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
        if (fromNode != null) {
          val isFromPassThrough = fromNode.isDisabled && 
                                  fromNode.data is NodeData.Behavior && fromNode.data.isOperator
          
          if (!fromNode.isDisabled || isFromPassThrough) {
            if (active.add(edge.fromPort.nodeId)) queue.add(edge.fromPort.nodeId)
          }
        }
      }
    }
    return active
  }

  private fun findActualSourceNode(graph: BrushGraph, nodeId: String): List<GraphNode> {
    val node = graph.nodes.find { it.id == nodeId } ?: return emptyList()
    if (!node.isDisabled) return listOf(node)
    if (node.data is NodeData.Behavior && node.data.isOperator) {
      val incomingEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == nodeId }
      val sources = mutableListOf<GraphNode>()
      for (edge in incomingEdges) {
        sources.addAll(findActualSourceNode(graph, edge.fromPort.nodeId))
      }
      return sources
    }
    return emptyList()
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
    for (edge in graph.edges.filter { it.fromPort.nodeId == nodeId }) {
      checkCycle(edge.toPort.nodeId, graph, visited, path)
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

    val incomingEdges = graph.edges.filter { !it.isDisabled && it.toPort.nodeId == nodeId }
    for (edge in incomingEdges) {
      findOpacityTargetNodes(edge.fromPort.nodeId, graph, visited, results)
    }
  }
}
