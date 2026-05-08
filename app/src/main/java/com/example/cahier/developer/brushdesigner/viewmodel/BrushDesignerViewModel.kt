/*
 * Copyright 2025 Google LLC. All rights reserved.
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

package com.example.cahier.developer.brushdesigner.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.core.ui.theme.BrushBlack
import com.example.cahier.developer.brushdesigner.data.BrushDesignerRepository
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import com.example.cahier.developer.brushdesigner.data.CustomBrushEntity
import com.google.protobuf.ByteString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ink.proto.BrushBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip

@OptIn(ExperimentalInkCustomBrushApi::class, FlowPreview::class)
@HiltViewModel
class BrushDesignerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: BrushDesignerRepository,
    private val textureStore: CahierTextureBitmapStore,
    private val customBrushDao: CustomBrushDao
) : ViewModel() {

    val activeBrushProto: StateFlow<ProtoBrushFamily> = repository.activeBrushProto

    val testStrokes: StateFlow<List<Stroke>> = repository.testStrokes

    private val _brushColor = MutableStateFlow(BrushBlack)
    val brushColor: StateFlow<Color> = _brushColor.asStateFlow()

    val previewBrushFamily: StateFlow<BrushFamily?> = repository.activeBrushProto
        .debounce(150)
        .map { proto ->
            withContext(Dispatchers.IO) {
                try {
                    val rawBytes = proto.toByteArray()
                    val baos = ByteArrayOutputStream()
                    GZIPOutputStream(baos).use { it.write(rawBytes) }

                    ByteArrayInputStream(baos.toByteArray()).use { inputStream ->
                        BrushFamily.decode(inputStream) { textureId, bitmap ->
                            bitmap?.let { textureStore.loadTexture(textureId, it) }
                            textureId
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _brushSize = MutableStateFlow(15f)
    val brushSize: StateFlow<Float> = _brushSize.asStateFlow()

    val activeBrush: StateFlow<Brush?> = combine(
        previewBrushFamily,
        _brushColor,
        _brushSize
    ) { family, color, size ->
        if (family == null) null
        else Brush.createWithComposeColor(
            family = family,
            color = color,
            size = size,
            epsilon = 0.1f
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val autoSaveFile = File(context.cacheDir, "autosave.brush")

    init {
        if (autoSaveFile.exists()) {
            loadBrushFromFile(Uri.fromFile(autoSaveFile))
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.activeBrushProto
                .debounce(1000L)
                .collect { proto ->
                    try {
                        autoSaveFile.outputStream().use { outputStream ->
                            GZIPOutputStream(outputStream).use { gzip ->
                                gzip.write(proto.toByteArray())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
        }
    }

    val savedPaletteBrushes: StateFlow<List<CustomBrushEntity>> =
        customBrushDao.getAllCustomBrushes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _selectedCoatIndex = MutableStateFlow(0)
    val selectedCoatIndex: StateFlow<Int> = _selectedCoatIndex.asStateFlow()


    fun getActiveBrush(): Brush? = activeBrush.value

    fun onStrokesFinished(newStrokes: List<Stroke>) {
        repository.updateTestStrokes(repository.testStrokes.value + newStrokes)
    }

    fun replaceStrokes(updatedStrokes: List<Stroke>) {
        repository.updateTestStrokes(updatedStrokes)
    }

    fun clearCanvas() {
        repository.updateTestStrokes(emptyList())
    }


    fun saveBrushToFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    GZIPOutputStream(outputStream).use { gzipStream ->
                        gzipStream.write(repository.activeBrushProto.value.toByteArray())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadBrushFromFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        GZIPInputStream(inputStream).use { gzipStream ->
                            gzipStream.readBytes()
                        }
                    }
                }
                if (bytes != null) {
                    val loadedProto = ProtoBrushFamily.parseFrom(bytes)
                    repository.updateActiveBrushProto(loadedProto)
                    clearCanvas()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadStockBrush(stockBrush: BrushFamily) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baos = ByteArrayOutputStream()
                stockBrush.encode(baos, textureStore)

                val gzippedBytes = baos.toByteArray()
                ByteArrayInputStream(gzippedBytes).use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        val rawBytes = gzipStream.readBytes()
                        val loadedProto = ProtoBrushFamily.parseFrom(rawBytes)
                        repository.updateActiveBrushProto(loadedProto)
                        clearCanvas()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateClientBrushFamilyId(id: String) {
        val builder = repository.activeBrushProto.value.toBuilder()
        builder.clientBrushFamilyId = id
        repository.updateActiveBrushProto(builder.build())
    }

    fun updateDeveloperComment(comment: String) {
        val builder = repository.activeBrushProto.value.toBuilder()
        builder.developerComment = comment
        repository.updateActiveBrushProto(builder.build())
    }

    fun updateTip(updateBlock: (ProtoBrushTip.Builder) -> Unit) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val index = _selectedCoatIndex.value

        val coatBuilder = familyBuilder.getCoats(index).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()
        updateBlock(tipBuilder)

        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(index, coatBuilder)
        repository.updateActiveBrushProto(familyBuilder.build())
    }

    fun updateSelfOverlap(overlap: ProtoBrushPaint.SelfOverlap) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val index = _selectedCoatIndex.value

        if (familyBuilder.coatsCount <= index) {
            familyBuilder.addCoats(
                ProtoBrushCoat.newBuilder()
                    .setTip(ProtoBrushTip.newBuilder())
            )
        }
        val coatBuilder = familyBuilder.getCoats(index).toBuilder()

        if (coatBuilder.paintPreferencesCount == 0) {
            coatBuilder.addPaintPreferences(ProtoBrushPaint.newBuilder())
        }
        val paintBuilder = coatBuilder.getPaintPreferences(0).toBuilder()

        paintBuilder.selfOverlap = overlap

        coatBuilder.setPaintPreferences(0, paintBuilder)
        familyBuilder.setCoats(index, coatBuilder)
        repository.updateActiveBrushProto(familyBuilder.build())
    }

    fun updateSlidingWindowModel(windowMillis: Long, upsamplingHz: Int) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()

        val inputModelBuilder = familyBuilder.inputModel.toBuilder()

        val swBuilder = inputModelBuilder.slidingWindowModel.toBuilder()

        swBuilder.setWindowSizeSeconds(windowMillis / 1000f)

        if (upsamplingHz <= 0) {
            swBuilder.setExperimentalUpsamplingPeriodSeconds(Float.POSITIVE_INFINITY)
        } else {
            swBuilder.setExperimentalUpsamplingPeriodSeconds(1f / upsamplingHz.toFloat())
        }

        inputModelBuilder.setSlidingWindowModel(swBuilder)
        familyBuilder.setInputModel(inputModelBuilder)

        repository.updateActiveBrushProto(familyBuilder.build())
    }

    fun updateInputModelToPassthrough() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val inputModelBuilder = familyBuilder.inputModel.toBuilder()

        inputModelBuilder.setPassthroughModel(
            ink.proto.BrushFamily.PassthroughModel.getDefaultInstance()
        )

        familyBuilder.setInputModel(inputModelBuilder)
        repository.updateActiveBrushProto(familyBuilder.build())
    }

    fun setBrushColor(color: Color) {
        _brushColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }

    fun saveToPalette(brushName: String): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBytes = repository.activeBrushProto.value.toByteArray()
                val baos = ByteArrayOutputStream()

                GZIPOutputStream(baos).use { gzip ->
                    gzip.write(rawBytes)
                }

                val finalCompressedBytes = baos.toByteArray()

                customBrushDao.saveCustomBrush(
                    CustomBrushEntity(
                        name = brushName,
                        brushBytes = finalCompressedBytes
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFromPalette(entity: CustomBrushEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ByteArrayInputStream(entity.brushBytes).use { bais ->
                    GZIPInputStream(bais).use { gzip ->
                        val rawBytes = gzip.readBytes()
                        val loadedProto = ProtoBrushFamily.parseFrom(rawBytes)

                        withContext(Dispatchers.Main) {
                            repository.updateActiveBrushProto(loadedProto)
                            clearCanvas()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addCustomTexture(uri: Uri, textureId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: return@launch

                textureStore.loadTexture(textureId, bitmap)

                val builder = repository.activeBrushProto.value.toBuilder()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                builder.putTextureIdToBitmap(
                    textureId,
                    ByteString.copyFrom(baos.toByteArray())
                )

                val index = _selectedCoatIndex.value
                if (builder.coatsCount <= index) {
                    builder.addCoats(
                        ProtoBrushCoat
                            .newBuilder().setTip(ProtoBrushTip.newBuilder())
                    )
                }
                val coatBuilder = builder.getCoats(index).toBuilder()

                if (coatBuilder.paintPreferencesCount == 0) {
                    coatBuilder.addPaintPreferences(ProtoBrushPaint.newBuilder())
                }
                val paintBuilder = coatBuilder.getPaintPreferences(0).toBuilder()

                val textureLayer = ProtoBrushPaint.TextureLayer.newBuilder()
                    .setClientTextureId(textureId)
                    .setMapping(ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING)
                    .setBlendMode(ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER)
                    .setSizeUnit(ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE)
                    .setSizeX(1.0f)
                    .setSizeY(1.0f)
                    .build()

                paintBuilder.clearTextureLayers()
                paintBuilder.addTextureLayers(textureLayer)

                coatBuilder.setPaintPreferences(0, paintBuilder)
                builder.setCoats(index, coatBuilder)

                withContext(Dispatchers.Main) {
                    repository.updateActiveBrushProto(builder.build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addBehavior(nodes: List<BrushBehavior.Node>) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val index = selectedCoatIndex.value

        if (familyBuilder.coatsCount <= index) return

        val coatBuilder = familyBuilder.getCoats(index).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()

        val behavior = BrushBehavior.newBuilder()
            .addAllNodes(nodes)
            .build()

        tipBuilder.addBehaviors(behavior)

        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(index, coatBuilder)

        repository.updateActiveBrushProto(familyBuilder.build())
    }

    fun clearBehaviors() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val index = _selectedCoatIndex.value
        val coatBuilder = familyBuilder.getCoats(index).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()
        tipBuilder.clearBehaviors()
        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(index, coatBuilder)
        repository.updateActiveBrushProto(familyBuilder.build())
    }

    /**
     * Replaces all paint preferences for the current coat.
     * Used by [EditableListWidget] for multi-paint editing.
     */
    fun updatePaintPreferences(paints: List<ProtoBrushPaint>) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val index = _selectedCoatIndex.value
        if (familyBuilder.coatsCount <= index) return

        val coatBuilder = familyBuilder.getCoats(index).toBuilder()
        coatBuilder.clearPaintPreferences()
        paints.forEach { coatBuilder.addPaintPreferences(it) }
        familyBuilder.setCoats(index, coatBuilder)
        repository.updateActiveBrushProto(familyBuilder.build())
    }

    /**
     * Replaces all behaviors for the current coat's tip.
     * Used by [EditableListWidget] for behavior graph editing.
     */
    fun updateBehaviorsList(behaviors: List<BrushBehavior>) {
        updateTip { tipBuilder ->
            tipBuilder.clearBehaviors()
            behaviors.forEach { tipBuilder.addBehaviors(it) }
        }
    }

    /** Returns the loaded bitmap for a texture ID, or null if not loaded. */
    fun getTextureBitmap(textureId: String): Bitmap? = textureStore.get(textureId)

    fun setSelectedCoat(index: Int) {
        _selectedCoatIndex.value = index
    }

    fun addNewCoat() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()

        val newCoat = ProtoBrushCoat.newBuilder()
            .setTip(ProtoBrushTip.newBuilder().setScaleX(1f).setScaleY(1f).setCornerRounding(1f))
            .addPaintPreferences(ProtoBrushPaint.newBuilder().build())
            .build()

        familyBuilder.addCoats(newCoat)
        repository.updateActiveBrushProto(familyBuilder.build())

        _selectedCoatIndex.value = familyBuilder.coatsCount - 1
    }

    fun deleteSelectedCoat() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        if (familyBuilder.coatsCount <= 1) return

        val indexToRemove = _selectedCoatIndex.value
        familyBuilder.removeCoats(indexToRemove)

        repository.updateActiveBrushProto(familyBuilder.build())

        _selectedCoatIndex.value = (indexToRemove - 1).coerceAtLeast(0)
    }

    fun deleteFromPalette(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            customBrushDao.deleteCustomBrush(name)
        }
    }

    companion object {
        private const val TAG = "BrushDesignerViewModel"
    }
}
