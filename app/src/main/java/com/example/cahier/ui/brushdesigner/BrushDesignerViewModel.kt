package com.example.cahier.ui.brushdesigner

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
import com.example.cahier.ui.CahierTextureBitmapStore
import com.google.protobuf.ByteString
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
import ink.proto.ColorFunction as ProtoColorFunction

@OptIn(ExperimentalInkCustomBrushApi::class, FlowPreview::class)
@HiltViewModel
class BrushDesignerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: BrushDesignerRepository,
    private val customBrushDao: CustomBrushDao
) : ViewModel() {

    val activeBrushProto: StateFlow<ProtoBrushFamily> = repository.activeBrushProto.asStateFlow()
    val testStrokes: StateFlow<List<Stroke>> = repository.testStrokes.asStateFlow()

    private val _brushColor = MutableStateFlow(Color.Black)
    val brushColor: StateFlow<Color> = _brushColor.asStateFlow()

    val previewBrushFamily: StateFlow<BrushFamily?> = repository.activeBrushProto
        .map { proto ->
            withContext(Dispatchers.IO) {
                try {
                    val rawBytes = proto.toByteArray()
                    val baos = ByteArrayOutputStream()
                    GZIPOutputStream(baos).use { it.write(rawBytes) }

                    ByteArrayInputStream(baos.toByteArray()).use { inputStream ->
                        BrushFamily.decode(inputStream)
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
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val autoSaveFile = File(context.cacheDir, "autosave.brush")

    private var textureStore: CahierTextureBitmapStore? = null

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

        viewModelScope.launch(Dispatchers.IO) {
            repository.activeBrushProto
                .map { it.textureIdToBitmapMap }
                .distinctUntilChanged()
                .collect { map ->
                    syncProtobufTexturesToStore(map)
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

    private fun syncProtobufTexturesToStore(map: Map<String, ByteString>) {
        map.forEach { (id, byteString) ->
            if (textureStore?.get(id) == null) {
                try {
                    val bytes = byteString.toByteArray()
                    val bitmap = BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        textureStore?.loadTexture(id, bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getActiveBrush(): Brush? = activeBrush.value

    fun onStrokesFinished(newStrokes: List<Stroke>) {
        repository.testStrokes.update { it + newStrokes }
    }

    fun replaceStrokes(updatedStrokes: List<Stroke>) {
        repository.testStrokes.value = updatedStrokes
    }

    fun clearCanvas() {
        repository.testStrokes.value = emptyList()
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
                    repository.activeBrushProto.value = loadedProto
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
                stockBrush.encode(baos)

                val gzippedBytes = baos.toByteArray()
                ByteArrayInputStream(gzippedBytes).use { inputStream ->
                    GZIPInputStream(inputStream).use { gzipStream ->
                        val rawBytes = gzipStream.readBytes()
                        val loadedProto = ProtoBrushFamily.parseFrom(rawBytes)
                        repository.activeBrushProto.value = loadedProto
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
        repository.activeBrushProto.value = builder.build()
    }

    fun updateDeveloperComment(comment: String) {
        val builder = repository.activeBrushProto.value.toBuilder()
        builder.developerComment = comment
        repository.activeBrushProto.value = builder.build()
    }

    fun updateTip(updateBlock: (ProtoBrushTip.Builder) -> Unit) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()

        if (familyBuilder.coatsCount == 0) {
            familyBuilder.addCoats(
                ProtoBrushCoat.newBuilder()
                    .setTip(ProtoBrushTip.newBuilder())
                    .addPaintPreferences(ProtoBrushPaint.newBuilder())
            )
        }

        val coatBuilder = familyBuilder.getCoats(0).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()

        updateBlock(tipBuilder)

        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(0, coatBuilder)

        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun updateSelfOverlap(overlap: ProtoBrushPaint.SelfOverlap) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()

        if (familyBuilder.coatsCount == 0) {
            familyBuilder.addCoats(
                ProtoBrushCoat.newBuilder()
                    .setTip(ProtoBrushTip.newBuilder())
            )
        }
        val coatBuilder = familyBuilder.getCoats(0).toBuilder()

        if (coatBuilder.paintPreferencesCount == 0) {
            coatBuilder.addPaintPreferences(ProtoBrushPaint.newBuilder())
        }
        val paintBuilder = coatBuilder.getPaintPreferences(0).toBuilder()

        paintBuilder.selfOverlap = overlap

        coatBuilder.setPaintPreferences(0, paintBuilder)
        familyBuilder.setCoats(0, coatBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun updateOpacityMultiplier(multiplier: Float) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()

        if (familyBuilder.coatsCount == 0) {
            familyBuilder.addCoats(
                ProtoBrushCoat.newBuilder()
                    .setTip(ProtoBrushTip.newBuilder())
            )
        }
        val coatBuilder = familyBuilder.getCoats(0).toBuilder()

        if (coatBuilder.paintPreferencesCount == 0) {
            coatBuilder.addPaintPreferences(ProtoBrushPaint.newBuilder())
        }
        val paintBuilder = coatBuilder.getPaintPreferences(0).toBuilder()

        val colorFuncBuilder = ProtoColorFunction.newBuilder().setOpacityMultiplier(multiplier)
        if (paintBuilder.colorFunctionsCount == 0) {
            paintBuilder.addColorFunctions(colorFuncBuilder)
        } else {
            paintBuilder.setColorFunctions(0, colorFuncBuilder)
        }

        coatBuilder.setPaintPreferences(0, paintBuilder)
        familyBuilder.setCoats(0, coatBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
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

        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun updateInputModelToSpring() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val inputModelBuilder = familyBuilder.inputModel.toBuilder()

        inputModelBuilder.setSpringModel(ink.proto.BrushFamily.SpringModel.getDefaultInstance())

        familyBuilder.setInputModel(inputModelBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun updateInputModelToNaive() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val inputModelBuilder = familyBuilder.inputModel.toBuilder()

        inputModelBuilder.setExperimentalNaiveModel(
            ink.proto.BrushFamily.ExperimentalNaiveModel.getDefaultInstance()
        )

        familyBuilder.setInputModel(inputModelBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun setBrushColor(color: Color) {
        _brushColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }


    fun saveToPalette(brushName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawBytes = repository.activeBrushProto.value.toByteArray()
                val baos = ByteArrayOutputStream()
                GZIPOutputStream(baos).use { it.write(rawBytes) }

                customBrushDao.saveCustomBrush(
                    CustomBrushEntity(name = brushName, brushBytes = baos.toByteArray())
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
                            repository.activeBrushProto.value = loadedProto
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
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@launch

                textureStore?.loadTexture(textureId, bitmap)

                val builder = repository.activeBrushProto.value.toBuilder()

                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                builder.putTextureIdToBitmap(
                    textureId,
                    ByteString.copyFrom(baos.toByteArray())
                )

                if (builder.coatsCount == 0) {
                    builder.addCoats(
                        ProtoBrushCoat
                            .newBuilder().setTip(ProtoBrushTip.newBuilder())
                    )
                }
                val coatBuilder = builder.getCoats(0).toBuilder()

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
                builder.setCoats(0, coatBuilder)

                withContext(Dispatchers.Main) {
                    repository.activeBrushProto.value = builder.build()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addBehavior(nodes: List<ink.proto.BrushBehavior.Node>) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        if (familyBuilder.coatsCount == 0) return
        val coatBuilder = familyBuilder.getCoats(0).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()

        val behavior = ink.proto.BrushBehavior.newBuilder()
            .addAllNodes(nodes)
            .build()

        tipBuilder.addBehaviors(behavior)

        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(0, coatBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun clearBehaviors() {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        val coatBuilder = familyBuilder.getCoats(0).toBuilder()
        val tipBuilder = coatBuilder.tip.toBuilder()
        tipBuilder.clearBehaviors()
        coatBuilder.setTip(tipBuilder)
        familyBuilder.setCoats(0, coatBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun updateTextureLayer(updateBlock: (ProtoBrushPaint.TextureLayer.Builder) -> Unit) {
        val familyBuilder = repository.activeBrushProto.value.toBuilder()
        if (familyBuilder.coatsCount == 0) return
        val coatBuilder = familyBuilder.getCoats(0).toBuilder()

        if (coatBuilder.paintPreferencesCount == 0) {
            coatBuilder.addPaintPreferences(ProtoBrushPaint.newBuilder())
        }
        val paintBuilder = coatBuilder.getPaintPreferences(0).toBuilder()

        val textureLayerBuilder = if (paintBuilder.textureLayersCount == 0) {
            ProtoBrushPaint.TextureLayer.newBuilder()
                .setSizeUnit(ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE)
                .setSizeX(1.0f)
                .setSizeY(1.0f)
        } else {
            paintBuilder.getTextureLayers(0).toBuilder()
        }

        updateBlock(textureLayerBuilder)

        if (!textureLayerBuilder.hasSizeUnit() ||
            textureLayerBuilder.sizeUnit ==
            ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_UNSPECIFIED
        ) {
            textureLayerBuilder.setSizeUnit(
                ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE
            )
        }

        if (paintBuilder.textureLayersCount == 0) {
            paintBuilder.addTextureLayers(textureLayerBuilder)
        } else {
            paintBuilder.setTextureLayers(0, textureLayerBuilder)
        }

        coatBuilder.setPaintPreferences(0, paintBuilder)
        familyBuilder.setCoats(0, coatBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun setTextureStore(store: CahierTextureBitmapStore) {
        this.textureStore = store
    }
}