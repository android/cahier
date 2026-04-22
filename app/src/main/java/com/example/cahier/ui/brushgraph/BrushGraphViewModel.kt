@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.ink.strokes.Stroke
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.ui.CahierTextureBitmapStore
import com.example.cahier.ui.brushgraph.data.BrushGraphRepository
import com.example.cahier.ui.brushdesigner.CustomBrushDao
import com.example.cahier.ui.brushdesigner.CustomBrushEntity
import android.content.Context
import androidx.ink.storage.decode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
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
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import androidx.compose.runtime.snapshotFlow
import com.example.cahier.ui.brushgraph.ui.NodeRegistry
import java.util.UUID
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

/** ViewModel to manage the state of the brush graph. */
@HiltViewModel
class BrushGraphViewModel @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val customBrushDao: CustomBrushDao,
  val textureStore: CahierTextureBitmapStore,
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
  var testBrushColor by mutableStateOf(Color.Black)

  /** The current size of the brush in the test canvas. */
  var testBrushSize by androidx.compose.runtime.mutableFloatStateOf(10f)

  /** The current brush being designed. */
  val brush: StateFlow<Brush> = combine(
    repository.graph,
    snapshotFlow { testBrushColor },
    snapshotFlow { testBrushSize }
  ) { graph, color, size ->
    val family = try {
      BrushFamilyConverter.convert(graph)
    } catch (e: Exception) {
      null
    }
    if (family != null) {
      Brush.createWithColorIntArgb(family, color.toArgb(), size, 0.1f)
    } else {
      Brush.createWithColorIntArgb(StockBrushes.marker(), color.toArgb(), size, 0.1f)
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = Brush.createWithColorIntArgb(
      StockBrushes.marker(),
      0xFF0000FF.toInt(), // blue
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
  var offset by mutableStateOf(Offset.Zero)
    private set

  private val _events = MutableSharedFlow<BrushGraphEvent>()
  val events = _events.asSharedFlow()

  sealed class BrushGraphEvent {
    data class CenterOnNode(val offset: Offset) : BrushGraphEvent()
  }

  /** Whether text fields in the UI are locked for editing. */
  var textFieldsLocked by mutableStateOf(false)
    private set

  /** The ID of the node currently selected as the source for the inspector. */
  var selectedNodeId by mutableStateOf<String?>(null)
    private set

  /** The edge currently detached for editing. */
  var detachedEdge by mutableStateOf<GraphEdge?>(null)

  /** Whether the preview pane at the bottom is expanded. */
  var isPreviewExpanded by mutableStateOf(true)
    private set

  /** Whether the test canvas background is dark. */
  var isDarkCanvas by mutableStateOf(false)
    private set

  /** The current size of the graph viewport in pixels. */
  var viewportSize by mutableStateOf(Size.Zero)
    private set

  /** Whether to auto update strokes in the test canvas. */


  fun updateTestBrushColor(color: Color) {
    testBrushColor = color
  }

  fun updateTestBrushSize(size: Float) {
    testBrushSize = size
  }

  var allTextureIds by mutableStateOf(textureStore.getAllIds())
    private set

  fun updateAllTextureIds() {
    allTextureIds = textureStore.getAllIds()
  }

  /** The current step in the tutorial, or null if not in tutorial mode. */
  var tutorialStep by mutableStateOf<TutorialStep?>(null)
    private set

  var currentStepIndex by mutableStateOf(0)
    private set

  private val tutorialSteps = mutableStateListOf<TutorialStep>()

  var savedBrushFamily by mutableStateOf<BrushFamily?>(null)
    private set

  var isTutorialSandboxMode by mutableStateOf(false)
    private set

  fun startTutorial() {
    tutorialSteps.clear()
    tutorialSteps.addAll(TUTORIAL_STEPS)
    currentStepIndex = 0
    tutorialStep = tutorialSteps.getOrNull(currentStepIndex)
  }

  fun startTutorialSandbox() {
    savedBrushFamily = brush.value.family
    val defaultGraph = repository.createDefaultGraph()
    repository.setGraph(defaultGraph)
    graph = defaultGraph
    isTutorialSandboxMode = true
    
    startTutorial()
    

    // Refresh current step just in case
    tutorialStep = tutorialSteps.getOrNull(currentStepIndex)
    
    validate()
  }

  fun advanceTutorial(action: TutorialAction = TutorialAction.CLICK_NEXT): Boolean {
    val step = tutorialStep
    if (step != null && step.actionRequired == action) {
      currentStepIndex++
      if (currentStepIndex < tutorialSteps.size) {
        tutorialStep = tutorialSteps[currentStepIndex]
      } else {
        tutorialStep = null // Tutorial finished!
      }
      return true
    }
    return false
  }

  fun regressTutorial() {
    if (currentStepIndex > 0) {
      currentStepIndex--
      tutorialStep = tutorialSteps[currentStepIndex]
    }
  }

  fun endTutorialSandbox(keepChanges: Boolean) {
    isTutorialSandboxMode = false
    if (!keepChanges && savedBrushFamily != null) {
      loadBrushFamily(savedBrushFamily!!)
    }
    savedBrushFamily = null
    tutorialStep = null
  }

  companion object {
    private const val TAG = "BrushGraphViewModel"
  }



  init {
    android.util.Log.d(TAG, "Initializing BrushGraphViewModel")
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
    
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
      val prefs = context.getSharedPreferences("brush_graph_prefs", android.content.Context.MODE_PRIVATE)
      val savedBrushBase64 = prefs.getString("auto_save_brush", null)
      if (savedBrushBase64 != null) {
        try {
          val decodedBytes = android.util.Base64.decode(savedBrushBase64, android.util.Base64.DEFAULT)
          val bais = java.io.ByteArrayInputStream(decodedBytes)
          val family = androidx.ink.storage.AndroidBrushFamilySerialization.decode(
            bais,
            androidx.ink.storage.BrushFamilyDecodeCallback { id: String, bitmap: android.graphics.Bitmap? ->
              if (bitmap != null) {
                textureStore.loadTexture(id, bitmap)
              }
              id
            }
          )
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            allTextureIds = textureStore.getAllIds()
            loadBrushFamily(family)
          }
        } catch (e: Exception) {
          android.util.Log.e(TAG, "Failed to decode brush family from prefs", e)
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
    } else if (data is NodeData.ColorFunc) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
  }

  /** Adds a new Family node at the specified position. */
  fun addFamilyNode(position: Offset) {
    addNode(NodeData.Family(), GraphPoint(position.x, position.y))
  }

  /** Adds a new Coat node at the specified position. */
  fun addCoatNode(position: Offset) {
    addNode(NodeData.Coat(), GraphPoint(position.x, position.y))
  }

  /** Adds a new Paint node at the specified position. */
  fun addPaintNode(position: Offset) {
    addNode(NodeData.Paint(ProtoBrushPaint.getDefaultInstance()), GraphPoint(position.x, position.y))
  }

  /** Adds a new Tip node at the specified position. */
  fun addTipNode(position: Offset) {
    addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()), GraphPoint(position.x, position.y))
  }

  /** Adds a new Color Function node at the specified position. */
  fun addColorFunctionNode(position: Offset) {
    addNode(NodeData.ColorFunc(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()), GraphPoint(position.x, position.y))
  }

  /** Adds a new Texture Layer node at the specified position. */
  fun addTextureLayerNode(position: Offset) {
    addNode(NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()), GraphPoint(position.x, position.y))
  }

  /** Adds a new Behavior node (TargetNode by default) at the specified position. */
  fun addBehaviorNode(position: Offset) {
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
      GraphPoint(position.x, position.y),
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
    android.util.Log.d(TAG, "onNodeClick: $nodeId")
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
    android.util.Log.d(TAG, "onEdgeClick: $edge")
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
      centerNode(issue.nodeId, isLandscape, density)
    }
    
    // Tutorial progression
    advanceTutorial(TutorialAction.CLICK_ERROR_LINK)
  }

  /** Centers the specified node in the safe area of the viewport. */
  fun centerNode(nodeId: String, isLandscape: Boolean, density: Float) {
    selectedNodeId = nodeId
    selectedEdge = null
    isErrorPaneOpen = false
    checkSelectNodeTrigger(nodeId)

    // Center the node in the remaining on-screen safe area, accounting for sidebar and preview
    // panes.
    val node = graph.nodes.find { it.id == nodeId }
    if (node != null && viewportSize.width > 0 && viewportSize.height > 0) {
      val previewHeightPx =
        (if (isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED) * density

      val (safeWidth, safeHeight) =
        if (isLandscape) {
          val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
          (viewportSize.width - inspectorWidthPx) to (viewportSize.height - previewHeightPx)
        } else {
          val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
          // Both inspector and preview are at the bottom.
          viewportSize.width to (viewportSize.height - maxOf(inspectorHeightPx, previewHeightPx))
        }

      val targetCenterX = safeWidth / 2f
      val targetCenterY = safeHeight / 2f

      val nodeCenterX = node.position.x + node.data.width() / 2f
      val nodeCenterY = node.position.y + node.data.height() / 2f

      val newOffset = Offset(targetCenterX - nodeCenterX * zoom, targetCenterY - nodeCenterY * zoom)
      viewModelScope.launch {
        _events.emit(BrushGraphEvent.CenterOnNode(newOffset))
      }
    }
  }

  /** Updates the tracked size of the viewport. */
  fun updateViewportSize(size: Size) {
    viewportSize = size
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
    } else if (nodeData is NodeData.ColorFunc) {
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
  fun getBrushColor(): Color = Color(brush.value.colorIntArgb)



  /** Updates the zoom level. */
  fun updateZoom(newZoom: Float) {
    zoom = newZoom
  }

  /** Updates the pan offset. */
  fun updateOffset(newOffset: Offset) {
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
        val baos = java.io.ByteArrayOutputStream()
        AndroidBrushFamilySerialization.encode(brush.value.family, baos, textureStore)
        val finalCompressedBytes = baos.toByteArray()

        customBrushDao.saveCustomBrush(
          CustomBrushEntity(name = brushName, brushBytes = finalCompressedBytes)
        )
      } catch (e: Exception) {
        android.util.Log.e(TAG, "Failed to save brush to palette", e)
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
          java.io.ByteArrayInputStream(entity.brushBytes),
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
        android.util.Log.e(TAG, "Failed to load brush from palette", e)
      }
    }
  }
}
