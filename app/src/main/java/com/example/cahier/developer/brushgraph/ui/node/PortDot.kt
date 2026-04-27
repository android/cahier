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

package com.example.cahier.developer.brushgraph.ui.node

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.cahier.R
import com.example.cahier.developer.brushgraph.data.Port
import com.example.cahier.developer.brushgraph.data.PortSide
import com.example.cahier.developer.brushgraph.ui.INPUT_ROW_HEIGHT

@Composable
fun PortDot(
  port: Port,
  count: Int,
  modifier: Modifier,
  zoom: Float,
  onDrag: (PortSide, String, Boolean) -> Unit,
  onDragUpdate: (Offset) -> Unit = {},
  onDragEnd: () -> Unit = {},
  onPortPositioned: (Offset) -> Unit,
  canvasCoordinates: LayoutCoordinates? = null,
  portPosition: Offset,
  isReorderable: Boolean = false,
  onReorderUpdate: (Float) -> Unit = {},
  onReorderEnd: () -> Unit = {},
  isDragging: Boolean = false,
  dragOffset: Float = 0f,
  isLargeHandle: Boolean = false,
) {
  var portCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
  val density = LocalDensity.current

  val currentOnDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
  val currentOnDragUpdate by androidx.compose.runtime.rememberUpdatedState(onDragUpdate)
  val currentOnDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
  val currentOnReorderUpdate by androidx.compose.runtime.rememberUpdatedState(onReorderUpdate)
  val currentOnReorderEnd by androidx.compose.runtime.rememberUpdatedState(onReorderEnd)
  val currentPortId by androidx.compose.runtime.rememberUpdatedState(port.id)
  val currentPortSide by androidx.compose.runtime.rememberUpdatedState(port.side)

  val outerWidth = if (port.side == PortSide.INPUT) 24.dp else 12.dp
  val outerHeight = if (port.side == PortSide.INPUT) 32.dp else 12.dp
  val outerX = if (port.side == PortSide.INPUT) (-24).dp else 14.dp
  
  val animatedY by animateDpAsState(
      targetValue = with(density) { portPosition.y.toDp() } - (if (port.side == PortSide.INPUT) 16.dp else 6.dp),
      label = "portY"
  )
  val finalY = if (isDragging) with(density) { portPosition.y.toDp() } - (if (port.side == PortSide.INPUT) 16.dp else 6.dp) else animatedY

  Box(
    modifier =
      modifier
        .offset {
          IntOffset(outerX.roundToPx(), finalY.roundToPx())
        }
        .size(width = outerWidth, height = outerHeight)
        .graphicsLayer {
          if (isDragging) {
              translationY = dragOffset
          }
        }
        .zIndex(if (isDragging) 1f else 0f)
  ) {
    if (port.side == PortSide.INPUT) {
      // Input Port (Left Half)
      Box(
        modifier = Modifier
          .align(Alignment.TopStart)
          .size(width = 12.dp, height = 32.dp)
          .pointerInput(port.nodeId, port.side, canvasCoordinates, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDragStart = { currentOnDrag(currentPortSide, currentPortId, true) },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
              ) { change, _ ->
              change.consume()
              val canvasCo = canvasCoordinates
              val portCo = portCoordinates
              if (canvasCo != null && portCo != null && canvasCo.isAttached && portCo.isAttached) {
                val graphSpacePos = canvasCo.localPositionOf(portCo, change.position)
                currentOnDragUpdate(graphSpacePos)
              }
            }
          }
      ) {
          Box(
            modifier = Modifier
              .align(Alignment.Center)
              .size(12.dp)
              .background(
                  if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  CircleShape
              )
              .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
              .onGloballyPositioned { coordinates: androidx.compose.ui.layout.LayoutCoordinates ->
                portCoordinates = coordinates
                val canvasCo = canvasCoordinates
                if (canvasCo != null && coordinates.isAttached) {
                  val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                  val graphSpacePos = canvasCo.localPositionOf(coordinates, center)
                  onPortPositioned(graphSpacePos)
                }
              }
          )
      }

      // Reorder Handle (Right Half)
      if (isReorderable) {
        val handleHeight = if (isLargeHandle) with(density) { (INPUT_ROW_HEIGHT * 2).toDp() } else 32.dp
        Box(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(width = 12.dp, height = handleHeight)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
            .pointerInput(port.nodeId, port.side, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDrag = { change, dragAmount ->
                  change.consume()
                  currentOnReorderUpdate(dragAmount.y)
                },
                onDragEnd = { currentOnReorderEnd() },
                onDragCancel = { currentOnReorderEnd() }
              )
            }
        ) {
            Icon(
              painter = painterResource(R.drawable.gs_drag_indicator_vd_theme_24),
              contentDescription = stringResource(R.string.bg_cd_reorder),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.align(Alignment.Center).size(12.dp)
            )
        }
      }
    } else {
      // Output Port
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(12.dp)
          .background(
              if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              CircleShape
          )
          .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
          .onGloballyPositioned { coordinates: androidx.compose.ui.layout.LayoutCoordinates ->
            portCoordinates = coordinates
            val canvasCo = canvasCoordinates
            if (canvasCo != null && coordinates.isAttached) {
              val center = Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
              val graphSpacePos = canvasCo.localPositionOf(coordinates, center)
              onPortPositioned(graphSpacePos)
            }
          }
          .pointerInput(port.nodeId, port.side, canvasCoordinates, zoom) {
              detectPortDragGestures(
                zoom = zoom,
                onDragStart = { currentOnDrag(currentPortSide, currentPortId, true) },
                onDragEnd = { currentOnDragEnd() },
                onDragCancel = { currentOnDragEnd() },
              ) { change, _ ->
              change.consume()
              val canvasCo = canvasCoordinates
              val portCo = portCoordinates
              if (canvasCo != null && portCo != null && canvasCo.isAttached && portCo.isAttached) {
                val graphSpacePos = canvasCo.localPositionOf(portCo, change.position)
                currentOnDragUpdate(graphSpacePos)
              }
            }
          }
      )
    }
  }
}

suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectPortDragGestures(
  zoom: Float,
  onDragStart: (Offset) -> Unit = {},
  onDragEnd: () -> Unit = {},
  onDragCancel: () -> Unit = {},
  onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
  val touchSlop = viewConfiguration.touchSlop / zoom

  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    var drag: PointerInputChange? = null
    var dragStartedCalled = false

    // Wait for the pointer to move beyond the (zoom-adjusted) touch slop.
    val pointerId = down.id
    var totalMainPositionChange = Offset.Zero
    while (true) {
      val event = awaitPointerEvent()
      val dragEvent = event.changes.firstOrNull { it.id == pointerId } ?: break
      if (dragEvent.isConsumed) break
      if (dragEvent.changedToUpIgnoreConsumed()) break

      val positionChange = dragEvent.positionChange()
      totalMainPositionChange += positionChange
      val distance = totalMainPositionChange.getDistance()
      if (distance >= touchSlop) {
        onDragStart(dragEvent.position)
        dragStartedCalled = true
        onDrag(dragEvent, totalMainPositionChange)
        if (dragEvent.isConsumed) {
          drag = dragEvent
        }
        break
      }
      if (event.changes.all { it.changedToUpIgnoreConsumed() }) break
    }

    if (drag != null || totalMainPositionChange.getDistance() >= touchSlop) {
      val dragSuccessful =
        drag(pointerId) {
          onDrag(it, it.positionChange())
          it.consume()
        }
      if (!dragSuccessful) {
        if (dragStartedCalled) onDragCancel()
      } else {
        if (dragStartedCalled) onDragEnd()
      }
    }
  }
}
