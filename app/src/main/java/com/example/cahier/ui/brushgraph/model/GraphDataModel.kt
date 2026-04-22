@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.model

import android.util.Log
import androidx.compose.ui.geometry.Offset
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

const val PORT_TEXT_SIZE_SP = 14

const val NODE_WIDTH = 300f
const val NODE_PADDING_VERTICAL = 8f
const val NODE_PADDING_BOTTOM = 12f
const val TITLE_AREA_HEIGHT = 64f
const val SUBTITLE_LINE_HEIGHT = 32f
const val PREVIEW_AREA_HEIGHT = 64f
const val INPUT_ROW_HEIGHT = 60f

const val INSPECTOR_WIDTH_LANDSCAPE = 320f
const val INSPECTOR_HEIGHT_PORTRAIT = 400f
const val PREVIEW_HEIGHT_EXPANDED = 200f
const val PREVIEW_HEIGHT_COLLAPSED = 40f

data class GraphPoint(val x: Float, val y: Float)

/** Representation of a single node in the brush behavior graph. */
data class GraphNode(
  val id: String = UUID.randomUUID().toString(),
  val data: NodeData,
  val position: GraphPoint,
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
  fun inputLabels(): List<String> = emptyList()

  /** Returns whether this node has an output port. */
  fun hasOutput(): Boolean = true

  /** Title to be displayed on the node. */
  fun title(): String

  /** Subtitle for additional context, if any. */
  fun subtitles(): List<String> = emptyList()

  /**
   * Total estimated width of the node on the canvas. The actual width may differ slightly due to
   * padding.
   */
  fun width(): Float = NODE_WIDTH

  /**
   * Total estimated height of the node on the canvas. The actual height may differ slightly due to
   * padding.
   */
  fun height(portCount: Int = inputLabels().size): Float {
    val previewH = if (this is NodeData.ColorFunc || this is NodeData.TextureLayer) PREVIEW_AREA_HEIGHT else 0f
    return NODE_PADDING_VERTICAL +
      titleHeight() +
      previewH +
      maxOf(portCount, 1) * INPUT_ROW_HEIGHT +
      NODE_PADDING_BOTTOM
  }

  fun titleHeight(): Float {
    val subs = subtitles()
    val subtitleHeight = subs.size * SUBTITLE_LINE_HEIGHT
    if (subtitleHeight > 0f) {
      return TITLE_AREA_HEIGHT + subtitleHeight
    }
    if (this is NodeData.Tip || this is NodeData.Coat) {
      return TITLE_AREA_HEIGHT + PREVIEW_AREA_HEIGHT
    }
    return TITLE_AREA_HEIGHT
  }



  /** Wraps a [ProtoBrushTip]. */
  data class Tip(
    val tip: ProtoBrushTip,
    val behaviorPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels() = listOf("Behaviors")

    override fun title() = "Tip"

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (portId in behaviorPortIds) {
            ports.add(Port.Input(nodeId, portId, "Behavior"))
        }
        ports.add(Port.Add(nodeId, "add_behavior", "Add behavior..."))
        return ports
    }
  }

  /** Wraps a [ProtoBrushPaint]. */
  data class Paint(
    val paint: ProtoBrushPaint,
    val texturePortIds: List<String> = emptyList(),
    val colorPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels(): List<String> {
      val labels = mutableListOf<String>()
      for (i in texturePortIds.indices) labels.add("Texture")
      labels.add("Add Texture...")
      for (i in colorPortIds.indices) labels.add("Color")
      labels.add("Add Color...")
      return labels
    }

    override fun title() = "Paint"

    override fun subtitles() = listOf("overlap: ${paint.selfOverlap.displayString()}")

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (portId in texturePortIds) {
            ports.add(Port.Input(nodeId, portId, "Texture"))
        }
        ports.add(Port.AddTexture(nodeId, "add_texture", "Add Texture..."))

        for (portId in colorPortIds) {
            ports.add(Port.Input(nodeId, portId, "Color"))
        }
        ports.add(Port.AddColor(nodeId, "add_color", "Add Color..."))
        return ports
    }
  }

  /** Wraps a [ProtoBrushPaint.TextureLayer]. */
  data class TextureLayer(
    val layer: ProtoBrushPaint.TextureLayer
  ) : NodeData {
    override fun title() = "Texture Layer"

        override fun subtitles() = listOf(layer.clientTextureId)
  }

  /** Wraps a [ProtoColorFunction]. */
  data class ColorFunc(
    val function: ProtoColorFunction
  ) : NodeData {
    override fun title() = "Color Function"

    override fun subtitles() =
      listOf(if (function.hasOpacityMultiplier()) "opacity multiplier" else "replace color")
  }

  data class Behavior(
    val node: ProtoBrushBehavior.Node,
    val developerComment: String = "",
    val behaviorId: String = "",
    val inputPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels(): List<String> {
      return when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> listOf("Input")
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> listOf("Input")
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> listOf("Input")
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> listOf("Input")
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> listOf("A", "B")
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> listOf("Value", "Start", "End")
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> listOf("Input")
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> listOf("Angle", "Mag")
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
        ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> "Source"
        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> "Constant"
        ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> "Noise"
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> "Tool Type Filter"
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> "Damping"
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> "Response"
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> "Integral"
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> "Binary Op"
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> "Interpolation"
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> "Target"
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> "Polar Target"
        else -> "Unknown"
      }

    override fun subtitles(): List<String> {
      val s = when (node.nodeCase) {
        ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> node.sourceNode.source.displayString()
        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> "%.2f".format(node.constantNode.value)
        ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
          return listOf(node.noiseNode.varyOver.displayString(), "period: ${node.noiseNode.basePeriod}")
        ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> {
          val bitmask = node.toolTypeFilterNode.enabledToolTypes
          val enabled = mutableListOf<String>()
          if (bitmask and (1 shl 0) != 0) enabled.add("unknown")
          if (bitmask and (1 shl 1) != 0) enabled.add("mouse")
          if (bitmask and (1 shl 2) != 0) enabled.add("touch")
          if (bitmask and (1 shl 3) != 0) enabled.add("stylus")
          if (enabled.isEmpty()) "none" else enabled.joinToString(", ")
        }
        ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> {
          val source = node.dampingNode.dampingSource
          val unit = when (source) {
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS -> "cm"
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE -> "size"
            ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_TIME_IN_SECONDS -> "s"
            else -> ""
          }
          return listOf(source.displayString(), "gap: ${node.dampingNode.dampingGap}$unit")
        }
        ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> node.responseNode.displayString()
        ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> node.integralNode.integrateOver.displayString()
        ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> node.binaryOpNode.operation.displayString()
        ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> node.interpolationNode.interpolation.displayString()
        ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> node.targetNode.target.displayString()
        ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> node.polarTargetNode.target.displayString()
        else -> node.nodeCase.name
      }
      return listOf(s)
    }

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        when (node.nodeCase) {
            ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
                val labels = inputLabels()
                for (i in labels.indices) {
                    val portId = if (i < inputPortIds.size) inputPortIds[i] else labels[i]
                    ports.add(Port.Input(nodeId, portId, labels[i]))
                }
            }
            ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
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
                    ports.add(Port.Input(nodeId, portId, label))
                    nextIndex++
                }
                ports.add(Port.Add(nodeId, "add_input", "Add input..."))
            }
            ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
                val labels = inputLabels()
                for (i in inputPortIds.indices) {
                    val label = labels[i % labels.size]
                    ports.add(Port.Input(nodeId, inputPortIds[i], label))
                }
                ports.add(Port.Add(nodeId, "add_inputs", "Add inputs..."))
            }
            else -> {
                val labels = inputLabels()
                if (labels.size == 1) {
                    for (portId in inputPortIds) {
                        ports.add(Port.Input(nodeId, portId, labels[0]))
                    }
                    ports.add(Port.Add(nodeId, "add_input", "Add input..."))
                } else {
                    for (i in labels.indices) {
                        val portId = if (i < inputPortIds.size) inputPortIds[i] else labels[i]
                        ports.add(Port.Input(nodeId, portId, labels[i]))
                    }
                }
            }
        }
        return ports
    }
  }

  /** Represents a Brush Coat (structural). */
  data class Coat(
    val tipPortId: String = "tip",
    val paintPortIds: List<String> = emptyList()
  ) : NodeData {
    override fun inputLabels(): List<String> {
      val labels = mutableListOf("Tip")
      for (i in paintPortIds.indices) {
        labels.add("Paint")
      }
      labels.add("Add Paint...")
      return labels
    }

    override fun title() = "Coat"

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        ports.add(Port.Input(nodeId, tipPortId, "Tip"))
        for (portId in paintPortIds) {
            ports.add(Port.Input(nodeId, portId, "Paint"))
        }
        ports.add(Port.Add(nodeId, "add_paint", "Add Paint..."))
        return ports
    }
  }

  /** Represents the Brush Family root (structural). */
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
    override fun inputLabels(): List<String> {
      val labels = mutableListOf<String>()
      for (i in coatPortIds.indices) {
        labels.add("Coat $i")
      }
      labels.add("Add Coat...")
      return labels
    }

    override fun title() = "Family"

        override fun subtitles() = listOf(clientBrushFamilyId)

    override fun width() = 3 * NODE_WIDTH

    override fun hasOutput() = false

    override fun getVisiblePorts(nodeId: String, graph: BrushGraph): List<Port> {
        val ports = mutableListOf<Port>()
        for (i in coatPortIds.indices) {
            ports.add(Port.Input(nodeId, coatPortIds[i], "Coat $i"))
        }
        ports.add(Port.Add(nodeId, "add_coat", "Add Coat..."))
        return ports
    }
  }
}

/** Side of a node where a port is located. */
enum class PortSide {
  INPUT,
  OUTPUT,
}

/** The severity of a validation issue. */
enum class ValidationSeverity {
  ERROR,
  WARNING,
  DEBUG,
}

/** Exception thrown when the brush graph fails validation. */
data class GraphValidationException(
  override val message: String,
  val nodeId: String? = null,
  val severity: ValidationSeverity = ValidationSeverity.ERROR,
) : IllegalStateException(message)

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
) {
  companion object {
    /** Returns whether a connection from [from] to [to] at [toPortId] is valid. */
    fun isValidConnection(from: GraphNode, to: GraphNode, toPortId: String, graph: BrushGraph = BrushGraph()): String? {
      val fromData = from.data
      val toData = to.data
      val fromIsStructural =
        fromData is NodeData.Tip ||
          fromData is NodeData.Coat ||
          fromData is NodeData.Paint ||
          fromData is NodeData.TextureLayer ||
          fromData is NodeData.ColorFunc ||
          fromData is NodeData.Family
      val toIsStructural =
        toData is NodeData.Tip ||
          toData is NodeData.Coat ||
          toData is NodeData.Paint ||
          toData is NodeData.TextureLayer ||
          toData is NodeData.ColorFunc ||
          toData is NodeData.Family

      val toPort = to.getVisiblePorts(graph).find { it.id == toPortId }

      return when (toData) {
        is NodeData.Coat -> {
          if (toPortId == toData.tipPortId) {
            if (fromData is NodeData.Tip) {
              null
            } else {
              "Coat can only accept input from Tip at the tip port"
            }
          } else if (toData.paintPortIds.contains(toPortId) || toPort is Port.Add) {
            if (fromData is NodeData.Paint) {
              null
            } else {
              "Coat can only accept input from Paint at paint ports"
            }
          } else {
            "Invalid port for Coat"
          }
        }
        is NodeData.Family -> {
          if (toData.coatPortIds.contains(toPortId) || toPort is Port.Add) {
            if (fromData is NodeData.Coat) {
              null
            } else {
              "Family can only connect to Coat"
            }
          } else {
            "Invalid port for Family"
          }
        }
        is NodeData.Tip -> {
          if (
            !(fromData is NodeData.Behavior) ||
              (fromData.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
                fromData.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
          ) {
            "Tip can only accept input from Target or PolarTarget"
          } else {
            null
          }
        }
        is NodeData.Paint -> {
            if (toData.texturePortIds.contains(toPortId) || toPort is Port.AddTexture) {
                if (fromData is NodeData.TextureLayer) {
                  null
                } else {
                  "Paint can only accept input from TextureLayer at Texture ports"
                }
            } else if (toData.colorPortIds.contains(toPortId) || toPort is Port.AddColor) {
                if (fromData is NodeData.ColorFunc) {
                  null
                } else {
                  "Paint can only accept input from ColorFunction at Color ports"
                }
            } else {
                "Invalid port for Paint"
            }
        }
        is NodeData.TextureLayer -> "TextureLayer cannot accept inputs"
        is NodeData.ColorFunc -> "ColorFunction cannot accept inputs"
        else -> {
          // 'to' is a behavior node.
          if (
            fromData is NodeData.Behavior &&
              (fromData.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ||
                fromData.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
          ) {
            // Targets can only connect to Tip.
            "Behavior node ${toData.title()} cannot accept input from ${fromData.title()}"
          } else if (!fromIsStructural && !toIsStructural) {
            null
          } else {
            "Behavior node ${toData.title()} cannot accept input from structural node ${fromData.title()}"
          }
        }
      }
    }
  }
}

sealed class Port(
    val nodeId: String,
    val id: String,
    val label: String? = null,
    val isAddPort: Boolean = false
) {
    abstract val side: PortSide

    class Output(nodeId: String, id: String = "output", label: String? = null) : 
        Port(nodeId, id, label, isAddPort = false) {
        override val side = PortSide.OUTPUT
    }

    class Input(nodeId: String, id: String, label: String? = null) : 
        Port(nodeId, id, label, isAddPort = false) {
        override val side = PortSide.INPUT
    }

    class Add(nodeId: String, id: String, label: String? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddTexture(nodeId: String, id: String, label: String? = null) : 
        Port(nodeId, id, label, isAddPort = true) {
        override val side = PortSide.INPUT
    }

    class AddColor(nodeId: String, id: String, label: String? = null) : 
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
          ink.proto.BrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
            incomingEdges.take(3).forEachIndexed { index, edge ->
              val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else java.util.UUID.randomUUID().toString()
              newIds.add(portId)
              updatedEdges.add(edge.copy(toPortId = portId))
            }
          }
          ink.proto.BrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
            incomingEdges.take(26).forEachIndexed { index, edge ->
              val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else java.util.UUID.randomUUID().toString()
              newIds.add(portId)
              updatedEdges.add(edge.copy(toPortId = portId))
            }
          }
          ink.proto.BrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
            incomingEdges.forEachIndexed { index, edge ->
              val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else java.util.UUID.randomUUID().toString()
              newIds.add(portId)
              updatedEdges.add(edge.copy(toPortId = portId))
            }
          }
          else -> {
            val labels = newData.inputLabels()
            if (labels.size == 1) {
              incomingEdges.forEachIndexed { index, edge ->
                val portId = if (index < newData.inputPortIds.size) newData.inputPortIds[index] else java.util.UUID.randomUUID().toString()
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

fun GraphNode.inferNodeDataForPort(port: Port): NodeData? {
    val data = this.data
    return when (data) {
      is NodeData.Family -> {
        if (port.label?.contains("Coat", ignoreCase = true) == true) {
          NodeData.Coat()
        } else {
          null
        }
      }
      is NodeData.Coat -> {
        if (port.label?.contains("Tip", ignoreCase = true) == true) {
          NodeData.Tip(ProtoBrushTip.getDefaultInstance())
        } else if (port.label?.contains("Paint", ignoreCase = true) == true) {
          NodeData.Paint(ProtoBrushPaint.getDefaultInstance())
        } else {
          null
        }
      }
      is NodeData.Tip -> {
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setTargetNode(
              ProtoBrushBehavior.TargetNode.newBuilder()
                .setTarget(ink.proto.BrushBehavior.Target.TARGET_OPACITY_MULTIPLIER)
                .setTargetModifierRangeStart(0.0f)
                .setTargetModifierRangeEnd(1.0f)
            )
            .build(),
          "",
          UUID.randomUUID().toString()
        )
      }
      is NodeData.Behavior -> {
        // Default behavior is source node
        NodeData.Behavior(
          ProtoBrushBehavior.Node.newBuilder()
            .setSourceNode(
              ProtoBrushBehavior.SourceNode.newBuilder()
                .setSource(ink.proto.BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                .setSourceOutOfRangeBehavior(ink.proto.BrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
                .setSourceValueRangeStart(0.0f)
                .setSourceValueRangeEnd(1.0f)
            )
            .build(),
          "",
          data.behaviorId
        )
      }
      is NodeData.Paint -> {
        if (port.label?.contains("Texture") == true) {
          NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance())
        } else if (port.label?.contains("Color") == true) {
          NodeData.ColorFunc(ProtoColorFunction.newBuilder()
            .setReplaceColor(
                    ink.proto.Color.newBuilder()
                      .setRed(0f)
                      .setGreen(0f)
                      .setBlue(0f)
                      .setAlpha(1f)
                      .build()
                  ).build())
        } else {
          null
        }
      }
      else -> null
    }
}


