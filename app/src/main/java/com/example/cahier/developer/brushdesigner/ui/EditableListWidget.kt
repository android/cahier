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

package com.example.cahier.developer.brushdesigner.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cahier.R

/**
 * Wrapper for A/B testing: tracks whether each item is enabled or disabled
 * without removing it from the list.
 */
data class CheckableItem<T>(val item: T, val enabled: Boolean = true)

/**
 * A generic list editor that supports add, delete, duplicate, and A/B
 * testing (enable/disable toggle) for ordered lists of proto items.
 *
 * Stateless: the canonical item list is passed in via [items], and all
 * changes are emitted via [onItemsChanged] with only the enabled items.
 *
 * @param title section header text
 * @param items current list of items (from proto)
 * @param defaultItem factory default for new items
 * @param onItemsChanged callback with the updated full list of items
 * @param itemHeader display label for each item in collapsed view
 * @param editorContent expanded editor for the selected item
 */
@Composable
internal fun <T> EditableListWidget(
    title: String,
    items: List<T>,
    defaultItem: T,
    onItemsChanged: (List<T>) -> Unit,
    itemHeader: @Composable (T) -> String,
    editorContent: @Composable (item: T, onItemChanged: (T) -> Unit) -> Unit
) {
    var itemStates by remember { mutableStateOf(items
        .map { CheckableItem(it, true) }) }


    if (itemStates.size != items.size ||
        itemStates.zip(items).any { (s, i) -> s.item != i }) {
            itemStates = items.mapIndexed { index, item ->
                CheckableItem(item, itemStates
                    .getOrNull(index)?.enabled ?: true)
            }
        }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    fun emitEnabledItems() {
        onItemsChanged(itemStates.filter { it.enabled }.map { it.item })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (title.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = {
                    itemStates = itemStates + CheckableItem(defaultItem, true)
                    selectedIndex = itemStates.size
                    emitEnabledItems()
                }) {
                    Icon(
                        painterResource(R.drawable.add_24px),
                        contentDescription = stringResource(
                            R.string.brush_designer_add_item, title
                        ),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        itemStates.forEachIndexed { index, state ->
            val isSelected = selectedIndex == index
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { selectedIndex = if (isSelected) null else index }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.enabled,
                        onCheckedChange = { checked ->
                            val newList = itemStates.toMutableList()
                            newList[index] = state.copy(enabled = checked)
                            itemStates = newList
                            emitEnabledItems()
                        }
                    )

                    Text(
                        text = itemHeader(state.item),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(onClick = {
                        val newList = itemStates.toMutableList()
                        newList.add(index + 1, state.copy())
                        itemStates = newList
                        emitEnabledItems()
                    }) {
                        Icon(
                            painterResource(R.drawable.content_copy_24px),
                            contentDescription = stringResource(
                                R.string.brush_designer_duplicate_item
                            )
                        )
                    }

                    IconButton(onClick = {
                        val newList = itemStates.toMutableList()
                        newList.removeAt(index)
                        itemStates = newList
                        if (selectedIndex == index) selectedIndex = null
                        else if (selectedIndex != null && selectedIndex!! > index) {
                            selectedIndex = selectedIndex!! - 1
                        }
                        emitEnabledItems()
                    }) {
                        Icon(
                            painterResource(R.drawable.delete_24px),
                            contentDescription = stringResource(
                                R.string.brush_designer_delete_item
                            ),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        selectedIndex?.let { index ->
            if (index < itemStates.size) {
                val currentItem = itemStates[index].item
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    border = BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(
                                R.string.brush_designer_editing_item, index + 1
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(8.dp))

                        editorContent(currentItem) { updatedItem ->
                            val newList = itemStates.toMutableList()
                            newList[index] = itemStates[index].copy(item = updatedItem)
                            itemStates = newList
                            emitEnabledItems()
                        }
                    }
                }
            }
        }
    }
}
