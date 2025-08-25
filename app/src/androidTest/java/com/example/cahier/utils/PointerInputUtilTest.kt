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


package com.example.cahier.utils

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PointerInputUtilTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pointerInputWithSiblingFallthrough_allowsEventToPassToSibling() {
        var bottomBoxWasClicked = false
        var topBoxWasClicked = false

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("root")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                bottomBoxWasClicked = true
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInputWithSiblingFallthrough {
                            topBoxWasClicked = true
                        }
                )
            }
        }

        composeTestRule.onNodeWithTag("root").performClick()


        assertTrue("Top box should have been clicked", topBoxWasClicked)
        assertTrue(
            "Bottom box should have received the fallthrough click",
            bottomBoxWasClicked
        )
    }

    @Test
    fun standardPointerInput_doesNotAllowEventToPassToSibling() {
        var bottomBoxWasClicked = false
        var topBoxWasClicked = false

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("root")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                bottomBoxWasClicked = true
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                topBoxWasClicked = true
                            }
                        }
                )
            }
        }

        composeTestRule.onNodeWithTag("root").performClick()

        assertTrue("Top box should have been clicked", topBoxWasClicked)
        assertFalse(
            "Bottom box should NOT have received the click",
            bottomBoxWasClicked
        )
    }
}