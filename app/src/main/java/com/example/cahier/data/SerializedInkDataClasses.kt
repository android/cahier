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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Parcelize
@Serializable
data class SerializedStroke(
    val inputs: ByteArray,
    val brush: SerializedBrush
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializedStroke

        if (!inputs.contentEquals(other.inputs)) return false
        if (brush != other.brush) return false

        return true
    }

    override fun hashCode(): Int {
        var result = inputs.contentHashCode()
        result = 31 * result + brush.hashCode()
        return result
    }
}

@Parcelize
@Serializable
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush,
    val clientBrushFamilyId: String? = null
) : Parcelable

enum class SerializedStockBrush {
    MarkerLatest,
    PressurePenLatest,
    HighlighterLatest,
    DashedLineLatest,
}