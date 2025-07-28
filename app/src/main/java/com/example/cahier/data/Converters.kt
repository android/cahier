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

package com.example.cahier.data

import android.util.Log
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type

private const val TAG = "Converters"

class Converters {

    private val gson: Gson = GsonBuilder().create()


    companion object {
        private val stockBrushToEnumValues = mapOf(
            StockBrushes.markerLatest to SerializedStockBrush.MARKER_LATEST,
            StockBrushes.pressurePenLatest to SerializedStockBrush.PRESSURE_PEN_LATEST,
            StockBrushes.highlighterLatest to SerializedStockBrush.HIGHLIGHTER_LATEST,
            StockBrushes.dashedLineLatest to SerializedStockBrush.DASHED_LINE_LATEST,
        )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MARKER_LATEST,
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
        return gson.toJson(serializedStroke)
    }

    private fun deserializeStroke(serializedStroke: SerializedStroke): Stroke? {
        val inputs = ByteArrayInputStream(serializedStroke.inputs).use { inputStream ->
            StrokeInputBatch.decode(inputStream)
        }
        val brush = deserializeBrush(serializedStroke.brush)
        return Stroke(brush = brush, inputs = inputs)
    }

    private fun deserializeBrush(serializedBrush: SerializedBrush): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush] ?: StockBrushes.markerV1

        return Brush.createWithColorLong(
            family = stockBrushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    fun deserializeStrokeFromString(data: String): Stroke? {
        val serializedStroke = gson.fromJson(data, SerializedStroke::class.java)
        return deserializeStroke(serializedStroke)
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(jsonString: String?): List<String>? {
        if (jsonString == null) {
            return emptyList()
        }
        val listType: Type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error decoding string list from JSON: $jsonString", e
            )
            emptyList()
        }
    }
}