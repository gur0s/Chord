package com.gur0s.chord.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)

    @Update
    suspend fun update(track: TrackEntity)

    @Delete
    suspend fun delete(track: TrackEntity)

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks ORDER BY lastPlayedAt DESC NULLS LAST LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    suspend fun getMostPlayed(limit: Int): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE source = :source")
    suspend fun getBySource(source: String): List<TrackEntity>

    @Query("DELETE FROM tracks WHERE lastPlayedAt IS NOT NULL AND lastPlayedAt < :cutoff")
    suspend fun cleanupOldEntries(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int
}