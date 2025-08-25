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
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.data.Note
import com.example.cahier.navigation.TextCanvasDestination
import com.example.cahier.utils.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class CanvasScreenViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: CanvasScreenViewModel
    private lateinit var notesRepository: FakeNotesRepository
    private val fileHelper: FileHelper = mock()
    private var noteId: Long = 0L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        notesRepository = FakeNotesRepository()
        noteId = runBlocking {
            notesRepository.addNote(Note(title = "Initial Title", text = "Initial Text"))
        }
        val savedStateHandle = SavedStateHandle(
            mapOf(TextCanvasDestination.NOTE_ID_ARG to noteId)
        )
        viewModel = CanvasScreenViewModel(
            savedStateHandle, notesRepository, fileHelper
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateNoteTitle_updates_note_in_repository() = runTest {
        val newTitle = "Updated Title"
        viewModel.updateNoteTitle(newTitle)

        notesRepository.getNoteStream(noteId).test {
            assertEquals(newTitle, awaitItem().title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateNoteText_updates_note_in_repository() = runTest {
        val newText = "Updated text."
        viewModel.updateNoteText(newText)

        notesRepository.getNoteStream(noteId).test {
            assertEquals(newText, awaitItem().text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFavorite_toggles_favorite_status_in_repository() = runTest {
        viewModel.toggleFavorite()

        notesRepository.getNoteStream(noteId).test {
            assertTrue(awaitItem().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }
}