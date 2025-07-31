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


package com.example.cahier.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * A replacement for [androidx.compose.ui.input.pointer] that allows pointer events to fall
 * through to sibling composables instead of just ancestor composables. This just delegates to the
 * normal implementation, but has
 * [androidx.compose.ui.node.PointerInputModifierNode.sharePointerInputWithSiblings] return true.
 */
internal fun Modifier.pointerInputWithSiblingFallthrough(
    pointerInputEventHandler: PointerInputEventHandler
) = this then PointerInputSiblingFallthroughElement(pointerInputEventHandler)

private class PointerInputSiblingFallthroughModifierNode(
    pointerInputEventHandler: PointerInputEventHandler
) : PointerInputModifierNode, DelegatingNode() {

    var pointerInputEventHandler: PointerInputEventHandler
        get() = delegateNode.pointerInputEventHandler
        set(value) {
            delegateNode.pointerInputEventHandler = value
        }

    val delegateNode = delegate(
        SuspendingPointerInputModifierNode(pointerInputEventHandler)
    )

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        delegateNode.onPointerEvent(pointerEvent, pass, bounds)
    }

    override fun onCancelPointerInput() {
        delegateNode.onCancelPointerInput()
    }

    override fun sharePointerInputWithSiblings() = true
}

private data class PointerInputSiblingFallthroughElement(
    val pointerInputEventHandler: PointerInputEventHandler
) : ModifierNodeElement<PointerInputSiblingFallthroughModifierNode>() {

    override fun create() = PointerInputSiblingFallthroughModifierNode(pointerInputEventHandler)

    override fun update(node: PointerInputSiblingFallthroughModifierNode) {
        node.pointerInputEventHandler = pointerInputEventHandler
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pointerInputWithSiblingFallthrough"
        properties["pointerInputEventHandler"] = pointerInputEventHandler
    }
}