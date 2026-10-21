package com.gur0s.chord.domain

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val artworkUrl: String?,
    val streamUrl: String,
    val source: TrackSource,
    val license: LicenseInfo,
    val explicit: Boolean = false,
    val previewUrl: String? = null,
)

@Serializable
enum class TrackSource {
    AUDIUS,
    JAMENDO,
    MUSICBRAINZ,
    LOCAL
}

@Serializable
data class LicenseInfo(
    val type: LicenseType,
    val url: String?,
    val attributionRequired: Boolean,
    val commercialUseAllowed: Boolean,
    val modificationsAllowed: Boolean,
)

@Serializable
enum class LicenseType {
    CC_BY,
    CC_BY_SA,
    CC_BY_NC,
    CC_BY_NC_SA,
    CC_BY_ND,
    CC_BY_NC_ND,
    CUSTOM,
    PUBLIC_DOMAIN,
    UNKNOWN
}

@Serializable
data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val bio: String?,
    val verified: Boolean,
    val source: TrackSource,
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val releaseDate: String?,
    val trackCount: Int,
    val source: TrackSource,
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val artworkUrl: String?,
    val trackIds: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isUserCreated: Boolean,
)

@Serializable
data class SearchResult(
    val tracks: List<Track>,
    val artists: List<Artist>,
    val albums: List<Album>,
    val playlists: List<Playlist>,
)