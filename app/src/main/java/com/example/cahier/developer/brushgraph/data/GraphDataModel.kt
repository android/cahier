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

import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import java.util.UUID
import androidx.ink.brush.BrushFamily
import androidx.ink.storage.decode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import com.example.cahier.R
import ink.proto.Color as ProtoColor

/**
 * Converts a [ProtoBrushFamily] into a functional [BrushFamily] object.
 *
 * This handles the necessary GZIP compression and decoding steps required by the [BrushFamily.decode] API.
 */
fun ProtoBrushFamily.toBrushFamily(): BrushFamily {
  val rawBytes = this.toByteArray()
  val baos = ByteArrayOutputStream()
  GZIPOutputStream(baos).use { it.write(rawBytes) }
  return ByteArrayInputStream(baos.toByteArray()).use { inputStream ->
    BrushFamily.decode(inputStream)
  }
}

data class GraphPoint(val x: Float, val y: Float)

/** Representation of a single node in the brush behavior graph. */
data class GraphNode(
  val id: String = UUID.randomUUID().toString(),
  val data: NodeData,
  val isExpanded: Boolean = false,
  val hasError: Boolean = false,
  val hasWarning: Boolean = false,
  val isDisabled: Boolean = false,
)

/** Represents the core data/component within a node. */
sealed interface NodeData {
  /** Returns a list of the input ports visible on this node  */
  fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> = emptyList()

  /** Metadata for the inputs of this node. */
  fun inputLabels(): List<Int> = emptyList()

  /** Returns whether this node has an output port. */
  fun hasOutput(): Boolean = true

  /** Title to be displayed on the node. */
  fun title(): Int

  /** Subtitle for additional context, if any. */
  fun subtitles(): List<DisplayText> = emptyList()

  /** Wraps a [ProtoBrushTip]. */
  data class Tip(
    val tip: ProtoBrushTip,
    val behaviorPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels() = listOf(R.string.bg_port_behaviors)

    override fun title() = R.string.bg_tip

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (portId in behaviorPortIds) {
            ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(R.string.bg_port_behavior)))
        }
        ports.add(Port.AddBehavior(nodeId, "add_behavior", label = DisplayText.Resource(R.string.bg_behavior)))
        return ports
    }
  }

  /** Wraps a [ProtoBrushPaint]. */
  data class Paint(
    val paint: ProtoBrushPaint,
    val texturePortIds: List<String> = emptyList(),
    val colorPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels(): List<Int> {
      val labels = mutableListOf<Int>()
      for (i in texturePortIds.indices) labels.add(R.string.bg_port_texture)
      labels.add(R.string.bg_port_texture)
      for (i in colorPortIds.indices) labels.add(R.string.bg_port_color)
      labels.add(R.string.bg_port_color)
      return labels
    }

    override fun title() = R.string.bg_paint

    override fun subtitles() = listOf(DisplayText.Resource(R.string.bg_overlap_label, listOf(DisplayText.Resource(paint.selfOverlap.displayStringRId()))))

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (portId in texturePortIds) {
            ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(R.string.bg_port_texture)))
        }
        ports.add(Port.AddTexture(nodeId, "add_texture", label = DisplayText.Resource(R.string.bg_port_texture)))

        for (portId in colorPortIds) {
            ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(R.string.bg_port_color)))
        }
        ports.add(Port.AddColor(nodeId, "add_color", label = DisplayText.Resource(R.string.bg_port_color)))
        return ports
    }
  }

  /** Wraps a [ProtoBrushPaint.TextureLayer]. */
  data class TextureLayer(
    val layer: ProtoBrushPaint.TextureLayer
  ) : NodeData {
    override fun title() = R.string.bg_texture_layer

    override fun subtitles() = listOf(DisplayText.Literal(layer.clientTextureId))
  }

  /** Wraps a [ProtoColorFunction]. */
  data class ColorFunction(
    val function: ProtoColorFunction
  ) : NodeData {
    override fun title() = R.string.bg_color_function

    override fun subtitles() =
      listOf(
        if (function.hasOpacityMultiplier()) {
          DisplayText.Resource(R.string.bg_opacity_multiplier)
        } else {
          DisplayText.Resource(R.string.bg_replace_color)
        }
      )
  }

  /** Wraps a [ProtoBrushBehavior.Node] */
  data class Behavior(
    val node: ProtoBrushBehavior.Node,
    val developerComment: String = "",
    val behaviorId: String = "",
    val inputPortIds: List<String> = when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf("value", "start", "end")
        else -> emptyList()
    }
  ) : NodeData {
    override fun inputLabels(): List<Int> {
      return when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> listOf(R.string.bg_port_input)
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> listOf(R.string.bg_port_input)
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> listOf(R.string.bg_port_input)
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> listOf(R.string.bg_port_input)
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> listOf(R.string.bg_port_a, R.string.bg_port_b)
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf(R.string.bg_port_value, R.string.bg_port_start, R.string.bg_port_end)
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> listOf(R.string.bg_port_input)
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> listOf(R.string.bg_port_angle, R.string.bg_port_mag)
        else -> emptyList()
      }
    }

    val isOperator: Boolean
      get() = when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE,
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE,
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE,
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE,
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE,
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> true
        else -> false
      }

    override fun title() =
      when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> R.string.bg_node_source
        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> R.string.bg_node_constant
        ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> R.string.bg_node_noise
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> R.string.bg_node_tool_type_filter
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> R.string.bg_node_damping
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> R.string.bg_node_response
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> R.string.bg_node_integral
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> R.string.bg_node_binary_op
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> R.string.bg_node_interpolation
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> R.string.bg_node_target
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> R.string.bg_node_polar_target
        else -> R.string.bg_node_unknown
      }

    override fun subtitles(): List<DisplayText> {
      val s = when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> DisplayText.Resource(node.sourceNode.source.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> DisplayText.Literal("%.2f".format(java.util.Locale.US, node.constantNode.value))
        ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
          return listOf(
            DisplayText.Resource(node.noiseNode.varyOver.displayStringRId()),
            DisplayText.Resource(R.string.bg_period_label, listOf(node.noiseNode.basePeriod.toString()))
          )
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> {
          val bitmask = node.toolTypeFilterNode.enabledToolTypes
          val enabled = mutableListOf<DisplayText>()
          if (bitmask and (1 shl 0) != 0) enabled.add(DisplayText.Resource(R.string.bg_tool_type_unknown))
          if (bitmask and (1 shl 1) != 0) enabled.add(DisplayText.Resource(R.string.bg_tool_type_mouse))
          if (bitmask and (1 shl 2) != 0) enabled.add(DisplayText.Resource(R.string.bg_tool_type_touch))
          if (bitmask and (1 shl 3) != 0) enabled.add(DisplayText.Resource(R.string.bg_tool_type_stylus))
          return if (enabled.isEmpty()) listOf(DisplayText.Resource(R.string.bg_none))
          else enabled
        }
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> {
          val source = node.dampingNode.dampingSource
          val unit = when (source) {
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> DisplayText.Resource(R.string.bg_unit_cm)
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> DisplayText.Resource(R.string.bg_unit_size)
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> DisplayText.Resource(R.string.bg_unit_s)
            else -> DisplayText.Literal("")
          }
          return listOf(
            DisplayText.Resource(source.displayStringRId()),
            DisplayText.Resource(R.string.bg_gap_label, listOf(node.dampingNode.dampingGap.toString(), unit))
          )
        }
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> DisplayText.Resource(node.responseNode.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> DisplayText.Resource(node.integralNode.integrateOver.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> DisplayText.Resource(node.binaryOpNode.operation.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> DisplayText.Resource(node.interpolationNode.interpolation.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> DisplayText.Resource(node.targetNode.target.displayStringRId())
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> DisplayText.Resource(node.polarTargetNode.target.displayStringRId())
        else -> DisplayText.Literal(node.nodeCase.name)
      }
      return listOf(s)
    }

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        when (node.nodeCase) {
            ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
                val labels = inputLabels()
                for (i in labels.indices) {
                    val portId = inputPortIds.getOrElse(i) { "invalid_port_$i" }
                    ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(labels[i])))
                }
            }
            ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
                var nextIndex = 0
                for (portId in inputPortIds) {
                    var n = nextIndex + 1
                    val builder = StringBuilder()
                    while (n > 0) {
                        val m = (n - 1) % 26
                        builder.append(('A'.code + m).toChar())
                        n = (n - 1) / 26
                    }
                    val label = builder.reverse().toString()
                    ports.add(Port.Input(nodeId, portId, label = DisplayText.Literal(label)))
                    nextIndex++
                }
                ports.add(Port.AddInput(nodeId, "add_input", label = DisplayText.Resource(R.string.bg_port_input)))
            }
            ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
                val labels = inputLabels()
                for (i in inputPortIds.indices) {
                    val label = labels[i % labels.size]
                    ports.add(Port.Input(nodeId, inputPortIds[i], label = DisplayText.Resource(label)))
                }
                ports.add(Port.AddInput(nodeId, "add_input", label = DisplayText.Resource(R.string.bg_port_input)))
            }
            else -> {
                val labels = inputLabels()
                if (labels.size == 1) {
                    for (portId in inputPortIds) {
                        ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(labels[0])))
                    }
                    ports.add(Port.AddInput(nodeId, "add_input", label = DisplayText.Resource(R.string.bg_port_input)))
                } else {
                    for (i in labels.indices) {
                        val portId = inputPortIds.getOrElse(i) { "invalid_port_$i" }
                        ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(labels[i])))
                    }
                }
            }
        }
        return ports
    }
  }

  /** Represents a [ProtoBrushCoat]. */
  data class Coat(
    val tipPortId: String = "tip",
    val paintPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels(): List<Int> {
      val labels = mutableListOf(R.string.bg_port_tip)
      for (i in paintPortIds.indices) {
        labels.add(R.string.bg_port_paint)
      }
      labels.add(R.string.bg_port_paint)
      return labels
    }

    override fun title() = R.string.bg_coat

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        ports.add(Port.AddTip(nodeId, tipPortId, label = DisplayText.Resource(R.string.bg_port_tip)))
        for (portId in paintPortIds) {
            ports.add(Port.Input(nodeId, portId, label = DisplayText.Resource(R.string.bg_port_paint)))
        }
        ports.add(Port.AddPaint(nodeId, "add_paint", label = DisplayText.Resource(R.string.bg_port_paint)))
        return ports
    }
  }

  /** Represents the [ProtoBrushFamily] root. */
  data class Family(
    val clientBrushFamilyId: String = "",
    val developerComment: String = "",
    val inputModel: ProtoBrushFamily.InputModel =
      ProtoBrushFamily.InputModel.newBuilder()
        .setSlidingWindowModel(
          ProtoBrushFamily.SlidingWindowModel.newBuilder()
            .setWindowSizeSeconds(0.02f)
            .setExperimentalUpsamplingPeriodSeconds(0.005f)
        )
        .build(),
    val coatPortIds: List<String> = emptyList(),
  ) : NodeData {
    override fun inputLabels(): List<Int> {
      val labels = mutableListOf<Int>()
      for (i in coatPortIds.indices) {
        labels.add(R.string.bg_port_coat)
      }
      labels.add(R.string.bg_coat)
      return labels
    }

    override fun title() = R.string.bg_family

    override fun subtitles() = listOf(DisplayText.Literal(clientBrushFamilyId))

    override fun hasOutput() = false

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (i in coatPortIds.indices) {
            ports.add(Port.Input(nodeId, coatPortIds[i], label = DisplayText.Resource(R.string.bg_port_coat, listOf(i))))
        }
        ports.add(Port.AddCoat(nodeId, "add_coat", label = DisplayText.Resource(R.string.bg_coat)))
        return ports
    }
  }
}

/** Side of a node where a port is located. */
enum class PortSide {
  INPUT,
  OUTPUT,
}

/** Represents a connection between two nodes. */
data class GraphEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val toPortId: String,
    val isDisabled: Boolean = false
)

/** Represents the entire node graph state. */
data class BrushGraph(
  val nodes: List<GraphNode> = emptyList(),
  val edges: List<GraphEdge> = emptyList(),
)

sealed class Port(
    val nodeId: String,
    val id: String,
    val label: DisplayText? = null,
    val isAddPort: Boolean = false
) {
    abstract val side: PortSide

    companion object {
        const val OUTPUT_PORT_ID = "output"
    }

    class Output(nodeId: String, id: String = OUTPUT_PORT_ID, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = false) {
        override val side = PortSide.OUTPUT
    }

    class Input(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = false) {
        override val side = PortSide.INPUT
    }

    class AddCoat(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddBehavior(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddInput(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddTexture(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddColor(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddTip(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddPaint(nodeId: String, id: String, label: DisplayText? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }
}

fun GraphNode.getVisiblePorts(graph: BrushGraph): List<Port> {
    return data.getVisiblePorts(this.id, graph)
}

/** Preserves input edges when changing node types by mapping them to new port IDs. */
fun preserveEdgesOnTypeChange(
    nodeId: String,
    oldData: NodeData?,
    newData: NodeData,
    edges: List<GraphEdge>
): Pair<NodeData, List<GraphEdge>> {
    var finalNewData = newData
    var finalEdges = edges

    if (oldData is NodeData.Behavior && newData is NodeData.Behavior) {
      val oldCase = oldData.node.nodeCase
      val newCase = newData.node.nodeCase
      if (oldCase != newCase) {
        val incomingEdges = edges.filter { it.toNodeId == nodeId }
        val newIds = mutableListOf<String>()
        val updatedEdges = mutableListOf<GraphEdge>()

        when (newCase) {
          ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
            val defaultIds = listOf("value", "start", "end")
            for (i in 0..2) {
              val edge = incomingEdges.getOrNull(i)
              val portId = edge?.toPortId ?: defaultIds[i]
              newIds.add(portId)
              if (edge != null) {
                updatedEdges.add(edge.copy(toPortId = portId))
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
            incomingEdges.take(26).forEachIndexed { index, edge ->
              val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else UUID.randomUUID().toString()
              newIds.add(portId)
              updatedEdges.add(edge.copy(toPortId = portId))
            }
          }
          ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
            incomingEdges.forEachIndexed { index, edge ->
              val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else UUID.randomUUID().toString()
              newIds.add(portId)
              updatedEdges.add(edge.copy(toPortId = portId))
            }
          }
          else -> {
            val labels = newData.inputLabels()
            if (labels.size == 1) {
              incomingEdges.forEachIndexed { index, edge ->
                val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else UUID.randomUUID().toString()
                newIds.add(portId)
                updatedEdges.add(edge.copy(toPortId = portId))
              }
            }
          }
        }

        finalNewData = newData.copy(inputPortIds = newIds)
        val edgesWithoutIncoming = edges.filter { it.toNodeId != nodeId }
        finalEdges = edgesWithoutIncoming + updatedEdges
      } else if (newData.inputPortIds.isEmpty() && oldData.inputPortIds.isNotEmpty()) {
        finalNewData = newData.copy(inputPortIds = oldData.inputPortIds)
      }
    }
    return Pair(finalNewData, finalEdges)
}

/** Based on [Port] subtype, create default [NodeData] to populate it. */
fun Port.inferNodeData(node: GraphNode): NodeData? = when (this) {
    is Port.AddCoat -> NodeData.Coat()
    is Port.AddTip -> NodeData.Tip(ProtoBrushTip.getDefaultInstance())
    is Port.AddPaint -> NodeData.Paint(ProtoBrushPaint.getDefaultInstance())
    is Port.AddTexture -> NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance())
    is Port.AddColor -> NodeData.ColorFunction(ProtoColorFunction.newBuilder()
            .setReplaceColor(
                    ProtoColor.newBuilder()
                      .setRed(0f)
                      .setGreen(0f)
                      .setBlue(0f)
                      .setAlpha(1f)
                      .build()
                  ).build())
    is Port.AddBehavior -> {
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setTargetNode(
              ProtoBrushBehavior.TargetNode.newBuilder()
                .setTarget(ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER)
                .setTargetModifierRangeStart(0.0f)
                .setTargetModifierRangeEnd(1.0f)
            )
            .build(),
          "",
          UUID.randomUUID().toString()
        )
    }
    is Port.AddInput -> {
        val data = node.data as NodeData.Behavior
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setSourceNode(
              ProtoBrushBehavior.SourceNode.newBuilder()
                .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                .setSourceValueRangeStart(0.0f)
                .setSourceValueRangeEnd(1.0f)
            )
            .build(),
          "",
          data.behaviorId
        )
      }
    is Port.Input -> {
        val data = node.data
        if (data is NodeData.Behavior) {
            NodeData.Behavior(
              ProtoBrushBehavior.Node.newBuilder()
                .setSourceNode(
                  ProtoBrushBehavior.SourceNode.newBuilder()
                    .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                    .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                    .setSourceValueRangeStart(0.0f)
                    .setSourceValueRangeEnd(1.0f)
                )
                .build(),
              "",
              data.behaviorId
            )
        } else {
            null
        }
    }
    else -> null
}

/** Determines if a given [Port] can be reordered with drag handles in the UI. */
fun NodeData.isPortReorderable(port: Port, index: Int, hasAddPort: Boolean): Boolean {
  return !port.isAddPort && hasAddPort && when (this) {
    is NodeData.Coat -> index != 0
    is NodeData.Behavior -> {
      if (this.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
        index % 2 == 0
      } else {
        true
      }
    }
    else -> true
  }
}
