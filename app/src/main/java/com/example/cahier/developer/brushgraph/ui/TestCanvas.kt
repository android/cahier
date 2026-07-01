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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.cahier.R
import com.example.cahier.core.ui.DrawingSurface
import com.example.cahier.core.ui.theme.BrushBlack
import com.example.cahier.core.ui.theme.BrushBlue
import com.example.cahier.core.ui.theme.BrushGreen
import com.example.cahier.core.ui.theme.BrushRed
import com.example.cahier.core.ui.theme.BrushYellow
import com.example.cahier.core.ui.theme.extendedColorScheme
import com.example.cahier.developer.brushgraph.data.GraphValidationException
import com.example.cahier.developer.brushgraph.data.ValidationSeverity

@Composable
private fun TestCanvas(
    isInvertedCanvas: Boolean,
    strokeList: List<Stroke>,
    strokeRenderer: CanvasStrokeRenderer,
    brush: Brush,
    modifier: Modifier = Modifier,
    maskPath: Path? = null,
    onGetNextBrush: () -> Brush,
    onStrokesAdded: (List<Stroke>) -> Unit,
) {
    Box(modifier = modifier) {
        Text(
            stringResource(R.string.bg_test_canvas_draw_prompt),
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (isInvertedCanvas) {
                    MaterialTheme.colorScheme.inverseOnSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )
        DrawingSurface(
            strokes = strokeList,
            canvasStrokeRenderer = strokeRenderer,
            onStrokesFinished = onStrokesAdded,
            onErase = { _, _ -> },
            onEraseStart = {},
            onEraseEnd = {},
            currentBrush = brush,
            onGetNextBrush = onGetNextBrush,
            isEraserMode = false,
            backgroundImageUri = null,
            onStartDrag = {},
            modifier = Modifier.fillMaxSize(),
            maskPath = maskPath,
        )
    }
}

@Composable
private fun ClippedCanvasContainer(
    currentHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    AndroidView(
        factory = { context ->
            val frameLayout = FrameLayout(context).apply {
                clipChildren = true
            }
            val composeView = ComposeView(context).apply {
                setContent {
                    content()
                }
            }
            frameLayout.addView(
                composeView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            frameLayout
        },
        update = { frameLayout ->
            val heightPx = with(density) { currentHeight.roundToPx() }
            val lp = frameLayout.layoutParams
                ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx)
            if (lp.height != heightPx) {
                lp.height = heightPx
                frameLayout.layoutParams = lp
            }
        },
        modifier = modifier
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollapsiblePreviewPane(
    isPreviewExpanded: Boolean,
    isInvertedCanvas: Boolean,
    testAutoUpdateStrokes: Boolean,
    brushColor: Color,
    brushSize: Float,
    brush: Brush,
    strokeList: List<Stroke>,
    strokeRenderer: CanvasStrokeRenderer,
    topIssue: GraphValidationException?,
    currentHeight: Dp,
    onDragStateChanged: (Boolean) -> Unit,
    onHeightDrag: (Dp) -> Unit,
    modifier: Modifier = Modifier,
    exclusionRects: List<Rect> = emptyList(),
    onGetNextBrush: () -> Brush,
    onTogglePreviewExpanded: () -> Unit,
    onClearStrokes: () -> Unit,
    onToggleCanvasTheme: () -> Unit,
    onSetTestAutoUpdateStrokes: (Boolean) -> Unit,
    onUpdateTestBrushColor: (Color) -> Unit,
    onUpdateTestBrushSize: (Float) -> Unit,
    onStrokesAdded: (List<Stroke>) -> Unit,
    onChooseColor: (Color, (Color) -> Unit) -> Unit,
    onToggleNotificationPane: () -> Unit,
) {
    val density = LocalDensity.current
    val onHeightDragState = rememberUpdatedState(onHeightDrag)
    val currentHeightState = rememberUpdatedState(currentHeight)
    var isDragging by remember { mutableStateOf(false) }

    // Mutable state to synchronously track height during a drag gesture
    val dragHeightState = remember { mutableStateOf(currentHeight) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle Tab (always visible, draggable only when expanded)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .then(
                    if (isPreviewExpanded) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    dragHeightState.value = currentHeightState.value
                                    onDragStateChanged(true)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onDragStateChanged(false)
                                },
                                onDragCancel = {
                                    isDragging = false
                                    onDragStateChanged(false)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val deltaDp = with(density) { (-dragAmount.y).toDp() }
                                    val newHeight =
                                        (dragHeightState.value + deltaDp).coerceIn(120.dp, 500.dp)
                                    dragHeightState.value = newHeight
                                    onHeightDragState.value(newHeight)
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                )
                .clickable { onTogglePreviewExpanded() },
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isPreviewExpanded) {
                            Icons.Default.KeyboardArrowDown
                        } else {
                            Icons.Default.KeyboardArrowUp
                        },
                        contentDescription = if (isPreviewExpanded) stringResource(R.string.bg_test_canvas_collapse) else stringResource(
                            R.string.bg_test_canvas_expand
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.bg_test_canvas_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isPreviewExpanded) {
                        AdaptivePreviewControlsLayout(
                            badgeMinWidth = 160.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val scrollState = rememberScrollState()
                            val showLeftFade = scrollState.value > 0
                            val showRightFade =
                                scrollState.value < scrollState.maxValue && scrollState.maxValue > 0

                            Box(
                                modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .horizontalScroll(scrollState)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(R.string.bg_test_canvas_reset),
                                        modifier = Modifier.clickable { onClearStrokes() },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        stringResource(R.string.bg_test_canvas_invert),
                                        modifier = Modifier.clickable { onToggleCanvasTheme() },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.width(16.dp))

                                    // Auto-update toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = testAutoUpdateStrokes,
                                            onCheckedChange = { onSetTestAutoUpdateStrokes(it) }
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            stringResource(R.string.bg_auto_update),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))

                                    // Color picker
                                    var colorMenuExpanded by remember { mutableStateOf(false) }
                                    Box {
                                        val iconTint =
                                            if (brushColor.luminance() < 0.5f) Color.White else Color.Black

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(brushColor, shape = CircleShape)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    shape = CircleShape
                                                )
                                                .clip(CircleShape)
                                                .clickable { colorMenuExpanded = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.palette_24px),
                                                contentDescription = stringResource(R.string.color),
                                                tint = iconTint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = colorMenuExpanded,
                                            onDismissRequest = { colorMenuExpanded = false }
                                        ) {
                                            val colors = remember {
                                                listOf(
                                                    R.string.brush_designer_color_black to BrushBlack,
                                                    R.string.brush_designer_color_red to BrushRed,
                                                    R.string.brush_designer_color_blue to BrushBlue,
                                                    R.string.brush_designer_color_green to BrushGreen,
                                                    R.string.brush_designer_color_yellow to BrushYellow
                                                )
                                            }
                                            colors.forEach { (nameRes, color) ->
                                                val name = stringResource(nameRes)
                                                DropdownMenuItem(
                                                    text = { Text(name) },
                                                    leadingIcon = {
                                                        Icon(
                                                            painter = painterResource(R.drawable.circle_24px),
                                                            contentDescription = name,
                                                            tint = color
                                                        )
                                                    },
                                                    onClick = {
                                                        onUpdateTestBrushColor(color)
                                                        colorMenuExpanded = false
                                                    }
                                                )
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.brush_designer_custom_color)) },
                                                leadingIcon = {
                                                    Icon(
                                                        painter = painterResource(R.drawable.palette_24px),
                                                        contentDescription = stringResource(R.string.brush_designer_custom_color),
                                                        tint = MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    colorMenuExpanded = false
                                                    onChooseColor(brushColor) { newColor ->
                                                        onUpdateTestBrushColor(newColor)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))

                                    // Size selector
                                    var sizeExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = sizeExpanded,
                                        onExpandedChange = { sizeExpanded = it },
                                        modifier = Modifier.width(80.dp)
                                    ) {
                                        Text(
                                            text = "${brushSize.toInt()}px",
                                            modifier = Modifier
                                                .menuAnchor(
                                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                    enabled = true
                                                )
                                                .clickable { sizeExpanded = true },
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        ExposedDropdownMenu(
                                            expanded = sizeExpanded,
                                            onDismissRequest = { sizeExpanded = false }
                                        ) {
                                            for (size in (2..4 step 1) + (6..10 step 2) + (20..40 step 10) + (50..100 step 25)) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            stringResource(
                                                                R.string.bg_size_px,
                                                                size
                                                            )
                                                        )
                                                    },
                                                    onClick = {
                                                        onUpdateTestBrushSize(size.toFloat())
                                                        sizeExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Scroll fades
                                if (showLeftFade) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(16.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                            .align(Alignment.CenterStart)
                                    )
                                }
                                if (showRightFade) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(16.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                )
                                            )
                                            .align(Alignment.CenterEnd)
                                    )
                                }
                            }

                            // Top issue, if one is present
                            if (topIssue != null) {
                                val isError = topIssue.severity == ValidationSeverity.ERROR
                                Surface(
                                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.extendedColorScheme.warning,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .clickable { onToggleNotificationPane() }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isError) Icons.Default.Error else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColorScheme.onWarning,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(
                                                    if (isError) R.string.bg_error else R.string.bg_warning,
                                                    topIssue.displayMessage.asString()
                                                ),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isError) MaterialTheme.colorScheme.onError else MaterialTheme.extendedColorScheme.onWarning,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Drag handle pill centered at the very top of the bar (only visible when expanded)
                if (isPreviewExpanded) {
                    val handleScale by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isDragging) 1.2f else 1.0f,
                        label = "handleScale"
                    )
                    val handleAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isDragging) 0.8f else 0.4f,
                        label = "handleAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp)
                            .graphicsLayer(scaleX = handleScale, scaleY = handleScale)
                            .size(width = 36.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = handleAlpha),
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // The drawer height is smoothly driven by currentHeight (animatedPreviewHeight)
                // minus the 40.dp tab header. We coerce it to >= 0.dp.
                .height((currentHeight - 40.dp).coerceAtLeast(0.dp)),
            tonalElevation = 8.dp,
            color = if (isInvertedCanvas) {
                MaterialTheme.colorScheme.inverseSurface
            } else {
                MaterialTheme.colorScheme.surface
            },
        ) {
            val contentHeight = (currentHeight - 40.dp).coerceAtLeast(0.dp)
            var containerPositionInWindow by remember { mutableStateOf(Offset.Zero) }
            var containerSize by remember { mutableStateOf(IntSize.Zero) }
            var canvasPositionInWindow by remember { mutableStateOf(Offset.Zero) }
            var canvasSize by remember { mutableStateOf(IntSize.Zero) }

            val maskPath = remember(
                containerPositionInWindow,
                containerSize,
                canvasPositionInWindow,
                canvasSize,
                exclusionRects
            ) {
                createMaskPath(
                    containerPositionInWindow,
                    containerSize,
                    canvasPositionInWindow,
                    canvasSize,
                    exclusionRects
                )
            }

            ClippedCanvasContainer(
                currentHeight = contentHeight,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        containerPositionInWindow = coordinates.positionInWindow()
                        containerSize = coordinates.size
                    }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TestCanvas(
                        strokeList = strokeList,
                        strokeRenderer = strokeRenderer,
                        brush = brush,
                        isInvertedCanvas = isInvertedCanvas,
                        maskPath = maskPath,
                        onGetNextBrush = onGetNextBrush,
                        onStrokesAdded = onStrokesAdded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(500.dp)
                            .onGloballyPositioned { coordinates ->
                                canvasPositionInWindow = coordinates.positionInWindow()
                                canvasSize = coordinates.size
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptivePreviewControlsLayout(
    badgeMinWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        if (measurables.size == 1) {
            val placeable = measurables[0].measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        } else if (measurables.size >= 2) {
            val controlsMeasurable = measurables[0]
            val badgeMeasurable = measurables[1]

            val minBadgePx = badgeMinWidth.roundToPx()
            val controlsMaxWidth = maxOf(0, constraints.maxWidth - minBadgePx)

            // Measure controls first up to available width minus minimum badge width
            val controlsPlaceable = controlsMeasurable.measure(
                constraints.copy(minWidth = 0, maxWidth = controlsMaxWidth)
            )

            // Badge takes all the remaining space
            val badgeWidth = maxOf(minBadgePx, constraints.maxWidth - controlsPlaceable.width)
            val badgePlaceable = badgeMeasurable.measure(
                constraints.copy(minWidth = badgeWidth, maxWidth = badgeWidth)
            )

            val totalWidth =
                minOf(constraints.maxWidth, controlsPlaceable.width + badgePlaceable.width)
            val height = maxOf(controlsPlaceable.height, badgePlaceable.height)

            layout(totalWidth, height) {
                controlsPlaceable.placeRelative(0, (height - controlsPlaceable.height) / 2)
                badgePlaceable.placeRelative(
                    controlsPlaceable.width,
                    (height - badgePlaceable.height) / 2
                )
            }
        } else {
            layout(0, 0) {}
        }
    }
}

private fun createMaskPath(
    containerPositionInWindow: Offset,
    containerSize: IntSize,
    canvasPositionInWindow: Offset,
    canvasSize: IntSize,
    exclusionRects: List<Rect>,
): Path? {
    if (containerSize == IntSize.Zero || canvasSize == IntSize.Zero) return null

    val yContainerTop = containerPositionInWindow.y
    val yCanvasTop = canvasPositionInWindow.y
    val yContainerBottom = yContainerTop + containerSize.height

    val yVisibleTopLocal = (yContainerTop - yCanvasTop).coerceAtLeast(0f)
    val yVisibleBottomLocal =
        (yContainerBottom - yCanvasTop).coerceAtMost(canvasSize.height.toFloat())

    return Path().apply {
        if (yVisibleTopLocal > 0f) {
            addRect(
                Rect(
                    left = 0f,
                    top = 0f,
                    right = canvasSize.width.toFloat(),
                    bottom = yVisibleTopLocal
                )
            )
        }
        if (yVisibleBottomLocal < canvasSize.height.toFloat()) {
            addRect(
                Rect(
                    left = 0f,
                    top = yVisibleBottomLocal,
                    right = canvasSize.width.toFloat(),
                    bottom = canvasSize.height.toFloat()
                )
            )
        }
        val xCanvasLeft = canvasPositionInWindow.x
        exclusionRects.forEach { rect ->
            val localLeft = (rect.left - xCanvasLeft).coerceAtLeast(0f)
            val localTop = (rect.top - yCanvasTop).coerceAtLeast(0f)
            val localRight = (rect.right - xCanvasLeft).coerceAtMost(canvasSize.width.toFloat())
            val localBottom = (rect.bottom - yCanvasTop).coerceAtMost(canvasSize.height.toFloat())

            if (localLeft < localRight && localTop < localBottom) {
                addRect(
                    Rect(
                        left = localLeft,
                        top = localTop,
                        right = localRight,
                        bottom = localBottom
                    )
                )
            }
        }
    }
}
