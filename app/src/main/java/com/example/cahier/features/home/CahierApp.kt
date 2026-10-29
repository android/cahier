/*
 * Copyright 2025 Google LLC. All rights reserved.
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

package com.example.cahier.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cahier.core.data.CahierTextureBitmapStore
import com.example.cahier.core.data.NoteType
import com.example.cahier.core.navigation.BrushGraphDestination
import com.example.cahier.core.navigation.DrawingCanvasDestination
import com.example.cahier.core.navigation.TextCanvasDestination
import com.example.cahier.developer.brushgraph.ui.BrushGraphScreen
import com.example.cahier.features.drawing.DrawingCanvas
import com.example.cahier.features.text.TextNoteCanvasScreen

@Composable
fun CahierApp(
  noteId: Long,
  noteType: NoteType?,
  textureStore: CahierTextureBitmapStore,
  modifier: Modifier = Modifier,
  navigateToBrushGraph: Boolean = false,
  onNavigateToBrushGraphHandled: () -> Unit = {},
) {
  val navController = rememberNavController()

  LaunchedEffect(noteId, noteType) {
    if (noteId > 0) {
      val destination =
        when (noteType) {
          NoteType.Text -> "${TextCanvasDestination.route}/$noteId"
          NoteType.Drawing -> "${DrawingCanvasDestination.route}/$noteId"
          else -> null
        }
      destination?.let { navController.navigate(it) }
    }
  }

  LaunchedEffect(navigateToBrushGraph) {
    if (navigateToBrushGraph) {
      if (navController.currentDestination?.route != BrushGraphDestination.route) {
        navController.navigate(BrushGraphDestination.route)
      }
      onNavigateToBrushGraphHandled()
    }
  }

  CahierNavHost(navController = navController, textureStore = textureStore, modifier = modifier)
}

@OptIn(ExperimentalComposeApi::class)
@Composable
private fun CahierNavHost(
  navController: NavHostController,
  textureStore: CahierTextureBitmapStore,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = HomeDestination.route,
    modifier = modifier,
  ) {
    composable(HomeDestination.route) {
      HomePane(
        navigateToCanvas = { noteId ->
          navController.navigate("${TextCanvasDestination.route}/$noteId")
        },
        navigateToDrawingCanvas = { noteId ->
          navController.navigate("${DrawingCanvasDestination.route}/$noteId")
        },
        navigateUp = { navController.navigateUp() },
        navigateToBrushGraph = { navController.navigate(BrushGraphDestination.route) },
      )
    }
    composable(
      route = TextCanvasDestination.routeWithArgs,
      arguments = listOf(navArgument(TextCanvasDestination.NOTE_ID_ARG) { type = NavType.LongType }),
    ) { navBackStackEntry ->
      TextNoteCanvasScreen(onExit = { navController.navigateUp() })
    }
    composable(
      route = DrawingCanvasDestination.routeWithArgs,
      arguments =
        listOf(navArgument(DrawingCanvasDestination.NOTE_ID_ARG) { type = NavType.LongType }),
    ) { navBackStackEntry ->
      DrawingCanvas(
        navigateUp = { navController.navigateUp() },
        navigateToBrushGraph = { navController.navigate(BrushGraphDestination.route) },
      )
    }
    composable(route = BrushGraphDestination.route) {
      BrushGraphScreen(onNavigateUp = { navController.navigateUp() })
    }
  }
}
