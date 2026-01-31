package com.example.music.di

import android.content.Context
import androidx.room.Room
import com.example.music.data.local.AppDatabase
import com.example.music.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "music_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavoriteLocalSongDao(database: AppDatabase) =
        database.favoriteLocalSongDao()

    @Provides
    fun provideFavoriteStreamingSongDao(database: AppDatabase) =
        database.favoriteStreamingSongDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase) =
        database.playlistDao()

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase) =
        database.searchHistoryDao()
}