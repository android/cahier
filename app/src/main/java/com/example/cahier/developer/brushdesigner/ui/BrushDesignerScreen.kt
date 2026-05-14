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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.brush.Brush
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.viewmodel.BrushDesignerViewModel
import ink.proto.BrushTip as ProtoBrushTip

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
    viewModel: BrushDesignerViewModel = hiltViewModel(),
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
    val selectedCoatIndex by viewModel.selectedCoatIndex.collectAsStateWithLifecycle()
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
                        ControlsPane(
                            modifier = Modifier.fillMaxWidth(),
                            activeProto = activeProto,
                            selectedCoatIndex = selectedCoatIndex,
                            viewModel = viewModel
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
                        onReplaceStrokes = { viewModel.replaceStrokes(it) },
                        onStrokesFinished = { viewModel.onStrokesFinished(it) },
                        onGetNextBrush = {
                            viewModel.getActiveBrush()
                                ?: activeBrush
                                ?: Brush.createWithColorIntArgb(
                                    StockBrushes.marker(),
                                    android.graphics.Color.BLACK,
                                    15f, 0.1f
                                )
                        },
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
                        ControlsPane(
                            modifier = Modifier.fillMaxSize(),
                            activeProto = activeProto,
                            selectedCoatIndex = selectedCoatIndex,
                            viewModel = viewModel
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
                            onReplaceStrokes = { viewModel.replaceStrokes(it) },
                            onStrokesFinished = { viewModel.onStrokesFinished(it) },
                            onGetNextBrush = {
                                viewModel.getActiveBrush()
                                    ?: activeBrush
                                    ?: Brush.createWithColorIntArgb(
                                        StockBrushes.marker(),
                                        android.graphics.Color.BLACK,
                                        15f, 0.1f
                                    )
                            },
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
 * The controls panel containing coat management, metadata fields,
 * input model selector, and tabbed content (Tip Shape / Paint / Behaviors).
 *
 * Note: Still accepts ViewModel for delegation to tab content and input model
 * sections. All state (activeProto, selectedCoatIndex) is hoisted from the screen.
 */
@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ControlsPane(
    modifier: Modifier = Modifier,
    activeProto: ink.proto.BrushFamily,
    selectedCoatIndex: Int,
    viewModel: BrushDesignerViewModel,
) {
    var textFieldsLocked by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(BrushDesignerTab.TipShape) }

    val currentTip = activeProto
        .coatsList.getOrNull(selectedCoatIndex)?.tip ?: ProtoBrushTip.getDefaultInstance()
    val inputModel = activeProto.inputModel

    var showTextureDialog by remember { mutableStateOf(false) }
    var pendingTextureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var textureIdInput by remember { mutableStateOf("") }

    val texturePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            pendingTextureUri = it
            showTextureDialog = true
        }
    }

    if (showTextureDialog) {
        TextureNameDialog(
            textureIdInput = textureIdInput,
            onTextureIdChange = { textureIdInput = it },
            onConfirm = {
                val uri = pendingTextureUri
                if (textureIdInput.isNotBlank() && uri != null) {
                    viewModel.addCustomTexture(uri, textureIdInput)
                    showTextureDialog = false
                    textureIdInput = ""
                }
            },
            onDismiss = { showTextureDialog = false }
        )
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CoatLayersSection(
            activeProto = activeProto,
            selectedCoatIndex = selectedCoatIndex,
            onSelectCoat = { viewModel.setSelectedCoat(it) },
            onAddCoat = { viewModel.addNewCoat() },
            onDeleteCoat = { viewModel.deleteSelectedCoat() }
        )

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        MetadataSection(
            developerComment = activeProto.developerComment,
            textFieldsLocked = textFieldsLocked,
            onToggleLock = { textFieldsLocked = it },
            onCommentChange = { viewModel.updateDeveloperComment(it) }
        )

        HorizontalDivider()

        InputModelSection(
            inputModel = inputModel,
            onUpdateInputModelToPassthrough = { viewModel.updateInputModelToPassthrough() },
            onUpdateSlidingWindowModel = {
                    ms,
                    hz,
                ->
                viewModel.updateSlidingWindowModel(ms, hz)
            }
        )

        HorizontalDivider()

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            BrushDesignerTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(stringResource(tab.labelResId)) }
                )
            }
        }

        when (selectedTab) {
            BrushDesignerTab.TipShape -> TipShapeTabContent(
                currentTip = currentTip,
                activeBrush = viewModel.getActiveBrush(),
                onUpdateTip = { block -> viewModel.updateTip(block) }
            )

            BrushDesignerTab.Paint -> PaintTabContent(
                activeProto = activeProto,
                selectedCoatIndex = selectedCoatIndex,
                onUpdatePaintPreferences = { viewModel.updatePaintPreferences(it) },
                onUpdateSelfOverlap = { viewModel.updateSelfOverlap(it) },
                texturePickerLauncher = texturePickerLauncher,
                getTextureBitmap = { viewModel.getTextureBitmap(it) }
            )

            BrushDesignerTab.Behaviors -> BehaviorsTabContent(
                activeProto = activeProto,
                selectedCoatIndex = selectedCoatIndex,
                onUpdateBehaviors = { viewModel.updateBehaviorsList(it) },
                onAddBehavior = { nodes -> viewModel.addBehavior(nodes) },
            )
        }
    }
}

@Composable
internal fun CoatLayersSection(
    activeProto: ink.proto.BrushFamily,
    selectedCoatIndex: Int,
    onSelectCoat: (Int) -> Unit,
    onAddCoat: () -> Unit,
    onDeleteCoat: () -> Unit,
) {
    Text(
        stringResource(R.string.brush_designer_brush_layers),
        style = MaterialTheme.typography.titleMedium
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        activeProto.coatsList.forEachIndexed { index, _ ->
            FilterChip(
                selected = selectedCoatIndex == index,
                onClick = { onSelectCoat(index) },
                label = { Text(stringResource(R.string.brush_designer_coat_label, index + 1)) }
            )
        }

        IconButton(onClick = onAddCoat) {
            Icon(
                painterResource(R.drawable.add_24px),
                contentDescription = stringResource(R.string.brush_designer_add_layer)
            )
        }

        if (activeProto.coatsList.size > 1) {
            IconButton(onClick = onDeleteCoat) {
                Icon(
                    painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.brush_designer_delete_layer),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun MetadataSection(
    developerComment: String,
    textFieldsLocked: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onCommentChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = textFieldsLocked,
            onCheckedChange = onToggleLock
        )
        Text(
            stringResource(R.string.brush_designer_lock_fields),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    OutlinedTextField(
        value = developerComment,
        onValueChange = onCommentChange,
        label = { Text(stringResource(R.string.brush_designer_developer_comment)) },
        enabled = !textFieldsLocked,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InputModelSection(
    inputModel: ink.proto.BrushFamily.InputModel,
    onUpdateInputModelToPassthrough: () -> Unit,
    onUpdateSlidingWindowModel: (Long, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(R.string.brush_designer_input_model),
        style = MaterialTheme.typography.titleMedium
    )

    var expandedModelMenu by remember { mutableStateOf(false) }
    val currentModelString = when {
        inputModel.hasPassthroughModel() ->
            stringResource(R.string.brush_designer_passthrough_model)

        inputModel.hasSlidingWindowModel() ->
            stringResource(R.string.brush_designer_sliding_window)

        else -> stringResource(R.string.brush_designer_sliding_window_default)
    }

    ExposedDropdownMenuBox(
        expanded = expandedModelMenu,
        onExpandedChange = { expandedModelMenu = it },
    ) {
        OutlinedTextField(
            value = currentModelString,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.brush_designer_model_type)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModelMenu)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expandedModelMenu,
            onDismissRequest = { expandedModelMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_passthrough_model)) },
                onClick = {
                    onUpdateInputModelToPassthrough()
                    expandedModelMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.brush_designer_sliding_window)) },
                onClick = {
                    onUpdateSlidingWindowModel(20L, 180)
                    expandedModelMenu = false
                }
            )
        }
    }

    if (inputModel.hasSlidingWindowModel() || !inputModel.hasPassthroughModel()) {
        SlidingWindowControls(
            inputModel = inputModel,
            onUpdateSlidingWindowModel = onUpdateSlidingWindowModel
        )
    }
}

@Composable
internal fun SlidingWindowControls(
    inputModel: ink.proto.BrushFamily.InputModel,
    onUpdateSlidingWindowModel: (Long, Int) -> Unit,
) {
    val swModel = inputModel.slidingWindowModel
    val windowMs =
        if (swModel.hasWindowSizeSeconds()) (swModel.windowSizeSeconds * 1000)
            .toLong() else 20L
    val upsamplingHz = if (swModel.hasExperimentalUpsamplingPeriodSeconds()) {
        val period = swModel.experimentalUpsamplingPeriodSeconds
        if (period == Float.POSITIVE_INFINITY || period == 0f) 0 else (1f / period).toInt()
    } else 180

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BrushSliderControl(
                label = stringResource(R.string.brush_designer_window_size_ms),
                value = windowMs.toFloat(),
                valueRange = 1f..100f,
                onValueChange = { newValue ->
                    onUpdateSlidingWindowModel(newValue.toLong(), upsamplingHz)
                }
            )
            BrushSliderControl(
                label = stringResource(R.string.brush_designer_upsampling_frequency_hz),
                value = upsamplingHz.toFloat(),
                valueRange = 0f..500f,
                onValueChange = { newValue ->
                    onUpdateSlidingWindowModel(windowMs, newValue.toInt())
                }
            )
        }
    }
}

@Composable
internal fun TextureNameDialog(
    textureIdInput: String,
    onTextureIdChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.brush_designer_name_texture_title)) },
        text = {
            OutlinedTextField(
                value = textureIdInput,
                onValueChange = onTextureIdChange,
                label = { Text(stringResource(R.string.brush_designer_texture_id_hint)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.brush_designer_load))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.brush_designer_cancel))
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
private fun BrushSliderControlPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            BrushSliderControl(
                label = stringResource(R.string.brush_designer_scale_x),
                value = 1.5f,
                valueRange = 0.1f..5f,
                onValueChange = {}
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 300)
@Composable
private fun TipShapeTabPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TipShapeTabContent(
                currentTip = ProtoBrushTip.getDefaultInstance(),
                activeBrush = null,
                onUpdateTip = {}
            )
        }
    }
}