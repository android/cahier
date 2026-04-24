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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.features.drawing.CustomBrushes
import com.example.cahier.ui.brushgraph.model.NodeData
import com.example.cahier.ui.brushgraph.ui.TooltipDialog
import com.example.cahier.ui.brushgraph.model.displayStringRId
import com.example.cahier.ui.brushgraph.ui.getInputModelTooltip
import ink.proto.BrushFamily as ProtoBrushFamily

@Composable
fun FamilyNodeFields(
  data: NodeData.Family,
  onUpdate: (NodeData) -> Unit,
  onDropdownEditComplete: () -> Unit,
  textFieldsLocked: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  
  OutlinedTextField(
    value = data.clientBrushFamilyId,
    onValueChange = { onUpdate(data.copy(clientBrushFamilyId = it)) },
    label = { Text(stringResource(R.string.bg_client_brush_family_id)) },
    modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
    singleLine = true,
    enabled = !textFieldsLocked,
  )
  OutlinedTextField(
    value = data.developerComment,
    onValueChange = { onUpdate(data.copy(developerComment = it)) },
    label = { Text(stringResource(R.string.bg_brush_developer_comment)) },
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    minLines = 3,
    enabled = !textFieldsLocked,
  )
  
  var expandedModel by remember { mutableStateOf(false) }
  var showModelTooltip by remember { mutableStateOf(false) }
  
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    ExposedDropdownMenuBox(
      expanded = expandedModel,
      onExpandedChange = { expandedModel = it },
      modifier = Modifier.weight(1f)
    ) {
      OutlinedTextField(
        value = stringResource(data.inputModel.displayStringRId()),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.bg_input_model)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModel) },
        modifier = Modifier.menuAnchor().fillMaxWidth()
      )
      ExposedDropdownMenu(
        expanded = expandedModel,
        onDismissRequest = { expandedModel = false }
      ) {
        listOf(R.string.bg_model_sliding_window, R.string.bg_model_spring, R.string.bg_model_naive_experimental).forEach { modelResId ->
          DropdownMenuItem(
            text = { Text(stringResource(modelResId)) },
            onClick = {
              val newModel =
                when (modelResId) {
                  R.string.bg_model_naive_experimental ->
                    ProtoBrushFamily.InputModel.newBuilder()
                      .setExperimentalNaiveModel(
                        ProtoBrushFamily.ExperimentalNaiveModel.getDefaultInstance()
                      )
                      .build()
                  R.string.bg_model_sliding_window ->
                    ProtoBrushFamily.InputModel.newBuilder()
                      .setSlidingWindowModel(
                        ProtoBrushFamily.SlidingWindowModel.newBuilder()
                          .setWindowSizeSeconds(0.02f)
                          .setExperimentalUpsamplingPeriodSeconds(0.005f)
                      )
                      .build()
                  R.string.bg_model_spring ->
                    ProtoBrushFamily.InputModel.newBuilder()
                      .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                      .build()
                  else ->
                    ProtoBrushFamily.InputModel.newBuilder()
                      .setSpringModel(ProtoBrushFamily.SpringModel.getDefaultInstance())
                      .build()
                }
              onUpdate(data.copy(inputModel = newModel))
              onDropdownEditComplete()
              expandedModel = false
            }
          )
        }
      }
    }
    IconButton(onClick = { showModelTooltip = true }) {
      Icon(Icons.AutoMirrored.Filled.Help, contentDescription = stringResource(R.string.bg_cd_help))
    }
  }
  
  if (showModelTooltip) {
    TooltipDialog(
      title = stringResource(R.string.bg_title_input_model_format, stringResource(data.inputModel.displayStringRId())),
      text = stringResource(getInputModelTooltip(data.inputModel.displayStringRId())),
      onDismiss = { showModelTooltip = false }
    )
  }
}
