package com.example.music.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
//import com.example.music.data.local.dao.PlaylistDao
import com.example.music.data.local.dao.SongDao
//import com.example.music.data.local.entity.PlaylistEntity
import com.example.music.data.local.entity.SongEntity


@Database(
    entities = [
        SongEntity::class,
        // PlaylistEntity::class  // COMENTAR TEMPORALMENTE
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    // abstract fun playlistDao(): PlaylistDao  // COMENTAR
}