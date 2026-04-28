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

private class ConversionContext(
    val graph: BrushGraph,
    val nodesById: Map<String, GraphNode>,
    val edgesByToNode: Map<String, List<GraphEdge>>,
    val behaviorCache: MutableMap<String, List<List<ink.proto.BrushBehavior.Node>>> = mutableMapOf()
)

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

    val nodesById = graph.nodes.associateBy { it.id }
    val edgesByToNode = graph.edges.filter { !it.isDisabled }.groupBy { it.toNodeId }
    val context = ConversionContext(graph, nodesById, edgesByToNode)

    val familyNode = graph.nodes.first { it.data is NodeData.Family }
    val familyData = familyNode.data as NodeData.Family

    val coatEdges = context.edgesByToNode[familyNode.id] ?: emptyList()
    val sortedCoatEdges = familyData.coatPortIds.mapNotNull { portId ->
        coatEdges.find { it.toPortId == portId }
    }
    if (sortedCoatEdges.isEmpty()) {
      throw GraphValidationException(
        displayMessage = DisplayText.Resource(R.string.bg_err_family_no_coat),
        nodeId = familyNode.id,
      )
    }

    val coats = sortedCoatEdges.mapNotNull { edge ->
      val coatNode = context.nodesById[edge.fromNodeId]
          ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_coat_node_not_found, listOf(edge.fromNodeId)))
      if (coatNode.isDisabled) null
      else createCoat(coatNode, context)
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
    behaviorCache: MutableMap<String, List<List<ink.proto.BrushBehavior.Node>>> = mutableMapOf()
  ): ProtoBrushCoat {
    val nodesById = graph.nodes.associateBy { it.id }
    val edgesByToNode = graph.edges.filter { !it.isDisabled }.groupBy { it.toNodeId }
    val context = ConversionContext(graph, nodesById, edgesByToNode, behaviorCache)
    return createCoat(coatNode, context)
  }

  private fun createCoat(
    coatNode: GraphNode,
    context: ConversionContext
  ): ProtoBrushCoat {
    val inputs = context.edgesByToNode[coatNode.id] ?: emptyList()
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

    val tip = createTip(tipEdge.fromNodeId, context, mutableSetOf())
    
    val builder = ProtoBrushCoat.newBuilder()
      .setTip(tip)
      
    for (edge in paintEdges) {
        val paint = createPaint(edge.fromNodeId, context)
        builder.addPaintPreferences(paint)
    }

    return builder.build()
  }

  private fun createTip(
    nodeId: String,
    context: ConversionContext,
    path: MutableSet<String>,
  ): ProtoBrushTip {
    val graphNode = context.nodesById[nodeId]
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
        context.edgesByToNode[nodeId]?.find { it.toPortId == portId }
    }
    for (edge in behaviorEdges) {
      val actualSources = GraphValidator.findActualSourceNode(context.graph, edge.fromNodeId)
      for (actualSourceNode in actualSources) {
        val behaviorLists = collectBehaviorNodes(actualSourceNode.id, context, path)
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
    context: ConversionContext,
    path: MutableSet<String>,
  ): List<List<ProtoBrushBehavior.Node>> {
    if (path.contains(nodeId)) {
      throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_cycle_detected, listOf(nodeId)), nodeId = nodeId)
    }
    context.behaviorCache[nodeId]?.let { return it }

    val graphNode = context.nodesById[nodeId] ?: return emptyList()
    val data = graphNode.data as? NodeData.Behavior ?: return emptyList()
    val inputEdges = context.edgesByToNode[nodeId] ?: emptyList()

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
            val sources = edge?.let { GraphValidator.findActualSourceNode(context.graph, it.fromNodeId) } ?: emptyList()
            val lists = mutableListOf<List<ProtoBrushBehavior.Node>>()
            if (sources.isEmpty()) {
                lists.add(listOf(createDefaultNode()))
            } else {
                for (source in sources) {
                    lists.addAll(collectBehaviorNodes(source.id, context, path))
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
                val sources = edge?.let { GraphValidator.findActualSourceNode(context.graph, it.fromNodeId) } ?: emptyList()
                val lists = mutableListOf<List<ProtoBrushBehavior.Node>>()
                if (sources.isEmpty()) {
                    lists.add(listOf(createDefaultNode()))
                } else {
                    for (src in sources) {
                        lists.addAll(collectBehaviorNodes(src.id, context, path))
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
                val sources = edge?.let { GraphValidator.findActualSourceNode(context.graph, it.fromNodeId) } ?: emptyList()
                if (sources.isNotEmpty()) {
                    for (source in sources) {
                        val childLists = collectBehaviorNodes(source.id, context, path)
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
    context.behaviorCache[nodeId] = resultLists
    return resultLists
  }

  private fun createPaint(nodeId: String, context: ConversionContext): ProtoBrushPaint {
    val graphNode = context.nodesById[nodeId]
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.Paint
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("Paint", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
        )

    val textureEdges = data.texturePortIds.mapNotNull { portId ->
        context.edgesByToNode[nodeId]?.find { edge ->
            if (edge.toPortId != portId) return@find false
            val fromNode = context.nodesById[edge.fromNodeId]
            fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.TextureLayer
        }
    }

    val colorEdges = data.colorPortIds.mapNotNull { portId ->
        context.edgesByToNode[nodeId]?.find { edge ->
            if (edge.toPortId != portId) return@find false
            val fromNode = context.nodesById[edge.fromNodeId]
            fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.ColorFunction
        }
    }

    val builder = data.paint.toBuilder()
    builder.clearTextureLayers()
    builder.clearColorFunctions()

    for (edge in textureEdges) {
      builder.addTextureLayers(createTextureLayer(edge.fromNodeId, context))
    }
    for (edge in colorEdges) {
      builder.addColorFunctions(createColorFunction(edge.fromNodeId, context))
    }

    return builder.build()
  }

  private fun createTextureLayer(nodeId: String, context: ConversionContext): ProtoBrushPaint.TextureLayer {
    val graphNode = context.nodesById[nodeId]
        ?: throw GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_node_not_found, listOf(nodeId)))
    val data =
      graphNode.data as? NodeData.TextureLayer
        ?: throw GraphValidationException(
          displayMessage = DisplayText.Resource(R.string.bg_err_expected_node_type, listOf("TextureLayer", graphNode.data::class.simpleName ?: "Unknown")),
          nodeId = nodeId,
        )
    return data.layer
  }

  private fun createColorFunction(nodeId: String, context: ConversionContext): ProtoColorFunction {
    val graphNode = context.nodesById[nodeId]
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
