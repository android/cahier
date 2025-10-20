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

import android.net.Uri
import android.util.Log
import android.view.DragAndDropPermissions
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.data.Note
import com.example.cahier.data.NotesRepository
import com.example.cahier.navigation.TextCanvasDestination
import com.example.cahier.ui.CahierUiState
import com.example.cahier.utils.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CanvasScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NotesRepository,
    val fileHelper: FileHelper
) : ViewModel() {
    private val _uiState = MutableStateFlow(CahierUiState())
    val uiState: StateFlow<CahierUiState> = _uiState.asStateFlow()

    private val noteId: Long? = savedStateHandle[TextCanvasDestination.NOTE_ID_ARG]

    init {
        viewModelScope.launch {
            if (noteId != null && noteId != 0L) {
                noteRepository.getNoteStream(noteId)
                    .filterNotNull()
                    .collect { loadedNote ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                note = loadedNote,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateNoteTitle(title: String) {
        updateNoteField(title) { note, value -> note.copy(title = value) }
    }

    fun updateNoteText(text: String) {
        updateNoteField(text) { note, value -> note.copy(text = value) }
    }

    private fun <T> updateNoteField(value: T, updater: (Note, T) -> Note) {
        try {
            _uiState.value = _uiState.value.copy(
                note = updater(_uiState.value.note, value)
            )
            viewModelScope.launch {
                if (noteId != null)
                    noteRepository.updateNote(_uiState.value.note)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Error updating note: ${e.message}")
        }
    }

    fun addImage(localUri: Uri?) {
        if (localUri == null) {
            Log.w(TAG, "addImage called with null URI, ignoring.")
            return
        }
        viewModelScope.launch {
            try {
                val currentList = _uiState.value.note.imageUriList ?: emptyList()
                val newList = currentList + localUri.toString()

                _uiState.update { currentState ->
                    val updatedNote = currentState.note.copy(imageUriList = newList)
                    currentState.copy(note = updatedNote)
                }

                noteId?.let {
                    noteRepository.updateNoteImageUriList(
                        it,
                        newList
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add image", e)
                _uiState.update { it.copy(error = "Failed to add image.") }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            noteId?.let { noteRepository.toggleFavorite(it) }
        }
    }

    fun handleDroppedUri(uri: Uri, permissions: DragAndDropPermissions?) {
        viewModelScope.launch {
            try {
                val localUri = fileHelper.copyUriToInternalStorage(uri)
                addImage(localUri)
            } finally {
                permissions?.release()
            }
        }
    }

    fun handlePickedImageUri(uri: Uri) {
        viewModelScope.launch {
            val localUri = fileHelper.copyUriToInternalStorage(uri)
            addImage(localUri)
        }
    }

    companion object {
        private const val TAG = "CanvasScreenViewModel"
    }
}