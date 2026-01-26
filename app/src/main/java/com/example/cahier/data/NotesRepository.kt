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

package com.example.cahier.data

import androidx.ink.strokes.Stroke
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    /**
     * Retrieve all the notes from the given data source.
     */
    fun getAllNotesStream(): Flow<List<Note>>

    /**
     * Retrieve a note from the given data source that matches with the [id].
     */
    fun getNoteStream(id: Long): Flow<Note?>

    /**
     * Insert note in the data source
     */
    suspend fun addNote(note: Note): Long

    /**
     * Delete note from the data source
     */
    suspend fun deleteNote(note: Note)

    /**
     * Update note in the data source
     */
    suspend fun updateNote(note: Note)

    /**
     * Update the strokes data of a note.
     */
    suspend fun updateNoteStrokes(noteId: Long, strokes: List<Stroke>, clientBrushFamilyId: String?)

    /**
     * Retrieve strokes data for a note.
     */
    suspend fun getNoteStrokes(noteId: Long): List<Stroke>

    /**
     * Toggles the favorite status of a note.
     */
    suspend fun toggleFavorite(noteId: Long)

    /**
     * Updates the image URI list of a note.
     */
    suspend fun updateNoteImageUriList(noteId: Long, imageUriList: List<String>?)
}