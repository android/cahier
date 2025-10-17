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

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FileHelperTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fileHelper: FileHelper

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
    }

    @Test
    fun copyUriToInternalStorage_creates_a_new_file() = runTest {
        val sourceFile = File(context.cacheDir, "source.txt")
        sourceFile.writeText("hello world")
        val sourceUri = sourceFile.toUri()

        val resultUri = fileHelper.copyUriToInternalStorage(sourceUri)

        assertNotNull(resultUri)
        val resultFile = File(resultUri.path!!)
        assertTrue(resultFile.exists())
        assertEquals("hello world", resultFile.readText())

        sourceFile.delete()
        resultFile.delete()
    }

    @Test
    fun saveBitmapToCache_creates_a_new_file() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val resultUri = fileHelper.saveBitmapToCache(bitmap)

        assertNotNull(resultUri)
        assertEquals(ContentResolver.SCHEME_CONTENT, resultUri.scheme)

        val inputStream = contentResolver.openInputStream(resultUri)
        assertNotNull(inputStream)
        inputStream?.close()
    }

    @Test
    fun createShareableUri_creates_a_new_file() = runTest {
        val internalFile = File(context.filesDir, "internal.txt")
        internalFile.writeText("share me")

        val resultUri = fileHelper.createShareableUri(internalFile)

        assertNotNull(resultUri)
        assertEquals(ContentResolver.SCHEME_CONTENT, resultUri.scheme)

        internalFile.delete()
    }
}