/*
 * Copyright 2026 Google LLC. All rights reserved.
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

package com.example.cahier

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cahier.core.data.CahierTextureBitmapStore
import com.example.cahier.core.data.FakeNotesRepository
import com.example.cahier.core.data.Note
import com.example.cahier.core.data.NoteType
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.features.home.HomePane
import com.example.cahier.features.home.viewmodel.HomeScreenViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CahierListDetailTest {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1) val composeTestRule = createComposeRule()

  private val notesRepository = FakeNotesRepository()
  private val fakeViewModel = HomeScreenViewModel(notesRepository)
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun homeScreen_showListOnly() {
    composeTestRule.setContent {
      DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(compatWidthWindow)) {
        HomeContent(forceCompact = true)
      }
    }

    composeTestRule.onNodeWithTag("List").assertExists()
    composeTestRule.onNodeWithTag("Detail").assertDoesNotExist()
  }

  @Test
  fun homeScreen_showListAndDetail() {
    composeTestRule.setContent {
      DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(mediumWidthWindow)) {
        HomeContent(forceCompact = false)
      }
    }

    composeTestRule.onNodeWithTag("List").assertExists()
    composeTestRule.onNodeWithTag("Detail").assertExists()
  }

  @Test
  fun addDrawingNote_withDetailPane_selectsNewNote() {
    // Start with another note selected so the test can tell whether the new note replaces it.
    val existingNoteId = runBlocking { notesRepository.addNote(Note(type = NoteType.Drawing)) }
    composeTestRule.setContent {
      DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(mediumWidthWindow)) {
        HomeContent(forceCompact = false)
      }
    }
    composeTestRule.waitForIdle()
    assertEquals(existingNoteId, fakeViewModel.uiState.value.note.id)

    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.add_note))
      .performClick()
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.drawing_note))
      .performClick()
    composeTestRule.waitForIdle()

    val newNote = notesRepository.getNotes().single { it.id != existingNoteId }
    assertEquals(newNote.id, fakeViewModel.uiState.value.note.id)
  }

  @Test
  fun addDrawingNote_compact_doesNotSelectNewNote() {
    composeTestRule.setContent {
      DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(compatWidthWindow)) {
        HomeContent(forceCompact = true)
      }
    }

    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.add_note))
      .performClick()
    composeTestRule
      .onNodeWithContentDescription(context.getString(R.string.drawing_note))
      .performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, notesRepository.getNotes().size)
    assertEquals(Note(), fakeViewModel.uiState.value.note)
    assertEquals(0, notesRepository.getNoteStrokesCallCount)
  }

  private val mediumWidthWindow = DpSize(width = 1200.dp, height = 900.dp)

  private val compatWidthWindow = DpSize(width = 400.dp, height = 900.dp)

  @Composable
  private fun HomeContent(forceCompact: Boolean) {
    CompositionLocalProvider(LocalTextureStore provides CahierTextureBitmapStore(context)) {
      HomePane(
        navigateToCanvas = { _ -> },
        navigateToDrawingCanvas = { _ -> },
        navigateToBrushGraph = {},
        navigateUp = {},
        forceCompact = forceCompact,
        homeScreenViewModel = fakeViewModel,
      )
    }
  }
}
