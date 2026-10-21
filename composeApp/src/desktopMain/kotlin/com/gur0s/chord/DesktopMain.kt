package com.gur0s.chord

import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowState
import androidx.compose.desktop.compose
import androidx.compose.desktop.startupErrorHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.gur0s.chord.player.DesktopMediaPlayer
import com.gur0s.chord.player.MediaPlayer
import com.gur0s.chord.provider.AudiusProvider
import com.gur0s.chord.provider.JamendoProvider
import com.gur0s.chord.provider.MusicBrainzProvider
import com.gur0s.chord.provider.ProviderRegistry
import com.gur0s.chord.repository.ChordRepository
import com.gur0s.chord.db.ChordDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun main() = Window(
    state = WindowState(
        width = 1200,
        height = 800,
        title = "Chord"
    ),
    startupErrorHandler = { e ->
        e.printStackTrace()
    }
) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val httpClient = HttpClient(CIO)
    val providerRegistry = ProviderRegistry(listOf(
        AudiusProvider.createWithDefaults(),
        MusicBrainzProvider.createWithUserAgent("Chord/1.0.0 (gur0s@chord.app)"),
        JamendoProvider.createWithClientId("YOUR_JAMENDO_CLIENT_ID"),
    ))
    val database = ChordDatabase.getDesktopInstance()
    val repository = ChordRepository(database, providerRegistry)
    val player = DesktopMediaPlayer(CoroutineScope(Dispatchers.Main))
    
    MaterialTheme {
        Surface {
            DesktopChordApp(player = player, onNavigate = { _ -> })
        }
    }
}