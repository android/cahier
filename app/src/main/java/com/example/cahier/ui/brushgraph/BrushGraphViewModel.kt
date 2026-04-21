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
  @ApplicationContext private val context: Context,
  private val customBrushDao: CustomBrushDao
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

  /** Registry to track the actual position of ports and sizes of nodes on the screen. */
  val nodeRegistry = NodeRegistry()

  /** The ID of the node currently selected as the source for a new edge. */
  var activeEdgeSourceId by mutableStateOf<String?>(null)
    private set

  /** The edge currently selected for inspection. */
  var selectedEdge by mutableStateOf<GraphEdge?>(null)
    private set

  /** The current brush being designed. */
  var brush: Brush by
    mutableStateOf(
      Brush.createWithColorIntArgb(
        StockBrushes.marker(),
        0xFF0000FF.toInt(), // blue
        size = 20f,
        epsilon = 0.1f,
      )
    )
    private set

  /** The list of strokes drawn in the preview area. */
  val strokeList = mutableStateListOf<Stroke>()

  /** All current validation issues and notifications (errors, warnings, debug). */
  var graphIssues by mutableStateOf<List<GraphValidationException>>(emptyList())
    private set

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
  var testAutoUpdateStrokes by mutableStateOf(true)

  /** The current color of the brush in the test canvas. */
  var testBrushColor by mutableStateOf(Color.Black)

  /** The current size of the brush in the test canvas. */
  var testBrushSize by androidx.compose.runtime.mutableFloatStateOf(10f)

  fun updateTestBrushColor(color: Color) {
    testBrushColor = color
    updateBrushFromFamily(brush.family)
  }

  fun updateTestBrushSize(size: Float) {
    testBrushSize = size
    updateBrushFromFamily(brush.family)
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
    savedBrushFamily = brush.family
    graph = createDefaultGraph()
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

  private fun createDefaultGraph(): BrushGraph {
    val defaultTip = ProtoBrushTip.getDefaultInstance()
    val defaultPaint = ProtoBrushPaint.getDefaultInstance()
    val defaultCoat = ProtoBrushCoat.newBuilder()
      .setTip(defaultTip)
      .addPaintPreferences(defaultPaint)
      .build()
    val defaultProto = ProtoBrushFamily.newBuilder()
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

  init {
    android.util.Log.d(TAG, "Initializing BrushGraphViewModel")
    graph = createDefaultGraph()
    validate()
  }

  /** Posts a transient debug message. */
  fun postDebug(text: String) {
    val newIssue = GraphValidationException(text, severity = ValidationSeverity.DEBUG)
    graphIssues =
      (graphIssues + newIssue).distinctBy { it.message + (it.nodeId ?: "") + it.severity }
  }

  fun addNode(data: NodeData, position: GraphPoint) {
    dismissPanes()
    val newNode = GraphNode(id = UUID.randomUUID().toString(), data = data, position = position)
    graph = graph.copy(nodes = graph.nodes + newNode)
    selectedNodeId = newNode.id
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
      graph = graph.copy(
        nodes = graph.nodes.map {
          if (selectedNodeIds.contains(it.id)) it.copy(position = GraphPoint(it.position.x + deltaX, it.position.y + deltaY)) else it
        }
      )
    } else {
      graph = graph.copy(
        nodes = graph.nodes.map { if (it.id == nodeId) it.copy(position = newPosition) else it }
      )
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

  /** Deletes all selected nodes and connected edges. */
  fun deleteSelectedNodes() {
    // Identify edges leaving the selected set (pointing to non-selected nodes).
    // These need to be deleted via deleteEdge to trigger proper port removal in NodeData.
    val edgesLeavingSelectedSet = graph.edges.filter { edge ->
      selectedNodeIds.contains(edge.fromNodeId) && !selectedNodeIds.contains(edge.toNodeId)
    }
    
    for (edge in edgesLeavingSelectedSet) {
      deleteEdge(edge)
    }
    
    // Clean up ports in registry for removed nodes
    selectedNodeIds.forEach { nodeRegistry.clearNode(it) }

    // Remove edges targeting selected nodes and the nodes themselves.
    graph = graph.copy(
      edges = graph.edges.filterNot { edge -> selectedNodeIds.contains(edge.toNodeId) },
      nodes = graph.nodes.filterNot { node -> selectedNodeIds.contains(node.id) }
    )
    
    // Tutorial progression
    advanceTutorial(TutorialAction.DELETE_NODE)
    
    exitSelectionMode()
    validate()
  }

  /** Duplicates all selected nodes and edges between them. */
  fun duplicateSelectedNodes() {
    val nodesToDuplicate = graph.nodes.filter { selectedNodeIds.contains(it.id) }
    val idMap = nodesToDuplicate.associate { it.id to UUID.randomUUID().toString() }
    
    val offset = Offset(50f, 50f) // Fixed offset
    
    val newNodes = nodesToDuplicate.map { node ->
      node.copy(
        id = idMap[node.id]!!,
        position = GraphPoint(node.position.x + offset.x, node.position.y + offset.y)
      )
    }
    
    val edgesToDuplicate = graph.edges.filter { edge ->
      selectedNodeIds.contains(edge.fromNodeId) && selectedNodeIds.contains(edge.toNodeId)
    }
    
    val newEdges = edgesToDuplicate.map { edge ->
      edge.copy(
        fromNodeId = idMap[edge.fromNodeId]!!,
        toNodeId = idMap[edge.toNodeId]!!
      )
    }
    
    graph = graph.copy(
      nodes = graph.nodes + newNodes,
      edges = graph.edges + newEdges
    )
    
    // Select the duplicated nodes!
    selectedNodeIds = idMap.values.toSet()
    validate()
    
    // Tutorial progression
    advanceTutorial(TutorialAction.DUPLICATE_NODES)
  }

  /** Updates the data/properties of a node. */
  fun updateNodeData(nodeId: String, newData: NodeData) {
    val oldNode = graph.nodes.find { it.id == nodeId }
    val oldData = oldNode?.data

    val (finalNewData, finalEdges) = com.example.cahier.ui.brushgraph.model.preserveEdgesOnTypeChange(nodeId, oldData, newData, graph.edges)

    graph =
      graph.copy(
        nodes = graph.nodes.map { if (it.id == nodeId) it.copy(data = finalNewData) else it },
        edges = finalEdges
      )

    nodeRegistry.clearNode(nodeId)

    if (oldData != null) {
      // Delete edges that connect to ports that no longer exist.
      val updatedNode = graph.nodes.find { it.id == nodeId }
      val visiblePortIds = updatedNode?.getVisiblePorts(graph)?.map { it.id } ?: emptyList()
      
      graph = graph.copy(
        edges = graph.edges.filter { edge ->
          if (edge.toNodeId == nodeId) {
            edge.toPortId in visiblePortIds
          } else {
            true
          }
        }
      )
    }

    validate()
  }

  /** Sets the disabled state of a node. */
  fun setNodeDisabled(nodeId: String, isDisabled: Boolean) {
    graph = graph.copy(
      nodes = graph.nodes.map { if (it.id == nodeId) it.copy(isDisabled = isDisabled) else it }
    )
    validate()
  }

  /** Sets the disabled state of an edge. */
  fun setEdgeDisabled(edge: GraphEdge, isDisabled: Boolean) {
    val updatedEdge = edge.copy(isDisabled = isDisabled)
    graph = graph.copy(
      edges = graph.edges.map { 
        if (it.fromNodeId == edge.fromNodeId && 
            it.toNodeId == edge.toNodeId && it.toPortId == edge.toPortId) updatedEdge else it 
      }
    )
    selectedEdge = updatedEdge
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

  fun onPortTapped(nodeId: String, port: Port) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    
    val inferredNodeData = when (node.data) {
      is NodeData.Family -> {
        if (port.label?.contains("Coat", ignoreCase = true) == true) {
          NodeData.Coat()
        } else {
          null
        }
      }
      is NodeData.Coat -> {
        if (port.label?.contains("Tip", ignoreCase = true) == true) {
          NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        } else if (port.label?.contains("Paint", ignoreCase = true) == true) {
          NodeData.Paint(ProtoBrushPaint.getDefaultInstance())
        } else {
          null
        }
      }
      is NodeData.Tip -> {
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setTargetNode(
              ProtoBrushBehavior.TargetNode.newBuilder()
                .setTarget(ink.proto.BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER)
                .setTargetModifierRangeStart(0.0f)
                .setTargetModifierRangeEnd(1.0f)
            )
            .build(),
          "",
          UUID.randomUUID().toString()
        )
      }
      is NodeData.Behavior -> {
        // Default behavior is source node
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setSourceNode(
              ProtoBrushBehavior.SourceNode.newBuilder()
                .setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                .setSourceOutOfRangeBehavior(ink.proto.BrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                .setSourceValueRangeStart(0.0f)
                .setSourceValueRangeEnd(1.0f)
            )
            .build(),
          "",
          node.data.behaviorId
        )
      }
      is NodeData.Paint -> {
        if (port.label?.contains("Texture") == true) {
          NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance())
        } else if (port.label?.contains("Color") == true) {
          NodeData.ColorFunc(ProtoColorFunction.newBuilder()
            .setReplaceColor(
                    ink.proto.Color.newBuilder()
                      .setRed(0f)
                      .setGreen(0f)
                      .setBlue(0f)
                      .setAlpha(1f)
                      .build()
                  ).build())
        } else {
          null
        }
      }
      else -> null
    } ?: return

    val newNodeId = UUID.randomUUID().toString()
    // Position reasonably: to the left of the tapped node.
    val portAbsolute = nodeRegistry.getPortPosition(node.id, port.id, graph)
    val newX = node.position.x - inferredNodeData.width() - 100f
    val newY = portAbsolute.y - inferredNodeData.height() / 2f

    val newNode = GraphNode(id = newNodeId, data = inferredNodeData, position = GraphPoint(newX, newY))
    graph = graph.copy(nodes = graph.nodes + newNode)
    
    addEdge(newNodeId, nodeId, port.id)

    // Tutorial progression
    if (inferredNodeData is NodeData.Behavior) {
      advanceTutorial(TutorialAction.ADD_BEHAVIOR)
    } else if (inferredNodeData is NodeData.ColorFunc) {
      advanceTutorial(TutorialAction.ADD_COLOR)
    }
  }

  /** Adds a new edge between two nodes. */
  fun addEdge(fromNodeId: String, toNodeId: String, initialToPortId: String) {
    var toPortId = initialToPortId
    // Don't allow self-loops.
    if (fromNodeId == toNodeId) return

    // Don't allow edges to non-existent nodes.
    val nodesById = graph.nodes.associateBy { it.id }
    if (nodesById[fromNodeId] == null) return
    val toNode = nodesById[toNodeId] ?: return

    // Enforce single-connection constraint (allow reconnecting same edge if disabled).
    val existingEdge = graph.edges.find { it.toNodeId == toNodeId && it.toPortId == toPortId }
    if (existingEdge != null) {
      if (existingEdge.fromNodeId != fromNodeId) {
        return // Occupied by another node!
      }
      if (!existingEdge.isDisabled) {
        return // Already connected and active!
      }
    }

    var newGraph = graph
    val toData = toNode.data

    // Special handling for dynamic ports.
    val toPort = toNode.getVisiblePorts(graph).find { it.id == toPortId }
    when (toPort) {
      is Port.AddTexture -> {
        val newPortId = UUID.randomUUID().toString()
        val newData = (toData as NodeData.Paint).copy(texturePortIds = toData.texturePortIds + newPortId)
        newGraph = newGraph.copy(
          nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
        )
        toPortId = newPortId
      }
      is Port.AddColor -> {
        val newPortId = UUID.randomUUID().toString()
        val newData = (toData as NodeData.Paint).copy(colorPortIds = toData.colorPortIds + newPortId)
        newGraph = newGraph.copy(
          nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
        )
        toPortId = newPortId
      }
      is Port.Add -> {
        when (toData) {
          is NodeData.Family -> {
            val newPortId = UUID.randomUUID().toString()
            val newData = toData.copy(coatPortIds = toData.coatPortIds + newPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
            )
            toPortId = newPortId
          }
          is NodeData.Coat -> {
            val newPortId = UUID.randomUUID().toString()
            val newData = toData.copy(paintPortIds = toData.paintPortIds + newPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
            )
            toPortId = newPortId
          }
          is NodeData.Behavior -> {
            if (toData.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
              val newPortId1 = UUID.randomUUID().toString()
              val newPortId2 = UUID.randomUUID().toString()
              val newData = toData.copy(inputPortIds = toData.inputPortIds + listOf(newPortId1, newPortId2))
              newGraph = newGraph.copy(
                nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
              )
              toPortId = newPortId1
            } else {
              val newPortId = UUID.randomUUID().toString()
              val newData = toData.copy(inputPortIds = toData.inputPortIds + newPortId)
              newGraph = newGraph.copy(
                nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
              )
              toPortId = newPortId
            }
          }
          is NodeData.Tip -> {
            val newPortId = UUID.randomUUID().toString()
            val newData = toData.copy(behaviorPortIds = toData.behaviorPortIds + newPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == toNodeId) it.copy(data = newData) else it }
            )
            toPortId = newPortId
          }
          else -> {}
        }
      }
      else -> {}
    }

    val fromNode = nodesById[fromNodeId]!!
    val fromPortId = if (fromNode.data.hasOutput()) "output" else return
    val newEdge = GraphEdge(fromNodeId = fromNodeId, toNodeId = toNodeId, toPortId = toPortId)
    graph = newGraph.copy(edges = newGraph.edges + newEdge)
    validate()
    
    // Tutorial progression
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
    val newEdges = graph.edges.map {
      if (it.fromNodeId == edge.fromNodeId && 
          it.toNodeId == edge.toNodeId && it.toPortId == edge.toPortId) it.copy(isDisabled = true) else it
    }
    graph = graph.copy(edges = newEdges)
    validate()
  }

  /** Reorders ports in a list by swapping connected edges. */
  fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
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

    var newGraph = graph
    val toNode = newGraph.nodes.find { it.id == edge.toNodeId }
    val toData = toNode?.data

    if (toData != null) {
      val filteredEdges = newGraph.edges.filter { 
        !(it.fromNodeId == edge.fromNodeId && 
          it.toNodeId == edge.toNodeId && it.toPortId == edge.toPortId)
      }
      val remainingEdges = filteredEdges.filter { it.toNodeId == edge.toNodeId }
      
      newGraph = newGraph.copy(edges = filteredEdges)

      when (toData) {
        is NodeData.Coat -> {
          if (toData.paintPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(paintPortIds = toData.paintPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            nodeRegistry.clearNode(edge.toNodeId)
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
                nodeRegistry.clearNode(edge.toNodeId)
              }
            }
          } else {
            if (toData.inputPortIds.contains(edge.toPortId)) {
              val newData = toData.copy(inputPortIds = toData.inputPortIds - edge.toPortId)
              newGraph = newGraph.copy(
                nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
              )
              nodeRegistry.clearNode(edge.toNodeId)
            }
          }
        }
        is NodeData.Tip -> {
          if (toData.behaviorPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(behaviorPortIds = toData.behaviorPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            nodeRegistry.clearNode(edge.toNodeId)
          }
        }
        is NodeData.Family -> {
          if (toData.coatPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(coatPortIds = toData.coatPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            nodeRegistry.clearNode(edge.toNodeId)
          }
        }
        is NodeData.Paint -> {
          if (toData.texturePortIds.contains(edge.toPortId)) {
            val newData = toData.copy(texturePortIds = toData.texturePortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            nodeRegistry.clearNode(edge.toNodeId)
          } else if (toData.colorPortIds.contains(edge.toPortId)) {
            val newData = toData.copy(colorPortIds = toData.colorPortIds - edge.toPortId)
            newGraph = newGraph.copy(
              nodes = newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it }
            )
            nodeRegistry.clearNode(edge.toNodeId)
          }
        }
        else -> {}
      }
    }

    graph = newGraph
    validate()
  }

  fun addNodeBetween(edge: GraphEdge) {
    val fromNode = graph.nodes.find { it.id == edge.fromNodeId } ?: return
    val toNode = graph.nodes.find { it.id == edge.toNodeId } ?: return
    
    if (fromNode.data !is NodeData.Behavior || toNode.data !is NodeData.Behavior) {
      return // Only for behavior nodes!
    }
    
    dismissPanes()
    
    val newNodeId = java.util.UUID.randomUUID().toString()
    val newPortId = java.util.UUID.randomUUID().toString()
    val newNode = GraphNode(
      id = newNodeId,
      data = NodeData.Behavior(
        node = ProtoBrushBehavior.Node.newBuilder()
          .setResponseNode(
            ProtoBrushBehavior.ResponseNode.newBuilder()
              .setPredefinedResponseCurve(ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
          )
          .build(),
        inputPortIds = listOf(newPortId)
      ),
      position = GraphPoint(
        (fromNode.position.x + toNode.position.x) / 2f,
        (fromNode.position.y + toNode.position.y) / 2f
      )
    )
    
    val behaviorData = newNode.data as NodeData.Behavior
    val edge1 = GraphEdge(fromNodeId = edge.fromNodeId, toNodeId = newNodeId, toPortId = newPortId)
    val edge2 = GraphEdge(fromNodeId = newNodeId, toNodeId = edge.toNodeId, toPortId = edge.toPortId)
    
    val newEdges = graph.edges.filter { it != edge } + edge1 + edge2
    val newNodes = graph.nodes + newNode
    
    graph = graph.copy(nodes = newNodes, edges = newEdges)
    selectedNodeId = newNodeId
    validate()
    
    // Tutorial progression
    advanceTutorial(TutorialAction.ADD_NODE_BETWEEN)
  }

  /** Clears the graph. */
  fun clearGraph() {
    dismissPanes()
    graph = createDefaultGraph()
    graphIssues = emptyList()
    clearStrokes()
    validate()
    postDebug("Graph cleared")
  }

  /** Deletes a node and all associated edges. */
  fun deleteNode(nodeId: String) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    if (node.data is NodeData.Family) {
      postDebug("Cannot delete Family node")
      return
    }
    if (selectedNodeId == nodeId) {
      selectedNodeId = null
    }
    
    // Tutorial progression
    advanceTutorial(TutorialAction.DELETE_NODE)
    // Identify edges that will be removed.
    // Identify edges that will be removed.
    val edgesToRemove = graph.edges.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
    // Remove edges going into the node being deleted.
    graph = graph.copy(edges = graph.edges.filter { it.toNodeId != nodeId })

    // Delete edges leaving the node being deleted via deleteEdge to trigger proper port removal in target nodes.
    val edgesFromNode = edgesToRemove.filter { it.fromNodeId == nodeId }

    for (edge in edgesFromNode) {
      deleteEdge(edge)
    }

    // Finally remove the node itself.
    graph = graph.copy(nodes = graph.nodes.filter { it.id != nodeId })
    nodeRegistry.clearNode(nodeId)
    validate()
  }

  /** Validates the current graph and applies it to the preview brush. */
  fun validate() {
    // 1. Run all-node validation.
    val issues = BrushFamilyConverter.validateAll(graph).toMutableList()

    // 2. Mark nodes with errors/warnings in the graph model.
    val errorNodeIds =
      issues.filter { it.severity == ValidationSeverity.ERROR }.mapNotNull { it.nodeId }.toSet()
    val warningNodeIds =
      issues.filter { it.severity == ValidationSeverity.WARNING }.mapNotNull { it.nodeId }.toSet()

    graph =
      graph.copy(
        nodes =
          graph.nodes.map {
            it.copy(
              hasError = errorNodeIds.contains(it.id),
              hasWarning = warningNodeIds.contains(it.id) && !errorNodeIds.contains(it.id),
            )
          }
      )

    graphIssues = issues

    // 3. Attempt to update the live brush.
    if (issues.none { it.severity == ValidationSeverity.ERROR }) {
      try {
        val family = BrushFamilyConverter.convert(graph)
        updateBrushFromFamily(family)
      } catch (e: Exception) {
        if (e is GraphValidationException) {
          graphIssues =
            (graphIssues + e).distinctBy { it.message + (it.nodeId ?: "") + it.severity }
        } else {
          android.util.Log.e(TAG, "Validation conversion failed", e)
          graphIssues =
            (graphIssues + GraphValidationException("Internal error during validation: ${e.message ?: e.javaClass.simpleName}"))
              .distinctBy { it.message + (it.nodeId ?: "") + it.severity }
        }
      }
    }
  }

  /** Reorganizes the graph layout and validates it. */
  fun reorganize() {
    dismissPanes()
    // Clear previous errors.
    graph = graph.copy(nodes = graph.nodes.map { it.copy(hasError = false) })

    try {
      val family = BrushFamilyConverter.convert(graph)
      graph = BrushGraphConverter.fromBrushFamily(family)
      updateBrushFromFamily(family)
      validate()
      postDebug("Graph reorganized successfully")
    } catch (e: Exception) {
      // Reorganization also triggers validation.
      validate()
      if (e is GraphValidationException) {
        graphIssues = (graphIssues + e).distinctBy { it.message + (it.nodeId ?: "") + it.severity }
      } else {
        android.util.Log.e(TAG, "Reorganization failed", e)
        postDebug("Reorganization failed: ${e.message}")
      }
    }
  }

  /** Internal helper to update the brush and strokes from a [BrushFamily]. */
  private fun updateBrushFromFamily(family: BrushFamily) {
    brush = Brush.createWithColorIntArgb(family, testBrushColor.toArgb(), testBrushSize, brush.epsilon)
    // Update existing strokes to use the new brush if auto-update is enabled.
    if (testAutoUpdateStrokes) {
      for (i in strokeList.indices) {
        strokeList[i] = strokeList[i].copy(brush = brush)
      }
    }
  }

  /** Clears all strokes in the preview area. */
  fun clearStrokes() {
    strokeList.clear()
  }

  /** Loads a [BrushFamily] into the graph and reconstructs layout. */
  fun loadBrushFamily(family: BrushFamily) {
    dismissPanes()
    try {
      graph = BrushGraphConverter.fromBrushFamily(family)
      updateBrushFromFamily(family)
      validate()
      postDebug("Brush loaded successfully")
    } catch (e: Exception) {
      android.util.Log.e("BrushGraph", "Failed to load brush", e)
      postDebug("Failed to load brush: ${e.message}")
    }
  }

  /** Returns current brush color. */
  fun getBrushColor(): Color = Color(brush.colorIntArgb)

  /** Updates brush color. */
  fun updateBrushColor(color: Color) {
    brush = Brush.createWithColorIntArgb(brush.family, color.toArgb(), brush.size, brush.epsilon)
  }

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
        AndroidBrushFamilySerialization.encode(brush.family, baos, textureStore)
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
