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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class MusicBrainzProvider(
    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(Logging)
    },
    private val userAgent: String = "Chord/1.0.0 (gur0s@chord.app)", // Must identify app per MB policy
) : MusicProvider {
    override val source = TrackSource.MUSICBRAINZ
    override val name = "MusicBrainz"
    override val baseUrl = "https://musicbrainz.org/ws/2"
    override val requiresApiKey = false
    override val rateLimitPerMinute = 50 // MB rate limit: 1 req/sec average

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val coverArtBaseUrl = "https://coverartarchive.org"

    override suspend fun search(query: String, limit: Int = 20, offset: Int = 0): Result<SearchResult> {
        return try {
            val response = httpClient.get("$baseUrl/recording/") {
                parameter("query", query)
                parameter("limit", limit)
                parameter("offset", offset)
                parameter("fmt", "json")
                header("User-Agent", userAgent)
            }
            parseSearchResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getTrack(id: String): Result<Track> {
        return try {
            val response = httpClient.get("$baseUrl/recording/$id") {
                parameter("fmt", "json")
                parameter("inc", "artists+releases+tags")
                header("User-Agent", userAgent)
            }
            parseTrackResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getArtist(id: String): Result<Artist> {
        return try {
            val response = httpClient.get("$baseUrl/artist/$id") {
                parameter("fmt", "json")
                parameter("inc", "aliases+tags+ratings")
                header("User-Agent", userAgent)
            }
            parseArtistResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbum(id: String): Result<Album> {
        return try {
            val response = httpClient.get("$baseUrl/release-group/$id") {
                parameter("fmt", "json")
                parameter("inc", "artists+releases+tags")
                header("User-Agent", userAgent)
            }
            parseAlbumResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getArtistTopTracks(artistId: String, limit: Int = 10): Result<List<Track>> {
        return try {
            val response = httpClient.get("$baseUrl/recording/") {
                parameter("query", "artist:$artistId")
                parameter("limit", limit)
                parameter("fmt", "json")
                header("User-Agent", userAgent)
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getAlbumTracks(albumId: String): Result<List<Track>> {
        return try {
            val response = httpClient.get("$baseUrl/recording/") {
                parameter("query", "releases:$albumId")
                parameter("fmt", "json")
                parameter("inc", "artists")
                header("User-Agent", userAgent)
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override suspend fun getRecommendations(basedOn: List<String>, limit: Int = 20): Result<List<Track>> {
        // MusicBrainz doesn't have recommendations; return recent releases
        return try {
            val response = httpClient.get("$baseUrl/recording/") {
                parameter("query", "status:official")
                parameter("limit", limit)
                parameter("fmt", "json")
                header("User-Agent", userAgent)
            }
            parseTrackListResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    override fun getStreamUrl(track: Track): String {
        // MusicBrainz doesn't host audio; return empty, handled by other providers
        return ""
    }

    override fun getArtworkUrl(track: Track, size: Int = 300): String? {
        val mbid = track.id.removePrefix("mb_")
        return "$coverArtBaseUrl/release/$mbid/front-$size.jpg"
    }

    override fun supportsOfflineDownload(): Boolean = false // Metadata only

    override fun getLicenseInfo(track: Track): LicenseInfo = LicenseInfo(
        type = LicenseType.CUSTOM,
        url = "https://musicbrainz.org/doc/MusicBrainz_License",
        attributionRequired = true,
        commercialUseAllowed = false,
        modificationsAllowed = true,
    )

    override suspend fun getAlbum(id: String): Result<Album> {
        return try {
            val response = httpClient.get("$baseUrl/release-group/$id") {
                parameter("fmt", "json")
                parameter("inc", "artists+releases+tags")
                header("User-Agent", userAgent)
            }
            parseAlbumResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    private suspend fun parseSearchResponse(response: HttpResponse): Result<SearchResult> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val recordings = obj["recordings"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No recordings array"))
        val tracks = recordings.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(SearchResult(tracks = tracks, artists = emptyList(), albums = emptyList(), playlists = emptyList()))
    }

    override suspend fun getTrack(id: String): Result<Track> {
        return try {
            val response = httpClient.get("$baseUrl/recording/$id") {
                parameter("fmt", "json")
                parameter("inc", "artists+releases+tags")
                header("User-Agent", userAgent)
            }
            parseTrackResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    private suspend fun parseTrackResponse(response: HttpResponse): Result<Track> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        parseTrackFromJson(obj)?.let { Result.success(it) }
            ?: Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse track"))
    }

    private suspend fun parseArtistResponse(response: HttpResponse): Result<Artist> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        parseArtistFromJson(obj)?.let { Result.success(it) }
            ?: Result.failure(ProviderError.INVALID_RESPONSE("Failed to parse artist"))
    }

    private suspend fun parseTrackListResponse(response: HttpResponse): Result<List<Track>> {
        val body = response.bodyAsText()
        val obj = json.parseToJsonElement(body).jsonObject
        val recordings = obj["recordings"]?.jsonArray ?: return Result.failure(ProviderError.INVALID_RESPONSE("No recordings array"))
        val tracks = recordings.mapNotNull { parseTrackFromJson(it.jsonObject) }
        Result.success(tracks)
    }

    private fun parseTrackFromJson(obj: JsonObject): Track? {
        val id = obj["id"]?.toString() ?: return null
        val title = obj["title"]?.toString() ?: return null
        val artistCredit = obj["artist-credit"]?.jsonArray
        val artistName = artistCredit?.firstOrNull()?.jsonObject?.get("artist")?.jsonObject?.["name"]?.toString() ?: "Unknown Artist"
        val length = obj["length"]?.toString()?.toLong() ?: 0L
        val releaseList = obj["releases"]?.jsonArray
        val albumName = releaseList?.firstOrNull()?.jsonObject?.["title"]?.toString()
        val artworkUrl = releaseList?.firstOrNull()?.jsonObject?.["id"]?.let { "https://coverartarchive.org/release/$it/front-300.jpg" }
        val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonObject["name"]?.toString() } ?: emptyList()
        val explicit = tags.any { it.lowercase().contains("explicit") }
        Track(
            id = "mb_$id",
            title = title,
            artist = obj["artist-credit"]?.jsonArray?.firstOrNull()?.jsonObject?.get("artist")?.jsonObject?.["name"]?.toString() ?: "Unknown Artist",
            album = releaseList?.firstOrNull()?.jsonObject?.["title"]?.toString(),
            duration = (obj["length"]?.toString()?.toLong() ?: 0L),
            artworkUrl = releaseList?.firstOrNull()?.jsonObject?.["id"]?.let { "https://coverartarchive.org/release/$it/front-300.jpg" },
            streamUrl = "", // MB doesn't host audio
            source = TrackSource.MUSICBRAINZ,
            license = LicenseInfo(LicenseType.CUSTOM, "https://musicbrainz.org/doc/MusicBrainz_License", true, false, true),
            explicit = obj["tags"]?.jsonArray?.any { it.jsonObject["name"]?.toString()?.lowercase().contains("explicit") == true } ?: false,
        )
    }

    private fun parseArtistFromJson(obj: JsonObject): Artist? {
        val id = obj["id"]?.toString() ?: return null
        val name = obj["name"]?.toString() ?: return null
        Artist(id = "mb_$id", name = name, imageUrl = null, bio = obj["disambiguation"]?.toString(), verified = false, source = TrackSource.MUSICBRAINZ)
    }

    private fun mapError(e: Exception): ProviderError = when {
        e is io.ktor.client.call.HttpResponseException -> when (e.response.status) {
            io.ktor.http.HttpStatusCode.TooManyRequests -> ProviderError.RATE_LIMITED("Rate limit exceeded", true, 429)
            io.ktor.http.HttpStatusCode.NotFound -> ProviderError.NOT_FOUND("Resource not found", false, 404)
            else -> ProviderError.NETWORK_ERROR("HTTP ${e.response.status}", true, e.response.status.value)
        }
        else -> ProviderError.NETWORK_ERROR(e.message ?: "Network error", true, null)
    }

    companion object {
        fun createWithUserAgent(userAgent: String): MusicBrainzProvider = MusicBrainzProvider(userAgent = userAgent)
    }
}