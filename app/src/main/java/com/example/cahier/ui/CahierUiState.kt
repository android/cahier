package com.example.cahier.ui

import androidx.ink.strokes.Stroke
import com.example.cahier.data.Note

data class CahierUiState(
    val note: Note = Note(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val strokes: List<Stroke> = listOf()
)