package com.gur0s.chord.player

import com.gur0s.chord.domain.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asStateFlow

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

data class PlayerState(
    val currentTrack: Track? = null,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val position: Long = 0,
    val duration: Long = 0,
    val volume: Float = 1.0f,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val error: String? = null,
)

interface MediaPlayer {
    val state: StateFlow<PlayerState>
    suspend fun play(track: Track, queue: List<Track> = emptyList(), startIndex: Int = 0)
    suspend fun pause()
    suspend fun resume()
    suspend fun seek(position: Long)
    suspend fun next()
    suspend fun previous()
    suspend fun setVolume(volume: Float)
    suspend fun setShuffle(shuffle: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun addToQueue(track: Track)
    suspend fun removeFromQueue(index: Int)
    suspend fun clearQueue()
    suspend fun stop()
}

class ChordPlayer : MediaPlayer {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private val commandChannel = Channel<PlayerCommand>(capacity = 10)

    enum class PlayerCommand {
        PLAY(Track, List<Track>, Int),
        PAUSE,
        RESUME,
        SEEK(Long),
        NEXT,
        PREVIOUS,
        VOLUME(Float),
        SHUFFLE(Boolean),
        REPEAT(RepeatMode),
        ADD_TO_QUEUE(Track),
        REMOVE_FROM_QUEUE(Int),
        CLEAR_QUEUE,
        STOP,
        ERROR(String)
    }

    init {
        // Process commands
    }

    override suspend fun play(track: Track, queue: List<Track> = emptyList(), startIndex: Int = 0) {
        commandChannel.send(PlayerCommand.PLAY(track, queue, startIndex))
    }

    override suspend fun pause() = commandChannel.send(PlayerCommand.PAUSE)

    override suspend fun resume() = commandChannel.send(PlayerCommand.RESUME)

    override suspend fun seek(position: Long) = commandChannel.send(PlayerCommand.SEEK(position))

    override suspend fun next() = commandChannel.send(PlayerCommand.NEXT)

    override suspend fun previous() = commandChannel.send(PlayerCommand.PREVIOUS)

    override suspend fun setVolume(volume: Float) = commandChannel.send(PlayerCommand.VOLUME(volume))

    override suspend fun setShuffle(shuffle: Boolean) = commandChannel.send(PlayerCommand.SHUFFLE(shuffle))

    override suspend fun setRepeatMode(mode: RepeatMode) = commandChannel.send(PlayerCommand.REPEAT(mode))

    override suspend fun addToQueue(track: Track) = commandChannel.send(PlayerCommand.ADD_TO_QUEUE(track))

    override suspend fun removeFromQueue(index: Int) = commandChannel.send(PlayerCommand.REMOVE_FROM_QUEUE(index))

    override suspend fun clearQueue() = commandChannel.send(PlayerCommand.CLEAR_QUEUE)

    override suspend fun stop() = commandChannel.send(PlayerCommand.STOP)
}