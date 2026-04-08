@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.GraphValidationException
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED
import com.example.cahier.ui.brushgraph.model.PortSide
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import com.example.cahier.ui.brushgraph.model.toBrushFamily
import com.example.cahier.ui.theme.CahierAppTheme
import com.example.cahier.ui.theme.extendedColorScheme
import kotlinx.coroutines.launch

import com.example.cahier.ui.CahierTextureBitmapStore

/** The main UI for the Brush Graph studio. */
@Composable
fun BrushGraphWidget(
  onNavigateUp: () -> Unit,
) {
  val viewModel: BrushGraphViewModel = hiltViewModel()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  
  // Use existing CahierTextureBitmapStore
  val textureStore = remember { CahierTextureBitmapStore(context) }
  var allTextureIds by remember { mutableStateOf(textureStore.getAllIds()) }
  
  val renderer = remember { CanvasStrokeRenderer.create(textureStore) }

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

  if (showTextureNameDialog) {
    AlertDialog(
      onDismissRequest = { showTextureNameDialog = false },
      title = { Text("Name Texture") },
      text = {
        OutlinedTextField(
          value = textureNameInput,
          onValueChange = { textureNameInput = it },
          label = { Text("Texture ID") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (textureNameInput.isNotBlank() && pendingTextureUri != null) {
              val uri = pendingTextureUri!!
              val name = textureNameInput
              scope.launch {
                val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it)
                }
                if (bitmap != null) {
                  textureStore.loadTexture(name, bitmap)
                  allTextureIds = textureStore.getAllIds()
                }
              }
              showTextureNameDialog = false
              textureNameInput = ""
            }
          }
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTextureNameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  val brushFilePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let {
      scope.launch {
        try {
          val family = context.contentResolver.openInputStream(it)?.use { stream ->
            AndroidBrushFamilySerialization.decode(
              stream,
              BrushFamilyDecodeCallback { id: String, bitmap: Bitmap? ->
                if (bitmap != null) {
                  textureStore.loadTexture(id, bitmap)
                  allTextureIds = textureStore.getAllIds()
                }
                id
              }
            )
          } ?: throw Exception("Could not decode brush family")

          viewModel.loadBrushFamily(family)
          viewModel.postDebug("Brush loaded successfully")
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
              viewModel.brush.family,
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

  if (showSavePaletteDialog) {
    AlertDialog(
      onDismissRequest = { showSavePaletteDialog = false },
      title = { Text("Save to Cahier Palette") },
      text = {
        OutlinedTextField(
          value = paletteBrushNameInput,
          onValueChange = { paletteBrushNameInput = it },
          label = { Text("Brush Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (paletteBrushNameInput.isNotBlank()) {
              viewModel.saveToPalette(paletteBrushNameInput, textureStore)
              showSavePaletteDialog = false
              paletteBrushNameInput = ""
            }
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSavePaletteDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

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
        allTextureIds = allTextureIds,
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

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isLandscape = maxWidth > maxHeight

    Scaffold { paddingValues ->
      Box(
        modifier =
          Modifier.fillMaxSize().padding(paddingValues).onGloballyPositioned { coordinates ->
            viewModel.updateViewportSize(coordinates.size.toSize())
          }
      ) {
        val isSidePaneOpen =
          isLandscape && (viewModel.selectedNodeId != null || viewModel.isErrorPaneOpen)
        val indicatorPaddingEnd by
          animateDpAsState(
            targetValue = if (isSidePaneOpen) (INSPECTOR_WIDTH_LANDSCAPE + 16).dp else 16.dp,
            label = "indicatorPaddingEnd",
          )
        val previewHeight =
          if (viewModel.isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
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

        // Main node graph canvas as the background.
        NodeGraphCanvas(
          graph = viewModel.graph,
          zoom = viewModel.zoom,
          offset = viewModel.offset,
          onZoomChange = { viewModel.updateZoom(it) },
          onOffsetChange = { viewModel.updateOffset(it) },
          onNodeMove = { id, pos -> viewModel.moveNode(id, pos) },
          onNodeClick = { id, _ -> viewModel.onNodeClick(id) },
          onNodeDelete = { id -> viewModel.deleteNode(id) },
          onAddEdge = { from, to, index -> viewModel.addEdge(from, to, index) },
          onEdgeClick = { viewModel.onEdgeClick(it) },
          onEdgeDelete = { viewModel.deleteEdge(it) },
          selectedEdge = viewModel.selectedEdge,
          activeEdgeSourceId = viewModel.activeEdgeSourceId,
          onNodeDataUpdate = { id, data -> viewModel.updateNodeData(id, data) },
          onChooseColor = onChooseColor,
          textureStore = textureStore,
          allTextureIds = allTextureIds,
          onLoadTexture = onLoadTexture,
          strokeRenderer = strokeRenderer,
          textFieldsLocked = viewModel.textFieldsLocked,
          selectedNodeId = viewModel.selectedNodeId,
          brush = viewModel.brush,
          bottomPadding = trashPaddingBottom,
        )

        // Overlaying Inspector Pane appears on top of the graph.
        AdaptiveInspectorPane(
          isLandscape = isLandscape,
          viewModel = viewModel,
          onChooseColor = onChooseColor,
          textureStore = textureStore,
          allTextureIds = allTextureIds,
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
        val issues = viewModel.graphIssues
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
            Icon(icon, contentDescription = "Show Notifications")
          }
        }

        // Collapsible Preview Pane (always at bottom, spanning full width).
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
          CollapsiblePreviewPane(
            viewModel = viewModel,
            strokeRenderer = strokeRenderer,
            textureStore = textureStore,
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
          modifier = Modifier.align(Alignment.BottomEnd),
        )
      }
    }
  }
}

@Composable
fun CreateNodeFAB(
  viewModel: BrushGraphViewModel,
  isLandscape: Boolean,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  val previewHeight =
    if (viewModel.isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED
  val isInspectorOpen = (viewModel.selectedNodeId != null || viewModel.selectedEdge != null)
  val isErrorPaneOpen = viewModel.isErrorPaneOpen
  val isAnySidePaneOpen = isInspectorOpen || isErrorPaneOpen

  val fabPaddingBottom by
    animateDpAsState(
      targetValue =
        if (!isLandscape && isAnySidePaneOpen) {
          (maxOf(previewHeight, INSPECTOR_HEIGHT_PORTRAIT) + 16).dp
        } else {
          (previewHeight + 16).dp
        },
      label = "fabPaddingBottom",
    )

  val fabPaddingEnd by
    animateDpAsState(
      targetValue =
        if (isLandscape && isAnySidePaneOpen) {
          (INSPECTOR_WIDTH_LANDSCAPE + 16).dp
        } else {
          16.dp
        },
      label = "fabPaddingEnd",
    )

  val density = LocalDensity.current.density
  val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
  val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
  val previewHeightPx = previewHeight * density

  val (visibleWidth, visibleHeight) =
    if (isLandscape) {
      val w =
        if (isAnySidePaneOpen) {
          viewModel.viewportSize.width - inspectorWidthPx
        } else {
          viewModel.viewportSize.width
        }
      val h = viewModel.viewportSize.height - previewHeightPx
      w to h
    } else {
      val w = viewModel.viewportSize.width
      val h =
        if (isAnySidePaneOpen) {
          viewModel.viewportSize.height - maxOf(previewHeightPx, inspectorHeightPx)
        } else {
          viewModel.viewportSize.height - previewHeightPx
        }
      w to h
    }

  val visibleCenter = Offset(visibleWidth / 2f, visibleHeight / 2f)
  val centerInCanvas = (visibleCenter - viewModel.offset) / viewModel.zoom

  Box(modifier = modifier.padding(bottom = fabPaddingBottom, end = fabPaddingEnd).zIndex(2f)) {
    Column(horizontalAlignment = Alignment.End) {
      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Surface(
          modifier = Modifier.padding(bottom = 8.dp).width(180.dp),
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 4.dp,
          shadowElevation = 8.dp,
        ) {
          Column(modifier = Modifier.padding(vertical = 8.dp)) {
            DropdownMenuItem(
              text = { Text("Family") },
              leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
              onClick = {
                viewModel.addFamilyNode(centerInCanvas)
                expanded = false
              },
            )
            DropdownMenuItem(
              text = { Text("Coat") },
              leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
              onClick = {
                viewModel.addCoatNode(centerInCanvas)
                expanded = false
              },
            )
            DropdownMenuItem(
              text = { Text("Paint") },
              leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
              onClick = {
                viewModel.addPaintNode(centerInCanvas)
                expanded = false
              },
            )
            DropdownMenuItem(
              text = { Text("Tip") },
              leadingIcon = { Icon(Icons.Default.ShapeLine, contentDescription = null) },
              onClick = {
                viewModel.addTipNode(centerInCanvas)
                expanded = false
              },
            )
            DropdownMenuItem(
              text = { Text("Behavior") },
              leadingIcon = { Icon(Icons.Default.Psychology, contentDescription = null) },
              onClick = {
                viewModel.addBehaviorNode(centerInCanvas)
                expanded = false
              },
            )
          }
        }
      }

      androidx.compose.material3.FloatingActionButton(
        onClick = { expanded = !expanded },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(
          if (expanded) Icons.Default.Close else Icons.Default.Add,
          contentDescription = "Create Node",
        )
      }
    }
  }
}

@Composable
fun AdaptiveInspectorPane(
  isLandscape: Boolean,
  viewModel: BrushGraphViewModel,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  textureStore: TextureBitmapStore,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier,
) {
  val selectedNode = viewModel.graph.nodes.find { it.id == viewModel.selectedNodeId }
  val selectedEdge = viewModel.selectedEdge
  val density = androidx.compose.ui.platform.LocalDensity.current.density

  AnimatedVisibility(
    visible = selectedNode != null || selectedEdge != null,
    enter =
      if (isLandscape) {
        slideInHorizontally(initialOffsetX = { it })
      } else {
        slideInVertically(initialOffsetY = { it })
      },
    exit =
      if (isLandscape) {
        slideOutHorizontally(targetOffsetX = { it })
      } else {
        slideOutVertically(targetOffsetY = { it })
      },
    modifier = modifier.zIndex(10f),
  ) {
    if (selectedNode != null || selectedEdge != null) {
      Surface(
        modifier =
          if (isLandscape) {
            Modifier.fillMaxHeight().width(INSPECTOR_WIDTH_LANDSCAPE.dp)
          } else {
            Modifier.fillMaxWidth().height(INSPECTOR_HEIGHT_PORTRAIT.dp)
          },
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
      ) {
        Column {
          // Title bar with close button
          Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            ) {
              val selectionName =
                if (selectedNode != null) {
                  selectedNode.data.title()
                } else {
                  "Edge"
                }
              val titleText = "Inspector: ${selectionName}"
              Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
              )
              IconButton(
                onClick = {
                  viewModel.clearSelectedNode()
                  viewModel.clearSelectedEdge()
                }
              ) {
                Icon(Icons.Default.Close, contentDescription = "Close Inspector")
              }
            }
          }
          Box {
            if (selectedNode != null) {
              NodeInspector(
                node = selectedNode,
                onUpdate = { viewModel.updateNodeData(selectedNode.id, it) },
                onChooseColor = onChooseColor,
                allTextureIds = allTextureIds,
                onLoadTexture = onLoadTexture,
                strokeRenderer = strokeRenderer,
                textFieldsLocked = viewModel.textFieldsLocked,
                onDelete = { viewModel.deleteNode(selectedNode.id) },
              )
            } else if (selectedEdge != null) {
              val fromNode = viewModel.graph.nodes.find { it.id == selectedEdge.fromNodeId }
              val toNode = viewModel.graph.nodes.find { it.id == selectedEdge.toNodeId }
              if (fromNode != null && toNode != null) {
                EdgeInspector(
                  fromNode = fromNode,
                  toNode = toNode,
                  toInputIndex = selectedEdge.toInputIndex,
                  onNodeFocus = { nodeId: String ->
                    viewModel.centerNode(nodeId, isLandscape, density)
                  },
                  onDelete = { viewModel.deleteEdge(selectedEdge) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun CollapsiblePreviewPane(
  viewModel: BrushGraphViewModel,
  strokeRenderer: CanvasStrokeRenderer,
  textureStore: TextureBitmapStore,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Toggle Tab (always visible)
    Surface(
      modifier =
        Modifier.fillMaxWidth().height(40.dp).clickable { viewModel.togglePreviewExpanded() },
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 4.dp,
      shadowElevation = 8.dp,
    ) {
      Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          modifier = Modifier.align(Alignment.CenterStart),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            if (viewModel.isPreviewExpanded) {
              Icons.Default.KeyboardArrowDown
            } else {
              Icons.Default.KeyboardArrowUp
            },
            contentDescription = if (viewModel.isPreviewExpanded) "Collapse" else "Expand",
          )
          Spacer(Modifier.width(8.dp))
          Text(
            "Test canvas",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          if (viewModel.isPreviewExpanded) {
            Spacer(Modifier.width(16.dp))
            Text(
              "Reset",
              modifier = Modifier.clickable { viewModel.clearStrokes() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(16.dp))
            Text(
              "Invert canvas",
              modifier = Modifier.clickable { viewModel.toggleCanvasTheme() },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // Expanding Drawer Content
    AnimatedVisibility(
      visible = viewModel.isPreviewExpanded,
      enter = expandVertically(),
      exit = shrinkVertically(),
    ) {
      Surface(
        modifier =
          Modifier.fillMaxWidth().height((PREVIEW_HEIGHT_EXPANDED - PREVIEW_HEIGHT_COLLAPSED).dp),
        tonalElevation = 8.dp,
        color =
          if (viewModel.isDarkCanvas) {
            MaterialTheme.colorScheme.inverseSurface
          } else {
            MaterialTheme.colorScheme.surface
          },
      ) {
        CanvasSection(
          viewModel = viewModel,
          strokeList = viewModel.strokeList,
          strokeRenderer = strokeRenderer,
          textureStore = textureStore,
          brush = viewModel.brush,
          onStrokesAdded = { viewModel.strokeList.addAll(it) },
          isDark = viewModel.isDarkCanvas,
        )
      }
    }
  }
}

@Composable
fun CanvasSection(
  viewModel: BrushGraphViewModel,
  strokeList: List<androidx.ink.strokes.Stroke>,
  strokeRenderer: CanvasStrokeRenderer,
  textureStore: TextureBitmapStore,
  brush: androidx.ink.brush.Brush,
  onStrokesAdded: (List<androidx.ink.strokes.Stroke>) -> Unit,
  isDark: Boolean = false,
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(
      "Draw here to test",
      modifier = Modifier.align(Alignment.Center),
      style = MaterialTheme.typography.labelMedium,
      color =
        if (isDark) {
          MaterialTheme.colorScheme.inverseOnSurface
        } else {
          MaterialTheme.colorScheme.onSurface
        },
    )
    DrawingSurface(
      strokes = strokeList,
      canvasStrokeRenderer = strokeRenderer,
      textureStore = textureStore,
      onStrokesFinished = onStrokesAdded,
      onErase = { _, _ -> },
      onEraseStart = {},
      onEraseEnd = {},
      currentBrush = brush,
      onGetNextBrush = { viewModel.brush },
      isEraserMode = false,
      backgroundImageUri = null,
      onStartDrag = {},
    )
  }
}

@Composable
fun FloatingActionMenu(
  onClose: () -> Unit,
  onExport: () -> Unit,
  onLoadBrushFile: () -> Unit,
  onSaveToPalette: () -> Unit,
  viewModel: BrushGraphViewModel,
  textureStore: TextureBitmapStore,
  onOrganize: () -> Unit,
  onDeleteBrush: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showMoreMenu by remember { mutableStateOf(false) }
  var showPaletteMenu by remember { mutableStateOf(false) }
  var showClearConfirmation by remember { mutableStateOf(false) }
  var showReorganizeConfirmation by remember { mutableStateOf(false) }

  val savedBrushes by viewModel.savedPaletteBrushes.collectAsState()

  if (showClearConfirmation) {
    AlertDialog(
      onDismissRequest = { showClearConfirmation = false },
      title = { Text("Clear Graph") },
      text = {
        Text("Are you sure you want to clear the entire brush graph? This action cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteBrush()
            showClearConfirmation = false
          }
        ) {
          Text("Clear")
        }
      },
      dismissButton = { Button(onClick = { showClearConfirmation = false }) { Text("Cancel") } },
    )
  }

  if (showReorganizeConfirmation) {
    AlertDialog(
      onDismissRequest = { showReorganizeConfirmation = false },
      title = { Text("Reorganize Graph") },
      text = {
        Text(
          "Are you sure you want to reorganize the graph? This will reset all node positions and expansion states. This action cannot be undone."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onOrganize()
            showReorganizeConfirmation = false
          }
        ) {
          Text("Reorganize")
        }
      },
      dismissButton = {
        Button(onClick = { showReorganizeConfirmation = false }) { Text("Cancel") }
      },
    )
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(32.dp),
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    tonalElevation = 4.dp,
    shadowElevation = 8.dp,
  ) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onClose,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit")
      }

      VerticalDivider(
        modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )

      Box {
        IconButton(onClick = { showMoreMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }

        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
          DropdownMenuItem(
            text = { Text("Export") },
            onClick = {
              showMoreMenu = false
              onExport()
            },
          )
          DropdownMenuItem(
            text = { Text("Load") },
            onClick = {
              showMoreMenu = false
              onLoadBrushFile()
            },
          )
          DropdownMenuItem(
            text = { Text("Organize") },
            onClick = {
              showMoreMenu = false
              showReorganizeConfirmation = true
            },
          )
          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
          DropdownMenuItem(
            text = { Text("Delete Brush", color = MaterialTheme.colorScheme.error) },
            onClick = {
              showMoreMenu = false
              showClearConfirmation = true
            },
          )
        }
      }

      Box {
        TextButton(onClick = { showPaletteMenu = true }) {
          Text("My Palette")
        }

        DropdownMenu(expanded = showPaletteMenu, onDismissRequest = { showPaletteMenu = false }) {
          if (savedBrushes.isEmpty()) {
            DropdownMenuItem(
              text = { Text("No saved brushes yet") },
              onClick = { showPaletteMenu = false },
            )
          } else {
            savedBrushes.forEach { entity ->
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(entity.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                      viewModel.deleteFromPalette(entity.name)
                    }) {
                      Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                      )
                    }
                  }
                },
                onClick = {
                  viewModel.loadFromPalette(entity, textureStore)
                  showPaletteMenu = false
                }
              )
            }
          }
        }
      }

      Spacer(Modifier.width(8.dp))

      Button(
        onClick = onSaveToPalette,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(40.dp),
      ) {
        Text("Save to Palette")
      }
    }
  }
}

@Composable
fun NotificationPane(
  isLandscape: Boolean,
  viewModel: BrushGraphViewModel,
  modifier: Modifier = Modifier,
) {
  val issues = viewModel.graphIssues
  val hasErrors = issues.any { it.severity == ValidationSeverity.ERROR }
  val hasWarnings = issues.any { it.severity == ValidationSeverity.WARNING }

  AnimatedVisibility(
    visible = viewModel.isErrorPaneOpen,
    enter =
      if (isLandscape) {
        slideInHorizontally(initialOffsetX = { it })
      } else {
        slideInVertically(initialOffsetY = { it })
      },
    exit =
      if (isLandscape) {
        slideOutHorizontally(targetOffsetX = { it })
      } else {
        slideOutVertically(targetOffsetY = { it })
      },
    modifier = modifier.zIndex(10f),
  ) {
    Surface(
      modifier =
        if (isLandscape) {
          Modifier.fillMaxHeight().width(INSPECTOR_WIDTH_LANDSCAPE.dp)
        } else {
          Modifier.fillMaxWidth().height(INSPECTOR_HEIGHT_PORTRAIT.dp)
        },
      tonalElevation = 8.dp,
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface,
    ) {
      Column {
        // Title bar with close button
        val headerColor =
          when {
            hasErrors -> MaterialTheme.colorScheme.error
            hasWarnings -> MaterialTheme.extendedColorScheme.warning
            else -> MaterialTheme.colorScheme.primary
          }
        val iconColor =
          when {
            hasErrors -> MaterialTheme.colorScheme.onError
            hasWarnings -> MaterialTheme.extendedColorScheme.onWarning
            else -> MaterialTheme.colorScheme.onPrimary
          }
        val headerIcon =
          when {
            hasErrors -> Icons.Default.Error
            hasWarnings -> Icons.Default.Warning
            else -> Icons.Default.Info
          }

        Surface(color = headerColor, tonalElevation = 2.dp) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
          ) {
            Icon(headerIcon, contentDescription = null, tint = iconColor)
            Spacer(Modifier.width(8.dp))
            Text(
              text = "Notifications (${issues.size})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.weight(1f),
              color = iconColor,
            )
            IconButton(onClick = { viewModel.toggleErrorPane() }) {
              Icon(Icons.Default.Close, contentDescription = "Close Pane", tint = iconColor)
            }
          }
        }
        LazyColumn(modifier = Modifier.padding(16.dp)) {
          val errors = issues.filter { it.severity == ValidationSeverity.ERROR }
          val warnings = issues.filter { it.severity == ValidationSeverity.WARNING }
          val debugs = issues.filter { it.severity == ValidationSeverity.DEBUG }

          if (errors.isNotEmpty()) {
            item {
              NotificationGroup(
                title = "Errors",
                issues = errors,
                icon = Icons.Default.Error,
                color = MaterialTheme.colorScheme.error,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
          if (warnings.isNotEmpty()) {
            item {
              NotificationGroup(
                title = "Warnings",
                issues = warnings,
                icon = Icons.Default.Warning,
                color = MaterialTheme.extendedColorScheme.warning,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
          if (debugs.isNotEmpty()) {
            item {
              NotificationGroup(
                title = "Debug",
                issues = debugs,
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                viewModel = viewModel,
                isLandscape = isLandscape,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NotificationGroup(
  title: String,
  issues: List<GraphValidationException>,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  viewModel: BrushGraphViewModel,
  isLandscape: Boolean,
) {
  var expanded by remember { mutableStateOf(true) }
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
    Surface(
      modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      shape = RoundedCornerShape(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      ) {
        Icon(
          if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.ChevronRight,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
          text = "$title (${issues.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = color,
        )
      }
    }
    if (expanded) {
      Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
        for (issue in issues) {
          val density = androidx.compose.ui.platform.LocalDensity.current.density
          Surface(
            modifier =
              Modifier.fillMaxWidth().padding(vertical = 4.dp).let {
                if (issue.nodeId != null) {
                  it.clickable { viewModel.onIssueClick(issue, isLandscape, density) }
                } else {
                  it
                }
              },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(4.dp),
          ) {
            Text(
              text = issue.message,
              modifier = Modifier.padding(8.dp),
              style = MaterialTheme.typography.bodySmall,
              color = color,
            )
          }
        }
      }
    }
  }
}
