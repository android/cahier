@file:OptIn(androidx.ink.brush.ExperimentalInkCustomBrushApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.example.cahier.ui.brushdesigner.BrushSliderControl
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.displayStringRId
import com.example.cahier.ui.brushgraph.model.safeCopy
import ink.proto.BrushPaint as ProtoBrushPaint

@Composable
fun TextureLayerInspector(
  layer: ProtoBrushPaint.TextureLayer,
  allTextureIds: Set<String>,
  onLoadTexture: () -> Unit,
  onUpdate: (NodeData.TextureLayer) -> Unit,
  strokeRenderer: CanvasStrokeRenderer,
) {
  Column {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 8.dp)) {
      Box(modifier = Modifier.weight(1f)) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it }
        ) {
          OutlinedTextField(
            value = layer.clientTextureId,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.bg_texture_id)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            allTextureIds.forEach { id ->
              DropdownMenuItem(
                text = { Text(id) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(clientTextureId = id)))
                  expanded = false
                }
              )
            }
          }
        }
      }
      IconButton(onClick = onLoadTexture, enabled = true) {
        Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.bg_cd_upload_texture))
      }
    }

    TextureLayerPreviewWidget(textureLayer = layer, renderer = strokeRenderer)

    InspectorSectionHeader(stringResource(R.string.bg_section_mapping), stringResource(R.string.bg_section_mapping_sub))

    var expandedMapping by remember { mutableStateOf(false) }
    var showMappingTooltip by remember { mutableStateOf(false) }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
      ExposedDropdownMenuBox(
        expanded = expandedMapping,
        onExpandedChange = { expandedMapping = it },
        modifier = Modifier.weight(1f)
      ) {
        OutlinedTextField(
          value = stringResource(layer.mapping.displayStringRId()),
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.bg_mapping_mode)) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMapping) },
          modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
          expanded = expandedMapping,
          onDismissRequest = { expandedMapping = false }
        ) {
          arrayOf(
            ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING,
            ProtoBrushPaint.TextureLayer.Mapping.MAPPING_STAMPING,
          ).forEach { mapping ->
            DropdownMenuItem(
              text = { Text(stringResource(mapping.displayStringRId())) },
              onClick = {
                onUpdate(NodeData.TextureLayer(layer.safeCopy(mapping = mapping)))
                expandedMapping = false
              }
            )
          }
        }
      }
      IconButton(onClick = { showMappingTooltip = true }) {
        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
      }
    }
    if (showMappingTooltip) {
      TooltipDialog(
        title = stringResource(R.string.bg_label_mapping_mode_with_value, stringResource(layer.mapping.displayStringRId())),
        text = stringResource(layer.mapping.getTooltip()),
        onDismiss = { showMappingTooltip = false }
      )
    }

    if (layer.mapping == ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING) {
      BrushSliderControl(
        label = stringResource(R.string.bg_label_size_x),
        value = layer.sizeX,
        valueRange = 0.1f..1000f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeX = it))) }
      )
      BrushSliderControl(
        label = stringResource(R.string.bg_label_size_y),
        value = layer.sizeY,
        valueRange = 0.1f..1000f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeY = it))) }
      )
      var expandedUnit by remember { mutableStateOf(false) }
      var showUnitTooltip by remember { mutableStateOf(false) }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
      ) {
        ExposedDropdownMenuBox(
          expanded = expandedUnit,
          onExpandedChange = { expandedUnit = it },
          modifier = Modifier.weight(1f)
        ) {
          OutlinedTextField(
            value = stringResource(layer.sizeUnit.displayStringRId()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.bg_size_unit)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expandedUnit,
            onDismissRequest = { expandedUnit = false }
          ) {
            arrayOf(
              ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_BRUSH_SIZE,
              ProtoBrushPaint.TextureLayer.SizeUnit.SIZE_UNIT_STROKE_COORDINATES,
            ).forEach { unit ->
              DropdownMenuItem(
                text = { Text(stringResource(unit.displayStringRId())) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeUnit = unit)))
                  expandedUnit = false
                }
              )
            }
          }
        }
        IconButton(onClick = { showUnitTooltip = true }) {
          Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
        }
      }
      if (showUnitTooltip) {
        TooltipDialog(
          title = stringResource(R.string.bg_label_size_unit_with_value, stringResource(layer.sizeUnit.displayStringRId())),
          text = stringResource(layer.sizeUnit.getTooltip()),
          onDismiss = { showUnitTooltip = false }
        )
      }
    } else {
      // Stamping mapping
      BrushSliderControl(
        label = stringResource(R.string.bg_label_animation_rows),
        value = layer.animationRows.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationRows = it.toInt()))) }
      )
      BrushSliderControl(
        label = stringResource(R.string.bg_label_animation_columns),
        value = layer.animationColumns.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationColumns = it.toInt()))) }
      )
      BrushSliderControl(
        label = stringResource(R.string.bg_label_animation_frames),
        value = layer.animationFrames.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationFrames = it.toInt()))) }
      )
      BrushSliderControl(
        label = stringResource(R.string.bg_label_animation_duration_ms),
        value = layer.animationDurationSeconds * 1000f,
        valueRange = 1f..10000f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationDurationSeconds = it / 1000f))) }
      )
    }

    InspectorSectionHeader(stringResource(R.string.bg_section_positioning), stringResource(R.string.bg_section_positioning_sub))

    var expandedOrigin by remember { mutableStateOf(false) }
    var showOriginTooltip by remember { mutableStateOf(false) }
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
      ExposedDropdownMenuBox(
        expanded = expandedOrigin,
        onExpandedChange = { expandedOrigin = it },
        modifier = Modifier.weight(1f)
      ) {
        OutlinedTextField(
          value = stringResource(layer.origin.displayStringRId()),
          onValueChange = {},
          readOnly = true,
          label = { Text(stringResource(R.string.bg_origin)) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigin) },
          modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
          expanded = expandedOrigin,
          onDismissRequest = { expandedOrigin = false }
        ) {
          arrayOf(
            ProtoBrushPaint.TextureLayer.Origin.ORIGIN_STROKE_SPACE_ORIGIN,
            ProtoBrushPaint.TextureLayer.Origin.ORIGIN_FIRST_STROKE_INPUT,
            ProtoBrushPaint.TextureLayer.Origin.ORIGIN_LAST_STROKE_INPUT,
          ).forEach { origin ->
            DropdownMenuItem(
              text = { Text(stringResource(origin.displayStringRId())) },
              onClick = {
                onUpdate(NodeData.TextureLayer(layer.safeCopy(origin = origin)))
                expandedOrigin = false
              }
            )
          }
        }
      }
      IconButton(onClick = { showOriginTooltip = true }) {
        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
      }
    }
    if (showOriginTooltip) {
      TooltipDialog(
        title = stringResource(R.string.bg_label_origin_with_value, stringResource(layer.origin.displayStringRId())),
        text = stringResource(layer.origin.getTooltip()),
        onDismiss = { showOriginTooltip = false }
      )
    }
    BrushSliderControl(
      label = stringResource(R.string.bg_label_offset_x),
      value = layer.offsetX,
      valueRange = -1f..1f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetX = it))) }
    )
    BrushSliderControl(
      label = stringResource(R.string.bg_label_offset_y),
      value = layer.offsetY,
      valueRange = -1f..1f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetY = it))) }
    )
    BrushSliderControl(
      label = stringResource(R.string.bg_label_rotation_degrees),
      value = Math.toDegrees(layer.rotationInRadians.toDouble()).toFloat(),
      valueRange = 0f..360f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(rotationInRadians = Math.toRadians(it.toDouble()).toFloat()))) }
    )

    InspectorSectionHeader(stringResource(R.string.bg_section_wrapping), stringResource(R.string.bg_section_wrapping_sub))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        var expandedWrapX by remember { mutableStateOf(false) }
        var showWrapXTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          ExposedDropdownMenuBox(
            expanded = expandedWrapX,
            onExpandedChange = { expandedWrapX = it },
            modifier = Modifier.weight(1f)
          ) {
            OutlinedTextField(
              value = stringResource(layer.wrapX.displayStringRId()),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_wrap_x)) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWrapX) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedWrapX,
              onDismissRequest = { expandedWrapX = false }
            ) {
              arrayOf(
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
              ).forEach { wrap ->
                DropdownMenuItem(
                  text = { Text(stringResource(wrap.displayStringRId())) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapX = wrap)))
                    expandedWrapX = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showWrapXTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showWrapXTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_label_wrap_x_with_value, stringResource(layer.wrapX.displayStringRId())),
            text = stringResource(layer.wrapX.getTooltip()),
            onDismiss = { showWrapXTooltip = false }
          )
        }
        var expandedWrapY by remember { mutableStateOf(false) }
        var showWrapYTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          ExposedDropdownMenuBox(
            expanded = expandedWrapY,
            onExpandedChange = { expandedWrapY = it },
            modifier = Modifier.weight(1f)
          ) {
            OutlinedTextField(
              value = stringResource(layer.wrapY.displayStringRId()),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_wrap_y)) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWrapY) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedWrapY,
              onDismissRequest = { expandedWrapY = false }
            ) {
              arrayOf(
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_REPEAT,
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_MIRROR,
                ProtoBrushPaint.TextureLayer.Wrap.WRAP_CLAMP,
              ).forEach { wrap ->
                DropdownMenuItem(
                  text = { Text(stringResource(wrap.displayStringRId())) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapY = wrap)))
                    expandedWrapY = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showWrapYTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showWrapYTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_label_wrap_y_with_value, stringResource(layer.wrapY.displayStringRId())),
            text = stringResource(layer.wrapY.getTooltip()),
            onDismiss = { showWrapYTooltip = false }
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

    InspectorSectionHeader(stringResource(R.string.bg_section_blending), stringResource(R.string.bg_section_blending_sub))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(modifier = Modifier.weight(1f)) {
        var expandedBlend by remember { mutableStateOf(false) }
        var showBlendTooltip by remember { mutableStateOf(false) }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
          ExposedDropdownMenuBox(
            expanded = expandedBlend,
            onExpandedChange = { expandedBlend = it },
            modifier = Modifier.weight(1f)
          ) {
            OutlinedTextField(
              value = stringResource(layer.blendMode.displayStringRId()),
              onValueChange = {},
              readOnly = true,
              label = { Text(stringResource(R.string.bg_blend_mode)) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBlend) },
              modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
              expanded = expandedBlend,
              onDismissRequest = { expandedBlend = false }
            ) {
              arrayOf(
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
              ).forEach { mode ->
                DropdownMenuItem(
                  text = { Text(stringResource(mode.displayStringRId())) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(blendMode = mode)))
                    expandedBlend = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showBlendTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
          }
        }
        if (showBlendTooltip) {
          TooltipDialog(
            title = stringResource(R.string.bg_label_blend_mode_with_value, stringResource(layer.blendMode.displayStringRId())),
            text = stringResource(layer.blendMode.getTooltip()),
            onDismiss = { showBlendTooltip = false }
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
private fun InspectorSectionHeader(title: String, subtitle: String) {
  Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.secondary,
      lineHeight = 16.sp
    )
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
  }
}
