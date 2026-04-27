/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
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
    fun getVisiblePorts_singleInputNodeNoConnections_returnsAddInputPort() {
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
    fun getVisiblePorts_singleInputNodeWithConnections_returnsInputAndAddInputPorts() {
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
    fun getVisiblePorts_binaryOpNoConnections_returnsAddInputPort() {
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
    fun getVisiblePorts_binaryOpWithConnections_returnsInputsAndAddInputPorts() {
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
    fun getVisiblePorts_polarTargetNoConnections_returnsAddInputPort() {
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
    fun getVisiblePorts_polarTargetWithConnections_returnsInputsAndAddInputPorts() {
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
    fun getVisiblePorts_paintNoConnections_returnsAddTextureAndAddColorPorts() {
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
    fun getVisiblePorts_paintWithConnections_returnsPortsAndAddPorts() {
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

    @Test
    fun preserveEdgesOnTypeChange_binaryOpToInterpolation_edgesArePreserved() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().build())
                .build(),
            inputPortIds = listOf("input_0", "input_1", "input_2")
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        val sourceId3 = "4"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "input_0")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "input_1")
        val edge3 = GraphEdge(fromNodeId = sourceId3, toNodeId = targetNodeId, toPortId = "input_2")
        
        val edges = listOf(edge1, edge2, edge3)
        
        val newData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setInterpolationNode(ProtoBrushBehavior.InterpolationNode.getDefaultInstance())
                .build()
        )
        
        val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(targetNodeId, oldData, newData, edges)
        
        assertEquals(3, finalEdges.size)
        
        val e1 = finalEdges.find { it.fromNodeId == sourceId1 }!!
        val behaviorData = finalNewData as NodeData.Behavior
        assertEquals(behaviorData.inputPortIds[0], e1.toPortId)
        
        val e2 = finalEdges.find { it.fromNodeId == sourceId2 }!!
        assertEquals(behaviorData.inputPortIds[1], e2.toPortId)
        
        val e3 = finalEdges.find { it.fromNodeId == sourceId3 }!!
        assertEquals(behaviorData.inputPortIds[2], e3.toPortId)
        
        assertEquals(3, behaviorData.inputPortIds.size)
    }

    @Test
    fun preserveEdgesOnTypeChange_interpolationToBinaryOp_edgesArePreserved() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setInterpolationNode(ProtoBrushBehavior.InterpolationNode.getDefaultInstance())
                .build()
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        val sourceId3 = "4"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "Value")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "Start")
        val edge3 = GraphEdge(fromNodeId = sourceId3, toNodeId = targetNodeId, toPortId = "End")
        
        val edges = listOf(edge1, edge2, edge3)
        
        val newData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.getDefaultInstance())
                .build()
        )
        
        val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(targetNodeId, oldData, newData, edges)
        
        assertEquals(3, finalEdges.size)
        
        val behaviorData = finalNewData as NodeData.Behavior
        val e1 = finalEdges.find { it.fromNodeId == sourceId1 }!!
        assertEquals(behaviorData.inputPortIds[0], e1.toPortId)
        
        val e2 = finalEdges.find { it.fromNodeId == sourceId2 }!!
        assertEquals(behaviorData.inputPortIds[1], e2.toPortId)
        
        val e3 = finalEdges.find { it.fromNodeId == sourceId3 }!!
        assertEquals(behaviorData.inputPortIds[2], e3.toPortId)
        
        assertEquals(3, behaviorData.inputPortIds.size)
    }

    @Test
    fun preserveEdgesOnTypeChange_binaryOpToPolarTarget_edgesArePreserved() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().build())
                .build(),
            inputPortIds = listOf("input_0", "input_1")
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "input_0")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "input_1")
        
        val edges = listOf(edge1, edge2)
        
        val newData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setPolarTargetNode(ProtoBrushBehavior.PolarTargetNode.getDefaultInstance())
                .build()
        )
        
        val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(targetNodeId, oldData, newData, edges)
        
        assertEquals(2, finalEdges.size)
        
        val behaviorData = finalNewData as NodeData.Behavior
        val e1 = finalEdges.find { it.fromNodeId == sourceId1 }!!
        assertEquals(behaviorData.inputPortIds[0], e1.toPortId)
        
        val e2 = finalEdges.find { it.fromNodeId == sourceId2 }!!
        assertEquals(behaviorData.inputPortIds[1], e2.toPortId)
        
        assertEquals(2, behaviorData.inputPortIds.size)
    }

    @Test
    fun preserveEdgesOnTypeChange_toTargetNode_edgesArePreserved() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setBinaryOpNode(ProtoBrushBehavior.BinaryOpNode.newBuilder().build())
                .build(),
            inputPortIds = listOf("input_0", "input_1")
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "input_0")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "input_1")
        
        val edges = listOf(edge1, edge2)
        
        val newData = NodeData.Behavior(
            node = ProtoBrushBehavior.Node.newBuilder()
                .setTargetNode(ProtoBrushBehavior.TargetNode.getDefaultInstance())
                .build()
        )
        
        val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(targetNodeId, oldData, newData, edges)
        
        assertEquals(2, finalEdges.size)
        
        val behaviorData = finalNewData as NodeData.Behavior
        val e1 = finalEdges.find { it.fromNodeId == sourceId1 }!!
        assertEquals(behaviorData.inputPortIds[0], e1.toPortId)
        
        val e2 = finalEdges.find { it.fromNodeId == sourceId2 }!!
        assertEquals(behaviorData.inputPortIds[1], e2.toPortId)
        
        assertEquals(2, behaviorData.inputPortIds.size)
    }

    @Test
    fun subtitles_constantNode_formatsWithDotSeparator() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setConstantNode(ProtoBrushBehavior.ConstantNode.newBuilder().setValue(1.5f).build())
                    .build()
            )
        )
        
        val subtitles = node.data.subtitles()
        
        assertEquals(1, subtitles.size)
        val subtitle = subtitles[0]
        assert(subtitle is DisplayText.Literal)
        assertEquals("1.50", (subtitle as DisplayText.Literal).text)
    }
}
