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

package com.example.cahier.developer.brushdesigner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import ink.proto.BrushBehavior

/**
 * A Canvas-based composable that visually plots the easing/response function
 * of a [BrushBehavior.ResponseNode].
 *
 * Supports:
 * - Cubic Bézier curves (cubicBezierResponseCurve)
 * - Linear piecewise curves (linearResponseCurve)
 * - Predefined easing functions (rendered as a straight line placeholder)
 * - Steps response curves (rendered as a staircase)
 */
@Composable
internal fun MathCurvePreview(
    responseNode: BrushBehavior.ResponseNode,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val w = size.width
            val h = size.height

            drawLine(
                gridColor,
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = 2f
            )
            drawLine(
                gridColor,
                start = Offset(0f, 0f),
                end = Offset(0f, h),
                strokeWidth = 2f
            )

            for (i in 1..3) {
                val frac = i / 4f
                drawLine(
                    gridColor.copy(alpha = 0.3f),
                    start = Offset(0f, h * (1f - frac)),
                    end = Offset(w, h * (1f - frac)),
                    strokeWidth = 1f
                )
                drawLine(
                    gridColor.copy(alpha = 0.3f),
                    start = Offset(w * frac, 0f),
                    end = Offset(w * frac, h),
                    strokeWidth = 1f
                )
            }

            val path = Path()
            val curveCase = responseNode.responseCurveCase

            when (curveCase) {
                BrushBehavior.ResponseNode.ResponseCurveCase.CUBIC_BEZIER_RESPONSE_CURVE -> {
                    val cb = responseNode.cubicBezierResponseCurve
                    path.moveTo(0f, h)
                    path.cubicTo(
                        cb.x1 * w, h - (cb.y1 * h),
                        cb.x2 * w, h - (cb.y2 * h),
                        w, 0f
                    )
                }

                BrushBehavior.ResponseNode.ResponseCurveCase.LINEAR_RESPONSE_CURVE -> {
                    val linear = responseNode.linearResponseCurve
                    val xs = linear.xList
                    val ys = linear.yList
                    if (xs.isNotEmpty() && ys.isNotEmpty()) {
                        path.moveTo(xs[0] * w, h - (ys[0] * h))
                        for (i in 1 until minOf(xs.size, ys.size)) {
                            path.lineTo(xs[i] * w, h - (ys[i] * h))
                        }
                    } else {
                        path.moveTo(0f, h)
                        path.lineTo(w, 0f)
                    }
                }

                BrushBehavior.ResponseNode.ResponseCurveCase.STEPS_RESPONSE_CURVE -> {
                    val steps = responseNode.stepsResponseCurve
                    val stepCount = steps.stepCount
                    if (stepCount > 0) {
                        val stepWidth = w / stepCount
                        val stepHeight = h / stepCount
                        path.moveTo(0f, h)
                        for (i in 0 until stepCount) {
                            val y = h - ((i + 1) * stepHeight)
                            path.lineTo(i * stepWidth, y)
                            path.lineTo((i + 1) * stepWidth, y)
                        }
                    } else {
                        path.moveTo(0f, h)
                        path.lineTo(w, 0f)
                    }
                }

                else -> {
                    path.moveTo(0f, h)
                    path.lineTo(w, 0f)
                }
            }

            drawPath(
                path,
                color = lineColor,
                style = Stroke(width = 4f)
            )
        }
    }
}
