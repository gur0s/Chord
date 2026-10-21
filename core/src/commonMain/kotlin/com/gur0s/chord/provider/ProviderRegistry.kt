package com.gur0s.chord.provider

import com.gur0s.chord.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProviderRegistry(
    private val providers: List<MusicProvider>
) {
    private val _activeProviders = MutableStateFlow(providers.filter { it.requiresApiKey.not() || it.name != "Jamendo" })
    val activeProviders: StateFlow<List<MusicProvider>> = _activeProviders

    fun getProvider(source: TrackSource): MusicProvider? = providers.find { it.source == source }

    fun enableProvider(source: TrackSource, apiKey: String? = null) {
        val provider = providers.find { it.source == source }
        if (provider != null) {
            _activeProviders.update { current ->
                if (source == TrackSource.JAMENDO) {
                    // Would need to recreate with API key
                    current
                } else {
                    current + provider
                }
            }
        }
    }

    fun disableProvider(source: TrackSource) {
        _activeProviders.update { it.filter { it.source != source } }
    }

    fun isProviderActive(source: TrackSource): Boolean = _activeProviders.value.any { it.source == source }

    suspend fun searchAll(query: String, limit: Int = 20): List<Track> = coroutineScope {
        val active = _activeProviders.value
        val deferred = active.map { provider ->
            async { provider.search(query, limit / active.size + 1).getOrNull()?.tracks ?: emptyList() }
        }
        deferred.awaitAll().flatten().distinctBy { it.id }.take(limit)
    }
}

inline fun <reified T> coroutineScope(crossinline block: suspend CoroutineScope.() -> T): suspend T = 
    kotlinx.coroutines.coroutineScope { block() }

suspend fun <T> async(block: suspend () -> T) = kotlinx.coroutines.async { block() }