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
@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class, androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.example.cahier.developer.brushgraph.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.core.layout.WindowHeightSizeClass
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.ui.node.NodeRegistry
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cahier.developer.brushdesigner.ui.CustomColorPickerDialog
import com.example.cahier.developer.brushgraph.viewmodel.BrushGraphViewModel
import com.example.cahier.developer.brushgraph.ui.EdgeInspector
import com.example.cahier.developer.brushgraph.ui.NodeInspector
import com.example.cahier.developer.brushgraph.ui.AdaptiveInspectorPane
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphPoint
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.developer.brushgraph.ui.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.developer.brushgraph.ui.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.developer.brushgraph.ui.getTooltip
import com.example.cahier.developer.brushgraph.data.inferNodeData
import com.example.cahier.developer.brushgraph.ui.PREVIEW_HEIGHT_COLLAPSED
import kotlinx.coroutines.launch
import com.example.cahier.core.ui.theme.CahierAppTheme
import com.example.cahier.developer.brushgraph.ui.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.developer.brushgraph.data.TutorialAction
import com.example.cahier.core.ui.CahierTextureBitmapStore
import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.core.ui.LocalTextureStore

/** The main UI for the Brush Graph studio. */
@Composable
fun BrushGraphScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  val viewModel: BrushGraphViewModel = hiltViewModel()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val isWideScreen = windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT
  val isTallAndWide = isWideScreen && windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.EXPANDED
  
  val textureStore = LocalTextureStore.current
  val renderer = remember { CanvasStrokeRenderer.create(textureStore) }

  val primaryColor = MaterialTheme.colorScheme.primary
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface
  LaunchedEffect(primaryColor) {
    viewModel.updateTestBrushColor(primaryColor)
  }

  var showColorPicker by remember { mutableStateOf(false) }
  var colorPickerInitialColor by remember { mutableStateOf(onSurfaceColor) }
  var colorPickerOnColorSelected by remember { mutableStateOf({ _: Color -> }) }

  if (showColorPicker) {
    CustomColorPickerDialog(
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
          val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { 
              BitmapFactory.decodeStream(it)
            }
          }
          if (bitmap != null) {
            viewModel.loadTexture(name, bitmap)
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
          val family = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            context.contentResolver.openInputStream(it)?.use { stream ->
              try {
                AndroidBrushFamilySerialization.decode(
                  stream,
                  BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
                    if (bitmap != null) {
                      viewModel.loadTexture(id, bitmap)
                    }
                    id
                  }
                )
              } catch (e: Exception) {
                Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, trying legacy fallback")
                null
              }
            }
          }

          if (family == null) {
            Log.d("BrushGraphWidget", "Failed to decode with AndroidBrushFamilySerialization, and legacy fallback is disabled.")
            viewModel.postDebug(DisplayText.Resource(R.string.bg_err_load_brush_legacy_unsupported))
          } else {
            viewModel.loadBrushFamily(family)
            viewModel.postDebug(DisplayText.Resource(R.string.bg_msg_brush_loaded_success))
          }
        } catch (e: Exception) {
          android.util.Log.e("BrushGraphWidget", "Failed to load brush", e)
          viewModel.postDebug(DisplayText.Resource(R.string.bg_err_load_brush_failed_with_msg, listOf(e.message ?: "")))
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
          viewModel.postDebug(DisplayText.Resource(R.string.bg_msg_brush_exported_success))
        } catch (e: Exception) {
          android.util.Log.e("BrushGraphWidget", "Failed to export brush", e)
          viewModel.postDebug(DisplayText.Resource(R.string.bg_err_export_brush_failed_with_msg, listOf(e.message ?: "")))
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
        viewModel.saveToPalette(paletteBrushNameInput)
        showSavePaletteDialog = false
        paletteBrushNameInput = ""
      }
    }
  )

  CahierAppTheme {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
      var viewportSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
      var showTutorialFinishDialog by remember { mutableStateOf(false) }

      val isSidePaneOpen = isWideScreen && (uiState.selectedNodeId != null || uiState.isErrorPaneOpen)
      val indicatorPaddingEnd by animateDpAsState(
        targetValue = if (isSidePaneOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp,
        label = "indicatorPaddingEnd",
      )
      val previewHeight = if (uiState.isPreviewExpanded) {
        PREVIEW_HEIGHT_EXPANDED
      } else {
        PREVIEW_HEIGHT_COLLAPSED
      }
      val animatedPreviewHeight by animateDpAsState(
        targetValue = previewHeight.dp,
        label = "animatedPreviewHeight"
      )
      val isNodeSelected = uiState.selectedNodeId != null
      val isEdgeSelected = uiState.selectedEdge != null
      val isErrorPaneOpen = uiState.isErrorPaneOpen
      val isAnySidePaneOpen = isNodeSelected || isEdgeSelected || isErrorPaneOpen

      val trashPaddingBottom by animateDpAsState(
        targetValue =
          if (!isWideScreen && isAnySidePaneOpen) {
            (maxOf(previewHeight, INSPECTOR_HEIGHT_PORTRAIT) + 16).dp
          } else {
            (previewHeight + 16).dp
          },
        label = "trashPaddingBottom",
      )

      val nodeRegistry = remember { NodeRegistry() }
      val issues = uiState.graphIssues

      LaunchedEffect(uiState.graph) {
        val missingNodes = uiState.graph.nodes.filter { nodeRegistry.getNodePosition(it.id) == null }
        if (missingNodes.isNotEmpty()) {
          val layout = GraphLayout.calculateLayout(uiState.graph)
          layout.forEach { (id, pos) ->
            if (nodeRegistry.getNodePosition(id) == null) {
              nodeRegistry.updateNodePosition(id, pos)
            }
          }
        }
      }

      Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BrushGraphContent(
          isNodeSelected = isNodeSelected,
          isEdgeSelected = isEdgeSelected,
          isErrorPaneOpen = isErrorPaneOpen,
          isPreviewExpanded = uiState.isPreviewExpanded,
          viewportSize = viewportSize,
          onViewportSizeChange = { viewportSize = it },
          canvasSlot = { padding ->
            GraphCanvas(
              graph = uiState.graph,
              zoom = uiState.zoom,
              offset = Offset(uiState.offset.x, uiState.offset.y),
              onZoomChange = { viewModel.updateZoom(it) },
              onOffsetChange = { viewModel.updateOffset(GraphPoint(it.x, it.y)) },
              onNodeMoveFinished = { viewModel.advanceTutorial(TutorialAction.MOVE_NODE) },
              onNodeClick = { id, _ ->
                if (uiState.isSelectionMode) {
                  viewModel.toggleNodeSelection(id)
                } else {
                  viewModel.onNodeClick(id)
                }
              },
              onNodeLongPress = { id -> viewModel.enterSelectionMode(id) },
              onNodeDelete = { id -> viewModel.deleteNode(id) },
              isSelectionMode = uiState.isSelectionMode,
              selectedNodeIds = uiState.selectedNodeIds,
              onSelectAll = { viewModel.selectAllNodes() },
              onDuplicateSelected = {
                val idMap = viewModel.duplicateSelectedNodes()
                idMap.forEach { (oldId, newId) ->
                  val oldPos = nodeRegistry.getNodePosition(oldId)
                  if (oldPos != null) {
                    nodeRegistry.updateNodePosition(newId, oldPos + Offset(50f, 50f))
                  }
                }
              },
              onDeleteSelected = { viewModel.deleteSelectedNodes() },
              onDoneSelection = { viewModel.exitSelectionMode() },
              onAddEdge = { from, to, portId -> viewModel.addEdge(from, to, portId) },
              onEdgeClick = { viewModel.onEdgeClick(it) },
              onEdgeDelete = { viewModel.deleteEdge(it) },
              onEdgeDetach = { viewModel.detachEdge(it) },
              onFinalizeEdgeEdit = { oldEdge, fromId, toId, portId -> viewModel.finalizeEdgeEdit(oldEdge, fromId, toId, portId) },
              onCanvasClick = { viewModel.dismissPanes() },
              onPortClick = { nodeId, port ->
                val node = uiState.graph.nodes.find { it.id == nodeId }
                val nodeData = node?.let { port.inferNodeData(it) }
                if (nodeData != null) {
                  val portPos = nodeRegistry.getPortPosition(nodeId, port.id, uiState.graph)
                  val nodePos = nodeRegistry.getNodePosition(nodeId) ?: Offset.Zero
                  val newX = nodePos.x - nodeData.width() - 100f
                  val newY = portPos.y - nodeData.height() / 2f
                  val newNodeId = viewModel.addNodeAndConnect(nodeData, nodeId, port.id)
                  nodeRegistry.updateNodePosition(newNodeId, Offset(newX, newY))
                }
              },
              onReorderPorts = { nodeId, fromIndex, toIndex -> viewModel.reorderPorts(nodeId, fromIndex, toIndex) },
              nodeRegistry = nodeRegistry,
              selectedEdge = uiState.selectedEdge,
              detachedEdge = uiState.detachedEdge,
              activeEdgeSourceId = uiState.activeEdgeSourceId,
              onNodeDataUpdate = { id, data -> viewModel.updateNodeData(id, data) },
              onChooseColor = { initialColor, onColorSelected ->
                colorPickerInitialColor = initialColor
                colorPickerOnColorSelected = onColorSelected
                showColorPicker = true
              },
              allTextureIds = uiState.allTextureIds,
              onLoadTexture = { texturePickerLauncher.launch(arrayOf("image/*")) },
              strokeRenderer = renderer,
              textFieldsLocked = uiState.textFieldsLocked,
              selectedNodeId = uiState.selectedNodeId,
              brush = viewModel.brush.collectAsStateWithLifecycle().value,
              bottomPadding = padding,
              rightPadding = if (isSidePaneOpen) INSPECTOR_WIDTH_LANDSCAPE.dp else 0.dp,
            )
          },
          inspectorSlot = {
            val selectedNode = uiState.graph.nodes.find { it.id == uiState.selectedNodeId }
            val selectedEdge = uiState.selectedEdge
            val selectionName = if (selectedNode != null) {
                stringResource(selectedNode.data.title())
            } else {
                stringResource(R.string.bg_label_edge)
            }
            val titleText = stringResource(R.string.bg_title_inspector_with_name, selectionName)
            val selectionTooltip = selectedNode?.data?.getTooltip()?.let { stringResource(it) }

            AdaptiveInspectorPane(
              isWideScreen = isWideScreen,
              visible = selectedNode != null || selectedEdge != null,
              title = titleText,
              tooltipText = selectionTooltip,
              onClose = {
                viewModel.clearSelectedNode()
                viewModel.clearSelectedEdge()
              },
              modifier = Modifier
                .align(if (isWideScreen) Alignment.CenterEnd else Alignment.BottomCenter)
                .let {
                  if (isTallAndWide) {
                    it.padding(bottom = animatedPreviewHeight)
                  } else {
                    it
                  }
                },
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
                    allTextureIds = uiState.allTextureIds,
                    onLoadTexture = { texturePickerLauncher.launch(arrayOf("image/*")) },
                    strokeRenderer = renderer,
                    textFieldsLocked = uiState.textFieldsLocked,
                    onDelete = { viewModel.deleteNode(selectedNode.id) },
                    onFieldEditComplete = { viewModel.advanceTutorial(TutorialAction.EDIT_FIELD) },
                    onDropdownEditComplete = { viewModel.advanceTutorial(TutorialAction.EDIT_DROPDOWN) },
                  )
                } else if (selectedEdge != null) {
                  val fromNode = uiState.graph.nodes.find { it.id == selectedEdge.fromNodeId }
                  val toNode = uiState.graph.nodes.find { it.id == selectedEdge.toNodeId }
                  if (fromNode != null && toNode != null) {
                    val visiblePorts = toNode.getVisiblePorts(uiState.graph)
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
                      onAddNodeBetween = {
                        val fromNodePos = nodeRegistry.getNodePosition(selectedEdge.fromNodeId) ?: Offset.Zero
                        val toNodePos = nodeRegistry.getNodePosition(selectedEdge.toNodeId) ?: Offset.Zero
                        val midpoint = (fromNodePos + toNodePos) / 2f
                        val newNodeId = viewModel.addNodeBetween(selectedEdge)
                        if (newNodeId != null) {
                          nodeRegistry.updateNodePosition(newNodeId, midpoint)
                        }
                      },
                    )
                  }
                }
            }
          },
          notificationPaneSlot = {
            NotificationPane(
              isWideScreen = isWideScreen,
              viewModel = viewModel,
              modifier = Modifier
                .align(if (isWideScreen) Alignment.CenterEnd else Alignment.BottomCenter)
                .let {
                  if (isTallAndWide) {
                    it.padding(bottom = animatedPreviewHeight)
                  } else {
                    it
                  }
                },
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
              val topIssue = remember(issues) {
                  issues.firstOrNull { it.severity == ValidationSeverity.ERROR } ?:
                  issues.firstOrNull { it.severity == ValidationSeverity.WARNING }
              }
              CollapsiblePreviewPane(
                  isPreviewExpanded = uiState.isPreviewExpanded,
                  isInvertedCanvas = uiState.isDarkCanvas,
                  testAutoUpdateStrokes = uiState.testAutoUpdateStrokes,
                  brushColor = uiState.testBrushColor ?: primaryColor,
                  brushSize = uiState.testBrushSize,
                  brush = viewModel.brush.collectAsStateWithLifecycle().value,
                  strokeList = viewModel.strokeList,
                  strokeRenderer = renderer,
                  topIssue = topIssue,
                  onGetNextBrush = { viewModel.brush.value },
                  onTogglePreviewExpanded = { viewModel.togglePreviewExpanded() },
                  onClearStrokes = { viewModel.clearStrokes() },
                  onToggleCanvasTheme = { viewModel.toggleCanvasTheme() },
                  onSetTestAutoUpdateStrokes = { viewModel.setTestAutoUpdateStrokes(it) },
                  onUpdateTestBrushColor = { viewModel.updateTestBrushColor(it) },
                  onUpdateTestBrushSize = { viewModel.updateTestBrushSize(it) },
                  onStrokesAdded = { strokes ->
                      viewModel.strokeList.addAll(strokes)
                      viewModel.advanceTutorial(TutorialAction.DRAW_ON_CANVAS)
                  },
                  onChooseColor = { initialColor, onColorSelected ->
                      colorPickerInitialColor = initialColor
                      colorPickerOnColorSelected = onColorSelected
                      showColorPicker = true
                  },
                  onToggleNotificationPane = { viewModel.toggleErrorPane() }
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
              onOrganize = viewModel::reorganize,
              onDeleteBrush = { viewModel.clearGraph() },
              onTutorialExitRequested = { showTutorialFinishDialog = true },
              savedBrushes = viewModel.savedPaletteBrushes.collectAsStateWithLifecycle().value,
              tutorialStep = viewModel.tutorialStep,
              isTutorialSandboxMode = viewModel.isTutorialSandboxMode,
              onEnterSelectionMode = { viewModel.enterSelectionMode(null) },
              onLoadBrushFamily = { viewModel.loadBrushFamily(it) },
              onLoadFromPalette = { viewModel.loadFromPalette(it) },
              onDeleteFromPalette = { viewModel.deleteFromPalette(it) },
              onStartTutorialSandbox = { viewModel.startTutorialSandbox() },
              textFieldsLocked = uiState.textFieldsLocked,
              onToggleTextFieldsLocked = { viewModel.toggleTextFieldsLocked() },
              modifier = Modifier.align(Alignment.TopStart).padding(16.dp).zIndex(2f),
            )
          },
          fabSlot = { vSize ->
            val density = LocalDensity.current.density
            val previewHeight = if (uiState.isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
            val isInspectorOpen = (uiState.selectedNodeId != null || uiState.selectedEdge != null)
            val isErrorPaneOpen = uiState.isErrorPaneOpen
            val isAnySidePaneOpen = isInspectorOpen || isErrorPaneOpen

            val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
            val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
            val previewHeightPx = previewHeight * density

            val (visibleWidth, visibleHeight) =
              if (isWideScreen) {
                val w = if (isAnySidePaneOpen) vSize.width - inspectorWidthPx else vSize.width
                val h = vSize.height - previewHeightPx
                w to h
              } else {
                val w = vSize.width
                val h = if (isAnySidePaneOpen) vSize.height - maxOf(previewHeightPx, inspectorHeightPx) else vSize.height - previewHeightPx
                w to h
              }

            val visibleCenter = Offset(visibleWidth / 2f, visibleHeight / 2f)
            val centerInCanvas = (visibleCenter - Offset(uiState.offset.x, uiState.offset.y)) / uiState.zoom

            CreateNodeSpeedDial(
              isWideScreen = isWideScreen,
              isAnySidePaneOpen = isAnySidePaneOpen,
              isPreviewExpanded = uiState.isPreviewExpanded,
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
                    SpeedDialAction(R.string.bg_coat, Icons.Default.Layers) { 
                      val id = viewModel.addCoatNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
                    SpeedDialAction(R.string.bg_paint, Icons.Default.Palette) { 
                      val id = viewModel.addPaintNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
                    SpeedDialAction(R.string.bg_tip, Icons.Default.ShapeLine) { 
                      val id = viewModel.addTipNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
                    SpeedDialAction(R.string.bg_behavior, Icons.Default.Psychology) { 
                      val id = viewModel.addBehaviorNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
                    SpeedDialAction(R.string.bg_color_function, Icons.Default.Palette) { 
                      val id = viewModel.addColorFunctionNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
                    SpeedDialAction(R.string.bg_texture_layer, Icons.Default.Layers) { 
                      val id = viewModel.addTextureLayerNode()
                      nodeRegistry.updateNodePosition(id, centerInCanvas)
                    },
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
              graph = uiState.graph,
              zoom = uiState.zoom,
              offset = Offset(uiState.offset.x, uiState.offset.y),
              selectedNodeId = uiState.selectedNodeId,
              selectedEdge = uiState.selectedEdge,
              currentStepIndex = viewModel.currentStepIndex,
              isWideScreen = isWideScreen,
              viewportSize = vSize,
              isPreviewExpanded = uiState.isPreviewExpanded,
              onAdvanceTutorial = { viewModel.advanceTutorial(it) },
              onRegressTutorial = { viewModel.regressTutorial() },
              onCloseTutorial = { showTutorialFinishDialog = true },
              nodeRegistry = nodeRegistry
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
        offset = Offset(uiState.offset.x, uiState.offset.y),
        tutorialStep = viewModel.tutorialStep,
        focusTrigger = uiState.focusTrigger,
        graph = uiState.graph,
        zoom = uiState.zoom,
        isPreviewExpanded = uiState.isPreviewExpanded,
        selectedNodeId = uiState.selectedNodeId,
        updateOffset = { viewModel.updateOffset(GraphPoint(it.x, it.y)) },
        viewportSize = viewportSize,
        context = context,
        isWideScreen = isWideScreen,
        maxWidthDp = maxWidth,
        nodeRegistry = nodeRegistry
      )
    }
  }
}
