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

package com.example.cahier.core.di

import android.content.Context
import androidx.room.Room
import coil3.ImageLoader
import com.example.cahier.core.data.MIGRATION_7_8
import com.example.cahier.core.data.MIGRATION_8_9
import com.example.cahier.core.data.NoteDatabase
import com.example.cahier.core.data.NotesRepository
import com.example.cahier.core.data.OfflineNotesRepository
import com.example.cahier.core.utils.FileHelper
import com.example.cahier.developer.brushdesigner.data.CustomBrushDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideNoteDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .build()
    }

    @Provides
    @Singleton
    fun provideCustomBrushDao(database: NoteDatabase): CustomBrushDao {
        return database.customBrushDao()
    }

    @Provides
    @Singleton
    fun provideNoteRepository(
        database: NoteDatabase,
        @ApplicationContext context: Context
    ): NotesRepository {
        return OfflineNotesRepository(database.noteDao(), context)
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader(context)
    }

    @Provides
    @Singleton
    fun provideFileHelper(@ApplicationContext context: Context): FileHelper {
        return FileHelper(context)
    }
}