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
@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class, kotlinx.coroutines.FlowPreview::class)

package com.example.cahier.developer.brushgraph.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.ink.strokes.Stroke
import com.example.cahier.developer.brushdesigner.data.CustomBrushEntity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.developer.brushgraph.data.BrushGraphRepository
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.cahier.developer.brushgraph.data.BrushFamilyConverter
import com.example.cahier.developer.brushgraph.data.BrushGraphConverter
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphPoint
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.developer.brushgraph.data.StarredField
import com.example.cahier.developer.brushgraph.data.StarredFieldType
import androidx.compose.runtime.snapshotFlow
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import com.example.cahier.developer.brushgraph.data.TUTORIAL_STEPS
import com.example.cahier.developer.brushgraph.data.TutorialStep
import com.example.cahier.developer.brushgraph.data.TutorialAnchor
import com.example.cahier.developer.brushgraph.data.TutorialAction
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

data class BrushGraphUiState(
  val graph: BrushGraph = BrushGraph(),
  val isSelectionMode: Boolean = false,
  val selectedNodeIds: Set<String> = emptySet(),
  val activeEdgeSourceId: String? = null,
  val selectedEdge: GraphEdge? = null,
  val testAutoUpdateStrokes: Boolean = true,
  val testBrushColor: Int? = null,
  val testBrushSize: Float = 10f,
  val isErrorPaneOpen: Boolean = false,
  val zoom: Float = 1f,
  val offset: GraphPoint = GraphPoint(0f, 0f),
  val textFieldsLocked: Boolean = false,
  val selectedNodeId: String? = null,
  val focusTrigger: Int = 0,
  val detachedEdge: GraphEdge? = null,
  val isPreviewExpanded: Boolean = true,
  val isDarkCanvas: Boolean = false,
  val graphIssues: List<GraphValidationException> = emptyList(),
  val allTextureIds: Set<String> = emptySet(),
  val starredFields: Set<StarredField> = emptySet(),
  val isStarredInspectorOpen: Boolean = false
)

/** ViewModel to manage the state of the brush graph. */
@HiltViewModel
class BrushGraphViewModel @Inject constructor(
  private val customBrushDao: CustomBrushDao,
  public val textureStore: CahierTextureBitmapStore,
  private val repository: BrushGraphRepository
) : ViewModel() {

  /** Saved brushes in the palette. */
  val savedPaletteBrushes: StateFlow<List<CustomBrushEntity>> =
    customBrushDao.getAllCustomBrushes()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
      )

  private val _uiState = MutableStateFlow(BrushGraphUiState())
  val uiState: StateFlow<BrushGraphUiState> = _uiState.asStateFlow()

  val brush: StateFlow<androidx.ink.brush.Brush> = uiState
    .map { Triple(it.graph, it.testBrushColor, it.testBrushSize) }
    .distinctUntilChanged()
    .map { (graph, testBrushColor, testBrushSize) ->
      val family = try {
        BrushFamilyConverter.convert(graph)
      } catch (e: Exception) {
        null
      }
      val color = testBrushColor ?: 0
      val size = testBrushSize
      if (family != null) {
        Brush.createWithColorIntArgb(family, color, size, 0.1f)
      } else {
        Brush.createWithColorIntArgb(StockBrushes.marker(), color, size, 0.1f)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = Brush.createWithColorIntArgb(
        StockBrushes.marker(),
        0,
        size = 20f,
        epsilon = 0.1f,
      )
    )

  /** The list of strokes drawn in the preview area. */
  val strokeList = mutableStateListOf<androidx.ink.strokes.Stroke>()

  fun updateTestBrushColor(colorArgb: Int) {
    _uiState.update { it.copy(testBrushColor = colorArgb) }
  }

  fun updateTestBrushSize(size: Float) {
    _uiState.update { it.copy(testBrushSize = size) }
  }

  fun updateAllTextureIds() {
    _uiState.update { state -> state.copy(allTextureIds = textureStore.getAllIds()) }
  }

  val tutorialManager = TutorialManager(repository)

  val tutorialStep get() = tutorialManager.tutorialStep
  val currentStepIndex get() = tutorialManager.currentStepIndex
  val isTutorialSandboxMode get() = tutorialManager.isTutorialSandboxMode

  fun startTutorial() {
    tutorialManager.startTutorial()
  }

  fun startTutorialSandbox() {
    val oldBrushFamily = brush.value.family
    val defaultGraph = repository.createDefaultGraph()
    repository.setGraph(defaultGraph)
    
    tutorialManager.startTutorialSandbox(oldBrushFamily)
    
    validate()
  }

  fun advanceTutorial(action: TutorialAction = TutorialAction.CLICK_NEXT): Boolean {
    return tutorialManager.advanceTutorial(action)
  }

  fun regressTutorial() {
    tutorialManager.regressTutorial()
  }

  fun endTutorialSandbox(keepChanges: Boolean) {
    val brushToRestore = tutorialManager.endTutorialSandbox(keepChanges)
    if (brushToRestore != null) {
      loadBrushFamily(brushToRestore)
    }
  }

  init {
    validate()
    
    viewModelScope.launch {
      repository.graph.collect { newGraph ->
        _uiState.update { it.copy(graph = newGraph) }
      }
    }
    
    viewModelScope.launch {
      repository.graphIssues.collect { newIssues ->
        _uiState.update { it.copy(graphIssues = newIssues) }
      }
    }
    
    viewModelScope.launch {
      brush.collect { newBrush ->
        if (uiState.value.testAutoUpdateStrokes) {
          for (i in strokeList.indices) {
            strokeList[i] = strokeList[i].copy(brush = newBrush)
          }
        }
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      val success = repository.loadAutoSaveBrush()
      if (success) {
        withContext(Dispatchers.Main) {
          _uiState.update { state -> state.copy(allTextureIds = textureStore.getAllIds()) }
        }
      }
    }
  }

  fun postDebug(displayText: DisplayText) {
    repository.postDebug(displayText)
  }

  fun addNode(data: NodeData): String {
    dismissPanes()
    val newNodeId = repository.addNode(data)
    _uiState.update { it.copy(selectedNodeId = newNodeId) }
    validate()
    
    if (data is NodeData.Behavior) {
      advanceTutorial(TutorialAction.ADD_INPUT_FAB) || advanceTutorial(TutorialAction.ADD_BEHAVIOR)
    } else if (data is NodeData.ColorFunction) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
    return newNodeId
  }

  fun addFamilyNode(): String {
    return addNode(NodeData.Family())
  }

  fun addCoatNode(): String {
    return addNode(NodeData.Coat())
  }

  fun addPaintNode(): String {
    return addNode(NodeData.Paint(ProtoBrushPaint.getDefaultInstance()))
  }

  fun addTipNode(): String {
    return addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))
  }

  fun addColorFunctionNode(): String {
    return addNode(NodeData.ColorFunction(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()))
  }

  fun addTextureLayerNode(): String {
    return addNode(NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()))
  }

  fun addBehaviorNode(): String {
    return addNode(
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setTargetNode(
            ProtoBrushBehavior.TargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER)
              .setTargetModifierRangeStart(0f)
              .setTargetModifierRangeEnd(1f)
          )
          .build()
      )
    )
  }

  fun enterSelectionMode(initialNodeId: String? = null) {
    val node = initialNodeId?.let { id -> uiState.value.graph.nodes.find { it.id == id } }
    if (node?.data is NodeData.Family) return
    _uiState.update { state -> state.copy(
      isSelectionMode = true,
      selectedNodeIds = if (initialNodeId != null) setOf(initialNodeId) else emptySet()
    ) }
    dismissPanes()
    
    advanceTutorial(TutorialAction.LONG_PRESS_NODE)
  }

  fun toggleNodeSelection(nodeId: String) {
    val node = uiState.value.graph.nodes.find { it.id == nodeId }
    if (node?.data is NodeData.Family) return
    _uiState.update { state ->
      val newSelected = if (state.selectedNodeIds.contains(nodeId)) {
        state.selectedNodeIds - nodeId
      } else {
        state.selectedNodeIds + nodeId
      }
      state.copy(selectedNodeIds = newSelected)
    }
    if (uiState.value.selectedNodeIds.isEmpty()) {
      exitSelectionMode()
    }
  }

  fun selectAllNodes() {
    val allNodeIds = uiState.value.graph.nodes.filter { it.data !is NodeData.Family }.map { it.id }.toSet()
    _uiState.update { it.copy(selectedNodeIds = allNodeIds) }
  }

  fun exitSelectionMode() {
    _uiState.update { it.copy(isSelectionMode = false) }
    _uiState.update { it.copy(selectedNodeIds = emptySet()) }
    
    advanceTutorial(TutorialAction.CLICK_DONE)
  }

  fun deleteSelectedNodes() {
    val modifiedNodeIds = repository.deleteSelectedNodes(uiState.value.selectedNodeIds)

    advanceTutorial(TutorialAction.DELETE_NODE)
    
    exitSelectionMode()
  }

  fun duplicateSelectedNodes() {
    val newNodeIds = repository.duplicateSelectedNodes(uiState.value.selectedNodeIds)
    
    _uiState.update { it.copy(selectedNodeIds = newNodeIds) }
    
    advanceTutorial(TutorialAction.DUPLICATE_NODES)
  }

  fun updateNodeData(nodeId: String, newData: NodeData) {
    val oldNode = uiState.value.graph.nodes.find { it.id == nodeId }
    val oldData = oldNode?.data

    repository.updateNodeData(nodeId, newData)

    val typeChanged = when {
        oldData == null -> false
        oldData.javaClass != newData.javaClass -> true
        oldData is NodeData.Behavior && newData is NodeData.Behavior -> {
            oldData.node.nodeCase != newData.node.nodeCase
        }
        else -> false
    }

    if (typeChanged) {
        _uiState.update { state ->
            val newStarred = state.starredFields.filter { it.nodeId != nodeId }.toSet()
            state.copy(starredFields = newStarred)
        }
    }

    validate()
  }

  fun setNodeDisabled(nodeId: String, isDisabled: Boolean) {
    repository.setNodeDisabled(nodeId, isDisabled)
    validate()
  }

  fun setEdgeDisabled(edge: GraphEdge, isDisabled: Boolean) {
    _uiState.update { it.copy(selectedEdge = repository.setEdgeDisabled(edge, isDisabled)) }
    validate()
  }

  fun onNodeClick(nodeId: String) {
    _uiState.update { state -> state.copy(selectedNodeId = if (state.selectedNodeId == nodeId) null else nodeId, selectedEdge = null, isErrorPaneOpen = false, isStarredInspectorOpen = false) }
    
    advanceTutorial(TutorialAction.SELECT_NODE)
  }

  private fun checkSelectNodeTrigger(nodeId: String) {
    val node = uiState.value.graph.nodes.find { it.id == nodeId }
    if (node != null) {
      val shouldAdvance = (node.data is NodeData.Tip) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.SOURCE_NODE) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.TARGET_NODE) ||
                          tutorialStep?.getTargetNode(uiState.value.graph)?.id == nodeId
      if (shouldAdvance) {
        advanceTutorial(TutorialAction.SELECT_NODE)
      }
    }
  }

  fun onEdgeClick(edge: GraphEdge) {
    _uiState.update { state ->
      val newEdge = if (state.selectedEdge?.fromNodeId == edge.fromNodeId && 
                         state.selectedEdge?.toNodeId == edge.toNodeId && state.selectedEdge?.toPortId == edge.toPortId) null else edge
      state.copy(selectedEdge = newEdge, isErrorPaneOpen = false, selectedNodeId = null, isStarredInspectorOpen = false)
    }
    
    advanceTutorial(TutorialAction.SELECT_EDGE)
  }

  fun clearSelectedNode() {
    _uiState.update { it.copy(selectedNodeId = null) }
    
    advanceTutorial(TutorialAction.EXIT_INSPECTOR)
  }

  fun clearSelectedEdge() {
    _uiState.update { it.copy(selectedEdge = null) }
  }

  fun toggleErrorPane() {
    _uiState.update { it.copy(isErrorPaneOpen = !it.isErrorPaneOpen) }
    if (uiState.value.isErrorPaneOpen) {
      _uiState.update { it.copy(selectedNodeId = null) }
      advanceTutorial(TutorialAction.CLICK_NOTIFICATION)
    }
  }

  fun dismissPanes() {
    clearSelectedNode()
    _uiState.update { it.copy(selectedEdge = null, isErrorPaneOpen = false, activeEdgeSourceId = null, isStarredInspectorOpen = false) }
  }

  fun toggleStarredField(nodeId: String, fieldType: StarredFieldType) {
    _uiState.update { state ->
      val field = StarredField(nodeId, fieldType)
      val newStarred = if (state.starredFields.contains(field)) {
        state.starredFields - field
      } else {
        state.starredFields + field
      }
      state.copy(starredFields = newStarred)
    }
  }

  fun setStarredInspectorOpen(isOpen: Boolean) {
    _uiState.update { state -> 
      state.copy(
        isStarredInspectorOpen = isOpen,
        selectedNodeId = if (isOpen) null else state.selectedNodeId,
        selectedEdge = if (isOpen) null else state.selectedEdge,
        isErrorPaneOpen = if (isOpen) false else state.isErrorPaneOpen
      )
    }
  }

  fun onIssueClick(issue: GraphValidationException, isLandscape: Boolean, density: Float) {
    if (issue.nodeId != null) {
      centerNode(issue.nodeId)
    }
    
    advanceTutorial(TutorialAction.CLICK_ERROR_LINK)
  }

  fun centerNode(nodeId: String) {
    _uiState.update { it.copy(selectedNodeId = nodeId, selectedEdge = null, isErrorPaneOpen = false, focusTrigger = it.focusTrigger + 1) }
    checkSelectNodeTrigger(nodeId)
  }

  fun togglePreviewExpanded() {
    _uiState.update { it.copy(isPreviewExpanded = !it.isPreviewExpanded) }
  }

  fun toggleCanvasTheme() {
    _uiState.update { it.copy(isDarkCanvas = !it.isDarkCanvas) }
  }

  fun addNodeAndConnect(nodeData: NodeData, targetNodeId: String, targetPortId: String): String {
    val newNodeId = repository.addNode(nodeData)
    
    addEdge(newNodeId, targetNodeId, targetPortId)

    if (nodeData is NodeData.Behavior) {
      advanceTutorial(TutorialAction.ADD_BEHAVIOR)
    } else if (nodeData is NodeData.ColorFunction) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
    return newNodeId
  }

  /** Adds a new edge between two nodes. */
  fun addEdge(fromNodeId: String, toNodeId: String, initialToPortId: String) {
    repository.addEdge(fromNodeId, toNodeId, initialToPortId)
    validate()
    
    val fromNode = uiState.value.graph.nodes.find { it.id == fromNodeId } ?: return
    val toNode = uiState.value.graph.nodes.find { it.id == toNodeId } ?: return
    val shouldAdvance = (fromNode.data is NodeData.Behavior && fromNode.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.SOURCE_NODE &&
                          toNode.data is NodeData.Behavior && toNode.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.TARGET_NODE) ||
                         (fromNode.data is NodeData.Coat && toNode.data is NodeData.Family) ||
                         (fromNode.data is NodeData.Behavior && toNode.data is NodeData.Tip)
    if (shouldAdvance) {
      advanceTutorial(TutorialAction.CONNECT_NODES)
    }
  }

  /** Finalizes an edge edit by deleting the old edge and adding the new one. */
  fun finalizeEdgeEdit(oldEdge: GraphEdge, newFromNodeId: String, newToNodeId: String, newToPortId: String) {
    if (oldEdge.toNodeId == newToNodeId && oldEdge.toPortId == newToPortId) {
      // Reconnecting to the same port, just re-enable it.
      setEdgeDisabled(oldEdge, false)
      _uiState.update { it.copy(detachedEdge = null) }
      return
    }

    deleteEdge(oldEdge)
    
    addEdge(newFromNodeId, newToNodeId, newToPortId)
  }

  /** Detaches an edge for editing by marking it as disabled. */
  fun detachEdge(edge: GraphEdge) {
    _uiState.update { state -> state.copy(detachedEdge = edge) }
    repository.setEdgeDisabled(edge, true)
  }

  fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int) {
    repository.reorderPorts(nodeId, fromIndex, toIndex)
    advanceTutorial(TutorialAction.SWAP_PORTS)
  }

  fun deleteEdge(edge: GraphEdge) {
    if (uiState.value.selectedEdge == edge) {
      _uiState.update { state -> state.copy(selectedEdge = null) }
    }
    if (uiState.value.detachedEdge == edge) {
      _uiState.update { state -> state.copy(detachedEdge = null) }
    }

    val modifiedNodeIds = repository.deleteEdge(edge)
  }

  fun addNodeBetween(edge: GraphEdge): String? {
    dismissPanes()
    val newNodeId = repository.addNodeBetween(edge)
    if (newNodeId != null) {
      _uiState.update { it.copy(selectedNodeId = newNodeId) }
    }
    
    advanceTutorial(TutorialAction.ADD_NODE_BETWEEN)
    return newNodeId
  }

  fun clearGraph() {
    dismissPanes()
    repository.clearGraph()
    clearStrokes()
    validate()
  }

  fun deleteNode(nodeId: String) {
    val node = uiState.value.graph.nodes.find { it.id == nodeId } ?: return
    if (node.data is NodeData.Family) {
      return
    }
    if (uiState.value.selectedNodeId == nodeId) {
      _uiState.update { it.copy(selectedNodeId = null) }
    }
    
    advanceTutorial(TutorialAction.DELETE_NODE)
    
    val modifiedNodeIds = repository.deleteNode(nodeId)
    validate()
  }

  fun validate() {
    repository.validate()
  }

  fun reorganize() {
    dismissPanes()
    repository.reorganize()
  }

  fun clearStrokes() {
    strokeList.clear()
  }

  fun loadBrushFamily(family: BrushFamily) {
    dismissPanes()
    repository.loadBrushFamily(family)
  }

  fun getBrushColor(): Int = brush.value.colorIntArgb

  fun updateZoom(newZoom: Float) {
    _uiState.update { state -> state.copy(zoom = newZoom) }
  }

  fun updateOffset(newOffset: GraphPoint) {
    _uiState.update { state -> state.copy(offset = newOffset) }
  }

  fun toggleTextFieldsLocked() {
    _uiState.update { state -> state.copy(textFieldsLocked = !state.textFieldsLocked) }
  }

  fun saveToPalette(brushName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val baos = ByteArrayOutputStream()
        AndroidBrushFamilySerialization.encode(brush.value.family, baos, textureStore)
        val finalCompressedBytes = baos.toByteArray()

        customBrushDao.saveCustomBrush(
          CustomBrushEntity(name = brushName, brushBytes = finalCompressedBytes)
        )
      } catch (e: Exception) {
        postDebug(DisplayText.Resource(com.example.cahier.R.string.bg_err_save_palette, listOf(e.message ?: e.javaClass.simpleName)))
      }
    }
  }

  fun deleteFromPalette(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      customBrushDao.deleteCustomBrush(name)
    }
  }

  fun loadFromPalette(entity: CustomBrushEntity) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val family = AndroidBrushFamilySerialization.decode(
          ByteArrayInputStream(entity.brushBytes),
          BrushFamilyDecodeCallback { id, bitmap ->
            if (bitmap != null) {
              textureStore.loadTexture(id, bitmap)
            }
            id
          }
        )
        withContext(Dispatchers.Main) {
           loadBrushFamily(family)
        }
      } catch (e: Exception) {
        postDebug(DisplayText.Resource(com.example.cahier.R.string.bg_err_load_palette, listOf(e.message ?: e.javaClass.simpleName)))
      }
    }
  }

  fun setTestAutoUpdateStrokes(value: Boolean) {
    _uiState.update { state -> state.copy(testAutoUpdateStrokes = value) }
  }
}
