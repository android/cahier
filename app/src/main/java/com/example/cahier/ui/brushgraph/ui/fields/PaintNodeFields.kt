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
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.ui.FieldWithTooltip
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
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_self_overlap_format, stringResource(paint.selfOverlap.displayStringRId())),
    tooltipText = stringResource(paint.selfOverlap.getTooltip()),
    modifier = modifier.padding(vertical = 4.dp)
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_self_overlap),
      currentValue = paint.selfOverlap,
      values = listOf(
        ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ANY,
        ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_ACCUMULATE,
        ProtoBrushPaint.SelfOverlap.SELF_OVERLAP_DISCARD,
      ),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { so ->
        onUpdate(NodeData.Paint(paint.toBuilder().setSelfOverlap(so).build(), texturePortIds = data.texturePortIds, colorPortIds = data.colorPortIds))
        onDropdownEditComplete()
      }
    )
  }
}
