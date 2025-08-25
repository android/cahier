/*
 *
 *  *
 *  *  * Copyright 2025 Google LLC. All rights reserved.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */


package com.example.cahier.data

import androidx.ink.strokes.Stroke
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

class FakeNotesRepository : NotesRepository {

    private val notesFlow = MutableStateFlow<LinkedHashMap<Long, Note>>(LinkedHashMap())
    private val strokesFlow =
        MutableStateFlow<LinkedHashMap<Long, List<Stroke>>>(LinkedHashMap())
    private var nextId = 1L

    fun getNotes(): List<Note> = notesFlow.value.values.toList()

    override fun getAllNotesStream(): Flow<List<Note>> {
        return notesFlow.asStateFlow()
            .map { it.values.toList().sortedByDescending { note -> note.id } }
    }

    override fun getNoteStream(id: Long): Flow<Note> {
        return notesFlow.asStateFlow().mapNotNull { it[id] }
    }

    override suspend fun addNote(note: Note): Long {
        val id = nextId++
        val newNote = note.copy(id = id)
        notesFlow.update {
            val current = LinkedHashMap(it)
            current[id] = newNote
            current
        }
        strokesFlow.update {
            val current = LinkedHashMap(it)
            current[id] = emptyList()
            current
        }
        return id
    }

    override suspend fun deleteNote(note: Note) {
        notesFlow.update {
            val current = LinkedHashMap(it)
            current.remove(note.id)
            current
        }
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.update {
            val current = LinkedHashMap(it)
            if (current.containsKey(note.id)) {
                current[note.id] = note
            }
            current
        }
    }

    override suspend fun updateNoteStrokes(noteId: Long, strokes: List<Stroke>) {
        //
    }

    override suspend fun getNoteStrokes(noteId: Long): List<Stroke> {
        return strokesFlow.value[noteId] ?: emptyList()
    }

    override suspend fun toggleFavorite(noteId: Long) {
        updateNoteField(noteId) { it.copy(isFavorite = !it.isFavorite) }
    }

    override suspend fun updateNoteImageUriList(noteId: Long, imageUriList: List<String>?) {
        updateNoteField(noteId) { it.copy(imageUriList = imageUriList) }
    }

    private fun updateNoteField(noteId: Long, updateAction: (Note) -> Note) {
        notesFlow.update {
            val current = LinkedHashMap(it)
            val note = current[noteId]
            if (note != null) {
                current[noteId] = updateAction(note)
            }
            current
        }
    }
}