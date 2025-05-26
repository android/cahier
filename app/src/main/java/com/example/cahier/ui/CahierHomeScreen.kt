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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.ink.strokes.Stroke
import com.example.cahier.AppArgs
import com.example.cahier.MainActivity
import com.example.cahier.R
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType
import com.example.cahier.navigation.NavigationDestination
import com.example.cahier.ui.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch

object HomeDestination : NavigationDestination {
    override val route = "home"
}

enum class AppDestinations(
    @StringRes val label: Int,
    val icon: ImageVector,
    @StringRes val contentDescription: Int
) {
    HOME(
        label = R.string.home,
        icon = Icons.Filled.Home,
        contentDescription = R.string.home
    ),
    SETTINGS(
        label = R.string.settings,
        icon = Icons.Filled.Settings,
        contentDescription = R.string.settings
    ),
}


@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomePane(
    navigateToCanvas: (Long) -> Unit,
    navigateToDrawingCanvas: (Long) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Note>()
    val noteList by homeScreenViewModel.noteList.collectAsState()
    val selectedNoteUIState by homeScreenViewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val activity = LocalActivity.current

    LaunchedEffect(selectedNoteUIState.note) {
        if (false) {
            if (noteList.noteList.isNotEmpty()) {
                coroutineScope.launch {
                    noteList.noteList.first().let { defaultSelectedNote ->
                        navigator.navigateTo(
                            ListDetailPaneScaffoldRole.Detail, defaultSelectedNote
                        )
                    }
                }
            } else {
                navigator.navigateTo(ListDetailPaneScaffoldRole.List)
            }
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        coroutineScope.launch {
            navigator.navigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    item(
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = stringResource(destination.contentDescription)
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                        selected = isSelected,
                        onClick = {
                            if (currentDestination != destination) {
                                currentDestination = destination
                                if (destination != AppDestinations.HOME
                                    && navigator.currentDestination?.pane ==
                                    ListDetailPaneScaffoldRole.Detail
                                ) {
                                    homeScreenViewModel.clearSelection()
                                }
                            }
                        }
                    )
                }
            },
            content = {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        ListDetailPaneScaffold(
                            directive = navigator.scaffoldDirective,
                            value = navigator.scaffoldValue,
                            listPane = {
                                ListPaneContent(
                                    noteList = noteList.noteList,
                                    onNoteClick = {
                                        coroutineScope.launch {
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                        }
                                        homeScreenViewModel.selectNote(it.id)
                                    },
                                    onAddNewTextNote = {
                                        homeScreenViewModel.addNote { it ->
                                            navigateToCanvas(it)
                                        }
                                    },
                                    onAddNewDrawingNote = {
                                        homeScreenViewModel.addDrawingNote { noteId ->
                                            navigateToDrawingCanvas(noteId)
                                        }
                                    },
                                    onDeleteNote = { note ->
                                        homeScreenViewModel.deleteNote()
                                        navigateUp()
                                    },
                                    onToggleFavorite = { noteId ->
                                        homeScreenViewModel.toggleFavorite(noteId)
                                    },
                                    onNewWindow = { note ->
                                        openNewWindow(activity, note)
                                    },
                                )
                            },
                            detailPane = {
                                selectedNoteUIState.note.let { note ->
                                    DetailPaneContent(
                                        note = note,
                                        strokes = selectedNoteUIState.strokes,
                                        onClickToEdit = {
                                            if (note.type == NoteType.TEXT) {
                                                navigateToCanvas(note.id)
                                            } else {
                                                navigateToDrawingCanvas(note.id)
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }

                    AppDestinations.SETTINGS -> {
                        SettingsPane(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }
        )
    }
}



@Composable
private fun ListPaneContent(
    noteList: List<Note>,
    onNoteClick: (Note) -> Unit,
    onAddNewTextNote: () -> Unit,
    onAddNewDrawingNote: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (Note) -> Unit = {},
    onNewWindow: (Note) -> Unit = {},
) {
    val (favorites, others) = noteList.partition { it.isFavorite }

    NoteList(
        noteList = noteList,
        favorites = favorites,
        otherNotes = others,
        onNoteClick = onNoteClick,
        onAddNewTextNote = onAddNewTextNote,
        onAddNewDrawingNote = onAddNewDrawingNote,
        onDeleteNote = onDeleteNote,
        onToggleFavorite = onToggleFavorite,
        onNewWindow = onNewWindow
    )
}

@Composable
private fun DetailPaneContent(
    note: Note,
    strokes: List<Stroke>,
    onClickToEdit: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    NoteDetail(
        note = note,
        strokes = strokes,
        onClickToEdit = onClickToEdit
    )
}

fun openNewWindow(activity: Activity?, note: Note) {
    val intent = Intent(activity, MainActivity::class.java)
    intent.putExtra(AppArgs.NOTE_TYPE_KEY, note.type)
    intent.putExtra(AppArgs.NOTE_ID_KEY, note.id)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT

    activity?.startActivity(intent)
}