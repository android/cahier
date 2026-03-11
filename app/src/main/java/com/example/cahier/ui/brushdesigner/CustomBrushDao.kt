package com.example.cahier.ui.brushdesigner

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomBrushDao {
    @Query("SELECT * FROM custom_brushes")
    fun getAllCustomBrushes(): Flow<List<CustomBrushEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomBrush(brush: CustomBrushEntity)

    @Query("DELETE FROM custom_brushes WHERE name = :name")
    suspend fun deleteCustomBrush(name: String)
}