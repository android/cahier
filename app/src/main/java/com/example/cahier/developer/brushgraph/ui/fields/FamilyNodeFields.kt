/*
 *  * Copyright 2026 Google LLC. All rights reserved.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.cahier.developer.brushgraph.ui.fields

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.EnumDropdown
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.NodeData
import com.example.cahier.developer.brushgraph.data.StarredField
import com.example.cahier.developer.brushgraph.data.StarredFieldType
import com.example.cahier.developer.brushgraph.ui.fields.StarrableNumericField
import com.example.cahier.developer.brushgraph.ui.FieldWithTooltip
import com.example.cahier.developer.brushgraph.data.displayStringRId
import com.example.cahier.developer.brushgraph.ui.getInputModelTooltip
import ink.proto.BrushFamily as ProtoBrushFamily

@Composable
fun FamilyNodeFields(
  data: NodeData.Family,
  onUpdate: (NodeData) -> Unit,
  onDropdownEditComplete: () -> Unit,
  textFieldsLocked: Boolean,
  nodeId: String,
  starredFields: Set<StarredField>,
  onToggleStar: (String, StarredFieldType) -> Unit,
  modifier: Modifier = Modifier
) {
  
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
  
  FieldWithTooltip(
    tooltipTitle = stringResource(R.string.bg_title_input_model_format, stringResource(data.inputModel.displayStringRId())),
    tooltipText = stringResource(getInputModelTooltip(data.inputModel.displayStringRId()))
  ) {
    EnumDropdown(
      label = stringResource(R.string.bg_input_model),
      currentValue = data.inputModel.displayStringRId(),
      values = listOf(R.string.bg_model_sliding_window, R.string.bg_model_spring, R.string.bg_model_naive_experimental),
      displayName = { stringResource(it) },
      onSelected = { modelResId ->
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
      }
    )
  }
  
  val inputModel = data.inputModel
  if (inputModel.hasSlidingWindowModel() || (!inputModel.hasSpringModel() && !inputModel.hasExperimentalNaiveModel())) {
    val swModel = inputModel.slidingWindowModel
    val windowMs = if (swModel.hasWindowSizeSeconds()) (swModel.windowSizeSeconds * 1000).toLong() else 20L
    val upsamplingHz = if (swModel.hasExperimentalUpsamplingPeriodSeconds()) {
        val period = swModel.experimentalUpsamplingPeriodSeconds
        if (period == Float.POSITIVE_INFINITY || period == 0f) 0 else (1f / period).toInt()
    } else 180

    Spacer(Modifier.height(16.dp))
    
    val isWindowSizeStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.FAMILY_WINDOW_SIZE))
    
    StarrableNumericField(
      nodeId = nodeId,
      fieldType = StarredFieldType.FAMILY_WINDOW_SIZE,
      value = windowMs.toFloat(),
      limits = NumericLimits.standard(1f, 100f, 1f),
      isStarred = isWindowSizeStarred,
      onToggleStar = { onToggleStar(nodeId, StarredFieldType.FAMILY_WINDOW_SIZE) },
      onValueChanged = { newValue ->
        val newModel = inputModel.toBuilder()
          .setSlidingWindowModel(
            swModel.toBuilder()
              .setWindowSizeSeconds(newValue / 1000f)
          )
          .build()
        onUpdate(data.copy(inputModel = newModel))
      }
    )
    
    val isUpsamplingStarred = starredFields.contains(StarredField(nodeId, StarredFieldType.FAMILY_UPSAMPLING_FREQ))

    StarrableNumericField(
      nodeId = nodeId,
      fieldType = StarredFieldType.FAMILY_UPSAMPLING_FREQ,
      value = upsamplingHz.toFloat(),
      limits = NumericLimits.standard(0f, 500f, 1f),
      isStarred = isUpsamplingStarred,
      onToggleStar = { onToggleStar(nodeId, StarredFieldType.FAMILY_UPSAMPLING_FREQ) },
      onValueChanged = { newValue ->
        val newPeriod = if (newValue == 0f) Float.POSITIVE_INFINITY else 1f / newValue
        val newModel = inputModel.toBuilder()
          .setSlidingWindowModel(
            swModel.toBuilder()
              .setExperimentalUpsamplingPeriodSeconds(newPeriod)
          )
          .build()
        onUpdate(data.copy(inputModel = newModel))
      }
    )
  }
}
