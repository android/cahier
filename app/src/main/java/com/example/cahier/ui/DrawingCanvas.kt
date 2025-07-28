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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import com.example.cahier.R
import com.example.cahier.ui.viewmodels.DrawingCanvasViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawingCanvas(
    navBackStackEntry: NavBackStackEntry,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    drawingCanvasViewModel: DrawingCanvasViewModel = hiltViewModel()
) {
    val uiState by drawingCanvasViewModel.uiState.collectAsState()
    LocalContext.current
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    val coroutineScope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<Stroke>() }
    val activity = LocalActivity.current as ComponentActivity
    val currentBrush by drawingCanvasViewModel.currentBrush.collectAsState()
    val isEraserMode by drawingCanvasViewModel.isEraserMode.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    val exportedUri by drawingCanvasViewModel.exportedImageUri.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val view = LocalView.current
    val canUndo by drawingCanvasViewModel.canUndo.collectAsState()
    val canRedo by drawingCanvasViewModel.canRedo.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            if (uiState.note.imageUriList?.isNotEmpty() == true) {
                pendingImageUri = it
                showConfirmationDialog = true
            } else {
                coroutineScope.launch {
                    drawingCanvasViewModel
                        .processAndAddImage(activity.contentResolver, it)
                }
            }
        }
    }

    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dragEvent = event.toAndroidDragEvent()
                activity.lifecycleScope.launch {
                    val permission = activity.requestDragAndDropPermissions(dragEvent)
                    if (permission != null) {
                        try {
                            if (dragEvent.clipData.itemCount > 0) {
                                val uri = dragEvent.clipData.getItemAt(0).uri
                                if (uri != null) {
                                    val localUri =
                                        drawingCanvasViewModel.fileHelper.copyUriToInternalStorage(
                                            activity.contentResolver, uri
                                        )
                                    drawingCanvasViewModel.addImageWithLocalUri(localUri)
                                }
                            }
                        } finally {
                            permission.release()
                        }
                    }
                }
                return true
            }
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
                activity.contentResolver,
                canvasSize.width,
                canvasSize.height
            )
        }
    }


    if (showConfirmationDialog) {
        ConfirmationDialog(
            onConfirm = {
                drawingCanvasViewModel.replaceImage(
                    activity.contentResolver,
                    pendingImageUri
                )
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = uiState.note.title,
                onValueChange = { newTitle ->
                    coroutineScope.launch {
                        drawingCanvasViewModel.updateNoteTitle(newTitle)
                    }
                },
                placeholder = { Text(text = stringResource(R.string.drawing_title)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { })
            )
        }
        DrawingToolbox(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp),
            drawingCanvasViewModel = drawingCanvasViewModel,
            imagePickerLauncher = imagePickerLauncher,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = drawingCanvasViewModel::undo,
            onRedo = drawingCanvasViewModel::redo,
            onExit = navigateUp,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .onSizeChanged { canvasSize = it }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.mimeTypes().any { it.startsWith("image/") }
                    }, target = dropTarget
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
                                arrayOf("image/png")
                            ),
                            ClipData.Item(uri)
                        )
                        val dragShadowBuilder = View.DragShadowBuilder(view)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingToolbox(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var brushMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val isEraserMode by drawingCanvasViewModel.isEraserMode.collectAsState()
    val uiState by drawingCanvasViewModel.uiState.collectAsState()
    var optionsMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        LazyRow(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )

        ) {
            item {
                IconButton(onClick = {
                    brushMenuExpanded = true
                    drawingCanvasViewModel.setEraserMode(false)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.brush_24px),
                        contentDescription = stringResource(R.string.brush),
                        modifier = Modifier.background(
                            color = if (isEraserMode) Color.Transparent else
                                MaterialTheme.colorScheme.inversePrimary,
                            shape = CircleShape
                        )
                    )
                }
            }

            item {
                IconButton(onClick = {
                    showColorPicker = true
                    drawingCanvasViewModel.setEraserMode(false)
                }) {
                    Icon(
                        painter = painterResource(R.drawable.palette_24px),
                        contentDescription = stringResource(R.string.color),
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }

            item {
                VerticalDivider(
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }

            item {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(
                        painter = painterResource(R.drawable.undo_24px),
                        contentDescription = stringResource(R.string.undo)
                    )
                }
            }

            item {
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(
                        painter = painterResource(R.drawable.redo_24px),
                        contentDescription = stringResource(R.string.redo)
                    )
                }
            }

            item {
                IconButton(onClick = { drawingCanvasViewModel.setEraserMode(true) }) {
                    Icon(
                        painter = painterResource(R.drawable.ink_eraser_24px),
                        contentDescription = stringResource(R.string.eraser),
                        modifier = Modifier.background(
                            color = if (isEraserMode)
                                MaterialTheme.colorScheme.inversePrimary else Color.Transparent,
                        )
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            drawingCanvasViewModel.clearScreen()
                        }
                    }
                ) {
                    Text(text = stringResource(R.string.clear))
                }
            }

            item {
                Spacer(modifier = Modifier.width(16.dp))
            }

            item {
                VerticalDivider(
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .height(48.dp)
                        .padding(vertical = 8.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }

            item {
                IconButton(onClick = { drawingCanvasViewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (uiState.note.isFavorite)
                            Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (uiState.note.isFavorite)
                            stringResource(R.string.unfavorite) else stringResource(R.string.favorite),
                        tint = if (uiState.note.isFavorite)
                            MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.size(4.dp))
            }

            item {
                IconButton(onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }) {
                    Icon(
                        painter = painterResource(R.drawable.image_24px),
                        contentDescription = stringResource(R.string.add_image)
                    )
                }
            }

            item {
                Box {
                    IconButton(onClick = { optionsMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.more_options)
                        )
                    }
                    DropdownMenu(
                        expanded = optionsMenuExpanded,
                        onDismissRequest = { optionsMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.exit)) },
                            onClick = {
                                optionsMenuExpanded = false
                                onExit()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        }

        BrushDropdownMenu(
            expanded = brushMenuExpanded,
            onDismissRequest = { brushMenuExpanded = false },
            onBrushChange = { newBrush, newSize ->
                coroutineScope.launch {
                    drawingCanvasViewModel.changeBrush(newBrush, newSize)
                }
                brushMenuExpanded = false
            },
        )

        ColorPickerDialog(
            showDialog = showColorPicker,
            onDismissRequest = { showColorPicker = false },
            onColorSelected = { color ->
                coroutineScope.launch {
                    drawingCanvasViewModel.changeBrushColor(color)
                }
                showColorPicker = false
            }
        )
    }

}

@Composable
fun BrushDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onBrushChange: (BrushFamily, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pressurePenSize = 5f
    val markerSize = 10f
    val highlighterSize = 25f
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true)
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.pressure_pen)) },
            onClick = { onBrushChange(StockBrushes.pressurePenLatest, pressurePenSize) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.marker)) },
            onClick = { onBrushChange(StockBrushes.markerLatest, markerSize) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.highlighter)) },
            onClick = { onBrushChange(StockBrushes.highlighterLatest, highlighterSize) }
        )
        DropdownMenuItem(
            text = { Text(text = stringResource(R.string.dashed_line)) },
            onClick = { onBrushChange(StockBrushes.dashedLineLatest, pressurePenSize) }
        )
    }
}