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

import android.util.Log
import android.view.MotionEvent
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.StockBrushes
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cahier.ui.CahierUiState
import com.example.cahier.data.NotesRepository
import com.example.cahier.navigation.DrawingCanvasDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DrawingCanvasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CahierUiState())
    val uiState: StateFlow<CahierUiState> = _uiState.asStateFlow()

    private val noteId: Long = checkNotNull(savedStateHandle[DrawingCanvasDestination.NOTE_ID_ARG])

    val currentNightMode = AppCompatDelegate.getDefaultNightMode()

    val defaultBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePenLatest,
        colorIntArgb = if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES)
            Color.White.toArgb() else Color.Gray.toArgb(),
        size = 5F,
        epsilon = 0.1F
    )

    private val _selectedBrush = MutableStateFlow<Brush>(defaultBrush)
    val selectedBrush: StateFlow<Brush> = _selectedBrush.asStateFlow()

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

    private fun updateStrokes(newStrokes: List<Stroke>) {
        if (historyIndex < history.size - 1) {
            history.subList(historyIndex + 1, history.size).clear()
        }
        history.add(newStrokes)
        historyIndex++

        _uiState.update { it.copy(strokes = newStrokes) }
        updateUndoRedoState()
        viewModelScope.launch { saveStrokes() }
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

    suspend fun updateImageUri(uri: String?) {
        if (uri == null) return

        var updatedList: List<String>? = null

        _uiState.update { currentState ->
            val currentList = currentState.note.imageUriList ?: emptyList()
            val newList = currentList + uri
            updatedList = newList
            val updatedNote = currentState.note.copy(imageUriList = newList)
            currentState.copy(note = updatedNote)
        }

        updatedList?.let { listToSave ->
            try {
                noteRepository.updateNoteImageUriList(noteId = noteId, imageUriList = listToSave)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save updated image list for note $noteId", e)
            }
        }
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
        _uiState.value = _uiState.value.copy(note = updatedNote)
    }

    fun setInProgressStrokesFinishedListener(
        inProgressStrokesView: InProgressStrokesView,
        listener: InProgressStrokesFinishedListener
    ) {
        inProgressStrokesView.addFinishedStrokesListener(listener)
    }

    fun removeInProgressStrokesFinishedListener(
        inProgressStrokesView: InProgressStrokesView,
        listener: InProgressStrokesFinishedListener
    ) {
        inProgressStrokesView.removeFinishedStrokesListener(listener)
    }

    fun handleDrawing(
        event: MotionEvent,
        inProgressStrokesView: InProgressStrokesView,
        pointerIdToStrokeId: MutableMap<Int, InProgressStrokeId>,
        predictor: MotionEventPredictor,
    ) {

        if (_isEraserMode.value) {
            if (event.actionMasked == MotionEvent.ACTION_MOVE
                || event.actionMasked == MotionEvent.ACTION_DOWN
            ) {

                val strokesBeforeErase = history.getOrElse(historyIndex) { emptyList() }
                val strokesAfterErase = eraseIntersectingStrokes(
                    event.x, event.y, strokesBeforeErase
                )

                if (strokesAfterErase.size != strokesBeforeErase.size) {
                    updateStrokes(strokesAfterErase)
                }
            }
        } else {
            predictor.record(event)
            val predictedEvent = predictor.predict()
            try {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val pointerIndex = event.actionIndex
                        pointerIdToStrokeId[event.getPointerId(pointerIndex)] =
                            inProgressStrokesView.startStroke(
                                event = event,
                                pointerId = event.getPointerId(pointerIndex),
                                brush = _selectedBrush.value
                            )
                    }

                    MotionEvent.ACTION_MOVE -> {
                        for (pointerIndex in 0 until event.pointerCount) {
                            val pointerId = event.getPointerId(pointerIndex)
                            val currentStrokeId = pointerIdToStrokeId[pointerId] ?: continue
                            inProgressStrokesView.addToStroke(
                                event,
                                pointerId,
                                currentStrokeId,
                                predictedEvent
                            )
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        val currentStrokeId =
                            pointerIdToStrokeId.remove(pointerId) ?: return
                        inProgressStrokesView.finishStroke(event, pointerId, currentStrokeId)
                        inProgressStrokesView.postInvalidate()
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        val currentStrokeId = pointerIdToStrokeId.remove(pointerId)
                            ?: return
                        inProgressStrokesView.cancelStroke(currentStrokeId, event)
                    }
                }
            } finally {
                predictedEvent?.recycle()
            }
            if (previousPoint != null) previousPoint = null
        }
    }

    @UiThread
    fun onStrokesFinished(
        strokes: Map<InProgressStrokeId, Stroke>,
        inProgressStrokesView: InProgressStrokesView
    ) {
        inProgressStrokesView.postOnAnimation {
            inProgressStrokesView.removeFinishedStrokes(strokes.keys)
        }
        val currentStrokes = history.getOrElse(historyIndex) { emptyList() }
        val newStrokes = currentStrokes + strokes.values.toList()
        updateStrokes(newStrokes)
        viewModelScope.launch { saveStrokes() }
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
        _selectedBrush.update { currentBrush ->
            currentBrush.copy(
                family = brushFamily,
                size = size
            )
        }
    }

    fun changeBrushColor(color: Color) {
        _selectedBrush.update {
            it.copyWithColorIntArgb(
                colorIntArgb = color.toArgb()
            )
        }
    }

    fun setEraserMode(enabled: Boolean) {
        _isEraserMode.update { enabled }
        if (!enabled) {
            previousPoint = null
        } else {
            previousPoint = null
        }
    }

    suspend fun clearStrokes() {
        if (_uiState.value.strokes.isNotEmpty()) {
            updateStrokes(emptyList())
        }
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
