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
@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class)

package com.example.cahier.developer.brushgraph.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import com.example.cahier.developer.brushgraph.ui.asString
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.ui.fields.NodeFields
import com.example.cahier.developer.brushgraph.ui.TooltipDialog
import com.example.cahier.developer.brushgraph.ui.getTooltip
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.DisplayText

/** Shows connection details between two nodes and allows deletion. */
@Composable
fun EdgeInspector(
  edge: GraphEdge,
  fromNode: GraphNode,
  toNode: GraphNode,
  onNodeFocus: (String) -> Unit,
  onDisableChange: (Boolean) -> Unit,
  onDelete: () -> Unit,
  onAddNodeBetween: () -> Unit,
  modifier: Modifier = Modifier,
  inputLabel: DisplayText? = null,
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text(stringResource(R.string.bg_delete_edge)) },
      text = { Text(stringResource(R.string.bg_delete_edge_confirmation)) },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete()
            showDeleteConfirmation = false
          }
        ) {
          Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.bg_cancel)) }
      },
    )
  }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    // From Node Section
    EdgeNodeInfo(label = DisplayText.Resource(R.string.bg_label_from), node = fromNode, onClick = { onNodeFocus(fromNode.id) })
    if (fromNode.data is NodeData.Behavior && toNode.data is NodeData.Behavior) {
      Spacer(Modifier.height(8.dp))
      Button(
        onClick = { onAddNodeBetween() },
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text(stringResource(R.string.bg_add_node_between))
      }
      Spacer(Modifier.height(8.dp))
    } else {
      Spacer(Modifier.height(16.dp))
    }

    // To Node Section
    EdgeNodeInfo(
      label = DisplayText.Resource(R.string.bg_label_to),
      node = toNode,
      inputLabel = inputLabel,
      onClick = { onNodeFocus(toNode.id) },
    )

    Spacer(modifier = Modifier.weight(1f))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
      Checkbox(
        checked = edge.isDisabled,
        onCheckedChange = { checked ->
          onDisableChange(checked)
        }
      )
      Spacer(Modifier.width(8.dp))
      Text(stringResource(R.string.bg_disable_edge))
    }
    Spacer(Modifier.height(8.dp))

    Button(
      onClick = { showDeleteConfirmation = true },
      modifier = Modifier.fillMaxWidth().height(48.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
      Text(stringResource(R.string.delete))
    }
  }
}

@Composable
private fun EdgeNodeInfo(
  label: DisplayText,
  node: GraphNode,
  modifier: Modifier = Modifier,
  inputLabel: DisplayText? = null,
  onClick: () -> Unit,
) {
  val title = node.data.title()
  val subtitles = node.data.subtitles()

  Column(modifier = modifier.fillMaxWidth().clickable { onClick() }.padding(8.dp)) {
    Text(
      text = label.asString(),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    for (subtitle in subtitles) {
      val text = subtitle.asString()
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (inputLabel != null) {
      val labelText = inputLabel.asString()
      Text(
        text = stringResource(R.string.bg_label_input_with_value, labelText),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** Renders the content of the node inspector. */
@Composable
fun NodeInspector(
  node: GraphNode,
  onUpdate: (NodeData) -> Unit,
  onDisableChange: (Boolean) -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  textFieldsLocked: Boolean,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  onFieldEditComplete: () -> Unit = {},
  onDropdownEditComplete: () -> Unit = {},
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text(stringResource(R.string.bg_delete_node)) },
      text = { Text(stringResource(R.string.bg_delete_node_confirmation)) },
      confirmButton = {
        TextButton(
          onClick = {
            onDelete()
            showDeleteConfirmation = false
          }
        ) {
          Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.bg_cancel)) }
      },
    )
  }

  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    Box(modifier = Modifier.weight(1f)) {
      NodeFields(
        node = node,
        onUpdate = onUpdate,
        onChooseColor = onChooseColor,
        allTextureIds = allTextureIds,
        onLoadTexture = onLoadTexture,
        strokeRenderer = strokeRenderer,
        textFieldsLocked = textFieldsLocked,
        onFieldEditComplete = onFieldEditComplete,
        onDropdownEditComplete = onDropdownEditComplete,
      )
    }

    Spacer(Modifier.height(16.dp))

    if (node.data !is NodeData.Family) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
      ) {
        Checkbox(
          checked = node.isDisabled,
          onCheckedChange = { checked ->
            onDisableChange(checked)
          }
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.bg_disable_node))
      }
      Spacer(Modifier.height(8.dp))

      Button(
        onClick = { showDeleteConfirmation = true },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
      ) {
        Text(stringResource(R.string.delete))
      }
    }
  }
}

@Composable
fun AdaptiveInspectorPane(
  isWideScreen: Boolean,
  visible: Boolean,
  title: String,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  tooltipText: String? = null,
  content: @Composable () -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter =
      if (isWideScreen) {
        slideInHorizontally(initialOffsetX = { it })
      } else {
        slideInVertically(initialOffsetY = { it })
      },
    exit =
      if (isWideScreen) {
        slideOutHorizontally(targetOffsetX = { it })
      } else {
        slideOutVertically(targetOffsetY = { it })
      },
    modifier = modifier.zIndex(10f),
  ) {
    Surface(
      modifier =
        if (isWideScreen) {
          Modifier.fillMaxHeight().width(INSPECTOR_WIDTH_LANDSCAPE.dp)
        } else {
          Modifier.fillMaxWidth().height(INSPECTOR_HEIGHT_PORTRAIT.dp)
        },
      tonalElevation = 8.dp,
      shadowElevation = 8.dp,
      color = MaterialTheme.colorScheme.surface,
    ) {
      Column {
        // Title bar with close button
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
          var showTooltip by remember { mutableStateOf(false) }
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              if (tooltipText != null) {
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { showTooltip = true }, modifier = Modifier.size(24.dp)) {
                  Icon(
                    Icons.AutoMirrored.Filled.Help,
                    contentDescription = stringResource(R.string.bg_cd_help),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
            IconButton(onClick = onClose) {
              Icon(Icons.Default.Close, contentDescription = stringResource(R.string.bg_content_description_close_inspector))
            }
            if (showTooltip && tooltipText != null) {
              TooltipDialog(
                title = title,
                text = tooltipText,
                onDismiss = { showTooltip = false }
              )
            }
          }
        }
        Box(modifier = Modifier.fillMaxSize()) {
          content()
        }
      }
    }
  }
}
