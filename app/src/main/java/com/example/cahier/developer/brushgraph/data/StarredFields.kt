/*
 * Copyright 2026 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cahier.developer.brushgraph.data

import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import ink.proto.BrushBehavior as ProtoBrushBehavior

/** Enum representing specific starrable numeric fields. */
enum class StarredFieldType(val id: Int, val displayNameRes: Int) {
    SOURCE_RANGE_START(1, R.string.bg_label_range_start),
    SOURCE_RANGE_END(2, R.string.bg_label_range_end),
    NOISE_SEED(3, R.string.bg_label_seed),
    NOISE_BASE_PERIOD(4, R.string.bg_label_base_period);
    
    companion object {
        fun fromId(id: Int): StarredFieldType? = values().find { it.id == id }
    }
}

/** Data class to represent a starred field instance. */
data class StarredField(
    val nodeId: String,
    val fieldType: StarredFieldType
)

/** Get the current value of a starred field from NodeData. */
fun NodeData.getNumericFieldValue(fieldType: StarredFieldType): Float {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START -> node.sourceNode.sourceValueRangeStart
                StarredFieldType.SOURCE_RANGE_END -> node.sourceNode.sourceValueRangeEnd
                StarredFieldType.NOISE_SEED -> node.noiseNode.seed.toFloat()
                StarredFieldType.NOISE_BASE_PERIOD -> node.noiseNode.basePeriod
            }
        }
        else -> 0f
    }
}

/** Get the limits of a starred field from NodeData. */
fun NodeData.getNumericFieldLimits(fieldType: StarredFieldType): NumericLimits {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START -> node.sourceNode.source.getNumericLimits()
                StarredFieldType.SOURCE_RANGE_END -> node.sourceNode.source.getNumericLimits()
                StarredFieldType.NOISE_SEED -> NumericLimits.standard(0f, 100f, 1f)
                StarredFieldType.NOISE_BASE_PERIOD -> node.noiseNode.varyOver.getNumericLimits(ProgressDomainContext.NOISE)
            }
        }
        else -> NumericLimits.standard(0f, 1f, 0.01f)
    }
}

/** Update NodeData with a new value for a starred field. */
fun NodeData.updateWithNumericFieldValue(fieldType: StarredFieldType, value: Float): NodeData {
    return when (this) {
        is NodeData.Behavior -> {
            val node = this.node
            val updatedNode = when (fieldType) {
                StarredFieldType.SOURCE_RANGE_START -> 
                    node.toBuilder().setSourceNode(node.sourceNode.toBuilder().setSourceValueRangeStart(value).build()).build()
                StarredFieldType.SOURCE_RANGE_END -> 
                    node.toBuilder().setSourceNode(node.sourceNode.toBuilder().setSourceValueRangeEnd(value).build()).build()
                StarredFieldType.NOISE_SEED -> 
                    node.toBuilder().setNoiseNode(node.noiseNode.toBuilder().setSeed(value.toInt()).build()).build()
                StarredFieldType.NOISE_BASE_PERIOD -> 
                    node.toBuilder().setNoiseNode(node.noiseNode.toBuilder().setBasePeriod(value).build()).build()
            }
            this.copy(node = updatedNode)
        }
        else -> this
    }
}
