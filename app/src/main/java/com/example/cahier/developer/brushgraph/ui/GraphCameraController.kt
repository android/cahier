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

package com.example.cahier.developer.brushgraph.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import com.example.cahier.developer.brushgraph.data.BrushGraph
import com.example.cahier.developer.brushgraph.data.GraphNode
import com.example.cahier.developer.brushgraph.data.TutorialAnchor
import com.example.cahier.developer.brushgraph.data.TutorialStep
import com.example.cahier.developer.brushgraph.ui.node.NodeRegistry

private const val TUTORIAL_TARGET_Y = 280f

@Composable
fun GraphCameraController(
    offset: Offset,
    tutorialStep: TutorialStep?,
    focusTrigger: Int,
    graph: BrushGraph,
    zoom: Float,
    isPreviewExpanded: Boolean,
    selectedNodeId: String?,
    updateOffset: (Offset) -> Unit,
    viewportSize: Size,
    context: Context,
    isWideScreen: Boolean,
    maxWidthDp: Dp,
    nodeRegistry: NodeRegistry,
) {
    val animatableOffset = remember { Animatable(offset, Offset.VectorConverter) }

    // Auto-pan to node in tutorial
    LaunchedEffect(tutorialStep) {
        val step = tutorialStep
        if (step != null && step.anchor == TutorialAnchor.NODE_CANVAS) {
            val node = step.getTargetNode(graph)
            if (node != null) {
                val density = context.resources.displayMetrics.density
                val targetY = TUTORIAL_TARGET_Y * density
                val targetX = maxWidthDp.value * density / 2f

                val newOffset = calculateFocusOffset(
                    node = node,
                    position = nodeRegistry.getNodePosition(node.id) ?: Offset.Zero,
                    zoom = zoom,
                    targetScreenPos = Offset(targetX, targetY)
                )

                animatableOffset.snapTo(offset)
                animatableOffset.animateTo(newOffset, animationSpec = tween(500)) {
                    updateOffset(this.value)
                }
            }
        }
    }

    // Listen for ViewModel events (e.g. center on node)
    LaunchedEffect(focusTrigger) {
        if (focusTrigger > 0) {
            selectedNodeId?.let { nodeId ->
                val node = graph.nodes.find { it.id == nodeId }
                if (node != null) {
                    val density = context.resources.displayMetrics.density
                    val newOffset = calculateFocusOffset(
                        node = node,
                        position = nodeRegistry.getNodePosition(node.id) ?: Offset.Zero,
                        zoom = zoom,
                        viewportSize = viewportSize,
                        density = density,
                        isWideScreen = isWideScreen,
                        isPreviewExpanded = isPreviewExpanded
                    )
                    animatableOffset.snapTo(offset)
                    animatableOffset.animateTo(newOffset, animationSpec = tween(500)) {
                        updateOffset(this.value)
                    }
                }
            }
        }
    }
}

private fun calculateFocusOffset(
    node: GraphNode,
    position: Offset,
    zoom: Float,
    viewportSize: Size = Size.Zero,
    density: Float = 1f,
    isWideScreen: Boolean = false,
    isPreviewExpanded: Boolean = false,
    targetScreenPos: Offset? = null,
): Offset {
    val nodeCenterX = position.x + node.data.width() / 2f
    val nodeCenterY = position.y + node.data.height() / 2f

    val targetPos = if (targetScreenPos != null) {
        Pair(targetScreenPos.x, targetScreenPos.y)
    } else {
        val previewHeightPx =
            (if (isPreviewExpanded) PREVIEW_HEIGHT_EXPANDED else PREVIEW_HEIGHT_COLLAPSED) * density
        val safeSize = if (isWideScreen) {
            val inspectorWidthPx = INSPECTOR_WIDTH_LANDSCAPE * density
            Pair(viewportSize.width - inspectorWidthPx, viewportSize.height - previewHeightPx)
        } else {
            val inspectorHeightPx = INSPECTOR_HEIGHT_PORTRAIT * density
            Pair(
                viewportSize.width,
                viewportSize.height - maxOf(inspectorHeightPx, previewHeightPx)
            )
        }
        Pair(safeSize.first / 2f, safeSize.second / 2f)
    }

    val targetX = targetPos.first
    val targetY = targetPos.second

    return Offset(targetX - nodeCenterX * zoom, targetY - nodeCenterY * zoom)
}
