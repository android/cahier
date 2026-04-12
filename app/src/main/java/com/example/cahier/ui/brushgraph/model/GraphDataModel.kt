@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.ui.brushgraph.model

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

/** Representation of a single node in the brush behavior graph. */
data class GraphNode(
  val id: String = UUID.randomUUID().toString(),
  val data: NodeData,
  val position: Offset,
  val isExpanded: Boolean = false,
  val hasError: Boolean = false,
  val hasWarning: Boolean = false,
  val isDisabled: Boolean = false,
)

/** Represents the core data/component within a node. */
sealed interface NodeData {

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
  fun height(): Float {
    val inputCount = inputLabels().size
    val previewH = if (this is NodeData.ColorFunc || this is NodeData.TextureLayer) PREVIEW_AREA_HEIGHT else 0f
    return NODE_PADDING_VERTICAL +
      titleHeight() +
      previewH +
      maxOf(inputCount, 1) * INPUT_ROW_HEIGHT +
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

  fun getPortPosition(side: PortSide, index: Int): Offset {
    val w = width()
    val yOffset = NODE_PADDING_VERTICAL + titleHeight() + (index + 0.5f) * INPUT_ROW_HEIGHT

    return when (side) {
      PortSide.INPUT -> Offset(0f, yOffset)
      PortSide.OUTPUT -> Offset(w, yOffset)
    }
  }

  /** Wraps a [ProtoBrushTip]. */
  data class Tip(val tip: ProtoBrushTip) : NodeData {
    override fun inputLabels() = listOf("Behaviors")

    override fun title() = "Tip"
  }

  /** Wraps a [ProtoBrushPaint]. */
  data class Paint(val paint: ProtoBrushPaint) : NodeData {
    override fun inputLabels() = listOf("Texture", "Color")

    override fun title() = "Paint"

        override fun subtitles() = listOf("overlap: ${paint.selfOverlap.displayString()}")
  }

  /** Wraps a [ProtoBrushPaint.TextureLayer]. */
  data class TextureLayer(val layer: ProtoBrushPaint.TextureLayer) : NodeData {
    override fun title() = "Texture Layer"

        override fun subtitles() = listOf(layer.clientTextureId)
  }

  /** Wraps a [ProtoColorFunction]. */
  data class ColorFunc(val function: ProtoColorFunction) : NodeData {
    override fun title() = "Color Function"

    override fun subtitles() =
      listOf(if (function.hasOpacityMultiplier()) "opacity multiplier" else "replace color")
  }

  data class Behavior(
    val node: ProtoBrushBehavior.Node,
    val developerComment: String = "",
    val behaviorId: String = ""
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
        ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> "%.1f".format(node.constantNode.value)
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
  }

  /** Represents a Brush Coat (structural). */
  object Coat : NodeData {
    override fun inputLabels() = listOf("Tip", "Paint")

    override fun title() = "Coat"
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
    val numCoats: Int = 0,
  ) : NodeData {
    override fun inputLabels(): List<String> {
      val labels = mutableListOf<String>()
      for (i in 0 until numCoats) {
        labels.add("Coat $i")
      }
      labels.add("Add Coat...")
      return labels
    }

    override fun title() = "Family"

        override fun subtitles() = listOf(clientBrushFamilyId)

    override fun width() = 3 * NODE_WIDTH

    override fun hasOutput() = false
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
data class GraphEdge(val fromNodeId: String, val toNodeId: String, val toInputIndex: Int = 0, val isDisabled: Boolean = false)

/** Represents the entire node graph state. */
data class BrushGraph(
  val nodes: List<GraphNode> = emptyList(),
  val edges: List<GraphEdge> = emptyList(),
) {
  companion object {
    /** Returns whether a connection from [from] to [to] at [toIndex] is valid. */
    fun isValidConnection(from: NodeData, to: NodeData, toIndex: Int): String? {
      val inputLabels = to.inputLabels()
      if (toIndex < 0 || toIndex >= inputLabels.size) {
        return "Index out of bounds"
      }

      val fromIsStructural =
        from is NodeData.Tip ||
          from is NodeData.Coat ||
          from is NodeData.Paint ||
          from is NodeData.TextureLayer ||
          from is NodeData.ColorFunc ||
          from is NodeData.Family
      val toIsStructural =
        to is NodeData.Tip ||
          to is NodeData.Coat ||
          to is NodeData.Paint ||
          to is NodeData.TextureLayer ||
          to is NodeData.ColorFunc ||
          to is NodeData.Family

      return when (to) {
        is NodeData.Coat -> {
          if (toIndex == 0) {
            if (from is NodeData.Tip) {
              null
            } else {
              "Coat can only accept input from Tip at the first port"
            }
          } else if (toIndex == 1) {
            if (from is NodeData.Paint) {
              null
            } else {
              "Coat can only accept input from Paint at the second port"
            }
          } else {
            "Internal: Coat can only accept input at index 0 or 1"
          }
        }
        is NodeData.Family -> {
          if (from is NodeData.Coat) {
            null
          } else {
            "Family can only connect to Coat"
          }
        }
        is NodeData.Tip -> {
          if (toIndex != 0) {
            "Internal: Tip should only accept input at index 0"
          } else if (
            !(from is NodeData.Behavior) ||
              (from.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.TARGET_NODE &&
                from.node.nodeCase != ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
          ) {
            "Tip can only accept input from Target or PolarTarget"
          } else {
            null
          }
        }
        is NodeData.Paint -> {
          if (toIndex == 0) {
            if (from is NodeData.TextureLayer) {
              null
            } else {
              "Paint can only accept input from TextureLayer at the first port"
            }
          } else if (toIndex == 1) {
            if (from is NodeData.ColorFunc) {
              null
            } else {
              "Paint can only accept input from ColorFunction at the second port"
            }
          } else {
            "Internal: Paint can only accept input at index 0 or 1"
          }
        }
        is NodeData.TextureLayer -> "TextureLayer cannot accept inputs"
        is NodeData.ColorFunc -> "ColorFunction cannot accept inputs"
        else -> {
          // 'to' is a behavior node.
          if (
            from is NodeData.Behavior &&
              (from.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ||
                from.node.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE)
          ) {
            // Targets can only connect to Tip.
            "Behavior node ${to.title()} cannot accept input from ${from.title()}"
          } else if (!fromIsStructural && !toIsStructural) {
            null
          } else {
            "Behavior node ${to.title()} cannot accept input from structural node ${from.title()}"
          }
        }
      }
    }
  }
}
