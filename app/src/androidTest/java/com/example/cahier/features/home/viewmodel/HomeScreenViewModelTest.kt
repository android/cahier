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

package com.example.cahier.features.home.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.cahier.core.data.FakeNotesRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device tests for [HomeScreenViewModel] that need real [Stroke]s, which require Ink's native
 * library and so can't run in JVM tests.
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@HiltAndroidTest
class HomeScreenViewModelTest {

  @get:Rule var hiltRule = HiltAndroidRule(this)

  @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var viewModel: HomeScreenViewModel
  private lateinit var notesRepository: FakeNotesRepository

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    notesRepository = FakeNotesRepository()
    viewModel = HomeScreenViewModel(noteRepository = notesRepository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun selectedNote_showsStrokesSavedWhileUnsubscribed() = runTest {
    var createdNoteId = 0L
    viewModel.addDrawingNote { id -> createdNoteId = id }
    viewModel.selectNote(createdNoteId)
    val savedStrokes =
      listOf(Stroke(Brush(StockBrushes.marker(), 10f, 1f), ImmutableStrokeInputBatch.EMPTY))

    notesRepository.updateNoteStrokes(createdNoteId, savedStrokes, null)

    viewModel.uiState.test {
      awaitItem()
      cancelAndIgnoreRemainingEvents()
    }
    assertEquals(savedStrokes, viewModel.uiState.value.strokes)
  }
}
