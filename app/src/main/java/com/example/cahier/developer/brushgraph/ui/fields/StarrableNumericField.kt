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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.cahier.R
import com.example.cahier.developer.brushdesigner.ui.NumericField
import com.example.cahier.developer.brushdesigner.ui.NumericLimits
import com.example.cahier.developer.brushgraph.data.StarredFieldType

/**
 * A wrapper around [NumericField] that adds a star icon to the right.
 *
 * @param nodeId the ID of the node this field belongs to.
 * @param fieldType the type of the field.
 * @param value the current value of the field.
 * @param limits the limits of the field.
 * @param isStarred whether the field is currently starred.
 * @param onToggleStar callback when the star icon is tapped.
 * @param onValueChanged callback when the value changes.
 * @param onValueChangeFinished callback when value change is finished (e.g. slider released).
 */
@Composable
fun StarrableNumericField(
    nodeId: String,
    fieldType: StarredFieldType,
    value: Float,
    limits: NumericLimits,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onValueChanged: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f)) {
            NumericField(
                title = stringResource(fieldType.displayNameRes),
                value = value,
                limits = limits,
                onValueChanged = onValueChanged,
                onValueChangeFinished = onValueChangeFinished
            )
        }
        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = stringResource(if (isStarred) R.string.bg_cd_unstar else R.string.bg_cd_star),
                tint = if (isStarred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
