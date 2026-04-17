@file:OptIn(
  androidx.ink.brush.ExperimentalInkCustomBrushApi::class,
  androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.cahier.ui.brushgraph.ui

import com.example.cahier.ui.brushgraph.model.NumericLimits
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.cahier.ui.brushdesigner.BrushSliderControl

import com.example.cahier.ui.brushgraph.model.*
import com.example.cahier.ui.brushgraph.ui.TipPreviewWidget

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
  onFieldEditComplete: () -> Unit = {},
  onDropdownEditComplete: () -> Unit = {},
) {
  Column(
    modifier =
      Modifier.padding(top = 8.dp).heightIn(max = 600.dp).verticalScroll(rememberScrollState())
  ) {
    if (node.data is NodeData.Behavior) {
      val nodeDataTypeName = node.data.node.nodeCase.name.removeSuffix("_NODE")
      var expandedNodeTypes by remember { mutableStateOf(false) }
      var showNodeTooltip by remember { mutableStateOf(false) }
      
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        ExposedDropdownMenuBox(
          expanded = expandedNodeTypes,
          onExpandedChange = { expandedNodeTypes = it },
          modifier = Modifier.weight(1f)
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
            @Composable
            fun DropdownSection(label: String, types: List<String>) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
              ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.outline,
                  modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
              }
              types.forEach { type ->
                DropdownMenuItem(
                  text = { Text(prettyDisplayString(type)) },
                  onClick = {
                    if (type != nodeDataTypeName) {
                      onUpdate(createDefaultNode(type))
                      onDropdownEditComplete()
                    }
                    expandedNodeTypes = false
                  }
                )
              }
            }

            DropdownSection("Start nodes:", NODE_TYPES_START)
            DropdownSection("Operator nodes:", NODE_TYPES_OPERATOR)
            DropdownSection("Terminal nodes:", NODE_TYPES_TERMINAL)
          }
        }
      }
    }

    when (val data = node.data) {
      is NodeData.Behavior -> {
        val behaviorNode = data.node
        if (behaviorNode.nodeCase == ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ||
            behaviorNode.nodeCase == ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE) {
          OutlinedTextField(
            value = data.developerComment,
            onValueChange = {
              onUpdate(data.copy(developerComment = it))
            },
            label = { Text("Developer comment") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            minLines = 2,
            enabled = !textFieldsLocked,
          )
        }
        when (behaviorNode.nodeCase) {
          ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE -> {
            val sourceNode = behaviorNode.sourceNode
            val limits = sourceNode.source.getNumericLimits()
            var expandedSource by remember { mutableStateOf(false) }
            var showSourceTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedSource,
                onExpandedChange = { expandedSource = it },
                modifier = Modifier.weight(1f)
              ) {
                OutlinedTextField(
                  value = prettyDisplayString(sourceNode.source),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text("Source") },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedSource,
                  onDismissRequest = { expandedSource = false }
                ) {
                  @Composable
                  fun SourceSection(label: String, sources: List<ProtoBrushBehavior.Source>) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                      HorizontalDivider(modifier = Modifier.weight(1f))
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                      )
                      HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    sources.forEach { source ->
                      DropdownMenuItem(
                        text = { Text(prettyDisplayString(source)) },
                        onClick = {
                          val currentDisplayStart = if (sourceNode.source.isAngle()) Math.toDegrees(sourceNode.sourceValueRangeStart.toDouble()).toFloat() else sourceNode.sourceValueRangeStart
                          val currentDisplayEnd = if (sourceNode.source.isAngle()) Math.toDegrees(sourceNode.sourceValueRangeEnd.toDouble()).toFloat() else sourceNode.sourceValueRangeEnd

                          val newLimits = source.getNumericLimits()
                          val clampedDisplayStart = currentDisplayStart.coerceIn(newLimits.min, newLimits.max)
                          val clampedDisplayEnd = currentDisplayEnd.coerceIn(newLimits.min, newLimits.max)

                          val newProtoStart = if (source.isAngle()) Math.toRadians(clampedDisplayStart.toDouble()).toFloat() else clampedDisplayStart
                          val newProtoEnd = if (source.isAngle()) Math.toRadians(clampedDisplayEnd.toDouble()).toFloat() else clampedDisplayEnd

                          val needsClamp = source == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS ||
                                          source == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS
                          val newOor = if (needsClamp) ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP else sourceNode.sourceOutOfRangeBehavior
                          
                          onUpdate(
                            NodeData.Behavior(
                              behaviorNode.safeCopy(
                                sourceNode = sourceNode.safeCopy(
                                  source = source, 
                                  sourceOutOfRangeBehavior = newOor,
                                  sourceValueRangeStart = newProtoStart,
                                  sourceValueRangeEnd = newProtoEnd
                                )
                              )
                            )
                          )
                          onDropdownEditComplete()
                          expandedSource = false
                        }
                      )
                    }
                  }

                  SourceSection("Input:", SOURCES_INPUT)
                  SourceSection("Movement:", SOURCES_MOVEMENT)
                  SourceSection("Distance:", SOURCES_DISTANCE)
                  SourceSection("Time:", SOURCES_TIME)
                  SourceSection("Acceleration:", SOURCES_ACCELERATION)
                }
              }
              IconButton(onClick = { showSourceTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            
            if (showSourceTooltip) {
              TooltipDialog(
                title = "Source: " + prettyDisplayString(sourceNode.source),
                text = sourceNode.source.getTooltip(),
                onDismiss = { showSourceTooltip = false }
              )
            }
            val isAngleSource = sourceNode.source == ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS ||
                                sourceNode.source == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS

            val displayValueStart = if (isAngleSource) Math.toDegrees(sourceNode.sourceValueRangeStart.toDouble()).toFloat() else sourceNode.sourceValueRangeStart
            val displayValueEnd = if (isAngleSource) Math.toDegrees(sourceNode.sourceValueRangeEnd.toDouble()).toFloat() else sourceNode.sourceValueRangeEnd

            BrushSliderControl(
              label = "Range Start",
              value = displayValueStart,
              limits = limits,
              onValueChange = {
                val newValue = if (isAngleSource) Math.toRadians(it.toDouble()).toFloat() else it
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceValueRangeStart = newValue))))
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = "Range End",
              value = displayValueEnd,
              limits = limits,
              onValueChange = {
                val newValue = if (isAngleSource) Math.toRadians(it.toDouble()).toFloat() else it
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceValueRangeEnd = newValue))))
              },
              onValueChangeFinished = onFieldEditComplete
            )
            val isTimeSinceSource = sourceNode.source == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS ||
                                    sourceNode.source == ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS
            var expandedOor by remember { mutableStateOf(false) }
            var showOorTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedOor,
                onExpandedChange = { expandedOor = it },
                modifier = Modifier.weight(1f)
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
                    val isEnabled = !isTimeSinceSource || oor == ProtoBrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP
                    DropdownMenuItem(
                      text = { Text(prettyDisplayString(oor)) },
                      onClick = {
                        onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceOutOfRangeBehavior = oor))))
                        onDropdownEditComplete()
                        expandedOor = false
                      },
                      enabled = isEnabled
                    )
                  }
                }
              }
              IconButton(onClick = { showOorTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            
            if (showOorTooltip) {
              TooltipDialog(
                title = "Out of Range Behavior: ${prettyDisplayString(sourceNode.sourceOutOfRangeBehavior)}",
                text = sourceNode.sourceOutOfRangeBehavior.getTooltip(),
                onDismiss = { showOorTooltip = false }
              )
            }
            if (isTimeSinceSource) {
              Text(
                text = "This source is only compatible with 'clamp' behavior.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
          ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> {
            val constantNode = behaviorNode.constantNode
            BrushSliderControl(
              label = "Value",
              value = constantNode.value,
              limits = NumericLimits(-100f, 100f, 0.01f),
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(behaviorNode.safeCopy(constantNode = constantNode.safeCopy(value = it)))
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
          }
          ProtoBrushBehavior.Node.NodeCase.NOISE_NODE -> {
            val noiseNode = behaviorNode.noiseNode
            val limits = noiseNode.varyOver.getNumericLimits(ProgressDomainContext.NOISE)
            BrushSliderControl(
              label = "Seed",
              value = noiseNode.seed.toFloat(),
              valueRange = 0f..100f,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(seed = it.toInt()))
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            var expandedVary by remember { mutableStateOf(false) }
            var showVaryTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedVary,
                onExpandedChange = { expandedVary = it },
                modifier = Modifier.weight(1f)
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
                        val newLimits = domain.getNumericLimits(ProgressDomainContext.NOISE)
                        val clampedBasePeriod = noiseNode.basePeriod.coerceIn(newLimits.min, newLimits.max)

                        onUpdate(
                          NodeData.Behavior(
                            behaviorNode.safeCopy(
                              noiseNode = noiseNode.safeCopy(
                                varyOver = domain,
                                basePeriod = clampedBasePeriod
                              )
                            )
                          )
                        )
                        expandedVary = false
                      }
                    )
                  }
                }
              }
              IconButton(onClick = { showVaryTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            
            if (showVaryTooltip) {
              TooltipDialog(
                title = "Vary Over: ${prettyDisplayString(noiseNode.varyOver)}",
                text = noiseNode.varyOver.getTooltip(),
                onDismiss = { showVaryTooltip = false }
              )
            }
            BrushSliderControl(
              label = "Base Period",
              value = noiseNode.basePeriod,
              valueRange = limits.min..limits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(noiseNode = noiseNode.safeCopy(basePeriod = it))
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
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
            val limits = dampingNode.dampingSource.getNumericLimits(ProgressDomainContext.DAMPING)
            var expandedSource by remember { mutableStateOf(false) }
            var showDampingTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedSource,
                onExpandedChange = { expandedSource = it },
                modifier = Modifier.weight(1f)
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
                        val newLimits = domain.getNumericLimits(ProgressDomainContext.DAMPING)
                        val clampedGap = dampingNode.dampingGap.coerceIn(newLimits.min, newLimits.max)

                        onUpdate(
                          NodeData.Behavior(
                            behaviorNode.safeCopy(
                              dampingNode = dampingNode.safeCopy(
                                dampingSource = domain,
                                dampingGap = clampedGap
                              )
                            )
                          )
                        )
                        onDropdownEditComplete()
                        expandedSource = false
                      }
                    )
                  }
                }
              }
              IconButton(onClick = { showDampingTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            
            if (showDampingTooltip) {
              TooltipDialog(
                title = "Damping Source: ${prettyDisplayString(dampingNode.dampingSource)}",
                text = dampingNode.dampingSource.getTooltip(),
                onDismiss = { showDampingTooltip = false }
              )
            }
            BrushSliderControl(
              label = "Damping Gap",
              value = dampingNode.dampingGap,
              valueRange = limits.min..limits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(dampingNode = dampingNode.safeCopy(dampingGap = it))
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
          }
          ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE -> {
            val responseNode = behaviorNode.responseNode
            ResponseCurveEditor(
              responseNode = responseNode,
              onResponseNodeChanged = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(responseNode = it)
                  )
                )
              }
            )
          }
          ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE -> {
            val integralNode = behaviorNode.integralNode
            val limits = integralNode.integrateOver.getNumericLimits(ProgressDomainContext.INTEGRAL)
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
                      val newLimits = domain.getNumericLimits(ProgressDomainContext.INTEGRAL)
                      val clampedStart = integralNode.integralValueRangeStart.coerceIn(newLimits.min, newLimits.max)
                      val clampedEnd = integralNode.integralValueRangeEnd.coerceIn(newLimits.min, newLimits.max)

                      onUpdate(
                        NodeData.Behavior(
                          behaviorNode.safeCopy(
                            integralNode = integralNode.safeCopy(
                              integrateOver = domain,
                              integralValueRangeStart = clampedStart,
                              integralValueRangeEnd = clampedEnd
                            )
                          )
                        )
                      )
                      onDropdownEditComplete()
                      expandedOver = false
                    }
                  )
                }
              }
            }
            BrushSliderControl(
              label = "Range Start",
              value = integralNode.integralValueRangeStart,
              valueRange = limits.min..limits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      integralNode = integralNode.safeCopy(integralValueRangeStart = it)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = "Range End",
              value = integralNode.integralValueRangeEnd,
              valueRange = limits.min..limits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      integralNode = integralNode.safeCopy(integralValueRangeEnd = it)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
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
            var showOpTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedOp,
                onExpandedChange = { expandedOp = it },
                modifier = Modifier.weight(1f)
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
                        onDropdownEditComplete()
                        expandedOp = false
                      }
                    )
                  }
                }
              }
              IconButton(onClick = { showOpTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            if (showOpTooltip) {
              TooltipDialog(
                title = "Operation: ${prettyDisplayString(binaryNode.operation)}",
                text = binaryNode.operation.getTooltip(),
                onDismiss = { showOpTooltip = false }
              )
            }
          }
          ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE -> {
            val interpNode = behaviorNode.interpolationNode
            var expandedInterp by remember { mutableStateOf(false) }
            var showInterpTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedInterp,
                onExpandedChange = { expandedInterp = it },
                modifier = Modifier.weight(1f)
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
              IconButton(onClick = { showInterpTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            if (showInterpTooltip) {
              TooltipDialog(
                title = "Interpolation: ${prettyDisplayString(interpNode.interpolation)}",
                text = interpNode.interpolation.getTooltip(),
                onDismiss = { showInterpTooltip = false }
              )
            }
          }
          ProtoBrushBehavior.Node.NodeCase.TARGET_NODE -> {
            val targetNode = behaviorNode.targetNode
            val limits = targetNode.target.getNumericLimits()
            var expandedTarget by remember { mutableStateOf(false) }
            var showTargetTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedTarget,
                onExpandedChange = { expandedTarget = it },
                modifier = Modifier.weight(1f)
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
                  @Composable
                  fun TargetSection(label: String, targets: List<ProtoBrushBehavior.Target>) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                      HorizontalDivider(modifier = Modifier.weight(1f))
                      Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                      )
                      HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    targets.forEach { target ->
                      DropdownMenuItem(
                        text = { Text(prettyDisplayString(target)) },
                        onClick = {
                          val currentDisplayStart = if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble()).toFloat() else targetNode.targetModifierRangeStart
                          val currentDisplayEnd = if (targetNode.target.isAngle()) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble()).toFloat() else targetNode.targetModifierRangeEnd

                          val newLimits = target.getNumericLimits()
                          val clampedDisplayStart = currentDisplayStart.coerceIn(newLimits.min, newLimits.max)
                          val clampedDisplayEnd = currentDisplayEnd.coerceIn(newLimits.min, newLimits.max)

                          val newProtoStart = if (target.isAngle()) Math.toRadians(clampedDisplayStart.toDouble()).toFloat() else clampedDisplayStart
                          val newProtoEnd = if (target.isAngle()) Math.toRadians(clampedDisplayEnd.toDouble()).toFloat() else clampedDisplayEnd

                          onUpdate(
                            NodeData.Behavior(
                              behaviorNode.safeCopy(
                                targetNode = targetNode.safeCopy(
                                  target = target,
                                  targetModifierRangeStart = newProtoStart,
                                  targetModifierRangeEnd = newProtoEnd
                                )
                              )
                            )
                          )
                          onDropdownEditComplete()
                          expandedTarget = false
                        }
                      )
                    }
                  }

                  TargetSection("Size & Shape:", TARGETS_SIZE_SHAPE)
                  TargetSection("Position:", TARGETS_POSITION)
                  TargetSection("Color & Opacity:", TARGETS_COLOR_OPACITY)
                }
              }
              IconButton(onClick = { showTargetTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            if (showTargetTooltip) {
              TooltipDialog(
                title = "Target: ${prettyDisplayString(targetNode.target)}",
                text = targetNode.target.getTooltip(),
                onDismiss = { showTargetTooltip = false }
              )
            }
            val isAngleTarget = targetNode.target == ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ||
                                targetNode.target == ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ||
                                targetNode.target == ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS

            val displayValueStart = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble()).toFloat() else targetNode.targetModifierRangeStart
            val displayValueEnd = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble()).toFloat() else targetNode.targetModifierRangeEnd

            BrushSliderControl(
              label = "Range Start",
              value = displayValueStart,
              valueRange = limits.min..limits.max,
              unit = limits.displayUnit,
              onValueChange = {
                val newValue = if (isAngleTarget) Math.toRadians(it.toDouble()).toFloat() else it
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      targetNode = targetNode.safeCopy(targetModifierRangeStart = newValue)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = "Range End",
              value = displayValueEnd,
              valueRange = limits.min..limits.max,
              unit = limits.displayUnit,
              onValueChange = {
                val newValue = if (isAngleTarget) Math.toRadians(it.toDouble()).toFloat() else it
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      targetNode = targetNode.safeCopy(targetModifierRangeEnd = newValue)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
          }

          ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE -> {
            val polarNode = behaviorNode.polarTargetNode
            var expandedPolar by remember { mutableStateOf(false) }
            var showPolarTooltip by remember { mutableStateOf(false) }
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              ExposedDropdownMenuBox(
                expanded = expandedPolar,
                onExpandedChange = { expandedPolar = it },
                modifier = Modifier.weight(1f)
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
                        val newMagLimits = if (target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ||
                                            target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE) {
                            NumericLimits(-10.0f, 10.0f, 0.01f)
                        } else {
                            NumericLimits(0.0f, 1.0f, 0.1f)
                        }
                        val clampedMagStart = polarNode.magnitudeRangeStart.coerceIn(newMagLimits.min, newMagLimits.max)
                        val clampedMagEnd = polarNode.magnitudeRangeEnd.coerceIn(newMagLimits.min, newMagLimits.max)

                        onUpdate(
                          NodeData.Behavior(
                            behaviorNode.safeCopy(
                              polarTargetNode = polarNode.safeCopy(
                                target = target,
                                magnitudeRangeStart = clampedMagStart,
                                magnitudeRangeEnd = clampedMagEnd
                              )
                            )
                          )
                        )
                        onDropdownEditComplete()
                        expandedPolar = false
                      }
                    )
                  }
                }
              }
              IconButton(onClick = { showPolarTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
              }
            }
            if (showPolarTooltip) {
              TooltipDialog(
                title = "Polar Target: ${prettyDisplayString(polarNode.target)}",
                text = polarNode.target.getTooltip(),
                onDismiss = { showPolarTooltip = false }
              )
            }
            // Angle (All targets): -360° to 360° (mapped to radians 0 to 2pi for now, 
            // but the user specified -360 to 360 degrees)
            val displayAngleStart = Math.toDegrees(polarNode.angleRangeStart.toDouble()).toFloat()
            val displayAngleEnd = Math.toDegrees(polarNode.angleRangeEnd.toDouble()).toFloat()

            BrushSliderControl(
              label = "Angle Start",
              value = displayAngleStart,
              valueRange = -360f..360f,
              unit = "°",
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeStart = Math.toRadians(it.toDouble()).toFloat()))
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = "Angle End",
              value = displayAngleEnd,
              valueRange = -360f..360f,
              unit = "°",
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(polarTargetNode = polarNode.safeCopy(angleRangeEnd = Math.toRadians(it.toDouble()).toFloat()))
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            val magLimits = if (polarNode.target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE ||
                                polarNode.target == ProtoBrushBehavior.PolarTarget.POLAR_POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE) {
                NumericLimits(-10.0f, 10.0f, 0.01f)
            } else {
                NumericLimits(0.0f, 1.0f, 0.1f)
            }
            BrushSliderControl(
              label = "Mag Start",
              value = polarNode.magnitudeRangeStart,
              valueRange = magLimits.min..magLimits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      polarTargetNode = polarNode.safeCopy(magnitudeRangeStart = it)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = "Mag End",
              value = polarNode.magnitudeRangeEnd,
              valueRange = magLimits.min..magLimits.max,
              onValueChange = {
                onUpdate(
                  NodeData.Behavior(
                    behaviorNode.safeCopy(
                      polarTargetNode = polarNode.safeCopy(magnitudeRangeEnd = it)
                    )
                  )
                )
              },
              onValueChangeFinished = onFieldEditComplete
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
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleX = it))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Scale Y",
          value = tip.scaleY,
          valueRange = 0f..2f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleY = it))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Corner Rounding",
          value = tip.cornerRounding,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(cornerRounding = it))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Slant Degrees",
          value = Math.toDegrees(tip.slantRadians.toDouble()).toFloat(),
          valueRange = -90f..90f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(slantRadians = Math.toRadians(it.toDouble()).toFloat()))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Pinch",
          value = tip.pinch,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(pinch = it))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Rotation Degrees",
          value = Math.toDegrees(tip.rotationRadians.toDouble()).toFloat(),
          valueRange = 0f..360f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(rotationRadians = Math.toRadians(it.toDouble()).toFloat()))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Particle Gap Distance Scale",
          value = tip.particleGapDistanceScale,
          valueRange = 0f..5f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDistanceScale = it))) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = "Particle Gap Duration (ms)",
          value = tip.particleGapDurationSeconds * 1000f,
          valueRange = 0f..1000f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDurationSeconds = it / 1000f))) },
          onValueChangeFinished = onFieldEditComplete
        )
      }
      is NodeData.Coat -> {
        Text("Coat Node: Connect Tip and Paint to this node.", fontSize = 12.sp)
      }
      is NodeData.Paint -> {
        val paint = data.paint
        var expanded by remember { mutableStateOf(false) }
        var showTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
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
                    onDropdownEditComplete()
                    expanded = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showTooltip) {
          TooltipDialog(
            title = "Self Overlap: ${prettyDisplayString(paint.selfOverlap)}",
            text = paint.selfOverlap.getTooltip(),
            onDismiss = { showTooltip = false }
          )
        }
      }
      is NodeData.TextureLayer -> {
        TextureLayerInspector(
          layer = data.layer,
          allTextureIds = allTextureIds,
          onLoadTexture = onLoadTexture,
          onUpdate = { onUpdate(it) },
          strokeRenderer = strokeRenderer
        )
      }
      is NodeData.ColorFunc -> {
        val function = data.function
        var expandedType by remember { mutableStateOf(false) }
        val currentType = if (function.hasOpacityMultiplier()) {
          "Opacity Multiplier"
        } else {
          "Replace Color"
        }

        var showTypeTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          ExposedDropdownMenuBox(
            expanded = expandedType,
            onExpandedChange = { expandedType = it },
            modifier = Modifier.weight(1f)
          ) {
            OutlinedTextField(
              value = currentType,
              onValueChange = {},
              readOnly = true,
              label = { Text("Function Type") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedType,
              onDismissRequest = { expandedType = false }
            ) {
              listOf("Opacity Multiplier", "Replace Color").forEach { type ->
                DropdownMenuItem(
                  text = { Text(type) },
                  onClick = {
                    if (type != currentType) {
                      onUpdate(
                        if (type == "Opacity Multiplier") {
                          NodeData.ColorFunc(
                            ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()
                          )
                        } else {
                          NodeData.ColorFunc(
                            ProtoColorFunction.newBuilder()
                              .setReplaceColor(
                                ink.proto.Color.newBuilder()
                                  .setRed(0f)
                                  .setGreen(0f)
                                  .setBlue(0f)
                                  .setAlpha(1f)
                                  .build()
                              )
                              .build()
                          )
                        }
                      )
                    }
                    onDropdownEditComplete()
                    expandedType = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showTypeTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showTypeTooltip) {
          TooltipDialog(
            title = "Function Type: $currentType",
            text = getColorFunctionTooltip(currentType),
            onDismiss = { showTypeTooltip = false }
          )
        }
        if (function.hasOpacityMultiplier()) {
          BrushSliderControl(
            label = "Opacity Multiplier",
            value = function.opacityMultiplier,
            valueRange = 0f..2f,
            onValueChange = { onUpdate(NodeData.ColorFunc(function.safeCopy(opacityMultiplier = it))) },
            onValueChangeFinished = onFieldEditComplete
          )
        } else if (function.hasReplaceColor()) {
          val color = function.replaceColor
          val composeColor =
            Color(red = color.red, green = color.green, blue = color.blue, alpha = color.alpha)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            Text("Color: ", style = MaterialTheme.typography.bodyMedium)
            Surface(
              onClick = {
                onChooseColor(composeColor) { newColor ->
                  onUpdate(
                    NodeData.ColorFunc(
                      function.safeCopy(
                        replaceColor =
                          ink.proto.Color.newBuilder()
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
              shape = RoundedCornerShape(4.dp),
              color = composeColor,
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.size(40.dp)
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(
              text = String.format("ARGB #%08X", (composeColor.toArgb())),
              style = MaterialTheme.typography.bodySmall,
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
        var showModelTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          ExposedDropdownMenuBox(
            expanded = expandedModel,
            onExpandedChange = { expandedModel = it },
            modifier = Modifier.weight(1f)
          ) {
            OutlinedTextField(
              value = data.inputModel.displayString(),
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
              arrayOf("Sliding Window Model", "Spring Model", "Naive Experimental Model").forEach { model ->
                DropdownMenuItem(
                  text = { Text(model) },
                  onClick = {
                    val newModel =
                      when (model) {
                        "Naive Experimental Model" ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setExperimentalNaiveModel(
                              ProtoBrushFamily.ExperimentalNaiveModel.getDefaultInstance()
                            )
                            .build()
                        "Sliding Window Model" ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setSlidingWindowModel(
                              ProtoBrushFamily.SlidingWindowModel.newBuilder()
                                .setWindowSizeSeconds(0.02f)
                                .setExperimentalUpsamplingPeriodSeconds(0.005f)
                            )
                            .build()
                        "Spring Model" ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                            .build()
                        else ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                            .build()
                      }
                    onUpdate(data.copy(inputModel = newModel))
                    onDropdownEditComplete()
                    expandedModel = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showModelTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showModelTooltip) {
          TooltipDialog(
            title = "Input Model: ${data.inputModel.displayString()}",
            text = getInputModelTooltip(data.inputModel.displayString()),
            onDismiss = { showModelTooltip = false }
          )
        }

        if (data.inputModel.hasSlidingWindowModel()) {
          val slidingModel = data.inputModel.slidingWindowModel
          val windowSizeMs = slidingModel.windowSizeSeconds * 1000f
          val upsamplingHz = if (slidingModel.experimentalUpsamplingPeriodSeconds > 0) 1f / slidingModel.experimentalUpsamplingPeriodSeconds else 0f

          BrushSliderControl(
            label = "Window Size (ms)",
            value = windowSizeMs,
            valueRange = 1f..100f,
            onValueChange = { newMs ->
              val newSeconds = newMs / 1000f
              val newModel = data.inputModel.toBuilder()
                .setSlidingWindowModel(
                  slidingModel.toBuilder().setWindowSizeSeconds(newSeconds)
                )
                .build()
              onUpdate(data.copy(inputModel = newModel))
            },
            onValueChangeFinished = onFieldEditComplete
          )

          BrushSliderControl(
            label = "Upsampling Frequency (Hz)",
            value = upsamplingHz,
            valueRange = 0f..500f,
            onValueChange = { newHz ->
              val newPeriod = if (newHz > 0) 1f / newHz else 0f
              val newModel = data.inputModel.toBuilder()
                .setSlidingWindowModel(
                  slidingModel.toBuilder().setExperimentalUpsamplingPeriodSeconds(newPeriod)
                )
                .build()
              onUpdate(data.copy(inputModel = newModel))
            },
            onValueChangeFinished = onFieldEditComplete
          )
        }
      }
    }
  }
}

private fun ProtoBrushFamily.InputModel.displayString(): String {
  return when {
    hasSlidingWindowModel() -> "Sliding Window Model"
    hasSpringModel() -> "Spring Model"
    hasExperimentalNaiveModel() -> "Naive Experimental Model"
    else -> "Unknown Model"
  }
}

internal val SOURCES_INPUT = listOf(
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE,
  ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS
)

internal val SOURCES_MOVEMENT = listOf(
  ProtoBrushBehavior.Source.SOURCE_SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_SPEED_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_X_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_INPUT_VELOCITY_Y_IN_CENTIMETERS_PER_SECOND,
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS,
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_X,
  ProtoBrushBehavior.Source.SOURCE_NORMALIZED_DIRECTION_Y
)

internal val SOURCES_DISTANCE = listOf(
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_INPUT_DISTANCE_TRAVELED_IN_CENTIMETERS,
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Source.SOURCE_DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH
)

internal val SOURCES_TIME = listOf(
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_TIME_OF_INPUT_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_PREDICTED_TIME_ELAPSED_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_SECONDS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_INPUT_IN_MILLIS,
  ProtoBrushBehavior.Source.SOURCE_TIME_SINCE_STROKE_END_IN_SECONDS
)

internal val SOURCES_ACCELERATION = listOf(
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED,
  ProtoBrushBehavior.Source.SOURCE_INPUT_ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED
)

internal val TARGETS_SIZE_SHAPE = listOf(
  ProtoBrushBehavior.Target.TARGET_WIDTH_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_HEIGHT_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_SIZE_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_PINCH_OFFSET,
  ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_CORNER_ROUNDING_OFFSET
)

internal val TARGETS_POSITION = listOf(
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE,
  ProtoBrushBehavior.Target.TARGET_POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE
)

internal val TARGETS_COLOR_OPACITY = listOf(
  ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS,
  ProtoBrushBehavior.Target.TARGET_SATURATION_MULTIPLIER,
  ProtoBrushBehavior.Target.TARGET_LUMINOSITY,
  ProtoBrushBehavior.Target.TARGET_OPACITY_MULTIPLIER
)

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

internal val NODE_TYPES_START = listOf("Source", "Constant", "Noise")
internal val NODE_TYPES_OPERATOR = listOf("ToolTypeFilter", "Damping", "Response", "Integral", "BinaryOp", "Interpolation")
internal val NODE_TYPES_TERMINAL = listOf("Target", "PolarTarget")

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
    "ColorFunction" ->
      NodeData.ColorFunc(ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build())
    "BrushFamily" -> NodeData.Family()
    else ->
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
  }
}

internal fun prettyDisplayString(any: Any?): String =
  when (any) {
    is ProtoBrushBehavior.Source -> any.displayString()
    is ProtoBrushBehavior.Target -> any.displayString()
    is ProtoBrushBehavior.PolarTarget -> any.displayString()
    is ProtoBrushBehavior.BinaryOp -> any.displayString()
    is ProtoBrushBehavior.OutOfRange -> any.displayString()
    is ProtoBrushBehavior.ProgressDomain -> any.displayString()
    is ProtoBrushBehavior.Interpolation -> any.displayString()
    is ProtoBrushPaint.SelfOverlap -> any.displayString()
    is ProtoBrushPaint.TextureLayer.SizeUnit -> any.displayString()
    is ProtoBrushPaint.TextureLayer.Origin -> any.displayString()
    is ProtoBrushPaint.TextureLayer.Mapping -> any.displayString()
    is ProtoBrushPaint.TextureLayer.Wrap -> any.displayString()
    is ProtoBrushPaint.TextureLayer.BlendMode -> any.displayString()
    is InputToolType -> any.displayString()
    is ink.proto.StepPosition -> any.displayString()
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

internal fun ProtoBrushBehavior.Source.isAngle(): Boolean {
  return this == ProtoBrushBehavior.Source.SOURCE_TILT_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_TILT_X_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_TILT_Y_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_DIRECTION_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_DIRECTION_ABOUT_ZERO_IN_RADIANS ||
         this == ProtoBrushBehavior.Source.SOURCE_ORIENTATION_ABOUT_ZERO_IN_RADIANS
}

internal fun ProtoBrushBehavior.Target.isAngle(): Boolean {
  return this == ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ||
         this == ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ||
         this == ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS
}

@Composable
internal fun TooltipDialog(
  title: String,
  text: String,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Text(text) },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("OK")
      }
    }
  )
}
