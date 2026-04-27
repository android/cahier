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

import com.example.cahier.R
import ink.proto.BrushBehavior as ProtoBrushBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class GraphValidatorTest {

    @Test
    fun validateAll_duplicateIssuesWithDifferentArgs_areNotDeduplicated() {
        val exc1 = GraphValidationException(
            displayMessage = DisplayText.Resource(R.string.bg_err_interp_missing_input, listOf(DisplayText.Literal("A"))),
            nodeId = "1"
        )
        val exc2 = GraphValidationException(
            displayMessage = DisplayText.Resource(R.string.bg_err_interp_missing_input, listOf(DisplayText.Literal("B"))),
            nodeId = "1"
        )
        
        val issues = listOf(exc1, exc2, exc1)
        val distinctIssues = issues.distinct()
        
        assertEquals(2, distinctIssues.size)
        assertEquals(exc1, distinctIssues[0])
        assertEquals(exc2, distinctIssues[1])
    }

    @Test
    fun validateAll_interpolationNodeWithExcessInputs_doesNotThrowIndexOutOfBounds() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setInterpolationNode(ProtoBrushBehavior.InterpolationNode.getDefaultInstance())
                    .build(),
                inputPortIds = listOf("value", "start", "end", "excess_1", "excess_2")
            )
        )
        val graph = BrushGraph(nodes = listOf(node))
        
        val issues = GraphValidator.validateAll(graph)
        
        val missingInputIssues = issues.filter { it.displayMessage is DisplayText.Resource && it.displayMessage.resId == R.string.bg_err_interp_missing_input }
        assertEquals(3, missingInputIssues.size)
    }

    @Test
    fun validateAll_sourceRangeEqual_producesMessageWithoutNestedLists() {
        val node = GraphNode(
            id = "1",
            data = NodeData.Behavior(
                node = ProtoBrushBehavior.Node.newBuilder()
                    .setSourceNode(
                        ProtoBrushBehavior.SourceNode.newBuilder()
                            .setSourceValueRangeStart(1.0f)
                            .setSourceValueRangeEnd(1.0f)
                            .build()
                    )
                    .build()
            )
        )
        val graph = BrushGraph(nodes = listOf(node))
        
        val issues = GraphValidator.validateAll(graph)
        
        val rangeIssues = issues.filter { it.displayMessage is DisplayText.Resource && it.displayMessage.resId == R.string.bg_err_source_range_equal }
        assertEquals(1, rangeIssues.size)
        
        val displayMessage = rangeIssues[0].displayMessage as DisplayText.Resource
        assertEquals(1, displayMessage.args.size)
        
        val arg = displayMessage.args[0]
        assert(arg is DisplayText)
    }
}
