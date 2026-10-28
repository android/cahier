/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushgraph.data

import com.example.cahier.R
import ink.proto.BrushBehavior
import ink.proto.BrushBehaviorKt
import ink.proto.BrushFamily
import ink.proto.BrushPaintKt
import ink.proto.BrushTip
import ink.proto.ColorFunction
import ink.proto.brushBehavior
import ink.proto.brushCoat
import ink.proto.brushFamily
import ink.proto.brushPaint
import ink.proto.brushTip
import java.util.zip.GZIPInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BrushGraphConverterTest {

  @Test
  fun fromProtoBrushFamily_identicalNodes_areDeduplicated() {
    val behaviorProto = brushBehavior {
      nodes += BrushBehaviorKt.node {
        sourceNode = BrushBehaviorKt.sourceNode {
          source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE
        }
      }
      nodes += BrushBehaviorKt.node { targetNode = BrushBehavior.TargetNode.getDefaultInstance() }
      nodes += BrushBehaviorKt.node {
        sourceNode = BrushBehaviorKt.sourceNode {
          source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE
        }
      }
      nodes += BrushBehaviorKt.node { targetNode = BrushBehavior.TargetNode.getDefaultInstance() }
    }

    val tipProto = brushTip { behaviors += behaviorProto }

    val familyProto = brushFamily { coats += brushCoat { tip = tipProto } }

    val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)

    val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
    assertEquals(2, behaviorNodes.size)
  }

  @Test
  fun fromProtoBrushFamily_identicalNodesAcrossBehaviors_areDeduplicated() {
    val behaviorProto1 = brushBehavior {
      nodes += BrushBehaviorKt.node {
        sourceNode = BrushBehaviorKt.sourceNode {
          source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE
        }
      }
      nodes += BrushBehaviorKt.node { targetNode = BrushBehavior.TargetNode.getDefaultInstance() }
    }

    val behaviorProto2 = brushBehavior {
      nodes += BrushBehaviorKt.node {
        sourceNode = BrushBehaviorKt.sourceNode {
          source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE
        }
      }
      nodes += BrushBehaviorKt.node { targetNode = BrushBehavior.TargetNode.getDefaultInstance() }
    }

    val tipProto = brushTip {
      behaviors += behaviorProto1
      behaviors += behaviorProto2
    }

    val familyProto = brushFamily { coats += brushCoat { tip = tipProto } }

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
    val sourceNodeProto = BrushBehaviorKt.node {
      sourceNode = BrushBehaviorKt.sourceNode {
        source = BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE
      }
    }

    val polarTargetNodeProto = BrushBehaviorKt.node {
      polarTargetNode = BrushBehavior.PolarTargetNode.getDefaultInstance()
    }

    val behaviorProto1 = brushBehavior {
      nodes += sourceNodeProto
      nodes += sourceNodeProto
      nodes += polarTargetNodeProto
    }

    val tipProto = brushTip {
      behaviors += behaviorProto1
      behaviors += behaviorProto1
    }

    val familyProto = brushFamily { coats += brushCoat { tip = tipProto } }

    val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)

    val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
    assertEquals(2, behaviorNodes.size)
  }

  @Test
  fun fromProtoBrushFamily_textureLayerNodes_areDeduplicated() {
    val textureLayer1 = BrushPaintKt.textureLayer { clientTextureId = "texture_1" }

    val paintProto1 = brushPaint { textureLayers += textureLayer1 }

    val paintProto2 = brushPaint { textureLayers += textureLayer1 }

    val familyProto = brushFamily {
      coats += brushCoat {
        paintPreferences += paintProto1
        paintPreferences += paintProto2
      }
    }

    val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)

    val textureNodes = graph.nodes.filter { it.data is NodeData.TextureLayer }
    assertEquals(1, textureNodes.size)
  }

  @Test
  fun fromProtoBrushFamily_colorFunctionNodes_areDeduplicated() {
    val colorFunction1 = ColorFunction.getDefaultInstance()

    val paintProto1 = brushPaint { colorFunctions += colorFunction1 }

    val paintProto2 = brushPaint { colorFunctions += colorFunction1 }

    val familyProto = brushFamily {
      coats += brushCoat {
        paintPreferences += paintProto1
        paintPreferences += paintProto2
      }
    }

    val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)

    val colorNodes = graph.nodes.filter { it.data is NodeData.ColorFunction }
    assertEquals(1, colorNodes.size)
  }

  @Test
  fun fromProtoBrushFamily_allCustomBrushesRoundTrip_preservesContent() {
    val brushResources =
      listOf(
        R.raw.calligraphy,
        R.raw.flag_banner,
        R.raw.graffiti,
        R.raw.groovy,
        R.raw.holiday_lights,
        R.raw.lace,
        R.raw.music,
        R.raw.shadow,
        R.raw.twisted_yarn,
        R.raw.wet_paint,
      )

    for (resId in brushResources) {
      val stream = RuntimeEnvironment.getApplication().resources.openRawResource(resId)
      val gis = GZIPInputStream(stream)
      val originalProto = BrushFamily.parseFrom(gis)

      val graph = BrushGraphConverter.fromProtoBrushFamily(originalProto)
      val roundTrippedProto = BrushFamilyConverter.convertIntoProto(graph)

      val resName = RuntimeEnvironment.getApplication().resources.getResourceEntryName(resId)

      // Won't be identical, but we check for rough functional equivalency
      assertEquals(
        "Coats count mismatch for $resName",
        originalProto.coatsCount,
        roundTrippedProto.coatsCount,
      )
      assertEquals(
        "Client ID mismatch for $resName",
        originalProto.clientBrushFamilyId,
        roundTrippedProto.clientBrushFamilyId,
      )

      for (i in 0 until originalProto.coatsCount) {
        val originalCoat = originalProto.getCoats(i)
        val roundTrippedCoat = roundTrippedProto.getCoats(i)

        val originalTargets = collectTargets(originalCoat.tip)
        val roundTrippedTargets = collectTargets(roundTrippedCoat.tip)

        assertEquals(
          "Targets mismatch for brush resource $resName coat $i",
          originalTargets,
          roundTrippedTargets,
        )

        assertEquals(
          "Paint preferences mismatch for brush resource $resName coat $i",
          originalCoat.paintPreferencesList,
          roundTrippedCoat.paintPreferencesList,
        )
      }
    }
  }

  @Test
  fun fromProtoBrushFamily_targetNodesWithDifferentComments_areNotDeduplicated() {
    val targetNodeProto = BrushBehaviorKt.node {
      targetNode = BrushBehavior.TargetNode.getDefaultInstance()
    }

    val behaviorProto1 = brushBehavior {
      nodes += targetNodeProto
      developerComment = "Comment A"
    }

    val behaviorProto2 = brushBehavior {
      nodes += targetNodeProto
      developerComment = "Comment B"
    }

    val tipProto = brushTip {
      behaviors += behaviorProto1
      behaviors += behaviorProto2
    }

    val familyProto = brushFamily { coats += brushCoat { tip = tipProto } }

    val graph = BrushGraphConverter.fromProtoBrushFamily(familyProto)

    val behaviorNodes = graph.nodes.filter { it.data is NodeData.Behavior }
    assertEquals(2, behaviorNodes.size)
    assertTrue(behaviorNodes.any { (it.data as NodeData.Behavior).developerComment == "Comment A" })
    assertTrue(behaviorNodes.any { (it.data as NodeData.Behavior).developerComment == "Comment B" })
  }

  private fun collectTargets(tip: BrushTip): Set<String> {
    val targets = mutableSetOf<String>()
    for (behavior in tip.behaviorsList) {
      for (node in behavior.nodesList) {
        when {
          node.hasTargetNode() -> {
            targets.add(node.targetNode.toString().trim())
          }
          node.hasPolarTargetNode() -> {
            targets.add(node.polarTargetNode.toString().trim())
          }
        }
      }
    }
    return targets
  }
}
