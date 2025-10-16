/*
 *
 *  * Copyright 2025 Google LLC. All rights reserved.
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
 *
 */

package com.example.cahier.ui

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cahier.R
import com.example.cahier.ui.theme.CahierAppTheme
import com.example.cahier.ui.utils.createDropTarget
import com.example.cahier.ui.viewmodels.DrawingCanvasViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun DrawingCanvas(
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    drawingCanvasViewModel: DrawingCanvasViewModel = hiltViewModel()
) {
    val uiState by drawingCanvasViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            if (uiState.note.imageUriList?.isNotEmpty() == true) {
                pendingImageUri = it
                showConfirmationDialog = true
            } else {
                coroutineScope.launch {
                    drawingCanvasViewModel.processAndAddImage(it)
                }
            }
        }
    }

    if (showConfirmationDialog) {
        ConfirmationDialog(
            onConfirm = {
                drawingCanvasViewModel.replaceImage(pendingImageUri)
                showConfirmationDialog = false
                pendingImageUri = null
            },
            onDismiss = {
                showConfirmationDialog = false
                pendingImageUri = null
            },
            title = stringResource(R.string.replace_image_title),
            text = stringResource(R.string.replace_image_text)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        DrawingCanvasTopBar(drawingCanvasViewModel, coroutineScope)
        DrawingCanvasContent(
            drawingCanvasViewModel = drawingCanvasViewModel,
            imagePickerLauncher = imagePickerLauncher,
            onNavigateUp = navigateUp
        )
    }
}

@Composable
private fun DrawingCanvasTopBar(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val uiState by drawingCanvasViewModel.uiState.collectAsStateWithLifecycle()
    var titleState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.note.title))
    }
    var focusedFieldEnum by rememberSaveable { mutableStateOf(FocusedFieldEnum.None) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusedFieldEnum) {
        if (focusedFieldEnum == FocusedFieldEnum.Title) {
            titleFocusRequester.requestFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        TextField(
            value = titleState,
            onValueChange = { newTitle ->
                titleState = newTitle
                coroutineScope.launch {
                    drawingCanvasViewModel.updateNoteTitle(newTitle.text)
                }
            },
            placeholder = { Text(text = stringResource(R.string.drawing_title)) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(titleFocusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        focusedFieldEnum = FocusedFieldEnum.Title
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { })
        )
    }
}

@OptIn(
    ExperimentalMaterial3WindowSizeClassApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun DrawingCanvasContent(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current as ComponentActivity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    val canUndo by drawingCanvasViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by drawingCanvasViewModel.canRedo.collectAsStateWithLifecycle()
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var brushesMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var sizeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val customBrushes by drawingCanvasViewModel.customBrushes.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        DrawingSurfaceWithTarget(
            drawingCanvasViewModel,
            modifier = Modifier.fillMaxSize()
        )

        val toolboxModifier = if (isCompact) {
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp)
        }

        DrawingToolbox(
            isVertical = isCompact,
            modifier = toolboxModifier,
            drawingCanvasViewModel = drawingCanvasViewModel,
            imagePickerLauncher = imagePickerLauncher,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = drawingCanvasViewModel::undo,
            onRedo = drawingCanvasViewModel::redo,
            onExit = onNavigateUp,
            onColorPickerClick = { showColorPicker = true },
        )

        BrushesDropdownMenu(
            expanded = brushesMenuExpanded,
            onDismissRequest = { brushesMenuExpanded = false },
            onBrushChange = { newBrush ->
                drawingCanvasViewModel.changeBrush(newBrush)
                brushesMenuExpanded = false
            },
            customBrushes = customBrushes
        )

        SizeDropdownMenu(
            expanded = sizeMenuExpanded,
            onDismissRequest = { sizeMenuExpanded = false },
            onSizeChange = { newSize ->
                drawingCanvasViewModel.changeBrushSize(newSize)
                sizeMenuExpanded = false
            }
        )

        ColorPickerDialog(
            showDialog = showColorPicker,
            onDismissRequest = { showColorPicker = false },
            onColorSelected = { color ->
                drawingCanvasViewModel.changeBrushColor(color)
                showColorPicker = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawingSurfaceWithTarget(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by drawingCanvasViewModel.uiState.collectAsStateWithLifecycle()
    val exportedUri by drawingCanvasViewModel.exportedImageUri.collectAsStateWithLifecycle()
    val currentBrush by drawingCanvasViewModel.currentBrush.collectAsStateWithLifecycle()
    val isEraserMode by drawingCanvasViewModel.isEraserMode.collectAsStateWithLifecycle()
    val strokes = remember { mutableStateListOf<Stroke>() }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val view = LocalView.current
    val activity = LocalActivity.current as ComponentActivity

    val dropTarget = remember {
        createDropTarget(activity) { uri, permissions ->
            drawingCanvasViewModel.handleDroppedUri(uri, permissions)
        }
    }

    LaunchedEffect(uiState.strokes) {
        if (strokes != uiState.strokes) {
            strokes.clear()
            strokes.addAll(uiState.strokes)
        }
    }

    LaunchedEffect(
        uiState.strokes,
        uiState.note.imageUriList,
        canvasSize
    )
    {
        if (canvasSize != IntSize.Zero) {
            drawingCanvasViewModel.createExportedBitmap(
                canvasSize.width,
                canvasSize.height
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().any { it.startsWith("image/") }
                },
                target = dropTarget
            )
    ) {
        DrawingSurface(
            strokes = strokes,
            canvasStrokeRenderer = canvasStrokeRenderer,
            onStrokesFinished = { newStrokes ->
                strokes.addAll(newStrokes)
                drawingCanvasViewModel.onStrokesFinished(newStrokes)
            },
            onErase = drawingCanvasViewModel::erase,
            onEraseStart = drawingCanvasViewModel::startErase,
            onEraseEnd = drawingCanvasViewModel::endErase,
            onStartDrag = {
                exportedUri?.let { uri ->
                    val clipData = ClipData(
                        ClipDescription(
                            "Image",
                            arrayOf(
                                "image/png"
                            )
                        ),
                        ClipData.Item(uri)
                    )
                    val dragShadowBuilder = View.DragShadowBuilder(view)
                    // While Jetpack Compose offers the `dragAndDropSource`
                    // modifier, a custom implementation using the Android View
                    // system's `startDragAndDrop` is necessary here. This is
                    // because the Ink API's drawing gestures conflict with the
                    // long-press-to-drag gesture when using the standard Compose
                    // modifier, preventing drag detection. This approach allows
                    // for a custom gesture detector to coexist with the Ink API
                    // and manually initiate the drag for seamless interoperability.
                    view.startDragAndDrop(
                        clipData,
                        dragShadowBuilder,
                        null,
                        View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                    )
                }
            },
            currentBrush = currentBrush,
            onGetNextBrush = drawingCanvasViewModel::getCurrentBrush,
            isEraserMode = isEraserMode,
            backgroundImageUri = uiState.note.imageUriList?.firstOrNull(),
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DrawingCanvasPreview() {
    CahierAppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = "Drawing Title",
                    onValueChange = { },
                    placeholder = { Text(text = stringResource(R.string.drawing_title)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Drawing Surface")
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Text("Toolbox Placeholder", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}