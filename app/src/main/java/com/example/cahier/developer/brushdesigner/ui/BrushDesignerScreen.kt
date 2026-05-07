/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushdesigner.ui

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.viewmodel.BrushDesignerViewModel

/**
 * Main entry point for the Brush Designer feature.
 * This is the ONLY stateful composable — it owns the ViewModel reference
 * and hoists all state/callbacks for child composables.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalInkCustomBrushApi::class
)
@Composable
fun BrushDesignerScreen(
    onNavigateUp: () -> Unit,
    viewModel: BrushDesignerViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current ?: return
    val windowSizeClass = calculateWindowSizeClass(activity)
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.saveBrushToFile(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.loadBrushFromFile(it) } }

    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val paneExpansionState = rememberPaneExpansionState()
    var hasSetInitialProportion by remember { mutableStateOf(false) }

    val savedBrushes by viewModel.savedPaletteBrushes.collectAsStateWithLifecycle()
    val activeProto by viewModel.activeBrushProto.collectAsStateWithLifecycle()
    val activeBrush by viewModel.activeBrush.collectAsStateWithLifecycle()
    val testStrokes by viewModel.testStrokes.collectAsStateWithLifecycle()
    val brushColor by viewModel.brushColor.collectAsStateWithLifecycle()
    val brushSize by viewModel.brushSize.collectAsStateWithLifecycle()

    LaunchedEffect(isCompact, hasSetInitialProportion) {
        if (!isCompact && !hasSetInitialProportion) {
            paneExpansionState.setFirstPaneProportion(0.35f)
            hasSetInitialProportion = true
        }
    }

    var showSavePaletteDialog by remember { mutableStateOf(false) }
    if (showSavePaletteDialog) {
        SaveToPaletteDialog(
            onSave = { name -> viewModel.saveToPalette(name) },
            onDismiss = { showSavePaletteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            BrushDesignerTopBar(
                isCompact = isCompact,
                onNavigateUp = onNavigateUp,
                savedBrushes = savedBrushes,
                onLoadBrush = { viewModel.loadStockBrush(it) },
                onLoadFromPalette = { viewModel.loadFromPalette(it) },
                onDeleteFromPalette = { viewModel.deleteFromPalette(it) },
                onClearCanvas = { viewModel.clearCanvas() },
                onShowSaveDialog = { showSavePaletteDialog = true },
                onImport = {
                    importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                },
                onExport = { exportLauncher.launch("custom_brush.brush") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCompact) {
                val bottomSheetState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.PartiallyExpanded
                    )
                )
                BottomSheetScaffold(
                    scaffoldState = bottomSheetState,
                    sheetPeekHeight = 200.dp,
                    sheetContent = {
                        ControlsPlaceholder(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                ) {
                    PreviewPane(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        activeBrush = activeBrush,
                        activeProto = activeProto,
                        strokes = testStrokes,
                        brushColor = brushColor,
                        brushSize = brushSize,
                        onSetTextureStore = { viewModel.setTextureStore(it) },
                        onReplaceStrokes = { viewModel.replaceStrokes(it) },
                        onStrokesFinished = { viewModel.onStrokesFinished(it) },
                        onGetNextBrush = { viewModel.getActiveBrush() ?: activeBrush!! },
                        onSetBrushColor = { viewModel.setBrushColor(it) },
                        onSetBrushSize = { viewModel.setBrushSize(it) }
                    )
                }
            } else {
                ListDetailPaneScaffold(
                    directive = navigator.scaffoldDirective,
                    value = navigator.scaffoldValue,
                    paneExpansionState = paneExpansionState,
                    paneExpansionDragHandle = { state ->
                        val interactionSource = remember { MutableInteractionSource() }
                        VerticalDragHandle(
                            modifier = Modifier.paneExpansionDraggable(
                                state,
                                LocalMinimumInteractiveComponentSize.current,
                                interactionSource,
                            ),
                        )
                    },
                    listPane = {
                        ControlsPlaceholder(
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    detailPane = {
                        PreviewPane(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            activeBrush = activeBrush,
                            activeProto = activeProto,
                            strokes = testStrokes,
                            brushColor = brushColor,
                            brushSize = brushSize,
                            onSetTextureStore = { viewModel.setTextureStore(it) },
                            onReplaceStrokes = { viewModel.replaceStrokes(it) },
                            onStrokesFinished = { viewModel.onStrokesFinished(it) },
                            onGetNextBrush = { viewModel.getActiveBrush() ?: activeBrush!! },
                            onSetBrushColor = { viewModel.setBrushColor(it) },
                            onSetBrushSize = { viewModel.setBrushSize(it) }
                        )
                    }
                )
            }
        }
    }
}

/**
 * Placeholder for the controls pane — will be replaced with the full
 * tabbed editor (Tip Shape / Paint / Behaviors) in a follow-up PR.
 */
@Composable
private fun ControlsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.brush_designer_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.brush_designer_controls_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
