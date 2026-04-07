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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.ui.CahierTextureBitmapStore
import com.example.cahier.ui.brushdesigner.CustomBrushDao
import com.example.cahier.ui.brushdesigner.CustomBrushEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import java.util.UUID
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction

/** ViewModel to manage the state of the brush graph. */
@HiltViewModel
class BrushGraphViewModel @Inject constructor(
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

  /** Whether text fields in the UI are locked for editing. */
  var textFieldsLocked by mutableStateOf(false)
    private set

  /** The ID of the node currently selected as the source for the inspector. */
  var selectedNodeId by mutableStateOf<String?>(null)
    private set

  /** Whether the preview pane at the bottom is expanded. */
  var isPreviewExpanded by mutableStateOf(true)
    private set

  /** Whether the test canvas background is dark. */
  var isDarkCanvas by mutableStateOf(false)
    private set

  /** The current size of the graph viewport in pixels. */
  var viewportSize by mutableStateOf(Size.Zero)
    private set

  companion object {
    private const val TAG = "BrushGraphViewModel"
  }

  init {
    android.util.Log.d(TAG, "Initializing BrushGraphViewModel")
    // Initialize with a default brush structure: Family -> Coat -> (Tip, Paint)
    val defaultTip = ProtoBrushTip.getDefaultInstance()
    val defaultPaint = ProtoBrushPaint.getDefaultInstance()
    val defaultCoat = ProtoBrushCoat.newBuilder()
      .setTip(defaultTip)
      .addPaintPreferences(defaultPaint)
      .build()
    val defaultProto = ProtoBrushFamily.newBuilder()
      .addCoats(defaultCoat)
      .build()
    
    graph = BrushGraphConverter.fromProtoBrushFamily(defaultProto)
    validate()
  }

  /** Posts a transient debug message. */
  fun postDebug(text: String) {
    val newIssue = GraphValidationException(text, severity = ValidationSeverity.DEBUG)
    graphIssues =
      (graphIssues + newIssue).distinctBy { it.message + (it.nodeId ?: "") + it.severity }
  }

  /** Adds a new node of a specific type at the specified position. */
  fun addNode(data: NodeData, position: Offset) {
    val newNode = GraphNode(id = UUID.randomUUID().toString(), data = data, position = position)
    graph = graph.copy(nodes = graph.nodes + newNode)
    validate()
  }

  /** Adds a new Family node at the specified position. */
  fun addFamilyNode(position: Offset) {
    addNode(NodeData.Family(), position)
  }

  /** Adds a new Coat node at the specified position. */
  fun addCoatNode(position: Offset) {
    addNode(NodeData.Coat, position)
  }

  /** Adds a new Paint node at the specified position. */
  fun addPaintNode(position: Offset) {
    addNode(NodeData.Paint(ProtoBrushPaint.getDefaultInstance()), position)
  }

  /** Adds a new Tip node at the specified position. */
  fun addTipNode(position: Offset) {
    addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()), position)
  }

  /** Adds a new Behavior node (SourceNode by default) at the specified position. */
  fun addBehaviorNode(position: Offset) {
    addNode(
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setSourceNode(
            ProtoBrushBehavior.SourceNode.newBuilder()
              .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
              .setSourceValueRangeStart(0f)
              .setSourceValueRangeEnd(1f)
              .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      ),
      position,
    )
  }

  /** Updates the position of a node. */
  fun moveNode(nodeId: String, newPosition: Offset) {
    graph =
      graph.copy(
        nodes = graph.nodes.map { if (it.id == nodeId) it.copy(position = newPosition) else it }
      )
  }

  /** Moves a node by a delta. */
  fun moveNodeBy(nodeId: String, delta: Offset) {
    graph =
      graph.copy(
        nodes =
          graph.nodes.map { if (it.id == nodeId) it.copy(position = it.position + delta) else it }
      )
  }

  /** Updates the data/properties of a node. */
  fun updateNodeData(nodeId: String, newData: NodeData) {
    graph =
      graph.copy(nodes = graph.nodes.map { if (it.id == nodeId) it.copy(data = newData) else it })
    validate()
  }

  /** Handles a click on a node, depending on the current mode. */
  fun onNodeClick(nodeId: String) {
    android.util.Log.d(TAG, "onNodeClick: $nodeId")
    selectedNodeId = if (selectedNodeId == nodeId) null else nodeId
    selectedEdge = null
    isErrorPaneOpen = false
  }

  /** Handles a click on an edge. */
  fun onEdgeClick(edge: GraphEdge) {
    android.util.Log.d(TAG, "onEdgeClick: $edge")
    selectedEdge = if (selectedEdge == edge) null else edge
    selectedNodeId = null
    isErrorPaneOpen = false
  }

  /** Clears the current node selection (closes the inspector). */
  fun clearSelectedNode() {
    selectedNodeId = null
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
    }
  }

  /** Dismisses any open inspector or notification panes and clears selection. */
  fun dismissPanes() {
    selectedNodeId = null
    selectedEdge = null
    isErrorPaneOpen = false
    activeEdgeSourceId = null
  }

  /** Handles a click on an issue message: selects the erroneous node and closes the error pane. */
  fun onIssueClick(issue: GraphValidationException, isLandscape: Boolean, density: Float) {
    if (issue.nodeId != null) {
      centerNode(issue.nodeId, isLandscape, density)
    }
  }

  /** Centers the specified node in the safe area of the viewport. */
  fun centerNode(nodeId: String, isLandscape: Boolean, density: Float) {
    selectedNodeId = nodeId
    selectedEdge = null
    isErrorPaneOpen = false

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

      offset = Offset(targetCenterX - nodeCenterX * zoom, targetCenterY - nodeCenterY * zoom)
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

  /** Adds a new edge between two nodes. */
  fun addEdge(fromId: String, toId: String, toInputIndex: Int = 0) {
    // Don't allow self-loops.
    if (fromId == toId) return

    // Don't allow edges to non-existent nodes.
    val nodesById = graph.nodes.associateBy { it.id }
    if (nodesById[fromId] == null) return
    val toNode = nodesById[toId] ?: return

    var newGraph = graph

    // Special handling for Brush Family ports.
    val toData = toNode.data
    if (toData is NodeData.Family) {
      if (toInputIndex == toData.numCoats) {
        // Connecting to "Add Coat..." -> increment numCoats.
        val newData = toData.copy(numCoats = toData.numCoats + 1)
        newGraph =
          newGraph.copy(
            nodes = newGraph.nodes.map { if (it.id == toId) it.copy(data = newData) else it }
          )
      } else {
        // Connecting to an existing "Coat X" port.
        // Enforce single-connection constraint.
        if (newGraph.edges.any { it.toNodeId == toId && it.toInputIndex == toInputIndex }) {
          return
        }
      }
    }

    val newEdge = GraphEdge(fromNodeId = fromId, toNodeId = toId, toInputIndex = toInputIndex)
    graph = newGraph.copy(edges = newGraph.edges + newEdge)
    validate()
  }

  /** Deletes an edge. */
  fun deleteEdge(edge: GraphEdge) {
    if (selectedEdge == edge) {
      selectedEdge = null
    }

    var newGraph = graph
    val toNode = newGraph.nodes.find { it.id == edge.toNodeId }
    val toData = toNode?.data

    if (toData != null && toData is NodeData.Family) {
      // Shifting logic for Brush Family coats.
      val newData = toData.copy(numCoats = maxOf(0, toData.numCoats - 1))
      newGraph =
        newGraph.copy(
          nodes =
            newGraph.nodes.map { if (it.id == edge.toNodeId) it.copy(data = newData) else it },
          edges =
            newGraph.edges
              .filter { it != edge }
              .map {
                if (it.toNodeId == edge.toNodeId && it.toInputIndex > edge.toInputIndex) {
                  it.copy(toInputIndex = it.toInputIndex - 1)
                } else {
                  it
                }
              },
        )
    } else {
      newGraph = newGraph.copy(edges = newGraph.edges.filter { it != edge })
    }

    graph = newGraph
    validate()
  }

  /** Clears the graph. */
  fun clearGraph() {
    dismissPanes()
    graph = BrushGraph()
    graphIssues = emptyList()
    clearStrokes()
    validate()
    postDebug("Graph cleared")
  }

  /** Deletes a node and all associated edges. */
  fun deleteNode(nodeId: String) {
    if (selectedNodeId == nodeId) {
      selectedNodeId = null
    }
    // Identify edges that will be removed.
    val edgesToRemove = graph.edges.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
    // Remove edges going into the node being deleted.
    graph = graph.copy(edges = graph.edges.filter { it.toNodeId != nodeId })

    // Special handling for Brush Family ports. Delete edges one at a time by calling deleteEdge()
    // to ensure proper shifting logic is used.
    val edgesFromNode = edgesToRemove.filter { it.fromNodeId == nodeId }
    // Sort edges by index descending to avoid shifting issues if one node is connected multiple
    // times.
    val sortedEdgesFrom = edgesFromNode.sortedByDescending { it.toInputIndex }

    for (edge in sortedEdgesFrom) {
      deleteEdge(edge)
    }

    // Finally remove the node itself.
    graph = graph.copy(nodes = graph.nodes.filter { it.id != nodeId })
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
            (graphIssues + GraphValidationException("Internal error during validation"))
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
    brush = Brush.createWithColorIntArgb(family, brush.colorIntArgb, brush.size, brush.epsilon)
    // Update existing strokes to use the new brush.
    for (i in strokeList.indices) {
      strokeList[i] = strokeList[i].copy(brush = brush)
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
