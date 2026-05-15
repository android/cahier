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

package com.example.cahier.features.drawing

import android.content.Context
import android.util.Log
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.Version
import androidx.ink.storage.AndroidBrushFamilySerialization
import com.example.cahier.R
import com.example.cahier.core.data.CustomBrush
import com.example.cahier.core.ui.CahierTextureBitmapStore


object CustomBrushes {
    private var customBrushes: List<CustomBrush>? = null
    private const val TAG = "CustomBrushes"

    fun getBrushes(context: Context, textureStore: CahierTextureBitmapStore): List<CustomBrush> {
        return customBrushes ?: synchronized(this) {
            customBrushes ?: loadCustomBrushes(context, textureStore).also { customBrushes = it }
        }
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private fun loadCustomBrushes(
        context: Context,
        textureStore: CahierTextureBitmapStore,
    ): List<CustomBrush> {
        val brushFiles = mapOf(
            context.getString(R.string.calligraphy) to (R.raw.calligraphy to R.drawable.draw_24px),
            context.getString(R.string.flag_banner) to (R.raw.flag_banner to R.drawable.flag_24px),
            context.getString(R.string.graffiti) to (R.raw.graffiti to R.drawable.format_paint_24px),
            context.getString(R.string.groovy) to (R.raw.groovy to R.drawable.bubble_chart_24px),
            context.getString(R.string.holiday_lights) to (R.raw.holiday_lights to R.drawable.lightbulb_24px),
            context.getString(R.string.jelly_wobble) to (R.raw.jelly_wobble to R.drawable.airwave_24px),
            context.getString(R.string.lace) to (R.raw.lace to R.drawable.styler_24px),
            context.getString(R.string.music) to (R.raw.music to R.drawable.music_note_24px),
            context.getString(R.string.pressure_wave) to (R.raw.pressure_wave to R.drawable.vital_signs_24px),
            context.getString(R.string.shading_pencil) to (R.raw.shading_pencil to R.drawable.stylus_pencil_24px),
            context.getString(R.string.shadow) to (R.raw.shadow to R.drawable.blur_on_24px),
            context.getString(R.string.twisted_yarn) to (R.raw.twisted_yarn to R.drawable.line_weight_24px),
            context.getString(R.string.wet_paint) to (R.raw.wet_paint to R.drawable.water_drop_24px),
        )

        val loadedBrushes = brushFiles.mapNotNull { (name, pair) ->
            val (resourceId, icon) = pair
            try {
                val brushFamily = context.resources.openRawResource(resourceId).use { inputStream ->
                    AndroidBrushFamilySerialization.decode(
                        inputStream,
                        maxVersion = Version.DEVELOPMENT
                    ) { id, bitmap ->
                        if (bitmap != null)
                            textureStore.loadTexture(id, bitmap)
                        id
                    }
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