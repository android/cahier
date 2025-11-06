package com.example.cahier

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices.PIXEL_9
import androidx.compose.ui.tooling.preview.Devices.PIXEL_TABLET
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.example.cahier.data.FakeNotesRepository
import com.example.cahier.ui.HomePane
import com.example.cahier.ui.theme.CahierAppTheme
import com.example.cahier.ui.viewmodels.HomeScreenViewModel

class ScreenshotTesting {

    private val fakeViewModel = HomeScreenViewModel(FakeNotesRepository())

    @PreviewTest
    @Preview(name = "Compact Window", showSystemUi = true, device = PIXEL_9)
    @Preview(name = "Medium Window", showSystemUi = true, device = PIXEL_TABLET)
    @Composable
    fun CahierAppPreview() {
        CahierAppTheme {
            HomePane(
                navigateToDrawingCanvas = {_ -> },
                navigateToCanvas = {_ -> },
                navigateUp = {},
                homeScreenViewModel = fakeViewModel
            )
        }
    }
}