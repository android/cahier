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

package com.example.cahier.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.AppArgs
import com.example.cahier.MainActivity
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType
import com.example.cahier.data.NotesRepository
import com.example.cahier.ui.CahierUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CahierUiState())
    val uiState: StateFlow<CahierUiState> = _uiState.asStateFlow()

    private val _newWindowEvent = Channel<Intent>()
    val newWindowEvent = _newWindowEvent.receiveAsFlow()

    /**
     * Holds ui state for the list of notes on the home pane.
     * The list of items are retrieved from [NotesRepository] and mapped to
     * [NoteListUiState]
     */
    val noteList: StateFlow<NoteListUiState> =
        noteRepository.getAllNotesStream().map { NoteListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = NoteListUiState()
            )

    fun selectNote(noteId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                noteRepository.getNoteStream(noteId)
                    .filterNotNull()
                    .collect { note ->
                        val strokes = if (note.type == NoteType.Drawing) {
                            noteRepository.getNoteStrokes(noteId)
                        } else {
                            emptyList()
                        }
                        _uiState.value = CahierUiState(note = note, strokes = strokes)
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error retrieving note: ${e.message}",
                    isLoading = false
                )
                Log.e(TAG, "Error retrieving note: ${e.message}")
            }
        }
    }

    fun addNote(callback: (noteId: Long) -> Unit) {
        viewModelScope.launch {
            val newNoteId = addNoteOfType(NoteType.Text)
            newNoteId?.let {
                callback(it)
            }
        }
    }

    fun addDrawingNote(callback: (id: Long) -> Unit) {
        viewModelScope.launch {
            val newNoteId = addNoteOfType(NoteType.Drawing)
            newNoteId?.let {
                callback(it)
            }
        }
    }

    private suspend fun addNoteOfType(noteType: NoteType): Long? {
        return try {
            val newNote = Note(
                id = 0,
                title = "",
                type = noteType,
                text = if (noteType == NoteType.Text) "" else null,
            )
            val insertedId = noteRepository.addNote(newNote)
            _uiState.value = CahierUiState(note = newNote.copy(id = insertedId))
            insertedId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding note: ${e.message}")
            _uiState.value =
                _uiState.value.copy(error = "Error adding note: ${e.message}")
            null
        }
    }

    fun deleteNote(noteToDelete: Note) {
        try {
            viewModelScope.launch {
                noteRepository.deleteNote(noteToDelete)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note: ${e.message}")
            _uiState.value =
                _uiState.value.copy(error = "Error deleting note: ${e.message}")
        }
    }

    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.toggleFavorite(noteId)
                if (_uiState.value.note.id == noteId) {
                    val updatedNote = noteRepository.getNoteStream(noteId).first()
                    _uiState.update { it.copy(note = updatedNote) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite: ${e.message}")
            }
        }
    }

    fun clearSelection() {
        _uiState.update { CahierUiState() }
    }

    fun openInNewWindow(note: Note) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(AppArgs.NOTE_TYPE_KEY, note.type)
            putExtra(AppArgs.NOTE_ID_KEY, note.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
        }
        viewModelScope.launch {
            _newWindowEvent.send(intent)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
        private const val TAG = "HomeScreenViewModel"
    }
}

/**
 * Ui State for HomeScreen
 */
data class NoteListUiState(val noteList: List<Note> = listOf())