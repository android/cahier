package com.example.cahier.ui.brushgraph.model

import com.example.cahier.ui.brushgraph.model.GraphPoint
import com.example.cahier.ui.brushgraph.model.Port
import com.example.cahier.ui.brushgraph.model.PortSide
import ink.proto.BrushBehavior as ProtoBrushBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class GraphDataModelTest {

    @Test
    fun testGetVisiblePorts_SingleInputNode_NoConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setResponseNode(ProtoBrushBehavior.ResponseNode.getDefaultInstance())
                    .build()
            ),
            position = GraphPoint(0f, 0f)
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("add_input", ports[0].id)
        assertEquals("Add input...", ports[0].label)
    }

    @Test
    fun testGetVisiblePorts_SingleInputNode_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setResponseNode(ProtoBrushBehavior.ResponseNode.getDefaultInstance())
                    .build(),
                inputPortIds = listOf("Input")
            ),
            position = GraphPoint(0f, 0f)
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()), position = GraphPoint(0f, 0f))
        val edge = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "Input")
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("Input", ports[0].label)
        assertEquals(false, ports[0].isAddPort)
        
        assertEquals("Add input...", ports[1].label)
        assertEquals(true, ports[1].isAddPort)
    }

    @Test
    fun testGetVisiblePorts_BinaryOp_NoConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM))
                    .build()
            ),
            position = GraphPoint(0f, 0f)
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("add_input", ports[0].id)
        assertEquals("Add input...", ports[0].label)
    }

    @Test
    fun testGetVisiblePorts_BinaryOp_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM))
                    .build(),
                inputPortIds = listOf("input_0", "input_1")
            ),
            position = GraphPoint(0f, 0f)
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()), position = GraphPoint(0f, 0f))
        val edge = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "input_0")
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(3, ports.size)
        assertEquals("input_0", ports[0].id)
        assertEquals("A", ports[0].label)
        assertEquals("input_1", ports[1].id)
        assertEquals("B", ports[1].label)
        assertEquals("add_input", ports[2].id)
        assertEquals("Add input...", ports[2].label)
    }

    @Test
    fun testGetVisiblePorts_Paint_NoConnections() {
        val node = GraphNode(id = "1", data = NodeData.Paint(paint = ink.proto.BrushPaint.getDefaultInstance()), position = GraphPoint(0f, 0f))
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("add_texture", ports[0].id)
        assertEquals("Add Texture...", ports[0].label)
        assertEquals("add_color", ports[1].id)
        assertEquals("Add Color...", ports[1].label)
    }

    @Test
    fun testGetVisiblePorts_Paint_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Paint(
                paint = ink.proto.BrushPaint.getDefaultInstance(),
                texturePortIds = listOf("texture_0"),
                colorPortIds = listOf("color_0")
            ),
            position = GraphPoint(0f, 0f)
        )
        val textureNode = GraphNode(id = "2", data = NodeData.TextureLayer(layer = ink.proto.BrushPaint.TextureLayer.getDefaultInstance()), position = GraphPoint(0f, 0f))
        val colorNode = GraphNode(id = "3", data = NodeData.ColorFunc(function = ink.proto.ColorFunction.getDefaultInstance()), position = GraphPoint(0f, 0f))
        
        val edge1 = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "texture_0")
        val edge2 = GraphEdge(fromNodeId = "3", toNodeId = "1", toPortId = "color_0")
        
        val graph = BrushGraph(nodes = listOf(node, textureNode, colorNode), edges = listOf(edge1, edge2))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(4, ports.size)
        assertEquals("Texture", ports[0].label)
        assertEquals("Add Texture...", ports[1].label)
        assertEquals("Color", ports[2].label)
        assertEquals("Add Color...", ports[3].label)
    }
}
