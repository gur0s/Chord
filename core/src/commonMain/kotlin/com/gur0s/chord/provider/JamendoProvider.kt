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

class JamendoProvider(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(Logging)
    },
    private val clientId: String = "YOUR_JAMENDO_CLIENT_ID", // Must be configured
) : MusicProvider {
    override val source = TrackSource.JAMENDO
    override val name = "Jamendo"
    override val baseUrl = "https://api.jamendo.com/v3.0"
    override val requiresApiKey = true
    override val rateLimitPerMinute = 60

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun search(query: String, limit: Int = 20, offset: Int = 0): Result<SearchResult> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("search", query)
                parameter("limit", limit)
                parameter("offset", offset)
                parameter("format", "json")
                parameter("include", "musicinfo")
                parameter("order", "relevance_desc")
            }
            parseSearchResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getTrack(id: String): Result<Track> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("ids", id)
                parameter("format", "json")
                parameter("include", "musicinfo")
            }
            parseTrackResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getArtist(id: String): Result<Artist> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/artists/") {
                parameter("client_id", clientId)
                parameter("id", id)
                parameter("format", "json")
            }
            parseArtistResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbum(id: String): Result<Album> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/albums/") {
                parameter("client_id", clientId)
                parameter("id", id)
                parameter("format", "json")
            }
            parseAlbumResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getArtistTopTracks(artistId: String, limit: Int = 10): Result<List<Track>> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("artist_id", artistId)
                parameter("limit", limit)
                parameter("format", "json")
                parameter("include", "musicinfo")
                parameter("order", "popularity_total_desc")
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbumTracks(albumId: String): Result<List<Track>> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("album_id", albumId)
                parameter("format", "json")
                parameter("include", "musicinfo")
                parameter("order", "track_number_asc")
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getRecommendations(basedOn: List<String>, limit: Int = 20): Result<List<Track>> {
        return if (clientId == "YOUR_JAMENDO_CLIENT_ID" || clientId.isBlank()) {
            Result.failure(ProviderError.AUTH_REQUIRED("Jamendo client_id not configured"))
        } else try {
            val response = httpClient.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("limit", limit)
                parameter("format", "json")
                parameter("include", "musicinfo")
                parameter("order", "popularity_total_desc")
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override fun getStreamUrl(track: Track): String = track.streamUrl

    override fun getArtworkUrl(track: Track, size: Int = 300): String? = track.artworkUrl

    override fun supportsOfflineDownload(): Boolean = true // Jamendo CC licenses often allow download

    override fun getLicenseInfo(track: Track): LicenseInfo = track.license

    private suspend fun parseSearchResponse(response: HttpResponse): Result<SearchResult> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val results = obj["results"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No results array"))
        val tracks = results.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(SearchResult(tracks = tracks, artists = emptyList(), albums = emptyList(), playlists = emptyList()))
    }

    private suspend fun parseTrackResponse(response: HttpResponse): Result<Track> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val results = obj["results"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No results array"))
        if (results.isEmpty()) return Result.failure(ProviderError.NOT_FOUND("Track not found"))
        parseTrackFromJson(results[0].jsonObject)?.let { Result.success(it) }
            ?: Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse track"))
    }

    private suspend fun parseArtistResponse(response: HttpResponse): Result<Artist> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val results = obj["results"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No results array"))
        if (results.isEmpty()) return Result.failure(ProviderError.NOT_FOUND("Artist not found"))
        parseArtistFromJson(results[0].jsonObject)?.let { Result.success(it) }
            ?: Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse artist"))
    }

    private suspend fun parseTrackListResponse(response: HttpResponse): Result<List<Track>> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val results = obj["results"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No results array"))
        val tracks = results.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(tracks)
    }

    private fun parseTrackFromJson(obj: JsonObject): Track? {
        val id = obj["id"]?.toString() ?: return null
        val title = obj["name"]?.toString() ?: return null
        val artistName = obj["artist_name"]?.toString() ?: "Unknown Artist"
        val duration = obj["duration"]?.toString()?.toLong() ?: 0L
        val artworkUrl = obj["image"]?.toString()
        val streamUrl = obj["audiodownload_allowed"]?.toString()?.toBoolean() == true
            ? obj["audiodownload"]?.toString()
            : obj["audio"]?.toString()
        val licenseStr = obj["license"]?.toString()
        val license = parseJamendoLicense(licenseStr)
        streamUrl?.let {
            Track(
                id = "jamendo_$id",
                title = obj["name"]?.toString() ?: "Unknown",
                artist = obj["artist_name"]?.toString() ?: "Unknown Artist",
                album = obj["album_name"]?.toString(),
                duration = (obj["duration"]?.toString()?.toLong() ?: 0L) * 1000,
                artworkUrl = obj["image"]?.toString(),
                streamUrl = it,
                source = TrackSource.JAMENDO,
                license = license,
                explicit = obj["explicit_lyrics"]?.toString()?.toBoolean() ?: false,
            )
        }
    }

    private fun parseArtistFromJson(obj: JsonObject): Artist? {
        val id = obj["id"]?.toString() ?: return null
        val name = obj["name"]?.toString() ?: return null
        val imageUrl = obj["image"]?.toString()
        Artist(id = "jamendo_$id", name = name, imageUrl = obj["image"]?.toString(), bio = obj["description"]?.toString(), verified = false, source = TrackSource.JAMENDO)
    }

    private fun parseJamendoLicense(licenseStr: String?): LicenseInfo {
        return when (licenseStr?.lowercase()) {
            "cc-by" -> LicenseInfo(LicenseType.CC_BY, "https://creativecommons.org/licenses/by/4.0/", true, true, true)
            "cc-by-sa" -> LicenseInfo(LicenseType.CC_BY_SA, "https://creativecommons.org/licenses/by-sa/4.0/", true, true, true)
            "cc-by-nc" -> LicenseInfo(LicenseType.CC_BY_NC, "https://creativecommons.org/licenses/by-nc/4.0/", true, false, true)
            "cc-by-nc-sa" -> LicenseInfo(LicenseType.CC_BY_NC_SA, "https://creativecommons.org/licenses/by-nc-sa/4.0/", true, false, true)
            "cc-by-nd" -> LicenseInfo(LicenseType.CC_BY_ND, "https://creativecommons.org/licenses/by-nd/4.0/", true, true, false)
            "cc-by-nc-nd" -> LicenseInfo(LicenseType.CC_BY_NC_ND, "https://creativecommons.org/licenses/by-nc-nd/4.0/", true, false, false)
            else -> LicenseInfo(LicenseType.CUSTOM, "https://www.jamendo.com/licensing", true, false, false)
        }
    }

    private fun mapError(e: Exception): ProviderError = when {
        e is io.ktor.client.call.HttpResponseException -> when (e.response.status) {
            io.ktor.http.HttpStatusCode.TooManyRequests -> ProviderError.RATE_LIMITED("Rate limit exceeded", true, 429)
            io.ktor.http.HttpStatusCode.NotFound -> ProviderError.NOT_FOUND("Resource not found", false, 404)
            io.ktor.http.HttpStatusCode.Unauthorized -> ProviderError.AUTH_REQUIRED("Invalid client_id", false, 401)
            io.ktor.http.HttpStatusCode.Forbidden -> ProviderError.QUOTA_EXCEEDED("API quota exceeded", false, 403)
            else -> ProviderError.NETWORK_ERROR("HTTP ${e.response.status}", true, e.response.status.value)
        }
        else -> ProviderError.NETWORK_ERROR(e.message ?: "Network error", true, null)
    }

    companion object {
        fun createWithClientId(clientId: String): JamendoProvider = JamendoProvider(clientId = clientId)
    }
}