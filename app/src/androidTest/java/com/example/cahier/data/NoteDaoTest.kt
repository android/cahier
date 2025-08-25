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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NoteDaoTest {

    private lateinit var noteDao: NoteDao
    private lateinit var db: NoteDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
        noteDao = db.noteDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun add_note_and_get_it_back() = runBlocking {
        val note = Note(id = 1, title = "Test Note", text = "This is a test.")
        noteDao.addNote(note)
        val allNotes = noteDao.getAllNotes().first()
        assertEquals(allNotes[0], note)
    }

    @Test
    @Throws(Exception::class)
    fun update_note_and_check_if_updated() = runBlocking {
        val note = Note(id = 1, title = "Original Title")
        noteDao.addNote(note)
        val updatedNote = Note(id = 1, title = "Updated Title")
        noteDao.updateNote(updatedNote)
        val retrievedNote = noteDao.getNote(1).first()
        assertEquals(retrievedNote.title, "Updated Title")
    }

    @Test
    @Throws(Exception::class)
    fun delete_note_and_check_if_it_is_gone() = runBlocking {
        val note = Note(id = 1, title = "To Be Deleted")
        noteDao.addNote(note)
        noteDao.deleteNote(note)
        val allNotes = noteDao.getAllNotes().first()
        assertEquals(0, allNotes.size)
    }
}