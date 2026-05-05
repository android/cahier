package com.example.cahier.developer.brushgraph.viewmodel

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
  val testBrushColor: Int? = null,
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
  val allTextureIds: Set<String> = emptySet()
)