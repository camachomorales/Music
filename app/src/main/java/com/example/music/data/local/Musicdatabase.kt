package com.example.music.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==================== ENTIDADES ====================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val userName: String,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val userId: String?, // null = local only, non-null = sincronizado con usuario
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtUri: String? = null,
    val isStreaming: Boolean = false,
    val streamingId: String? = null,
    val streamingProvider: String? = null
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: String,
    val songId: Long,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long? = null, // Para canciones locales
    val streamingSongId: String? = null, // Para streaming
    val userId: String?, // null = local only
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val userId: String?, // null = local only
    val playedAt: Long = System.currentTimeMillis()
)

// ==================== DATA CLASS CON RELACIONES ====================

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

// ==================== DAOs ====================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>
}

@Dao
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlists WHERE userId = :userId OR userId IS NULL ORDER BY updatedAt DESC")
    fun getPlaylistsForUser(userId: String?): Flow<List<PlaylistWithSongs>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removePlaylistSong(playlistId: String, songId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: String)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongs(playlistId: String): List<PlaylistSongCrossRef>
}

@Dao
interface FavoriteDao {
    @Query("""
        SELECT * FROM favorites 
        WHERE (userId = :userId OR userId IS NULL) 
        AND (songId IS NOT NULL OR streamingSongId IS NOT NULL)
        ORDER BY addedAt DESC
    """)
    fun getFavoritesForUser(userId: String?): Flow<List<FavoriteEntity>>

    @Query("SELECT songId FROM favorites WHERE userId = :userId OR userId IS NULL")
    fun getFavoriteSongIds(userId: String?): Flow<List<Long>>

    @Query("SELECT streamingSongId FROM favorites WHERE (userId = :userId OR userId IS NULL) AND streamingSongId IS NOT NULL")
    fun getFavoriteStreamingSongIds(userId: String?): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId AND (userId = :userId OR userId IS NULL)")
    suspend fun deleteFavoriteBySongId(songId: Long, userId: String?)

    @Query("DELETE FROM favorites WHERE streamingSongId = :streamingSongId AND (userId = :userId OR userId IS NULL)")
    suspend fun deleteFavoriteByStreamingSongId(streamingSongId: String, userId: String?)
}

@Dao
interface RecentlyPlayedDao {
    @Query("""
        SELECT * FROM recently_played 
        WHERE userId = :userId OR userId IS NULL 
        ORDER BY playedAt DESC 
        LIMIT 50
    """)
    fun getRecentlyPlayedForUser(userId: String?): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(recentlyPlayed: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played WHERE userId = :userId OR userId IS NULL")
    suspend fun clearRecentlyPlayed(userId: String?)
}

// ==================== DATABASE ====================

@Database(
    entities = [
        UserEntity::class,
        PlaylistEntity::class,
        SongEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        RecentlyPlayedEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao

    companion object {
        @Volatile
        private var INSTANCE: MusicDatabase? = null

        fun getDatabase(context: Context): MusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}