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

import android.graphics.Bitmap
import android.util.Log
import com.example.cahier.R
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import com.example.cahier.developer.brushdesigner.data.AUTOSAVE_KEY
import androidx.ink.brush.BrushFamily
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import ink.proto.PredefinedEasingFunction as ProtoPredefinedEasingFunction
import javax.inject.Inject
import javax.inject.Singleton
import com.example.cahier.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlin.OptIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface BrushGraphRepository {
  val graph: StateFlow<BrushGraph>
  val graphIssues: StateFlow<List<GraphValidationException>>

  fun setGraph(newGraph: BrushGraph)
  fun clearGraph()
  fun getBrushFamily(): BrushFamily?
  fun reorganize(): BrushFamily?

  fun postDebug(displayText: DisplayText)
  fun validate(): Boolean
  fun clearIssues()
  
  suspend fun loadAutoSaveBrush(): Boolean
  fun loadBrushFamily(family: BrushFamily): Boolean

  fun createDefaultGraph(): BrushGraph
  fun addNode(data: NodeData): String
  fun updateNodeData(nodeId: String, newData: NodeData)
  fun setNodeDisabled(nodeId: String, isDisabled: Boolean)
  fun deleteNode(nodeId: String): Set<String>
  fun deleteSelectedNodes(selectedNodeIds: Set<String>): Set<String>
  fun duplicateSelectedNodes(selectedNodeIds: Set<String>): Map<String, String>

  fun addEdge(fromNodeId: String, toNodeId: String, initialToPortId: String)
  fun setEdgeDisabled(edge: GraphEdge, isDisabled: Boolean): GraphEdge
  fun deleteEdge(edge: GraphEdge): Set<String>
  fun addNodeBetween(edge: GraphEdge): String?

  fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int)
}

@Singleton
@OptIn(FlowPreview::class)
class DefaultBrushGraphRepository @Inject constructor(
  private val customBrushDao: CustomBrushDao,
  val textureStore: CahierTextureBitmapStore,
  @ApplicationScope private val scope: CoroutineScope
) : BrushGraphRepository {
  private val _graph = MutableStateFlow(createDefaultGraph())
  override val graph: StateFlow<BrushGraph> = _graph.asStateFlow()
  init {
    scope.launch {
      graph
        .drop(1)
        .debounce(1000)
        .collect { graph ->
          try {
            val family = BrushFamilyConverter.convert(graph)
            val baos = ByteArrayOutputStream()
            AndroidBrushFamilySerialization.encode(family, baos, textureStore)
            customBrushDao.saveCustomBrush(com.example.cahier.developer.brushdesigner.data.CustomBrushEntity(AUTOSAVE_KEY, baos.toByteArray()))
          } catch (e: Exception) {
            android.util.Log.e("DefaultBrushGraphRepository", "Failed to auto-save brush", e)
          }
        }
    }
  }

  private var _lastValidBrushFamily: BrushFamily? = null
  private val _graphIssues = MutableStateFlow<List<GraphValidationException>>(emptyList())
  override val graphIssues: StateFlow<List<GraphValidationException>> = _graphIssues.asStateFlow()

  override fun setGraph(newGraph: BrushGraph) {
    _graph.update { newGraph }
  }

  override fun clearGraph() {
    _graph.update { createDefaultGraph() }
    validate()
    postDebug(DisplayText.Resource(R.string.bg_msg_graph_cleared))
  }

  override fun postDebug(displayText: DisplayText) {
    val newIssue = GraphValidationException(displayMessage = displayText, severity = ValidationSeverity.DEBUG)
    _graphIssues.update { (it + newIssue).distinctBy { issue -> Triple(issue.displayMessage, issue.nodeId, issue.severity) } }
  }

  override fun validate(): Boolean {
    val issues = GraphValidator.validateAll(_graph.value).toMutableList()

    val errorNodeIds =
      issues.filter { it.severity == ValidationSeverity.ERROR }.mapNotNull { it.nodeId }.toSet()
    val warningNodeIds =
      issues.filter { it.severity == ValidationSeverity.WARNING }.mapNotNull { it.nodeId }.toSet()

    _graph.update { currentGraph ->
      currentGraph.copy(
        nodes = currentGraph.nodes.map {
          it.copy(
            hasError = errorNodeIds.contains(it.id),
            hasWarning = warningNodeIds.contains(it.id) && !errorNodeIds.contains(it.id),
          )
        }
      )
    }

    _graphIssues.update { issues }
    return issues.none { it.severity == ValidationSeverity.ERROR }
  }

  override fun clearIssues() {
    _graphIssues.value = emptyList()
  }

  override suspend fun loadAutoSaveBrush(): Boolean {
    val entity = customBrushDao.getAutoSaveBrush().firstOrNull() ?: return false
    val decodedBytes = entity.brushBytes
    return try {
      val bais = ByteArrayInputStream(decodedBytes)
      val family = AndroidBrushFamilySerialization.decode(
        bais,
        BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
          if (bitmap != null) {
            textureStore.loadTexture(id, bitmap)
          }
          id
        }
      )
      loadBrushFamily(family)
      true
    } catch (e: Exception) {
      android.util.Log.e("DefaultBrushGraphRepository", "Failed to decode auto saved brush family", e)
      false
    }
  }

  override fun getBrushFamily(): BrushFamily? {
    if (!validate()) return _lastValidBrushFamily
    return try {
      val family = BrushFamilyConverter.convert(_graph.value)
      _lastValidBrushFamily = family
      family
    } catch (e: Exception) {
      val internalError = GraphValidationException(displayMessage = DisplayText.Resource(R.string.bg_err_internal_conversion, listOf(e.message ?: e.javaClass.simpleName)))
      _graphIssues.update { currentIssues ->
        (currentIssues + internalError).distinctBy { issue -> Triple(issue.displayMessage, issue.nodeId, issue.severity) }
      }
      _lastValidBrushFamily
    }
  }

  override fun addNode(data: NodeData): String {
    val newNode = GraphNode(id = UUID.randomUUID().toString(), data = data)
    _graph.update { it.copy(nodes = it.nodes + newNode) }
    validate()
    return newNode.id
  }

  override fun addEdge(fromNodeId: String, toNodeId: String, initialToPortId: String) {
    var toPortId = initialToPortId
    if (fromNodeId == toNodeId) return
    
    _graph.update { currentGraph ->
      val nodesById = currentGraph.nodes.associateBy { it.id }
      if (nodesById[fromNodeId] == null) return@update currentGraph
      val toNode = nodesById[toNodeId] ?: return@update currentGraph
      val existingEdge = currentGraph.edges.find { it.toNodeId == toNodeId && it.toPortId == toPortId }
      if (existingEdge != null) {
        if (existingEdge.fromNodeId != fromNodeId) return@update currentGraph
        if (!existingEdge.isDisabled) return@update currentGraph
      }
      var newGraph = currentGraph
      val toData = toNode.data
      val toPort = toNode.getVisiblePorts(currentGraph).find { it.id == toPortId }
      when (toPort) {
        is Port.AddTexture -> {
          val newPortId = UUID.randomUUID().toString()
          val newData = (toData as NodeData.Paint).copy(texturePortIds = toData.texturePortIds + newPortId)
          newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
          toPortId = newPortId
        }
        is Port.AddColor -> {
          val newPortId = UUID.randomUUID().toString()
          val newData = (toData as NodeData.Paint).copy(colorPortIds = toData.colorPortIds + newPortId)
          newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
          toPortId = newPortId
        }
        is Port.AddPaint -> {
          val newPortId = UUID.randomUUID().toString()
          val newData = (toData as NodeData.Coat).copy(paintPortIds = toData.paintPortIds + newPortId)
          newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
          toPortId = newPortId
        }
        is Port.AddCoat -> {
          val newPortId = UUID.randomUUID().toString()
          val newData = (toData as NodeData.Family).copy(coatPortIds = toData.coatPortIds + newPortId)
          newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
          toPortId = newPortId
        }
        is Port.AddBehavior -> {
          val newPortId = UUID.randomUUID().toString()
          val newData = (toData as NodeData.Tip).copy(behaviorPortIds = toData.behaviorPortIds + newPortId)
          newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
          toPortId = newPortId
        }
        is Port.AddInput -> {
          val data = toData as NodeData.Behavior
          if (data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val newPortId1 = UUID.randomUUID().toString()
            val newPortId2 = UUID.randomUUID().toString()
            val newData = data.copy(inputPortIds = data.inputPortIds + listOf(newPortId1, newPortId2))
            newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
            toPortId = newPortId1
          } else {
            val newPortId = UUID.randomUUID().toString()
            val newData = data.copy(inputPortIds = data.inputPortIds + newPortId)
            newGraph = newGraph.copy(nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it })
            toPortId = newPortId
          }
        }
        else -> {}
      }
      val fromNode = nodesById[fromNodeId]!!
      val fromPortId = if (fromNode.data.hasOutput()) "output" else return@update currentGraph
      val newEdge = GraphEdge(fromNodeId = fromNodeId, toNodeId = toNodeId, toPortId = toPortId)
      newGraph.copy(edges = newGraph.edges + newEdge)
    }
    validate()
  }

  override fun setEdgeDisabled(edge: GraphEdge, isDisabled: Boolean): GraphEdge {
    val updatedEdge = edge.copy(isDisabled = isDisabled)
    _graph.update { currentGraph ->
      currentGraph.copy(
        edges = currentGraph.edges.map { 
          if (it.fromNodeId == edge.fromNodeId && 
              it.toNodeId == edge.toNodeId && it.toPortId == edge.toPortId) updatedEdge else it 
        }
      )
    }
    validate()
    return updatedEdge
  }

  override fun deleteEdge(edge: GraphEdge): Set<String> {
    var modifiedNodeIds = emptySet<String>()
    _graph.update { currentGraph ->
      val (newGraph, ids) = calculateDeleteEdge(currentGraph, edge)
      modifiedNodeIds = ids
      newGraph
    }
    validate()
    return modifiedNodeIds
  }

  private fun calculateDeleteEdge(
    currentGraph: BrushGraph,
    edge: GraphEdge
  ): Pair<BrushGraph, Set<String>> {
    val modifiedNodeIds = mutableSetOf<String>()
    val toNode = currentGraph.nodes.find { it.id == edge.toNodeId }
    val toData = toNode?.data

    if (toData != null) {
      val filteredEdges = currentGraph.edges.filter { 
        !(it.fromNodeId == edge.fromNodeId && 
          it.toNodeId == edge.toNodeId && it.toPortId == edge.toPortId)
      }
      val remainingEdges = filteredEdges.filter { it.toNodeId == edge.toNodeId }
      
      var newGraph = currentGraph.copy(edges = filteredEdges)

      when (toData) {
        is NodeData.Coat -> {
          if (toData.paintPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(paintPortIds = toData.paintPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            modifiedNodeIds.add(edge.toNodeId)
          }
        }
        is NodeData.Behavior -> {
          val nodeCase = toData.node.nodeCase
          if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            val chunkedIds = toData.inputPortIds.chunked(2)
            val pair = chunkedIds.find { it.contains(edge.toPortId) }
            if (pair != null && pair.size == 2) {
              val hasAngle = remainingEdges.any { it.toPortId == pair[0] }
              val hasMag = remainingEdges.any { it.toPortId == pair[1] }
              if (!hasAngle && !hasMag) {
                val newData = toData.copy(inputPortIds = toData.inputPortIds - pair.toSet())
                newGraph = newGraph.copy(
                  nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
                )
                modifiedNodeIds.add(edge.toNodeId)
              }
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE) {
            // Do nothing to inputPortIds for fixed schema nodes!
          } else {
            if (toData.inputPortIds.contains(edge.toPortId)) {
              val newData = toData.copy(inputPortIds = toData.inputPortIds - edge.toPortId)
              newGraph = newGraph.copy(
                nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
              )
              modifiedNodeIds.add(edge.toNodeId)
            }
          }
        }
        is NodeData.Tip -> {
          if (toData.behaviorPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(behaviorPortIds = toData.behaviorPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            modifiedNodeIds.add(edge.toNodeId)
          }
        }
        is NodeData.Family -> {
          if (toData.coatPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(coatPortIds = toData.coatPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            modifiedNodeIds.add(edge.toNodeId)
          }
        }
        is NodeData.Paint -> {
          if (toData.texturePortIds.contains(edge.toPortId)) {
            val newData = toData.copy(texturePortIds = toData.texturePortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            modifiedNodeIds.add(edge.toNodeId)
          } else if (toData.colorPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(colorPortIds = toData.colorPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            modifiedNodeIds.add(edge.toNodeId)
          }
        }
        else -> {}
      }
      return Pair(newGraph, modifiedNodeIds)
    }
    return Pair(currentGraph, emptySet())
  }

  override fun deleteSelectedNodes(selectedNodeIds: Set<String>): Set<String> {
    val modifiedNodeIds = mutableSetOf<String>()
    _graph.update { currentGraph ->
      var g = currentGraph
      val edgesLeavingSelectedSet = g.edges.filter { edge ->
        selectedNodeIds.contains(edge.fromNodeId) && !selectedNodeIds.contains(edge.toNodeId)
      }
      
      for (edge in edgesLeavingSelectedSet) {
        val (newG, ids) = calculateDeleteEdge(g, edge)
        g = newG
        modifiedNodeIds.addAll(ids)
      }
      
      g.copy(
        edges = g.edges.filterNot { edge -> selectedNodeIds.contains(edge.toNodeId) },
        nodes = g.nodes.filterNot { node -> selectedNodeIds.contains(node.id) }
      )
    }
    validate()
    return modifiedNodeIds + selectedNodeIds
  }

  override fun updateNodeData(nodeId: String, newData: NodeData) {
    _graph.update { currentGraph ->
      val oldNode = currentGraph.nodes.find { it.id == nodeId }
      val oldData = oldNode?.data

      val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(nodeId, oldData, newData, currentGraph.edges)

      var newGraph = currentGraph.copy(
        nodes = currentGraph.nodes.map { if (it.id == nodeId) it.copy(data = finalNewData) else it },
        edges = finalEdges
      )

      if (oldData != null) {
        val updatedNode = newGraph.nodes.find { it.id == nodeId }
        val visiblePortIds = updatedNode?.getVisiblePorts(newGraph)?.map { it.id } ?: emptyList()
        
        newGraph = newGraph.copy(
          edges = newGraph.edges.filter { edge ->
            if (edge.toNodeId == nodeId) {
              edge.toPortId in visiblePortIds
            } else {
              true
            }
          }
        )
      }
      newGraph
    }
    validate()
  }

  override fun setNodeDisabled(nodeId: String, isDisabled: Boolean) {
    _graph.update { currentGraph ->
      currentGraph.copy(
        nodes = currentGraph.nodes.map { if (it.id == nodeId) it.copy(isDisabled = isDisabled) else it }
      )
    }
    validate()
  }

  override fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int) {
    val node = _graph.value.nodes.find { it.id == nodeId } ?: return
    val data = node.data
    
    when (data) {
      is NodeData.Family -> {
        val newList = data.coatPortIds.toMutableList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)
        updateNodeData(nodeId, data.copy(coatPortIds = newList))
      }
      is NodeData.Behavior -> {
        if (data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
          val setSize = 2
          val fromSet = fromIndex / setSize
          val toSet = toIndex / setSize
          if (fromSet == toSet) return
          
          val newList = data.inputPortIds.toMutableList()
          val requiredSize = maxOf(fromSet * 2 + 2, toSet * 2 + 2)
          while (newList.size < requiredSize) {
            newList.add("invalid_port_${newList.size}")
          }
          
          val temp0 = newList[fromSet * 2]
          val temp1 = newList[fromSet * 2 + 1]
          newList[fromSet * 2] = newList[toSet * 2]
          newList[fromSet * 2 + 1] = newList[toSet * 2 + 1]
          newList[toSet * 2] = temp0
          newList[toSet * 2 + 1] = temp1
          
          updateNodeData(nodeId, data.copy(inputPortIds = newList))
        } else {
          val newList = data.inputPortIds.toMutableList()
          if (fromIndex in newList.indices && toIndex in newList.indices) {
            val item = newList.removeAt(fromIndex)
            newList.add(toIndex, item)
            updateNodeData(nodeId, data.copy(inputPortIds = newList))
          }
        }
      }
      is NodeData.Paint -> {
        val T = data.texturePortIds.size
        
        val isFromTexture = fromIndex in 0 until T
        val isToTexture = toIndex in 0 until T
        val isFromColor = fromIndex in (T + 1) until (T + 1 + data.colorPortIds.size)
        val isToColor = toIndex in (T + 1) until (T + 1 + data.colorPortIds.size)
        
        if (isFromTexture && isToTexture) {
          val newList = data.texturePortIds.toMutableList()
          val item = newList.removeAt(fromIndex)
          newList.add(toIndex, item)
          updateNodeData(nodeId, data.copy(texturePortIds = newList))
        } else if (isFromColor && isToColor) {
          val fromColorIndex = fromIndex - (T + 1)
          val toColorIndex = toIndex - (T + 1)
          val newList = data.colorPortIds.toMutableList()
          val item = newList.removeAt(fromColorIndex)
          newList.add(toColorIndex, item)
          updateNodeData(nodeId, data.copy(colorPortIds = newList))
        }
      }
      is NodeData.Tip -> {
        val newList = data.behaviorPortIds.toMutableList()
        val item = newList.removeAt(fromIndex)
        newList.add(toIndex, item)
        updateNodeData(nodeId, data.copy(behaviorPortIds = newList))
      }
      is NodeData.Coat -> {
        val newList = data.paintPortIds.toMutableList()
        val item = newList.removeAt(fromIndex - 1) // Tip is at index 0
        newList.add(toIndex - 1, item)
        updateNodeData(nodeId, data.copy(paintPortIds = newList))
      }
      else -> {}
    }
  }

  override fun addNodeBetween(edge: GraphEdge): String? {
    var newNodeId: String? = null
    _graph.update { currentGraph ->
      val fromNode = currentGraph.nodes.find { it.id == edge.fromNodeId } ?: return@update currentGraph
      val toNode = currentGraph.nodes.find { it.id == edge.toNodeId } ?: return@update currentGraph
      
      if (fromNode.data !is NodeData.Behavior || toNode.data !is NodeData.Behavior) {
        return@update currentGraph // Only for behavior nodes!
      }
      
      val id = UUID.randomUUID().toString()
      newNodeId = id
      val newPortId = UUID.randomUUID().toString()
      val newNode = GraphNode(
        id = id,
        data = NodeData.Behavior(
          node = ink.proto.BrushBehavior.Node.newBuilder()
            .setResponseNode(
              ink.proto.BrushBehavior.ResponseNode.newBuilder()
                .setPredefinedResponseCurve(ProtoPredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
            )
            .build(),
          inputPortIds = listOf(newPortId)
        )
      )
      
      val edge1 = GraphEdge(fromNodeId = edge.fromNodeId, toNodeId = id, toPortId = newPortId)
      val edge2 = GraphEdge(fromNodeId = id, toNodeId = edge.toNodeId, toPortId = edge.toPortId)
      
      val newEdges = currentGraph.edges.filter { it != edge } + edge1 + edge2
      val newNodes = currentGraph.nodes + newNode
      
      currentGraph.copy(nodes = newNodes, edges = newEdges)
    }
    validate()
    return newNodeId
  }

  override fun reorganize(): BrushFamily? {
    var family: BrushFamily? = null
    var success = false
    _graph.update { currentGraph ->
      val clearedNodes = currentGraph.nodes.map { it.copy(hasError = false) }
      val g = currentGraph.copy(nodes = clearedNodes)
      
      try {
        val f = BrushFamilyConverter.convert(g)
        family = f
        success = true
        BrushGraphConverter.fromBrushFamily(f)
      } catch (e: Exception) {
        success = false
        g
      }
    }
    validate()
    if (success) {
      postDebug(DisplayText.Resource(R.string.bg_msg_graph_reorganized_success))
    } else {
      postDebug(DisplayText.Resource(R.string.bg_err_reorganization_failed))
    }
    return family
  }

  override fun loadBrushFamily(family: BrushFamily): Boolean {
    return try {
      _graph.update { BrushGraphConverter.fromBrushFamily(family) }
      validate()
      postDebug(DisplayText.Resource(R.string.bg_msg_brush_loaded_success))
      _lastValidBrushFamily = family
      true
    } catch (e: Exception) {
      Log.e("BrushGraph", "Failed to load brush", e)
      postDebug(DisplayText.Resource(R.string.bg_err_load_brush_failed))
      false
    }
  }

  override fun duplicateSelectedNodes(selectedNodeIds: Set<String>): Map<String, String> {
    var idMap = emptyMap<String, String>()
    _graph.update { currentGraph ->
      val nodesToDuplicate = currentGraph.nodes.filter { selectedNodeIds.contains(it.id) }
      idMap = nodesToDuplicate.associate { it.id to UUID.randomUUID().toString() }
      
      val newNodes = nodesToDuplicate.map { node ->
        node.copy(
          id = idMap[node.id]!!
        )
      }
      
      val edgesToDuplicate = currentGraph.edges.filter { edge ->
        selectedNodeIds.contains(edge.fromNodeId) && selectedNodeIds.contains(edge.toNodeId)
      }
      
      val newEdges = edgesToDuplicate.map { edge ->
        edge.copy(
          fromNodeId = idMap[edge.fromNodeId]!!,
          toNodeId = idMap[edge.toNodeId]!!
        )
      }
      
      currentGraph.copy(
        nodes = currentGraph.nodes + newNodes,
        edges = currentGraph.edges + newEdges
      )
    }
    validate()
    return idMap
  }

  override fun deleteNode(nodeId: String): Set<String> {
    val modifiedNodeIds = mutableSetOf<String>()
    val node = _graph.value.nodes.find { it.id == nodeId } ?: return modifiedNodeIds
    if (node.data is NodeData.Family) {
      postDebug(DisplayText.Resource(R.string.bg_err_cannot_delete_family_node))
      return modifiedNodeIds
    }

    _graph.update { currentGraph ->
      val edgesToRemove = currentGraph.edges.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
      
      // Remove edges going into the node being deleted.
      var newGraph = currentGraph.copy(edges = currentGraph.edges.filter { it.toNodeId != nodeId })
      
      // Delete edges leaving the node being deleted via calculateDeleteEdge to trigger proper port removal in target nodes.
      val edgesFromNode = edgesToRemove.filter { it.fromNodeId == nodeId }
      for (edge in edgesFromNode) {
        val (newG, ids) = calculateDeleteEdge(newGraph, edge)
        newGraph = newG
        modifiedNodeIds.addAll(ids)
      }
      
      // Finally remove the node itself.
      newGraph.copy(nodes = newGraph.nodes.filter { it.id != nodeId })
    }
    
    validate()
    modifiedNodeIds.add(nodeId)
    return modifiedNodeIds
  }

  override fun createDefaultGraph(): BrushGraph {
    val defaultTip = ink.proto.BrushTip.getDefaultInstance()
    val defaultPaint = ink.proto.BrushPaint.getDefaultInstance()
    val defaultCoat = ink.proto.BrushCoat.newBuilder()
      .setTip(defaultTip)
      .addPaintPreferences(defaultPaint)
      .build()
    val defaultProto = ink.proto.BrushFamily.newBuilder()
      .setInputModel(
        ink.proto.BrushFamily.InputModel.newBuilder()
          .setSlidingWindowModel(
            ink.proto.BrushFamily.SlidingWindowModel.newBuilder()
              .setWindowSizeSeconds(0.02f)
              .setExperimentalUpsamplingPeriodSeconds(0.005f)
          )
      )
      .addCoats(defaultCoat)
      .build()
    return BrushGraphConverter.fromProtoBrushFamily(defaultProto)
  }
}
