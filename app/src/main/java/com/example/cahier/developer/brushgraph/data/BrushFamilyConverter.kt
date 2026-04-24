@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.data

import androidx.ink.brush.BrushFamily
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.toBrushFamily
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
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
    val issues = GraphValidator.validateAll(graph)
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
        displayMessage = DisplayText.Resource(R.string.bg_err_family_no_coat),
        nodeId = familyNode.id,
      )
    }

    val behaviorCache = mutableMapOf<String, List<List<ink.proto.BrushBehavior.Node>>>()
    val coats = sortedCoatEdges.mapNotNull { edge ->
      val coatNode =
        graph.nodes.find { it.id == edge.fromNodeId }
          ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_coat_node_not_found, listOf(edge.fromNodeId)))
      if (coatNode.isDisabled) null
      else createCoat(coatNode, graph, behaviorCache)
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
          displayMessage = DisplayText.Resource(R.string.bg_err_coat_missing_tip_input, listOf(coatNode.id)),
          nodeId = coatNode.id,
        )
        
    val paintEdges = coatData.paintPortIds.mapNotNull { portId ->
        inputs.find { it.toPortId == portId }
    }
    if (paintEdges.isEmpty()) {
        throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_coat_missing_paint_input, listOf(coatNode.id)),
          nodeId = coatNode.id,
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
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.Tip
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("Tip", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
        )

    val builder = data.tip.toBuilder()
    builder.clearBehaviors()

    val behaviorEdges = data.behaviorPortIds.mapNotNull { portId ->
        graph.edges.find { !it.isDisabled && it.toNodeId == nodeId && it.toPortId == portId }
    }
    for (edge in behaviorEdges) {
      val actualSources = GraphValidator.findActualSourceNode(graph, edge.fromNodeId)
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
      throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_cycle_detected, listOf(nodeId)), nodeId = nodeId)
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
            val sources = edge?.let { GraphValidator.findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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
                val sources = edge?.let { GraphValidator.findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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
                val sources = edge?.let { GraphValidator.findActualSourceNode(graph, it.fromNodeId) } ?: emptyList()
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
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.Paint
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("Paint", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
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
            fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.ColorFunction
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
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.TextureLayer
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("TextureLayer", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
        )
    return data.layer
  }

  private fun createColorFunction(nodeId: String, graph: BrushGraph): ProtoColorFunction {
    val graphNode =
      graph.nodes.find { it.id == nodeId }
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.ColorFunction
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("ColorFunction", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
        )
    return data.function
  }

}
