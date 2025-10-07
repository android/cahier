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

package com.example.cahier.ui.utils

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent

@OptIn(ExperimentalFoundationApi::class)
fun createDropTarget(
    activity: Activity,
    onUriReceived: (Uri) -> Unit
): DragAndDropTarget {
    return object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val dragEvent = event.toAndroidDragEvent()
            val permission = activity.requestDragAndDropPermissions(dragEvent)
            if (permission != null) {
                try {
                    val uri = dragEvent.clipData.getItemAt(0)?.uri
                    uri?.let {
                        onUriReceived(it)
                    }
                } finally {
                    permission.release()
                }
            }
            return true
        }
    }
}
