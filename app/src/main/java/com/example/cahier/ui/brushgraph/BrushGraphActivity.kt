package com.example.cahier.ui.brushgraph

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.Window
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockTextureBitmapStore
import androidx.ink.brush.TextureBitmapStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.lifecycle.lifecycleScope
import com.example.cahier.ui.brushgraph.model.toBrushFamily
import com.example.cahier.ui.brushgraph.ui.BrushGraphStudio
import com.example.cahier.ui.theme.CahierAppTheme
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import dagger.hilt.android.AndroidEntryPoint

/** Activity that allows a user to design a brush using a node graph based approach. */
@OptIn(ExperimentalInkCustomBrushApi::class)
@AndroidEntryPoint
class BrushGraphActivity : ComponentActivity() {

  companion object {
    const val EXTRA_BRUSH_FAMILY = "extra_brush_family"
    const val EXTRA_BRUSH_FAMILY_RESULT = "extra_brush_family_result"
    const val KEY_AUTO_SAVE_BRUSH = "auto_save_brush"

    /**
     * Mapping of resource names to their corresponding drawable IDs. This is populated by
     * DocumentActivity to avoid binary dependencies between the brushgraph library and the main app
     * target's resources.
     */
    var resourceMapping = mutableMapOf<String, Int>()
    const val KEY_BRUSH_GRAPH_SESSION_ACTIVE = "brush_graph_session_active"
  }

  private lateinit var textureStore: LocalTextureStore
  private val _allTextureIds = MutableStateFlow<Set<String>>(emptySet())
  private val allTextureIds: StateFlow<Set<String>> = _allTextureIds.asStateFlow()

  private lateinit var openTextureResultLauncher: ActivityResultLauncher<Array<String>>
  private lateinit var openBrushResultLauncher: ActivityResultLauncher<Array<String>>

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)

    textureStore = LocalTextureStore(applicationContext)
    _allTextureIds.value = textureStore.getAllIds()

    val encodedBrushFamily = intent.getByteArrayExtra(EXTRA_BRUSH_FAMILY)
    if (encodedBrushFamily != null) {
      lifecycleScope.launch {
        val viewModel: BrushGraphViewModel = viewModels<BrushGraphViewModel>().value
        try {
          val brushFamily = decodeBrushFamily(encodedBrushFamily)
          viewModel.loadBrushFamily(brushFamily)
        } catch (e: Exception) {
          Log.e("BrushGraphActivity", "Failed to decode brush family from intent", e)
        }
      }
    }

    openTextureResultLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
          showTextureNameDialog(uri)
        }
      }

    openBrushResultLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
          lifecycleScope.launch {
            val viewModel: BrushGraphViewModel = viewModels<BrushGraphViewModel>().value
            val result = loadBrushFamilyFromUri(it)
            if (result.isSuccess) {
              viewModel.loadBrushFamily(result.getOrThrow())
            } else {
              val legacyResult = loadLegacyBrushFromUri(it)
              if (legacyResult.isSuccess) {
                viewModel.loadBrushFamily(legacyResult.getOrThrow().family)
              } else {
                Log.e(
                  "BrushGraphActivity",
                  "Failed to load brush or legacy brush",
                  legacyResult.exceptionOrNull(),
                )
              }
            }
          }
        }
      }

    setContent {
      CahierAppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          val viewModel: BrushGraphViewModel = viewModels<BrushGraphViewModel>().value
          val renderer = remember { CanvasStrokeRenderer.create(textureStore) }

          var showColorPicker by remember { mutableStateOf(false) }
          var colorPickerOriginalColor by remember { mutableStateOf(Color.Black) }
          var colorPickerOnNewColor by remember { mutableStateOf({ _: Color -> }) }

          if (showColorPicker) {
            var currentColor by remember { mutableStateOf(colorPickerOriginalColor) }
            AlertDialog(
              onDismissRequest = { showColorPicker = false },
              title = { Text("Pick a color") },
              text = {
                ClassicColorPicker(
                  color = HsvColor.from(colorPickerOriginalColor),
                  onColorChanged = { currentColor = it.toColor() },
                )
              },
              confirmButton = {
                Button(
                  onClick = {
                    colorPickerOnNewColor(currentColor)
                    showColorPicker = false
                  }
                ) {
                  Text("Done")
                }
              },
            )
          }

          LaunchedEffect(viewModel.brush.family) {
            // Debounce to avoid saving on every single slider movement frame.
            delay(500)
            @Suppress("GlobalCoroutineDispatchers")
            val encodedBrushFamily =
              withContext(Dispatchers.IO) {
                ByteArrayOutputStream().use { stream ->
                  viewModel.brush.family.encode(stream, textureStore)
                  stream.toByteArray()
                }
              }
            getSharedPreferences("brush_graph_prefs", Context.MODE_PRIVATE).edit {
              putString(
                KEY_AUTO_SAVE_BRUSH,
                Base64.encodeToString(encodedBrushFamily, Base64.DEFAULT),
              )
            }
          }

          BrushGraphStudio(
            viewModel = viewModel,
            onLoadTexture = { openTextureResultLauncher.launch(arrayOf("image/*")) },
            onLoadBrushFile = { openBrushResultLauncher.launch(arrayOf("*/*")) },
            onChooseColor = { originalColor, onNewColor ->
              colorPickerOriginalColor = originalColor
              colorPickerOnNewColor = onNewColor
              showColorPicker = true
            },
            strokeRenderer = renderer,
            textureStore = textureStore,
            onExport = { openBrushShareDialog(viewModel.brush.family) },
            onSaveToPalette = { /* TODO: Support saving to palette from activity if needed */ },
            onNavigateUp = { finish() },
          )
        }
      }
    }
  }

  override fun finish() {
    setBrushFamilyResult()
    super.finish()
  }

  private fun setBrushFamilyResult() {
    val viewModel: BrushGraphViewModel = viewModels<BrushGraphViewModel>().value
    val encodedBrushFamily =
      ByteArrayOutputStream().use { stream ->
        viewModel.brush.family.encode(stream, textureStore)
        stream.toByteArray()
      }
    val resultIntent = Intent().apply { putExtra(EXTRA_BRUSH_FAMILY_RESULT, encodedBrushFamily) }
    setResult(RESULT_OK, resultIntent)
  }

  @SuppressLint("NewApi")
  private fun openBrushShareDialog(brushFamily: BrushFamily) {
    lifecycleScope.launch {
      val encodedProto =
        ByteArrayOutputStream().use { outputStream ->
          brushFamily.encode(outputStream, textureStore)
          outputStream.toByteArray()
        }

      val fileName =
        "BrushFamily-${SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).format(Date())}.brushfamily"

      val contentValues =
        ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.Files.FileColumns.MIME_TYPE, "application/ink")
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }

      val resolver = applicationContext.contentResolver
      val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

      if (uri != null) {
        resolver.openOutputStream(uri)?.use { outputStream -> outputStream.write(encodedProto) }
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        val shareIntent =
          Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/ink"
            putExtra(Intent.EXTRA_STREAM, uri)
          }
        startActivity(Intent.createChooser(shareIntent, "Share Brush Family"))
      }
    }
  }

  private fun showTextureNameDialog(uri: Uri) {
    val input = EditText(this)
    input.inputType = android.text.InputType.TYPE_CLASS_TEXT
    androidx.appcompat.app.AlertDialog.Builder(this)
      .setTitle("Enter texture name")
      .setView(input)
      .setPositiveButton("OK") { _, _ ->
        val name = input.text.toString()
        if (name.isNotEmpty()) {
          loadTexture(uri, name)
        }
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun loadTexture(uri: Uri, name: String) {
    lifecycleScope.launch {
      val bitmap = loadBitmapFromUri(uri)
      if (bitmap != null) {
        textureStore.addTexture(name, bitmap)
        _allTextureIds.value = textureStore.getAllIds()
      }
    }
  }

  private fun loadBitmapFromUri(uri: Uri): Bitmap? =
    runCatching {
        contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)
        }
      }
      .getOrNull()

  private fun showColorPicker(originalColor: Color, onNewColor: (Color) -> Unit) {
    // This is now handled via state in setContent
  }

  private fun syncTexturesFromProto(proto: ink.proto.BrushFamily) {
    proto.textureIdToBitmapMap.forEach { (id, byteString) ->
      if (textureStore.get(id) == null) {
        val bytes = byteString.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
          textureStore.addTexture(id, bitmap)
        }
      }
    }
  }

  @Suppress("GlobalCoroutineDispatchers")
  private suspend fun loadBrushFamilyFromUri(uri: Uri): Result<BrushFamily> =
    withContext(Dispatchers.IO) {
      return@withContext runCatching {
        contentResolver.openInputStream(uri).use { stream ->
          val proto = ink.proto.BrushFamily.parseFrom(stream!!)
          syncTexturesFromProto(proto)
          proto.toBrushFamily()
        }
      }
    }

  @Suppress("GlobalCoroutineDispatchers")
  private suspend fun loadLegacyBrushFromUri(uri: Uri): Result<Brush> =
    withContext(Dispatchers.IO) {
      return@withContext runCatching {
        contentResolver.openInputStream(uri).use { stream ->
          loadLegacyBrushFromInputStream(stream!!).getOrThrow()
        }
      }
    }

  private fun loadLegacyBrushFromInputStream(inputStream: InputStream): Result<Brush> {
    val proto = ink.proto.BrushFamily.parseFrom(inputStream)
    syncTexturesFromProto(proto)
    return runCatching {
      Brush.createWithColorIntArgb(proto.toBrushFamily(), 0xFF000000.toInt(), size = 10f, epsilon = 0.1f)
    }
  }

  private fun decodeBrushFamily(encoded: ByteArray): BrushFamily {
    val proto = ink.proto.BrushFamily.parseFrom(encoded)
    syncTexturesFromProto(proto)
    return proto.toBrushFamily()
  }

  @OptIn(ExperimentalInkCustomBrushApi::class)
  private class LocalTextureStore(context: Context) : TextureBitmapStore {
    private val resources = context.resources
    private val packageName = "com.google.inputmethod.ink.strokes.demo"
    private val stockTextures = StockTextureBitmapStore(resources)
    private val customTextures = mutableMapOf<String, Bitmap>()
    private val stockBitmapCache = mutableMapOf<String, Bitmap>()
    private val extraStockTextureNames =
      listOf(
        "charcoal",
        "emoji-check",
        "emoji-diamond",
        "emoji-heart",
        "emoji-poop",
        "emoji-star",
        "holly-berry",
        "holly-leaf",
        "music-clef-g",
        "music-note-sixteenth",
        "pencil",
        "fire",
      )

    override fun get(clientTextureId: String): Bitmap? {
      Log.d("BrushGraphDebug", "LocalTextureStore.get(clientTextureId='$clientTextureId')")
      if (stockTextures[clientTextureId] != null) {
        Log.d("BrushGraphDebug", "  Found in stockTextures")
        return stockTextures[clientTextureId]
      }

      val id = clientTextureId.removePrefix("ink://ink").removePrefix("/texture:").removePrefix("/")
      if (customTextures.containsKey(id)) {
        Log.d("BrushGraphDebug", "  Found in customTextures")
        return customTextures[id]
      }
      if (stockBitmapCache.containsKey(id)) {
        Log.d("BrushGraphDebug", "  Found in stockBitmapCache")
        return stockBitmapCache[id]
      }

      val resName =
        when (id) {
          "charcoal" -> "charcoal_000"
          "emoji-check" -> "emoji_check_000"
          "emoji-diamond" -> "emoji_diamond_000"
          "emoji-heart" -> "emoji_heart_000"
          "emoji-poop" -> "emoji_poop_000"
          "emoji-star" -> "emoji_star_000"
          "holly-berry" -> "holly_berry_000"
          "holly-leaf" -> "holly_leaf_000"
          "music-clef-g" -> "music_clef_g_000"
          "music-note-sixteenth" -> "music_note_sixteenth_000"
          "pencil" -> "pencil_000"
          "fire" -> "fire_200x200frame_12x10grid_120frames"
          else -> null
        }

      if (resName != null) {
        val resId = resourceMapping[resName] ?: 0
        if (resId != 0) {
          val bitmap = BitmapFactory.decodeResource(resources, resId)
          if (bitmap != null) {
            Log.d("BrushGraphDebug", "  Successfully decoded resource for id='$id'")
            stockBitmapCache[id] = bitmap
            return bitmap
          }
        }
      }
      Log.e("BrushGraphDebug", "  Texture NOT FOUND for id='$id'")
      return null
    }

    fun addTexture(id: String, bitmap: Bitmap) {
      customTextures[id] = bitmap
    }

    fun getAllIds(): Set<String> {
      return extraStockTextureNames.toSet() + customTextures.keys
    }
  }
}
