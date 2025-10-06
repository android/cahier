/*
 *
 *  * Copyright 2025 Google LLC. All rights reserved.
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
 *
 */

package com.example.cahier.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cahier.ui.viewmodels.DrawingCanvasViewModel

@Composable
internal fun HorizontalToolbox(
    drawingCanvasViewModel: DrawingCanvasViewModel,
    imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onExit: () -> Unit,
    onColorPickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            ToolboxBrushControls(
                drawingCanvasViewModel = drawingCanvasViewModel,
                isVertical = false,
                onColorPickerClick = onColorPickerClick,
            )
        }
        item { Spacer(modifier = Modifier.width(8.dp)) }
        item {
            VerticalDivider(
                thickness = 4.dp,
                modifier = Modifier
                    .height(48.dp)
                    .padding(vertical = 8.dp)
            )
        }
        item { Spacer(modifier = Modifier.width(8.dp)) }
        item {
            ToolboxHistoryControls(
                onUndo = onUndo,
                canUndo = canUndo,
                onRedo = onRedo,
                canRedo = canRedo,
                drawingCanvasViewModel = drawingCanvasViewModel,
                onClear = {
                    drawingCanvasViewModel.clearScreen()
                },
                isVertical = false
            )
        }
        item { Spacer(modifier = Modifier.width(16.dp)) }
        item {
            VerticalDivider(
                thickness = 4.dp,
                modifier = Modifier
                    .height(48.dp)
                    .padding(vertical = 8.dp)
            )
        }
        item { Spacer(modifier = Modifier.width(8.dp)) }
        item {
            ToolboxNoteActions(
                drawingCanvasViewModel = drawingCanvasViewModel,
                imagePickerLauncher = imagePickerLauncher,
                onExit = onExit,
                isVertical = false
            )
        }
    }
}
