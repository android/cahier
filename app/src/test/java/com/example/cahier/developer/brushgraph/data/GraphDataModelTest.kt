package com.example.cahier.developer.brushgraph.data

import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import com.example.cahier.R
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
            )
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("add_input", ports[0].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[0].label)
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
            )
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()))
        val edge = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "Input")
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals(DisplayText.Resource(R.string.bg_port_input), ports[0].label)
        assertEquals(false, ports[0].isAddPort)
        
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[1].label)
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
            )
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("add_input", ports[0].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[0].label)
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
            )
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()))
        val edge = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "input_0")
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(3, ports.size)
        assertEquals("input_0", ports[0].id)
        assertEquals(DisplayText.Literal("A"), ports[0].label)
        assertEquals("input_1", ports[1].id)
        assertEquals(DisplayText.Literal("B"), ports[1].label)
        assertEquals("add_input", ports[2].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[2].label)
    }

    @Test
    fun testGetVisiblePorts_PolarTarget_NoConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setPolarTargetNode(ProtoBrushBehavior.PolarTargetNode.getDefaultInstance())
                    .build()
            )
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("add_input", ports[0].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[0].label)
    }

    @Test
    fun testGetVisiblePorts_PolarTarget_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setPolarTargetNode(ProtoBrushBehavior.PolarTargetNode.getDefaultInstance())
                    .build(),
                inputPortIds = listOf("angle_0", "mag_0")
            )
        )
        val sourceNode1 = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()))
        val sourceNode2 = GraphNode(id = "3", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()))
        val edge1 = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "angle_0")
        val edge2 = GraphEdge(fromNodeId = "3", toNodeId = "1", toPortId = "mag_0")
        val graph = BrushGraph(nodes = listOf(node, sourceNode1, sourceNode2), edges = listOf(edge1, edge2))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(3, ports.size)
        assertEquals("angle_0", ports[0].id)
        assertEquals(DisplayText.Resource(R.string.bg_port_angle), ports[0].label)
        assertEquals("mag_0", ports[1].id)
        assertEquals(DisplayText.Resource(R.string.bg_port_mag), ports[1].label)
        assertEquals("add_input", ports[2].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_input), ports[2].label)
    }

    @Test
    fun testGetVisiblePorts_Paint_NoConnections() {
        val node = GraphNode(id = "1", data = NodeData.Paint(paint = ink.proto.BrushPaint.getDefaultInstance()))
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("add_texture", ports[0].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_texture), ports[0].label)
        assertEquals("add_color", ports[1].id)
        assertEquals(DisplayText.Resource(R.string.bg_add_color), ports[1].label)
    }

    @Test
    fun testGetVisiblePorts_Paint_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Paint(
                paint = ink.proto.BrushPaint.getDefaultInstance(),
                texturePortIds = listOf("texture_0"),
                colorPortIds = listOf("color_0")
            )
        )
        val textureNode = GraphNode(id = "2", data = NodeData.TextureLayer(layer = ink.proto.BrushPaint.TextureLayer.getDefaultInstance()))
        val colorNode = GraphNode(id = "3", data = NodeData.ColorFunction(function = ink.proto.ColorFunction.getDefaultInstance()))
        
        val edge1 = GraphEdge(fromNodeId = "2", toNodeId = "1", toPortId = "texture_0")
        val edge2 = GraphEdge(fromNodeId = "3", toNodeId = "1", toPortId = "color_0")
        
        val graph = BrushGraph(nodes = listOf(node, textureNode, colorNode), edges = listOf(edge1, edge2))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(4, ports.size)
        assertEquals(DisplayText.Resource(R.string.bg_port_texture), ports[0].label)
        assertEquals(DisplayText.Resource(R.string.bg_add_texture), ports[1].label)
        assertEquals(DisplayText.Resource(R.string.bg_port_color), ports[2].label)
        assertEquals(DisplayText.Resource(R.string.bg_add_color), ports[3].label)
    }
}
