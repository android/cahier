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
import java.util.UUID

class BrushGraphConverterTest {

    @Test
    fun testDeduplication() {
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
    fun testCrossBehaviorDeduplication() {
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
    fun testPolarTargetDeduplication() {
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
    fun testTextureLayerDeduplication() {
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
    fun testColorFunctionDeduplication() {
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
}
