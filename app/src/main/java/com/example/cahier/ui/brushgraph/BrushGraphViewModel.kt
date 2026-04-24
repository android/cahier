@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class, kotlinx.coroutines.FlowPreview::class)

package com.example.cahier.ui.brushgraph

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
import com.example.cahier.ui.brushgraph.data.BrushGraphRepository
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import com.example.cahier.ui.brushgraph.converters.BrushFamilyConverter
import com.example.cahier.ui.brushgraph.converters.BrushGraphConverter
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphPoint
import com.example.cahier.ui.brushgraph.model.Port
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import androidx.compose.runtime.snapshotFlow
import com.example.cahier.ui.brushgraph.ui.NodeRegistry
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import com.example.cahier.ui.brushgraph.model.TUTORIAL_STEPS
import com.example.cahier.ui.brushgraph.model.TutorialStep
import com.example.cahier.ui.brushgraph.model.TutorialAnchor
import com.example.cahier.ui.brushgraph.model.TutorialAction
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

/** ViewModel to manage the state of the brush graph. */
@HiltViewModel
class BrushGraphViewModel @Inject constructor(
  private val customBrushDao: CustomBrushDao,
  val textureStore: CahierTextureBitmapStore,
  private val repository: BrushGraphRepository,
  private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

  /** Saved brushes in the palette. */
  val savedPaletteBrushes: StateFlow<List<CustomBrushEntity>> =
    customBrushDao.getAllCustomBrushes()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
      )

  /** The current state of the brush graph. */
  var graph by mutableStateOf(BrushGraph())
    private set

  /** Whether we are in multi-selection mode. */
  var isSelectionMode by mutableStateOf(false)
    private set

  /** The set of selected node IDs. */
  var selectedNodeIds by mutableStateOf(setOf<String>())
    private set

  /** The ID of the node currently selected as the source for a new edge. */
  var activeEdgeSourceId by mutableStateOf<String?>(null)
    private set

  /** The edge currently selected for inspection. */
  var selectedEdge by mutableStateOf<GraphEdge?>(null)
    private set

  var testAutoUpdateStrokes by mutableStateOf(true)

  /** The current color of the brush in the test canvas. */
  var testBrushColor by mutableStateOf<Int?>(null)

  /** The current size of the brush in the test canvas. */
  var testBrushSize by androidx.compose.runtime.mutableFloatStateOf(10f)

  /** The current brush being designed. */
  val brush: StateFlow<Brush> = combine(
    repository.graph,
    snapshotFlow { testBrushColor }.filterNotNull(),
    snapshotFlow { testBrushSize }
  ) { graph, color, size ->
    val family = try {
      BrushFamilyConverter.convert(graph)
    } catch (e: Exception) {
      null
    }
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
      0, // Transparent until initialized by UI
      size = 20f,
      epsilon = 0.1f,
    )
  )

  /** The list of strokes drawn in the preview area. */
  val strokeList = mutableStateListOf<Stroke>()

  /** All current validation issues and notifications (errors, warnings, debug). */
  val graphIssues: StateFlow<List<GraphValidationException>> = repository.graphIssues

  /** Whether the error pane is currently open. */
  var isErrorPaneOpen by mutableStateOf(false)
    private set

  /** The current zoom level of the graph canvas. */
  var zoom by androidx.compose.runtime.mutableFloatStateOf(1f)
    private set

  /** The current pan offset of the graph canvas. */
  var offset by mutableStateOf(GraphPoint(0f, 0f))
    private set

  /** Whether text fields in the UI are locked for editing. */
  var textFieldsLocked by mutableStateOf(false)
    private set

  /** The ID of the node currently selected as the source for the inspector. */
  private val _selectedNodeId = mutableStateOf(savedStateHandle.get<String>("selectedNodeId"))
  var selectedNodeId: String?
    get() = _selectedNodeId.value
    private set(value) {
      _selectedNodeId.value = value
      savedStateHandle.set("selectedNodeId", value)
    }

  /** Trigger to notify UI to focus on the selected node. */
  var focusTrigger by androidx.compose.runtime.mutableIntStateOf(0)
    private set

  /** The edge currently detached for editing. */
  var detachedEdge by mutableStateOf<GraphEdge?>(null)

  /** Whether the preview pane at the bottom is expanded. */
  var isPreviewExpanded by mutableStateOf(true)
    private set

  /** Whether the test canvas background is dark. */
  var isDarkCanvas by mutableStateOf(false)
    private set


  fun updateTestBrushColor(colorArgb: Int) {
    testBrushColor = colorArgb
  }

  fun updateTestBrushSize(size: Float) {
    testBrushSize = size
  }

  var allTextureIds by mutableStateOf(textureStore.getAllIds())
    private set

  fun updateAllTextureIds() {
    allTextureIds = textureStore.getAllIds()
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
    graph = defaultGraph
    
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

  companion object {
    private const val TAG = "BrushGraphViewModel"
  }



  init {
    graph = repository.graph.value
    validate()
    
    viewModelScope.launch {
      brush.collect { newBrush ->
        if (testAutoUpdateStrokes) {
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
          allTextureIds = textureStore.getAllIds()
          graph = repository.graph.value
        }
      }
    }
  }

  /** Posts a transient debug message. */
  fun postDebug(text: String) {
    repository.postDebug(text)
  }

  fun addNode(data: NodeData, position: GraphPoint) {
    dismissPanes()
    val newNodeId = repository.addNode(data, position)
    graph = repository.graph.value
    selectedNodeId = newNodeId
    validate()
    
    if (data is NodeData.Behavior) {
      advanceTutorial(TutorialAction.ADD_INPUT_FAB) || advanceTutorial(TutorialAction.ADD_BEHAVIOR)
    } else if (data is NodeData.ColorFunction) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
  }

  /** Adds a new Family node at the specified position. */
  fun addFamilyNode(position: GraphPoint) {
    addNode(NodeData.Family(), position)
  }

  /** Adds a new Coat node at the specified position. */
  fun addCoatNode(position: GraphPoint) {
    addNode(NodeData.Coat(), position)
  }

  /** Adds a new Paint node at the specified position. */
  fun addPaintNode(position: GraphPoint) {
    addNode(NodeData.Paint(ProtoBrushPaint.getDefaultInstance()), position)
  }

  /** Adds a new Tip node at the specified position. */
  fun addTipNode(position: GraphPoint) {
    addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()), position)
  }

  /** Adds a new Color Function node at the specified position. */
  fun addColorFunctionNode(position: GraphPoint) {
    addNode(NodeData.ColorFunction(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()), position)
  }

  /** Adds a new Texture Layer node at the specified position. */
  fun addTextureLayerNode(position: GraphPoint) {
    addNode(NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()), position)
  }

  /** Adds a new Behavior node (TargetNode by default) at the specified position. */
  fun addBehaviorNode(position: GraphPoint) {
    addNode(
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setTargetNode(
            ProtoBrushBehavior.TargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER)
              .setTargetModifierRangeStart(0f)
              .setTargetModifierRangeEnd(1f)
          )
          .build()
      ),
      position,
    )
  }

  /** Updates the position of a node. */
  fun moveNode(nodeId: String, newPosition: GraphPoint) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    
    if (isSelectionMode && selectedNodeIds.contains(nodeId)) {
      val deltaX = newPosition.x - node.position.x
      val deltaY = newPosition.y - node.position.y
      repository.moveNodes(selectedNodeIds, deltaX, deltaY)
      graph = repository.graph.value
    } else {
      repository.moveNode(nodeId, newPosition)
      graph = repository.graph.value
    }
    
  }

  /** Enters selection mode and selects the initial node. */
  fun enterSelectionMode(initialNodeId: String? = null) {
    val node = initialNodeId?.let { id -> graph.nodes.find { it.id == id } }
    if (node?.data is NodeData.Family) return
    isSelectionMode = true
    selectedNodeIds = if (initialNodeId != null) setOf(initialNodeId) else emptySet()
    dismissPanes()
    
    // Tutorial progression
    advanceTutorial(TutorialAction.LONG_PRESS_NODE)
  }

  /** Toggles selection of a node. */
  fun toggleNodeSelection(nodeId: String) {
    val node = graph.nodes.find { it.id == nodeId }
    if (node?.data is NodeData.Family) return
    selectedNodeIds = if (selectedNodeIds.contains(nodeId)) {
      selectedNodeIds - nodeId
    } else {
      selectedNodeIds + nodeId
    }
    if (selectedNodeIds.isEmpty()) {
      exitSelectionMode()
    }
  }

  /** Selects all nodes in the graph. */
  fun selectAllNodes() {
    selectedNodeIds = graph.nodes.filter { it.data !is NodeData.Family }.map { it.id }.toSet()
  }

  /** Exits selection mode. */
  fun exitSelectionMode() {
    isSelectionMode = false
    selectedNodeIds = emptySet()
    
    // Tutorial progression
    advanceTutorial(TutorialAction.CLICK_DONE)
  }

  fun deleteSelectedNodes() {
    val modifiedNodeIds = repository.deleteSelectedNodes(selectedNodeIds)
    graph = repository.graph.value

    
    // Tutorial progression
    advanceTutorial(TutorialAction.DELETE_NODE)
    
    exitSelectionMode()
  }

  fun duplicateSelectedNodes() {
    val newNodeIds = repository.duplicateSelectedNodes(selectedNodeIds)
    graph = repository.graph.value
    
    // Select the duplicated nodes!
    selectedNodeIds = newNodeIds
    
    // Tutorial progression
    advanceTutorial(TutorialAction.DUPLICATE_NODES)
  }

  /** Updates the data/properties of a node. */
  fun updateNodeData(nodeId: String, newData: NodeData) {
    repository.updateNodeData(nodeId, newData)
    graph = repository.graph.value

    validate()
  }

  fun setNodeDisabled(nodeId: String, isDisabled: Boolean) {
    repository.setNodeDisabled(nodeId, isDisabled)
    graph = repository.graph.value
    validate()
  }

  fun setEdgeDisabled(edge: GraphEdge, isDisabled: Boolean) {
    selectedEdge = repository.setEdgeDisabled(edge, isDisabled)
    graph = repository.graph.value
    validate()
  }

  /** Handles a click on a node, depending on the current mode. */
  fun onNodeClick(nodeId: String) {
    selectedNodeId = if (selectedNodeId == nodeId) null else nodeId
    selectedEdge = null
    isErrorPaneOpen = false
    
    advanceTutorial(TutorialAction.SELECT_NODE)
  }

  private fun checkSelectNodeTrigger(nodeId: String) {
    val node = graph.nodes.find { it.id == nodeId }
    if (node != null) {
      val shouldAdvance = (node.data is NodeData.Tip) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.SOURCE_NODE) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE) ||
                          (node.data is NodeData.Behavior && node.data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.TARGET_NODE) ||
                          tutorialStep?.getTargetNode(graph)?.id == nodeId
      if (shouldAdvance) {
        advanceTutorial(TutorialAction.SELECT_NODE)
      }
    }
  }

  /** Handles a click on an edge. */
  fun onEdgeClick(edge: GraphEdge) {
    selectedEdge = if (selectedEdge?.fromNodeId == edge.fromNodeId && 
                       selectedEdge?.toNodeId == edge.toNodeId && selectedEdge?.toPortId == edge.toPortId) null else edge
    selectedNodeId = null
    isErrorPaneOpen = false
    
    // Tutorial progression
    advanceTutorial(TutorialAction.SELECT_EDGE)
  }

  /** Clears the current node selection (closes the inspector). */
  fun clearSelectedNode() {
    selectedNodeId = null
    
    advanceTutorial(TutorialAction.EXIT_INSPECTOR)
  }

  /** Clears the current edge selection. */
  fun clearSelectedEdge() {
    selectedEdge = null
  }

  /** Toggles the visibility of the error pane. */
  fun toggleErrorPane() {
    isErrorPaneOpen = !isErrorPaneOpen
    if (isErrorPaneOpen) {
      selectedNodeId = null
      advanceTutorial(TutorialAction.CLICK_NOTIFICATION)
    }
  }

  /** Dismisses any open inspector or notification panes and clears selection. */
  fun dismissPanes() {
    clearSelectedNode()
    selectedEdge = null
    isErrorPaneOpen = false
    activeEdgeSourceId = null
  }

  /** Handles a click on an issue message: selects the erroneous node and closes the error pane. */
  fun onIssueClick(issue: GraphValidationException, isLandscape: Boolean, density: Float) {
    if (issue.nodeId != null) {
      centerNode(issue.nodeId)
    }
    
    // Tutorial progression
    advanceTutorial(TutorialAction.CLICK_ERROR_LINK)
  }

  /** Focuses on the specified node in the UI. */
  fun centerNode(nodeId: String) {
    selectedNodeId = nodeId
    selectedEdge = null
    isErrorPaneOpen = false
    checkSelectNodeTrigger(nodeId)
    focusTrigger++
  }

  /** Toggles the expansion state of the preview pane. */
  fun togglePreviewExpanded() {
    isPreviewExpanded = !isPreviewExpanded
  }

  /** Toggles the theme of the test canvas. */
  fun toggleCanvasTheme() {
    isDarkCanvas = !isDarkCanvas
  }

  fun addNodeAndConnect(nodeData: NodeData, position: GraphPoint, targetNodeId: String, targetPortId: String) {
    val newNodeId = repository.addNode(nodeData, position)
    graph = repository.graph.value
    
    addEdge(newNodeId, targetNodeId, targetPortId)

    // Tutorial progression
    if (nodeData is NodeData.Behavior) {
      advanceTutorial(TutorialAction.ADD_BEHAVIOR)
    } else if (nodeData is NodeData.ColorFunction) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
  }

  /** Adds a new edge between two nodes. */
  fun addEdge(fromNodeId: String, toNodeId: String, initialToPortId: String) {
    repository.addEdge(fromNodeId, toNodeId, initialToPortId)
    graph = repository.graph.value
    validate()
    
    // Tutorial progression
    val fromNode = graph.nodes.find { it.id == fromNodeId } ?: return
    val toNode = graph.nodes.find { it.id == toNodeId } ?: return
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
      detachedEdge = null
      return
    }

    deleteEdge(oldEdge)
    
    addEdge(newFromNodeId, newToNodeId, newToPortId)
  }

  /** Detaches an edge for editing by marking it as disabled. */
  fun detachEdge(edge: GraphEdge) {
    detachedEdge = edge
    repository.setEdgeDisabled(edge, true)
    graph = repository.graph.value
  }

  fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int) {
    repository.reorderPorts(nodeId, fromIndex, toIndex)
    graph = repository.graph.value
    advanceTutorial(TutorialAction.SWAP_PORTS)
  }

  /** Deletes an edge. */
  fun deleteEdge(edge: GraphEdge) {
    if (selectedEdge == edge) {
      selectedEdge = null
    }
    if (detachedEdge == edge) {
      detachedEdge = null
    }

    val modifiedNodeIds = repository.deleteEdge(edge)
    graph = repository.graph.value
  }

  fun addNodeBetween(edge: GraphEdge) {
    dismissPanes()
    val newNodeId = repository.addNodeBetween(edge)
    graph = repository.graph.value
    if (newNodeId != null) {
      selectedNodeId = newNodeId
    }
    
    // Tutorial progression
    advanceTutorial(TutorialAction.ADD_NODE_BETWEEN)
  }

  /** Clears the graph. */
  fun clearGraph() {
    dismissPanes()
    repository.clearGraph()
    clearStrokes()
    validate()
  }

  fun deleteNode(nodeId: String) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    if (node.data is NodeData.Family) {
      return
    }
    if (selectedNodeId == nodeId) {
      selectedNodeId = null
    }
    
    // Tutorial progression
    advanceTutorial(TutorialAction.DELETE_NODE)
    
    val modifiedNodeIds = repository.deleteNode(nodeId)
    graph = repository.graph.value
    validate()
  }

  fun validate() {
    graph = repository.graph.value
  }

  fun reorganize() {
    dismissPanes()
    repository.reorganize()
    graph = repository.graph.value
  }

  /** Clears all strokes in the preview area. */
  fun clearStrokes() {
    strokeList.clear()
  }

  fun loadBrushFamily(family: BrushFamily) {
    dismissPanes()
    repository.loadBrushFamily(family)
    graph = repository.graph.value
  }

  /** Returns current brush color. */
  fun getBrushColor(): Int = brush.value.colorIntArgb

  /** Updates the zoom level. */
  fun updateZoom(newZoom: Float) {
    zoom = newZoom
  }

  /** Updates the pan offset. */
  fun updateOffset(newOffset: GraphPoint) {
    offset = newOffset
  }

  /** Toggles the text fields locked state. */
  fun toggleTextFieldsLocked() {
    textFieldsLocked = !textFieldsLocked
  }

  /** Saves the current brush to the palette. */
  fun saveToPalette(brushName: String, textureStore: TextureBitmapStore) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val baos = ByteArrayOutputStream()
        AndroidBrushFamilySerialization.encode(brush.value.family, baos, textureStore)
        val finalCompressedBytes = baos.toByteArray()

        customBrushDao.saveCustomBrush(
          CustomBrushEntity(name = brushName, brushBytes = finalCompressedBytes)
        )
      } catch (e: Exception) {
        println("Failed to save brush to palette: $e")
      }
    }
  }

  /** Deletes a brush from the palette. */
  fun deleteFromPalette(name: String) {
    viewModelScope.launch(Dispatchers.IO) {
      customBrushDao.deleteCustomBrush(name)
    }
  }

  /** Loads a brush from the palette. */
  fun loadFromPalette(entity: CustomBrushEntity, textureStore: TextureBitmapStore) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val family = AndroidBrushFamilySerialization.decode(
          ByteArrayInputStream(entity.brushBytes),
          BrushFamilyDecodeCallback { id, bitmap ->
            if (bitmap != null && textureStore is CahierTextureBitmapStore) {
              textureStore.loadTexture(id, bitmap)
            }
            id
          }
        )
        withContext(Dispatchers.Main) {
           loadBrushFamily(family)
        }
      } catch (e: Exception) {
        println("Failed to load brush from palette: $e")
      }
    }
  }
}
