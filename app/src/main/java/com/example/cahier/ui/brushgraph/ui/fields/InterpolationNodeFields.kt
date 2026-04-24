@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.model.safeCopy
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.ui.fields.ALL_INTERPOLATIONS
import com.example.cahier.ui.brushgraph.ui.getTooltip
import com.example.cahier.ui.brushgraph.model.displayStringRId
import ink.proto.BrushBehavior as ProtoBrushBehavior

@Composable
fun InterpolationNodeFields(
  interpNode: ProtoBrushBehavior.InterpolationNode,
  behaviorNode: ProtoBrushBehavior.Node,
  onUpdate: (NodeData) -> Unit,
  modifier: Modifier = Modifier
) {
  var showInterpTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_interpolation),
      currentValue = interpNode.interpolation,
      values = ALL_INTERPOLATIONS.toList(),
      modifier = Modifier.weight(1f),
      displayName = { stringResource(it.displayStringRId()) },
      onSelected = { interp ->
        onUpdate(
          NodeData.Behavior(
            behaviorNode.safeCopy(
              interpolationNode = interpNode.safeCopy(interpolation = interp)
            )
          )
        )
      }
    )
    IconButton(onClick = { showInterpTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  if (showInterpTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_interpolation_format, stringResource(interpNode.interpolation.displayStringRId())),
      text = stringResource(interpNode.interpolation.getTooltip()),
      onDismiss = { showInterpTooltip = false }
    )
  }
}
