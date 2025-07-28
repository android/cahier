/*
 *
 *  * Copyright 2025 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.example.cahier.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.compose.copyWithComposeColor
import androidx.ink.brush.compose.createWithComposeColor
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.cahier.data.CahierUiState
import com.example.cahier.data.NotesRepository
import com.example.cahier.navigation.DrawingCanvasDestination
import com.example.cahier.utils.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingCanvasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NotesRepository,
    val fileHelper: FileHelper,
    private val imageLoader: ImageLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(CahierUiState())
    val uiState: StateFlow<CahierUiState> = _uiState.asStateFlow()

    private val noteId: Long = checkNotNull(savedStateHandle[DrawingCanvasDestination.NOTE_ID_ARG])

    private val currentNightMode = AppCompatDelegate.getDefaultNightMode()

    private val _defaultBrush = MutableStateFlow(
        Brush.createWithComposeColor(
            family = StockBrushes.pressurePenLatest,
            color = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES)
                Color.White else Color.Gray,
            size = 5F,
            epsilon = 0.1F
        )
    )

    private val _currentBrush = MutableStateFlow(_defaultBrush.value)
    val currentBrush = _currentBrush.asStateFlow()

    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode.asStateFlow()

    private var previousPoint: MutableVec? = null
    private val eraserPadding = 50f

    private val history = mutableListOf<List<Stroke>>()
    private var historyIndex = -1
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _exportedImageUri = MutableStateFlow<Uri?>(null)
    val exportedImageUri: StateFlow<Uri?> = _exportedImageUri.asStateFlow()


    init {
        viewModelScope.launch {
            noteRepository.getNoteStream(noteId)
                .filterNotNull()
                .collect { note ->
                    val initialStrokes = if (note.strokesData != null) {
                        noteRepository.getNoteStrokes(note.id)
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(note = note, strokes = initialStrokes)
                    }
                    if (history.isEmpty()) {
                        history.clear()
                        history.add(initialStrokes)
                        historyIndex = 0
                        updateUndoRedoState()
                    } else {
                        if (historyIndex >= 0 && historyIndex < history.size) {
                            _uiState.update { it.copy(strokes = history[historyIndex]) }
                        }
                        updateUndoRedoState()
                    }
                }
        }
    }

    fun addImageWithLocalUri(localUri: Uri) {
        val newImageUri = localUri.toString()
        val updatedNote = _uiState.value.note.copy(imageUriList = listOf(newImageUri))
        viewModelScope.launch {
            noteRepository.updateNote(updatedNote)
        }
    }

    private fun updateStrokes(newStrokes: List<Stroke>) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newStrokes)
        historyIndex++

        _uiState.update { it.copy(strokes = newStrokes) }
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < history.size - 1
    }

    fun undo() {
        if (canUndo.value) {
            historyIndex--
            _uiState.update { it.copy(strokes = history[historyIndex]) }
            updateUndoRedoState()
            viewModelScope.launch { saveStrokes() }
        }
    }

    fun redo() {
        if (canRedo.value) {
            historyIndex++
            _uiState.update { it.copy(strokes = history[historyIndex]) }
            updateUndoRedoState()
            viewModelScope.launch { saveStrokes() }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            noteRepository.toggleFavorite(noteId)
        }
    }

    suspend fun processAndAddImage(contentResolver: ContentResolver, uri: Uri?) {
        if (uri == null) return
        val localFileUri = fileHelper.copyUriToInternalStorage(contentResolver, uri)
        addImageWithLocalUri(localFileUri)
    }

    fun replaceImage(contentResolver: ContentResolver, uri: Uri?) {
        viewModelScope.launch {
            processAndAddImage(contentResolver, uri)
        }
    }

    suspend fun createExportedBitmap(contentResolver: ContentResolver, width: Int, height: Int) {
        val backgroundImageUri = _uiState.value.note.imageUriList?.firstOrNull()
        val strokes = _uiState.value.strokes

        val backgroundBitmap = if (backgroundImageUri != null) {
            val request = ImageRequest.Builder(context)
                .data(backgroundImageUri.toUri())
                .allowHardware(false)
                .build()
            imageLoader.execute(request).image?.toBitmap()
        } else {
            null
        }

        val exportBitmap = createBitmap(width, height)
        val canvas = Canvas(exportBitmap)

        val backgroundColor = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
        canvas.drawColor(backgroundColor)

        backgroundBitmap?.let { bmp ->
            val canvasWidth = width.toFloat()
            val canvasHeight = height.toFloat()
            val bmpWidth = bmp.width.toFloat()
            val bmpHeight = bmp.height.toFloat()

            val scaleX = canvasWidth / bmpWidth
            val scaleY = canvasHeight / bmpHeight

            val scale = maxOf(scaleX, scaleY)

            val dx = (canvasWidth - bmpWidth * scale) / 2f
            val dy = (canvasHeight - bmpHeight * scale) / 2f

            val matrix = android.graphics.Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)

            canvas.drawBitmap(bmp, matrix, null)
        }

        val strokeRenderer = CanvasStrokeRenderer.create(forcePathRendering = true)
        strokes.forEach { stroke ->
            strokeRenderer.draw(canvas, stroke, android.graphics.Matrix())
        }
        _exportedImageUri.value = fileHelper.saveBitmapToCache(exportBitmap)
    }

    suspend fun saveStrokes() {
        if (historyIndex >= 0 && historyIndex < history.size) {
            noteRepository.updateNoteStrokes(noteId, history[historyIndex])
        } else if (history.isEmpty()) {
            noteRepository.updateNoteStrokes(noteId, emptyList())
        }
    }

    suspend fun updateNoteTitle(newTitle: String) {
        val updatedNote = _uiState.value.note.copy(title = newTitle)
        noteRepository.updateNote(updatedNote)
    }

    @UiThread
    fun onStrokesFinished(finishedStrokes: List<Stroke>) {
        val currentStrokes = history.getOrElse(historyIndex) { emptyList() }
        val newStrokes = currentStrokes + finishedStrokes
        updateStrokes(newStrokes)
        viewModelScope.launch {
            saveStrokes()
        }
    }

    fun startErase() {
        previousPoint = null
    }

    fun endErase() {
        previousPoint = null
        viewModelScope.launch { saveStrokes() }
    }


    fun erase(x: Float, y: Float) {
        val strokesBeforeErase = history.getOrElse(historyIndex) { emptyList() }
        val strokesAfterErase = eraseIntersectingStrokes(
            x, y, strokesBeforeErase
        )

        if (strokesAfterErase.size != strokesBeforeErase.size) {
            updateStrokes(strokesAfterErase)
        }
    }

    private fun eraseIntersectingStrokes(
        currentX: Float,
        currentY: Float,
        currentStrokes: List<Stroke>,
    ): List<Stroke> {
        val prev = previousPoint
        previousPoint = MutableVec(currentX, currentY)

        if (prev == null) return currentStrokes

        val segment = MutableSegment(prev, MutableVec(currentX, currentY))
        val parallelogram = MutableParallelogram.fromSegmentAndPadding(segment, eraserPadding)

        val strokesToRemove = currentStrokes.filter { stroke ->
            stroke.shape.intersects(parallelogram, AffineTransform.IDENTITY)
        }

        return if (strokesToRemove.isNotEmpty()) {
            currentStrokes - strokesToRemove.toSet()
        } else {
            currentStrokes
        }
    }

    fun changeBrush(brushFamily: BrushFamily, size: Float) {
        _currentBrush.update {
            it.copy(
                family = brushFamily,
                size = size,
            )
        }
    }

    fun changeBrushColor(color: Color) {
        _currentBrush.update {
            it.copyWithComposeColor(color = color)
        }
    }

    fun setEraserMode(enabled: Boolean) {
        _isEraserMode.update { enabled }
    }

    fun clearStrokes() {
        if (_uiState.value.strokes.isNotEmpty()) {
            updateStrokes(emptyList())
        }
    }

    fun clearImages() {
        val updatedNote = _uiState.value.note.copy(imageUriList = emptyList())
        viewModelScope.launch {
            noteRepository.updateNote(updatedNote)
        }
    }

    fun clearScreen() {
        clearStrokes()
        clearImages()
    }


    fun getCurrentBrush(): Brush {
        return _currentBrush.value
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            saveStrokes()
        }
    }

    companion object {
        private const val TAG = "DrawingCanvasViewModel"
    }
}