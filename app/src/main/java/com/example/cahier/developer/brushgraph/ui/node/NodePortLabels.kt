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

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.ui.INPUT_ROW_HEIGHT
import com.example.cahier.developer.brushgraph.ui.asString

@Composable
fun NodePortLabels(
    node: GraphNode,
    graph: BrushGraph,
    visiblePorts: List<Port>,
    isSelectionMode: Boolean,
    onPortClick: (String, Port) -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val occupiedPortIds = remember(node.id, graph.edges) {
        graph.edges.filter { it.toNodeId == node.id }.map { it.toPortId }.toSet()
    }
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            for ((index, port) in visiblePorts.withIndex()) {
                with(density) {
                    val isPortEmpty = port.id !in occupiedPortIds
                    Box(
                        modifier =
                            Modifier.height(INPUT_ROW_HEIGHT.toDp())
                                .fillMaxWidth()
                                .padding(
                                    start = 8.dp,
                                    end = if (index == 0 && node.data.hasOutput()) 48.dp else 8.dp
                                )
                                .let {
                                    if (isPortEmpty) {
                                        it
                                            .clickable(enabled = !isSelectionMode) {
                                                onPortClick(
                                                    node.id,
                                                    port
                                                )
                                            }
                                            .background(
                                                MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                    alpha = 0.5f
                                                ), RoundedCornerShape(4.dp)
                                            )
                                    } else {
                                        it
                                    }
                                },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            if (isPortEmpty) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.bg_cd_add),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = port.label?.asString() ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPortEmpty) MaterialTheme.colorScheme.primary else textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        // Output label on the right, aligned with the first input row.
        if (node.data.hasOutput()) {
            with(density) {
                Box(
                    modifier =
                        Modifier
                            .height(INPUT_ROW_HEIGHT.toDp())
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bg_label_out),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
        }
    }
}
