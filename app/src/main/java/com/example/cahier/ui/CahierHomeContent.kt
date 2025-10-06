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
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.ink.strokes.Stroke
import coil3.compose.AsyncImage
import com.example.cahier.R
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType
import com.example.cahier.ui.theme.CahierAppTheme

@Composable
fun NoteList(
    favorites: List<Note>,
    otherNotes: List<Note>,
    isCompact: Boolean,
    selectedNoteId: Long?,
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
            floatingActionButton = {
                val expanded = rememberSaveable { mutableStateOf(false) }
                CahierFloatingButton(
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
            },
            modifier = Modifier
        ) { innerPadding ->
            NoteListContent(
                favorites = favorites,
                otherNotes = otherNotes,
                isCompact = isCompact,
                selectedNoteId = selectedNoteId,
                onNoteClick = onNoteClick,
                onDeleteNote = onDeleteNote,
                onToggleFavorite = onToggleFavorite,
                onNewWindow = onNewWindow,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun NoteListContent(
    favorites: List<Note>,
    otherNotes: List<Note>,
    isCompact: Boolean,
    selectedNoteId: Long?,
    onNoteClick: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onNewWindow: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
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
                    isCompact = isCompact,
                    isSelected = selectedNoteId == note.id,
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
                    isCompact = isCompact,
                    isSelected = selectedNoteId == note.id,
                    onClick = { onNoteClick(note) },
                    onDelete = { onDeleteNote(note) },
                    onToggleFavorite = { onToggleFavorite(note.id) },
                    onNewWindow = { onNewWindow(note) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteItem(
    note: Note,
    isCompact: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNewWindow: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(CardDefaults.outlinedShape)
            .clickable { onClick() }
    ) {
        NoteItemContent(note)
        NoteItemActions(onToggleFavorite, note, onDelete, onNewWindow, isCompact)
    }
}

@Composable
private fun NoteItemContent(
    note: Note,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = note.title.ifBlank { stringResource(R.string.untitled_note) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (!note.imageUriList.isNullOrEmpty()) {
                note.imageUriList.forEach { imageUri ->
                    AsyncImage(
                        model = imageUri,
                        contentDescription = stringResource(R.string.note_image_preview),
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.media)
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            NoteItemBody(note)
        }
    }
}

@Composable
private fun NoteItemBody(
    note: Note,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        when (note.type) {
            NoteType.Text -> {
                if (!note.text.isNullOrBlank()) {
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            NoteType.Drawing -> {
                Image(
                    painterResource(id = R.drawable.ic_drawing_mode),
                    contentDescription = stringResource(R.string.drawing_note_indicator),
                    modifier = Modifier.size(24.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteItemActions(
    onToggleFavorite: () -> Unit,
    note: Note,
    onDelete: () -> Unit,
    onNewWindow: () -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
            tooltip = {
                PlainTooltip {
                    Text(
                        if (note.isFavorite) stringResource(R.string.unfavorite)
                        else stringResource(R.string.favorite)
                    )
                }
            },
            state = rememberTooltipState(),
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = if (note.isFavorite)
                        painterResource(id = R.drawable.favorite_24px_filled) else
                        painterResource(id = R.drawable.favorite_24px),
                    contentDescription = if (note.isFavorite)
                        stringResource(R.string.unfavorite) else stringResource(
                        R.string.add_to_favorites
                    ),
                    tint = if (note.isFavorite)
                        MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                TooltipAnchorPosition.Above
            ),
            tooltip = { PlainTooltip { Text(stringResource(R.string.delete_note)) } },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_24px),
                    contentDescription = stringResource(R.string.delete_note)
                )
            }
        }
        AnimatedVisibility(!isCompact) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above
                ),
                tooltip = {
                    PlainTooltip { Text(stringResource(R.string.open_new_window)) }
                },
                state = rememberTooltipState()
            ) {
                IconButton(
                    onClick = onNewWindow,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.outline_open_in_new_24),
                        contentDescription = stringResource(R.string.open_new_window)
                    )
                }
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
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 6.dp
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        when (note.type) {
            NoteType.Text -> {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .clickable { onClickToEdit(note) }
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    note.text?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            NoteType.Drawing -> {
                DrawingPreview(
                    strokes = strokes,
                    onClick = { onClickToEdit(note) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    backgroundImageUri = note.imageUriList?.firstOrNull(),
                )
            }
        }
    }
}

@Composable
fun CahierFloatingButton(
    expanded: MutableState<Boolean>,
    onTextNoteSelected: () -> Unit,
    onDrawingNoteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(16.dp)
    ) {
        AnimatedVisibility(
            visible = expanded.value,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            ExpandedFabContent(
                onTextNoteSelected = onTextNoteSelected,
                onDrawingNoteSelected = onDrawingNoteSelected,
                onCollapse = { expanded.value = false }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FloatingActionButton(
            onClick = { expanded.value = !expanded.value },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                painter = if (expanded.value)
                    painterResource(R.drawable.close_24px) else
                    painterResource(R.drawable.add_24px),
                contentDescription = stringResource(R.string.add_note)
            )
        }
    }
}

@Composable
private fun ExpandedFabContent(
    onTextNoteSelected: () -> Unit,
    onDrawingNoteSelected: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Text(
                stringResource(
                    R.string.drawing
                ), modifier = Modifier.padding(vertical = 20.dp)
            )
            Text(
                stringResource(
                    R.string.text_note
                ), modifier = Modifier.padding(vertical = 20.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            FloatingActionButton(
                onClick = {
                    onDrawingNoteSelected()
                    onCollapse()
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
                    onCollapse()
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

@Preview(showBackground = true)
@Composable
fun NoteListPreview(
    modifier: Modifier = Modifier
) {
    val sampleNotes = listOf(
        Note(
            id = 1,
            title = "Favorite Note 1",
            text = "This is a favorite note.",
            isFavorite = true,
            type = NoteType.Text
        ),
        Note(
            id = 2,
            title = "Drawing Note",
            isFavorite = true,
            type = NoteType.Drawing
        ),
        Note(
            id = 3,
            title = "Regular Note",
            isFavorite = false,
            text = "This is a regular note.",
            type = NoteType.Text
        ),
    )
    val (favorites, others) = sampleNotes.partition { it.isFavorite }
    CahierAppTheme {
        NoteList(
            favorites = favorites,
            otherNotes = others,
            isCompact = false,
            selectedNoteId = null,
            onAddNewTextNote = {},
            onAddNewDrawingNote = {},
            onNoteClick = {},
            onToggleFavorite = {},
            onNewWindow = {},
            onDeleteNote = {},
            modifier = modifier
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteItemPreview(
    @PreviewParameter(NotePreviewParameterProvider::class) note: Note
) {
    CahierAppTheme {
        NoteItem(
            note = note,
            isCompact = false,
            isSelected = false,
            onClick = {},
            onDelete = {},
            onToggleFavorite = {},
            onNewWindow = {}
        )
    }
}