package com.example.cahier.ui.brushdesigner

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_brushes")
data class CustomBrushEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val brushBytes: ByteArray
)
