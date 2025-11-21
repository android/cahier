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


package com.example.cahier

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.hasText

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CahierAppTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreen_displaysAndNavigatesToDrawing() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Add note")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Home").assertExists()
        composeTestRule.onNodeWithText("Settings").assertExists()


        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.onNodeWithContentDescription("Drawing note").performClick()

        composeTestRule.onNodeWithText("Drawing title", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithContentDescription("Brush").assertExists()
        composeTestRule.onNodeWithContentDescription("Color").assertExists()
        composeTestRule.onNodeWithContentDescription("Eraser").assertExists()
    }

    @Test
    fun homeScreen_navigateToSettings() {
        composeTestRule.onNodeWithText("Settings").assertExists().performClick()
        composeTestRule.onNodeWithText("Default notes app").assertExists()
    }

    @Test
    fun homeScreen_displaysAndNavigatesToText() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Add note")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.onNodeWithContentDescription("Text note").performClick()

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.title),
                useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.note),
                useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun textNoteCanvasScreen_savedState() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithContentDescription("Add note")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithContentDescription("Add note").performClick()
        composeTestRule.onNodeWithContentDescription("Text note").performClick()

        val titleText = "My test title"
        composeTestRule.onNode(hasSetTextAction() and
                hasText(composeTestRule.activity.getString(R.string.title)))
            .performTextInput(titleText)

        val noteText = "This is a test note."
        composeTestRule.onNode(hasSetTextAction() and
                hasText(composeTestRule.activity.getString(R.string.note)))
            .performTextInput(noteText)

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText(titleText, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText(noteText, useUnmergedTree = true).assertExists()
    }
}
