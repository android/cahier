package com.example.cahier.developer.brushgraph.ui

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.TutorialStep
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
import com.example.cahier.developer.brushdesigner.data.CustomBrushEntity
import com.example.cahier.features.drawing.CustomBrushes
import com.example.cahier.developer.brushgraph.viewmodel.BrushGraphViewModel
import com.example.cahier.developer.brushgraph.data.INSPECTOR_HEIGHT_PORTRAIT
import com.example.cahier.developer.brushgraph.data.INSPECTOR_WIDTH_LANDSCAPE
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_COLLAPSED
import com.example.cahier.developer.brushgraph.data.PREVIEW_HEIGHT_EXPANDED

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
    val uriHandler = LocalUriHandler.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_select)) },
            onClick = onSelectMode
        )
        DropdownMenuItem(
            text = { Text(if (isTutorialSandboxMode) stringResource(R.string.bg_menu_exit_tutorial) else stringResource(R.string.bg_menu_tutorial)) },
            onClick = onTutorialAction
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_export)) },
            onClick = onExport
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_import)) },
            onClick = onImport
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_organize)) },
            onClick = onOrganize
        )
        Box {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.bg_templates)) },
                onClick = { onShowTemplatesMenuChange(true) },
                trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
            DropdownMenu(
                expanded = showTemplatesMenu,
                onDismissRequest = { onShowTemplatesMenuChange(false) },
                offset = DpOffset(x = 127.dp, y = (-56).dp)
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bg_pressure_pen)) },
                    onClick = {
                        onTemplateSelect(StockBrushes.pressurePen())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.marker)) },
                    onClick = {
                        onTemplateSelect(StockBrushes.marker())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.highlighter)) },
                    onClick = {
                        onTemplateSelect(StockBrushes.highlighter())
                        onShowTemplatesMenuChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dashed_line)) },
                    onClick = {
                        onTemplateSelect(StockBrushes.dashedLine())
                        onShowTemplatesMenuChange(false)
                    }
                )
                
                if (customBrushes.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.bg_menu_custom_brushes),
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
            text = { Text(stringResource(R.string.bg_delete_brush), color = MaterialTheme.colorScheme.error) },
            onClick = onDeleteBrush
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_options)) },
            onClick = onOptions
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.bg_menu_feedback)) },
            leadingIcon = { Icon(painterResource(R.drawable.outline_open_in_new_24), contentDescription = null) },
            onClick = {
                onDismiss()
                uriHandler.openUri("https://github.com/android/cahier/issues")
            }
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
                text = { Text(stringResource(R.string.bg_no_saved_brushes)) },
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
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bg_cd_delete))
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
fun CreateNodeSpeedDial(
  isLandscape: Boolean,
  isAnySidePaneOpen: Boolean,
  isPreviewExpanded: Boolean,
  viewportSize: androidx.compose.ui.geometry.Size,
  modifier: Modifier = Modifier,
  menuContent: @Composable (onClose: () -> Unit) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  val previewHeight = if (isPreviewExpanded) {
    PREVIEW_HEIGHT_EXPANDED
  } else {
    PREVIEW_HEIGHT_COLLAPSED
  }

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
            menuContent { expanded = false }
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
          contentDescription = stringResource(R.string.bg_cd_create_node),
        )
      }
    }
  }
}

@Composable
fun GraphActionMenu(
  onClose: () -> Unit,
  onExport: () -> Unit,
  onLoadBrushFile: () -> Unit,
  onSaveToPalette: () -> Unit,
  textureStore: TextureBitmapStore,
  onOrganize: () -> Unit,
  onDeleteBrush: () -> Unit,
  onTutorialExitRequested: () -> Unit,
  savedBrushes: List<CustomBrushEntity>,
  tutorialStep: TutorialStep?,
  isTutorialSandboxMode: Boolean,
  onEnterSelectionMode: () -> Unit,
  onLoadBrushFamily: (BrushFamily) -> Unit,
  onLoadFromPalette: (CustomBrushEntity) -> Unit,
  onDeleteFromPalette: (String) -> Unit,
  onStartTutorialSandbox: () -> Unit,
  textFieldsLocked: Boolean,
  onToggleTextFieldsLocked: () -> Unit,
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


  ClearGraphConfirmationDialog(
    show = showClearConfirmation,
    onDismiss = { showClearConfirmation = false },
    onConfirm = {
      onDeleteBrush()
      showClearConfirmation = false
    }
  )

  LaunchedEffect(tutorialStep) {
    if (isTutorialSandboxMode && tutorialStep == null) {
      onTutorialExitRequested()
    }
  }

  TutorialWarningDialog(
    show = showTutorialWarningDialog,
    onDismiss = { showTutorialWarningDialog = false },
    onConfirm = {
      onStartTutorialSandbox()
      showTutorialWarningDialog = false
    }
  )

  OptionsDialog(
    show = showOptionsDialog,
    onDismiss = { showOptionsDialog = false },
    textFieldsLocked = textFieldsLocked,
    onToggleTextFieldsLocked = onToggleTextFieldsLocked
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
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.bg_cd_exit))
      }

      VerticalDivider(
        modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )

      Box {
        IconButton(onClick = { showMoreMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.bg_cd_more_options))
        }

        MoreOptionsMenu(
          expanded = showMoreMenu,
          onDismiss = { showMoreMenu = false },
          isTutorialSandboxMode = isTutorialSandboxMode,
          onSelectMode = {
            onEnterSelectionMode()
            showTemplatesMenu = false
            showMoreMenu = false
          },
          onTutorialAction = {
            showMoreMenu = false
            if (isTutorialSandboxMode) {
              onTutorialExitRequested()
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
            onLoadBrushFamily(family)
            showTemplatesMenu = false
            showMoreMenu = false
          },
          customBrushes = CustomBrushes.getBrushes(context).map { it.name to it.brushFamily },
          onCustomBrushSelect = { family ->
            onLoadBrushFamily(family)
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
          Text(stringResource(R.string.bg_my_palette))
        }

        PaletteMenu(
          expanded = showPaletteMenu,
          onDismiss = { showPaletteMenu = false },
          savedBrushes = savedBrushes,
          onBrushSelect = { entity ->
            onLoadFromPalette(entity)
            showPaletteMenu = false
          },
          onBrushDelete = { entity ->
            onDeleteFromPalette(entity.name)
          }
        )
      }

      Spacer(Modifier.width(8.dp))

      Button(
        onClick = onSaveToPalette,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.height(40.dp),
      ) {
        Text(stringResource(R.string.bg_save_to_palette))
      }
    }
  }
}
