package com.example.cahier.ui.brushdesigner

import androidx.ink.strokes.Stroke
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushCoat as ProtoBrushCoat
import ink.proto.BrushTip as ProtoBrushTip

@Singleton
class BrushDesignerRepository @Inject constructor() {

    private val initialProto: ProtoBrushFamily = ProtoBrushFamily.newBuilder()
        .addCoats(
            ProtoBrushCoat.newBuilder()
                .setTip(
                    ProtoBrushTip.newBuilder()
                        .setScaleX(1f)
                        .setScaleY(1f)
                        .setCornerRounding(1f)
                )
        )
        .build()

    // Holds the protobuf state across app navigation
    val activeBrushProto = MutableStateFlow(initialProto)

    // Holds the strokes across app navigation
    val testStrokes = MutableStateFlow<List<Stroke>>(emptyList())
}
