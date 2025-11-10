package com.example.cahier


import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.ui.HomePane
import com.example.cahier.ui.viewmodels.HomeScreenViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeViewModel = HomeScreenViewModel(FakeNotesRepository())

    @Composable
    private fun HomeContent() {
        HomePane(
            navigateToCanvas = { _ -> },
            navigateToDrawingCanvas = { _ -> },
            navigateUp = {},
            homeScreenViewModel = fakeViewModel
        )
    }

    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    @Test
    fun globalNavigation_showNavRail() {
        composeTestRule.setContent { HomeContent() }

        composeTestRule.onRoot().captureRoboImage("reference_screenshot_navrail.png")
    }

    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7Pro)
    @Test
    fun globalNavigation_showBottomNavBar() {
        composeTestRule.setContent { HomeContent() }

        composeTestRule.onRoot().captureRoboImage("reference_screenshot_bottomnavbar.png")
    }
}