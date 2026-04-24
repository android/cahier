@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.data.NodeData
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.ui.FieldWithTooltip
import com.example.cahier.ui.brushgraph.data.displayStringRId
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
