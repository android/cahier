@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.ui.node.NodeRegistry
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cahier.core.ui.ClassicColorPickerDialog
import com.example.cahier.developer.brushgraph.viewmodel.BrushGraphViewModel
import com.example.cahier.developer.brushgraph.ui.EdgeInspector
import com.example.cahier.developer.brushgraph.ui.NodeInspector
import com.example.cahier.developer.brushgraph.ui.AdaptiveInspectorPane
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphPoint
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.developer.brushgraph.data.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.developer.brushgraph.ui.getTooltip
import com.example.cahier.developer.brushgraph.data.inferNodeData
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_COLLAPSED
import kotlinx.coroutines.launch
import com.example.cahier.core.ui.theme.CahierAppTheme
import androidx.compose.material3.Scaffold
import androidx.compose.animation.core.VectorConverter
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.developer.brushgraph.data.TutorialAction
import com.example.cahier.core.ui.theme.extendedColorScheme
import com.example.cahier.core.ui.CahierTextureBitmapStore
import androidx.compose.ui.res.painterResource

/** The main UI for the Brush Graph studio. */
@Composable
fun BrushGraphScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  val viewModel: BrushGraphViewModel = hiltViewModel()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  
  // Use hoisted CahierTextureBitmapStore from ViewModel
  val textureStore = viewModel.textureStore
  
  val renderer = remember { CanvasStrokeRenderer.create(textureStore) }

  val primaryColor = MaterialTheme.colorScheme.primary
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  LaunchedEffect(primaryColor) {
    viewModel.updateTestBrushColor(primaryColor.toArgb())
  }

  var showColorPicker by remember { mutableStateOf(false) }
  var colorPickerInitialColor by remember { mutableStateOf(onSurfaceColor) }
  var colorPickerOnColorSelected by remember { mutableStateOf({ _: Color -> }) }

  if (showColorPicker) {
    ClassicColorPickerDialog(
      initialColor = colorPickerInitialColor,
      onColorSelected = colorPickerOnColorSelected,
      onDismissRequest = { showColorPicker = false }
    )
  }

  // Texture picking logic
  var showTextureNameDialog by remember { mutableStateOf(false) }
  var pendingTextureUri by remember { mutableStateOf<Uri?>(null) }
  var textureNameInput by remember { mutableStateOf("") }

  val texturePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      pendingTextureUri = uri
      showTextureNameDialog = true
    }
  }

  NameTextureDialog(
    show = showTextureNameDialog,
    onDismiss = { showTextureNameDialog = false },
    textureNameInput = textureNameInput,
    onTextureNameInputChange = { textureNameInput = it },
    onConfirm = {
      if (textureNameInput.isNotBlank() && pendingTextureUri != null) {
        val uri = pendingTextureUri!!
        val name = textureNameInput
        scope.launch {
          val bitmap = context.contentResolver.openInputStream(uri)?.use { 
              BitmapFactory.decodeStream(it)
          }
          if (bitmap != null) {
            textureStore.loadTexture(name, bitmap)
            viewModel.updateAllTextureIds()
          }
          showTextureNameDialog = false
          textureNameInput = ""
        }
      }
    }
  )

  val brushFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      scope.launch {
        try {
          val family = context.contentResolver.openInputStream(it)?.use { stream ->
            try {
              AndroidBrushFamilySerialization.decode(
                stream,
                BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
                  if (bitmap != null) {
                    textureStore.loadTexture(id, bitmap)
                    viewModel.updateAllTextureIds()
                  }
                  id
                }
              )
            } catch (e: Exception) {
              Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, trying legacy fallback")
              null
            }
          }

          if (family == null) {
            Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, and legacy fallback is disabled.")
            viewModel.postDebug("Failed to load brush: Legacy format not supported yet.")
          } else {
            viewModel.loadBrushFamily(family)
            viewModel.postDebug("Brush loaded successfully")
          }
        } catch (e: Exception) {
          android.util.Log.e("BrushGraphWidget", "Failed to load brush", e)
          viewModel.postDebug("Failed to load brush: ${e.message}")
        }
      }
    }
  }

  val brushExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri: Uri? ->
    uri?.let {
      scope.launch {
        try {
          context.contentResolver.openOutputStream(it)?.use { outputStream -> 
            AndroidBrushFamilySerialization.encode(
              viewModel.brush.value.family,
              outputStream,
              textureStore
            )
          }
          viewModel.postDebug("Brush exported successfully")
        } catch (e: Exception) {
          android.util.Log.e("BrushGraphWidget", "Failed to export brush", e)
          viewModel.postDebug("Failed to export brush: ${e.message}")
        }
      }
    }
  }

  // Save to palette logic
  var showSavePaletteDialog by remember { mutableStateOf(false) }
  var paletteBrushNameInput by remember { mutableStateOf("") }

  SaveToPaletteDialog(
    show = showSavePaletteDialog,
    onDismiss = { showSavePaletteDialog = false },
    paletteBrushNameInput = paletteBrushNameInput,
    onPaletteBrushNameInputChange = { paletteBrushNameInput = it },
    onConfirm = {
      if (paletteBrushNameInput.isNotBlank()) {
        viewModel.saveToPalette(paletteBrushNameInput, textureStore)
        showSavePaletteDialog = false
        paletteBrushNameInput = ""
      }
    }
  )

  CahierAppTheme {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      val isLandscape = maxWidth > maxHeight
      var viewportSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
      var showTutorialFinishDialog by remember { mutableStateOf(false) }

      val isSidePaneOpen = isLandscape && (viewModel.selectedNodeId != null || viewModel.isErrorPaneOpen)
      val indicatorPaddingEnd by animateDpAsState(
        targetValue = if (isSidePaneOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp,
        label = "indicatorPaddingEnd",
      )
      val previewHeight = if (viewModel.isPreviewExpanded) {
        PREVIEW_HEIGHT_EXPANDED
      } else {
        PREVIEW_HEIGHT_COLLAPSED
      }
      val isNodeSelected = viewModel.selectedNodeId != null
      val isEdgeSelected = viewModel.selectedEdge != null
      val isErrorPaneOpen = viewModel.isErrorPaneOpen
      val isAnySidePaneOpen = isNodeSelected || isEdgeSelected || isErrorPaneOpen

      val trashPaddingBottom by animateDpAsState(
        targetValue =
          if (!isLandscape && isAnySidePaneOpen) {
            (maxOf(previewHeight, INSPECTOR_HEIGHT_PORTRAIT) + 16).dp
          } else {
            (previewHeight + 16).dp
          },
        label = "trashPaddingBottom",
      )

      val nodeRegistry = remember { NodeRegistry() }
      val issues = viewModel.graphIssues.collectAsStateWithLifecycle().value

      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BrushGraphContent(
          isLandscape = isLandscape,
          isNodeSelected = isNodeSelected,
          isEdgeSelected = isEdgeSelected,
          isErrorPaneOpen = isErrorPaneOpen,
          isPreviewExpanded = viewModel.isPreviewExpanded,
          viewportSize = viewportSize,
          onViewportSizeChange = { viewportSize = it },
          canvasSlot = { padding ->
            GraphCanvas(
              graph = viewModel.graph,
              zoom = viewModel.zoom,
              offset = Offset(viewModel.offset.x, viewModel.offset.y),
              onZoomChange = { viewModel.updateZoom(it) },
              onOffsetChange = { viewModel.updateOffset(GraphPoint(it.x, it.y)) },
              onNodeMove = { id, pos -> viewModel.moveNode(id, GraphPoint(pos.x, pos.y)) },
              onNodeMoveFinished = { viewModel.advanceTutorial(TutorialAction.MOVE_NODE) },
              onNodeClick = { id, _ ->
                if (viewModel.isSelectionMode) {
                  viewModel.toggleNodeSelection(id)
                } else {
                  viewModel.onNodeClick(id)
                }
              },
              onNodeLongPress = { id -> viewModel.enterSelectionMode(id) },
              onNodeDelete = { id -> viewModel.deleteNode(id) },
              isSelectionMode = viewModel.isSelectionMode,
              selectedNodeIds = viewModel.selectedNodeIds,
              onSelectAll = { viewModel.selectAllNodes() },
              onDuplicateSelected = { viewModel.duplicateSelectedNodes() },
              onDeleteSelected = { viewModel.deleteSelectedNodes() },
              onDoneSelection = { viewModel.exitSelectionMode() },
              onAddEdge = { from, to, portId -> viewModel.addEdge(from, to, portId) },
              onEdgeClick = { viewModel.onEdgeClick(it) },
              onEdgeDelete = { viewModel.deleteEdge(it) },
              onEdgeDetach = { viewModel.detachEdge(it) },
              onFinalizeEdgeEdit = { oldEdge, fromId, toId, portId -> viewModel.finalizeEdgeEdit(oldEdge, fromId, toId, portId) },
              onCanvasClick = { viewModel.dismissPanes() },
              onPortClick = { nodeId, port ->
                val node = viewModel.graph.nodes.find { it.id == nodeId }
                val nodeData = node?.let { port.inferNodeData(it) }
                if (nodeData != null) {
                  val portPos = nodeRegistry.getPortPosition(nodeId, port.id, viewModel.graph)
                  val newX = node.position.x - nodeData.width() - 100f
                  val newY = portPos.y - nodeData.height() / 2f
                  viewModel.addNodeAndConnect(nodeData, GraphPoint(newX, newY), nodeId, port.id)
                }
              },
              onReorderPorts = { nodeId, fromIndex, toIndex -> viewModel.reorderPorts(nodeId, fromIndex, toIndex) },
              nodeRegistry = nodeRegistry,
              selectedEdge = viewModel.selectedEdge,
              detachedEdge = viewModel.detachedEdge,
              activeEdgeSourceId = viewModel.activeEdgeSourceId,
              onNodeDataUpdate = { id, data -> viewModel.updateNodeData(id, data) },
              onChooseColor = { initialColor, onColorSelected ->
                colorPickerInitialColor = initialColor
                colorPickerOnColorSelected = onColorSelected
                showColorPicker = true
              },
              textureStore = textureStore,
              allTextureIds = viewModel.allTextureIds,
              onLoadTexture = { texturePickerLauncher.launch(arrayOf("image/*")) },
              strokeRenderer = renderer,
              textFieldsLocked = viewModel.textFieldsLocked,
              selectedNodeId = viewModel.selectedNodeId,
              brush = viewModel.brush.collectAsStateWithLifecycle().value,
              bottomPadding = padding,
            )
          },
          inspectorSlot = {
            val selectedNode = viewModel.graph.nodes.find { it.id == viewModel.selectedNodeId }
            val selectedEdge = viewModel.selectedEdge
            val selectionName = if (selectedNode != null) {
                stringResource(selectedNode.data.title())
            } else {
                stringResource(R.string.bg_label_edge)
            }
            val titleText = stringResource(R.string.bg_title_inspector_with_name, selectionName)
            val selectionTooltip = selectedNode?.data?.getTooltip()?.let { stringResource(it) }

            AdaptiveInspectorPane(
              isLandscape = isLandscape,
              visible = selectedNode != null || selectedEdge != null,
              title = titleText,
              tooltipText = selectionTooltip,
              onClose = {
                viewModel.clearSelectedNode()
                viewModel.clearSelectedEdge()
              },
              modifier = Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter),
            ) {
                if (selectedNode != null) {
                  NodeInspector(
                    node = selectedNode,
                    onUpdate = { viewModel.updateNodeData(selectedNode.id, it) },
                    onDisableChange = { viewModel.setNodeDisabled(selectedNode.id, it) },
                    onChooseColor = { initialColor, onColorSelected ->
                      colorPickerInitialColor = initialColor
                      colorPickerOnColorSelected = onColorSelected
                      showColorPicker = true
                    },
                    allTextureIds = viewModel.allTextureIds,
                    onLoadTexture = { texturePickerLauncher.launch(arrayOf("image/*")) },
                    strokeRenderer = renderer,
                    textFieldsLocked = viewModel.textFieldsLocked,
                    onDelete = { viewModel.deleteNode(selectedNode.id) },
                    onFieldEditComplete = { viewModel.advanceTutorial(TutorialAction.EDIT_FIELD) },
                    onDropdownEditComplete = { viewModel.advanceTutorial(TutorialAction.EDIT_DROPDOWN) },
                  )
                } else if (selectedEdge != null) {
                  val fromNode = viewModel.graph.nodes.find { it.id == selectedEdge.fromNodeId }
                  val toNode = viewModel.graph.nodes.find { it.id == selectedEdge.toNodeId }
                  if (fromNode != null && toNode != null) {
                    val visiblePorts = toNode.getVisiblePorts(viewModel.graph)
                    val port = visiblePorts.find { it.id == selectedEdge.toPortId }
                    val inputLabel = port?.label
                    EdgeInspector(
                      edge = selectedEdge,
                      fromNode = fromNode,
                      toNode = toNode,
                      inputLabel = inputLabel,
                      onNodeFocus = { nodeId: String -> viewModel.centerNode(nodeId) },
                      onDisableChange = { viewModel.setEdgeDisabled(selectedEdge, it) },
                      onDelete = { viewModel.deleteEdge(selectedEdge) },
                      onAddNodeBetween = { viewModel.addNodeBetween(selectedEdge) },
                    )
                  }
                }
            }
          },
          notificationPaneSlot = {
            NotificationPane(
              isLandscape = isLandscape,
              viewModel = viewModel,
              modifier = Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter),
            )
          },
          notificationIconSlot = { padding ->
            NotificationIcon(
              issues = issues,
              indicatorPaddingEnd = padding,
              onToggleErrorPane = { viewModel.toggleErrorPane() },
              modifier = Modifier.align(Alignment.TopEnd)
            )
          },
          previewSlot = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
              CollapsiblePreviewPane(
                viewModel = viewModel,
                strokeRenderer = renderer,
                textureStore = textureStore,
                onChooseColor = { initialColor, onColorSelected ->
                  colorPickerInitialColor = initialColor
                  colorPickerOnColorSelected = onColorSelected
                  showColorPicker = true
                },
              )
            }
          },
          menuSlot = {
            GraphActionMenu(
              onClose = onNavigateUp,
              onExport = {
                brushExportLauncher.launch("brush_${System.currentTimeMillis()}.brushfamily")
              },
              onLoadBrushFile = { brushFilePickerLauncher.launch(arrayOf("*/*")) },
              onSaveToPalette = {
                paletteBrushNameInput = ""
                showSavePaletteDialog = true
              },
              textureStore = textureStore,
              onOrganize = viewModel::reorganize,
              onDeleteBrush = { viewModel.clearGraph() },
              onTutorialExitRequested = { showTutorialFinishDialog = true },
              savedBrushes = viewModel.savedPaletteBrushes.collectAsStateWithLifecycle().value,
              tutorialStep = viewModel.tutorialStep,
              isTutorialSandboxMode = viewModel.isTutorialSandboxMode,
              onEnterSelectionMode = { viewModel.enterSelectionMode(null) },
              onLoadBrushFamily = { viewModel.loadBrushFamily(it) },
              onLoadFromPalette = { viewModel.loadFromPalette(it, textureStore) },
              onDeleteFromPalette = { viewModel.deleteFromPalette(it) },
              onStartTutorialSandbox = { viewModel.startTutorialSandbox() },
              textFieldsLocked = viewModel.textFieldsLocked,
              onToggleTextFieldsLocked = { viewModel.toggleTextFieldsLocked() },
              modifier = Modifier.align(Alignment.TopStart).padding(16.dp).zIndex(2f),
            )
          },
          fabSlot = { vSize ->
            val density = LocalDensity.current.density
            val previewHeight = if (viewModel.isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
            val isInspectorOpen = (viewModel.selectedNodeId != null || viewModel.selectedEdge != null)
            val isErrorPaneOpen = viewModel.isErrorPaneOpen
            val isAnySidePaneOpen = isInspectorOpen || isErrorPaneOpen

            val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
            val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
            val previewHeightPx = previewHeight * density

            val (visibleWidth, visibleHeight) =
              if (isLandscape) {
                val w = if (isAnySidePaneOpen) vSize.width - inspectorWidthPx else vSize.width
                val h = vSize.height - previewHeightPx
                w to h
              } else {
                val w = vSize.width
                val h = if (isAnySidePaneOpen) vSize.height - maxOf(previewHeightPx, inspectorHeightPx) else vSize.height - previewHeightPx
                w to h
              }

            val visibleCenter = Offset(visibleWidth / 2f, visibleHeight / 2f)
            val centerInCanvasOffset = (visibleCenter - Offset(viewModel.offset.x, viewModel.offset.y)) / viewModel.zoom
            val centerInCanvas = GraphPoint(centerInCanvasOffset.x, centerInCanvasOffset.y)

            CreateNodeSpeedDial(
              isLandscape = isLandscape,
              isAnySidePaneOpen = isAnySidePaneOpen,
              isPreviewExpanded = viewModel.isPreviewExpanded,
              viewportSize = vSize,
              modifier = Modifier.align(Alignment.BottomEnd),
              menuContent = { onClose ->
                data class SpeedDialAction(
                  val labelRes: Int,
                  val icon: androidx.compose.ui.graphics.vector.ImageVector,
                  val onClick: () -> Unit
                )

                val actions = remember(centerInCanvas) {
                  listOf(
                    SpeedDialAction(R.string.bg_coat, Icons.Default.Layers) { viewModel.addCoatNode(centerInCanvas) },
                    SpeedDialAction(R.string.bg_paint, Icons.Default.Palette) { viewModel.addPaintNode(centerInCanvas) },
                    SpeedDialAction(R.string.bg_tip, Icons.Default.ShapeLine) { viewModel.addTipNode(centerInCanvas) },
                    SpeedDialAction(R.string.bg_behavior, Icons.Default.Psychology) { viewModel.addBehaviorNode(centerInCanvas) },
                    SpeedDialAction(R.string.bg_color_function, Icons.Default.Palette) { viewModel.addColorFunctionNode(centerInCanvas) },
                    SpeedDialAction(R.string.bg_texture_layer, Icons.Default.Layers) { viewModel.addTextureLayerNode(centerInCanvas) },
                  )
                }

                actions.forEach { action ->
                  DropdownMenuItem(
                    text = { Text(stringResource(action.labelRes)) },
                    leadingIcon = { Icon(action.icon, contentDescription = null) },
                    onClick = {
                      action.onClick()
                      onClose()
                    }
                  )
                }
              }
            )
          },
          tutorialSlot = { vSize ->
            TutorialOverlayHost(
              tutorialStep = viewModel.tutorialStep,
              graph = viewModel.graph,
              zoom = viewModel.zoom,
              offset = Offset(viewModel.offset.x, viewModel.offset.y),
              selectedNodeId = viewModel.selectedNodeId,
              selectedEdge = viewModel.selectedEdge,
              currentStepIndex = viewModel.currentStepIndex,
              isLandscape = isLandscape,
              viewportSize = vSize,
              isPreviewExpanded = viewModel.isPreviewExpanded,
              onAdvanceTutorial = { viewModel.advanceTutorial(it) },
              onRegressTutorial = { viewModel.regressTutorial() },
              onCloseTutorial = { showTutorialFinishDialog = true }
            )
          },
          dialogSlot = {
            TutorialFinishDialog(
              show = showTutorialFinishDialog,
              onDismiss = { showTutorialFinishDialog = false },
              onKeepChanges = {
                viewModel.endTutorialSandbox(keepChanges = true)
                showTutorialFinishDialog = false
              },
              onRestoreOriginal = {
                viewModel.endTutorialSandbox(keepChanges = false)
                showTutorialFinishDialog = false
              }
            )
          }
        )
      }

      GraphCameraController(
        offset = Offset(viewModel.offset.x, viewModel.offset.y),
        tutorialStep = viewModel.tutorialStep,
        focusTrigger = viewModel.focusTrigger,
        graph = viewModel.graph,
        zoom = viewModel.zoom,
        isPreviewExpanded = viewModel.isPreviewExpanded,
        selectedNodeId = viewModel.selectedNodeId,
        updateOffset = { viewModel.updateOffset(GraphPoint(it.x, it.y)) },
        viewportSize = viewportSize,
        context = context,
        isLandscape = isLandscape,
        maxWidthDp = maxWidth
      )
    }
  }
}
