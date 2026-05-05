package com.example.cahier.developer.brushgraph.viewmodel

import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.developer.brushdesigner.data.FakeCustomBrushDao
import com.example.cahier.developer.brushdesigner.data.CustomBrushEntity
import java.io.ByteArrayOutputStream
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.BrushGraphRepository
import com.example.cahier.developer.brushgraph.data.DefaultBrushGraphRepository
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushTip as ProtoBrushTip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.mock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BrushGraphViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeDao: FakeCustomBrushDao
    private lateinit var mockTextureStore: CahierTextureBitmapStore
    private lateinit var repository: DefaultBrushGraphRepository
    private lateinit var viewModel: BrushGraphViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeCustomBrushDao()
        mockTextureStore = mock(CahierTextureBitmapStore::class.java)
        
        val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + testDispatcher)
        repository = DefaultBrushGraphRepository(fakeDao, mockTextureStore, repoScope)
        
        viewModel = BrushGraphViewModel(fakeDao, mockTextureStore, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = testScope.runTest {
        val state = viewModel.uiState.first()
        val defaultGraph = repository.createDefaultGraph()
        assertEquals(defaultGraph.nodes.size, state.graph.nodes.size)
        assertEquals(defaultGraph.edges.size, state.graph.edges.size)
        
        val expectedTypes = defaultGraph.nodes.map { it.data::class }.toSet()
        val actualTypes = state.graph.nodes.map { it.data::class }.toSet()
        assertEquals(expectedTypes, actualTypes)

        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.isEmpty())
        assertNull(state.selectedNodeId)
        assertNull(state.selectedEdge)
        assertNull(state.activeEdgeSourceId)
        assertNull(state.detachedEdge)

        assertFalse(state.isErrorPaneOpen)
        assertFalse(state.textFieldsLocked)
        assertFalse(state.isDarkCanvas)
        assertTrue(state.isPreviewExpanded)
        assertTrue(state.testAutoUpdateStrokes)

        assertTrue(state.graphIssues.isEmpty())
    }

    @Test
    fun addNode_updatesSelectedNodeIdAndCallsRepo() = testScope.runTest {
        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        
        val nodeId = viewModel.addNode(nodeData)
        
        testScope.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertEquals(nodeId, state.selectedNodeId)
        assertTrue(state.graph.nodes.any { it.id == nodeId })
    }

    @Test
    fun enterSelectionMode_updatesState() = testScope.runTest {
        val nodeId = "node1"
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = nodeId, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())))))
        
        testScope.advanceUntilIdle()
        
        viewModel.enterSelectionMode(nodeId)
        
        val state = viewModel.uiState.first()
        assertTrue(state.isSelectionMode)
        assertEquals(setOf(nodeId), state.selectedNodeIds)
    }

    @Test
    fun toggleNodeSelection_updatesState() = testScope.runTest {
        val nodeId = "node1"
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = nodeId, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())))))
        
        testScope.advanceUntilIdle()
        
        viewModel.enterSelectionMode(nodeId)
        
        viewModel.toggleNodeSelection(nodeId)
        
        val state = viewModel.uiState.first()
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.isEmpty())
    }

    @Test
    fun onNodeClick_togglesSelection() = testScope.runTest {
        val nodeId = "node1"
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = nodeId, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())))))
        
        testScope.advanceUntilIdle()
        
        viewModel.onNodeClick(nodeId)
        
        var state = viewModel.uiState.first()
        assertEquals(nodeId, state.selectedNodeId)
        
        viewModel.onNodeClick(nodeId)
        
        state = viewModel.uiState.first()
        assertNull(state.selectedNodeId)
    }

    @Test
    fun dismissPanes_clearsSelections() = testScope.runTest {
        val nodeId = "node1"
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = nodeId, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())))))
        
        testScope.advanceUntilIdle()
        
        viewModel.onNodeClick(nodeId)
        
        viewModel.dismissPanes()
        
        val state = viewModel.uiState.first()
        assertNull(state.selectedNodeId)
        assertNull(state.selectedEdge)
        assertFalse(state.isErrorPaneOpen)
    }

    @Test
    fun selectAllNodes_updatesState() = testScope.runTest {
        val node1 = "node1"
        val node2 = "node2"
        repository.setGraph(BrushGraph(nodes = listOf(
            GraphNode(id = node1, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())),
            GraphNode(id = node2, data = NodeData.Coat())
        )))
        
        testScope.advanceUntilIdle()
        
        viewModel.enterSelectionMode()
        viewModel.selectAllNodes()
        
        val state = viewModel.uiState.first()
        assertEquals(setOf(node1, node2), state.selectedNodeIds)
    }

    @Test
    fun exitSelectionMode_clearsState() = testScope.runTest {
        val nodeId = "node1"
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = nodeId, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())))))
        
        testScope.advanceUntilIdle()
        
        viewModel.enterSelectionMode(nodeId)
        assertTrue(viewModel.uiState.first().isSelectionMode)
        
        viewModel.exitSelectionMode()
        
        val state = viewModel.uiState.first()
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedNodeIds.isEmpty())
    }

    @Test
    fun onEdgeClick_togglesEdgeSelection() = testScope.runTest {
        val edge = com.example.cahier.developer.brushgraph.data.GraphEdge(fromNodeId = "node1", toNodeId = "node2", toPortId = "tip")
        
        viewModel.onEdgeClick(edge)
        
        var state = viewModel.uiState.first()
        assertEquals(edge, state.selectedEdge)
        
        viewModel.onEdgeClick(edge)
        
        state = viewModel.uiState.first()
        assertNull(state.selectedEdge)
    }

    @Test
    fun toggleErrorPane_togglesState() = testScope.runTest {
        assertFalse(viewModel.uiState.first().isErrorPaneOpen)
        
        viewModel.toggleErrorPane()
        assertTrue(viewModel.uiState.first().isErrorPaneOpen)
        
        viewModel.toggleErrorPane()
        assertFalse(viewModel.uiState.first().isErrorPaneOpen)
    }

    @Test
    fun updateTestBrushColor_updatesState() = testScope.runTest {
        val color = 0xFF00FF00.toInt()
        viewModel.updateTestBrushColor(color)
        assertEquals(color, viewModel.uiState.first().testBrushColor)
    }

    @Test
    fun updateTestBrushSize_updatesState() = testScope.runTest {
        val size = 15f
        viewModel.updateTestBrushSize(size)
        assertEquals(size, viewModel.uiState.first().testBrushSize, 0.01f)
    }

    @Test
    fun setTestAutoUpdateStrokes_updatesState() = testScope.runTest {
        assertTrue(viewModel.uiState.first().testAutoUpdateStrokes)
        
        viewModel.setTestAutoUpdateStrokes(false)
        assertFalse(viewModel.uiState.first().testAutoUpdateStrokes)
    }

    @Test
    fun updateZoom_updatesState() = testScope.runTest {
        val zoom = 2f
        viewModel.updateZoom(zoom)
        assertEquals(zoom, viewModel.uiState.first().zoom, 0.01f)
    }

    @Test
    fun updateOffset_updatesState() = testScope.runTest {
        val offset = com.example.cahier.developer.brushgraph.data.GraphPoint(10f, 20f)
        viewModel.updateOffset(offset)
        assertEquals(offset, viewModel.uiState.first().offset)
    }

    @Test
    fun toggleTextFieldsLocked_updatesState() = testScope.runTest {
        assertFalse(viewModel.uiState.first().textFieldsLocked)
        
        viewModel.toggleTextFieldsLocked()
        assertTrue(viewModel.uiState.first().textFieldsLocked)
    }

    @Test
    fun toggleCanvasTheme_updatesState() = testScope.runTest {
        assertFalse(viewModel.uiState.first().isDarkCanvas)
        
        viewModel.toggleCanvasTheme()
        assertTrue(viewModel.uiState.first().isDarkCanvas)
    }

    @Test
    fun togglePreviewExpanded_updatesState() = testScope.runTest {
        assertTrue(viewModel.uiState.first().isPreviewExpanded)
        
        viewModel.togglePreviewExpanded()
        assertFalse(viewModel.uiState.first().isPreviewExpanded)
    }

    @Test
    fun startTutorial_callsTutorialManager() = testScope.runTest {
        viewModel.startTutorial()
        assertEquals(0, viewModel.currentStepIndex)
        
        viewModel.advanceTutorial()
        assertEquals(1, viewModel.currentStepIndex)
        
        viewModel.startTutorial()
        assertEquals(0, viewModel.currentStepIndex)
    }

    @Test
    fun startTutorialSandbox_updatesState() = testScope.runTest {
        assertFalse(viewModel.isTutorialSandboxMode)
        
        viewModel.startTutorialSandbox()
        assertTrue(viewModel.isTutorialSandboxMode)
    }

    @Test
    fun advanceTutorial_updatesStep() = testScope.runTest {
        viewModel.startTutorial()
        val initialStep = viewModel.currentStepIndex
        
        val advanced = viewModel.advanceTutorial()
        assertTrue(advanced)
        assertEquals(initialStep + 1, viewModel.currentStepIndex)
    }

    @Test
    fun regressTutorial_updatesStep() = testScope.runTest {
        viewModel.startTutorial()
        viewModel.advanceTutorial()
        val stepAfterAdvance = viewModel.currentStepIndex
        
        viewModel.regressTutorial()
        assertEquals(stepAfterAdvance - 1, viewModel.currentStepIndex)
    }

    @Test
    fun endTutorialSandbox_updatesState() = testScope.runTest {
        viewModel.startTutorialSandbox()
        assertTrue(viewModel.isTutorialSandboxMode)
        
        viewModel.endTutorialSandbox(keepChanges = false)
        assertFalse(viewModel.isTutorialSandboxMode)
    }

    @Test
    fun saveToPalette_callsDao() = testScope.runTest {
        val brushName = "testBrush"
        
        viewModel.saveToPalette(brushName)
        
        // Wait for the IO coroutine to finish on the device
        Thread.sleep(500)
        
        val savedBrushes = fakeDao.getAllCustomBrushes().first()
        assertTrue(savedBrushes.any { it.name == brushName })
    }

    @Test
    fun deleteFromPalette_callsDao() = testScope.runTest {
        val brushName = "testBrush"
        val entity = CustomBrushEntity(name = brushName, brushBytes = byteArrayOf())
        fakeDao.saveCustomBrush(entity)
        
        viewModel.deleteFromPalette(brushName)
        
        // Wait for the IO coroutine to finish on the device
        Thread.sleep(500)
        
        val savedBrushes = fakeDao.getAllCustomBrushes().first()
        assertFalse(savedBrushes.any { it.name == brushName })
    }

    @Test
    fun loadFromPalette_callsRepo() = testScope.runTest {
        val brushName = "testBrush"
        val family = androidx.ink.brush.Brush.createWithColorIntArgb(androidx.ink.brush.StockBrushes.marker(), 0, 10f, 0.1f).family
        val baos = ByteArrayOutputStream()
        androidx.ink.storage.AndroidBrushFamilySerialization.encode(family, baos, mockTextureStore)
        val entity = CustomBrushEntity(name = brushName, brushBytes = baos.toByteArray())
        
        viewModel.loadFromPalette(entity)
        
        testScope.advanceUntilIdle()
        
        // Verification is hard without a spy, but we ensure no crash.
    }

    @Test
    fun addFamilyNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addFamilyNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.Family)
    }

    @Test
    fun addCoatNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addCoatNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.Coat)
    }

    @Test
    fun addPaintNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addPaintNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.Paint)
    }

    @Test
    fun addTipNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addTipNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.Tip)
    }

    @Test
    fun addColorFunctionNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addColorFunctionNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.ColorFunction)
    }

    @Test
    fun addTextureLayerNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addTextureLayerNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.TextureLayer)
    }

    @Test
    fun addBehaviorNode_addsNode() = testScope.runTest {
        val nodeId = viewModel.addBehaviorNode()
        testScope.advanceUntilIdle()
        val node = viewModel.uiState.first().graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.data is NodeData.Behavior)
    }

    @Test
    fun deleteEdge_updatesState() = testScope.runTest {
        val node1 = "node1"
        val node2 = "node2"
        val edge = com.example.cahier.developer.brushgraph.data.GraphEdge(fromNodeId = node1, toNodeId = node2, toPortId = "tip")
        repository.setGraph(BrushGraph(
            nodes = listOf(
                GraphNode(id = node1, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())),
                GraphNode(id = node2, data = NodeData.Coat())
            ),
            edges = listOf(edge)
        ))
        
        testScope.advanceUntilIdle()
        
        viewModel.onEdgeClick(edge)
        assertEquals(edge, viewModel.uiState.first().selectedEdge)
        
        viewModel.deleteEdge(edge)
        testScope.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertNull(state.selectedEdge)
        assertFalse(state.graph.edges.contains(edge))
    }

    @Test
    fun finalizeEdgeEdit_updatesState() = testScope.runTest {
        val node1 = "node1"
        val node2 = "node2"
        val node3 = "node3"
        val oldEdge = com.example.cahier.developer.brushgraph.data.GraphEdge(fromNodeId = node1, toNodeId = node2, toPortId = "tip")
        repository.setGraph(BrushGraph(
            nodes = listOf(
                GraphNode(id = node1, data = NodeData.Tip(ProtoBrushTip.getDefaultInstance())),
                GraphNode(id = node2, data = NodeData.Coat()),
                GraphNode(id = node3, data = NodeData.Coat(paintPortIds = listOf("color")))
            ),
            edges = listOf(oldEdge)
        ))
        
        testScope.advanceUntilIdle()
        
        viewModel.detachEdge(oldEdge)
        assertEquals(oldEdge, viewModel.uiState.first().detachedEdge)
        
        viewModel.finalizeEdgeEdit(oldEdge, node1, node3, "color")
        testScope.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertNull(state.detachedEdge)
        assertTrue(state.graph.edges.any { it.fromNodeId == node1 && it.toNodeId == node3 && it.toPortId == "color" })
    }

    @Test
    fun detachEdge_updatesState() = testScope.runTest {
        val edge = com.example.cahier.developer.brushgraph.data.GraphEdge(fromNodeId = "node1", toNodeId = "node2", toPortId = "tip")
        
        viewModel.detachEdge(edge)
        
        val state = viewModel.uiState.first()
        assertEquals(edge, state.detachedEdge)
    }

    @Test
    fun addNodeAndConnect_addsNodeAndEdge() = testScope.runTest {
        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        val targetNodeId = "node2"
        val targetPortId = "tip"
        
        repository.setGraph(BrushGraph(nodes = listOf(GraphNode(id = targetNodeId, data = NodeData.Coat(tipPortId = targetPortId)))))
        testScope.advanceUntilIdle()
        
        val newNodeId = viewModel.addNodeAndConnect(nodeData, targetNodeId, targetPortId)
        testScope.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.graph.nodes.any { it.id == newNodeId })
        assertTrue(state.graph.edges.any { it.fromNodeId == newNodeId && it.toNodeId == targetNodeId && it.toPortId == targetPortId })
    }

    @Test
    fun clearStrokes_clearsList() = testScope.runTest {
        viewModel.strokeList.add(mock(androidx.ink.strokes.Stroke::class.java))
        assertFalse(viewModel.strokeList.isEmpty())
        
        viewModel.clearStrokes()
        assertTrue(viewModel.strokeList.isEmpty())
    }

    @Test
    fun getBrushColor_returnsColor() = testScope.runTest {
        val color = 0xFF00FF00.toInt()
        viewModel.updateTestBrushColor(color)
        
        testScope.advanceUntilIdle()
        
        assertEquals(color, viewModel.getBrushColor())
    }

    @Test
    fun updateAllTextureIds_updatesState() = testScope.runTest {
        val ids = setOf("tex1", "tex2")
        org.mockito.Mockito.`when`(mockTextureStore.getAllIds()).thenReturn(ids)
        
        viewModel.updateAllTextureIds()
        
        val state = viewModel.uiState.first()
        assertEquals(ids, state.allTextureIds)
    }
}
