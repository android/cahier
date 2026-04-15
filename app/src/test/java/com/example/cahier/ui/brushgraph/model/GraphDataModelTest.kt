package com.example.cahier.ui.brushgraph.model

import androidx.compose.ui.geometry.Offset
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
            position = Offset.Zero
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("Add input...", ports[0].label)
        assertEquals(0, ports[0].index)
        assertEquals(true, ports[0].isAddPort)
    }

    @Test
    fun testGetVisiblePorts_SingleInputNode_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setResponseNode(ProtoBrushBehavior.ResponseNode.getDefaultInstance())
                    .build()
            ),
            position = Offset.Zero
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()), position = Offset.Zero)
        val edge = GraphEdge(fromPort = Port("2", PortSide.OUTPUT, 0), toPort = Port("1", PortSide.INPUT, 0))
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("Input", ports[0].label)
        assertEquals(0, ports[0].index)
        assertEquals(false, ports[0].isAddPort)
        
        assertEquals("Add input...", ports[1].label)
        assertEquals(1, ports[1].index)
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
            position = Offset.Zero
        )
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(1, ports.size)
        assertEquals("Add input...", ports[0].label)
        assertEquals(0, ports[0].index)
        assertEquals(true, ports[0].isAddPort)
    }

    @Test
    fun testGetVisiblePorts_BinaryOp_WithConnections() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM))
                    .build()
            ),
            position = Offset.Zero
        )
        val sourceNode = GraphNode(id = "2", data = NodeData.Behavior(node = ProtoBrushBehavior.Node.getDefaultInstance()), position = Offset.Zero)
        val edge = GraphEdge(fromPort = Port("2", PortSide.OUTPUT, 0), toPort = Port("1", PortSide.INPUT, 0))
        val graph = BrushGraph(nodes = listOf(node, sourceNode), edges = listOf(edge))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("A", ports[0].label)
        assertEquals(0, ports[0].index)
        
        assertEquals("Add input...", ports[1].label)
        assertEquals(1, ports[1].index)
        assertEquals(true, ports[1].isAddPort)
    }

    @Test
    fun testGetVisiblePorts_Paint_NoConnections() {
        val node = GraphNode(id = "1", data = NodeData.Paint(paint = ink.proto.BrushPaint.getDefaultInstance()), position = Offset.Zero)
        val graph = BrushGraph(nodes = listOf(node))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(2, ports.size)
        assertEquals("Add Texture...", ports[0].label)
        assertEquals(0, ports[0].index)
        
        assertEquals("Add Color...", ports[1].label)
        assertEquals(1, ports[1].index)
    }

    @Test
    fun testGetVisiblePorts_Paint_WithConnections() {
        val node = GraphNode(id = "1", data = NodeData.Paint(paint = ink.proto.BrushPaint.getDefaultInstance()), position = Offset.Zero)
        val textureNode = GraphNode(id = "2", data = NodeData.TextureLayer(layer = ink.proto.BrushPaint.TextureLayer.getDefaultInstance()), position = Offset.Zero)
        val colorNode = GraphNode(id = "3", data = NodeData.ColorFunc(function = ink.proto.ColorFunction.getDefaultInstance()), position = Offset.Zero)
        
        val edge1 = GraphEdge(fromPort = Port("2", PortSide.OUTPUT, 0), toPort = Port("1", PortSide.INPUT, 0))
        val edge2 = GraphEdge(fromPort = Port("3", PortSide.OUTPUT, 0), toPort = Port("1", PortSide.INPUT, 2))
        
        val graph = BrushGraph(nodes = listOf(node, textureNode, colorNode), edges = listOf(edge1, edge2))
        val ports = node.getVisiblePorts(graph)
        
        assertEquals(4, ports.size)
        assertEquals("Texture", ports[0].label)
        assertEquals(0, ports[0].index)
        
        assertEquals("Add Texture...", ports[1].label)
        assertEquals(1, ports[1].index)
        
        assertEquals("Color", ports[2].label)
        assertEquals(2, ports[2].index)
        
        assertEquals("Add Color...", ports[3].label)
        assertEquals(3, ports[3].index)
    }
}
