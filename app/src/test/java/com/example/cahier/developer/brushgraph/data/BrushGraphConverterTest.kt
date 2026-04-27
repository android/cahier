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

import com.example.cahier.developer.brushgraph.data.BrushGraphConverter
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.NodeData
import ink.proto.BrushBehavior
import ink.proto.BrushTip
import ink.proto.BrushPaint
import ink.proto.BrushFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import com.example.cahier.R
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class BrushGraphConverterTest {

    @Test
    fun fromProtoBrushFamily_identicalNodes_areDeduplicated() {
        val behaviorProto = BrushBehavior.newBuilder()
            .addNodes(BrushBehavior.Node.newBuilder().setSourceNode(BrushBehavior.SourceNode.newBuilder().setSource(BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).build()).build())
            .addNodes(BrushBehavior.Node.newBuilder().setTargetNode(BrushBehavior.TargetNode.newBuilder().build()).build())
            .addNodes(BrushBehavior.Node.newBuilder().setSourceNode(BrushBehavior.SourceNode.newBuilder().setSource(BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).build()).build())
            .addNodes(BrushBehavior.Node.newBuilder().setTargetNode(BrushBehavior.TargetNode.newBuilder().build()).build())
            .build()
            
        val tipProto = BrushTip.newBuilder()
            .addBehaviors(behaviorProto)
            .build()
            
        val familyProto = BrushFamily.newBuilder()
            .addCoats(ink.proto.BrushCoat.newBuilder().setTip(tipProto).build())
            .build()
            
        val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)
        
        val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
        assertEquals(2, behaviorNodes.size)
    }
    @Test
    fun fromProtoBrushFamily_identicalNodesAcrossBehaviors_areDeduplicated() {
        val behaviorProto1 = BrushBehavior.newBuilder()
            .addNodes(BrushBehavior.Node.newBuilder().setSourceNode(BrushBehavior.SourceNode.newBuilder().setSource(BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).build()).build())
            .addNodes(BrushBehavior.Node.newBuilder().setTargetNode(BrushBehavior.TargetNode.newBuilder().build()).build())
            .build()
            
        val behaviorProto2 = BrushBehavior.newBuilder()
            .addNodes(BrushBehavior.Node.newBuilder().setSourceNode(BrushBehavior.SourceNode.newBuilder().setSource(BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).build()).build())
            .addNodes(BrushBehavior.Node.newBuilder().setTargetNode(BrushBehavior.TargetNode.newBuilder().build()).build())
            .build()
            
        val tipProto = BrushTip.newBuilder()
            .addBehaviors(behaviorProto1)
            .addBehaviors(behaviorProto2)
            .build()
            
        val familyProto = BrushFamily.newBuilder()
            .addCoats(ink.proto.BrushCoat.newBuilder().setTip(tipProto).build())
            .build()
            
        val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)
        
        val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
        assertEquals(2, behaviorNodes.size)
        
        val tipNode = graph.nodes.find { it.data is NodeData.Tip }!!
        val edgesToTip = graph.edges.filter { it.toNodeId == tipNode.id }
        assertEquals(1, edgesToTip.size)
        
        val tipData = tipNode.data as NodeData.Tip
        assertEquals(1, tipData.behaviorPortIds.size)
    }

    @Test
    fun fromProtoBrushFamily_polarTargetNodes_areDeduplicated() {
        val sourceNode = ink.proto.BrushBehavior.Node.newBuilder()
            .setSourceNode(ink.proto.BrushBehavior.SourceNode.newBuilder().setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE).build())
            .build()
            
        val polarTargetNode = ink.proto.BrushBehavior.Node.newBuilder()
            .setPolarTargetNode(ink.proto.BrushBehavior.PolarTargetNode.newBuilder().build())
            .build()
            
        val behaviorProto1 = ink.proto.BrushBehavior.newBuilder()
            .addNodes(sourceNode)
            .addNodes(sourceNode)
            .addNodes(polarTargetNode)
            .build()
            
        val tipProto = ink.proto.BrushTip.newBuilder()
            .addBehaviors(behaviorProto1)
            .addBehaviors(behaviorProto1)
            .build()
            
        val familyProto = ink.proto.BrushFamily.newBuilder()
            .addCoats(ink.proto.BrushCoat.newBuilder().setTip(tipProto).build())
            .build()
            
        val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)
        
        val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
        assertEquals(2, behaviorNodes.size)
    }
    @Test
    fun fromProtoBrushFamily_textureLayerNodes_areDeduplicated() {
        val textureLayer1 = BrushPaint.TextureLayer.newBuilder()
            .setClientTextureId("texture_1")
            .build()
            
        val paintProto1 = BrushPaint.newBuilder()
            .addTextureLayers(textureLayer1)
            .build()
            
        val paintProto2 = BrushPaint.newBuilder()
            .addTextureLayers(textureLayer1)
            .build()
            
        val familyProto = BrushFamily.newBuilder()
            .addCoats(ink.proto.BrushCoat.newBuilder().addPaintPreferences(paintProto1).addPaintPreferences(paintProto2).build())
            .build()
            
        val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)
        
        val textureNodes = graph.nodes.filter { it.data is NodeData.TextureLayer }
        assertEquals(1, textureNodes.size)
    }

    @Test
    fun fromProtoBrushFamily_colorFunctionNodes_areDeduplicated() {
        val colorFunction1 = ink.proto.ColorFunction.getDefaultInstance()
            
        val paintProto1 = BrushPaint.newBuilder()
            .addColorFunctions(colorFunction1)
            .build()
            
        val paintProto2 = BrushPaint.newBuilder()
            .addColorFunctions(colorFunction1)
            .build()
            
        val familyProto = BrushFamily.newBuilder()
            .addCoats(ink.proto.BrushCoat.newBuilder().addPaintPreferences(paintProto1).addPaintPreferences(paintProto2).build())
            .build()
            
        val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)
        
        val colorNodes = graph.nodes.filter { it.data is NodeData.ColorFunction }
        assertEquals(1, colorNodes.size)
    }

    @Test
    fun fromProtoBrushFamily_allCustomBrushesRoundTrip_preservesContent() {
        val brushResources = listOf(
            R.raw.calligraphy,
            R.raw.flag_banner,
            R.raw.graffiti,
            R.raw.groovy,
            R.raw.holiday_lights,
            R.raw.lace,
            R.raw.music,
            R.raw.shadow,
            R.raw.twisted_yarn,
            R.raw.wet_paint
        )

        for (resId in brushResources) {
            val stream = RuntimeEnvironment.getApplication().resources.openRawResource(resId)
            val gis = java.util.zip.GZIPInputStream(stream)
            val originalProto = ink.proto.BrushFamily.parseFrom(gis)

            val graph = BrushGraphConverter.fromProtoBrushFamily(originalProto)
            val roundTrippedProto = BrushFamilyConverter.convertIntoProto(graph)

            val resName = RuntimeEnvironment.getApplication().resources.getResourceEntryName(resId)

            // Won't be identical, but we check for rough functional equivalency
            assertEquals("Coats count mismatch for $resName", originalProto.coatsCount, roundTrippedProto.coatsCount)
            assertEquals("Client ID mismatch for $resName", originalProto.clientBrushFamilyId, roundTrippedProto.clientBrushFamilyId)

            for (i in 0 until originalProto.coatsCount) {
                val originalCoat = originalProto.getCoats(i)
                val roundTrippedCoat = roundTrippedProto.getCoats(i)

                val originalTargets = collectTargets(originalCoat.tip)
                val roundTrippedTargets = collectTargets(roundTrippedCoat.tip)

                assertEquals("Targets mismatch for brush resource $resName coat $i", originalTargets, roundTrippedTargets)

                assertEquals("Paint preferences mismatch for brush resource $resName coat $i", originalCoat.paintPreferencesList, roundTrippedCoat.paintPreferencesList)
            }
        }
    }

    private fun collectTargets(tip: ink.proto.BrushTip): Set<String> {
        val targets = mutableSetOf<String>()
        for (behavior in tip.behaviorsList) {
            for (node in behavior.nodesList) {
                if (node.hasTargetNode()) {
                    targets.add(node.targetNode.toString().trim())
                } else if (node.hasPolarTargetNode()) {
                    targets.add(node.polarTargetNode.toString().trim())
                }
            }
        }
        return targets
    }
}
