package com.example.cahier.ui.brushgraph.data

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles persistence of the auto-saved brush graph using SharedPreferences.
 */
@Singleton
class BrushGraphPreferences @Inject constructor(
  @ApplicationContext private val context: Context
) {
  private val prefs = context.getSharedPreferences("brush_graph_prefs", Context.MODE_PRIVATE)

  /** Retrieves the raw bytes of the auto-saved brush, or null if none exists. */
  fun getAutoSaveBrush(): ByteArray? {
    val base64 = prefs.getString("auto_save_brush", null) ?: return null
    return Base64.decode(base64, Base64.DEFAULT)
  }

  /** Saves the raw bytes of the brush family as a Base64 string. */
  fun saveAutoSaveBrush(bytes: ByteArray) {
    val base64 = Base64.encodeToString(bytes, Base64.DEFAULT)
    prefs.edit().putString("auto_save_brush", base64).apply()
  }
}
