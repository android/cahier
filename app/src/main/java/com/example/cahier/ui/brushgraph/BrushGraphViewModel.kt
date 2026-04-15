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

  /** Adds a new node of a specific type at the specified position. */
  fun addNode(data: NodeData, position: Offset) {
    val newNode = GraphNode(id = UUID.randomUUID().toString(), data = data, position = position)
    graph = graph.copy(nodes = graph.nodes + newNode)
    selectedNodeId = newNode.id
    selectedEdge = null
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

  /** Adds a new Color Function node at the specified position. */
  fun addColorFunctionNode(position: Offset) {
    addNode(NodeData.ColorFunc(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()), position)
  }

  /** Adds a new Texture Layer node at the specified position. */
  fun addTextureLayerNode(position: Offset) {
    addNode(NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance()), position)
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
    val oldNode = graph.nodes.find { it.id == nodeId }
    val oldData = oldNode?.data

    graph =
      graph.copy(nodes = graph.nodes.map { if (it.id == nodeId) it.copy(data = newData) else it })

    if (oldData != null) {
      val oldPortCount = oldData.inputLabels().size
      val newPortCount = newData.inputLabels().size

      if (newPortCount < oldPortCount) {
        // Delete edges that connect to ports that no longer exist.
        graph = graph.copy(
          edges = graph.edges.filter { edge ->
            !(edge.toPort.nodeId == nodeId && edge.toPort.index >= newPortCount)
          }
        )
      }
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
      edges = graph.edges.map { if (it.fromPort == edge.fromPort && it.toPort == edge.toPort) updatedEdge else it }
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
  }

  /** Handles a click on an edge. */
  fun onEdgeClick(edge: GraphEdge) {
    android.util.Log.d(TAG, "onEdgeClick: $edge")
    selectedEdge = if (selectedEdge?.fromPort == edge.fromPort && selectedEdge?.toPort == edge.toPort) null else edge
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

  /** Handles tapping on an empty port to add a default node. */
  fun onPortTapped(nodeId: String, port: Port) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    val inferredNodeData = when (node.data) {
      is NodeData.Family -> {
        if (port.label?.contains("Coat", ignoreCase = true) == true) {
          NodeData.Coat
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
    val portOffset = node.data.getPortPosition(PortSide.INPUT, port.index)
    val newX = node.position.x - inferredNodeData.width() - 100f
    val newY = node.position.y + portOffset.y - inferredNodeData.height() / 2f

    val newNode = GraphNode(id = newNodeId, data = inferredNodeData, position = Offset(newX, newY))
    graph = graph.copy(nodes = graph.nodes + newNode)
    
    addEdge(newNodeId, nodeId, port.index)
  }

  /** Adds a new edge between two nodes. */
  fun addEdge(fromId: String, toId: String, toInputIndex: Int = 0) {
    // Don't allow self-loops.
    if (fromId == toId) return

    // Don't allow edges to non-existent nodes.
    val nodesById = graph.nodes.associateBy { it.id }
    if (nodesById[fromId] == null) return
    val toNode = nodesById[toId] ?: return

    // Enforce single-connection constraint (allow reconnecting same edge if disabled).
    val existingEdge = graph.edges.find { it.toPort.nodeId == toId && it.toPort.index == toInputIndex }
    if (existingEdge != null) {
      if (existingEdge.fromPort.nodeId != fromId) {
        return // Occupied by another node!
      }
      if (!existingEdge.isDisabled) {
        return // Already connected and active!
      }
    }

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
      }
    } else if (toData is NodeData.Paint) {
      val textureEdges = graph.edges.filter { edge ->
        if (edge.isDisabled) return@filter false
        if (edge.toPort.nodeId != toId) return@filter false
        val fromNode = nodesById[edge.fromPort.nodeId]
        fromNode != null && !fromNode.isDisabled && fromNode.data is NodeData.TextureLayer
      }
      val T = textureEdges.size
      if (toInputIndex == T) {
        // Connecting to "Add Texture..."
        // Shift all color edges up by 1!
        newGraph = newGraph.copy(
          edges = newGraph.edges.map { edge ->
            if (edge.toPort.nodeId == toId) {
              val fromNode = nodesById[edge.fromPort.nodeId]
              if (fromNode != null && fromNode.data is NodeData.ColorFunc) {
                return@map edge.copy(toPort = edge.toPort.copy(index = edge.toPort.index + 1))
              }
            }
            edge
          }
        )
      }
    }

    val newEdge = GraphEdge(fromPort = Port(fromId, PortSide.OUTPUT, 0), toPort = Port(toId, PortSide.INPUT, toInputIndex))
    graph = newGraph.copy(edges = newGraph.edges + newEdge)
    validate()
  }

  /** Finalizes an edge edit by deleting the old edge and adding the new one. */
  fun finalizeEdgeEdit(oldEdge: GraphEdge, newFromNodeId: String, newToNodeId: String, newToIndex: Int) {
    if (oldEdge.toPort.nodeId == newToNodeId && oldEdge.toPort.index == newToIndex) {
      // Reconnecting to the same port, just re-enable it.
      setEdgeDisabled(oldEdge, false)
      detachedEdge = null
      return
    }

    deleteEdge(oldEdge)
    
    var adjustedIndex = newToIndex
    if (oldEdge.toPort.nodeId == newToNodeId && oldEdge.toPort.index < newToIndex) {
      adjustedIndex = newToIndex - 1
    }
    
    addEdge(newFromNodeId, newToNodeId, adjustedIndex)
  }

  /** Detaches an edge for editing by marking it as disabled. */
  fun detachEdge(edge: GraphEdge) {
    detachedEdge = edge
    val newEdges = graph.edges.map {
      if (it.fromPort == edge.fromPort && it.toPort == edge.toPort) it.copy(isDisabled = true) else it
    }
    graph = graph.copy(edges = newEdges)
    validate()
  }

  /** Reorders ports in a list by swapping connected edges. */
  fun reorderPorts(nodeId: String, fromIndex: Int, toIndex: Int) {
    val node = graph.nodes.find { it.id == nodeId } ?: return
    val data = node.data
    
    if (data is NodeData.Behavior && data.node.nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
        val setSize = data.inputLabels().size
        val fromSet = fromIndex / setSize
        val toSet = toIndex / setSize
        if (fromSet == toSet) return
        
        val newEdges = graph.edges.map { edge ->
            if (edge.toPort.nodeId == nodeId) {
                val edgeSet = edge.toPort.index / setSize
                val edgeOffset = edge.toPort.index % setSize
                if (edgeSet == fromSet) {
                    edge.copy(toPort = edge.toPort.copy(index = toSet * setSize + edgeOffset))
                } else if (edgeSet == toSet) {
                    edge.copy(toPort = edge.toPort.copy(index = fromSet * setSize + edgeOffset))
                } else {
                    edge
                }
            } else {
                edge
            }
        }
        graph = graph.copy(edges = newEdges)
        validate()
        return
    }
    
    if (data is com.example.cahier.ui.brushgraph.model.NodeData.Paint) {
        val textureEdges = graph.edges.filter { edge ->
            val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
            fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.TextureLayer && edge.toPort.nodeId == nodeId
        }
        val T = textureEdges.size
        
        val colorEdges = graph.edges.filter { edge ->
            val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId }
            fromNode?.data is com.example.cahier.ui.brushgraph.model.NodeData.ColorFunc && edge.toPort.nodeId == nodeId
        }
        val C = colorEdges.size
        
        val isFromTexture = fromIndex in 0 until T
        val isToTexture = toIndex in 0 until T
        val isFromColor = fromIndex in (T + 1) until (T + 1 + C)
        val isToColor = toIndex in (T + 1) until (T + 1 + C)
        
        if ((isFromTexture && isToTexture) || (isFromColor && isToColor)) {
            // Valid swap within same list!
        } else {
            return // Invalid swap!
        }
    }
    
    val newEdges = graph.edges.map { edge ->
        if (edge.toPort.nodeId == nodeId) {
            if (edge.toPort.index == fromIndex) {
                edge.copy(toPort = edge.toPort.copy(index = toIndex))
            } else if (edge.toPort.index == toIndex) {
                edge.copy(toPort = edge.toPort.copy(index = fromIndex))
            } else {
                edge
            }
        } else {
            edge
        }
    }
    graph = graph.copy(edges = newEdges)
    validate()
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
    val toNode = newGraph.nodes.find { it.id == edge.toPort.nodeId }
    val toData = toNode?.data

    if (toData != null) {
      val filteredEdges = newGraph.edges.filter { it.fromPort != edge.fromPort || it.toPort != edge.toPort }
      val remainingEdges = filteredEdges.filter { it.toPort.nodeId == edge.toPort.nodeId }
      val maxInputIndex = graph.edges.filter { it.toPort.nodeId == edge.toPort.nodeId }.maxOfOrNull { it.toPort.index } ?: 0
      
      val updatedEdges = when (toData) {
        is NodeData.Coat -> {
          if (edge.toPort.index == 0) {
            // Deleting Tip edge. No shifting!
            filteredEdges
          } else {
            // Deleting Paint edge. Shift subsequent paint edges!
            val visiblePorts = toNode.getVisiblePorts(graph)
            val portToDelete = visiblePorts.find { it.index == edge.toPort.index }
            if (portToDelete != null) {
              nodeRegistry.deletePortAndShift(portToDelete)
            }
            filteredEdges.map {
              if (it.toPort.nodeId == edge.toPort.nodeId && it.toPort.index > edge.toPort.index) {
                it.copy(toPort = it.toPort.copy(index = it.toPort.index - 1))
              } else {
                it
              }
            }
          }
        }
        is NodeData.Behavior -> {
          val nodeCase = toData.node.nodeCase
          if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
            // Polar Target: Shift only if a full set is empty!
            val setIndex = edge.toPort.index / 2
            val hasAngle = remainingEdges.any { it.toPort.index == setIndex * 2 }
            val hasMag = remainingEdges.any { it.toPort.index == setIndex * 2 + 1 }
            
            if (!hasAngle && !hasMag) {
              // Set is empty! Delete it and shift subsequent sets up by 2!
              val firstPortIndex = setIndex * 2
              val visiblePorts = toNode.getVisiblePorts(graph)
              val portToDelete = visiblePorts.find { it.index == firstPortIndex }
              if (portToDelete != null) {
                nodeRegistry.deletePortAndShift(portToDelete)
                nodeRegistry.deletePortAndShift(portToDelete)
              }
              filteredEdges.map {
                if (it.toPort.nodeId == edge.toPort.nodeId && it.toPort.index > edge.toPort.index) {
                  it.copy(toPort = it.toPort.copy(index = it.toPort.index - 2))
                } else {
                  it
                }
              }
            } else {
              // Set not empty. No shifting!
              filteredEdges
            }
          } else if (nodeCase == ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE) {
            // Interpolation: Fixed ports. No shifting!
            filteredEdges
          } else {
            // Other behaviors (BinaryOp, Tip, etc.): Shift all subsequent edges!
            val visiblePorts = toNode.getVisiblePorts(graph)
            val portToDelete = visiblePorts.find { it.index == edge.toPort.index }
            if (portToDelete != null) {
              nodeRegistry.deletePortAndShift(portToDelete)
            }
            filteredEdges.map {
              if (it.toPort.nodeId == edge.toPort.nodeId && it.toPort.index > edge.toPort.index) {
                it.copy(toPort = it.toPort.copy(index = it.toPort.index - 1))
              } else {
                it
              }
            }
          }
        }
        is NodeData.Family, is NodeData.Paint -> {
          val visiblePorts = toNode.getVisiblePorts(graph)
          val portToDelete = visiblePorts.find { it.index == edge.toPort.index }
          if (portToDelete != null) {
            nodeRegistry.deletePortAndShift(portToDelete)
          }
          filteredEdges.map {
            if (it.toPort.nodeId == edge.toPort.nodeId && it.toPort.index > edge.toPort.index) {
              it.copy(toPort = it.toPort.copy(index = it.toPort.index - 1))
            } else {
              it
            }
          }
        }
        else -> filteredEdges
      }
      
      newGraph = newGraph.copy(edges = updatedEdges)
      
      if (toData is NodeData.Family) {
        val newData = toData.copy(numCoats = maxOf(0, toData.numCoats - 1))
        newGraph =
          newGraph.copy(
            nodes =
              newGraph.nodes.map { if (it.id == edge.toPort.nodeId) it.copy(data = newData) else it }
          )
      }
    }

    graph = newGraph
    validate()
  }

  /** Adds an operator node between two behavior nodes. */
  fun addNodeBetween(edge: GraphEdge) {
    val fromNode = graph.nodes.find { it.id == edge.fromPort.nodeId } ?: return
    val toNode = graph.nodes.find { it.id == edge.toPort.nodeId } ?: return
    
    if (fromNode.data !is NodeData.Behavior || toNode.data !is NodeData.Behavior) {
      return // Only for behavior nodes!
    }
    
    val newNodeId = UUID.randomUUID().toString()
    val newNode = GraphNode(
      id = newNodeId,
      data = NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setResponseNode(
            ProtoBrushBehavior.ResponseNode.newBuilder()
              .setPredefinedResponseCurve(ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
          )
          .build()
      ),
      position = (fromNode.position + toNode.position) / 2f
    )
    
    val edge1 = GraphEdge(fromPort = edge.fromPort, toPort = Port(newNodeId, PortSide.INPUT, 0))
    val edge2 = GraphEdge(fromPort = Port(newNodeId, PortSide.OUTPUT, 0), toPort = edge.toPort)
    
    val newEdges = graph.edges.filter { it != edge } + edge1 + edge2
    val newNodes = graph.nodes + newNode
    
    graph = graph.copy(nodes = newNodes, edges = newEdges)
    validate()
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
    // Identify edges that will be removed.
    val edgesToRemove = graph.edges.filter { it.fromPort.nodeId == nodeId || it.toPort.nodeId == nodeId }
    // Remove edges going into the node being deleted.
    graph = graph.copy(edges = graph.edges.filter { it.toPort.nodeId != nodeId })

    // Special handling for Brush Family ports. Delete edges one at a time by calling deleteEdge()
    // to ensure proper shifting logic is used.
    val edgesFromNode = edgesToRemove.filter { it.fromPort.nodeId == nodeId }
    // Sort edges by index descending to avoid shifting issues if one node is connected multiple
    // times.
    val sortedEdgesFrom = edgesFromNode.sortedByDescending { it.toPort.index }

    for (edge in sortedEdgesFrom) {
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
