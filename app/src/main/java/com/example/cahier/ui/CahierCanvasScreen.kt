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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.cahier.R
import com.example.cahier.data.Note
import com.example.cahier.ui.theme.CahierAppTheme
import com.example.cahier.ui.utils.createDropTarget
import com.example.cahier.ui.viewmodels.CanvasScreenViewModel
import com.example.cahier.utils.FileHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun NoteCanvas(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    canvasScreenViewModel: CanvasScreenViewModel = hiltViewModel()
) {
    val uiState by canvasScreenViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current as ComponentActivity

    var titleState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.note.title))
    }

    var bodyState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(uiState.note.text ?: ""))
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val localUri = canvasScreenViewModel.fileHelper.copyUriToInternalStorage(it)
                canvasScreenViewModel.addImage(localUri)
            }
        }
    }

    NoteCanvasContent(
        uiState = uiState,
        titleState = titleState,
        onTitleChange = {
            titleState = it
            canvasScreenViewModel.updateNoteTitle(it.text)
        },
        bodyState = bodyState,
        onBodyChange = {
            bodyState = it
            canvasScreenViewModel.updateNoteText(it.text)
        },
        onExit = onExit,
        imagePickerLauncher = imagePickerLauncher,
        onToggleFavorite = { canvasScreenViewModel.toggleFavorite() },
        onDroppedUri = { uri -> canvasScreenViewModel.handleDroppedUri(uri) },
        modifier = modifier,
        activity = activity
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCanvasContent(
    uiState: CahierUiState,
    titleState: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    bodyState: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    onExit: () -> Unit,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    onToggleFavorite: () -> Unit,
    onDroppedUri: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    activity: ComponentActivity
) {
    var focusedFieldEnum by rememberSaveable { mutableStateOf(FocusedFieldEnum.None) }
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(focusedFieldEnum) {
        when (focusedFieldEnum) {
            FocusedFieldEnum.Title -> titleFocusRequester.requestFocus()
            FocusedFieldEnum.Body -> {
                if (uiState.note.text != null) {
                    bodyFocusRequester.requestFocus()
                } else {
                    focusedFieldEnum = FocusedFieldEnum.None
                }
            }

            FocusedFieldEnum.None -> {
                /* Do nothing. */
            }
        }
    }

    val dropTarget = remember {
        createDropTarget(activity, onDroppedUri)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().any { it.startsWith("image/") }
                },
                target = dropTarget
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            NoteCanvasTopBar(
                titleState = titleState,
                onTitleChange = onTitleChange,
                titleFocusRequester = titleFocusRequester,
                onTitleFocusChanged = {
                    if (it.isFocused)
                        focusedFieldEnum = FocusedFieldEnum.Title
                },
                imagePickerLauncher = imagePickerLauncher,
                isFavorite = uiState.note.isFavorite,
                onToggleFavorite = onToggleFavorite,
                onExit = onExit
            )

            NoteCanvasBody(
                note = uiState.note,
                bodyState = bodyState,
                onBodyChange = onBodyChange,
                bodyFocusRequester = bodyFocusRequester,
                onBodyFocusChanged = { if (it.isFocused) focusedFieldEnum = FocusedFieldEnum.Body },
                coroutineScope = coroutineScope,
                view = LocalView.current
            )
        }
    }
}

@Composable
private fun NoteCanvasTopBar(
    titleState: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    titleFocusRequester: FocusRequester,
    onTitleFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var optionsMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = titleState,
            onValueChange = onTitleChange,
            placeholder = { Text(stringResource(R.string.title)) },
            modifier = Modifier
                .weight(1f)
                .focusRequester(titleFocusRequester)
                .onFocusChanged(onTitleFocusChanged),
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = true,
                capitalization = KeyboardCapitalization.Sentences
            ),
            textStyle = MaterialTheme.typography.titleLarge
        )

        Box {
            IconButton(onClick = { optionsMenuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.menu_24px),
                    contentDescription = stringResource(R.string.more_options)
                )
            }
            NoteCanvasDropdownMenu(
                expanded = optionsMenuExpanded,
                onDismissRequest = { optionsMenuExpanded = false },
                onUploadImage = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                isFavorite = isFavorite,
                onToggleFavorite = onToggleFavorite,
                onExit = onExit
            )
        }
    }
}

@Composable
private fun NoteCanvasDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onUploadImage: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.upload_image)) },
            onClick = {
                onDismissRequest()
                onUploadImage()
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
                    if (isFavorite) stringResource(R.string.unfavorite)
                    else stringResource(R.string.favorite)
                )
            },
            onClick = {
                onDismissRequest()
                onToggleFavorite()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.favorite_24px_filled
                        else R.drawable.favorite_24px
                    ),
                    contentDescription = if (isFavorite)
                        stringResource(R.string.unfavorite) else stringResource(
                        R.string.favorite
                    ),
                    tint = if (isFavorite)
                        MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.exit)) },
            onClick = {
                onDismissRequest()
                onExit()
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.exit_to_app_24px),
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun NoteCanvasBody(
    note: Note,
    bodyState: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
    bodyFocusRequester: FocusRequester,
    onBodyFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit,
    coroutineScope: CoroutineScope,
    view: View,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            note.text?.let {
                TextField(
                    value = bodyState,
                    placeholder = { Text(stringResource(R.string.note)) },
                    onValueChange = onBodyChange,
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = true,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .focusRequester(bodyFocusRequester)
                        .onFocusChanged(onBodyFocusChanged),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }
        }

        items(
            items = note.imageUriList ?: emptyList(),
            key = { it }
        ) { imageUriString ->
            NoteImage(
                imageUriString = imageUriString,
                coroutineScope = coroutineScope,
                view = view
            )
        }
    }
}

@Composable
private fun NoteImage(
    imageUriString: String,
    coroutineScope: CoroutineScope,
    view: View,
    modifier: Modifier = Modifier
) {
    val fileHelper = FileHelper(LocalContext.current)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(imageUriString) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        val fileUri = imageUriString.toUri()
                        val originalFile = fileUri.path?.let { File(it) }

                        if (originalFile != null && originalFile.exists()) {
                            coroutineScope.launch {
                                val contentUri = fileHelper.createShareableUri(originalFile)

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
                                    View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                                )
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
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

@Preview(showBackground = true)
@Composable
fun NoteCanvasScreenPreview(
    @PreviewParameter(NotePreviewParameterProvider::class) note: Note
) {
    CahierAppTheme {
        val activity = LocalActivity.current as ComponentActivity
        NoteCanvasContent(
            uiState = CahierUiState(note = note),
            titleState = TextFieldValue(note.title),
            onTitleChange = {},
            bodyState = TextFieldValue(note.text ?: ""),
            onBodyChange = {},
            onExit = {},
            imagePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = {}
            ),
            onToggleFavorite = {},
            onDroppedUri = {},
            activity = activity
        )
    }
}
