/*
 *
 *  *
 *  *  * Copyright 2025 Google LLC. All rights reserved.
 *  *  *
 *  *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  *  * you may not use this file except in compliance with the License.
 *  *  * You may obtain a copy of the License at
 *  *  *
 *  *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *  *
 *  *  * Unless required by applicable law or agreed to in writing, software
 *  *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  * See the License for the specific language governing permissions and
 *  *  * limitations under the License.
 *  *
 *
 */


package com.example.cahier.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.io.copyTo
import kotlin.io.use

class FileHelper (private val context: Context) {
    /**
     * Copies the content from a given URI to a new file in the app's internal storage.
     * This is crucial for handling URIs from external sources like drag-and-drop,
     * where permissions are temporary.
     *
     * @param uri The content URI to copy from.
     * @return The URI of the newly created local file.
     */
    suspend fun copyUriToInternalStorage(uri: Uri): Uri? {
        return try {
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imagesDir = File(context.filesDir, "images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val outputFile = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
                val outputStream = FileOutputStream(outputFile)
                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                Uri.fromFile(outputFile)
            }
        } catch (e: SecurityException) {
            Log.e("FileHelper", "SecurityException for URI: $uri", e)
            null
        } catch (e: IOException) {
            Log.e("FileHelper", "IOException for URI: $uri", e)
            null
        }
    }

    /**
     * Saves a bitmap to a temporary cache file and returns a shareable content URI.
     * @param bitmap The bitmap to save.
     * @return A shareable content URI for the saved image.
     */
    suspend fun saveBitmapToCache(bitmap: Bitmap): Uri {
        return withContext(Dispatchers.IO) {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_image_${UUID.randomUUID()}.png")
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }
    }

    /**
     * Creates a temporary, shareable copy of a permanent file from internal storage.
     * @param originalFile The File object from internal storage (e.g. in filesDir).
     * @return A secure, shareable content URI for the temporary copy.
     */
    suspend fun createShareableUri(originalFile: File): Uri {
        return withContext(Dispatchers.IO) {
            val shareCachePath = File(context.cacheDir, "shares")
            shareCachePath.mkdirs()

            val tempFile = File(shareCachePath, originalFile.name)

            originalFile.copyTo(tempFile, overwrite = true)

            val authority = "${context.packageName}.provider"
            FileProvider.getUriForFile(context, authority, tempFile)
        }
    }
}