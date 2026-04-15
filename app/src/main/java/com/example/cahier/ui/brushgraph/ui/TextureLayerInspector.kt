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
            value = prettyDisplayString(layer.clientTextureId),
            onValueChange = {},
            readOnly = true,
            label = { Text("Texture ID") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
          )
          ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
          ) {
            allTextureIds.forEach { id ->
              DropdownMenuItem(
                text = { Text(prettyDisplayString(id)) },
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
        Icon(Icons.Default.Upload, contentDescription = "Upload Texture")
      }
    }

    TextureLayerPreviewWidget(textureLayer = layer, renderer = strokeRenderer)

    InspectorSectionHeader("Mapping", "How is my texture imported?")

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
          value = prettyDisplayString(layer.mapping),
          onValueChange = {},
          readOnly = true,
          label = { Text("Mapping Mode") },
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
              text = { Text(prettyDisplayString(mapping)) },
              onClick = {
                onUpdate(NodeData.TextureLayer(layer.safeCopy(mapping = mapping)))
                expandedMapping = false
              }
            )
          }
        }
      }
      IconButton(onClick = { showMappingTooltip = true }) {
        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
      }
    }
    if (showMappingTooltip) {
      TooltipDialog(
        title = "Mapping Mode: ${prettyDisplayString(layer.mapping)}",
        text = layer.mapping.getTooltip(),
        onDismiss = { showMappingTooltip = false }
      )
    }

    if (layer.mapping == ProtoBrushPaint.TextureLayer.Mapping.MAPPING_TILING) {
      BrushSliderControl(
        label = "Size X",
        value = layer.sizeX,
        valueRange = 0.1f..1000f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeX = it))) }
      )
      BrushSliderControl(
        label = "Size Y",
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
            value = prettyDisplayString(layer.sizeUnit),
            onValueChange = {},
            readOnly = true,
            label = { Text("Size Unit") },
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
                text = { Text(prettyDisplayString(unit)) },
                onClick = {
                  onUpdate(NodeData.TextureLayer(layer.safeCopy(sizeUnit = unit)))
                  expandedUnit = false
                }
              )
            }
          }
        }
        IconButton(onClick = { showUnitTooltip = true }) {
          Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
        }
      }
      if (showUnitTooltip) {
        TooltipDialog(
          title = "Size Unit: ${prettyDisplayString(layer.sizeUnit)}",
          text = layer.sizeUnit.getTooltip(),
          onDismiss = { showUnitTooltip = false }
        )
      }
    } else {
      // Stamping mapping
      BrushSliderControl(
        label = "Animation Rows",
        value = layer.animationRows.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationRows = it.toInt()))) }
      )
      BrushSliderControl(
        label = "Animation Columns",
        value = layer.animationColumns.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationColumns = it.toInt()))) }
      )
      BrushSliderControl(
        label = "Animation Frames",
        value = layer.animationFrames.toFloat(),
        valueRange = 1f..100f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationFrames = it.toInt()))) }
      )
      BrushSliderControl(
        label = "Animation Duration (ms)",
        value = layer.animationDurationSeconds * 1000f,
        valueRange = 1f..10000f,
        onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(animationDurationSeconds = it / 1000f))) }
      )
    }

    InspectorSectionHeader("Positioning", "How is my texture drawn relative to the stroke?")

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
          value = prettyDisplayString(layer.origin),
          onValueChange = {},
          readOnly = true,
          label = { Text("Origin") },
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
              text = { Text(prettyDisplayString(origin)) },
              onClick = {
                onUpdate(NodeData.TextureLayer(layer.safeCopy(origin = origin)))
                expandedOrigin = false
              }
            )
          }
        }
      }
      IconButton(onClick = { showOriginTooltip = true }) {
        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
      }
    }
    if (showOriginTooltip) {
      TooltipDialog(
        title = "Origin: ${prettyDisplayString(layer.origin)}",
        text = layer.origin.getTooltip(),
        onDismiss = { showOriginTooltip = false }
      )
    }
    BrushSliderControl(
      label = "Offset X",
      value = layer.offsetX,
      valueRange = -1f..1f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetX = it))) }
    )
    BrushSliderControl(
      label = "Offset Y",
      value = layer.offsetY,
      valueRange = -1f..1f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(offsetY = it))) }
    )
    BrushSliderControl(
      label = "Rotation Degrees",
      value = Math.toDegrees(layer.rotationInRadians.toDouble()).toFloat(),
      valueRange = 0f..360f,
      onValueChange = { onUpdate(NodeData.TextureLayer(layer.safeCopy(rotationInRadians = Math.toRadians(it.toDouble()).toFloat()))) }
    )

    InspectorSectionHeader("Wrapping", "What happens when the stroke draws further than the bounds of my texture?")

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
              value = prettyDisplayString(layer.wrapX),
              onValueChange = {},
              readOnly = true,
              label = { Text("Wrap X") },
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
                  text = { Text(prettyDisplayString(wrap)) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapX = wrap)))
                    expandedWrapX = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showWrapXTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showWrapXTooltip) {
          TooltipDialog(
            title = "Wrap X: ${prettyDisplayString(layer.wrapX)}",
            text = layer.wrapX.getTooltip(),
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
              value = prettyDisplayString(layer.wrapY),
              onValueChange = {},
              readOnly = true,
              label = { Text("Wrap Y") },
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
                  text = { Text(prettyDisplayString(wrap)) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(wrapY = wrap)))
                    expandedWrapY = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showWrapYTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showWrapYTooltip) {
          TooltipDialog(
            title = "Wrap Y: ${prettyDisplayString(layer.wrapY)}",
            text = layer.wrapY.getTooltip(),
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

    InspectorSectionHeader("Blending", "How does my texture combine with the regular ink of the stroke?")

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
              value = prettyDisplayString(layer.blendMode),
              onValueChange = {},
              readOnly = true,
              label = { Text("Blend Mode") },
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
                  text = { Text(prettyDisplayString(mode)) },
                  onClick = {
                    onUpdate(NodeData.TextureLayer(layer.safeCopy(blendMode = mode)))
                    expandedBlend = false
                  }
                )
              }
            }
          }
          IconButton(onClick = { showBlendTooltip = true }) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Help")
          }
        }
        if (showBlendTooltip) {
          TooltipDialog(
            title = "Blend Mode: ${prettyDisplayString(layer.blendMode)}",
            text = layer.blendMode.getTooltip(),
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
