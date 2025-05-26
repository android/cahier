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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.ink.strokes.Stroke
import coil3.compose.AsyncImage
import com.example.cahier.R
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType

@Composable
fun NoteList(
    favorites: List<Note>,
    otherNotes: List<Note>,
    noteList: List<Note>,
    onAddNewTextNote: () -> Unit,
    onAddNewDrawingNote: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onNewWindow: (Note) -> Unit,
    modifier: Modifier = Modifier,
    onDeleteNote: (Note) -> Unit = {},
) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Scaffold(
            topBar = {
            }, floatingActionButton = {
                val expanded = rememberSaveable { mutableStateOf(false) }
                AddFloatingButton(
                    expanded = expanded,
                    onTextNoteSelected = {
                        expanded.value = true
                        onAddNewTextNote()
                    },
                    onDrawingNoteSelected = {
                        expanded.value = true
                        onAddNewDrawingNote()
                    }
                )
            }, modifier = modifier
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.favorites),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(favorites, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onDelete = { onDeleteNote(note) },
                            onToggleFavorite = { onToggleFavorite(note.id) },
                            onNewWindow = { onNewWindow(note) }
                        )
                    }

                }

                if (otherNotes.isNotEmpty()) {
                    item {
                        AnimatedVisibility(favorites.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.other_notes),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(otherNotes, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onDelete = { onDeleteNote(note) },
                            onToggleFavorite = { onToggleFavorite(note.id) },
                            onNewWindow = { onNewWindow(note) }
                        )
                    }
                }

                if (favorites.isEmpty() && otherNotes.isEmpty()) {
                    items(noteList, key = { it.id }) { note ->
                        NoteItem(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onDelete = { onDeleteNote(note) },
                            onToggleFavorite = { onToggleFavorite(note.id) },
                            onNewWindow = { onNewWindow(note) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}


@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNewWindow: () -> Unit,
    modifier: Modifier = Modifier
) {

    OutlinedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Text(
            text = note.title.ifBlank { stringResource(R.string.untitled_note) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp)
        )

        Column(modifier = Modifier.weight(1f)) {

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                note.imageUriList?.let { uriString ->
                    AsyncImage(
                        model = uriString,
                        contentDescription = stringResource(R.string.note_image_preview),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.media)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                if (note.type == NoteType.TEXT) {
                    note.text?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_drawing_mode),
                        contentDescription = stringResource(R.string.drawing_note_indicator),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.drawing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }
            }
        }

        Row {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (note.isFavorite)
                        Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (note.isFavorite)
                        stringResource(R.string.unfavorite) else stringResource(R.string.favorite),
                    tint = if (note.isFavorite)
                        MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_note)
                )
            }
            IconButton(onClick = onNewWindow) {
                Icon(
                    painter = painterResource(R.drawable.outline_open_in_new_24),
                    contentDescription = stringResource(R.string.open_new_window)
                )
            }
        }
    }
}

@Composable
fun NoteDetail(
    note: Note,
    strokes: List<Stroke>,
    onClickToEdit: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .clickable { onClickToEdit(note) }
                .padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            when (note.type) {
                NoteType.TEXT -> {
                    note.text?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                NoteType.DRAWING -> {
                    DrawingPreview(
                        strokes = strokes,
                        onClick = { onClickToEdit(note) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
            note.imageUriList.let { uriString ->
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = uriString,
                    contentDescription = note.title,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}


@Composable
fun AddFloatingButton(
    expanded: MutableState<Boolean>,
    onTextNoteSelected: () -> Unit,
    onDrawingNoteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
            .padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded.value,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = stringResource(R.string.drawing),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = stringResource(R.string.text_note),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    FloatingActionButton(
                        onClick = {
                            onDrawingNoteSelected()
                            expanded.value = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.stylus_note_24px),
                            contentDescription = stringResource(R.string.drawing_note)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FloatingActionButton(
                        onClick = {
                            onTextNoteSelected()
                            expanded.value = false
                        },
                        modifier = Modifier.padding(top = 8.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sticky_note_24px),
                            contentDescription = stringResource(R.string.text_note)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloatingActionButton(
            onClick = { expanded.value = !expanded.value },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector =
                    if (expanded.value) Icons.Default.Close else Icons.Default.Add,
                contentDescription = stringResource(R.string.add_note)
            )
        }
    }
}