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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavBackStackEntry
import coil3.compose.AsyncImage
import com.example.cahier.R
import com.example.cahier.data.FocusedField
import com.example.cahier.ui.viewmodels.CanvasScreenViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NoteCanvas(
    navBackStackEntry: NavBackStackEntry,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    canvasScreenViewModel: CanvasScreenViewModel = hiltViewModel()
) {
    val uiState by canvasScreenViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var optionsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current as ComponentActivity
    var focusedField by rememberSaveable { mutableStateOf(FocusedField.NONE) }
    val titleFocusRequester = canvasScreenViewModel.titleFocusRequester
    val bodyFocusRequester = canvasScreenViewModel.bodyFocusRequester
    val view = LocalView.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val localUri = canvasScreenViewModel.fileHelper.copyUriToInternalStorage(
                    activity.contentResolver, it
                )
                canvasScreenViewModel.addImage(localUri)
            }
        }
    }

    LaunchedEffect(Unit) {
        when (focusedField) {
            FocusedField.TITLE -> titleFocusRequester.requestFocus()
            FocusedField.BODY -> {
                if (uiState.note.text != null) {
                    bodyFocusRequester.requestFocus()
                } else {
                    focusedField = FocusedField.NONE
                }
            }

            FocusedField.NONE -> { /* Do nothing. */
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
                            val uri = dragEvent.clipData.getItemAt(0)?.uri
                            if (uri != null) {
                                val localUri = canvasScreenViewModel.fileHelper
                                    .copyUriToInternalStorage(
                                        activity.contentResolver,
                                        uri
                                    )
                                canvasScreenViewModel.addImage(localUri)
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

    Surface(
        modifier = modifier
            .fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event
                        .mimeTypes()
                        .any { it.startsWith("image/") }
                },
                target = dropTarget
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = uiState.note.title,
                    onValueChange = { canvasScreenViewModel.updateNoteTitle(it) },
                    placeholder = { Text(stringResource(R.string.title)) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(titleFocusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                focusedField = FocusedField.TITLE
                            }
                        },
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = true,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    textStyle = MaterialTheme.typography.titleLarge
                )

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
                            text = { Text(stringResource(R.string.upload_image)) },
                            onClick = {
                                optionsMenuExpanded = false
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts
                                            .PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.image_24px),
                                    contentDescription = stringResource(R.string.add_image)
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (uiState.note.isFavorite)
                                        stringResource(R.string.unfavorite)
                                    else stringResource(R.string.favorite)
                                )
                            },
                            onClick = {
                                optionsMenuExpanded = false
                                canvasScreenViewModel.toggleFavorite()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.note.isFavorite)
                                        Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = null
                                )
                            }
                        )

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

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    uiState.note.text?.let { text ->
                        TextField(
                            value = text,
                            placeholder = { Text(stringResource(R.string.note)) },
                            onValueChange = { canvasScreenViewModel.updateNoteText(it) },
                            keyboardOptions = KeyboardOptions(
                                autoCorrectEnabled = true,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .focusRequester(bodyFocusRequester)
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        focusedField = FocusedField.BODY
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                items(
                    items = uiState.note.imageUriList ?: emptyList(),
                    key = { it }) { imageUriString ->

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .pointerInput(imageUriString) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        val fileUri = imageUriString.toUri()
                                        val originalFile = fileUri.path?.let { File(it) }

                                        if (originalFile != null && originalFile.exists()) {
                                            coroutineScope.launch {
                                                val contentUri = canvasScreenViewModel
                                                    .fileHelper.createShareableUri(originalFile)

                                                val clipData = ClipData(
                                                    ClipDescription(
                                                        "Image",
                                                        arrayOf("image/*")
                                                    ),
                                                    ClipData.Item(contentUri)
                                                )
                                                val dragShadowBuilder = View.DragShadowBuilder(view)

                                                view.startDragAndDrop(
                                                    clipData,
                                                    dragShadowBuilder,
                                                    null,
                                                    View.DRAG_FLAG_GLOBAL
                                                            or View.DRAG_FLAG_GLOBAL_URI_READ
                                                )
                                            }
                                        }
                                    },
                                    onDrag = { _, _ ->
                                        // Consume events.
                                    },
                                    onDragEnd = {
                                        // Do nothing.
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = imageUriString,
                            contentDescription = stringResource(R.string.uploaded_image),
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                }
            }
        }
    }
}