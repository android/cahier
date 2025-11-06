package com.example.cahier

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.ui.CahierApp
import com.example.cahier.ui.HomePane
import com.example.cahier.ui.viewmodels.HomeScreenViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
                DeviceConfigurationOverride.ForcedSize(
                    DpSize(
                        width = 400.dp,
                        height = 900.dp
                    )
                )
            ) {
                HomePane(
                    navigateToCanvas = { _ -> },
                    navigateToDrawingCanvas = { _ -> },
                    navigateUp = {},
                    homeScreenViewModel = fakeViewModel
                )
            }
        }

        composeTestRule.onNodeWithTag("List").assertExists()
        composeTestRule.onNodeWithTag("Detail").assertDoesNotExist()
    }

    @Test
    fun homeScreen_showListAndDetail() {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(
                    DpSize(
                        width = 1200.dp,
                        height = 900.dp
                    )
                )
            ) {
                HomePane(
                    navigateToCanvas = { _ -> },
                    navigateToDrawingCanvas = { _ -> },
                    navigateUp = {},
                    homeScreenViewModel = fakeViewModel
                )
            }
        }

        composeTestRule.onNodeWithTag("List").assertExists()
        composeTestRule.onNodeWithTag("Detail").assertExists()
    }
}