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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cahier.R
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
      val nodeCase = node.data.node.nodeCase
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
            value = stringResource(nodeCase.displayStringRId()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.bg_node_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNodeTypes) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedNodeTypes,
            onDismissRequest = { expandedNodeTypes = false }
          ) {
            @Composable
            fun DropdownSection(label: String, types: List<ProtoBrushBehavior.Node.NodeCase>) {
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
                  text = { Text(stringResource(type.displayStringRId())) },
                  onClick = {
                    if (type != nodeCase) {
                      onUpdate(createDefaultNode(type))
                      onDropdownEditComplete()
                    }
                    expandedNodeTypes = false
                  }
                )
              }
            }

            DropdownSection(stringResource(R.string.bg_section_start_nodes), NODE_TYPES_START)
            DropdownSection(stringResource(R.string.bg_section_operator_nodes), NODE_TYPES_OPERATOR)
            DropdownSection(stringResource(R.string.bg_section_terminal_nodes), NODE_TYPES_TERMINAL)
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
            label = { Text(stringResource(R.string.bg_developer_comment)) },
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
                  value = stringResource(sourceNode.source.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_source)) },
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
                        text = { Text(stringResource(source.displayStringRId())) },
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

                  SourceSection(stringResource(R.string.bg_section_input), SOURCES_INPUT)
                  SourceSection(stringResource(R.string.bg_section_movement), SOURCES_MOVEMENT)
                  SourceSection(stringResource(R.string.bg_section_distance), SOURCES_DISTANCE)
                  SourceSection(stringResource(R.string.bg_section_time), SOURCES_TIME)
                  SourceSection(stringResource(R.string.bg_section_acceleration), SOURCES_ACCELERATION)
                }
              }
              IconButton(onClick = { showSourceTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            
            if (showSourceTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_source_format, stringResource(sourceNode.source.displayStringRId())),
                text = stringResource(sourceNode.source.getTooltip()),
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
              label = stringResource(R.string.bg_label_range_start),
              value = displayValueStart,
              limits = limits,
              onValueChange = {
                val newValue = if (isAngleSource) Math.toRadians(it.toDouble()).toFloat() else it
                onUpdate(NodeData.Behavior(behaviorNode.safeCopy(sourceNode = sourceNode.safeCopy(sourceValueRangeStart = newValue))))
              },
              onValueChangeFinished = onFieldEditComplete
            )
            BrushSliderControl(
              label = stringResource(R.string.bg_label_range_end),
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
                  value = stringResource(sourceNode.sourceOutOfRangeBehavior.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_out_of_range_behavior)) },
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
                      text = { Text(stringResource(oor.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            
            if (showOorTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_out_of_range_behavior_format, stringResource(sourceNode.sourceOutOfRangeBehavior.displayStringRId())),
                text = stringResource(sourceNode.sourceOutOfRangeBehavior.getTooltip()),
                onDismiss = { showOorTooltip = false }
              )
            }
            if (isTimeSinceSource) {
              Text(
                text = stringResource(R.string.bg_msg_source_clamp_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }
          ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE -> {
            val constantNode = behaviorNode.constantNode
            BrushSliderControl(
              label = stringResource(R.string.bg_port_value),
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
              label = stringResource(R.string.bg_label_seed),
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
                  value = stringResource(noiseNode.varyOver.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_vary_over)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVary) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedVary,
                  onDismissRequest = { expandedVary = false }
                ) {
                  ALL_PROGRESS_DOMAINS.forEach { domain ->
                    DropdownMenuItem(
                      text = { Text(stringResource(domain.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            
            if (showVaryTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_vary_over_format, stringResource(noiseNode.varyOver.displayStringRId())),
                text = stringResource(noiseNode.varyOver.getTooltip()),
                onDismiss = { showVaryTooltip = false }
              )
            }
            BrushSliderControl(
              label = stringResource(R.string.bg_label_base_period),
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
            Text(stringResource(R.string.bg_enabled_tool_types), style = MaterialTheme.typography.bodySmall)
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
                Text(stringResource(toolType.displayStringRId()))
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
                  value = stringResource(dampingNode.dampingSource.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_damping_source)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSource) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedSource,
                  onDismissRequest = { expandedSource = false }
                ) {
                  ALL_PROGRESS_DOMAINS.forEach { domain ->
                    DropdownMenuItem(
                      text = { Text(stringResource(domain.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            
            if (showDampingTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_damping_source_format, stringResource(dampingNode.dampingSource.displayStringRId())),
                text = stringResource(dampingNode.dampingSource.getTooltip()),
                onDismiss = { showDampingTooltip = false }
              )
            }
            BrushSliderControl(
              label = stringResource(R.string.bg_label_damping_gap),
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
                value = stringResource(integralNode.integrateOver.displayStringRId()),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.bg_integrate_over)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOver) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
              )
              ExposedDropdownMenu(
                expanded = expandedOver,
                onDismissRequest = { expandedOver = false }
              ) {
                ALL_PROGRESS_DOMAINS.forEach { domain ->
                  DropdownMenuItem(
                    text = { Text(stringResource(domain.displayStringRId())) },
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
              label = stringResource(R.string.bg_label_range_start),
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
              label = stringResource(R.string.bg_label_range_end),
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
                value = stringResource(integralNode.integralOutOfRangeBehavior.displayStringRId()),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.bg_out_of_range_behavior)) },
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
                    text = { Text(stringResource(oor.displayStringRId())) },
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
                  value = stringResource(binaryNode.operation.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_operation)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOp) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedOp,
                  onDismissRequest = { expandedOp = false }
                ) {
                  ALL_BINARY_OPS.forEach { op ->
                    DropdownMenuItem(
                      text = { Text(stringResource(op.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            if (showOpTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_operation_format, stringResource(binaryNode.operation.displayStringRId())),
                text = stringResource(binaryNode.operation.getTooltip()),
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
                  value = stringResource(interpNode.interpolation.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_interpolation)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterp) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedInterp,
                  onDismissRequest = { expandedInterp = false }
                ) {
                  ALL_INTERPOLATIONS.forEach { interp ->
                    DropdownMenuItem(
                      text = { Text(stringResource(interp.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            if (showInterpTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_interpolation_format, stringResource(interpNode.interpolation.displayStringRId())),
                text = stringResource(interpNode.interpolation.getTooltip()),
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
                  value = stringResource(targetNode.target.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_target)) },
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
                        text = { Text(stringResource(target.displayStringRId())) },
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

                  TargetSection(stringResource(R.string.bg_section_size_shape), TARGETS_SIZE_SHAPE)
                  TargetSection(stringResource(R.string.bg_section_position), TARGETS_POSITION)
                  TargetSection(stringResource(R.string.bg_section_color_opacity), TARGETS_COLOR_OPACITY)
                }
              }
              IconButton(onClick = { showTargetTooltip = true }) {
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            if (showTargetTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_target_format, stringResource(targetNode.target.displayStringRId())),
                text = stringResource(targetNode.target.getTooltip()),
                onDismiss = { showTargetTooltip = false }
              )
            }
            val isAngleTarget = targetNode.target == ProtoBrushBehavior.Target.TARGET_ROTATION_OFFSET_IN_RADIANS ||
                                targetNode.target == ProtoBrushBehavior.Target.TARGET_HUE_OFFSET_IN_RADIANS ||
                                targetNode.target == ProtoBrushBehavior.Target.TARGET_SLANT_OFFSET_IN_RADIANS

            val displayValueStart = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeStart.toDouble()).toFloat() else targetNode.targetModifierRangeStart
            val displayValueEnd = if (isAngleTarget) Math.toDegrees(targetNode.targetModifierRangeEnd.toDouble()).toFloat() else targetNode.targetModifierRangeEnd

            BrushSliderControl(
              label = stringResource(R.string.bg_label_range_start),
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
              label = stringResource(R.string.bg_label_range_end),
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
                  value = stringResource(polarNode.target.displayStringRId()),
                  onValueChange = {},
                  readOnly = true,
                  label = { Text(stringResource(R.string.bg_polar_target)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPolar) },
                  modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                  expanded = expandedPolar,
                  onDismissRequest = { expandedPolar = false }
                ) {
                  ALL_POLAR_TARGETS.forEach { target ->
                    DropdownMenuItem(
                      text = { Text(stringResource(target.displayStringRId())) },
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
                Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
              }
            }
            if (showPolarTooltip) {
              TooltipDialog(
                title = stringResource(R.string.bg_title_polar_target_format, stringResource(polarNode.target.displayStringRId())),
                text = stringResource(polarNode.target.getTooltip()),
                onDismiss = { showPolarTooltip = false }
              )
            }
            // Angle (All targets): -360° to 360° (mapped to radians 0 to 2pi for now, 
            // but the user specified -360 to 360 degrees)
            val displayAngleStart = Math.toDegrees(polarNode.angleRangeStart.toDouble()).toFloat()
            val displayAngleEnd = Math.toDegrees(polarNode.angleRangeEnd.toDouble()).toFloat()

            BrushSliderControl(
              label = stringResource(R.string.bg_label_angle_start),
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
              label = stringResource(R.string.bg_label_angle_end),
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
              label = stringResource(R.string.bg_label_mag_start),
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
              label = stringResource(R.string.bg_label_mag_end),
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
            Text(stringResource(R.string.bg_fallback_filter_node))
          }
          ProtoBrushBehavior.Node.NodeCase.NODE_NOT_SET -> {
            Text(stringResource(R.string.bg_no_node_data_set))
          }
          else -> {
            Text(stringResource(R.string.bg_unknown_behavior_node_type))
          }
        }
      }
      is NodeData.Tip -> {
        val tip = data.tip
        TipPreviewWidget(tip, strokeRenderer)

        BrushSliderControl(
          label = stringResource(R.string.bg_label_scale_x),
          value = tip.scaleX,
          valueRange = 0f..2f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleX = it), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_scale_y),
          value = tip.scaleY,
          valueRange = 0f..2f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(scaleY = it), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_corner_rounding),
          value = tip.cornerRounding,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(cornerRounding = it), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_slant_degrees),
          value = Math.toDegrees(tip.slantRadians.toDouble()).toFloat(),
          valueRange = -90f..90f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(slantRadians = Math.toRadians(it.toDouble()).toFloat()), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_pinch),
          value = tip.pinch,
          valueRange = 0f..1f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(pinch = it), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_rotation_degrees),
          value = Math.toDegrees(tip.rotationRadians.toDouble()).toFloat(),
          valueRange = 0f..360f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(rotationRadians = Math.toRadians(it.toDouble()).toFloat()), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_particle_gap_distance_scale),
          value = tip.particleGapDistanceScale,
          valueRange = 0f..5f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDistanceScale = it), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
        BrushSliderControl(
          label = stringResource(R.string.bg_label_particle_gap_duration_ms),
          value = tip.particleGapDurationSeconds * 1000f,
          valueRange = 0f..1000f,
          onValueChange = { onUpdate(NodeData.Tip(tip.safeCopy(particleGapDurationSeconds = it / 1000f), behaviorPortIds = data.behaviorPortIds)) },
          onValueChangeFinished = onFieldEditComplete
        )
      }
      is NodeData.Coat -> {
        Text(stringResource(R.string.bg_coat_node_description), style = MaterialTheme.typography.bodySmall)
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
              value = stringResource(paint.selfOverlap.displayStringRId()),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_self_overlap)) },
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
                  text = { Text(stringResource(so.displayStringRId())) },
                  onClick = {
                    onUpdate(NodeData.Paint(paint.safeCopy(selfOverlap = so), texturePortIds = data.texturePortIds, colorPortIds = data.colorPortIds))
                    onDropdownEditComplete()
                    expanded = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_title_self_overlap_format, stringResource(paint.selfOverlap.displayStringRId())),
            text = stringResource(paint.selfOverlap.getTooltip()),
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
        val currentTypeResId = if (function.hasOpacityMultiplier()) {
          R.string.bg_opacity_multiplier
        } else {
          R.string.bg_replace_color
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
              value = stringResource(currentTypeResId),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_function_type)) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedType,
              onDismissRequest = { expandedType = false }
            ) {
              listOf(R.string.bg_opacity_multiplier, R.string.bg_replace_color).forEach { resId ->
                DropdownMenuItem(
                  text = { Text(stringResource(resId)) },
                  onClick = {
                    if (resId != currentTypeResId) {
                      onUpdate(
                        if (resId == R.string.bg_opacity_multiplier) {
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
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showTypeTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_title_function_type_format, stringResource(currentTypeResId)),
            text = stringResource(getColorFunctionTooltip(currentTypeResId)),
            onDismiss = { showTypeTooltip = false }
          )
        }
        if (function.hasOpacityMultiplier()) {
          BrushSliderControl(
            label = stringResource(R.string.bg_label_opacity_multiplier),
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
            Text(stringResource(R.string.bg_color_label), style = MaterialTheme.typography.bodyMedium)
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
          label = { Text(stringResource(R.string.bg_client_brush_family_id)) },
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          singleLine = true,
          enabled = !textFieldsLocked,
        )
        OutlinedTextField(
          value = data.developerComment,
          onValueChange = { onUpdate(data.copy(developerComment = it)) },
          label = { Text(stringResource(R.string.bg_brush_developer_comment)) },
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
              value = stringResource(data.inputModel.displayStringRId()),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_input_model)) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedModel,
              onDismissRequest = { expandedModel = false }
            ) {
              listOf(R.string.bg_model_sliding_window, R.string.bg_model_spring, R.string.bg_model_naive_experimental).forEach { modelResId ->
                DropdownMenuItem(
                  text = { Text(stringResource(modelResId)) },
                  onClick = {
                    val newModel =
                      when (modelResId) {
                        R.string.bg_model_naive_experimental ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setExperimentalNaiveModel(
                              ProtoBrushFamily.ExperimentalNaiveModel.getDefaultInstance()
                            )
                            .build()
                        R.string.bg_model_sliding_window ->
                          ProtoBrushFamily.InputModel.newBuilder()
                            .setSlidingWindowModel(
                              ProtoBrushFamily.SlidingWindowModel.newBuilder()
                                .setWindowSizeSeconds(0.02f)
                                .setExperimentalUpsamplingPeriodSeconds(0.005f)
                            )
                            .build()
                        R.string.bg_model_spring ->
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
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showModelTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_title_input_model_format, stringResource(data.inputModel.displayStringRId())),
            text = stringResource(getInputModelTooltip(data.inputModel.displayStringRId())),
            onDismiss = { showModelTooltip = false }
          )
        }

        if (data.inputModel.hasSlidingWindowModel()) {
          val slidingModel = data.inputModel.slidingWindowModel
          val windowSizeMs = slidingModel.windowSizeSeconds * 1000f
          val upsamplingHz = if (slidingModel.experimentalUpsamplingPeriodSeconds > 0) 1f / slidingModel.experimentalUpsamplingPeriodSeconds else 0f

          BrushSliderControl(
            label = stringResource(R.string.bg_label_window_size),
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
            label = stringResource(R.string.bg_label_upsampling_frequency),
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

internal val NODE_TYPES_START = listOf(
  ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE,
  ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE,
  ProtoBrushBehavior.Node.NodeCase.NOISE_NODE
)
internal val NODE_TYPES_OPERATOR = listOf(
  ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE,
  ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE,
  ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE,
  ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE,
  ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE,
  ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE
)
internal val NODE_TYPES_TERMINAL = listOf(
  ProtoBrushBehavior.Node.NodeCase.TARGET_NODE,
  ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE
)

internal fun createDefaultNode(nodeCase: ProtoBrushBehavior.Node.NodeCase): NodeData {
  return when (nodeCase) {
    ProtoBrushBehavior.Node.NodeCase.SOURCE_NODE ->
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
    ProtoBrushBehavior.Node.NodeCase.CONSTANT_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setConstantNode(ProtoBrushBehavior.ConstantNode.newBuilder().setValue(0f))
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.NOISE_NODE ->
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
    ProtoBrushBehavior.Node.NodeCase.TOOL_TYPE_FILTER_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setToolTypeFilterNode(
            ProtoBrushBehavior.ToolTypeFilterNode.newBuilder()
              .setEnabledToolTypes(1 shl 3) // Stylus
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.DAMPING_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setDampingNode(
            ProtoBrushBehavior.DampingNode.newBuilder()
              .setDampingSource(ProtoBrushBehavior.ProgressDomain.PROGRESS_DOMAIN_DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE)
              .setDampingGap(0.1f)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.RESPONSE_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setResponseNode(
            ProtoBrushBehavior.ResponseNode.newBuilder()
              .setPredefinedResponseCurve(ink.proto.PredefinedEasingFunction.PREDEFINED_EASING_LINEAR)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.INTEGRAL_NODE ->
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
    ProtoBrushBehavior.Node.NodeCase.BINARY_OP_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setBinaryOpNode(
            ProtoBrushBehavior.BinaryOpNode.newBuilder()
              .setOperation(ProtoBrushBehavior.BinaryOp.BINARY_OP_SUM)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.INTERPOLATION_NODE ->
      NodeData.Behavior(
        ProtoBrushBehavior.Node.newBuilder()
          .setInterpolationNode(
            ProtoBrushBehavior.InterpolationNode.newBuilder()
              .setInterpolation(ProtoBrushBehavior.Interpolation.INTERPOLATION_LERP)
          )
          .build()
      )
    ProtoBrushBehavior.Node.NodeCase.TARGET_NODE ->
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
    ProtoBrushBehavior.Node.NodeCase.POLAR_TARGET_NODE ->
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
    else -> throw IllegalArgumentException("Unsupported node case: $nodeCase")
  }
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
        Text(stringResource(R.string.bg_ok))
      }
    }
  )
}
