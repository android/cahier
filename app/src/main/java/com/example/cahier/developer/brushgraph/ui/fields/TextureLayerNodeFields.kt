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

package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.ui.BlendModePreviewWidget
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.ui.TextureLayerPreviewWidget
import com.example.cahier.developer.brushgraph.ui.TextureWrapPreviewWidget
import com.example.cahier.developer.brushgraph.ui.getTooltip
import ink.proto.BrushPaint as ProtoBrushPaint

@Composable
fun TextureLayerNodeFields(
  layer: ProtoBrushPaint.TextureLayer,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  onUpdate: (NodeData.TextureLayer) -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
  modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
            EnumDropdown(
                label = stringResource(R.string.bg_texture_id),
                currentValue = layer.clientTextureId,
                values = allTextureIds.toList(),
                modifier = Modifier.weight(1f),
                displayName = { it },
                onSelected = { id ->
                    onUpdate(
                        NodeData.TextureLayer(
                            layer.toBuilder().setClientTextureId(id).build()
                        )
                    )
                }
            )
            IconButton(onClick = onLoadTexture, enabled = true) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = stringResource(R.string.bg_cd_upload_texture)
                )
            }
        }

        TextureLayerPreviewWidget(textureLayer = layer, renderer = strokeRenderer)

        InspectorSectionHeader(
            stringResource(R.string.bg_section_mapping),
            stringResource(R.string.bg_section_mapping_sub)
        )

        FieldWithTooltip(
            tooltipTitle = stringResource(
                R.string.bg_label_mapping_mode_with_value,
                stringResource(layer.mapping.displayStringRId())
            ),
            tooltipText = stringResource(layer.mapping.getTooltip()),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            EnumDropdown(
                label = stringResource(R.string.bg_mapping_mode),
                currentValue = layer.mapping,
                values = listOf(
                    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING,
                    ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING,
                ),
                displayName = { stringResource(it.displayStringRId()) },
                onSelected = { mapping ->
                    onUpdate(NodeData.TextureLayer(layer.toBuilder().setMapping(mapping).build()))
                }
            )
        }

        if (layer.mapping == ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING) {
            NumericField(
                title = stringResource(R.string.bg_label_size_x),
                value = layer.sizeX,
                limits = NumericLimits(0.1f, 1000f, 0.1f),
                onValueChanged = {
                    onUpdate(
                        NodeData.TextureLayer(
                            layer.toBuilder().setSizeX(it).build()
                        )
                    )
                }
            )
            NumericField(
                title = stringResource(R.string.bg_label_size_y),
                value = layer.sizeY,
                limits = NumericLimits(0.1f, 1000f, 0.1f),
                onValueChanged = {
                    onUpdate(
                        NodeData.TextureLayer(
                            layer.toBuilder().setSizeY(it).build()
                        )
                    )
                }
            )
            FieldWithTooltip(
                tooltipTitle = stringResource(
                    R.string.bg_label_size_unit_with_value,
                    stringResource(layer.sizeUnit.displayStringRId())
                ),
                tooltipText = stringResource(layer.sizeUnit.getTooltip()),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                EnumDropdown(
                    label = stringResource(R.string.bg_size_unit),
                    currentValue = layer.sizeUnit,
                    values = listOf(
                        ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE,
                        ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES,
                    ),
                    displayName = { stringResource(it.displayStringRId()) },
                    onSelected = { unit ->
                        onUpdate(NodeData.TextureLayer(layer.toBuilder().setSizeUnit(unit).build()))
                    }
                )
            }
        }

        InspectorSectionHeader(
            stringResource(R.string.bg_section_positioning),
            stringResource(R.string.bg_section_positioning_sub)
        )

        FieldWithTooltip(
            tooltipTitle = stringResource(
                R.string.bg_label_origin_with_value,
                stringResource(layer.origin.displayStringRId())
            ),
            tooltipText = stringResource(layer.origin.getTooltip()),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            EnumDropdown(
                label = stringResource(R.string.bg_origin),
                currentValue = layer.origin,
                values = listOf(
                    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN,
                    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT,
                    ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT,
                ),
                displayName = { stringResource(it.displayStringRId()) },
                onSelected = { origin ->
                    onUpdate(NodeData.TextureLayer(layer.toBuilder().setOrigin(origin).build()))
                }
            )
        }
        NumericField(
            title = stringResource(R.string.bg_label_offset_x),
            value = layer.offsetX,
            limits = NumericLimits(-1f, 1f, 0.01f),
            onValueChanged = {
                onUpdate(
                    NodeData.TextureLayer(
                        layer.toBuilder().setOffsetX(it).build()
                    )
                )
            }
        )
        NumericField(
            title = stringResource(R.string.bg_label_offset_y),
            value = layer.offsetY,
            limits = NumericLimits(-1f, 1f, 0.01f),
            onValueChanged = {
                onUpdate(
                    NodeData.TextureLayer(
                        layer.toBuilder().setOffsetY(it).build()
                    )
                )
            }
        )
        NumericField(
            title = stringResource(R.string.bg_label_rotation_degrees),
            value = layer.rotationInRadians,
            limits = NumericLimits.radiansShownAsDegrees(0f, 360f),
            onValueChanged = {
                onUpdate(
                    NodeData.TextureLayer(
                        layer.toBuilder().setRotationInRadians(it).build()
                    )
                )
            }
        )

        InspectorSectionHeader(
            stringResource(R.string.bg_section_wrapping),
            stringResource(R.string.bg_section_wrapping_sub)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                FieldWithTooltip(
                    tooltipTitle = stringResource(
                        R.string.bg_label_wrap_x_with_value,
                        stringResource(layer.wrapX.displayStringRId())
                    ),
                    tooltipText = stringResource(layer.wrapX.getTooltip()),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    EnumDropdown(
                        label = stringResource(R.string.bg_wrap_x),
                        currentValue = layer.wrapX,
                        values = listOf(
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
                        ),
                        displayName = { stringResource(it.displayStringRId()) },
                        onSelected = { wrap ->
                            onUpdate(
                                NodeData.TextureLayer(
                                    layer.toBuilder().setWrapX(wrap).build()
                                )
                            )
                        }
                    )
                }
                FieldWithTooltip(
                    tooltipTitle = stringResource(
                        R.string.bg_label_wrap_y_with_value,
                        stringResource(layer.wrapY.displayStringRId())
                    ),
                    tooltipText = stringResource(layer.wrapY.getTooltip()),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    EnumDropdown(
                        label = stringResource(R.string.bg_wrap_y),
                        currentValue = layer.wrapY,
                        values = listOf(
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
                            ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
                        ),
                        displayName = { stringResource(it.displayStringRId()) },
                        onSelected = { wrap ->
                            onUpdate(
                                NodeData.TextureLayer(
                                    layer.toBuilder().setWrapY(wrap).build()
                                )
                            )
                        }
                    )
                }
            }
            Box(modifier = Modifier.padding(start = 8.dp)) {
                TextureWrapPreviewWidget(
                    wrapX = layer.wrapX,
                    wrapY = layer.wrapY,
                    renderer = strokeRenderer,
                    clientTextureId = layer.clientTextureId
                )
            }
        }

        InspectorSectionHeader(
            stringResource(R.string.bg_section_blending),
            stringResource(R.string.bg_section_blending_sub)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                FieldWithTooltip(
                    tooltipTitle = stringResource(
                        R.string.bg_label_blend_mode_with_value,
                        stringResource(layer.blendMode.displayStringRId())
                    ),
                    tooltipText = stringResource(layer.blendMode.getTooltip()),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    EnumDropdown(
                        label = stringResource(R.string.bg_blend_mode),
                        currentValue = layer.blendMode,
                        values = listOf(
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OVER,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_MODULATE,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OVER,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_IN,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_IN,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_OUT,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_OUT,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_SRC_ATOP,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_DST_ATOP,
                            ProtoBrushPaint.TextureLayer.BlendMode.BLEND_MODE_XOR,
                        ),
                        displayName = { stringResource(it.displayStringRId()) },
                        onSelected = { mode ->
                            onUpdate(
                                NodeData.TextureLayer(
                                    layer.toBuilder().setBlendMode(mode).build()
                                )
                            )
                        }
                    )
                }
            }
            Box(modifier = Modifier.padding(start = 8.dp)) {
                BlendModePreviewWidget(
                    blendMode = layer.blendMode,
                    renderer = strokeRenderer,
                    clientTextureId = layer.clientTextureId
                )
            }
        }

    }
}

@Composable
private fun InspectorSectionHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
