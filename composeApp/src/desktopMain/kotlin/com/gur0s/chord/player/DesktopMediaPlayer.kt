package com.gur0s.chord.player

import com.gur0s.chord.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

class DesktopMediaPlayer(
    private val scope: CoroutineScope,
) : MediaPlayer {
    private var clip: Clip? = null
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    override suspend fun play(track: Track, queue: List<Track> = emptyList(), startIndex: Int = 0) = withContext(Dispatchers.IO) {
        stopInternal()
        try {
            val audioInputStream = URL(track.streamUrl).openStream()
            val bufferedInputStream = java.io.BufferedInputStream(audioInputStream)
            val audioInputStream2 = AudioSystem.getAudioInputStream(bufferedInputStream)
            clip = AudioSystem.getClip()
            clip.open(audioInputStream2)
            
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as? FloatControl
            gainControl?.value = 20f * Math.log10(_state.value.volume.coerceIn(0.0f, 1f)).toFloat()
            
            clip.addLineListener { event ->
                if (event.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    scope.launch { _state.update { it.copy(playbackState = PlaybackState.COMPLETED) } }
                }
            }
            
            clip.start()
            _state.update { it.copy(
                currentTrack = it,
                playbackState = PlaybackState.PLAYING,
                duration = it.duration,
            )}
        } catch (e: Exception) {
            _state.update { it.copy(playbackState = PlaybackState.ERROR, error = e.message) }
        }
    }

    override suspend fun pause() = withContext(Dispatchers.IO) {
        clip?.stop()
        _state.update { it.copy(playbackState = PlaybackState.PAUSED) }
    }

    override suspend fun resume() = withContext(Dispatchers.IO) {
        clip?.start()
        _state.update { it.copy(playbackState = PlaybackState.PLAYING) }
    }

    override suspend fun seek(position: Long) = withContext(Dispatchers.IO) {
        clip?.let {
            it.setMicrosecondPosition(position * 1000)
            _state.update { it.copy(position = position) }
        }
    }

    override suspend fun next() = withContext(Dispatchers.IO) {
        // Queue logic would go here
    }

    override suspend fun previous() = withContext(Dispatchers.IO) {
        // Queue logic would go here
    }

    override suspend fun setVolume(volume: Float) = withContext(Dispatchers.IO) {
        val gainControl = clip?.getControl(FloatControl.Type.MASTER_GAIN) as? javax.sound.sampled.FloatControl
        gainControl?.value = 20f * Math.log10(volume.coerceIn(0.0f, 1f)).toFloat()
        _state.update { it.copy(volume = volume) }
    }

    override suspend fun setShuffle(shuffle: Boolean) {
        _state.update { it.copy(shuffle = shuffle) }
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        _state.update { it.copy(repeatMode = mode) }
    }

    override suspend fun addToQueue(track: Track) {
        _state.update { it.copy(queue = it.queue + track) }
    }

    override suspend fun removeFromQueue(index: Int) {
        _state.update { it.copy(queue = it.queue.toMutableList().also { it.removeAt(index) }) }
    }

    override suspend fun clearQueue() {
        _state.update { it.copy(queue = emptyList()) }
    }

    override suspend fun stop() = withContext(Dispatchers.IO) {
        stopInternal()
        _state.update { it.copy(playbackState = PlaybackState.IDLE, currentTrack = null) }
    }

    private fun stopInternal() {
        clip?.stop()
        clip?.close()
        clip = null
    }
}