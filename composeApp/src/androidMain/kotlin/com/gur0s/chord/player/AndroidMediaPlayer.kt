package com.gur0s.chord.player

import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.MediaItem
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import com.gur0s.chord.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
class AndroidMediaPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
) : MediaPlayer {
    private val exoPlayer: ExoPlayer
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    init {
        val trackSelector = DefaultTrackSelector(context)
        exoPlayer = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _state.update { current ->
                    current.copy(playbackState = when (state) {
                        Player.STATE_IDLE -> PlaybackState.IDLE
                        Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                        Player.STATE_READY -> PlaybackState.PLAYING
                        Player.STATE_ENDED -> PlaybackState.COMPLETED
                        else -> PlaybackState.ERROR
                    })
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { current ->
                    current.copy(playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED)
                }
            }

            override fun onPositionDiscontinuity(reason: Int) {
                _state.update { current ->
                    current.copy(position = exoPlayer.currentPosition)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.update { it.copy(playbackState = PlaybackState.ERROR, error = error.message) }
            }
        })
    }

    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    override suspend fun play(track: Track, queue: List<Track> = emptyList(), startIndex: Int = 0) = withContext(Dispatchers.IO) {
        val mediaItem = MediaItem.fromUri(track.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _state.update { it.copy(
            currentTrack = track,
            playbackState = PlaybackState.BUFFERING,
            queue = queue,
            currentIndex = startIndex,
            duration = track.duration,
        )}
    }

    override suspend fun pause() = withContext(Dispatchers.IO) {
        exoPlayer.pause()
    }

    override suspend fun resume() = withContext(Dispatchers.IO) {
        exoPlayer.playWhenReady = true
    }

    override suspend fun seek(position: Long) = withContext(Dispatchers.IO) {
        exoPlayer.seekTo(position)
    }

    override suspend fun next() = withContext(Dispatchers.IO) {
        exoPlayer.seekToNext()
    }

    override suspend fun previous() = withContext(Dispatchers.IO) {
        exoPlayer.seekToPrevious()
    }

    override suspend fun setVolume(volume: Float) = withContext(Dispatchers.IO) {
        exoPlayer.volume = volume.coerceIn(0f, 1f)
    }

    override suspend fun setShuffle(shuffle: Boolean) {
        // ExoPlayer doesn't have built-in shuffle; implement in queue logic
    }

    override suspend fun setRepeatMode(mode: RepeatMode) = withContext(Dispatchers.IO) {
        exoPlayer.repeatMode = when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    override suspend fun addToQueue(track: Track) {
        // Implement queue management
    }

    override suspend fun removeFromQueue(index: Int) {
        // Implement queue management
    }

    override suspend fun clearQueue() {
        // Implement queue management
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        exoPlayer.stop()
    }
}