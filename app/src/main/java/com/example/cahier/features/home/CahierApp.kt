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

package com.example.cahier.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isCaptionBarVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cahier.R
import com.example.cahier.core.data.NoteType
import com.example.cahier.core.navigation.CahierNavHost
import com.example.cahier.core.navigation.DrawingCanvasDestination
import com.example.cahier.core.navigation.TextCanvasDestination

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CahierApp(
    noteId: Long,
    noteType: NoteType?,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(noteId, noteType) {
        if (noteId > 0) {
            val destination = when (noteType) {
                NoteType.Text -> "${TextCanvasDestination.route}/$noteId"
                NoteType.Drawing -> "${DrawingCanvasDestination.route}/$noteId"
                else -> null
            }
            destination?.let {
                navController.navigate(it)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (WindowInsets.isCaptionBarVisible) {
            val title = when (currentRoute) {
                TextCanvasDestination.routeWithArgs -> stringResource(R.string.text_note)
                DrawingCanvasDestination.routeWithArgs -> stringResource(R.string.drawing)
                else -> stringResource(R.string.app_name)
            }
            Row(
                modifier = Modifier
                    .windowInsetsTopHeight(WindowInsets.captionBar)
                    .fillMaxWidth()
                    .background(
                        if (isSystemInDarkTheme())
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        CahierNavHost(
            navController = navController,
            modifier = Modifier.weight(1f)
        )
    }
}