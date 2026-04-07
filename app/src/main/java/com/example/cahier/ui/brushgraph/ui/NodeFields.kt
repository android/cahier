@file:OptIn(
  androidx.ink.brush.ExperimentalInkCustomBrushApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.cahier.ui.brushgraph.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.proto.BrushBehavior as ProtoBrushBehavior
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import androidx.ink.brush.InputToolType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.cahier.ui.brushdesigner.BrushSliderControl

import com.example.cahier.ui.brushgraph.ui.TipPreviewWidget
import com.example.cahier.ui.brushgraph.model.*

/** Renders the editable fields for a node. */
@Composable
fun NodeFields(
  node: GraphNode,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onUpdate: (NodeData) -> Unit,
  textFieldsLocked: Boolean,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: androidx.ink.rendering.android.canvas.CanvasStrokeRenderer,
) {
  Column(
    modifier =
      Modifier.padding(top = 8.dp).heightIn(max = 600.dp).verticalScroll(rememberScrollState())
  ) {
    val nodeDataTypeName =
      when (val data = node.data) {
        is NodeData.Behavior -> data.node.nodeCase.name.removeSuffix("_NODE")
        is NodeData.Tip -> "Tip"
        is NodeData.Paint -> "Paint"
        is NodeData.TextureLayer -> "TextureLayer"
        is NodeData.ColorFunc -> "ColorFunction"
        is NodeData.Coat -> "Coat"
        is NodeData.Family -> "BrushFamily"
      }

    var expandedNodeTypes by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
      expanded = expandedNodeTypes,
      onExpandedChange = { expandedNodeTypes = it }
    ) {
      OutlinedTextField(
        value = prettyDisplayString(nodeDataTypeName),
        onValueChange = {},
        readOnly = true,
        label = { Text("Node Type") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNodeTypes) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedNodeTypes,
        onDismissRequest = { expandedNodeTypes = false }
      ) {
        NODE_TYPES.forEach { newTypeName ->
          DropdownMenuItem(
            text = { Text(prettyDisplayString(newTypeName)) },
            onClick = {
              if (newTypeName != nodeDataTypeName) {
                onUpdate(createDefaultNode(newTypeName))
              }
              expandedNodeTypes = false
            }
          )
        }
      }
    }

    when (val data = node.data) {
      is NodeData.Behavior -> {
        val behaviorNode = data.node
        when (behaviorNode.nodeCase) {
          ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> {
            val sourceNode = behaviorNode.sourceNode
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expanded,
              onExpandedChange = { expanded = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(sourceNode.source),
                onValueChange = {},
                readOnly = true,
                label = { Text("Source") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
              ) {
                ALL_SOURCES.forEach { source ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(source)) },
                    onClick = {
                      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(source = source))))
                      expanded = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Range Start",
              value = sourceNode.sourceValueRangeStart,
              valueRange = 0f..1f,
              onValueChange = {
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceValueRangeStart = it))))
              }
            )
            BrushSliderControl(
              label = "Range End",
              value = sourceNode.sourceValueRangeEnd,
              valueRange = 0f..1f,
              onValueChange = {
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceValueRangeEnd = it))))
              }
            )
            var expandedOor by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedOor,
              onExpandedChange = { expandedOor = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(sourceNode.sourceOutOfRangeBehavior),
                onValueChange = {},
                readOnly = true,
                label = { Text("Out of Range Behavior") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOor) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedOor,
                onDismissRequest = { expandedOor = false }
              ) {
                ALL_OUT_OF_RANGE.forEach { oor ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(oor)) },
                    onClick = {
                      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceOutOfRangeBehavior = oor))))
                      expandedOor = false
                    }
                  )
                }
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> {
            val constantNode = behaviorNode.constantNode
            BrushSliderControl(
              label = "Value",
              value = constantNode.value,
              valueRange = -100f..100f,
              onValueChange = {
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(constantNode = constantNode.safeCopy(value = it))))
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> {
            val noiseNode = behaviorNode.noiseNode
            BrushSliderControl(
              label = "Seed",
              value = noiseNode.seed.toFloat(),
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(seed = it.toInt()))))
              }
            )
            var expandedVary by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedVary,
              onExpandedChange = { expandedVary = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(noiseNode.varyOver),
                onValueChange = {},
                readOnly = true,
                label = { Text("Vary Over") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVary) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedVary,
                onDismissRequest = { expandedVary = false }
              ) {
                ALL_PROGRESS_DOMAINS.forEach { domain ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(domain)) },
                    onClick = {
                      onUpdate(NodeData.Behavior(behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(varyOver = domain))))
                      expandedVary = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Base Period",
              value = noiseNode.basePeriod,
              valueRange = 0.001f..100f,
              onValueChange = {
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(basePeriod = it))))
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE -> {
            val filterNode = behaviorNode.toolTypeFilterNode
            Text("Enabled Tool Types:", fontSize = 12.sp)
            ALL_TOOL_TYPES.forEach { toolType ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                val bitIndex = toolTypeBitIndex(toolType)
                Checkbox(
                  checked = (filterNode.enabledToolTypes and (1 shl bitIndex)) != 0,
                  onCheckedChange = { checked ->
                    val newMask =
                      if (checked) {
                        filterNode.enabledToolTypes or (1 shl bitIndex)
                      } else {
                        filterNode.enabledToolTypes and (1 shl bitIndex).inv()
                      }
                    onUpdate(
                      NodeData.Behavior(
                        behaviorNode.safeCopy(
                          toolTypeFilterNode = filterNode.safeCopy(enabledToolTypes = newMask)
                        )
                      )
                    )
                  }
                )
                Text(prettyDisplayString(toolType))
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE -> {
            val dampingNode = behaviorNode.dampingNode
            var expandedSource by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedSource,
              onExpandedChange = { expandedSource = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(dampingNode.dampingSource),
                onValueChange = {},
                readOnly = true,
                label = { Text("Damping Source") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedSource,
                onDismissRequest = { expandedSource = false }
              ) {
                ALL_PROGRESS_DOMAINS.forEach { domain ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(domain)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            dampingNode = dampingNode.safeCopy(dampingSource = domain)
                          )
                        )
                      )
                      expandedSource = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Damping Gap",
              value = dampingNode.dampingGap,
              valueRange = 0f..10f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(dampingNode = dampingNode.safeCopy(dampingGap = it))
                  )
                )
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> {
            // ResponseNode editing is complex, typically involves a curve editor.
            // For now, we'll just show it's a response node.
            Text("Response Node: (Curve editing not yet implemented in this view)")
          }
          ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> {
            val integralNode = behaviorNode.integralNode
            var expandedOver by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedOver,
              onExpandedChange = { expandedOver = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(integralNode.integrateOver),
                onValueChange = {},
                readOnly = true,
                label = { Text("Integrate Over") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOver) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedOver,
                onDismissRequest = { expandedOver = false }
              ) {
                ALL_PROGRESS_DOMAINS.forEach { domain ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(domain)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            integralNode = integralNode.safeCopy(integrateOver = domain)
                          )
                        )
                      )
                      expandedOver = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Range Start",
              value = integralNode.integralValueRangeStart,
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      integralNode = integralNode.safeCopy(integralValueRangeStart = it)
                    )
                  )
                )
              }
            )
            BrushSliderControl(
              label = "Range End",
              value = integralNode.integralValueRangeEnd,
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      integralNode = integralNode.safeCopy(integralValueRangeEnd = it)
                    )
                  )
                )
              }
            )
            var expandedIntegralOor by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedIntegralOor,
              onExpandedChange = { expandedIntegralOor = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(integralNode.integralOutOfRangeBehavior),
                onValueChange = {},
                readOnly = true,
                label = { Text("Out of Range Behavior") },
                trailingIcon = {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIntegralOor)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedIntegralOor,
                onDismissRequest = { expandedIntegralOor = false }
              ) {
                ALL_OUT_OF_RANGE.forEach { oor ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(oor)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            integralNode = integralNode.safeCopy(integralOutOfRangeBehavior = oor)
                          )
                        )
                      )
                      expandedIntegralOor = false
                    }
                  )
                }
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE -> {
            val binaryNode = behaviorNode.binaryOpNode
            var expandedOp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedOp,
              onExpandedChange = { expandedOp = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(binaryNode.operation),
                onValueChange = {},
                readOnly = true,
                label = { Text("Operation") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOp) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedOp,
                onDismissRequest = { expandedOp = false }
              ) {
                ALL_BINARY_OPS.forEach { op ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(op)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(binaryOpNode = binaryNode.safeCopy(operation = op))
                        )
                      )
                      expandedOp = false
                    }
                  )
                }
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
            val interpNode = behaviorNode.interpolationNode
            var expandedInterp by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedInterp,
              onExpandedChange = { expandedInterp = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(interpNode.interpolation),
                onValueChange = {},
                readOnly = true,
                label = { Text("Interpolation") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterp) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedInterp,
                onDismissRequest = { expandedInterp = false }
              ) {
                ALL_INTERPOLATIONS.forEach { interp ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(interp)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            interpolationNode = interpNode.safeCopy(interpolation = interp)
                          )
                        )
                      )
                      expandedInterp = false
                    }
                  )
                }
              }
            }
          }
          ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> {
            val targetNode = behaviorNode.targetNode
            var expandedTarget by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedTarget,
              onExpandedChange = { expandedTarget = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(targetNode.target),
                onValueChange = {},
                readOnly = true,
                label = { Text("Target") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTarget) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedTarget,
                onDismissRequest = { expandedTarget = false }
              ) {
                ALL_TARGETS.forEach { target ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(target)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(targetNode = targetNode.safeCopy(target = target))
                        )
                      )
                      expandedTarget = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Range Start",
              value = targetNode.targetModifierRangeStart,
              valueRange = 0f..1f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      targetNode = targetNode.safeCopy(targetModifierRangeStart = it)
                    )
                  )
                )
              }
            )
            BrushSliderControl(
              label = "Range End",
              value = targetNode.targetModifierRangeEnd,
              valueRange = 0f..1f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(targetNode = targetNode.safeCopy(targetModifierRangeEnd = it))
                  )
                )
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
            val polarNode = behaviorNode.polarTargetNode
            var expandedPolar by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expandedPolar,
              onExpandedChange = { expandedPolar = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(polarNode.target),
                onValueChange = {},
                readOnly = true,
                label = { Text("Polar Target") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPolar) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedPolar,
                onDismissRequest = { expandedPolar = false }
              ) {
                ALL_POLAR_TARGETS.forEach { target ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(target)) },
                    onClick = {
                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            polarTargetNode = polarNode.safeCopy(target = target)
                          )
                        )
                      )
                      expandedPolar = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Angle Start",
              value = polarNode.angleRangeStart,
              valueRange = 0f..6.28f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeStart = it))
                  )
                )
              }
            )
            BrushSliderControl(
              label = "Angle End",
              value = polarNode.angleRangeEnd,
              valueRange = 0f..6.28f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeEnd = it))
                  )
                )
              }
            )
            BrushSliderControl(
              label = "Mag Start",
              value = polarNode.magnitudeRangeStart,
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      polarTargetNode = polarNode.safeCopy(magnitudeRangeStart = it)
                    )
                  )
                )
              }
            )
            BrushSliderControl(
              label = "Mag End",
              value = polarNode.magnitudeRangeEnd,
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      polarTargetNode = polarNode.safeCopy(magnitudeRangeEnd = it)
                    )
                  )
                )
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.FALLBACK_FILTER_NODE -> {
            Text("Fallback Filter Node")
          }
          ProtoBrushBehavior.Node.NodeCase.NODE_NOT_SET -> {
            Text("No node data set")
          }
          else -> {
            Text("Unknown Behavior Node Type")
          }
        }
      }
      is NodeData.Tip -> {
        val tip = data.tip
        TipPreviewWidget(tip, strokeRenderer)

        BrushSliderControl(
          label = "Scale X",
          value = tip.scaleX,
          valueRange = 0f..2f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleX = it))) }
        )
        BrushSliderControl(
          label = "Scale Y",
          value = tip.scaleY,
          valueRange = 0f..2f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleY = it))) }
        )
        BrushSliderControl(
          label = "Corner Rounding",
          value = tip.cornerRounding,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(cornerRounding = it))) }
        )
        BrushSliderControl(
          label = "Slant Degrees",
          value = Math.toDegrees(tip.slantRadians.toDouble()).toFloat(),
          valueRange = -90f..90f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(slantRadians = Math.toRadians(it.toDouble()).toFloat()))) }
        )
        BrushSliderControl(
          label = "Pinch",
          value = tip.pinch,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(pinch = it))) }
        )
        BrushSliderControl(
          label = "Rotation Degrees",
          value = Math.toDegrees(tip.rotationRadians.toDouble()).toFloat(),
          valueRange = 0f..360f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(rotationRadians = Math.toRadians(it.toDouble()).toFloat()))) }
        )
        BrushSliderControl(
          label = "Particle Gap Distance Scale",
          value = tip.particleGapDistanceScale,
          valueRange = 0f..5f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDistanceScale = it))) }
        )
        BrushSliderControl(
          label = "Particle Gap Duration (ms)",
          value = tip.particleGapDurationSeconds * 1000f,
          valueRange = 0f..1000f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDurationSeconds = it / 1000f))) }
        )
      }
      is NodeData.Coat -> {
        Text("Coat Node: Connect Tip and Paint to this node.", fontSize = 12.sp)
      }
      is NodeData.Paint -> {
        val paint = data.paint
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(paint.selfOverlap),
            onValueChange = {},
            readOnly = true,
            label = { Text("Self Overlap") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            arrayOf(
              ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY,
              ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE,
              ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD,
            ).forEach { so ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(so)) },
                onClick = {
                  onUpdate(NodeData.Paint(paint.safeCopy(selfOverlap = so)))
                  expanded = false
                }
              )
            }
          }
        }
      }
      is NodeData.TextureLayer -> {
        val layer = data.layer
        Row(verticalAlignment = Alignment.Bottom) {
          Box(modifier = Modifier.weight(1f)) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = expanded,
              onExpandedChange = { expanded = it }
            ) {
              OutlinedTextField(
                value = prettyDisplayString(layer.clientTextureId),
                onValueChange = {},
                readOnly = true,
                label = { Text("Texture ID") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
              ) {
                allTextureIds.forEach { id ->
                  DropdownMenuItem(
                    text = { Text(prettyDisplayString(id)) },
                    onClick = {
                      onUpdate(NodeData.TextureLayer(layer.safeCopy(clientTextureId = id)))
                      expanded = false
                    }
                  )
                }
              }
            }
          }
          IconButton(onClick = onLoadTexture, enabled = true) {
            Icon(Icons.Default.Upload, contentDescription = "Upload Texture")
          }
        }
        TextureLayerPreviewWidget(textureLayer = layer, renderer = strokeRenderer)
        BrushSliderControl(
          label = "Size X",
          value = layer.sizeX,
          valueRange = 0.1f..1000f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeX = it))) }
        )
        BrushSliderControl(
          label = "Size Y",
          value = layer.sizeY,
          valueRange = 0.1f..1000f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeY = it))) }
        )
        var expandedMapping by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedMapping,
          onExpandedChange = { expandedMapping = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.mapping),
            onValueChange = {},
            readOnly = true,
            label = { Text("Mapping") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapping) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedMapping,
            onDismissRequest = { expandedMapping = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING,
              ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING,
            ).forEach { mapping ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(mapping)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(mapping = mapping)))
                  expandedMapping = false
                }
              )
            }
          }
        }
        var expandedUnit by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedUnit,
          onExpandedChange = { expandedUnit = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.sizeUnit),
            onValueChange = {},
            readOnly = true,
            label = { Text("Size Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedUnit,
            onDismissRequest = { expandedUnit = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE,
              ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES,
            ).forEach { unit ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(unit)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeUnit = unit)))
                  expandedUnit = false
                }
              )
            }
          }
        }
        BrushSliderControl(
          label = "Offset X",
          value = layer.offsetX,
          valueRange = -1f..1f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetX = it))) }
        )
        BrushSliderControl(
          label = "Offset Y",
          value = layer.offsetY,
          valueRange = -1f..1f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetY = it))) }
        )
        BrushSliderControl(
          label = "Rotation Degrees",
          value = Math.toDegrees(layer.rotationInRadians.toDouble()).toFloat(),
          valueRange = 0f..360f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(rotationInRadians = Math.toRadians(it.toDouble()).toFloat()))) }
        )
        BrushSliderControl(
          label = "Animation Rows",
          value = layer.animationRows.toFloat(),
          valueRange = 1f..100f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationRows = it.toInt()))) }
        )
        BrushSliderControl(
          label = "Animation Columns",
          value = layer.animationColumns.toFloat(),
          valueRange = 1f..100f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationColumns = it.toInt()))) }
        )
        BrushSliderControl(
          label = "Animation Frames",
          value = layer.animationFrames.toFloat(),
          valueRange = 1f..100f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationFrames = it.toInt()))) }
        )
        BrushSliderControl(
          label = "Animation Duration (ms)",
          value = layer.animationDurationSeconds * 1000f,
          valueRange = 1f..10000f,
          onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationDurationSeconds = it / 1000f))) }
        )
        var expandedOrigin by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedOrigin,
          onExpandedChange = { expandedOrigin = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.origin),
            onValueChange = {},
            readOnly = true,
            label = { Text("Origin") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigin) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedOrigin,
            onDismissRequest = { expandedOrigin = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN,
              ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT,
              ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT,
            ).forEach { origin ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(origin)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(origin = origin)))
                  expandedOrigin = false
                }
              )
            }
          }
        }
        var expandedWrapX by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedWrapX,
          onExpandedChange = { expandedWrapX = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.wrapX),
            onValueChange = {},
            readOnly = true,
            label = { Text("Wrap X") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWrapX) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedWrapX,
            onDismissRequest = { expandedWrapX = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
            ).forEach { wrap ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(wrap)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapX = wrap)))
                  expandedWrapX = false
                }
              )
            }
          }
        }
        var expandedWrapY by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedWrapY,
          onExpandedChange = { expandedWrapY = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.wrapY),
            onValueChange = {},
            readOnly = true,
            label = { Text("Wrap Y") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWrapY) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedWrapY,
            onDismissRequest = { expandedWrapY = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
              ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
            ).forEach { wrap ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(wrap)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapY = wrap)))
                  expandedWrapY = false
                }
              )
            }
          }
        }
        var expandedBlend by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedBlend,
          onExpandedChange = { expandedBlend = it }
        ) {
          OutlinedTextField(
            value = prettyDisplayString(layer.blendMode),
            onValueChange = {},
            readOnly = true,
            label = { Text("Blend Mode") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBlend) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedBlend,
            onDismissRequest = { expandedBlend = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP,
              ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR,
            ).forEach { mode ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(mode)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(blendMode = mode)))
                  expandedBlend = false
                }
              )
            }
          }
        }
      }
      is NodeData.ColorFunc -> {
        val function = data.function
        var expandedType by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedType,
          onExpandedChange = { expandedType = it }
        ) {
          OutlinedTextField(
            value = if (function.hasOpacityMultiplier()) "OPACITY" else "REPLACE",
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedType,
            onDismissRequest = { expandedType = false }
          ) {
            arrayOf("OPACITY", "REPLACE").forEach { type ->
              DropdownMenuItem(
                text = { Text(type) },
                onClick = {
                  onUpdate(
                    if (type == "OPACITY") {
                      NodeData.ColorFunc(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build())
                    } else {
                      NodeData.ColorFunc(ProtoColorFunction.newBuilder().setReplaceColor(ink.proto.Color.newBuilder().setAlpha(1f).build()).build())
                    }
                  )
                  expandedType = false
                }
              )
            }
          }
        }
        if (function.hasOpacityMultiplier()) {
          BrushSliderControl(
            label = "Opacity Multiplier",
            value = function.opacityMultiplier,
            valueRange = 0f..2f,
            onValueChange = { onUpdate(NodeData.ColorFunc(function.safeCopy(opacityMultiplier = it))) }
          )
        } else {
          val color = function.replaceColor
          val composeColor = Color(red = color.red, green = color.green, blue = color.blue, alpha = color.alpha)
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Color: ")
            OutlinedIconButton(
              onClick = {
                onChooseColor(composeColor) { newColor ->
                  onUpdate(
                    NodeData.ColorFunc(
                      function.safeCopy(
                        replaceColor = ink.proto.Color.newBuilder()
                          .setRed(newColor.red)
                          .setGreen(newColor.green)
                          .setBlue(newColor.blue)
                          .setAlpha(newColor.alpha)
                          .build()
                      )
                    )
                  )
                }
              },
              enabled = true,
            ) {
              androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp).padding(4.dp)) {
                drawCircle(color = composeColor)
              }
            }
            Text(
              text = String.format("RGBA (%.2f, %.2f, %.2f, %.2f)", color.red, color.green, color.blue, color.alpha),
              modifier = Modifier.padding(start = 8.dp),
            )
          }
        }
      }
      is NodeData.Family -> {
        OutlinedTextField(
          value = data.clientBrushFamilyId,
          onValueChange = { onUpdate(data.copy(clientBrushFamilyId = it)) },
          label = { Text("Client Brush Family ID") },
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          singleLine = true,
          enabled = !textFieldsLocked,
        )
        OutlinedTextField(
          value = data.developerComment,
          onValueChange = { onUpdate(data.copy(developerComment = it)) },
          label = { Text("Brush developer comment") },
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          minLines = 3,
          enabled = !textFieldsLocked,
        )
        var expandedModel by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expandedModel,
          onExpandedChange = { expandedModel = it }
        ) {
          OutlinedTextField(
            value = data.inputModel.toString().removeSuffix("_MODEL"),
            onValueChange = {},
            readOnly = true,
            label = { Text("Input Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedModel,
            onDismissRequest = { expandedModel = false }
          ) {
            // "LEGACY_SPRING" removed as it's not in Cahier's Ink version
            arrayOf("NAIVE", "SLIDING_WINDOW", "SPRING").forEach { model ->
              DropdownMenuItem(
                text = { Text(model) },
                onClick = {
                  val newModel =
                    when (model) {
                      "NAIVE" ->
                        ProtoBrushFamily.InputModel.newBuilder()
                          .setExperimentalNaiveModel(
                            ProtoBrushFamily.ExperimentalNaiveModel.getDefaultInstance()
                          )
                          .build()
                      "SLIDING_WINDOW" ->
                        ProtoBrushFamily.InputModel.newBuilder()
                          .setSlidingWindowModel(
                            ProtoBrushFamily.SlidingWindowModel.newBuilder()
                              .setWindowSizeSeconds(0.02f)
                              .setExperimentalUpsamplingPeriodSeconds(0.005f)
                          )
                          .build()
                      "SPRING" ->
                        ProtoBrushFamily.InputModel.newBuilder()
                          .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                          .build()
                      else ->
                        ProtoBrushFamily.InputModel.newBuilder()
                          .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                          .build()
                    }
                  onUpdate(data.copy(inputModel = newModel))
                  expandedModel = false
                }
              )
            }
          }
        }
      }
    }
  }
}

private val ALL_SOURCES =
  ProtoBrushBehavior.Source.values()
    .filter { it != ProtoBrushBehavior.Source.SOURCE_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_TARGETS =
  ProtoBrushBehavior.Target.values()
    .filter { it != ProtoBrushBehavior.Target.TARGET_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_POLAR_TARGETS =
  ProtoBrushBehavior.PolarTarget.values()
    .filter { it != ProtoBrushBehavior.PolarTarget.POLAR_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_BINARY_OPS =
  ProtoBrushBehavior.BinaryOp.values()
    .filter { it != ProtoBrushBehavior.BinaryOp.BINARY_OP_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_OUT_OF_RANGE =
  ProtoBrushBehavior.OutOfRange.values()
    .filter { it != ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_PROGRESS_DOMAINS =
  ProtoBrushBehavior.ProgressDomain.values()
    .filter {
      it != ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_UNSPECIFIED && it.ordinal >= 0
    }
    .toTypedArray()

private val ALL_INTERPOLATIONS =
  ProtoBrushBehavior.Interpolation.values()
    .filter { it != ProtoBrushBehavior.Interpolation.INTERPOLATION_UNSPECIFIED && it.ordinal >= 0 }
    .toTypedArray()

private val ALL_TOOL_TYPES =
  arrayOf(InputToolType.STYLUS, InputToolType.TOUCH, InputToolType.MOUSE, InputToolType.UNKNOWN)

private val NODE_TYPES =
  arrayOf(
    "Source",
    "Constant",
    "Noise",
    "ToolTypeFilter",
    "Damping",
    "Response",
    "Integral",
    "BinaryOp",
    "Interpolation",
    "Target",
    "PolarTarget",
    "Tip",
    "Coat",
    "Paint",
    "TextureLayer",
    "ColorFunction",
    "BrushFamily",
  )

internal fun createDefaultNode(typeName: String): NodeData {
  return when (typeName) {
    "Source" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setSourceNode(
            ProtoBrushBehavior.SourceNode.newBuilder()
              .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
              .setSourceValueRangeStart(0f)
              .setSourceValueRangeEnd(1f)
              .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      )
    "Constant" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setConstantNode(ProtoBrushBehavior.ConstantNode.newBuilder().setValue(0f))
          .build()
      )
    "Noise" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setNoiseNode(
            ProtoBrushBehavior.NoiseNode.newBuilder()
              .setSeed(0)
              .setVaryOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
              .setBasePeriod(1f)
          )
          .build()
      )
    "ToolTypeFilter" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setToolTypeFilterNode(
            ProtoBrushBehavior.ToolTypeFilterNode.newBuilder()
              .setEnabledToolTypes(1 shl 3) // Stylus
          )
          .build()
      )
    "Damping" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setDampingNode(
            ProtoBrushBehavior.DampingNode.newBuilder()
              .setDampingSource(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
              .setDampingGap(0.1f)
          )
          .build()
      )
    "Response" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setResponseNode(
            ProtoBrushBehavior.ResponseNode.newBuilder()
              .setPredefinedResponseCurve(ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
          )
          .build()
      )
    "Integral" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setIntegralNode(
            ProtoBrushBehavior.IntegralNode.newBuilder()
              .setIntegrateOver(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_CENTIMETERS)
              .setIntegralValueRangeStart(0f)
              .setIntegralValueRangeEnd(1f)
              .setIntegralOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      )
    "BinaryOp" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setBinaryOpNode(
            ProtoBrushBehavior.BinaryOpNode.newBuilder()
              .setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM)
          )
          .build()
      )
    "Interpolation" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setInterpolationNode(
            ProtoBrushBehavior.InterpolationNode.newBuilder()
              .setInterpolation(ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP)
          )
          .build()
      )
    "Target" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setTargetNode(
            ProtoBrushBehavior.TargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER)
              .setTargetModifierRangeStart(0f)
              .setTargetModifierRangeEnd(1f)
          )
          .build()
      )
    "PolarTarget" ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setPolarTargetNode(
            ProtoBrushBehavior.PolarTargetNode.newBuilder()
              .setTarget(ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE)
              .setAngleRangeStart(0f)
              .setAngleRangeEnd(6.28f)
              .setMagnitudeRangeStart(0f)
              .setMagnitudeRangeEnd(1f)
          )
          .build()
      )
    "Tip" -> NodeData.Tip(ProtoBrushTip.getDefaultInstance())
    "Coat" -> NodeData.Coat
    "Paint" -> NodeData.Paint(ProtoBrushPaint.getDefaultInstance())
    "TextureLayer" -> NodeData.TextureLayer(ProtoBrushPaint.TextureLayer.getDefaultInstance())
    "ColorFunction" -> NodeData.ColorFunc(ProtoColorFunction.getDefaultInstance())
    "BrushFamily" -> NodeData.Family()
    else ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setSourceNode(
            ProtoBrushBehavior.SourceNode.newBuilder()
              .setSource(ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
              .setSourceValueRangeStart(0f)
              .setSourceValueRangeEnd(1f)
              .setSourceOutOfRangeBehavior(ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP)
          )
          .build()
      )
  }
}

private fun prettyDisplayString(any: Any?): String =
  when (any) {
    is ProtoBrushBehavior.Source -> any.name
    is ProtoBrushBehavior.Target -> any.name
    is ProtoBrushBehavior.PolarTarget -> any.name
    is ProtoBrushBehavior.BinaryOp -> any.name
    is ProtoBrushBehavior.OutOfRange -> any.name
    is ProtoBrushBehavior.ProgressDomain -> any.name
    is ProtoBrushBehavior.Interpolation -> any.name
    is ProtoBrushPaint.SelfOverlap -> any.name
    is ProtoBrushPaint.TextureLayer.SizeUnit -> any.name
    is ProtoBrushPaint.TextureLayer.Origin -> any.name
    is ProtoBrushPaint.TextureLayer.Mapping -> any.name
    is ProtoBrushPaint.TextureLayer.Wrap -> any.name
    is ProtoBrushPaint.TextureLayer.BlendMode -> any.name
    is InputToolType -> any.toString()
    is ink.proto.StepPosition -> any.name
    is NodeData.ColorFunc -> "color function"
    is String ->
      any.replace("([a-z])([A-Z])".toRegex(), "$1 $2").lowercase().replaceFirstChar {
        it.uppercase()
      }
    else -> any.toString()
  }

private fun toolTypeBitIndex(toolType: InputToolType): Int =
  when (toolType) {
    InputToolType.UNKNOWN -> 0
    InputToolType.MOUSE -> 1
    InputToolType.TOUCH -> 2
    InputToolType.STYLUS -> 3
    else -> 0
  }
