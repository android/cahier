@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushPaint as ProtoBrushPaint

@Composable
fun PaintNodeFields(
  data: NodeData.Paint,
  onUpdate: (NodeData) -> Unit,
  onDropdownEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val paint = data.paint
  var expanded by remember { mutableStateOf(false) }
  var showTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
  ) {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(paint.selfOverlap.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_self_overlap)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        arrayOf(
          ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY,
          ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE,
          ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD,
        ).forEach { so ->
          DropdownMenuItem(
            text = { Text(stringResource(so.displayStringRId())) },
            onClick = {
              onUpdate(NodeData.Paint(paint.safeCopy(selfOverlap = so), texturePortIds = data.texturePortIds, colorPortIds = data.colorPortIds))
              onDropdownEditComplete()
              expanded = false
            }
          )
        }
      }
    }
    IconButton(onClick = { showTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  if (showTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_self_overlap_format, stringResource(paint.selfOverlap.displayStringRId())),
      text = stringResource(paint.selfOverlap.getTooltip()),
      onDismiss = { showTooltip = false }
    )
  }
}
