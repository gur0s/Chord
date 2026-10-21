package com.gur0s.chord.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT * FROM history WHERE trackId = :trackId ORDER BY playedAt DESC LIMIT 1")
    suspend fun getLastPlayed(trackId: String): HistoryEntity?

    @Query("SELECT * FROM history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HistoryEntity>

    @Query("SELECT DISTINCT trackId FROM history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getRecentTrackIds(limit: Int): List<String>

    @Query("DELETE FROM history WHERE playedAt < :cutoff")
    suspend fun cleanupOld(cutoff: Long): Int
}

@Dao
interface LikedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(liked: LikedTrackEntity)

    @Delete
    suspend fun delete(liked: LikedTrackEntity)

    @Query("SELECT * FROM liked_tracks WHERE trackId = :trackId")
    suspend fun isLiked(trackId: String): Boolean

    @Query("SELECT trackId FROM liked_tracks ORDER BY likedAt DESC")
    suspend fun getAllLikedIds(): List<String>

    @Query("SELECT COUNT(*) FROM liked_tracks")
    suspend fun count(): Int
}