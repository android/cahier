package com.example.cahier.ui.brushgraph

import androidx.compose.ui.geometry.Offset
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.TutorialAction
import com.example.cahier.ui.brushgraph.model.TutorialAnchor
import com.example.cahier.ui.brushgraph.model.TutorialStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import com.example.cahier.ui.brushdesigner.CustomBrushDao
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrushGraphViewModelTest {

    private lateinit var viewModel: BrushGraphViewModel
    private val mockDao = mock(CustomBrushDao::class.java)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = BrushGraphViewModel(context, mockDao)
    }

    @Test
    fun testTutorialStart() {
        assertNull(viewModel.tutorialStep)
        viewModel.startTutorial()
        assertNotNull(viewModel.tutorialStep)
        assertEquals("Welcome & Goal", viewModel.tutorialStep?.title)
    }

    @Test
    fun testTutorialAdvance() {
        viewModel.startTutorial()
        assertEquals(0, viewModel.currentStepIndex)
        viewModel.advanceTutorial()
        assertEquals(1, viewModel.currentStepIndex)
        assertEquals("Create a Tip", viewModel.tutorialStep?.title)
    }
}
