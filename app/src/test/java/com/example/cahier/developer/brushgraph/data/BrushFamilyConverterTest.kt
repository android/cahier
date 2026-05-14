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

import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.data.ValidationSeverity
import com.example.cahier.developer.brushgraph.data.GraphValidator
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.developer.brushgraph.data.BrushFamilyConverter
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.getVisiblePorts
import ink.proto.BrushBehavior
import ink.proto.BrushTip
import ink.proto.BrushPaint
import ink.proto.BrushFamily
import com.example.cahier.R
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushFamilyConverterTest {

    @Test
    fun validateAll_disabledNonOperatorNode_isIgnored() {
        val disabledNode = GraphNode(
            id = "target_node",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            ),
            isDisabled = true
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "target_node", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, disabledNode),
            edges = edges
        )

        val issues = GraphValidator.validateAll(graph)
        println("Test 1 issues: $issues")
        
        // Target node should not report errors because it is disabled.
        assertTrue(issues.none { it.nodeId == "target_node" })
    }

    @Test
    fun validateAll_disabledOperatorNodeActsAsPassThrough_succeeds() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            )
        )
        
        val dampingNode = GraphNode(
            id = "damping",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setDampingNode(BrushBehavior.DampingNode.newBuilder().build())
                    .build()
            ),
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "damping", toPortId = "Input"),
            GraphEdge(fromNodeId = "damping", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, dampingNode, sourceNode),
            edges = edges
        )

        val issues = GraphValidator.validateAll(graph)
        println("Test 2 issues: $issues")
        
        // Should pass because Damping passes through!
        assertTrue(issues.none { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun validateAll_disabledMultiInputOperatorFirstInput_succeeds() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            )
        )
        
        val binaryOpNode = GraphNode(
            id = "binary_op",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build()
            ),
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "binary_op", toPortId = "input_0"),
            GraphEdge(fromNodeId = "binary_op", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, sourceNode),
            edges = edges
        )

        val issues = GraphValidator.validateAll(graph)
        println("Test 3 issues: $issues")
        
        assertTrue(issues.none { it.severity == ValidationSeverity.ERROR })
    }

    @Test
    fun validateAll_disabledMultiInputOperatorSecondInput_noMissingSourceError() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder()
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            )
        )
        
        val binaryOpNode = GraphNode(
            id = "binary_op",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build()
            ),
            isDisabled = true
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "binary_op", toPortId = "input_1"),
            GraphEdge(fromNodeId = "binary_op", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, sourceNode),
            edges = edges
        )

        val issues = GraphValidator.validateAll(graph)
        println("Test 4 issues: $issues")
        
        org.junit.Assert.assertFalse(issues.any { it.message?.contains("Missing source for pass-through connection") == true })
    }

    @Test
    fun validateAll_downstreamNodeDisabled_reportsUnusedOutput() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setSourceNode(BrushBehavior.SourceNode.newBuilder().build())
                    .build()
            )
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0")),
            isDisabled = true
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, sourceNode),
            edges = edges
        )

        val issues = GraphValidator.validateAll(graph)
        
        assertTrue(issues.any { it.nodeId == "tip" && it.displayMessage is DisplayText.Resource && it.displayMessage.resId == R.string.bg_err_unused_output })
        assertTrue(issues.any { it.nodeId == "paint" && it.displayMessage is DisplayText.Resource && it.displayMessage.resId == R.string.bg_err_unused_output })
    }

    @Test
    fun convertIntoProto_startNodeReachedMultipleTimes_duplicatesSourceNode() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder()
                        .setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .build())
                    .build()
            )
        )
        
        val dampingNodeA = GraphNode(
            id = "dampingA",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setDampingNode(ink.proto.BrushBehavior.DampingNode.newBuilder().build())
                    .build()
            )
        )
        
        val dampingNodeB = GraphNode(
            id = "dampingB",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setDampingNode(ink.proto.BrushBehavior.DampingNode.newBuilder().build())
                    .build()
            )
        )
        
        val binaryOpNode = GraphNode(
            id = "binary_op",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ink.proto.BrushBehavior.BinaryOpNode.newBuilder().setOperation(ink.proto.BrushBehavior.BinaryOp.BINARY_OP_SUM))
                    .build()
            )
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(ink.proto.BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "dampingA", toPortId = "Input"),
            GraphEdge(fromNodeId = "source", toNodeId = "dampingB", toPortId = "Input"),
            GraphEdge(fromNodeId = "dampingA", toNodeId = "binary_op", toPortId = "input_0"),
            GraphEdge(fromNodeId = "dampingB", toNodeId = "binary_op", toPortId = "input_1"),
            GraphEdge(fromNodeId = "binary_op", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, dampingNodeA, dampingNodeB, sourceNode),
            edges = edges
        )
        
        val brushFamily = try {
            BrushFamilyConverter.convertIntoProto(graph)
        } catch (e: GraphValidationException) {
            println("Validation failed: ${e.message}")
            throw e
        }
        
        val tip = brushFamily.getCoats(0).tip
        org.junit.Assert.assertEquals(1, tip.behaviorsCount)
        val behavior = tip.getBehaviors(0)
        
        val sourceNodeCount = behavior.nodesList.count { it.hasSourceNode() }
        org.junit.Assert.assertEquals(2, sourceNodeCount)
    }

    @Test
    fun convertIntoProto_interpolationNodeWithFullSetOfInputs_createsInterpolationNode() {
        val valueNode = GraphNode(
            id = "value",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val startNode = GraphNode(
            id = "start",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setConstantNode(ink.proto.BrushBehavior.ConstantNode.newBuilder().setValue(0f).build())
                    .build()
            )
        )
        
        val endNode = GraphNode(
            id = "end",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setConstantNode(ink.proto.BrushBehavior.ConstantNode.newBuilder().setValue(1f).build())
                    .build()
            )
        )
        
        val lerpNode = GraphNode(
            id = "lerp",
            data = NodeData.Behavior(
                node = ink.proto.BrushBehavior.Node.newBuilder()
                    .setInterpolationNode(ink.proto.BrushBehavior.InterpolationNode.newBuilder().setInterpolation(ink.proto.BrushBehavior.Interpolation.INTERPOLATION_LERP).build())
                    .build(),
                inputPortIds = listOf("Value", "Start", "End")
            )
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "value", toNodeId = "lerp", toPortId = "Value"),
            GraphEdge(fromNodeId = "start", toNodeId = "lerp", toPortId = "Start"),
            GraphEdge(fromNodeId = "end", toNodeId = "lerp", toPortId = "End"),
            GraphEdge(fromNodeId = "lerp", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, lerpNode, valueNode, startNode, endNode),
            edges = edges
        )
        
        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)
        
        val tip = brushFamily.getCoats(0).tip
        val behavior = tip.getBehaviors(0)
        
        org.junit.Assert.assertEquals(5, behavior.nodesCount)
        org.junit.Assert.assertTrue(behavior.getNodes(3).hasInterpolationNode())
    }

    @Test
    fun convertIntoProto_coatWithMultiplePaints_createsMultiplePaints() {
        val valueNode = GraphNode(
            id = "value",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )
        
        val paintNode1 = GraphNode(
            id = "paint1",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val paintNode2 = GraphNode(
            id = "paint2",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0", "paint_1"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "value", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "Input"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint1", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "paint2", toNodeId = "coat", toPortId = "paint_1"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode1, paintNode2, targetNode, valueNode),
            edges = edges
        )
        
        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)
        
        val coat = brushFamily.getCoats(0)
        org.junit.Assert.assertEquals(2, coat.paintPreferencesCount)
    }

    @Test
    fun convertIntoProto_tipWithMultipleBehaviors_createsMultipleBehaviors() {
        val valueNode1 = GraphNode(
            id = "value1",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val targetNode1 = GraphNode(
            id = "target1",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val valueNode2 = GraphNode(
            id = "value2",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_TILT_IN_RADIANS).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val targetNode2 = GraphNode(
            id = "target2",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("0", "1"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "value1", toNodeId = "target1", toPortId = "Input"),
            GraphEdge(fromNodeId = "target1", toNodeId = "tip", toPortId = "0"),
            GraphEdge(fromNodeId = "value2", toNodeId = "target2", toPortId = "Input"),
            GraphEdge(fromNodeId = "target2", toNodeId = "tip", toPortId = "1"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode1, targetNode2, valueNode1, valueNode2),
            edges = edges
        )
        
        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)
        
        val tip = brushFamily.getCoats(0).tip
        org.junit.Assert.assertEquals(2, tip.behaviorsCount)
    }

    @Test
    fun convertIntoProto_binaryOpWithMultipleInputs_chainsInputs() {
        val sourceNode1 = GraphNode(
            id = "source1",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val sourceNode2 = GraphNode(
            id = "source2",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val sourceNode3 = GraphNode(
            id = "source3",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val binaryOpNode = GraphNode(
            id = "binOp",
            data = NodeData.Behavior(
                node = ink.proto.BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ink.proto.BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build(),
                inputPortIds = listOf("input_0", "input_1", "input_2")
            )
        )
        
        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("Input"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source1", toNodeId = "binOp", toPortId = "input_0"),
            GraphEdge(fromNodeId = "source2", toNodeId = "binOp", toPortId = "input_1"),
            GraphEdge(fromNodeId = "source3", toNodeId = "binOp", toPortId = "input_2"),
            GraphEdge(fromNodeId = "binOp", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "Input"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, binaryOpNode, sourceNode1, sourceNode2, sourceNode3),
            edges = edges
        )
        
        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)
        
        val coat = brushFamily.getCoats(0)
        val tip = coat.tip
        
        org.junit.Assert.assertEquals(1, tip.behaviorsCount)
        val behavior = tip.getBehaviors(0)
        org.junit.Assert.assertEquals(6, behavior.nodesCount)
        
        org.junit.Assert.assertTrue(behavior.getNodes(0).hasSourceNode())
        org.junit.Assert.assertTrue(behavior.getNodes(1).hasSourceNode())
        org.junit.Assert.assertTrue(behavior.getNodes(2).hasBinaryOpNode())
        org.junit.Assert.assertTrue(behavior.getNodes(3).hasSourceNode())
        org.junit.Assert.assertTrue(behavior.getNodes(4).hasBinaryOpNode())
        org.junit.Assert.assertTrue(behavior.getNodes(5).hasTargetNode())
    }
    
    @Test
    fun convertIntoProto_passThroughWithMultipleInputs_propagatesInputs() {
        val source1 = GraphNode(
            id = "source1",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val source2 = GraphNode(
            id = "source2",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )
        
        val disabledResponse = GraphNode(
            id = "disabled_response",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setBinaryOpNode(ink.proto.BrushBehavior.BinaryOpNode.newBuilder().build())
                    .build()
            ),
            isDisabled = true
        )
        
        val target = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(ink.proto.BrushBehavior.TargetNode.newBuilder().build())
                    .build()
            )
        )
        
        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("Input"))
        )
        
        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )
        
        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )
        
        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )
        
        val edges = listOf(
            GraphEdge(fromNodeId = "source1", toNodeId = "disabled_response", toPortId = "input_0"),
            GraphEdge(fromNodeId = "source2", toNodeId = "disabled_response", toPortId = "input_1"),
            GraphEdge(fromNodeId = "disabled_response", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "Input"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )
        
        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, target, disabledResponse, source1, source2),
            edges = edges
        )
        
        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)
        
        val coat = brushFamily.getCoats(0)
        val tip = coat.tip
        
        org.junit.Assert.assertEquals(2, tip.behaviorsCount)
        
        val behavior1 = tip.getBehaviors(0)
        val behavior2 = tip.getBehaviors(1)
        
        org.junit.Assert.assertEquals(2, behavior1.nodesCount)
        org.junit.Assert.assertEquals(2, behavior2.nodesCount)
        
        org.junit.Assert.assertTrue(behavior1.getNodes(0).hasSourceNode())
        org.junit.Assert.assertTrue(behavior1.getNodes(1).hasTargetNode())
        
        org.junit.Assert.assertTrue(behavior2.getNodes(0).hasSourceNode())
        org.junit.Assert.assertTrue(behavior2.getNodes(1).hasTargetNode())
    }

    @Test
    fun convertIntoProto_targetNodeWithComment_serializesCommentToBehavior() {
        val sourceNode = GraphNode(
            id = "source",
            data = NodeData.Behavior(
                ink.proto.BrushBehavior.Node.newBuilder()
                    .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).setSourceValueRangeStart(0f).setSourceValueRangeEnd(1f).build())
                    .build()
            )
        )

        val targetNode = GraphNode(
            id = "target",
            data = NodeData.Behavior(
                node = ink.proto.BrushBehavior.Node.newBuilder()
                    .setTargetNode(BrushBehavior.TargetNode.newBuilder().build())
                    .build(),
                developerComment = "Target Metadata Comment"
            )
        )

        val tipNode = GraphNode(
            id = "tip",
            data = NodeData.Tip(tip = ink.proto.BrushTip.newBuilder().build(), behaviorPortIds = listOf("behavior_0"))
        )

        val paintNode = GraphNode(
            id = "paint",
            data = NodeData.Paint(ink.proto.BrushPaint.newBuilder().build())
        )

        val coatNode = GraphNode(
            id = "coat",
            data = NodeData.Coat(paintPortIds = listOf("paint_0"))
        )

        val familyNode = GraphNode(
            id = "family",
            data = NodeData.Family(coatPortIds = listOf("coat_0"))
        )

        val edges = listOf(
            GraphEdge(fromNodeId = "source", toNodeId = "target", toPortId = "Input"),
            GraphEdge(fromNodeId = "target", toNodeId = "tip", toPortId = "behavior_0"),
            GraphEdge(fromNodeId = "tip", toNodeId = "coat", toPortId = "tip"),
            GraphEdge(fromNodeId = "paint", toNodeId = "coat", toPortId = "paint_0"),
            GraphEdge(fromNodeId = "coat", toNodeId = "family", toPortId = "coat_0")
        )

        val graph = BrushGraph(
            nodes = listOf(familyNode, coatNode, tipNode, paintNode, targetNode, sourceNode),
            edges = edges
        )

        val brushFamily = BrushFamilyConverter.convertIntoProto(graph)

        val tip = brushFamily.getCoats(0).tip
        org.junit.Assert.assertEquals(1, tip.behaviorsCount)
        org.junit.Assert.assertEquals("Target Metadata Comment", tip.getBehaviors(0).developerComment)
    }
}
