/*
 *
 *  *
 *  *  * Copyright 2025 Google LLC. All rights reserved.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */


package com.example.cahier.ui

import android.util.Log
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import androidx.room.TypeConverter
import com.example.cahier.data.CustomBrush
import com.example.cahier.data.SerializedBrush
import com.example.cahier.data.SerializedStockBrush
import com.example.cahier.data.SerializedStroke
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private const val TAG = "Converters"

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val stockBrushToEnumValues = mapOf(
            StockBrushes.marker() to SerializedStockBrush.MarkerLatest,
            StockBrushes.pressurePen() to SerializedStockBrush.PressurePenLatest,
            StockBrushes.highlighter() to SerializedStockBrush.HighlighterLatest,
            StockBrushes.dashedLine() to SerializedStockBrush.DashedLineLatest,
        )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MarkerLatest,
            clientBrushFamilyId = brush.family.clientBrushFamilyId
        )
    }

    fun serializeStroke(stroke: Stroke): String {
        val serializedBrush = serializeBrush(stroke.brush)
        val encodedSerializedInputs = ByteArrayOutputStream().use { outputStream ->
            stroke.inputs.encode(outputStream)
            outputStream.toByteArray()
        }

        val serializedStroke = SerializedStroke(
            inputs = encodedSerializedInputs,
            brush = serializedBrush
        )
        return json.encodeToString(serializedStroke)
    }

    private fun deserializeStroke(
        serializedStroke: SerializedStroke,
        customBrushes: List<CustomBrush>
    ): Stroke? {
        val inputs = ByteArrayInputStream(serializedStroke.inputs).use { inputStream ->
            StrokeInputBatch.decode(inputStream)
        }
        val brush = deserializeBrush(serializedStroke.brush, customBrushes)
        return Stroke(brush = brush, inputs = inputs)
    }

    private fun deserializeBrush(
        serializedBrush: SerializedBrush,
        customBrushes: List<CustomBrush>
    ): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush]
        val customBrush = customBrushes.find {
            it.brushFamily.clientBrushFamilyId == serializedBrush.clientBrushFamilyId
        }

        val brushFamily = customBrush?.brushFamily ?: stockBrushFamily ?: StockBrushes.marker()

        return Brush.createWithColorLong(
            family = brushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    fun deserializeStrokeFromString(data: String, customBrushes: List<CustomBrush>): Stroke? {
        val serializedStroke = json.decodeFromString<SerializedStroke>(data)
        return deserializeStroke(serializedStroke, customBrushes)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(jsonString: String?): List<String>? {
        if (jsonString == null) {
            return emptyList()
        }
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error decoding string list from JSON: $jsonString", e
            )
            emptyList()
        }
    }
}