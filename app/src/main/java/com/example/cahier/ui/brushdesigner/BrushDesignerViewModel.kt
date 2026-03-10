package com.example.cahier.ui.brushdesigner

import android.content.Context
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction

@OptIn(ExperimentalInkCustomBrushApi::class)
@HiltViewModel
class BrushDesignerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: BrushDesignerRepository
) : ViewModel() {

    val activeBrushProto: StateFlow<ProtoBrushFamily> = repository.activeBrushProto.asStateFlow()
    val testStrokes: StateFlow<List<Stroke>> = repository.testStrokes.asStateFlow()

    private val _brushColor = MutableStateFlow(Color.Black)
    val brushColor: StateFlow<Color> = _brushColor.asStateFlow()

    val previewBrushFamily: StateFlow<BrushFamily?> = repository.activeBrushProto
        .map { proto ->
            try {
                val rawBytes = proto.toByteArray()

                val baos = ByteArrayOutputStream()
                withContext(Dispatchers.IO) {
                    GZIPOutputStream(baos).use { it.write(rawBytes) }
                }
                val gzippedBytes = baos.toByteArray()

                ByteArrayInputStream(gzippedBytes).use { inputStream ->
                    BrushFamily.decode(inputStream)
                }
            } catch (e: Exception) {
                null
            }
        }
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

        inputModelBuilder.setExperimentalNaiveModel(ink.proto.BrushFamily.ExperimentalNaiveModel.getDefaultInstance())

        familyBuilder.setInputModel(inputModelBuilder)
        repository.activeBrushProto.value = familyBuilder.build()
    }

    fun setBrushColor(color: Color) {
        _brushColor.value = color
    }

    fun setBrushSize(size: Float) {
        _brushSize.value = size
    }
}