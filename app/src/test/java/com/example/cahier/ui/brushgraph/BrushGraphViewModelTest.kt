package com.example.cahier.developer.brushgraph

import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.preserveEdgesOnTypeChange
import ink.proto.BrushBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class BrushGraphViewModelTest {

    @Test
    fun testPreserveEdges_BinaryOpToInterpolation() {
        // 1. Setup inputs
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
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
            node = BrushBehavior.Node.newBuilder()
                .setInterpolationNode(BrushBehavior.InterpolationNode.getDefaultInstance())
                .build()
        )
        
        // 2. Call function
        val (finalNewData, finalEdges) = preserveEdgesOnTypeChange(targetNodeId, oldData, newData, edges)
        
        // 3. Verify
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
    fun testPreserveEdges_InterpolationToBinaryOp() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setInterpolationNode(BrushBehavior.InterpolationNode.getDefaultInstance())
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
            node = BrushBehavior.Node.newBuilder()
                .setBinaryOpNode(BrushBehavior.BinaryOpNode.getDefaultInstance())
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
    fun testPreserveEdges_BinaryOpToPolarTarget() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                .build(),
            inputPortIds = listOf("input_0", "input_1")
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "input_0")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "input_1")
        
        val edges = listOf(edge1, edge2)
        
        val newData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setPolarTargetNode(BrushBehavior.PolarTargetNode.getDefaultInstance())
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
    fun testPreserveEdges_ToTargetNode() {
        val targetNodeId = "1"
        val oldData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                .build(),
            inputPortIds = listOf("input_0", "input_1")
        )
        
        val sourceId1 = "2"
        val sourceId2 = "3"
        
        val edge1 = GraphEdge(fromNodeId = sourceId1, toNodeId = targetNodeId, toPortId = "input_0")
        val edge2 = GraphEdge(fromNodeId = sourceId2, toNodeId = targetNodeId, toPortId = "input_1")
        
        val edges = listOf(edge1, edge2)
        
        val newData = NodeData.Behavior(
            node = BrushBehavior.Node.newBuilder()
                .setTargetNode(BrushBehavior.TargetNode.getDefaultInstance())
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
}
