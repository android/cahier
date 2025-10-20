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
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.defaultDragHandleSemantics
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.ink.strokes.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    @DrawableRes val icon: Int,
    @StringRes val contentDescription: Int
) {
    Home(
        label = R.string.home,
        icon = R.drawable.home_24px,
        contentDescription = R.string.home
    ),
    Settings(
        label = R.string.settings,
        icon = R.drawable.settings_24px,
        contentDescription = R.string.settings
    ),
}


@SuppressLint("NewApi")
@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3WindowSizeClassApi::class
)
@Composable
fun HomePane(
    navigateToCanvas: (Long) -> Unit,
    navigateToDrawingCanvas: (Long) -> Unit,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.Home) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Note>()
    val noteList by homeScreenViewModel.noteList.collectAsStateWithLifecycle()
    val selectedNoteUIState by homeScreenViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val paneExpansionState = rememberPaneExpansionState()
    var hasSetInitialProportion by remember {
        mutableStateOf(false)
    }
    val isCompact = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact
    val context = LocalContext.current

    LaunchedEffect(context) {
        homeScreenViewModel.newWindowEvent.collect { intent ->
            context.startActivity(intent)
        }
    }

    LaunchedEffect(isCompact) {
        if (isCompact) {
            homeScreenViewModel.clearSelection()
            navigator.navigateTo(ListDetailPaneScaffoldRole.List)
        }
    }

    LaunchedEffect(isCompact, noteList) {
        if (!isCompact && noteList.noteList.isNotEmpty() && selectedNoteUIState.note.id == 0L) {
            homeScreenViewModel.selectNote(noteList.noteList.first().id)
        }
    }

    LaunchedEffect(isCompact, hasSetInitialProportion) {
        if (!isCompact && !hasSetInitialProportion) {
            paneExpansionState.setFirstPaneProportion(0.3f)
            hasSetInitialProportion = true
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
            navigationItems = {
                AppDestinations.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationSuiteItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = destination.icon),
                                contentDescription = stringResource(
                                    destination.contentDescription
                                )
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                        selected = isSelected,
                        onClick = {
                            if (currentDestination != destination) {
                                currentDestination = destination
                                if (destination != AppDestinations.Home
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
            navigationItemVerticalArrangement = Arrangement.Center,
            content = {
                when (currentDestination) {
                    AppDestinations.Home -> {
                        ListDetailPaneScaffold(
                            directive = navigator.scaffoldDirective,
                            value = navigator.scaffoldValue,
                            paneExpansionState = paneExpansionState,
                            paneExpansionDragHandle = { state ->
                                val interactionSource = remember { MutableInteractionSource() }
                                VerticalDragHandle(
                                    modifier =
                                        Modifier.paneExpansionDraggable(
                                            state,
                                            LocalMinimumInteractiveComponentSize
                                                .current,
                                            interactionSource,
                                            state.defaultDragHandleSemantics()
                                        ),
                                )
                            },
                            listPane = {
                                ListPaneContent(
                                    noteList = noteList.noteList,
                                    isCompact = isCompact,
                                    selectedNoteId = if (isCompact) null
                                    else selectedNoteUIState.note.id,
                                    onNoteClick = {
                                        if (isCompact) {
                                            if (it.type == NoteType.Drawing) {
                                                navigateToDrawingCanvas(it.id)
                                            } else {
                                                navigateToCanvas(it.id)
                                            }
                                        } else {
                                            homeScreenViewModel.selectNote(it.id)
                                        }
                                    },
                                    onAddNewTextNote = {
                                        homeScreenViewModel.addNote { noteId ->
                                            navigateToCanvas(noteId)
                                        }
                                    },
                                    onAddNewDrawingNote = {
                                        homeScreenViewModel.addDrawingNote { noteId ->
                                            navigateToDrawingCanvas(noteId)
                                        }
                                    },
                                    onDeleteNote = { note ->
                                        homeScreenViewModel.deleteNote(note)
                                        navigateUp()
                                    },
                                    onToggleFavorite = { noteId ->
                                        homeScreenViewModel.toggleFavorite(noteId)
                                    },
                                    onNewWindow = { note ->
                                        homeScreenViewModel.openInNewWindow(note)
                                    },
                                )
                            },
                            detailPane = {
                                if (!isCompact) {
                                    selectedNoteUIState.note.let { note ->
                                        DetailPaneContent(
                                            note = note,
                                            strokes = selectedNoteUIState.strokes,
                                            onClickToEdit = {
                                                if (note.type == NoteType.Text) {
                                                    navigateToCanvas(note.id)
                                                } else {
                                                    navigateToDrawingCanvas(note.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }

                    AppDestinations.Settings -> {
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
    isCompact: Boolean,
    selectedNoteId: Long?,
    onNoteClick: (Note) -> Unit,
    onAddNewTextNote: () -> Unit,
    onAddNewDrawingNote: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (Note) -> Unit,
    onNewWindow: (Note) -> Unit,
) {
    val (favorites, others) = noteList.partition { it.isFavorite }

    NoteList(
        favorites = favorites,
        otherNotes = others,
        isCompact = isCompact,
        selectedNoteId = selectedNoteId,
        onNoteClick = onNoteClick,
        onAddNewTextNote = onAddNewTextNote,
        onAddNewDrawingNote = onAddNewDrawingNote,
        onDeleteNote = onDeleteNote,
        onToggleFavorite = onToggleFavorite,
        onNewWindow = onNewWindow,
        modifier = modifier
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
        onClickToEdit = onClickToEdit,
        modifier = modifier
    )
}