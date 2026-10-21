package com.gur0s.chord.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        HistoryEntity::class,
        LikedTrackEntity::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class ChordDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun likedDao(): LikedDao

    companion object {
        @Volatile private var INSTANCE: ChordDatabase? = null

        fun getInstance(context: android.content.Context): ChordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    ChordDatabase::class.java,
                    "chord_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }

        @Suppress("UNUSED_PARAMETER")
        fun getDesktopInstance(): ChordDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: androidx.room.Room.databaseBuilder(
                    java.io.File("chord.db").absolutePath,
                    ChordDatabase::class.java,
                    "chord_database"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}