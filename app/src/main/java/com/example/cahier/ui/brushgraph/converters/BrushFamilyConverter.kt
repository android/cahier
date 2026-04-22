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

    val coatEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == familyNode.id }
    val sortedCoatEdges = familyData.coatPortIds.mapNotNull { portId ->
        coatEdges.find { it.toPortId == portId }
    }
    if (sortedCoatEdges.isEmpty()) {
      throw GraphValidationException(
        "Brush Family must be connected to at least one coat.",
        familyNode.id,
      )
    }

    val behaviorCache = mutableMapOf<String, List<List<ink.proto.BrushBehavior.Node>>>()
    val coats = sortedCoatEdges.map { edge ->
      val coatNode =
        graph.nodes.find { it.id == edge.fromNodeId }
          ?: throw GraphValidationException("Coat node ${edge.fromNodeId} not found")
      createCoat(coatNode, graph, behaviorCache)
    }

    return ProtoBrushFamily.newBuilder()
      .addAllCoats(coats)
      .setInputModel(familyData.inputModel)
      .setClientBrushFamilyId(familyData.clientBrushFamilyId)
      .setDeveloperComment(familyData.developerComment)
      .build()
  }

  fun createCoat(
    coatNode: GraphNode,
    graph: BrushGraph,
    behaviorCache: MutableMap<String, List<List<ink.proto.BrushBehavior.Node>>>,
  ): ProtoBrushCoat {
    val inputs = graph.edges.filter { !it.isDisabled && it.toNodeId == coatNode.id }
    val coatData = coatNode.data as NodeData.Coat
    
    val tipEdge =
      inputs.find { it.toPortId == coatData.tipPortId }
        ?: throw GraphValidationException(
          "Coat node ${coatNode.id} missing Tip input.",
          coatNode.id,
        )
        
    val paintEdges = coatData.paintPortIds.mapNotNull { portId ->
        inputs.find { it.toPortId == portId }
    }
    if (paintEdges.isEmpty()) {
        throw GraphValidationException(
          "Coat node ${coatNode.id} missing Paint input.",
          coatNode.id,
        )
    }

    val tip = createTip(tipEdge.fromNodeId, graph, behaviorCache, mutableSetOf())
    
    val builder = ProtoBrushCoat.newBuilder()
      .setTip(tip)
      
    for (edge in paintEdges) {
        val paint = createPaint(edge.fromNodeId, graph)
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

    val behaviorEdges = data.behaviorPortIds.mapNotNull { portId ->
        graph.edges.find { !it.isDisabled && it.toNodeId == nodeId && it.toPortId == portId }
    }
    for (edge in behaviorEdges) {
      val actualSources = findActualSourceNode(graph, edge.fromNodeId)
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
    val inputEdges = graph.edges.filter { !it.isDisabled && it.toNodeId == nodeId }

    path.add(nodeId)
    val resultLists = mutableListOf<List<ProtoBrushBehavior.Node>>()

    fun createDefaultNode(): ProtoBrushBehavior.Node {
        return ProtoBrushBehavior.Node.newBuilder()
            .setSourceNode(ProtoBrushBehavior.SourceNode.newBuilder().setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE))
            .build()
    }

    val labels = data.inputLabels()
    val nodeCase = data.node.nodeCase
    
    val ids = if (data.inputPortIds.isEmpty()) {
        when (nodeCase) {
            ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE -> listOf("input_0", "input_1")
            ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> listOf("angle_0", "mag_0")
            ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf("Value", "Start", "End")
            else -> if (labels.size == 1) listOf("Input") else emptyList()
        }
    } else data.inputPortIds
    
    val sortedEdges = ids.map { portId ->
        inputEdges.find { it.toPortId == portId }
    }

    if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
        val setLists = mutableListOf<List<List<ProtoBrushBehavior.Node>>>()
        
        for (edge in sortedEdges) {
            val sources = edge?.let { findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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
        // Multi-input behavior node (e.g. PolarTarget)
        val chunkedEdges = sortedEdges.chunked(labels.size)

        for (set in chunkedEdges) {
            val setLists = mutableListOf<List<List<ProtoBrushBehavior.Node>>>()
            for (edge in set) {
                val sources = edge?.let { findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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
        if (sortedEdges.isEmpty()) {
            // Source node
            resultLists.add(listOf(data.node))
        } else {
            for (edge in sortedEdges) {
                val sources = edge?.let { findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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

    val textureEdges = data.texturePortIds.mapNotNull { portId ->
        graph.edges.find { edge ->
            if (edge.isDisabled || edge.toNodeId != nodeId || edge.toPortId != portId) return@find false
            val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
            fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.TextureLayer
        }
    }

    val colorEdges = data.colorPortIds.mapNotNull { portId ->
        graph.edges.find { edge ->
            if (edge.isDisabled || edge.toNodeId != nodeId || edge.toPortId != portId) return@find false
            val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
            fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.ColorFunc
        }
    }

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
            issues.add(GraphValidationException("Coat missing Tip input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
          if (!hasPaint) {
            issues.add(GraphValidationException("Coat missing at least one Paint input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
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
                issues.add(GraphValidationException("Interpolation missing input for \"$label\".", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
              }
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val chunkedIds = ids.chunked(2)
            val hasValidSet = chunkedIds.any { set -> set.size == 2 && set.all { connectedPortIds.contains(it) } }
            if (!hasValidSet) {
              issues.add(GraphValidationException("Polar Target needs at least one complete set of inputs (Angle and Magnitude).", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) {
            val numInputs = ids.count { connectedPortIds.contains(it) }
            if (numInputs < 2) {
              issues.add(GraphValidationException("Binary Op requires at least 2 inputs.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            } else if (numInputs > 26) {
              issues.add(GraphValidationException("Binary Op cannot have more than 26 inputs.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          } else {
            if (connectedPortIds.isEmpty() && data.inputLabels().isNotEmpty()) {
              issues.add(GraphValidationException("${data.title()} missing input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
            }
          }
        }

        is NodeData.Family -> {
          if (connectedPortIds.isEmpty()) {
            issues.add(GraphValidationException("Family missing coat input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
        else -> {
          if (!isOptionalInput && data.inputLabels().isNotEmpty() && connectedPortIds.isEmpty()) {
             issues.add(GraphValidationException("${data.title()} missing input.", node.id, if (active) ValidationSeverity.ERROR else ValidationSeverity.WARNING))
          }
        }
      }

      for (edge in incomingEdges) {
        val fromNode = graph.nodes.find { it.id == edge.fromNodeId }
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
          val actualSources = findActualSourceNode(graph, edge.fromNodeId)
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
              BrushGraph.isValidConnection(actualSourceNode, node, edge.toPortId, graph)?.let { message ->
                issues.add(
                  GraphValidationException(
                    "Invalid connection from ${actualSourceNode.data.title()} to ${node.data.title()} at port ${edge.toPortId}: $message",
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
        if (graph.edges.none { !it.isDisabled && it.toNodeId == node.id && activeNodeIds.contains(it.fromNodeId) }) {
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
        if (graph.edges.none { !it.isDisabled && it.fromNodeId == node.id && activeNodeIds.contains(it.toNodeId) }) {
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
                      "Self overlap discard is incompatible with an opacity multiplier target on the coat tip.",
                      paintNode.id,
                      ValidationSeverity.WARNING,
                    )
                  )
              }
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

  private fun findActualSourceNode(graph: BrushGraph, nodeId: String): List<GraphNode> {
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
