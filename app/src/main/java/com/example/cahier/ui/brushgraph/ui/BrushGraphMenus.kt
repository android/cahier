package com.example.cahier.ui.brushgraph.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.ui.brushdesigner.CustomBrushEntity
import com.example.cahier.ui.CustomBrushes
import com.example.cahier.ui.brushgraph.BrushGraphViewModel
import com.example.cahier.ui.brushgraph.model.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.ui.brushgraph.model.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.ui.brushgraph.model.PREVIEW_HEIGHT_EXPANDED

@Composable
fun MoreOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isTutorialSandboxMode: Boolean,
    onSelectMode: () -> Unit,
    onTutorialAction: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOrganize: () -> Unit,
    showTemplatesMenu: Boolean,
    onShowTemplatesMenuChange: (Boolean) -> Unit,
    onTemplateSelect: (BrushFamily) -> Unit,
    customBrushes: List<Pair<String, BrushFamily>>,
    onCustomBrushSelect: (BrushFamily) -> Unit,
    onDeleteBrush: () -> Unit,
    onOptions: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Select") },
            onClick = onSelectMode
        )
        DropdownMenuItem(
            text = { Text(if (isTutorialSandboxMode) "Exit Tutorial" else "Tutorial") },
            onClick = onTutorialAction
        )
        DropdownMenuItem(
            text = { Text("Export") },
            onClick = onExport
        )
        DropdownMenuItem(
            text = { Text("Import") },
            onClick = onImport
        )
        DropdownMenuItem(
            text = { Text("Organize") },
            onClick = onOrganize
        )
        Box {
            DropdownMenuItem(
                text = { Text("Templates") },
                onClick = { onShowTemplatesMenuChange(true) },
                trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
            DropdownMenu(
                expanded = showTemplatesMenu,
                onDismissRequest = { onShowTemplatesMenuChange(false) },
                offset = DpOffset(x = 127.dp, y = (-56).dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Pressure Pen") },
                    onClick = {
                        onTemplateSelect(StockBrushes.pressurePen())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Marker") },
                    onClick = {
                        onTemplateSelect(StockBrushes.marker())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Highlighter") },
                    onClick = {
                        onTemplateSelect(StockBrushes.highlighter())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Dashed Line") },
                    onClick = {
                        onTemplateSelect(StockBrushes.dashedLine())
                        onShowTemplatesMenuChange(false)
                    }
                )
                
                if (customBrushes.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Custom Brushes",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    customBrushes.forEach { (name, family) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onCustomBrushSelect(family)
                                onShowTemplatesMenuChange(false)
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        DropdownMenuItem(
            text = { Text("Delete Brush", color = MaterialTheme.colorScheme.error) },
            onClick = onDeleteBrush
        )
        DropdownMenuItem(
            text = { Text("Options") },
            onClick = onOptions
        )
    }
}

@Composable
fun PaletteMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    savedBrushes: List<CustomBrushEntity>,
    onBrushSelect: (CustomBrushEntity) -> Unit,
    onBrushDelete: (CustomBrushEntity) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (savedBrushes.isEmpty()) {
            DropdownMenuItem(
                text = { Text("No saved brushes yet") },
                onClick = onDismiss,
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
                            IconButton(onClick = { onBrushDelete(entity) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    onClick = { onBrushSelect(entity) }
                )
            }
        }
    }
}

@Composable
fun CreateNodeFAB(
  viewModel: BrushGraphViewModel,
  isLandscape: Boolean,
  viewportSize: Size,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }

  val previewHeight = if (viewModel.isPreviewExpanded) {
    PREVIEW_HEIGHT_EXPANDED
  } else {
    PREVIEW_HEIGHT_COLLAPSED
  }
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
          viewportSize.width - inspectorWidthPx
        } else {
          viewportSize.width
        }
      val h = viewportSize.height - previewHeightPx
      w to h
    } else {
      val w = viewportSize.width
      val h =
        if (isAnySidePaneOpen) {
          viewportSize.height - maxOf(previewHeightPx, inspectorHeightPx)
        } else {
          viewportSize.height - previewHeightPx
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
            DropdownMenuItem(
              text = { Text("Color Function") },
              leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
              onClick = {
                viewModel.addColorFunctionNode(centerInCanvas)
                expanded = false
              },
            )
            DropdownMenuItem(
              text = { Text("Texture Layer") },
              leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
              onClick = {
                viewModel.addTextureLayerNode(centerInCanvas)
                expanded = false
              },
            )
          }
        }
      }

      FloatingActionButton(
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
  val context = LocalContext.current
  var showMoreMenu by remember { mutableStateOf(false) }
  var showPaletteMenu by remember { mutableStateOf(false) }
  var showClearConfirmation by remember { mutableStateOf(false) }
  var showReorganizeConfirmation by remember { mutableStateOf(false) }
  var showTemplatesMenu by remember { mutableStateOf(false) }
  var showOptionsDialog by remember { mutableStateOf(false) }
  var showTutorialWarningDialog by remember { mutableStateOf(false) }
  var showTutorialFinishDialog by remember { mutableStateOf(false) }

  val savedBrushes by viewModel.savedPaletteBrushes.collectAsState()

  ClearGraphConfirmationDialog(
    show = showClearConfirmation,
    onDismiss = { showClearConfirmation = false },
    onConfirm = {
      onDeleteBrush()
      showClearConfirmation = false
    }
  )

  LaunchedEffect(viewModel.tutorialStep) {
    if (viewModel.isTutorialSandboxMode && viewModel.tutorialStep == null) {
      showTutorialFinishDialog = true
    }
  }

  TutorialWarningDialog(
    show = showTutorialWarningDialog,
    onDismiss = { showTutorialWarningDialog = false },
    onConfirm = {
      viewModel.startTutorialSandbox()
      showTutorialWarningDialog = false
    }
  )

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

  OptionsDialog(
    show = showOptionsDialog,
    onDismiss = { showOptionsDialog = false },
    textFieldsLocked = viewModel.textFieldsLocked,
    onToggleTextFieldsLocked = { viewModel.toggleTextFieldsLocked() }
  )

  ReorganizeConfirmationDialog(
    show = showReorganizeConfirmation,
    onDismiss = { showReorganizeConfirmation = false },
    onConfirm = {
      onOrganize()
      showReorganizeConfirmation = false
    }
  )

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

        MoreOptionsMenu(
          expanded = showMoreMenu,
          onDismiss = { showMoreMenu = false },
          isTutorialSandboxMode = viewModel.isTutorialSandboxMode,
          onSelectMode = {
            viewModel.enterSelectionMode(null)
            showTemplatesMenu = false
            showMoreMenu = false
          },
          onTutorialAction = {
            showMoreMenu = false
            if (viewModel.isTutorialSandboxMode) {
              showTutorialFinishDialog = true
            } else {
              showTutorialWarningDialog = true
            }
          },
          onExport = {
            showMoreMenu = false
            onExport()
          },
          onImport = {
            showMoreMenu = false
            onLoadBrushFile()
          },
          onOrganize = {
            showMoreMenu = false
            showReorganizeConfirmation = true
          },
          showTemplatesMenu = showTemplatesMenu,
          onShowTemplatesMenuChange = { showTemplatesMenu = it },
          onTemplateSelect = { family ->
            viewModel.loadBrushFamily(family)
            showTemplatesMenu = false
            showMoreMenu = false
          },
          customBrushes = CustomBrushes.getBrushes(context).map { it.name to it.brushFamily },
          onCustomBrushSelect = { family ->
            viewModel.loadBrushFamily(family)
            showTemplatesMenu = false
            showMoreMenu = false
          },
          onDeleteBrush = {
            showMoreMenu = false
            showClearConfirmation = true
          },
          onOptions = {
            showMoreMenu = false
            showOptionsDialog = true
          }
        )
      }

      Box {
        TextButton(onClick = { showPaletteMenu = true }) {
          Text("My Palette")
        }

        PaletteMenu(
          expanded = showPaletteMenu,
          onDismiss = { showPaletteMenu = false },
          savedBrushes = savedBrushes,
          onBrushSelect = { entity ->
            viewModel.loadFromPalette(entity, textureStore)
            showPaletteMenu = false
          },
          onBrushDelete = { entity ->
            viewModel.deleteFromPalette(entity.name)
          }
        )
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
