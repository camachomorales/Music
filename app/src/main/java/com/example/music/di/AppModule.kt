package com.example.music.di

import com.google.android.datatransport.runtime.dagger.Module

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "music_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase) = database.playlistDao()

    @Provides
    fun provideSongDao(database: AppDatabase) = database.songDao()

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase) = database.searchHistoryDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")