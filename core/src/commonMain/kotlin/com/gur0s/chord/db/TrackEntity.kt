package com.gur0s.chord.db

import androidx.room.Entity
import com.gur0s.chord.domain.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToString
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val artworkUrl: String?,
    val streamUrl: String,
    val source: String,
    val licenseType: String,
    val licenseUrl: String?,
    val attributionRequired: Boolean,
    val commercialUseAllowed: Boolean,
    val modificationsAllowed: Boolean,
    val explicit: Boolean,
    val previewUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0,
) {
    fun toDomain(): Track {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            artworkUrl = artworkUrl,
            streamUrl = streamUrl,
            source = TrackSource.valueOf(source),
            license = json.decodeFromString(json.encodeToString(this)) as com.gur0s.chord.domain.LicenseInfo,
            explicit = explicit,
            previewUrl = previewUrl,
        )
    }

    companion object {
        fun fromTrack(track: Track): TrackEntity = TrackEntity(
            id = track.id,
            title = track.title,
            artist = track.artist,
            album = track.album,
            duration = track.duration,
            artworkUrl = track.artworkUrl,
            streamUrl = track.streamUrl,
            source = track.source.name,
            licenseType = track.license.type.name,
            licenseUrl = track.license.url,
            attributionRequired = track.license.attributionRequired,
            commercialUseAllowed = track.license.commercialUseAllowed,
            modificationsAllowed = track.license.modificationsAllowed,
            explicit = track.explicit,
            previewUrl = track.previewUrl,
        )
    }
}