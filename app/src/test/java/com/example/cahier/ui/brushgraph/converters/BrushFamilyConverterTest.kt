package com.example.cahier.ui.brushgraph.converters

import androidx.compose.ui.geometry.Offset
import com.example.cahier.ui.brushgraph.model.BrushGraph
import com.example.cahier.ui.brushgraph.model.GraphEdge
import com.example.cahier.ui.brushgraph.model.GraphNode
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.ValidationSeverity
import ink.proto.BrushBehavior
import ink.proto.BrushTip
import ink.proto.BrushPaint
import ink.proto.BrushFamily
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushFamilyConverterTest {

    @Test
    fun validateAll_ignoresDisabledNonOperatorNodes() {
        val disabledNode = GraphNode(
            id = "target_node",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero,
            isDisabled = true
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(BrushTip.newBuilder().build()),
            position = Offset.Zero
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build()),
            position = Offset.Zero
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat,
            position = Offset.Zero
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(numCoats = 1),
            position = Offset.Zero
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "target_node", toNodeId = "tip", toInputIndex = 0),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toInputIndex = 0),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toInputIndex = 1),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toInputIndex = 0)
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, disabledNode),
            edges = edges
        )

        val issues = BrushFamilyConverter.validateAll(graph)
        println("Test 1 issues: $issues")
        
        // Target node should not report errors because it is disabled.
        assertTrue(issues.none { it.nodeId == "target_node" })
    }

    @Test
    fun validateAll_passThroughOperatorNode() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val dampingNode = GraphNode(
            id = "damping",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setDampingNode(BrushBehavior.DampingNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero,
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(BrushTip.newBuilder().build()),
            position = Offset.Zero
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build()),
            position = Offset.Zero
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat,
            position = Offset.Zero
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(numCoats = 1),
            position = Offset.Zero
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "damping", toInputIndex = 0),
            GraphEdge(fromNodeId = "damping", toNodeId = "target", toInputIndex = 0),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toInputIndex = 0),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toInputIndex = 0),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toInputIndex = 1),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toInputIndex = 0)
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, dampingNode, sourceNode),
            edges = edges
        )

        val issues = BrushFamilyConverter.validateAll(graph)
        println("Test 2 issues: $issues")
        
        // Should pass because Damping passes through!
        assertTrue(issues.none { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun validateAll_passThroughMultiInputOperator_firstInput() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val binaryOpNode = GraphNode(
            id = "binary_op",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero,
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(BrushTip.newBuilder().build()),
            position = Offset.Zero
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build()),
            position = Offset.Zero
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat,
            position = Offset.Zero
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(numCoats = 1),
            position = Offset.Zero
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "binary_op", toInputIndex = 0),
            GraphEdge(fromNodeId = "binary_op", toNodeId = "target", toInputIndex = 0),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toInputIndex = 0),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toInputIndex = 0),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toInputIndex = 1),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toInputIndex = 0)
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, sourceNode),
            edges = edges
        )

        val issues = BrushFamilyConverter.validateAll(graph)
        println("Test 3 issues: $issues")
        
        assertTrue(issues.none { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun validateAll_passThroughMultiInputOperator_secondInputIgnored() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val binaryOpNode = GraphNode(
            id = "binary_op",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero,
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(BrushTip.newBuilder().build()),
            position = Offset.Zero
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build()),
            position = Offset.Zero
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat,
            position = Offset.Zero
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(numCoats = 1),
            position = Offset.Zero
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "binary_op", toInputIndex = 1),
            GraphEdge(fromNodeId = "binary_op", toNodeId = "target", toInputIndex = 0),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toInputIndex = 0),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toInputIndex = 0),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toInputIndex = 1),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toInputIndex = 0)
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, sourceNode),
            edges = edges
        )

        val issues = BrushFamilyConverter.validateAll(graph)
        println("Test 4 issues: $issues")
        
        // Should FAIL because BinaryOp only passes through input 0!
        assertTrue(issues.any { it.message.contains("Missing source for pass-through connection") })
    }

    @Test
    fun testUnusedOutputWarningWhenDownstreamNodeIsDisabled() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            position = Offset.Zero
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(BrushTip.newBuilder().build()),
            position = Offset.Zero
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build()),
            position = Offset.Zero
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat,
            position = Offset.Zero,
            isDisabled = true // Disable Coat!
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(numCoats = 1),
            position = Offset.Zero
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "target", toInputIndex = 0),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toInputIndex = 0),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toInputIndex = 0),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toInputIndex = 1),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toInputIndex = 0)
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, sourceNode),
            edges = edges
        )

        val issues = BrushFamilyConverter.validateAll(graph)
        
        // Tip output should be reported as not used because Coat is disabled.
        assertTrue(issues.any { it.nodeId == "tip" && it.message.contains("output is not used") })
        // Paint output should be reported as not used because Coat is disabled.
        assertTrue(issues.any { it.nodeId == "paint" && it.message.contains("output is not used") })
    }
}
