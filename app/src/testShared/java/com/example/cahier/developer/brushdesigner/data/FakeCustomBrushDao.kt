package com.example.cahier.developer.brushdesigner.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeCustomBrushDao : CustomBrushDao {
    private val brushes = mutableMapOf<String, CustomBrushEntity>()
    private val autoSaveFlow = MutableStateFlow<CustomBrushEntity?>(null)

    override fun getAllCustomBrushes(autosaveKey: String): Flow<List<CustomBrushEntity>> {
        return flowOf(brushes.values.filter { it.name != autosaveKey })
    }

    override fun getAutoSaveBrush(autosaveKey: String): Flow<CustomBrushEntity?> {
        return autoSaveFlow.asStateFlow()
    }

    override suspend fun saveCustomBrush(brush: CustomBrushEntity) {
        brushes[brush.name] = brush
        if (brush.name == "__autosave__") {
            autoSaveFlow.value = brush
        }
    }

    override suspend fun deleteCustomBrush(name: String) {
        brushes.remove(name)
        if (name == "__autosave__") {
            autoSaveFlow.value = null
        }
    }
}
