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

package com.example.cahier.core.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.DragAndDropPermissions
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import com.example.cahier.AppArgs
import com.example.cahier.core.data.Note

@OptIn(ExperimentalFoundationApi::class)
fun createDropTarget(
    activity: ComponentActivity?,
    onUriReceived: (Uri, DragAndDropPermissions?) -> Unit
): DragAndDropTarget {
    return object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val dragEvent = event.toAndroidDragEvent()
            val permission = activity?.requestDragAndDropPermissions(dragEvent)
            val uri = dragEvent.clipData.getItemAt(0)?.uri
            uri?.let {
                onUriReceived(it, permission)
            }
            return true
        }
    }
}

fun Modifier.createDragAndDropSource(
    activity: Activity?,
    note: Note
): Modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
    dragAndDropSource { _ ->
        activity?.let {
            DragAndDropTransferData(
                clipData = getClipData(it, note),
                flags = View.DRAG_FLAG_GLOBAL_SAME_APPLICATION or View.DRAG_FLAG_START_INTENT_SENDER_ON_UNHANDLED_DRAG,
            )
        }
    }
} else {
    this
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun getClipData(activity: Activity, note: Note): ClipData {
    val componentName = activity.componentName
    val intent = Intent.makeMainActivity(componentName).apply {
        putExtra(AppArgs.NOTE_ID_KEY, note.id)
        putExtra(AppArgs.NOTE_TYPE_KEY, note.type)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
    }
    val pendingIntent = PendingIntent.getActivity(
        activity, 0, intent, PendingIntent.FLAG_IMMUTABLE
    )
    return ClipData(
        AppArgs.NOTE_ID_KEY, arrayOf(ClipDescription.MIMETYPE_TEXT_INTENT),
        ClipData.Item.Builder().setIntentSender(pendingIntent.intentSender).build()
    )
}
