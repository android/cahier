package com.example.cahier.developer.brushdesigner.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.cahier.developer.brushdesigner.data.BrushDesignerRepository
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class BrushDesignerViewModelTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Inject
    lateinit var customBrushDao: CustomBrushDao

    @Inject
    lateinit var repository: BrushDesignerRepository

    private lateinit var viewModel: BrushDesignerViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = BrushDesignerViewModel(context, repository, customBrushDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateTip_modifies_correct_coat_index() = runTest {
        assertEquals(
            1.0f,
            viewModel.activeBrushProto.value.getCoats(0).tip.scaleX,
            0.01f
        )

        viewModel.updateTip { it.setScaleX(2.5f) }

        assertEquals(
            2.5f,
            viewModel.activeBrushProto.value.getCoats(0).tip.scaleX,
            0.01f
        )
    }

    @Test
    fun addBehavior_stacks_multiple_rules() = runTest {
        viewModel.clearBehaviors()
        assertEquals(
            0,
            viewModel.activeBrushProto.value.getCoats(0).tip.behaviorsCount
        )

        val node1 = ink.proto.BrushBehavior.Node.newBuilder().setConstantNode(
            ink.proto.BrushBehavior.ConstantNode.newBuilder().setValue(1f)
        ).build()
        viewModel.addBehavior(listOf(node1))

        val node2 = ink.proto.BrushBehavior.Node.newBuilder().setConstantNode(
            ink.proto.BrushBehavior.ConstantNode.newBuilder().setValue(2f)
        ).build()
        viewModel.addBehavior(listOf(node2))

        val behaviors = viewModel.activeBrushProto.value.getCoats(0).tip.behaviorsList
        assertEquals(2, behaviors.size)
    }

    @Test
    fun addNewCoat_increments_count_and_switches_selection() = runTest {
        assertEquals(1, viewModel.activeBrushProto.value.coatsCount)
        assertEquals(0, viewModel.selectedCoatIndex.value)

        viewModel.addNewCoat()

        assertEquals(2, viewModel.activeBrushProto.value.coatsCount)
        assertEquals(1, viewModel.selectedCoatIndex.value)
    }

    @Test
    fun saveToPalette_persists_to_dao() = runTest {
        val brushName = "Test Persistence Brush"
        viewModel.updateClientBrushFamilyId("test-id")

        viewModel.saveToPalette(brushName)

        kotlinx.coroutines.delay(500)

        val savedBrushes = customBrushDao.getAllCustomBrushes().first()
        assertTrue(savedBrushes.any { it.name == brushName })
    }

    @Test
    fun previewBrushFamily_is_null_on_invalid_proto() = runTest {
        val invalidRepo = BrushDesignerRepository()
        invalidRepo.updateActiveBrushProto(ink.proto.BrushFamily.newBuilder().build())

        val vm = BrushDesignerViewModel(
            ApplicationProvider.getApplicationContext(),
            invalidRepo,
            customBrushDao
        )

        assertNull(vm.previewBrushFamily.value)
    }
}
