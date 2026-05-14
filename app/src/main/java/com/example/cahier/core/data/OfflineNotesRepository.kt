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

package com.example.cahier.core.data

import android.content.Context
import androidx.ink.brush.Version
import androidx.ink.storage.AndroidBrushFamilySerialization
import androidx.ink.storage.BrushFamilyDecodeCallback
import androidx.ink.strokes.Stroke
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.core.ui.Converters
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import com.example.cahier.features.drawing.CustomBrushes
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream

class OfflineNotesRepository(
    private val notesDao: NoteDao,
    private val context: Context,
    private val customBrushDao: CustomBrushDao,
    private val textureStore: CahierTextureBitmapStore,
) : NotesRepository {

    private val converters = Converters()

    override fun getAllNotesStream(): Flow<List<Note>> = notesDao.getAllNotes()

    override fun getNoteStream(id: Long): Flow<Note?> = notesDao.getNote(id)

    override suspend fun addNote(note: Note): Long {
        return notesDao.addNote(note)
    }

    override suspend fun deleteNote(note: Note) = notesDao.deleteNote(note)

    override suspend fun updateNote(note: Note) = notesDao.updateNote(note)

    private suspend fun getAllCustomBrushes(): List<CustomBrush> {
        val builtIn = CustomBrushes.getBrushes(context, textureStore)
        val dbEntities = customBrushDao.getAllCustomBrushesSync()

        val dbBrushes = dbEntities.mapNotNull { entity ->
            try {
                ByteArrayInputStream(entity.brushBytes).use { inputStream ->
                    val family = AndroidBrushFamilySerialization.decode(
                        inputStream,
                        maxVersion = Version.DEVELOPMENT,
                        BrushFamilyDecodeCallback { id, bitmap ->
                            if (bitmap != null && textureStore[id] == null) textureStore.loadTexture(
                                id,
                                bitmap
                            )
                            id
                        }
                    )
                    CustomBrush(entity.name, com.example.cahier.R.drawable.edit_24px, family, true)
                }
            } catch (e: Exception) {
                null
            }
        }
        return builtIn + dbBrushes
    }

    override suspend fun updateNoteStrokes(
        noteId: Long,
        strokes: List<Stroke>,
        clientBrushFamilyId: String?,
    ) {
        val customBrushes = getAllCustomBrushes()
        val strokesData = strokes.map { converters.serializeStroke(it, customBrushes) }
        val strokesJson = Json.encodeToString(strokesData)

        val note = notesDao.getNoteById(noteId)
        if (note != null) {
            val updatedNote =
                note.copy(strokesData = strokesJson, clientBrushFamilyId = clientBrushFamilyId)
            notesDao.updateNote(updatedNote)
        }
    }

    override suspend fun getNoteStrokes(noteId: Long): List<Stroke> {
        val note = notesDao.getNoteById(noteId)
        val strokesJson = note?.strokesData ?: return emptyList()
        val customBrushes = getAllCustomBrushes()

        val strokesData = Json.decodeFromString<List<String>>(strokesJson)
        return strokesData.mapNotNull { converters.deserializeStrokeFromString(it, customBrushes) }
    }

    override suspend fun toggleFavorite(noteId: Long) {
        val note = notesDao.getNoteById(noteId)
        if (note != null) {
            val updatedNote = note.copy(isFavorite = !note.isFavorite)
            notesDao.updateNote(updatedNote)
        }
    }

    override suspend fun updateNoteImageUriList(noteId: Long, imageUriList: List<String>?) {
        val note = notesDao.getNoteById(noteId)
        if (note != null) {
            val updatedNote = note.copy(imageUriList = imageUriList)
            notesDao.updateNote(updatedNote)
        }
    }

}