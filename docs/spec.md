# Chord - Especificação Técnica

## Visão Geral

**Chord** é um player de música multiplataforma (Android, Windows, Linux) que utiliza exclusivamente fontes legais e documentadas: **Audius** (streaming), **Jamendo** (Creative Commons) e **MusicBrainz** (metadados + capas). Não utiliza APIs privadas, não contorna DRM/paywall, não requer login.

## Entidades Principais

### Track (Faixa)
```kotlin
data class Track(
    val id: String,                    // Prefixo: aud_ | jam_ | mb_
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,                // ms
    val artworkUrl: String?,
    val streamUrl: String,             // URL direta de áudio
    val source: TrackSource,           // AUDIUS | JAMENDO | MUSICBRAINZ | LOCAL
    val license: LicenseInfo,
    val explicit: Boolean,
    val previewUrl: String?
)
```

### Artist (Artista)
```kotlin
data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val bio: String?,
    val verified: Boolean,
    val source: TrackSource
)
```

### Album (Álbum)
```kotlin
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val releaseDate: String?,
    val trackCount: Int,
    val source: TrackSource
)
```

### Playlist
```kotlin
data class Playlist(
    val id: String,
    val name: String,
    val description: String?,
    val artworkUrl: String?,
    val trackIds: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isUserCreated: Boolean
)
```

### LicenseInfo
```kotlin
data class LicenseInfo(
    val type: LicenseType,             // CC_BY | CC_BY_SA | CC_BY_NC | CC_BY_NC_SA | CC_BY_ND | CC_BY_NC_ND | CUSTOM | PUBLIC_DOMAIN | UNKNOWN
    val url: String?,
    val attributionRequired: Boolean,
    val commercialUseAllowed: Boolean,
    val modificationsAllowed: Boolean
)
```

## Fluxos Principais

### 1. Busca Unificada
```
User Query → ProviderRegistry.searchAll(query, limit)
    ├─ AudiusProvider.search()
    ├─ JamendoProvider.search()     (se client_id configurado)
    └─ MusicBrainzProvider.search()
        → Merge + dedup by ID → SearchResult
```

### 2. Reprodução
```
User selects Track → MediaPlayer.play(track, queue, index)
    ├─ Android: ExoPlayer (Media3) via MediaItem.fromUri(streamUrl)
    └─ Desktop: Java Sound API (Clip) via URL.openStream()
        → StateFlow<PlayerState> atualizado em tempo real
```

### 3. Fila (Queue)
- `queue: List<Track>` + `currentIndex: Int`
- Operações: `next()`, `previous()`, `addToQueue()`, `removeFromQueue()`, `reorder()`, `clearQueue()`
- Persistência: `PlaylistEntity` com `isUserCreated = false` para fila temporária

### 4. Shuffle / Repeat
```
RepeatMode: OFF → ALL → ONE → OFF
Shuffle: Boolean (embaralha fila, mantém currentIndex)
```
- Shuffle: Fisher-Yates na fila, preserva posição atual
- Repeat ONE: `exoPlayer.repeatMode = REPEAT_MODE_ONE` / loop manual no desktop

### 5. Biblioteca Local
```
Liked: LikedTrackEntity (trackId, likedAt)
Playlists: PlaylistEntity + PlaylistTrackEntity (position)
History: HistoryEntity (trackId, playedAt, position, completed)
Cache: TrackEntity (metadados + artwork + streamUrl)
```

### 4. Estados de Erro
| Código | Significado | Ação |
|--------|-------------|------|
| `NETWORK_ERROR` | Falha de rede | Retry automático (exponencial) |
| `RATE_LIMITED` | 429 Too Many Requests | Backoff exponencial + retry |
| `NOT_FOUND` | 404 | Remover da fila, notificar usuário |
| `INVALID_RESPONSE` | Parse falhou | Log + skip track |
| `AUTH_REQUIRED` | 401/403 | Desabilitar provider, notificar |
| `LICENSE_RESTRICTION` | Licença impede ação | Desabilitar botão download |
| `UNSUPPORTED_OPERATION` | Provider não suporta | Fallback para outro provider |

## Player & Sistema

### Android (Media3 ExoPlayer)
- `ExoPlayer` com `DefaultMediaSourceFactory`
- `MediaSessionCompat` para controles de notificação/trava
- `AudioFocus` request/abandon
- `Foreground Service` para background playback
- `MediaSessionService` para Android Auto / Wear

### Windows
- Avaliar: `MediaPlayer` nativo (Windows 10+) via `winrt` ou `javacpp` + `MediaFoundation`
- Teclas de mídia: `GlobalHotKey` via `JIntellitype` ou `jnativehook`
- Integração SMTC (System Media Transport Controls) via `winrt`

### Linux
- MPRIS2 via `dbus` (org.mpris.MediaPlayer2)
- Teclas de mídia: `GlobalHotKey` via `jnativehook` ou `libinput`
- Ícone de bandeja: `AppIndicator` (GTK) ou `systemtray` (Qt)

### Atalhos Globais
| Ação | Android | Windows | Linux | Desktop (Compose) |
|------|---------|---------|-------|-------------------|
| Play/Pause | Headset/Notification | `MediaPlayPause` | `XF86AudioPlay` | `Space` |
| Next | Headset/Notification | `MediaNext` | `XF86AudioNext` | `→` / `Ctrl+→` |
| Previous | Headset/Notification | `MediaPrevious` | `XF86AudioPrev` | `←` / `Ctrl+←` |
| Seek +10s | - | `Ctrl+→` | `Ctrl+→` | `Ctrl+→` |
| Seek -10s | - | `Ctrl+←` | `Ctrl+←` | `Ctrl+←` |
| Volume Up | Hardware | `MediaVolumeUp` | `XF86AudioRaiseVolume` | `↑` |
| Volume Down | Hardware | `MediaVolumeDown` | `XF86AudioLowerVolume` | `↓` |
| Buscar | - | `Ctrl+K` | `Ctrl+K` | `Ctrl+K` |

**Regra**: Não capturar atalhos quando foco em campo de texto (`TextField`, `EditText`).

## Cache & Offline
- **Metadados**: Room (SQLite) - TTL 7 dias
- **Artwork**: DiskLruCache (okio) - TTL 30 dias, max 100MB
- **Stream URLs**: Não cacheadas (expiram)
- **Offline Download**: Apenas Jamendo (quando licença permite)
- **Modo Offline**: Busca/Play apenas do cache local

## Configuração
```properties
# local.properties (não commitado)
jamendo.client.id=YOUR_CLIENT_ID
musicbrainz.useragent=Chord/1.0.0 (seu@email.com)
audius.app.name=Chord
```