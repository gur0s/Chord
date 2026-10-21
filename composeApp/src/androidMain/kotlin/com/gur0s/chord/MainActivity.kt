package com.gur0s.chord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import com.gur0s.chord.player.AndroidMediaPlayer
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

class MainActivity : ComponentActivity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val httpClient = HttpClient(CIO)
    private val providerRegistry = ProviderRegistry(listOf(
        AudiusProvider.createWithDefaults(),
        MusicBrainzProvider.createWithUserAgent("Chord/1.0.0 (gur0s@chord.app)"),
        JamendoProvider.createWithClientId("YOUR_JAMENDO_CLIENT_ID"), // Configure in settings
    ))
    private val database by lazy { ChordDatabase.getInstance(this) }
    private val repository by lazy { ChordRepository(database, providerRegistry) }
    private val player by lazy { AndroidMediaPlayer(this, CoroutineScope(Dispatchers.Main)) }
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    ChordApp(player = player, onNavigate = { _ -> })
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        uiScope.cancel()
        super.onDestroy()
    }
}