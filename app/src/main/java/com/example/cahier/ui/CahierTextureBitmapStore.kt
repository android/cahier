package com.example.cahier.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import com.example.cahier.R

@OptIn(ExperimentalInkCustomBrushApi::class)
class CahierTextureBitmapStore(context: Context) : TextureBitmapStore {
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

    private fun getShortName(clientTextureId: String): String =
        clientTextureId.removePrefix("ink://ink").removePrefix("/texture:")

    private fun loadBitmap(@DrawableRes drawable: Int): Bitmap {
        return BitmapFactory.decodeResource(resources, drawable)
            ?: throw IllegalStateException("Could not load bitmap for resource $drawable")
    }
}