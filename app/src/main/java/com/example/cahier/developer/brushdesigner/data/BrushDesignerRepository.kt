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

import androidx.ink.strokes.Stroke
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.BrushTip as ProtoBrushTip
import ink.proto.ColorFunction as ProtoColorFunction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class BrushDesignerRepository @Inject constructor() {

    private val initialProto: ProtoBrushFamily = ProtoBrushFamily.newBuilder()
        .addCoats(
            ProtoBrushCoat.newBuilder()
                .setTip(
                    ProtoBrushTip.newBuilder().setScaleX(1f).setScaleY(1f).setCornerRounding(1f)
                )
                .addPaintPreferences(
                    ProtoBrushPaint.newBuilder()
                        .setSelfOverlap(ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY)
                        .addColorFunctions(
                            ProtoColorFunction.newBuilder()
                                .setOpacityMultiplier(1f)
                        )
                )
        )
        .setInputModel(
            ProtoBrushFamily.InputModel.newBuilder()
                .setSlidingWindowModel(
                    ProtoBrushFamily.SlidingWindowModel.newBuilder()
                        .setWindowSizeSeconds(0.02f)
                        .setExperimentalUpsamplingPeriodSeconds(0.005f)
                )
        )
        .build()

    private val _activeBrushProto = MutableStateFlow(initialProto)
    val activeBrushProto: StateFlow<ProtoBrushFamily> = _activeBrushProto.asStateFlow()

    private val _testStrokes = MutableStateFlow<List<Stroke>>(emptyList())
    val testStrokes: StateFlow<List<Stroke>> = _testStrokes.asStateFlow()

    fun updateActiveBrushProto(proto: ProtoBrushFamily) {
        _activeBrushProto.value = proto
    }

    fun updateTestStrokes(strokes: List<Stroke>) {
        _testStrokes.value = strokes
    }
}
