/*
 * Copyright 2025 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.features.drawing.viewmodel

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
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import com.example.cahier.core.data.CahierTextureBitmapStore
import com.example.cahier.core.data.FakeNotesRepository
import com.example.cahier.core.data.Note
import com.example.cahier.core.navigation.DrawingCanvasDestination
import com.example.cahier.core.utils.FileHelper
import com.example.cahier.developer.brushdesigner.data.FakeCustomBrushDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@HiltAndroidTest
class DrawingCanvasViewModelTest {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = UnconfinedTestDispatcher()

  @Inject lateinit var fileHelper: FileHelper

  @Inject lateinit var imageLoader: ImageLoader

  private lateinit var notesRepository: FakeNotesRepository
  private lateinit var viewModel: DrawingCanvasViewModel
  private var noteId: Long = 0L

  @Before
  fun setup() {
    hiltRule.inject()
    Dispatchers.setMain(testDispatcher)
    notesRepository = FakeNotesRepository()
    noteId = runBlocking { notesRepository.addNote(Note(title = "Drawing Test")) }

    viewModel = createViewModel(noteId)
  }

  private fun createViewModel(
    noteId: Long,
    applicationScope: CoroutineScope = CoroutineScope(testDispatcher),
  ): DrawingCanvasViewModel {
    val savedStateHandle = SavedStateHandle(mapOf(DrawingCanvasDestination.NOTE_ID_ARG to noteId))
    val context = ApplicationProvider.getApplicationContext<Context>()
    return DrawingCanvasViewModel(
      context,
      savedStateHandle,
      notesRepository,
      fileHelper,
      imageLoader,
      customBrushDao = FakeCustomBrushDao(),
      CahierTextureBitmapStore(context),
      ioDispatcher = testDispatcher,
      applicationScope = applicationScope,
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
    viewModel.changeBrushAndSize(StockBrushes.highlighter(), 20f)

    val newBrush = viewModel.getCurrentBrush()
    assertNotEquals(initialBrush.family, newBrush.family)
    assertEquals(StockBrushes.highlighter(), newBrush.family)
    assertEquals(20f, newBrush.size, 0.01f)
    assertTrue(newBrush.composeColor.alpha < 1.0f)
  }

  @Test
  fun changeBrushColor_updates_selectedBrush_state() = runTest {
    viewModel.changeBrushColor(Color.Red)
    assertEquals(Color.Red.value, viewModel.getCurrentBrush().composeColor.value)
  }

  @Test
  fun changeBrushColor_preserveAlpha_retains_custom_alpha() = runTest {
    val translucentBlue = Color(red = 0f, green = 0f, blue = 1f, alpha = 0.5f)
    viewModel.changeBrushColor(translucentBlue, preserveAlpha = true)
    assertEquals(0.5f, viewModel.getCurrentBrush().composeColor.alpha, 0.01f)

    viewModel.changeBrush(StockBrushes.highlighter())
    val customHighlighterColor = Color(red = 1f, green = 0f, blue = 0f, alpha = 0.6f)
    viewModel.changeBrushColor(customHighlighterColor, preserveAlpha = true)
    assertEquals(0.6f, viewModel.getCurrentBrush().composeColor.alpha, 0.01f)
  }

  @Test
  fun changeBrushSize_updates_selectedBrush_state() = runTest {
    viewModel.changeBrushSize(15f)
    assertEquals(15f, viewModel.getCurrentBrush().size, 0.01f)
  }

  @Test
  fun highlighter_brush_has_reduced_alpha() = runTest {
    viewModel.changeBrushAndSize(StockBrushes.highlighter(), 20f)
    val highlighterBrush = viewModel.getCurrentBrush()
    assertTrue(highlighterBrush.composeColor.alpha < 1.0f)

    viewModel.changeBrushAndSize(StockBrushes.marker(), 10f)
    val markerBrush = viewModel.getCurrentBrush()
    assertEquals(1.0f, markerBrush.composeColor.alpha, 0.01f)
  }

  @Test
  fun undo_restores_previous_strokes() = runTest {
    val brush = Brush(StockBrushes.marker(), 10f, 1f)
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
    val brush = Brush(StockBrushes.marker(), 10f, 1f)
    val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
    viewModel.onStrokesFinished(listOf(stroke))

    assertEquals(1, viewModel.uiState.value.strokes.size)
    assertEquals(1, notesRepository.getNoteStrokes(noteId).size)

    viewModel.clearStrokes()

    assertEquals(0, viewModel.uiState.value.strokes.size)
    val repoStrokes = notesRepository.getNoteStrokes(noteId)
    assertTrue("Strokes should have been cleared from the repository.", repoStrokes.isEmpty())
  }

  @Test
  fun changeBrush_disables_eraserMode() = runTest {
    viewModel.setEraserMode(true)
    assertTrue("Precondition failed: Eraser mode should be enabled.", viewModel.isEraserMode.value)

    viewModel.changeBrush(StockBrushes.marker())

    assertFalse("Calling changeBrush should disable eraser mode.", viewModel.isEraserMode.value)
  }

  @Test
  fun createExportedBitmap_returnsNullForEmptyBounds() = runTest {
    assertNull(viewModel.createExportedBitmap(0, 100))
    assertNull(viewModel.createExportedBitmap(100, 0))
  }

  @Test
  fun onStrokesFinished_doesNotReloadStrokesFromRepository() = runTest {
    assertEquals(1, notesRepository.getNoteStrokesCallCount)

    val brush = Brush(StockBrushes.marker(), 10f, 1f)
    val stroke1 = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
    val stroke2 = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

    viewModel.onStrokesFinished(listOf(stroke1))
    viewModel.onStrokesFinished(listOf(stroke2))
    viewModel.onTitleChanged("Updated Title")

    assertEquals(1, notesRepository.getNoteStrokesCallCount)
    assertEquals("Updated Title", viewModel.uiState.value.note.title)
    assertEquals(2, viewModel.uiState.value.strokes.size)
    assertSame(stroke1, viewModel.uiState.value.strokes[0])
    assertSame(stroke2, viewModel.uiState.value.strokes[1])
  }

  @Test
  fun init_restoresBrushSavedWithNote() = runTest {
    val savedBrush = viewModel.customBrushes.value.first()
    val brushNoteId =
      notesRepository.addNote(Note(title = "Brush Note", clientBrushFamilyId = savedBrush.name))

    val restoredViewModel = createViewModel(brushNoteId)

    assertEquals(savedBrush.brushFamily, restoredViewModel.currentBrush.value.family)
  }

  @Test
  fun onStrokesFinished_savesLatestStrokesInBackground() = runTest {
    val backgroundDispatcher = StandardTestDispatcher(testScheduler)
    val savingViewModel = createViewModel(noteId, CoroutineScope(backgroundDispatcher))
    val brush = Brush(StockBrushes.marker(), 10f, 1f)
    val stroke1 = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
    val stroke2 = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)

    savingViewModel.onStrokesFinished(listOf(stroke1))
    savingViewModel.onStrokesFinished(listOf(stroke2))
    assertEquals(0, notesRepository.updateNoteStrokesCallCount)

    advanceUntilIdle()
    // The save queued for stroke1 is skipped because a newer save was requested.
    assertEquals(1, notesRepository.updateNoteStrokesCallCount)
    assertEquals(listOf(stroke1, stroke2), notesRepository.getNoteStrokes(noteId))
  }
}
