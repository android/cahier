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


package com.example.cahier.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.R
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@OptIn(ExperimentalInkCustomBrushApi::class)
@Singleton
class CahierTextureBitmapStore @Inject constructor(@ApplicationContext context: Context) : TextureBitmapStore {
    private val resources = context.resources

    private val textureResources: Map<String, Int> = mapOf(
        "music-clef-g" to R.drawable.music_clef_g,
        "music-note-sixteenth" to R.drawable.music_note_sixteenth
    )

    private val loadedBitmaps = mutableMapOf<String, Bitmap>()

    override operator fun get(clientTextureId: String): Bitmap? {
        val id = getShortName(clientTextureId)
        return loadedBitmaps.getOrPut(id) {
            textureResources[id]?.let { loadBitmap(it) } ?: return null
        }
    }

    /** Returns all available texture IDs. */
    fun getAllIds(): Set<String> {
        return textureResources.keys + loadedBitmaps.keys
    }

    private fun getShortName(clientTextureId: String): String =
        clientTextureId.removePrefix("ink://ink").removePrefix("/texture:")

    private fun loadBitmap(@DrawableRes drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable)
            ?: throw IllegalStateException("Could not load bitmap for resource $drawable")
    }

    fun loadTexture(textureId: String, bitmap: Bitmap) {
        val id = getShortName(textureId)
        loadedBitmaps[id] = bitmap
    }
}