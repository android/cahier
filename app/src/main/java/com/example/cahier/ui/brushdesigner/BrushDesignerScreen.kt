package com.example.cahier.ui.brushdesigner

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.cahier.R
import com.example.cahier.ui.DrawingSurface
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalMaterial3AdaptiveApi::class
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

    var stockBrushMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isCompact, hasSetInitialProportion) {
        if (!isCompact && !hasSetInitialProportion) {
            paneExpansionState.setFirstPaneProportion(0.25f)
            hasSetInitialProportion = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Brush Designer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painterResource(R.drawable.arrow_back_24px),
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box {
                        TextButton(onClick = { stockBrushMenuExpanded = true }) {
                            Text("Stock Brushes")
                        }
                        DropdownMenu(
                            expanded = stockBrushMenuExpanded,
                            onDismissRequest = { stockBrushMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Highlighter") },
                                onClick = {
                                    viewModel.loadStockBrush(StockBrushes.highlighter());
                                    stockBrushMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Marker") },
                                onClick = {
                                    viewModel.loadStockBrush(StockBrushes.marker());
                                    stockBrushMenuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pressure Pen") },
                                onClick = {
                                    viewModel.loadStockBrush(StockBrushes.pressurePen());
                                    stockBrushMenuExpanded = false
                                }
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.clearCanvas() }) { Text("Clear") }
                    TextButton(onClick = {
                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    }) { Text("Import") }
                    TextButton(onClick = {
                        exportLauncher.launch("custom_brush.brush")
                    }) { Text("Export") }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCompact) {
                Column(modifier = Modifier.fillMaxSize()) {
                    PreviewPane(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        viewModel = viewModel
                    )
                    ControlsPane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        viewModel = viewModel
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
                            viewModel = viewModel
                        )
                    },
                    detailPane = {
                        PreviewPane(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            viewModel = viewModel
                        )
                    }
                )
            }
        }
    }
}

@OptIn(
    ExperimentalInkCustomBrushApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun ControlsPane(
    modifier: Modifier = Modifier,
    viewModel: BrushDesignerViewModel
) {
    val activeProto by viewModel.activeBrushProto.collectAsState()

    var textFieldsLocked by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentTip = activeProto.coatsList.firstOrNull()?.tip
        ?: ProtoBrushTip.getDefaultInstance()
    val inputModel = activeProto.inputModel

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = textFieldsLocked,
                onCheckedChange = { textFieldsLocked = it }
            )
            Text("Lock text fields", style = MaterialTheme.typography.bodyMedium)
        }

        OutlinedTextField(
            value = activeProto.clientBrushFamilyId,
            onValueChange = { viewModel.updateClientBrushFamilyId(it) },
            label = { Text("Client brush family ID") },
            enabled = !textFieldsLocked,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = activeProto.developerComment,
            onValueChange = { viewModel.updateDeveloperComment(it) },
            label = { Text("Developer comment") },
            enabled = !textFieldsLocked,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        HorizontalDivider()

        Text("Input Model", style = MaterialTheme.typography.titleMedium)

        var expandedModelMenu by remember { mutableStateOf(false) }
        val currentModelString = when {
            inputModel.hasSpringModel() -> "Spring Model"
            inputModel.hasExperimentalNaiveModel() -> "Experimental Naive Model"
            inputModel.hasSlidingWindowModel() -> "Sliding Window Model"
            else -> "Sliding Window Model (Default)"
        }

        ExposedDropdownMenuBox(
            expanded = expandedModelMenu,
            onExpandedChange = { expandedModelMenu = it },
        ) {
            OutlinedTextField(
                value = currentModelString,
                onValueChange = {},
                readOnly = true,
                label = { Text("Model Type") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expandedModelMenu
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu (
                expanded = expandedModelMenu,
                onDismissRequest = { expandedModelMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Spring Model") },
                    onClick = {
                        viewModel.updateInputModelToSpring()
                        expandedModelMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Experimental Naive Model") },
                    onClick = {
                        viewModel.updateInputModelToNaive()
                        expandedModelMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sliding Window Model") },
                    onClick = {
                        viewModel.updateSlidingWindowModel(20L, 180)
                        expandedModelMenu = false
                    }
                )
            }

        }


        if (inputModel.hasSlidingWindowModel() || (!inputModel.hasSpringModel()
                    && !inputModel.hasExperimentalNaiveModel())
        ) {
            val swModel = inputModel.slidingWindowModel
            val windowMs =
                if (swModel.hasWindowSizeSeconds()) (swModel.windowSizeSeconds * 1000).toLong() else 20L
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
                        label = "Window size (ms)",
                        value = windowMs.toFloat(),
                        valueRange = 1f..100f,
                        onValueChange = { newValue ->
                            viewModel.updateSlidingWindowModel(
                                newValue.toLong(), upsamplingHz)
                        }
                    )
                    BrushSliderControl(
                        label = "Upsampling frequency (Hz)",
                        value = upsamplingHz.toFloat(),
                        valueRange = 0f..500f,
                        onValueChange = { newValue ->
                            viewModel.updateSlidingWindowModel(
                                windowMs, newValue.toInt())
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Tip Shape") })
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Paint & Color") })
        }

        if (selectedTab == 0) {
            Text(
                "Tip Geometry",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            BrushSliderControl(
                label = "Scale X",
                value = if (currentTip.hasScaleX()) currentTip.scaleX else 1f,
                valueRange = 0.1f..5f,
                onValueChange = { newValue -> viewModel.updateTip { it.setScaleX(newValue) } }
            )

            BrushSliderControl(
                label = "Scale Y",
                value = if (currentTip.hasScaleY()) currentTip.scaleY else 1f,
                valueRange = 0.1f..5f,
                onValueChange = { newValue -> viewModel.updateTip { it.setScaleY(newValue) } }
            )

            BrushSliderControl(
                label = "Corner Rounding",
                value = if (currentTip.hasCornerRounding()) currentTip.cornerRounding else 1f,
                valueRange = 0f..1f,
                onValueChange = { newValue ->
                    viewModel.updateTip {
                        it.setCornerRounding(newValue)
                    }
                }
            )

            BrushSliderControl(
                label = "Slant (Radians)",
                value = if (currentTip.hasSlantRadians()) currentTip.slantRadians else 0f,
                valueRange = -1.57f..1.57f, // -PI/2 to PI/2
                onValueChange = { newValue -> viewModel.updateTip { it.setSlantRadians(newValue) } }
            )

            BrushSliderControl(
                label = "Pinch",
                value = if (currentTip.hasPinch()) currentTip.pinch else 0f,
                valueRange = 0f..1f,
                onValueChange = { newValue -> viewModel.updateTip { it.setPinch(newValue) } }
            )

        } else {
            val currentPaint =
                activeProto.coatsList.firstOrNull()?.paintPreferencesList?.firstOrNull()
                    ?: ProtoBrushPaint.getDefaultInstance()

            Text(
                "Paint Preferences",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            var overlapExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = overlapExpanded,
                onExpandedChange = { overlapExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentPaint.selfOverlap.name.replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Self Overlap") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = overlapExpanded
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = overlapExpanded,
                    onDismissRequest = { overlapExpanded = false }) {
                    ProtoBrushPaint.SelfOverlap.entries
                        .filter { it != ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_UNSPECIFIED }
                        .forEach { overlap ->
                            DropdownMenuItem(
                                text = { Text(overlap.name) },
                                onClick = {
                                    viewModel.updateSelfOverlap(overlap); overlapExpanded = false
                                }
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Color Functions", style = MaterialTheme.typography.titleMedium)

            val currentOpacity =
                currentPaint.colorFunctionsList.firstOrNull()?.opacityMultiplier ?: 1f
            BrushSliderControl(
                label = "Opacity Multiplier",
                value = currentOpacity,
                valueRange = 0f..2f,
                onValueChange = { viewModel.updateOpacityMultiplier(it) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun BrushSliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = String.format("%.2f", value), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@OptIn(ExperimentalInkCustomBrushApi::class)
@Composable
private fun PreviewPane(
    modifier: Modifier = Modifier,
    viewModel: BrushDesignerViewModel
) {
    val activeBrush by viewModel.activeBrush.collectAsState()

    val strokesState by viewModel.testStrokes.collectAsState()
    val brushColor by viewModel.brushColor.collectAsState()
    val brushSize by viewModel.brushSize.collectAsState()
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val localStrokes = remember { mutableStateListOf<Stroke>() }
    var showCustomColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(strokesState) {
        if (localStrokes != strokesState) {
            localStrokes.clear()
            localStrokes.addAll(strokesState)
        }
    }

    LaunchedEffect(activeBrush) {
        if (activeBrush != null && localStrokes.isNotEmpty()) {
            val updatedStrokes = localStrokes.map { it.copy(brush = activeBrush!!) }
            localStrokes.clear()
            localStrokes.addAll(updatedStrokes)
            viewModel.replaceStrokes(updatedStrokes)
        }
    }

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                2.dp, MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {

        if (showCustomColorPicker) {
            CustomColorPickerDialog(
                initialColor = brushColor,
                onColorSelected = { viewModel.setBrushColor(it) },
                onDismissRequest = {
                    showCustomColorPicker = false
                }
            )
        }

        if (activeBrush != null) {
            DrawingSurface(
                strokes = localStrokes,
                canvasStrokeRenderer = canvasStrokeRenderer,
                onStrokesFinished = { newStrokes ->
                    localStrokes.addAll(newStrokes)
                    viewModel.onStrokesFinished(newStrokes)
                },
                onErase = { _, _ -> },
                onEraseStart = { },
                onEraseEnd = { },
                currentBrush = activeBrush!!,
                onGetNextBrush = { viewModel.getActiveBrush() ?: activeBrush!! },
                isEraserMode = false,
                backgroundImageUri = null,
                onStartDrag = {},
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var sizeMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { sizeMenuExpanded = true }) {
                        Text("${brushSize.toInt()} px", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(
                        expanded = sizeMenuExpanded,
                        onDismissRequest = { sizeMenuExpanded = false })
                    {
                        listOf(2f, 5f, 10f, 15f, 25f, 50f, 100f).forEach { size ->
                            DropdownMenuItem(
                                text = { Text("${size.toInt()} px") },
                                onClick = {
                                    viewModel.setBrushSize(size); sizeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                var colorMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { colorMenuExpanded = true }) {
                        Icon(
                            painterResource(R.drawable.circle_24px),
                            contentDescription = "Color",
                            tint = brushColor
                        )
                    }
                    DropdownMenu(
                        expanded = colorMenuExpanded,
                        onDismissRequest = { colorMenuExpanded = false })
                    {
                        val colors = mapOf(
                            "Black" to Color.Black,
                            "Red" to Color.Red,
                            "Blue" to Color.Blue,
                            "Green" to Color.Green,
                            "Yellow" to Color.Yellow
                        )
                        colors.forEach { (name, color) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.circle_24px),
                                        contentDescription = name,
                                        tint = color
                                    )
                                },
                                onClick = {
                                    viewModel.setBrushColor(color); colorMenuExpanded = false
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        DropdownMenuItem(
                            text = { Text("Custom Color...") },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.circle_24px),
                                    contentDescription = "Custom Color",
                                    tint = brushColor
                                )
                            },
                            onClick = {
                                colorMenuExpanded = false
                                showCustomColorPicker = true
                            }
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Invalid Brush Configuration\nCheck Constraints",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CustomColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit
) {
    var currentColor by remember { mutableStateOf(HsvColor.from(initialColor)) }

    AlertDialog(
        onDismissRequest = {
            onColorSelected(currentColor.toColor())
            onDismissRequest()
        },
        title = { Text("Pick a color") },
        text = {
            Column(horizontalAlignment = Alignment.End) {
                ClassicColorPicker(
                    color = currentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    onColorChanged = { hsvColor: HsvColor ->
                        currentColor = hsvColor
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                val hexString = String.format("%08x", currentColor.toColor().toArgb())
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(currentColor.toColor())
                    onDismissRequest()
                }
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}