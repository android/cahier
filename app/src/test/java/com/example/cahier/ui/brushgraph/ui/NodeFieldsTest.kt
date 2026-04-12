package com.example.cahier.ui.brushgraph.ui

import ink.proto.BrushBehavior as ProtoBrushBehavior
import org.junit.Assert.assertEquals
import org.junit.Test

class NodeFieldsTest {

  @Test
  fun testAllSourcesAccountedFor() {
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
  fun testAllTargetsAccountedFor() {
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
  fun testAllNodeTypesAccountedFor() {
    val allBehaviorNodes = listOf(
      "Source", "Constant", "Noise", "ToolTypeFilter", "Damping",
      "Response", "Integral", "BinaryOp", "Interpolation", "Target", "PolarTarget"
    ).toSet()

    val categorizedNodeTypes = (
      NODE_TYPES_START +
      NODE_TYPES_OPERATOR +
      NODE_TYPES_TERMINAL
    ).toSet()

    assertEquals("Not all behavior node types are accounted for!", allBehaviorNodes, categorizedNodeTypes)
  }
}
