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
package com.example.cahier.developer.brushgraph.ui

import ink.proto.BrushBehavior as ProtoBrushBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import com.example.cahier.developer.brushgraph.ui.fields.SOURCES_INPUT
import com.example.cahier.developer.brushgraph.ui.fields.SOURCES_MOVEMENT
import com.example.cahier.developer.brushgraph.ui.fields.SOURCES_DISTANCE
import com.example.cahier.developer.brushgraph.ui.fields.SOURCES_TIME
import com.example.cahier.developer.brushgraph.ui.fields.SOURCES_ACCELERATION
import com.example.cahier.developer.brushgraph.ui.fields.TARGETS_SIZE_SHAPE
import com.example.cahier.developer.brushgraph.ui.fields.TARGETS_POSITION
import com.example.cahier.developer.brushgraph.ui.fields.TARGETS_COLOR_OPACITY
import com.example.cahier.developer.brushgraph.ui.fields.NODE_TYPES_START
import com.example.cahier.developer.brushgraph.ui.fields.NODE_TYPES_OPERATOR
import com.example.cahier.developer.brushgraph.ui.fields.NODE_TYPES_TERMINAL
import com.example.cahier.developer.brushgraph.ui.fields.isAngle

class NodeFieldsTest {

  @Test
  fun sources_allValues_areCategorized() {
    val allSources = ProtoBrushBehavior.Source.values()
      .filter { it != ProtoBrushBehavior.Source.SOURCE_UNSPECIFIED && it.ordinal >= 0 }
      .toSet()

    val categorizedSources = (
      SOURCES_INPUT +
      SOURCES_MOVEMENT +
      SOURCES_DISTANCE +
      SOURCES_TIME +
      SOURCES_ACCELERATION
    ).toSet()

    assertEquals("Not all sources are accounted for!", allSources, categorizedSources)
  }

  @Test
  fun targets_allValues_areCategorized() {
    val allTargets = ProtoBrushBehavior.Target.values()
      .filter { it != ProtoBrushBehavior.Target.TARGET_UNSPECIFIED && it.ordinal >= 0 }
      .toSet()

    val categorizedTargets = (
      TARGETS_SIZE_SHAPE +
      TARGETS_POSITION +
      TARGETS_COLOR_OPACITY
    ).toSet()

    assertEquals("Not all targets are accounted for!", allTargets, categorizedTargets)
  }

  @Test
  fun nodeTypes_allValues_areCategorized() {
    val allBehaviorNodes = listOf(
      "Source", "Constant", "Noise", "ToolTypeFilter", "Damping",
      "Response", "Integral", "BinaryOp", "Interpolation", "Target", "PolarTarget"
    ).toSet()

    val categorizedNodeTypes = (
      NODE_TYPES_START +
      NODE_TYPES_OPERATOR +
      NODE_TYPES_TERMINAL
    ).map { it.name.removeSuffix("_NODE").split("_").joinToString("") { part -> part.lowercase().replaceFirstChar { it.uppercase() } } }.toSet()

    assertEquals("Not all behavior node types are accounted for!", allBehaviorNodes, categorizedNodeTypes)
  }

  @Test
  fun source_isAngle_returnsTrueForAngleSources() {
    assertTrue(ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS.isAngle())
    
    assertFalse(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE.isAngle())
    assertFalse(ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND.isAngle())
  }

  @Test
  fun target_isAngle_returnsTrueForAngleTargets() {
    assertTrue(ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS.isAngle())
    assertTrue(ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS.isAngle())
    
    assertFalse(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER.isAngle())
    assertFalse(ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER.isAngle())
  }
}
