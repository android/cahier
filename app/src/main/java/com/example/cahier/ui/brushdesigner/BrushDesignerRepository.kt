package com.example.cahier.ui.brushdesigner

import androidx.ink.strokes.Stroke
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushTip as ProtoBrushTip
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class BrushDesignerRepository @Inject constructor() {

    private val initialProto: ProtoBrushFamily = ProtoBrushFamily.newBuilder()
        .addCoats(
            ProtoBrushCoat.newBuilder()
                .setTip(
                    ProtoBrushTip.newBuilder().setScaleX(1f).setScaleY(1f).setCornerRounding(1f)
                )
                .addPaintPreferences(
                    ink.proto.BrushPaint.newBuilder()
                        .setSelfOverlap(ink.proto.BrushPaint.SelfOverlap.SELF_OVERLAP_ANY)
                        .addColorFunctions(
                            ink.proto.ColorFunction.newBuilder()
                                .setOpacityMultiplier(1f)
                        )
                )
        )
        .setInputModel(
            ink.proto.BrushFamily.InputModel.newBuilder()
                .setSlidingWindowModel(
                    ink.proto.BrushFamily.SlidingWindowModel.newBuilder()
                        .setWindowSizeSeconds(0.02f)
                        .setExperimentalUpsamplingPeriodSeconds(0.005f)
                )
        )
        .build()

    val activeBrushProto = MutableStateFlow(initialProto)
    val testStrokes = MutableStateFlow<List<Stroke>>(emptyList())
}