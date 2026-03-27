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

package com.example.cahier

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isCaptionBarVisible
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.cahier.core.data.NoteType
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.core.ui.LocalTextureStore
import com.example.cahier.core.ui.theme.CahierAppTheme
import com.example.cahier.developer.brushgraph.data.BrushGraphRepository
import com.example.cahier.developer.brushgraph.data.DisplayText
import com.example.cahier.features.home.CahierApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @javax.inject.Inject
    lateinit var textureStore: CahierTextureBitmapStore

    @javax.inject.Inject
    lateinit var repository: dagger.Lazy<BrushGraphRepository>

    private val noteIdState = androidx.compose.runtime.mutableLongStateOf(-1L)
    private val noteTypeState = androidx.compose.runtime.mutableStateOf<NoteType?>(null)
    private val navigateToBrushGraphState = androidx.compose.runtime.mutableStateOf(false)

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d(
            "MainActivity",
            "onCreate called with intent: $intent, data: ${intent?.data}, action: ${intent?.action}"
        )
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val noteId = noteIdState.longValue
            val noteType = noteTypeState.value
            val navigateToBrushGraph = navigateToBrushGraphState.value

            CahierAppTheme {
                val isCaptionBarVisible = WindowInsets.isCaptionBarVisible
                LaunchedEffect(isCaptionBarVisible) {
                    if (isCaptionBarVisible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        window.insetsController?.setSystemBarsAppearance(
                            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND,
                            WindowInsetsController.APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND
                        )
                    }
                }
                androidx.compose.runtime.CompositionLocalProvider(LocalTextureStore provides textureStore) {
                    Surface(
                        modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CahierApp(noteId = noteId,
                            noteType = noteType,
                            navigateToBrushGraph = navigateToBrushGraph,
                            onNavigateToBrushGraphHandled = {
                                navigateToBrushGraphState.value = false
                            },
                            textureStore = textureStore
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        android.util.Log.d(
            "MainActivity",
            "onNewIntent called with intent: $intent, data: ${intent.data}, action: ${intent.action}"
        )
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val noteId = intent.getLongExtra(AppArgs.NOTE_ID_KEY, -1)
        val noteType = IntentCompat.getParcelableExtra(
            intent,
            AppArgs.NOTE_TYPE_KEY,
            NoteType::class.java
        )
        noteIdState.longValue = noteId
        noteTypeState.value = noteType

        val uri = intent.data
        android.util.Log.d(
            "MainActivity",
            "handleIntent: action=${intent.action}, uri=$uri, scheme=${uri?.scheme}, noteId=$noteId, noteType=$noteType"
        )

        if (intent.action == "com.example.cahier.intent.action.IMPORT_BRUSH" && uri != null) {
            intent.action = null
            intent.data = null
            lifecycleScope.launch {
                val repo = repository.get()
                val success = repo.importBrushFromUri(uri.toString())
                if (success) {
                    android.util.Log.d("MainActivity", "Import brush SUCCESS")
                    navigateToBrushGraphState.value = true
                    repo.postDebug(DisplayText.Resource(com.example.cahier.R.string.bg_msg_brush_loaded_success))
                } else {
                    android.util.Log.e("MainActivity", "Import brush FAILED")
                    repo.postDebug(DisplayText.Resource(com.example.cahier.R.string.bg_err_load_brush))
                }
            }
        }
    }
}