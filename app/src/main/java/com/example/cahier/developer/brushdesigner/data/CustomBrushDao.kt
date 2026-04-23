package com.example.cahier.ui.brushdesigner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBrushDao {
    @Query("SELECT * FROM custom_brushes WHERE name != '__autosave__'")
    fun getAllCustomBrushes(): Flow<List<CustomBrushEntity>>

    @Query("SELECT * FROM custom_brushes WHERE name = '__autosave__'")
    fun getAutoSaveBrush(): Flow<CustomBrushEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomBrush(brush: CustomBrushEntity)

    @Query("DELETE FROM custom_brushes WHERE name = :name")
    suspend fun deleteCustomBrush(name: String)
}
