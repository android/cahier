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

package com.example.cahier.developer.brushgraph.data

import java.util.Locale
import kotlin.math.PI

/**
 * Defines display limits and unit conversion for numeric fields.
 *
 * @param min minimum display value
 * @param max maximum display value
 * @param step increment/decrement step in display units
 * @param displayUnit suffix shown after the value (e.g. "°", "%")
 * @param unitScale multiplier to convert real value → display value
 */
class NumericLimits(
    val min: Float,
    val max: Float,
    val step: Float = 0.01f,
    val displayUnit: String = "",
    val unitScale: Float = 1.0f,
) {
    /** Convert a display value back to the real value as described by [unitScale]. */
    fun toRealValue(displayValue: Float): Float = displayValue / unitScale

    /** Convert a real value to the display value as described by [unitScale]. */
    fun fromRealValue(realValue: Float): Float = realValue * unitScale

    /** Format a display value with its unit suffix. */
    fun format(displayValue: Float): String {
        val formatted = if (displayValue == displayValue.toLong().toFloat()) {
            displayValue.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", displayValue)
        }
        return "$formatted$displayUnit"
    }

    companion object {
        /** Values stored as 0..1 shown as 0..100% */
        fun floatShownAsPercent(
            minPercent: Float = 0f,
            maxPercent: Float = 100f,
        ): NumericLimits =
            NumericLimits(
                min = minPercent,
                max = maxPercent,
                step = 1f,
                displayUnit = "%",
                unitScale = 100f
            )

        /** Values stored in radians, shown in degrees */
        fun radiansShownAsDegrees(
            minDegrees: Float,
            maxDegrees: Float,
        ): NumericLimits =
            NumericLimits(
                min = minDegrees,
                max = maxDegrees,
                step = 1f,
                displayUnit = "°",
                unitScale = (180f / PI.toFloat())
            )
    }
}
