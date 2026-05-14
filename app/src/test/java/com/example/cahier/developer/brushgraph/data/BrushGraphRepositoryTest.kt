package com.example.cahier.developer.brushgraph.data

import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.developer.brushdesigner.data.FakeCustomBrushDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushTip as ProtoBrushTip

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BrushGraphRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeDao: FakeCustomBrushDao
    private lateinit var mockTextureStore: CahierTextureBitmapStore
    private lateinit var repository: DefaultBrushGraphRepository
    private lateinit var repoScope: kotlinx.coroutines.CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeCustomBrushDao()
        mockTextureStore = mock(CahierTextureBitmapStore::class.java)
        org.mockito.Mockito.`when`(mockTextureStore.generation).thenReturn(MutableStateFlow(0))
        repoScope = kotlinx.coroutines.CoroutineScope(testDispatcher + Job())
        repository = DefaultBrushGraphRepository(fakeDao, mockTextureStore, repoScope)
    }

    @After
    fun tearDown() {
        repoScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefaultGraph() = testScope.runTest {
        val graph = repository.graph.first()
        assertNotNull(graph)
        assertTrue(graph.nodes.any { it.data is NodeData.Family })
    }

    @Test
    fun addNode_updatesGraph() = testScope.runTest {
        val initialNodeCount = repository.graph.first().nodes.size

        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        val nodeId = repository.addNode(nodeData)

        val updatedGraph = repository.graph.first()
        assertEquals(initialNodeCount + 1, updatedGraph.nodes.size)
        assertTrue(updatedGraph.nodes.any { it.id == nodeId })
    }

    @Test
    fun deleteNode_updatesGraph() = testScope.runTest {
        val initialNodeCount = repository.graph.first().nodes.size

        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        val nodeId = repository.addNode(nodeData)

        val graphAfterAdd = repository.graph.first()
        assertEquals(initialNodeCount + 1, graphAfterAdd.nodes.size)
        assertTrue(graphAfterAdd.nodes.any { it.id == nodeId })

        repository.deleteNode(nodeId)

        val graphAfterDelete = repository.graph.first()
        assertEquals(initialNodeCount, graphAfterDelete.nodes.size)
        assertFalse(graphAfterDelete.nodes.any { it.id == nodeId })
    }

    @Test
    fun validate_detectsWarnings() = testScope.runTest {
        val behaviorNode = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.getDefaultInstance())
                .build(),
            inputPortIds = listOf("input1", "input2")
        )
        repository.addNode(behaviorNode)

        // Orphaned nodes result in warnings, not errors, so the graph is still technically valid.
        assertTrue(repository.validate())

        val issues = repository.graphIssues.first()
        assertTrue(issues.any { it.severity == ValidationSeverity.WARNING })
    }

    @Test
    fun validate_detectsErrors() = testScope.runTest {
        val coatId = repository.graph.first().nodes.find { it.data is NodeData.Coat }
        repository.deleteNode(coatId?.id!!)

        // No coat on the family is an error
        assertFalse(repository.validate())

        val issues = repository.graphIssues.first()
        assertTrue(issues.any { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun setGraph_updatesGraph() = testScope.runTest {
        val newGraph = BrushGraph(nodes = listOf(GraphNode(id = "node1", data = NodeData.Family())))
        repository.setGraph(newGraph)
        assertEquals(newGraph, repository.graph.first())
    }

    @Test
    fun clearGraph_resetsToDefault() = testScope.runTest {
        assertTrue(repository.graph.first().nodes.size > 1)

        val newGraph = BrushGraph(nodes = listOf(GraphNode(id = "node1", data = NodeData.Family())))
        repository.setGraph(newGraph)

        assertEquals(repository.graph.first().nodes.size, 1)

        repository.clearGraph()

        val graph = repository.graph.first()
        assertTrue(graph.nodes.size > 1)
    }

    @Test
    fun postDebug_addsIssue() = testScope.runTest {
        assertTrue(repository.graphIssues.first().isEmpty())

        val text = DisplayText.Literal("debug message")
        repository.postDebug(text)

        val issues = repository.graphIssues.first()
        assertTrue(issues.any { it.displayMessage == text && it.severity == ValidationSeverity.DEBUG })
    }

    @Test
    fun clearIssues_removesIssues() = testScope.runTest {
        repository.postDebug(DisplayText.Literal("debug"))
        assertTrue(repository.graphIssues.first().isNotEmpty())

        repository.clearIssues()
        assertTrue(repository.graphIssues.first().isEmpty())
    }

    @Test
    fun addEdge_updatesGraph() = testScope.runTest {
        val node1 = repository.addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))
        val node2 = repository.addNode(NodeData.Coat())

        repository.addEdge(node1, node2, "tip")

        val graph = repository.graph.first()
        assertTrue(graph.edges.any { it.fromNodeId == node1 && it.toNodeId == node2 && it.toPortId == "tip" })
    }

    @Test
    fun deleteEdge_updatesGraph() = testScope.runTest {
        val node1 = repository.addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))
        val node2 = repository.addNode(NodeData.Coat())
        repository.addEdge(node1, node2, "tip")

        val edge =
            repository.graph.first().edges.find { it.fromNodeId == node1 && it.toNodeId == node2 }!!

        assertTrue(repository.graph.first().edges.contains(edge))

        repository.deleteEdge(edge)

        val graph = repository.graph.first()
        assertFalse(graph.edges.contains(edge))
    }

    @Test
    fun setEdgeDisabled_updatesGraph() = testScope.runTest {
        val node1 = repository.addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))
        val node2 = repository.addNode(NodeData.Coat())
        repository.addEdge(node1, node2, "tip")

        val edge =
            repository.graph.first().edges.find { it.fromNodeId == node1 && it.toNodeId == node2 }!!
        repository.setEdgeDisabled(edge, true)

        val graph = repository.graph.first()
        val updatedEdge = graph.edges.find { it.fromNodeId == node1 && it.toNodeId == node2 }
        assertTrue(updatedEdge?.isDisabled == true)
    }

    @Test
    fun updateNodeData_updatesGraph() = testScope.runTest {
        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        val nodeId = repository.addNode(nodeData)

        val newData = NodeData.Tip(
            ProtoBrushTip.newBuilder().addBehaviors(ProtoBrushBehavior.getDefaultInstance()).build()
        )
        repository.updateNodeData(nodeId, newData)

        val graph = repository.graph.first()
        val node = graph.nodes.find { it.id == nodeId }!!
        assertEquals(newData, node.data)
    }

    @Test
    fun setNodeDisabled_updatesGraph() = testScope.runTest {
        val nodeData = NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        val nodeId = repository.addNode(nodeData)

        repository.setNodeDisabled(nodeId, true)

        val graph = repository.graph.first()
        val node = graph.nodes.find { it.id == nodeId }!!
        assertTrue(node.isDisabled)
    }

    @Test
    fun deleteSelectedNodes_updatesGraph() = testScope.runTest {
        val node1 = repository.addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))
        val node2 = repository.addNode(NodeData.Coat())

        val initialCount = repository.graph.first().nodes.size

        repository.deleteSelectedNodes(setOf(node1, node2))

        val graph = repository.graph.first()
        assertEquals(initialCount - 2, graph.nodes.size)
        assertFalse(graph.nodes.any { it.id == node1 || it.id == node2 })
    }

    @Test
    fun duplicateSelectedNodes_updatesGraph() = testScope.runTest {
        val node1 = repository.addNode(NodeData.Tip(ProtoBrushTip.getDefaultInstance()))

        val initialCount = repository.graph.first().nodes.size

        val idMap = repository.duplicateSelectedNodes(setOf(node1))

        val graph = repository.graph.first()
        assertEquals(initialCount + 1, graph.nodes.size)
        assertTrue(idMap.containsKey(node1))
        val duplicatedId = idMap[node1]!!
        assertTrue(graph.nodes.any { it.id == duplicatedId })
    }

    @Test
    fun addNodeBetween_updatesGraph() = testScope.runTest {
        val behavior1 = NodeData.Behavior(ProtoBrushBehavior.Node.getDefaultInstance())
        val behavior2 = NodeData.Behavior(
            ProtoBrushBehavior.Node.getDefaultInstance(),
            inputPortIds = listOf("input1")
        )
        val node1 = repository.addNode(behavior1)
        val node2 = repository.addNode(behavior2)
        repository.addEdge(node1, node2, "input1")

        val edge =
            repository.graph.first().edges.find { it.fromNodeId == node1 && it.toNodeId == node2 }!!
        val newNodeId = repository.addNodeBetween(edge)

        assertNotNull(newNodeId)
        val graph = repository.graph.first()

        val edge1 = graph.edges.find { it.fromNodeId == node1 && it.toNodeId == newNodeId }
        val edge2 = graph.edges.find { it.fromNodeId == newNodeId && it.toNodeId == node2 }
        assertNotNull(edge1)
        assertNotNull(edge2)
        assertFalse(graph.edges.contains(edge))
    }

    @Test
    fun reorderPorts_updatesGraph() = testScope.runTest {
        val familyNode = repository.graph.first().nodes.find { it.data is NodeData.Family }!!

        val coat1 = repository.addNode(NodeData.Coat())
        val coat2 = repository.addNode(NodeData.Coat())

        repository.addEdge(coat1, familyNode.id, "add_coat")
        repository.addEdge(coat2, familyNode.id, "add_coat")

        val updatedFamilyNode = repository.graph.first().nodes.find { it.id == familyNode.id }!!
        val updatedData = updatedFamilyNode.data as NodeData.Family
        val portIds = updatedData.coatPortIds
        assertEquals(3, portIds.size)

        repository.reorderPorts(familyNode.id, 0, 1)

        val reorderedFamilyNode = repository.graph.first().nodes.find { it.id == familyNode.id }!!
        val reorderedData = reorderedFamilyNode.data as NodeData.Family
        assertEquals(portIds[1], reorderedData.coatPortIds[0])
        assertEquals(portIds[0], reorderedData.coatPortIds[1])
    }

}
