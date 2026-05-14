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

package com.example.cahier.developer.brushdesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

public const val AUTOSAVE_KEY = "__autosave__"

@Dao
interface CustomBrushDao {
    @Query("SELECT * FROM custom_brushes WHERE name != :autosaveKey")
    fun getAllCustomBrushes(autosaveKey: String = AUTOSAVE_KEY): Flow<List<CustomBrushEntity>>

    @Query("SELECT * FROM custom_brushes WHERE name = :autosaveKey")
    fun getAutoSaveBrush(autosaveKey: String = AUTOSAVE_KEY): Flow<CustomBrushEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomBrush(brush: CustomBrushEntity)

    @Query("DELETE FROM custom_brushes WHERE name = :name")
    suspend fun deleteCustomBrush(name: String)
}
