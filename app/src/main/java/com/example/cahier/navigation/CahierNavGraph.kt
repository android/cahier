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

package com.example.cahier.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.cahier.data.NoteType
import com.example.cahier.ui.DrawingCanvas
import com.example.cahier.ui.HomeDestination
import com.example.cahier.ui.HomePane
import com.example.cahier.ui.NoteCanvas


@Composable
fun CahierNavHost(
    navController: NavHostController,
    noteType: NoteType?,
    noteId: Long
) {
    NavHost(
        navController = navController,
        startDestination = defineHomeDestination(noteId, noteType)
    ) {
        composable(HomeDestination.route) {

            HomePane(
                navigateToCanvas = { noteId ->
                    navController.navigate("${TextCanvasDestination.route}/$noteId")
                },
                navigateToDrawingCanvas = { noteId ->
                    navController.navigate("${DrawingCanvasDestination.route}/$noteId")
                },
                navigateUp = {
                    navController.navigateUp()
                },
            )
        }
        composable(
            route = TextCanvasDestination.routeWithArgs,
            arguments = listOf(navArgument(TextCanvasDestination.NOTE_ID_ARG) {
                type = NavType.LongType
            })
        ) { navBackStackEntry ->
            NoteCanvas(
                navBackStackEntry = navBackStackEntry,
                onExit = { navController.navigateUp() },
            )
        }
        composable(
            route = DrawingCanvasDestination.routeWithArgs,
            arguments = listOf(navArgument(DrawingCanvasDestination.NOTE_ID_ARG) {
                type = NavType.LongType
            })
        ) { navBackStackEntry ->
            DrawingCanvas(
                navBackStackEntry = navBackStackEntry,
                navigateUp = { navController.navigateUp() },
            )
        }
    }
}

fun defineHomeDestination(noteId: Long, noteType: NoteType?): String {
    if (noteId < 0)  return HomeDestination.route //no valid id passed
    if (NoteType.TEXT == noteType) return "${TextCanvasDestination.route}/$noteId"
    if (NoteType.DRAWING == noteType) return "${DrawingCanvasDestination.route}/$noteId"
    else return HomeDestination.route
}

object TextCanvasDestination : NavigationDestination {
    override val route = "note_canvas"
    const val NOTE_ID_ARG = "noteId"
    val routeWithArgs = "$route/{$NOTE_ID_ARG}"
}


object DrawingCanvasDestination : NavigationDestination {
    override val route = "drawing_canvas"
    const val NOTE_ID_ARG = "noteId"
    val routeWithArgs = "$route/{$NOTE_ID_ARG}"
}