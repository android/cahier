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

package com.example.cahier.developer.brushgraph.viewmodel

import androidx.compose.ui.graphics.Color
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphEdge
import com.example.cahier.developer.brushgraph.data.GraphPoint
import com.example.cahier.developer.brushgraph.data.GraphValidationException

data class BrushGraphUiState(
  val graph: BrushGraph = BrushGraph(),
  val isSelectionMode: Boolean = false,
  val selectedNodeIds: Set<String> = emptySet(),
  val activeEdgeSourceId: String? = null,
  val selectedEdge: GraphEdge? = null,
  val testAutoUpdateStrokes: Boolean = true,
  val testBrushColor: Color? = null,
  val testBrushSize: Float = 10f,
  val isErrorPaneOpen: Boolean = false,
  val zoom: Float = 1f,
  val offset: GraphPoint = GraphPoint(0f, 0f),
  val textFieldsLocked: Boolean = false,
  val selectedNodeId: String? = null,
  val focusTrigger: Int = 0,
  val detachedEdge: GraphEdge? = null,
  val isPreviewExpanded: Boolean = true,
  val isDarkCanvas: Boolean = false,
  val graphIssues: List<GraphValidationException> = emptyList(),
  val allTextureIds: Set<String> = emptySet(),
)