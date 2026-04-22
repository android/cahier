@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import com.example.cahier.ui.brushdesigner.CustomBrushEntity
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import androidx.ink.brush.StockTextureBitmapStore
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cahier.ui.ClassicColorPickerDialog
import com.example.cahier.ui.DrawingSurface
import com.example.cahier.ui.brushgraph.BrushGraphViewModel
import com.example.cahier.ui.brushgraph.inspectors.EdgeInspector
import com.example.cahier.ui.brushgraph.inspectors.NodeInspector
import com.example.cahier.ui.brushgraph.inspectors.AdaptiveInspectorPane
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphPoint
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.getVisiblePorts
import com.example.cahier.ui.brushgraph.model.inferNodeData
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import com.example.cahier.ui.brushgraph.model.toBrushFamily
import com.example.cahier.ui.brushgraph.model.TutorialAction
import com.example.cahier.ui.theme.CahierAppTheme
import com.example.cahier.ui.theme.extendedColorScheme
import kotlinx.coroutines.launch

import com.example.cahier.ui.CahierTextureBitmapStore
import com.example.cahier.ui.CustomBrushes
import androidx.ink.brush.StockBrushes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset

/** The main UI for the Brush Graph studio. */
@Composable
fun BrushGraphWidget(
  onNavigateUp: () -> Unit,
) {
  val viewModel: BrushGraphViewModel = hiltViewModel()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  
  // Use hoisted CahierTextureBitmapStore from ViewModel
  val textureStore = viewModel.textureStore
  
  val renderer = remember { CanvasStrokeRenderer.create(textureStore) }

  val primaryColor = MaterialTheme.colorScheme.primary
  LaunchedEffect(primaryColor) {
    viewModel.updateTestBrushColor(primaryColor)
  }

  var showColorPicker by remember { mutableStateOf(false) }
  var colorPickerInitialColor by remember { mutableStateOf(Color.Black) }
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
              androidx.ink.storage.AndroidBrushFamilySerialization.decode(
                stream,
                androidx.ink.storage.BrushFamilyDecodeCallback { id: String, bitmap: android.graphics.Bitmap? ->
                  if (bitmap != null) {
                    textureStore.loadTexture(id, bitmap)
                    viewModel.updateAllTextureIds()
                  }
                  id
                }
              )
            } catch (e: Exception) {
              android.util.Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, trying legacy fallback")
              null
            }
          }

          if (family == null) {
            // TODO: Implement legacy brush loading fallback if needed.
            // Scrapped for now as we don't have a good proto to test with.
            android.util.Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, and legacy fallback is disabled.")
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      BrushGraphStudio(
        viewModel = viewModel,
        onLoadTexture = { texturePickerLauncher.launch(arrayOf("image/*")) },
        onLoadBrushFile = { brushFilePickerLauncher.launch(arrayOf("*/*")) },
        onChooseColor = { initialColor, onColorSelected ->
          colorPickerInitialColor = initialColor
          colorPickerOnColorSelected = onColorSelected
          showColorPicker = true
        },
        strokeRenderer = renderer,
        textureStore = textureStore,
        allTextureIds = viewModel.allTextureIds,
        onExport = {
          brushExportLauncher.launch("brush_${System.currentTimeMillis()}.brushfamily")
        },
        onSaveToPalette = {
          paletteBrushNameInput = ""
          showSavePaletteDialog = true
        },
        onNavigateUp = onNavigateUp,
      )
    }
  }
}

/** The main UI for the Brush Graph studio. */
@Composable
fun BrushGraphStudio(
  viewModel: BrushGraphViewModel,
  onLoadTexture: () -> Unit,
  onLoadBrushFile: () -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textureStore: TextureBitmapStore,
  allTextureIds: Set<String>,
  onExport: () -> Unit,
  onSaveToPalette: () -> Unit,
  onNavigateUp: () -> Unit,
) {
  val context = LocalContext.current
  var viewportSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isLandscape = maxWidth > maxHeight

    Scaffold { paddingValues ->
      Box(
        modifier =
          Modifier.fillMaxSize().padding(paddingValues).onGloballyPositioned { coordinates ->
            viewportSize = coordinates.size.toSize()
          }
      ) {
        val isSidePaneOpen =
          isLandscape && (viewModel.selectedNodeId != null || viewModel.isErrorPaneOpen)
        val indicatorPaddingEnd by
          animateDpAsState(
            targetValue = if (isSidePaneOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp,
            label = "indicatorPaddingEnd",
          )
        val previewHeight = if (viewModel.isPreviewExpanded) {
          PREVIEW_HEIGHT_EXPANDED
        } else {
          PREVIEW_HEIGHT_COLLAPSED
        }
        val isInspectorOpen = (viewModel.selectedNodeId != null || viewModel.selectedEdge != null)
        val isErrorPaneOpen = viewModel.isErrorPaneOpen
        val isAnySidePaneOpen = isInspectorOpen || isErrorPaneOpen

        val trashPaddingBottom by
          animateDpAsState(
            targetValue =
              if (!isLandscape && isAnySidePaneOpen) {
                (maxOf(previewHeight, INSPECTOR_HEIGHT_PORTRAIT) + 16).dp
              } else {
                (previewHeight + 16).dp
              },
            label = "trashPaddingBottom",
          )

        val nodeRegistry = remember { NodeRegistry() }

        // Main node graph canvas as the background.
        NodeGraphCanvas(
          graph = viewModel.graph,
          zoom = viewModel.zoom,
          offset = viewModel.offset,
          onZoomChange = { viewModel.updateZoom(it) },
          onOffsetChange = { viewModel.updateOffset(it) },
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
          onChooseColor = onChooseColor,
          textureStore = textureStore,
          allTextureIds = viewModel.allTextureIds,
          onLoadTexture = onLoadTexture,
          strokeRenderer = strokeRenderer,
          textFieldsLocked = viewModel.textFieldsLocked,
          selectedNodeId = viewModel.selectedNodeId,
          brush = viewModel.brush.collectAsState().value,
          bottomPadding = trashPaddingBottom,
        )

        // Overlaying Inspector Pane appears on top of the graph.
        AdaptiveInspectorPane(
          isLandscape = isLandscape,
          viewModel = viewModel,
          onChooseColor = onChooseColor,
          textureStore = textureStore,
          allTextureIds = viewModel.allTextureIds,
          onLoadTexture = onLoadTexture,
          strokeRenderer = strokeRenderer,
          modifier =
            Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter),
        )

        // Overlaying Notification Pane (including errors, warnings, debug)
        NotificationPane(
          isLandscape = isLandscape,
          viewModel = viewModel,
          modifier =
            Modifier.align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter),
        )

        // Floating Error/Warning/Debug Icon in the corner
        val issues = viewModel.graphIssues.collectAsState().value
        if (issues.isNotEmpty()) {
          val hasErrors = issues.any { it.severity == ValidationSeverity.ERROR }
          val hasWarnings = issues.any { it.severity == ValidationSeverity.WARNING }
          val icon =
            when {
              hasErrors -> Icons.Default.Error
              hasWarnings -> Icons.Default.Warning
              else -> Icons.Default.Info
            }
          val containerColor =
            when {
              hasErrors -> MaterialTheme.colorScheme.error
              hasWarnings -> MaterialTheme.extendedColorScheme.warning
              else -> MaterialTheme.colorScheme.secondary
            }
          val contentColor =
            when {
              hasErrors -> MaterialTheme.colorScheme.onError
              hasWarnings -> MaterialTheme.extendedColorScheme.onWarning
              else -> MaterialTheme.colorScheme.onSecondary
            }

          IconButton(
            onClick = { viewModel.toggleErrorPane() },
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(top = 16.dp, end = indicatorPaddingEnd)
                .zIndex(2f),
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
              ),
          ) {
            Icon(icon, contentDescription = stringResource(R.string.bg_cd_show_notifications))
          }
        }

        // Collapsible Preview Pane (always at bottom, spanning full width).
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          CollapsiblePreviewPane(
            viewModel = viewModel,
            strokeRenderer = strokeRenderer,
            textureStore = textureStore,
            onChooseColor = onChooseColor,
          )
        }

        // Floating Action Menu on the left.
        FloatingActionMenu(
          onClose = onNavigateUp,
          onExport = onExport,
          onLoadBrushFile = onLoadBrushFile,
          onSaveToPalette = onSaveToPalette,
          viewModel = viewModel,
          textureStore = textureStore,
          onOrganize = viewModel::reorganize,
          onDeleteBrush = { viewModel.clearGraph() },
          modifier = Modifier.align(Alignment.TopStart).padding(16.dp).zIndex(2f),
        )

        // Floating Action Button in the lower right.
        CreateNodeFAB(
          viewModel = viewModel,
          isLandscape = isLandscape,
          viewportSize = viewportSize,
          modifier = Modifier.align(Alignment.BottomEnd),
        )

        val currentMaxWidth = this@BoxWithConstraints.maxWidth

        val animatableOffset = remember { Animatable(viewModel.offset, androidx.compose.ui.geometry.Offset.VectorConverter) }

        // Auto-pan to node in tutorial
        LaunchedEffect(viewModel.tutorialStep) {
          val step = viewModel.tutorialStep
          if (step != null && step.anchor == com.example.cahier.ui.brushgraph.model.TutorialAnchor.NODE_CANVAS) {
            val node = step.getTargetNode(viewModel.graph)
            if (node != null) {
              val density = context.resources.displayMetrics.density
              val targetY = 280f * density
              val targetX = this@BoxWithConstraints.maxWidth.value * density / 2f
              
              val newOffset = calculateFocusOffset(
                node = node,
                zoom = viewModel.zoom,
                targetScreenPos = Offset(targetX, targetY)
              )
              
              animatableOffset.snapTo(viewModel.offset)
              animatableOffset.animateTo(newOffset, animationSpec = tween(500)) {
                viewModel.updateOffset(value)
              }
            }
          }
        }

        // Listen for ViewModel events (e.g. center on node)
        LaunchedEffect(viewModel.focusTrigger) {
          if (viewModel.focusTrigger > 0) {
            viewModel.selectedNodeId?.let { nodeId ->
              val node = viewModel.graph.nodes.find { it.id == nodeId }
              if (node != null) {
                val density = context.resources.displayMetrics.density
                val isLandscape = this@BoxWithConstraints.maxWidth > this@BoxWithConstraints.maxHeight
                val newOffset = calculateFocusOffset(
                  node = node,
                  zoom = viewModel.zoom,
                  viewportSize = viewportSize,
                  density = density,
                  isLandscape = isLandscape,
                  isPreviewExpanded = viewModel.isPreviewExpanded
                )
                animatableOffset.snapTo(viewModel.offset)
                animatableOffset.animateTo(newOffset, animationSpec = tween(500)) {
                  viewModel.updateOffset(value)
                }
              }
            }
          }
        }

        // Tutorial Overlay
        viewModel.tutorialStep?.let { step ->
          val density = androidx.compose.ui.platform.LocalDensity.current
          val isInspectorOpen = (viewModel.selectedNodeId != null || viewModel.selectedEdge != null)
          var overlaySize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
          
          val tutorialModifier = when (step.anchor) {
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.SCREEN_CENTER -> Modifier.align(Alignment.Center)
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.FAB -> {
              if (isInspectorOpen) {
                if (isLandscape) {
                  Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = (INSPECTOR_WIDTH_LANDSCAPE + 80).dp)
                } else {
                  Modifier.align(Alignment.BottomEnd).padding(bottom = (INSPECTOR_HEIGHT_PORTRAIT + 16).dp, end = 80.dp)
                }
              } else {
                Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 80.dp)
              }
            }
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.NODE_CANVAS -> {
              val node = step.getTargetNode(viewModel.graph)
              if (node != null) {
                val nodeCenterX = node.position.x + node.data.width() / 2f
                val nodeTopY = node.position.y
                
                val screenX = nodeCenterX * viewModel.zoom + viewModel.offset.x
                val screenY = nodeTopY * viewModel.zoom + viewModel.offset.y
                
                val paddingPx = with(density) { 16.dp.toPx() }
                Modifier.offset { 
                  androidx.compose.ui.unit.IntOffset(
                    (screenX - overlaySize.width / 2).toInt(),
                    (screenY - overlaySize.height - paddingPx).toInt()
                  )
                }
              } else {
                Modifier.align(Alignment.Center)
              }
            }
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.INSPECTOR -> {
              if (isLandscape) {
                Modifier.align(Alignment.CenterEnd).padding(end = (INSPECTOR_WIDTH_LANDSCAPE + 16).dp)
              } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = (INSPECTOR_HEIGHT_PORTRAIT + 16).dp)
              }
            }
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.TEST_CANVAS -> {
              val basePadding = if (viewModel.isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
              if (isInspectorOpen && !isLandscape) {
                Modifier.align(Alignment.BottomCenter).padding(bottom = (maxOf(INSPECTOR_HEIGHT_PORTRAIT, basePadding) + 16).dp)
              } else {
                Modifier.align(Alignment.BottomCenter).padding(bottom = (basePadding + 16).dp)
              }
            }
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.ACTION_BAR -> Modifier.align(Alignment.TopStart).padding(top = 80.dp, start = 16.dp)
            
            com.example.cahier.ui.brushgraph.model.TutorialAnchor.NOTIFICATION_ICON -> {
              if (isInspectorOpen && isLandscape) {
                Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = (INSPECTOR_WIDTH_LANDSCAPE + 16).dp)
              } else {
                Modifier.align(Alignment.TopEnd).padding(top = 80.dp, end = 16.dp)
              }
            }
          }.zIndex(20f)

          TutorialOverlay(
            step = step,
            onNext = { viewModel.advanceTutorial(step.actionRequired) },
            onBack = if (viewModel.currentStepIndex > 0) { { viewModel.regressTutorial() } } else null,
            modifier = tutorialModifier.onGloballyPositioned { coordinates ->
              overlaySize = coordinates.size
            }
          )
        }
      }
    }
  }
}

/**
 * Calculates the target offset to center a node in the viewport.
 */
private fun calculateFocusOffset(
  node: GraphNode,
  zoom: Float,
  viewportSize: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size.Zero,
  density: Float = 1f,
  isLandscape: Boolean = false,
  isPreviewExpanded: Boolean = false,
  targetScreenPos: androidx.compose.ui.geometry.Offset? = null
): androidx.compose.ui.geometry.Offset {
  val nodeCenterX = node.position.x + node.data.width() / 2f
  val nodeCenterY = node.position.y + node.data.height() / 2f

  val targetPos = if (targetScreenPos != null) {
    Pair(targetScreenPos.x, targetScreenPos.y)
  } else {
    val previewHeightPx = (if (isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED) * density
    val safeSize = if (isLandscape) {
      val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
      Pair(viewportSize.width - inspectorWidthPx, viewportSize.height - previewHeightPx)
    } else {
      val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
      Pair(viewportSize.width, viewportSize.height - maxOf(inspectorHeightPx, previewHeightPx))
    }
    Pair(safeSize.first / 2f, safeSize.second / 2f)
  }
  
  val targetX = targetPos.first
  val targetY = targetPos.second

  return androidx.compose.ui.geometry.Offset(targetX - nodeCenterX * zoom, targetY - nodeCenterY * zoom)
}
