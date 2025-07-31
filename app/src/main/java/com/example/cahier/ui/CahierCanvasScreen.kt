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

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.example.cahier.R
import com.example.cahier.data.FocusedField
import com.example.cahier.ui.viewmodels.CanvasScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun NoteCanvas(
    navBackStackEntry: NavBackStackEntry,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    canvasScreenViewModel: CanvasScreenViewModel = hiltViewModel()
) {
    val uiState by canvasScreenViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var optionsMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var focusedField = rememberSaveable { mutableStateOf(FocusedField.NONE) }
    val titleFocusRequester = canvasScreenViewModel.titleFocusRequester
    val bodyFocusRequester = canvasScreenViewModel.bodyFocusRequester

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flag)
            coroutineScope.launch {
                canvasScreenViewModel.updateImageUri(it.toString())
            }
        }
    }

    val initialText: String = ""

    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    LaunchedEffect(Unit) {
        when (focusedField.value) {
            FocusedField.TITLE -> titleFocusRequester.requestFocus()
            FocusedField.BODY -> {
                if (uiState.note.text != null) {
                    bodyFocusRequester.requestFocus()
                } else {
                    focusedField.value = FocusedField.NONE
                }
            }

            FocusedField.NONE -> { /* Do nothing */
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
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
                                focusedField.value = FocusedField.TITLE
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
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
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

            if (uiState.note.imageUriList.isNullOrEmpty()) {
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
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .focusRequester(bodyFocusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedField.value = FocusedField.BODY
                                }
                            },
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxSize()

                    ) {
                        uiState.note.text?.let { text ->
                            TextField(
                                value = text,
                                placeholder = { Text(stringResource(R.string.note)) },
                                onValueChange = { canvasScreenViewModel.updateNoteText(it) },
                                keyboardOptions = KeyboardOptions(
                                    autoCorrectEnabled = true,
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                modifier = Modifier.fillMaxSize(),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        if (uiState.note.imageUriList?.isNotEmpty() == true) {
                            NoteImagesContainer(
                                images = uiState.note.imageUriList!!,
                                onClearImages = { /*TODO*/ },
                            )
                        }
                    }
                }
            }
        }
    }
}
