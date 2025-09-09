/*
 *
 *  *
 *  *  * Copyright 2025 Google LLC. All rights reserved.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */

package com.example.cahier.ui

import android.content.Context
import android.util.Log
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.storage.decode
import com.example.cahier.R
import com.example.cahier.data.CustomBrush


object CustomBrushes {
    private var customBrushes: List<CustomBrush>? = null
    private const val TAG = "CustomBrushes"

    fun getBrushes(context: Context): List<CustomBrush> {
        if (customBrushes == null) {
            customBrushes = loadCustomBrushes(context)
        }
        return customBrushes ?: emptyList()
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private fun loadCustomBrushes(context: Context): List<CustomBrush> {

        val brushFiles = mapOf(
            "Calligraphy" to (R.raw.calligraphy to R.drawable.draw_24px),
            "Flag Banner" to (R.raw.flag_banner to R.drawable.flag_24px),
            "Graffiti" to (R.raw.graffiti to R.drawable.format_paint_24px),
            "Groovy" to (R.raw.groovy to R.drawable.bubble_chart_24px),
            "Holiday lights" to (R.raw.holiday_lights to R.drawable.lightbulb_24px),
            "Lace" to (R.raw.lace to R.drawable.styler_24px),
            "Music" to (R.raw.music to R.drawable.music_note_24px),
            "Shadow" to (R.raw.shadow to R.drawable.blur_on_24px),
            "Twisted yarn" to (R.raw.twisted_yarn to R.drawable.line_weight_24px),
            "Wet paint" to (R.raw.wet_paint to R.drawable.water_drop_24px)
        )

        val loadedBrushes = brushFiles.mapNotNull { (name, pair) ->
            val (resourceId, icon) = pair
            try {
                val brushFamily = context.resources.openRawResource(resourceId).use { inputStream ->
                    BrushFamily.decode(inputStream)
                }
                CustomBrush(name, icon, brushFamily)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading custom brush $name", e)
                null
            }
        }
        return loadedBrushes
    }
}