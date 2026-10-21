package com.gur0s.chord.provider

import com.gur0s.chord.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.runBlocking

class AudiusProvider(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(Logging)
    },
    private val apiKey: String? = null,
) : MusicProvider {
    override val source = TrackSource.AUDIUS
    override val name = "Audius"
    override val baseUrl = "https://api.audius.co"
    override val requiresApiKey = false
    override val rateLimitPerMinute = 60

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun search(query: String, limit: Int = 20, offset: Int = 0): Result<SearchResult> {
        return try {
            val response = httpClient.get("$baseUrl/v1/tracks/search") {
                parameter("query", query)
                parameter("limit", limit)
                parameter("offset", offset)
                apiKey?.let { parameter("app_name", it) }
            }
            parseSearchResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getTrack(id: String): Result<Track> {
        return try {
            val response = httpClient.get("$baseUrl/v1/tracks/$id") {
                apiKey?.let { parameter("app_name", it) }
            }
            parseTrackResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getArtist(id: String): Result<Artist> {
        return try {
            val response = httpClient.get("$baseUrl/v1/users/$id") {
                apiKey?.let { parameter("app_name", it) }
            }
            parseArtistResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbum(id: String): Result<Album> {
        return Result.failure(ProviderError.UNSUPPORTED_OPERATION("Albums not directly supported by Audius API"))
    }

    override suspend fun getArtistTopTracks(artistId: String, limit: Int = 10): Result<List<Track>> {
        return try {
            val response = httpClient.get("$baseUrl/v1/users/$artistId/tracks") {
                parameter("limit", limit)
                apiKey?.let { parameter("app_name", it) }
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbumTracks(albumId: String): Result<List<Track>> {
        return Result.failure(ProviderError.UNSUPPORTED_OPERATION("Albums not directly supported by Audius API"))
    }

    override suspend fun getRecommendations(basedOn: List<String>, limit: Int = 20): Result<List<Track>> {
        return try {
            val response = httpClient.get("$baseUrl/v1/tracks/trending") {
                parameter("limit", limit)
                parameter("time", "week")
                apiKey?.let { parameter("app_name", it) }
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override fun getStreamUrl(track: Track): String = track.streamUrl

    override fun getArtworkUrl(track: Track, size: Int = 300): String? = track.artworkUrl

    override fun supportsOfflineDownload(): Boolean = false // Audius terms don't permit offline downloads

    override fun getLicenseInfo(track: Track): LicenseInfo = track.license

    private suspend fun parseSearchResponse(response: HttpResponse): Result<SearchResult> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val data = obj["data"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No data array"))
        val tracks = data.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(SearchResult(tracks = tracks, artists = emptyList(), albums = emptyList(), playlists = emptyList()))
    }

    private suspend fun parseTrackResponse(response: HttpResponse): Result<Track> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val data = obj["data"]?.jsonObject ?: return Result.failure(ProviderError.INVALID_RESPONSE("No track data"))
        parseTrackFromJson(data)?.let { Result.success(it) } ?: Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse track"))
    }

    private suspend fun parseArtistResponse(response: HttpResponse): Result<Artist> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val data = obj["data"]?.jsonObject ?: return Result.failure(ProviderError.INVALID_RESPONSE("No artist data"))
        val artist = parseArtistFromJson(data) ?: return Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse artist"))
        Result.success(artist)
    }

    private suspend fun parseTrackListResponse(response: HttpResponse): Result<List<Track>> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val data = obj["data"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No data array"))
        val tracks = data.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(tracks)
    }

    private fun parseTrackFromJson(obj: JsonObject): Track? {
        val id = obj["track_id"]?.toString() ?: obj["id"]?.toString() ?: return null
        val title = obj["title"]?.toString() ?: return null
        val user = obj["user"]?.jsonObject
        val artistName = user?.["name"]?.toString() ?: user?.["handle"]?.toString() ?: "Unknown Artist"
        val duration = obj["duration"]?.toString()?.toLong() ?: 0L
        val artworkUrl = obj["artwork"]?.jsonObject?.["150x150"]?.toString()
            ?: obj["artwork"]?.jsonObject?.["480x480"]?.toString()
            ?: obj["artwork"]?.jsonObject?.["1000x1000"]?.toString()
        val streamUrl = "$baseUrl/v1/tracks/$id/stream"
        val license = LicenseInfo(
            type = LicenseType.CUSTOM,
            url = "https://audius.co/terms",
            attributionRequired = true,
            commercialUseAllowed = false,
            modificationsAllowed = false,
        )
        Track(
            id = id,
            title = title,
            artist = artistName,
            album = null,
            duration = duration * 1000, // convert to ms
            artworkUrl = artworkUrl,
            streamUrl = streamUrl,
            source = TrackSource.AUDIUS,
            license = license,
        )
    }

    private fun parseArtistFromJson(obj: JsonObject): Artist? {
        val id = obj["user_id"]?.toString() ?: obj["id"]?.toString() ?: return null
        val name = obj["name"]?.toString() ?: obj["handle"]?.toString() ?: return null
        val imageUrl = obj["profile_picture"]?.jsonObject?.["150x150"]?.toString()
            ?: obj["profile_picture"]?.jsonObject?.["480x480"]?.toString()
        Artist(
            id = id,
            name = name,
            imageUrl = imageUrl,
            bio = obj["bio"]?.toString(),
            verified = obj["verified"]?.toString()?.toBoolean() ?: false,
            source = TrackSource.AUDIUS,
        )
    }

    private fun mapError(e: Exception): ProviderError = when {
        e is io.ktor.client.call.NoTransformationFoundException -> ProviderError.INVALID_RESPONSE(e.message ?: "Invalid response")
        e is io.ktor.client.call.HttpResponseException -> when (e.response.status) {
            HttpStatusCode.TooManyRequests -> ProviderError.RATE_LIMITED("Rate limit exceeded", true, 429)
            HttpStatusCode.NotFound -> ProviderError.NOT_FOUND("Resource not found", false, 404)
            HttpStatusCode.Unauthorized -> ProviderError.AUTH_REQUIRED("Authentication required", false, 401)
            HttpStatusCode.Forbidden -> ProviderError.QUOTA_EXCEEDED("API quota exceeded", false, 403)
            else -> ProviderError.NETWORK_ERROR("HTTP ${e.response.status}", true, e.response.status.value)
        }
        else -> ProviderError.NETWORK_ERROR(e.message ?: "Network error", true, null)
    }

    companion object {
        fun createWithDefaults(): AudiusProvider = AudiusProvider()
    }
}