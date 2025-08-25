package com.example.cahier.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import com.example.cahier.ui.Converters
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OfflineNotesRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: OfflineNotesRepository
    private val noteDao: NoteDao = mock()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = OfflineNotesRepository(noteDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getNoteStrokes_deserializes_strokes_correctly() = runTest {
        val brush = Brush(StockBrushes.markerLatest, 1f, 1f)
        val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
        val serializedStroke = Converters().serializeStroke(stroke)
        val strokesJson = Gson().toJson(listOf(serializedStroke))
        val note = Note(id = 1, strokesData = strokesJson)

        whenever(noteDao.getNoteById(1L)).thenReturn(note)

        val resultStrokes = repository.getNoteStrokes(1L)

        assertEquals(1, resultStrokes.size)
    }

    @Test
    fun updateNoteStrokes_calls_DAO_with_serialized_strokes() = runTest {
        val noteId = 1L
        val brush = Brush(StockBrushes.markerLatest, 1f, 1f)
        val stroke = Stroke(brush, ImmutableStrokeInputBatch.EMPTY)
        val note = Note(id = noteId, title = "Test")
        whenever(noteDao.getNoteById(noteId)).thenReturn(note)

        repository.updateNoteStrokes(noteId, listOf(stroke))

        verify(noteDao).updateNote(any())
    }

    @Test
    fun toggleFavorite_updates_the_note_correctly() = runTest {
        val note = Note(id = 1L, isFavorite = false)
        whenever(noteDao.getNoteById(1L)).thenReturn(note)

        repository.toggleFavorite(1L)

        verify(noteDao).updateNote(note.copy(isFavorite = true))
    }

    @Test
    fun getAllNotesStream_returns_flow_from_DAO() = runTest {
        val notes = listOf(Note(id = 1), Note(id = 2))
        whenever(noteDao.getAllNotes()).thenReturn(flowOf(notes))

        val result = repository.getAllNotesStream().first()

        assertEquals(2, result.size)
        verify(noteDao).getAllNotes()
    }
}