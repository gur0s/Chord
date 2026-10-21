package com.gur0s.chord.repository

import com.gur0s.chord.domain.*
import com.gur0s.chord.db.*
import com.gur0s.chord.provider.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow

class ChordRepository(
    private val db: ChordDatabase,
    private val providerRegistry: ProviderRegistry,
) {
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val historyDao = db.historyDao()
    private val likedDao = db.likedDao()

    // Search
    suspend fun search(query: String, limit: Int = 20): Result<SearchResult> {
        return providerRegistry.searchAll(query, limit)
            .let { tracks ->
                Result.success(SearchResult(tracks = tracks, artists = emptyList(), albums = emptyList(), playlists = emptyList()))
            }
    }

    suspend fun getTrack(id: String): Result<Track> {
        val source = TrackSource.valueOf(id.substringBefore("_"))
        return providerRegistry.getProvider(source)?.getTrack(id) 
            ?: Result.failure(ProviderError.UNSUPPORTED_OPERATION("No provider for $source"))
    }

    suspend fun getArtist(id: String): Result<Artist> {
        val source = TrackSource.valueOf(id.substringBefore("_"))
        return providerRegistry.getProvider(source)?.getArtist(id)
            ?: Result.failure(ProviderError.UNSUPPORTED_OPERATION("No provider for $source"))
    }

    suspend fun getAlbum(id: String): Result<Album> {
        val source = TrackSource.valueOf(id.substringBefore("_"))
        return providerRegistry.getProvider(source)?.getAlbum(id)
            ?: Result.failure(ProviderError.UNSUPPORTED_OPERATION("No provider for $source"))
    }

    // Likes
    suspend fun toggleLike(track: Track) {
        if (likedDao.isLiked(track.id)) {
            likedDao.delete(LikedTrackEntity(track.id))
        } else {
            likedDao.insert(LikedTrackEntity(track.id))
        }
    }

    suspend fun isLiked(trackId: String): Boolean = likedDao.isLiked(trackId)

    val likedTracks: Flow<List<TrackEntity>> = likedDao.getAllLikedIds().map { ids ->
        trackDao.getByIds(ids)
    }.distinctUntilChanged()

    // Playlists
    suspend fun createPlaylist(name: String, description: String? = null): PlaylistEntity {
        val id = java.util.UUID.randomUUID().toString()
        val playlist = PlaylistEntity(id = id, name = name, description = description, artworkUrl = null)
        playlistDao.insert(playlist)
        return playlist
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.addTrack(PlaylistTrackEntity(playlistId, track.id, maxPos + 1))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrack(playlistId, trackId)
    }

    suspend fun reorderPlaylistTracks(playlistId: String, trackIds: List<String>) {
        playlistDao.reorderTracks(playlistId, trackIds)
    }

    val userPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getUserPlaylists().distinctUntilChanged()

    suspend fun getPlaylistTracks(playlistId: String): List<TrackEntity> {
        val trackEntities = playlistDao.getTracks(playlistId).map { pt -> trackDao.getById(pt.trackId) }.filterNotNull()
        return trackEntities
    }

    // History
    suspend fun recordPlayback(trackId: String, position: Long, completed: Boolean = false) {
        historyDao.insert(HistoryEntity(trackId = trackId, position = position, completed = completed))
        trackDao.update(TrackEntity.fromTrack(
            // We need to update lastPlayedAt and playCount
            trackDao.getById("")?.copy(lastPlayedAt = System.currentTimeMillis(), playCount = 1) 
                ?? throw IllegalArgumentException("Track not found")
        ))
    }

    val recentHistory: Flow<List<HistoryEntity>> = historyDao.getRecent(50).distinctUntilChanged()

    // Likes
    suspend fun isLiked(trackId: String): Boolean = likedDao.isLiked(trackId)

    suspend fun getLikedTracks(): List<TrackEntity> {
        val ids = likedDao.getAllLikedIds()
        return trackDao.getByIds(ids)
    }
}