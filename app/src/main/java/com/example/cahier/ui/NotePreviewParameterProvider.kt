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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.cahier.data.Note
import com.example.cahier.data.NoteType

class NotePreviewParameterProvider : PreviewParameterProvider<Note> {
    override val values = sequenceOf(
        Note(
            id = 1,
            title = "Favorite Note",
            text = "This is a favorite text note with an image.",
            isFavorite = true,
            type = NoteType.Text,
            imageUriList = listOf("image_uri_placeholder")
        ),
        Note(
            id = 2,
            title = "Drawing Note",
            isFavorite = true,
            type = NoteType.Drawing
        ),
        Note(
            id = 3,
            title = "Long note",
            text = "This is a regular note with a very long body of text to see how it wraps and" +
                    "truncates within the UI.",
            type = NoteType.Text
        ),
        Note(
            id = 4,
            title = "",
            text = "This is an untitled note.",
            type = NoteType.Text
        )
    )
}