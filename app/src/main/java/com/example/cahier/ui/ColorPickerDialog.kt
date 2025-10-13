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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.cahier.R

@Composable
fun ColorPickerDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
    ) {
        if (showDialog) {
            Dialog(
                onDismissRequest = onDismissRequest,
            ) {
                ColorPickerContent(
                    onColorSelected = onColorSelected,
                    onDismissRequest = onDismissRequest
                )
            }
        }
    }
}

@Composable
private fun ColorPickerContent(
    onColorSelected: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_color),
                style = MaterialTheme.typography.headlineSmall
            )
            ColorGrid(
                onColorSelected = onColorSelected,
            )
            Button(onClick = onDismissRequest, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(R.string.dismiss))
            }
        }
    }
}


@Composable
private fun ColorGrid(
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = remember {
        listOf(
            Color.Black, Color.Gray, Color.White, Color.Red, Color.Green, Color.Blue,
            Color.Yellow, Color.Cyan, Color.Magenta, Color.DarkGray, Color.LightGray
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 40.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = modifier
    ) {
        items(colors.size) { index ->
            ColorSwatch(
                color = colors[index],
                onColorSelected = onColorSelected
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(8.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onColorSelected(color) }
    )
}

@Preview
@Composable
private fun ColorPickerContentPreview() {
    ColorPickerContent(
        onColorSelected = {},
        onDismissRequest = {}
    )
}