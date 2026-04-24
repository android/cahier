@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.ui.brushgraph.ui.fields

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ink.proto.Color as ProtoColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.ui.brushgraph.data.NodeData
import com.example.cahier.ui.brushgraph.ui.FieldWithTooltip
import com.example.cahier.ui.brushgraph.ui.getColorFunctionTooltip
import ink.proto.ColorFunction as ProtoColorFunction

@Composable
fun ColorFunctionNodeFields(
  function: ProtoColorFunction,
  onUpdate: (NodeData) -> Unit,
  onChooseColor: (Color, (Color) -> Unit) -> Unit,
  onDropdownEditComplete: () -> Unit,
  onFieldEditComplete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentTypeResId = if (function.hasOpacityMultiplier()) {
    R.string.bg_opacity_multiplier
  } else {
    R.string.bg_replace_color
  }

  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_function_type_format, stringResource(currentTypeResId)),
    tooltipText = stringResource(getColorFunctionTooltip(currentTypeResId)),
    modifier = modifier
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_function_type),
      currentValue = currentTypeResId,
      values = listOf(R.string.bg_opacity_multiplier, R.string.bg_replace_color),
      displayName = { stringResource(it) },
      onSelected = { resId ->
        if (resId != currentTypeResId) {
          onUpdate(
            if (resId == R.string.bg_opacity_multiplier) {
              NodeData.ColorFunction(
                ProtoColorFunction.newBuilder().setOpacityMultiplier(1f).build()
              )
            } else {
              NodeData.ColorFunction(
                ProtoColorFunction.newBuilder()
                  .setReplaceColor(
                    ProtoColor.newBuilder()
                      .setRed(0f)
                      .setGreen(0f)
                      .setBlue(0f)
                      .setAlpha(1f)
                      .build()
                  )
                  .build()
              )
            }
          )
        }
        onDropdownEditComplete()
      }
    )
  }
  
  if (function.hasOpacityMultiplier()) {
    NumericField(
      title = stringResource(R.string.bg_label_opacity_multiplier),
      value = function.opacityMultiplier,
      limits = NumericLimits.standard(0f, 2f, 0.01f),
      onValueChanged = { onUpdate(NodeData.ColorFunction(function.toBuilder().setOpacityMultiplier(it).build())) },
      onValueChangeFinished = onFieldEditComplete
    )
  } else if (function.hasReplaceColor()) {
    val color = function.replaceColor
    val composeColor =
      Color(red = color.red, green = color.green, blue = color.blue, alpha = color.alpha)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(vertical = 8.dp)
    ) {
      Text(stringResource(R.string.bg_color_label), style = MaterialTheme.typography.bodyMedium)
      Surface(
        onClick = {
          onChooseColor(composeColor) { newColor ->
            onUpdate(
              NodeData.ColorFunction(
                function.toBuilder()
                  .setReplaceColor(
                    ProtoColor.newBuilder()
                      .setRed(newColor.red)
                      .setGreen(newColor.green)
                      .setBlue(newColor.blue)
                      .setAlpha(newColor.alpha)
                      .build()
                  )
                  .build()
              )
            )
          }
        },
        shape = RoundedCornerShape(4.dp),
        color = composeColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(40.dp)
      ) {}
      Spacer(Modifier.width(8.dp))
      Text(
        text = String.format("ARGB #%08X", (composeColor.toArgb())),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}
