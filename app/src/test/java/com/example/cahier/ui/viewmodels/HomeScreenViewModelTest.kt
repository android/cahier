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


package com.example.cahier.ui.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HomeScreenViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: HomeScreenViewModel
    private lateinit var notesRepository: FakeNotesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        notesRepository = FakeNotesRepository()
        viewModel = HomeScreenViewModel(notesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectNote_updates_uiState_with_the_correct_note() = runTest {
        val noteId = runBlocking {
            notesRepository.addNote(
                Note(
                    title = "Selected Note",
                    type = NoteType.DRAWING
                )
            )
        }

        viewModel.selectNote(noteId)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(noteId, state.note.id)
            assertEquals("Selected Note", state.note.title)
            assertNotNull(state.strokes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addDrawingNote_adds_a_drawing_note_to_repository() = runTest {
        var createdNoteId: Long? = null
        viewModel.addDrawingNote { id -> createdNoteId = id }

        val notes = notesRepository.getNotes()
        assertEquals(1, notes.size)
        assertEquals(NoteType.DRAWING, notes.first().type)
        assertEquals(createdNoteId, notes.first().id)
    }

    @Test
    fun deleteNote_removes_note_from_repository() = runTest {
        val noteId = runBlocking { notesRepository.addNote(Note(title = "To Delete")) }
        val noteToDelete = notesRepository.getNotes().first { it.id == noteId }

        viewModel.deleteNote(noteToDelete)

        assertTrue(notesRepository.getNotes().isEmpty())
    }

    @Test
    fun toggleFavorite_toggles_favorite_status_in_repository() = runTest {
        val noteId =
            runBlocking { notesRepository.addNote(Note(title = "My Note", isFavorite = false)) }

        viewModel.toggleFavorite(noteId)

        notesRepository.getNoteStream(noteId).test {
            assertTrue(awaitItem().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearSelection_clears_selection_in_uiState() = runTest {
        val noteId = runBlocking { notesRepository.addNote(Note(title = "A Note")) }
        viewModel.selectNote(noteId)

        viewModel.clearSelection()

        viewModel.uiState.test {
            assertEquals(Note(), awaitItem().note)
            cancelAndIgnoreRemainingEvents()
        }
    }
}