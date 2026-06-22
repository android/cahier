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

package com.example.cahier.developer.brushgraph.data

import android.content.Context
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.encode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cahier.core.ui.CahierTextureBitmapStore
import com.example.cahier.developer.brushdesigner.data.FakeCustomBrushDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class BrushGraphRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeDao: FakeCustomBrushDao
    private lateinit var mockTextureStore: CahierTextureBitmapStore
    private lateinit var repository: DefaultBrushGraphRepository
    private lateinit var repoScope: kotlinx.coroutines.CoroutineScope

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeCustomBrushDao()
        val context = ApplicationProvider.getApplicationContext<Context>()
        mockTextureStore = CahierTextureBitmapStore(context)
        repoScope = kotlinx.coroutines.CoroutineScope(testDispatcher + Job())
        repository = DefaultBrushGraphRepository(fakeDao, mockTextureStore, repoScope, context)
    }

    @After
    fun tearDown() {
        repoScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun importBrushFromUri_withValidBrush_savesToDao() = testScope.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File.createTempFile("test_brush", ".brushfamily", context.cacheDir)

        val family = StockBrushes.marker()
        val outputStream = FileOutputStream(tempFile)
        family.encode(outputStream, mockTextureStore)
        outputStream.close()

        val uriString = android.net.Uri.fromFile(tempFile).toString()
        val result = repository.importBrushFromUri(uriString)

        assertTrue(result)
        val savedEntity = fakeDao.getAutoSaveBrush().first()
        assertNotNull(savedEntity)
        assertEquals(
            com.example.cahier.developer.brushdesigner.data.AUTOSAVE_KEY,
            savedEntity!!.name
        )
        assertTrue(savedEntity.brushBytes.isNotEmpty())

        tempFile.delete()
    }

    @Test
    fun importBrushFromUri_withInvalidBrush_returnsFailure() = testScope.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = File.createTempFile("invalid_brush", ".brushfamily", context.cacheDir)

        val outputStream = FileOutputStream(tempFile)
        outputStream.write(byteArrayOf(1, 2, 3, 4, 5))
        outputStream.close()

        val uriString = android.net.Uri.fromFile(tempFile).toString()
        val result = repository.importBrushFromUri(uriString)

        org.junit.Assert.assertFalse(result)
        assertTrue(fakeDao.getAllCustomBrushes().first().isEmpty())

        tempFile.delete()
    }
}
