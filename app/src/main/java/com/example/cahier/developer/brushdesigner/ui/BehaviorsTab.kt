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

package com.example.cahier.developer.brushdesigner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.viewmodel.PrefabBehaviors
import ink.proto.BrushBehavior
import ink.proto.BrushFamily as ProtoBrushFamily

/**
 * Tab 2: Dynamics & Behaviors controls — editable behavior stack with nested
 * node graph editor, standard dynamics presets, and advanced dynamics presets.
 *
 * Uses [EditableListWidget] for managing behaviors (outer list) and nodes
 * within each behavior (inner nested list), with [NodeEditor] for
 * type-specific node editing.
 *
 * Stateless: receives data and callbacks, does not access ViewModel.
 */
@Composable
internal fun BehaviorsTabContent(
    activeProto: ProtoBrushFamily,
    selectedCoatIndex: Int,
    onUpdateBehaviors: (List<BrushBehavior>) -> Unit,
    onAddBehavior: (List<BrushBehavior.Node>) -> Unit,
) {
    val behaviors = activeProto
        .coatsList.getOrNull(selectedCoatIndex)?.tip?.behaviorsList
        ?: emptyList()

    Text(
        stringResource(R.string.brush_designer_dynamics_behaviors),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )

    EditableListWidget(
        title = stringResource(R.string.brush_designer_behavior_stack),
        items = behaviors,
        defaultItem = BrushBehavior.newBuilder()
            .addNodes(
                BrushBehavior.Node.newBuilder().setSourceNode(
                    BrushBehavior.SourceNode.newBuilder()
                        .setSource(BrushBehavior.Source.SOURCE_NORMALIZED_PRESSURE)
                        .setSourceValueRangeStart(0f)
                        .setSourceValueRangeEnd(1f)
                        .setSourceOutOfRangeBehavior(
                            BrushBehavior.OutOfRange.OUT_OF_RANGE_CLAMP
                        )
                )
            )
            .addNodes(
                BrushBehavior.Node.newBuilder().setTargetNode(
                    BrushBehavior.TargetNode.newBuilder()
                        .setTarget(BrushBehavior.Target.TARGET_SIZE_MULTIPLIER)
                        .setTargetModifierRangeStart(0.5f)
                        .setTargetModifierRangeEnd(1.5f)
                )
            )
            .build(),
        onItemsChanged = onUpdateBehaviors,
        itemHeader = { behavior ->
            val source = behavior.nodesList
                .find { it.hasSourceNode() }
                ?.sourceNode?.source?.name?.replace("SOURCE_", "")
                ?: "INPUT"
            val target = behavior.nodesList
                .find { it.hasTargetNode() }
                ?.targetNode?.target?.name?.replace("TARGET_", "")
                ?: "OUTPUT"
            "$source ➔ $target"
        },
        editorContent = { behavior, onBehaviorChanged ->
            BehaviorNodeGraphEditor(
                behavior = behavior,
                onBehaviorChanged = onBehaviorChanged
            )
        }
    )

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    StandardDynamicsSection(onAddBehavior = onAddBehavior)

    Spacer(modifier = Modifier.height(16.dp))

    AdvancedDynamicsSection(
        onAddBehavior = onAddBehavior
    )
}

/**
 * Nested node graph editor for a single [BrushBehavior].
 * Uses a nested [EditableListWidget] to manage the ordered list of nodes.
 */
@Composable
private fun BehaviorNodeGraphEditor(
    behavior: BrushBehavior,
    onBehaviorChanged: (BrushBehavior) -> Unit,
) {
    Text(
        stringResource(R.string.brush_designer_nodes_in_behavior),
        style = MaterialTheme.typography.labelLarge
    )

    EditableListWidget(
        title = "",
        items = behavior.nodesList,
        defaultItem = BrushBehavior.Node.newBuilder()
            .setConstantNode(
                BrushBehavior.ConstantNode.newBuilder().setValue(1f)
            )
            .build(),
        onItemsChanged = { newNodes ->
            onBehaviorChanged(
                behavior.toBuilder().clearNodes().addAllNodes(newNodes).build()
            )
        },
        itemHeader = { node ->
            node.nodeCase.name.replace("_NODE", "")
        },
        editorContent = { node, onNodeChanged ->
            NodeEditor(node = node, onNodeChanged = onNodeChanged)
        }
    )
}

/**
 * Standard un-damped dynamics presets using [PrefabBehaviors] simple variants.
 */
@Composable
private fun StandardDynamicsSection(
    onAddBehavior: (List<BrushBehavior.Node>) -> Unit,
) {
    Text(
        stringResource(R.string.brush_designer_standard_dynamics),
        style = MaterialTheme.typography.labelLarge
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = {
                onAddBehavior(PrefabBehaviors.simplePressureToSize())
            }) { Text(stringResource(R.string.brush_designer_pressure_size)) }

            Button(modifier = Modifier.weight(1f), onClick = {
                onAddBehavior(PrefabBehaviors.simpleSpeedToSize())
            }) { Text(stringResource(R.string.brush_designer_speed_size)) }
        }

        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            onAddBehavior(PrefabBehaviors.simpleSpeedToOpacity())
        }) { Text(stringResource(R.string.brush_designer_speed_opacity)) }
    }
}

/**
 * Advanced smoothed/jitter presets from [PrefabBehaviors].
 * All 17 prefabs are exposed here organized by category.
 */
@Composable
private fun AdvancedDynamicsSection(
    onAddBehavior: (List<BrushBehavior.Node>) -> Unit,
) {
    Text(
        stringResource(R.string.brush_designer_advanced_dynamics),
        style = MaterialTheme.typography.labelLarge
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // ── Pressure (smoothed) ──
        PrefabButton(
            label = stringResource(R.string.brush_designer_smooth_pressure_size),
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.pressureToSize()) }
        )
        PrefabButton(
            label = "Pressure → Width",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.pressureToWidth()) }
        )
        PrefabButton(
            label = "Pressure → Opacity",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.pressureToOpacity()) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Tilt (smoothed) ──
        PrefabButton(
            label = "Tilt → Width",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.tiltToWidth()) }
        )
        PrefabButton(
            label = "Tilt → Slant",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.tiltToSlant()) }
        )
        PrefabButton(
            label = "Tilt → Opacity",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.tiltToOpacity()) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Speed (smoothed) ──
        PrefabButton(
            label = stringResource(R.string.brush_designer_smooth_speed_opacity),
            iconRes = R.drawable.opacity_24px,
            onClick = { onAddBehavior(PrefabBehaviors.speedToOpacity()) }
        )
        PrefabButton(
            label = "Speed → Size (Smoothed)",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.speedToSize()) }
        )
        PrefabButton(
            label = "Speed → Width",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.speedToWidth()) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Direction ──
        PrefabButton(
            label = "Direction → Rotation",
            iconRes = R.drawable.brush_24px,
            onClick = { onAddBehavior(PrefabBehaviors.directionToRotation()) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Distance / Time ──
        PrefabButton(
            label = "Distance → Opacity Fade",
            iconRes = R.drawable.opacity_24px,
            onClick = { onAddBehavior(PrefabBehaviors.distanceToOpacityFade()) }
        )
        PrefabButton(
            label = "Time → Opacity Fade",
            iconRes = R.drawable.opacity_24px,
            onClick = { onAddBehavior(PrefabBehaviors.timeToOpacityFade()) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Jitter ──
        PrefabButton(
            label = stringResource(R.string.brush_designer_pencil_jitter),
            iconRes = R.drawable.texture_24px,
            onClick = { onAddBehavior(PrefabBehaviors.slantJitter()) }
        )
        PrefabButton(
            label = "Width Jitter",
            iconRes = R.drawable.texture_24px,
            onClick = { onAddBehavior(PrefabBehaviors.widthJitter()) }
        )
        PrefabButton(
            label = "Opacity Jitter",
            iconRes = R.drawable.texture_24px,
            onClick = { onAddBehavior(PrefabBehaviors.opacityJitter()) }
        )
        PrefabButton(
            label = "Hue Jitter",
            iconRes = R.drawable.texture_24px,
            onClick = { onAddBehavior(PrefabBehaviors.hueJitter()) }
        )
        PrefabButton(
            label = "Position Jitter",
            iconRes = R.drawable.texture_24px,
            onClick = { onAddBehavior(PrefabBehaviors.positionJitter()) }
        )
    }
}

/** Reusable button for advanced dynamics presets. */
@Composable
private fun PrefabButton(
    label: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Icon(
            painterResource(iconRes),
            null,
            Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
