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

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cahier.core.data.FakeNotesRepository
import com.example.cahier.features.home.HomePane
import com.example.cahier.features.home.viewmodel.HomeScreenViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CahierListDetailTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeViewModel = HomeScreenViewModel(FakeNotesRepository())

    @Test
    fun homeScreen_showListOnly() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(compatWidthWindow)
            ) {
                HomeContent(forceCompact = true)
            }
        }

        composeTestRule.onNodeWithTag("List").assertExists()
        composeTestRule.onNodeWithTag("Detail").assertDoesNotExist()
    }

    @Test
    fun homeScreen_showListAndDetail() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(mediumWidthWindow)
            ) {
                HomeContent(forceCompact = false)
            }
        }

        composeTestRule.onNodeWithTag("List").assertExists()
        composeTestRule.onNodeWithTag("Detail").assertExists()
    }


    private val mediumWidthWindow = DpSize(
        width = 1200.dp,
        height = 900.dp
    )

    private val compatWidthWindow = DpSize(
        width = 400.dp,
        height = 900.dp
    )

    @Composable
    private fun HomeContent(forceCompact: Boolean) {
        HomePane(
            navigateToCanvas = { _ -> },
            navigateToDrawingCanvas = { _ -> },
            navigateToBrushGraph = {},
            navigateUp = {},
            forceCompact = forceCompact,
            homeScreenViewModel = fakeViewModel
        )
    }
}