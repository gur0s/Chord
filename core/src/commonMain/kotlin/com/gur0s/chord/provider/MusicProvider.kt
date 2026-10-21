package com.gur0s.chord.provider

import com.gur0s.chord.domain.*
import kotlinx.coroutines.flow.Flow

interface MusicProvider {
    val source: TrackSource
    val name: String
    val baseUrl: String
    val requiresApiKey: Boolean
    val rateLimitPerMinute: Int

    suspend fun search(query: String, limit: Int = 20, offset: Int = 0): Result<SearchResult>
    suspend fun getTrack(id: String): Result<Track>
    suspend fun getArtist(id: String): Result<Artist>
    suspend fun getAlbum(id: String): Result<Album>
    suspend fun getArtistTopTracks(artistId: String, limit: Int = 10): Result<List<Track>>
    suspend fun getAlbumTracks(albumId: String): Result<List<Track>>
    suspend fun getRecommendations(basedOn: List<String>, limit: Int = 20): Result<List<Track>>
    fun getStreamUrl(track: Track): String
    fun getArtworkUrl(track: Track, size: Int = 300): String?
    fun supportsOfflineDownload(): Boolean
    fun getLicenseInfo(track: Track): LicenseInfo
}

sealed interface ProviderResult<out T> {
    data class Success<T>(val data: T) : ProviderResult<T>
    data class Failure(val error: ProviderError) : ProviderResult<Nothing>
}

enum class ProviderError(
    val message: String,
    val isRetryable: Boolean,
    val statusCode: Int?
) {
    NETWORK_ERROR("Network error", true, null),
    RATE_LIMITED("Rate limit exceeded", true, 429),
    NOT_FOUND("Resource not found", false, 404),
    INVALID_RESPONSE("Invalid response format", false, null),
    AUTH_REQUIRED("Authentication required", false, 401),
    QUOTA_EXCEEDED("API quota exceeded", false, 403),
    UNSUPPORTED_OPERATION("Operation not supported by this provider", false, null),
    LICENSE_RESTRICTION("License restriction prevents this action", false, null),
    UNKNOWN("Unknown error", true, null);
    override fun toString() = message
}

typealias Result<T> = ProviderResult<T>

inline fun <T> Result.success(data: T): Result<T> = ProviderResult.Success(data)
inline fun <T> Result.failure(error: ProviderError): Result<T> = ProviderResult.Failure(error)