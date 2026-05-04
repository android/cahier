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

import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import ink.proto.BrushFamily as ProtoBrushFamily
import ink.proto.BrushPaint as ProtoBrushPaint
import ink.proto.ColorFunction as ProtoColorFunction

/**
 * Tab 1: Paint & texture controls — multi-layer textures, multi-function colors,
 * self overlap, and texture import.
 *
 * Uses [EditableListWidget] for managing multiple texture layers and color
 * functions with add/remove/duplicate/toggle support.
 *
 * Stateless: receives data and callbacks, does not access ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaintTabContent(
    activeProto: ProtoBrushFamily,
    selectedCoatIndex: Int,
    onUpdatePaintPreferences: (List<ProtoBrushPaint>) -> Unit,
    onUpdateSelfOverlap: (ProtoBrushPaint.SelfOverlap) -> Unit,
    texturePickerLauncher: androidx.activity.result.ActivityResultLauncher<
            androidx.activity.result.PickVisualMediaRequest>,
    getTextureBitmap: (String) -> Bitmap?
) {
    val paintPrefs =
        activeProto.coatsList.getOrNull(selectedCoatIndex)?.paintPreferencesList
            ?: emptyList()

    val availableTextures = activeProto.textureIdToBitmapMap.keys.toList()

    Text(
        text = stringResource(R.string.brush_designer_paint_texture),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )

    EditableListWidget(
        title = stringResource(R.string.brush_designer_paint_preferences),
        items = paintPrefs,
        defaultItem = ProtoBrushPaint.newBuilder()
            .addTextureLayers(
                ProtoBrushPaint.TextureLayer.newBuilder()
                    .setSizeUnit(ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE)
                    .setSizeX(1f).setSizeY(1f)
                    .setMapping(ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING)
                    .setBlendMode(ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER)
            )
            .build(),
        onItemsChanged = onUpdatePaintPreferences,
        itemHeader = { paint ->
            val texCount = paint.textureLayersCount
            val colorCount = paint.colorFunctionsCount
            "$texCount texture(s), $colorCount color fn(s)"
        },
        editorContent = { paint, onPaintChanged ->
            PaintPreferenceEditor(
                paint = paint,
                availableTextures = availableTextures,
                onPaintChanged = onPaintChanged,
                getTextureBitmap = getTextureBitmap
            )
        }
    )

    Spacer(modifier = Modifier.height(16.dp))

    SelfOverlapSelector(
        currentPaint = paintPrefs.firstOrNull() ?: ProtoBrushPaint.getDefaultInstance(),
        onOverlapSelected = onUpdateSelfOverlap
    )

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()

    TextureImportSection(
        textureCount = activeProto.textureIdToBitmapMap.size,
        texturePickerLauncher = texturePickerLauncher
    )
}

/**
 * Editor for a single paint preference, containing its own texture layers
 * and color functions via nested [EditableListWidget].
 */
@Composable
private fun PaintPreferenceEditor(
    paint: ProtoBrushPaint,
    availableTextures: List<String>,
    onPaintChanged: (ProtoBrushPaint) -> Unit,
    getTextureBitmap: (String) -> Bitmap?
) {
    EditableListWidget(
        title = stringResource(R.string.brush_designer_texture_layers),
        items = paint.textureLayersList,
        defaultItem = ProtoBrushPaint.TextureLayer.newBuilder()
            .setSizeUnit(ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE)
            .setSizeX(1f).setSizeY(1f)
            .setMapping(ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING)
            .setBlendMode(ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER)
            .build(),
        onItemsChanged = { newLayers ->
            onPaintChanged(
                paint.toBuilder().clearTextureLayers()
                    .also { b -> newLayers.forEach { b.addTextureLayers(it) } }
                    .build()
            )
        },
        itemHeader = { layer ->
            val texId =
                layer.clientTextureId.ifEmpty { stringResource(R.string.brush_designer_empty) }
            val blend = layer.blendMode.name.replace("BLEND_MODE_", "")
            "$texId ($blend)"
        },
        editorContent = { layer, onLayerChanged ->
            TextureLayerEditor(
                layer = layer,
                availableTextures = availableTextures,
                onLayerChanged = onLayerChanged,
                getTextureBitmap = getTextureBitmap
            )
        }
    )

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(8.dp))

    if (paint.colorFunctionsList.isEmpty()) {
        Text(
            text = stringResource(R.string.brush_designer_no_color_functions),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
    EditableListWidget(
        title = stringResource(R.string.brush_designer_color_functions),
        items = paint.colorFunctionsList,
        defaultItem = ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build(),
        onItemsChanged = { newFuncs ->
            onPaintChanged(
                paint.toBuilder().clearColorFunctions()
                    .also { b -> newFuncs.forEach { b.addColorFunctions(it) } }
                    .build()
            )
        },
        itemHeader = { func ->
            when (func.functionCase) {
                ProtoColorFunction.FunctionCase.OPACITY_MULTIPLIER ->
                    stringResource(
                        R.string.brush_designer_opacity_multiplier,
                        func.opacityMultiplier
                    )

                ProtoColorFunction.FunctionCase.REPLACE_COLOR ->
                    stringResource(R.string.brush_designer_replace_color)

                else -> stringResource(R.string.brush_designer_unknown)
            }
        },
        editorContent = { func, onFuncChanged ->
            ColorFunctionEditor(
                colorFunction = func,
                onFunctionChanged = onFuncChanged
            )
        }
    )
}

/**
 * Full editor for a single [ProtoBrushPaint.TextureLayer], including all
 * fields from SSA: texture ID, mapping, size unit, scale, rotation,
 * origin, offset, wrap, blend mode, and animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextureLayerEditor(
    layer: ProtoBrushPaint.TextureLayer,
    availableTextures: List<String>,
    onLayerChanged: (ProtoBrushPaint.TextureLayer) -> Unit,
    getTextureBitmap: (String) -> Bitmap?
) {
    val bitmap = if (layer.clientTextureId.isNotEmpty()) {
        getTextureBitmap(layer.clientTextureId)
    } else null
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(
                id = R.string.brush_designer_texture_content_desc,
                layer.clientTextureId
            ),
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 8.dp)
        )
    }

    TextureIdSelector(
        currentId = layer.clientTextureId,
        availableTextures = availableTextures,
        onTextureSelected = { id ->
            onLayerChanged(layer.toBuilder().setClientTextureId(id).build())
        }
    )

    EnumDropdown(
        label = stringResource(R.string.brush_designer_mapping_mode),
        currentValue = layer.mapping,
        values = ProtoBrushPaint.TextureLayer.Mapping.entries.filter {
            it != ProtoBrushPaint.TextureLayer.Mapping.MAPPING_UNSPECIFIED
        },
        displayName = { it.name.replace("MAPPING_", "") },
        onSelected = { onLayerChanged(layer.toBuilder().setMapping(it).build()) }
    )

    if (layer.mapping == ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING) {
        EnumDropdown(
            label = stringResource(R.string.brush_designer_size_unit),
            currentValue = layer.sizeUnit,
            values = ProtoBrushPaint.TextureLayer.SizeUnit.entries.filter {
                it != ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_UNSPECIFIED
            },
            displayName = { it.name.replace("SIZE_UNIT_", "") },
            onSelected = { onLayerChanged(layer.toBuilder().setSizeUnit(it).build()) }
        )

        NumericField(
            title = stringResource(R.string.brush_designer_scale_x),
            value = if (layer.hasSizeX()) layer.sizeX else 1f,
            limits = NumericLimits(0.1f, 10f, 0.1f),
            onValueChanged = { onLayerChanged(layer.toBuilder().setSizeX(it).build()) }
        )
        NumericField(
            title = stringResource(R.string.brush_designer_scale_y),
            value = if (layer.hasSizeY()) layer.sizeY else 1f,
            limits = NumericLimits(0.1f, 10f, 0.1f),
            onValueChanged = { onLayerChanged(layer.toBuilder().setSizeY(it).build()) }
        )
    }

    NumericField(
        title = stringResource(R.string.brush_designer_rotation),
        value = if (layer.hasRotationInRadians()) layer.rotationInRadians else 0f,
        limits = NumericLimits.radiansShownAsDegrees(0f, 360f),
        onValueChanged = {
            onLayerChanged(layer.toBuilder().setRotationInRadians(it).build())
        }
    )

    EnumDropdown(
        label = stringResource(R.string.brush_designer_origin),
        currentValue = layer.origin,
        values = ProtoBrushPaint.TextureLayer.Origin.entries.toList(),
        displayName = { it.name.replace("ORIGIN_", "") },
        onSelected = { onLayerChanged(layer.toBuilder().setOrigin(it).build()) }
    )

    NumericField(
        title = stringResource(R.string.brush_designer_offset_x),
        value = if (layer.hasOffsetX()) layer.offsetX else 0f,
        limits = NumericLimits(-5f, 5f, 0.1f),
        onValueChanged = { onLayerChanged(layer.toBuilder().setOffsetX(it).build()) }
    )
    NumericField(
        title = stringResource(R.string.brush_designer_offset_y),
        value = if (layer.hasOffsetY()) layer.offsetY else 0f,
        limits = NumericLimits(-5f, 5f, 0.1f),
        onValueChanged = { onLayerChanged(layer.toBuilder().setOffsetY(it).build()) }
    )

    EnumDropdown(
        label = stringResource(R.string.brush_designer_wrap_x),
        currentValue = layer.wrapX,
        values = ProtoBrushPaint.TextureLayer.Wrap.entries.toList(),
        displayName = { it.name.replace("WRAP_", "") },
        onSelected = { onLayerChanged(layer.toBuilder().setWrapX(it).build()) }
    )
    EnumDropdown(
        label = stringResource(R.string.brush_designer_wrap_y),
        currentValue = layer.wrapY,
        values = ProtoBrushPaint.TextureLayer.Wrap.entries.toList(),
        displayName = { it.name.replace("WRAP_", "") },
        onSelected = { onLayerChanged(layer.toBuilder().setWrapY(it).build()) }
    )

    EnumDropdown(
        label = stringResource(R.string.brush_designer_blend_mode),
        currentValue = layer.blendMode,
        values = ProtoBrushPaint.TextureLayer.BlendMode.entries.filter {
            it != ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_UNSPECIFIED
        },
        displayName = { it.name.replace("BLEND_MODE_", "") },
        onSelected = { onLayerChanged(layer.toBuilder().setBlendMode(it).build()) }
    )
    Text(
        text = blendModeDescription(layer.blendMode),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )

    if (layer.mapping == ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.brush_designer_animation),
            style = MaterialTheme.typography.labelLarge
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                NumericField(
                    title = stringResource(R.string.brush_designer_rows),
                    value = if (layer.hasAnimationRows()) layer.animationRows.toFloat() else 1f,
                    limits = NumericLimits(1f, 10f, 1f),
                    onValueChanged = {
                        onLayerChanged(
                            layer.toBuilder().setAnimationRows(it.toInt()).build()
                        )
                    }
                )
                NumericField(
                    title = stringResource(R.string.brush_designer_columns),
                    value = if (layer.hasAnimationColumns()) layer.animationColumns.toFloat()
                    else 1f,
                    limits = NumericLimits(1f, 10f, 1f),
                    onValueChanged = {
                        onLayerChanged(
                            layer.toBuilder().setAnimationColumns(it.toInt()).build()
                        )
                    }
                )
                NumericField(
                    title = stringResource(R.string.brush_designer_frames),
                    value = if (layer.hasAnimationFrames()) layer.animationFrames.toFloat()
                    else 1f,
                    limits = NumericLimits(1f, 64f, 1f),
                    onValueChanged = {
                        onLayerChanged(
                            layer.toBuilder().setAnimationFrames(it.toInt()).build()
                        )
                    }
                )
                NumericField(
                    title = stringResource(R.string.brush_designer_duration_seconds),
                    value = if (layer.hasAnimationDurationSeconds())
                        layer.animationDurationSeconds else 0f,
                    limits = NumericLimits(0f, 5f, 0.1f),
                    onValueChanged = {
                        onLayerChanged(
                            layer.toBuilder().setAnimationDurationSeconds(it).build()
                        )
                    }
                )
            }
        }
    }
}

/**
 * Editor for a single [ProtoColorFunction], supporting OpacityMultiplier
 * and ReplaceColor function types.
 */
@Composable
private fun ColorFunctionEditor(
    colorFunction: ProtoColorFunction,
    onFunctionChanged: (ProtoColorFunction) -> Unit
) {
    val functionTypes = listOf("Opacity Multiplier", "Replace Color")
    val currentType = when (colorFunction.functionCase) {
        ProtoColorFunction.FunctionCase.REPLACE_COLOR -> 1
        else -> 0
    }

    EnumDropdown(
        label = stringResource(R.string.brush_designer_function_type),
        currentValue = functionTypes[currentType],
        values = functionTypes,
        displayName = { it },
        onSelected = { selected ->
            when (functionTypes.indexOf(selected)) {
                0 -> onFunctionChanged(
                    ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()
                )

                1 -> onFunctionChanged(
                    ProtoColorFunction.newBuilder()
                        .setReplaceColor(
                            ink.proto.Color.getDefaultInstance()
                        )
                        .build()
                )
            }
        }
    )

    when (colorFunction.functionCase) {
        ProtoColorFunction.FunctionCase.OPACITY_MULTIPLIER -> {
            NumericField(
                title = stringResource(R.string.brush_designer_opacity_multiplier_label),
                value = colorFunction.opacityMultiplier,
                limits = NumericLimits(0f, 2f, 0.05f),
                onValueChanged = {
                    onFunctionChanged(
                        ProtoColorFunction.newBuilder().setOpacityMultiplier(it).build()
                    )
                }
            )
        }

        ProtoColorFunction.FunctionCase.REPLACE_COLOR -> {
            Text(
                text = stringResource(R.string.brush_designer_replace_color_message),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        else -> {
            Text(
                text = stringResource(R.string.brush_designer_unknown_color_function),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * A generic [ExposedDropdownMenuBox] for selecting from enum-like value lists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> EnumDropdown(
    label: String,
    currentValue: T,
    values: List<T>,
    displayName: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayName(currentValue),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(displayName(value)) },
                    onClick = {
                        onSelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextureIdSelector(
    currentId: String,
    availableTextures: List<String>,
    onTextureSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentId.ifEmpty { stringResource(R.string.brush_designer_no_texture) },
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.brush_designer_texture_id)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableTextures.forEach { id ->
                DropdownMenuItem(
                    text = { Text(id) },
                    onClick = {
                        onTextureSelected(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelfOverlapSelector(
    currentPaint: ProtoBrushPaint,
    onOverlapSelected: (ProtoBrushPaint.SelfOverlap) -> Unit
) {
    var overlapExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = overlapExpanded,
        onExpandedChange = { overlapExpanded = it }
    ) {
        OutlinedTextField(
            value = currentPaint.selfOverlap.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.brush_designer_self_overlap)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = overlapExpanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = overlapExpanded,
            onDismissRequest = { overlapExpanded = false }
        ) {
            ProtoBrushPaint.SelfOverlap.entries
                .filter { it != ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_UNSPECIFIED }
                .forEach { overlap ->
                    DropdownMenuItem(
                        text = { Text(overlap.name) },
                        onClick = {
                            onOverlapSelected(overlap)
                            overlapExpanded = false
                        }
                    )
                }
        }
    }
}

@Composable
private fun TextureImportSection(
    textureCount: Int,
    texturePickerLauncher: androidx.activity.result.ActivityResultLauncher<
            androidx.activity.result.PickVisualMediaRequest>
) {
    Text(
        text = stringResource(R.string.brush_designer_textures),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
    Button(
        onClick = {
            texturePickerLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.brush_designer_import_texture))
    }

    Text(
        stringResource(R.string.brush_designer_loaded_textures, textureCount),
        style = MaterialTheme.typography.bodySmall
    )
}

/** Returns a short human-readable description for each blend mode. */
private fun blendModeDescription(mode: ProtoBrushPaint.TextureLayer.BlendMode): String =
    when (mode) {
        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER ->
            "Default — texture drawn over destination."

        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN ->
            "Keeps destination where texture is opaque."

        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT ->
            "Cuts out destination where texture is opaque."

        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP ->
            "Draws texture only where destination exists."

        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN ->
            "Draws texture only where destination is opaque."

        ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE ->
            "Multiplies source and destination colors."

        else -> ""
    }
