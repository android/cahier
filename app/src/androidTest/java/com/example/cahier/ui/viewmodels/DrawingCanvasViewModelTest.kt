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

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.graphics.Color
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.composeColor
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.navigation.DrawingCanvasDestination
import com.example.cahier.utils.FileHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
class DrawingCanvasViewModelTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Inject
    lateinit var fileHelper: FileHelper

    @Inject
    lateinit var imageLoader: ImageLoader

    private lateinit var notesRepository: FakeNotesRepository
    private lateinit var viewModel: DrawingCanvasViewModel
    private var noteId: Long = 0L

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        notesRepository = FakeNotesRepository()
        noteId = runBlocking {
            notesRepository.addNote(com.example.cahier.data.Note(title = "Drawing Test"))
        }

        val savedStateHandle = SavedStateHandle(
            mapOf(DrawingCanvasDestination.NOTE_ID_ARG to noteId)
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = DrawingCanvasViewModel(
            context, savedStateHandle, notesRepository, fileHelper, imageLoader
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_is_correct() = runTest {
        assertEquals("Drawing Test", viewModel.uiState.value.note.title)
        assertFalse(viewModel.isEraserMode.value)
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
    }

    @Test
    fun setEraserMode_updates_isEraserMode_state() = runTest {
        viewModel.setEraserMode(true)
        assertTrue(viewModel.isEraserMode.value)

        viewModel.setEraserMode(false)
        assertFalse(viewModel.isEraserMode.value)
    }

    @Test
    fun changeBrush_updates_selectedBrush_state() = runTest {
        val initialBrush = viewModel.getCurrentBrush()
        viewModel.changeBrushAndSize(StockBrushes.highlighterLatest, 20f)

        val newBrush = viewModel.getCurrentBrush()
        assertNotEquals(initialBrush.family, newBrush.family)
        assertEquals(StockBrushes.highlighterLatest, newBrush.family)
        assertEquals(20f, newBrush.size, 0.01f)
        assertTrue(newBrush.composeColor.alpha < 1.0f)
    }

    @Test
    fun changeBrushColor_updates_selectedBrush_state() = runTest {
        viewModel.changeBrushColor(Color.Red)
        assertEquals(
            Color.Red.value,
            viewModel.getCurrentBrush().composeColor.value
        )
    }

    @Test
    fun changeBrushSize_updates_selectedBrush_state() = runTest {
        viewModel.changeBrushSize(15f)
        assertEquals(15f, viewModel.getCurrentBrush().size, 0.01f)
    }

    @Test
    fun highlighter_brush_has_reduced_alpha() = runTest {
        viewModel.changeBrushAndSize(StockBrushes.highlighterLatest, 20f)
        val highlighterBrush = viewModel.getCurrentBrush()
        assertTrue(highlighterBrush.composeColor.alpha < 1.0f)

        viewModel.changeBrushAndSize(StockBrushes.markerLatest, 10f)
        val markerBrush = viewModel.getCurrentBrush()
        assertEquals(1.0f, markerBrush.composeColor.alpha, 0.01f)
    }

    @Test
    fun undo_restores_previous_strokes() = runTest {
        val brush = Brush(StockBrushes.markerLatest, 10f, 1f)
        val stroke1 = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

        viewModel.onStrokesFinished(listOf(stroke1))

        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
        assertEquals(1, viewModel.uiState.value.strokes.size)

        viewModel.undo()

        assertFalse(viewModel.canUndo.value)
        assertTrue(viewModel.canRedo.value)
        assertEquals(0, viewModel.uiState.value.strokes.size)

        viewModel.redo()

        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
        assertEquals(1, viewModel.uiState.value.strokes.size)
    }

    @Test
    fun clearStrokes_clears_and_saves_empty_strokes() = runTest {
        val brush = Brush(StockBrushes.markerLatest, 10f, 1f)
        val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

        viewModel.onStrokesFinished(listOf(stroke))
        assertEquals(1, viewModel.uiState.value.strokes.size)

        viewModel.clearStrokes()

        assertEquals(0, viewModel.uiState.value.strokes.size)

        val note = notesRepository.getNoteStream(noteId).first()
        assertTrue(note.strokesData.isNullOrEmpty())
    }
}