package proto

import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.models.MusicResponsiveListItemRenderer
import com.metrolist.innertubex.models.YouTubeClient
import com.metrolist.innertubex.models.YouTubeLocale
import com.metrolist.innertubex.models.splitBySeparator
import com.metrolist.innertubex.sabr.SabrAudioStream
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private val t0 = System.nanoTime()

fun log(msg: String) {
    val s = (System.nanoTime() - t0) / 1_000_000
    println("[%6d ms] %s".format(s, msg))
}

data class Track(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String,
    val type: String,
    val duration: String,
    val thumbnail: String,
)

/** Walks arbitrary JSON and collects every musicResponsiveListItemRenderer node. */
fun findAll(element: JsonElement, key: String, acc: MutableList<JsonElement> = mutableListOf()): List<JsonElement> {
    when (element) {
        is JsonObject -> element.forEach { (k, v) ->
            if (k == key) acc += v
            findAll(v, key, acc)
        }
        is kotlinx.serialization.json.JsonArray -> element.forEach { findAll(it, key, acc) }
        else -> Unit
    }
    return acc
}

class Yt(
    val httpClient: HttpClient,
) {
    val client = InnerTube(httpClient = httpClient)

    private val configStore = RemotePlayerConfigStore(
        httpClient = httpClient,
        repository = PlayerConfigRepository.disabled(),
    )
    private val cipher = YouTubeCipherService(httpClient, configStore)
    private val parser = YtConfigParserImpl(
        httpClient = httpClient,
        innerTube = client,
        remotePlayerConfigStore = configStore,
        cipherService = cipher,
    )
    val extractor = InnerTubeExtractor(
        configParser = parser,
        cipherService = cipher,
        innerTube = client,
    )

    suspend fun search(query: String): List<Track> {
        val response = client.search(YouTubeClient.WEB_REMIX, query = query)
        val text = response.bodyAsText()
        log("search(${query}) http=${response.status} bodyChars=${text.length}")
        val root = json.parseToJsonElement(text)
        val nodes = findAll(root, "musicResponsiveListItemRenderer")
        log("search: found ${nodes.size} musicResponsiveListItemRenderer nodes")
        val tracks = nodes.mapNotNull { node ->
            val item = runCatching {
                json.decodeFromJsonElement(MusicResponsiveListItemRenderer.serializer(), node)
            }.getOrNull()
                ?: return@mapNotNull null
            val videoId = item.playlistItemData?.videoId
                ?: item.navigationEndpoint?.watchEndpoint?.videoId
                ?: return@mapNotNull null
            val cols = item.flexColumns.mapNotNull { it.musicResponsiveListItemFlexColumnRenderer.text?.runs?.splitBySeparator() }
            val title = cols.getOrNull(0)?.getOrNull(0)?.joinToString("") { it.text } ?: ""
            val type = cols.getOrNull(1)?.getOrNull(0)?.joinToString("") { it.text } ?: ""
            val artist = cols.getOrNull(1)?.getOrNull(1)?.joinToString("") { it.text } ?: ""
            val album = cols.getOrNull(1)?.getOrNull(2)?.joinToString("") { it.text } ?: ""
            val duration = cols.getOrNull(1)?.getOrNull(3)?.joinToString("") { it.text } ?: ""
            val thumbs = item.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
            val thumb = thumbs?.maxByOrNull { it.width ?: 0 }?.url ?: ""
            Track(videoId, title.trim(), artist.trim(), album.trim(), type.trim(), duration.trim(), thumb)
        }
        return tracks
    }

    suspend fun extract(videoId: String, quality: AudioQuality = AudioQuality.AUTO): ExtractedStream {
        val hints = ContentHints(wantVideo = false)
        val stream = extractor.extract(videoId, hints, emptySet(), quality)
        return stream ?: error("extract returned null for $videoId")
    }
}

/** Minimal FFmpeg-backed player used to exercise real decoding of the extracted stream. */
class FfPlayer(
    val command: List<String>,
    private val stdinBytes: (suspend (java.io.OutputStream) -> Unit)? = null,
) {
    var process: Process? = null
    private var stderrThread: Thread? = null
    @Volatile var lastPositionSec: Double = -1.0
        private set
    @Volatile var totalDurationSec: Double = -1.0
        private set
    @Volatile var stderrTail: String = ""
        private set
    @Volatile var exited: Boolean = false
        private set
    var exitCode: Int = -1
        private set

    fun start(): Process {
        val p = ProcessBuilder(command).redirectErrorStream(true).start()
        process = p
        stderrThread = Thread {
            BufferedReader(InputStreamReader(p.inputStream)).forEachLine { line ->
                stderrTail = (stderrTail + "\n" + line).takeLast(4000)
                Regex("time=\\s*(\\d+):(\\d+):(\\d+\\.\\d+)").find(line)?.let {
                    lastPositionSec = it.groupValues[1].toDouble() * 3600 +
                        it.groupValues[2].toDouble() * 60 +
                        it.groupValues[3].toDouble()
                }
                Regex("out_time_us=(-?\\d+)").find(line)?.let {
                    lastPositionSec = it.groupValues[1].toDouble() / 1_000_000.0
                }
                Regex("Duration:\\s*(\\d+):(\\d+):(\\d+\\.\\d+)").find(line)?.let {
                    totalDurationSec = it.groupValues[1].toDouble() * 3600 +
                        it.groupValues[2].toDouble() * 60 +
                        it.groupValues[3].toDouble()
                }
            }
            exited = true
        }.also { it.isDaemon = true; it.start() }
        if (stdinBytes != null) {
            Thread {
                runBlocking { runCatching { stdinBytes(p.outputStream) } }
                runCatching { p.outputStream.close() }
            }.also { it.isDaemon = true; it.start() }
        }
        return p
    }

    fun pid(): Long = process?.pid() ?: -1

    fun pause() {
        val pid = pid()
        if (pid > 0) ProcessBuilder("kill", "-STOP", pid.toString()).start().waitFor()
    }

    fun resume() {
        val pid = pid()
        if (pid > 0) ProcessBuilder("kill", "-CONT", pid.toString()).start().waitFor()
    }

    fun stop() {
        process?.destroy()
        process?.waitFor(5, TimeUnit.SECONDS)
        runCatching { process?.destroyForcibly() }
    }

    fun awaitExit(timeoutSec: Long): Boolean {
        val ok = process?.waitFor(timeoutSec, TimeUnit.SECONDS) ?: false
        if (ok) exitCode = process?.exitValue() ?: -1
        return ok
    }
}

fun directCommand(url: String, headers: Map<String, String>, startSec: Double, realtime: Boolean, durationSec: Double?): List<String> {
    val cmd = mutableListOf("ffmpeg", "-hide_banner", "-nostdin")
    if (realtime) cmd += "-re"
    if (startSec > 0) {
        cmd += listOf("-ss", "%.3f".format(java.util.Locale.ROOT, startSec))
        cmd += listOf("-copyts")
    }
    if (headers.isNotEmpty()) {
        cmd += listOf("-headers", headers.entries.joinToString("\r\n") { "${it.key}: ${it.value}" } + "\r\n")
    }
    cmd += listOf("-i", url)
    if (durationSec != null && durationSec > 0) cmd += listOf("-t", "%.3f".format(java.util.Locale.ROOT, durationSec))
    cmd += listOf("-f", "null", "-")
    return cmd
}

fun sabrCommand(realtime: Boolean, durationSec: Double?): List<String> {
    val cmd = mutableListOf("ffmpeg", "-hide_banner", "-nostdin")
    if (realtime) cmd += "-re"
    cmd += listOf("-i", "pipe:0")
    if (durationSec != null && durationSec > 0) cmd += listOf("-t", "%.3f".format(java.util.Locale.ROOT, durationSec))
    cmd += listOf("-f", "null", "-")
    return cmd
}


fun pcmSha(input: String, startSec: Double): String {
    val cmd = listOf("ffmpeg", "-hide_banner", "-nostdin", "-ss", "%.3f".format(java.util.Locale.ROOT, startSec),
        "-i", input, "-t", "3", "-f", "s16le", "-ac", "2", "-ar", "48000", "-")
    val p = ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start()
    val md = java.security.MessageDigest.getInstance("SHA-256")
    p.inputStream.use { ins -> val buf = ByteArray(65536); while (true) { val n = ins.read(buf); if (n <= 0) break; md.update(buf, 0, n) } }
    p.waitFor()
    return md.digest().joinToString("") { "%02x".format(it) }.take(16)
}

fun main(args: Array<String>) {
    val cmd = args.firstOrNull() ?: "suite"
    val httpClient = HttpClient(OkHttp) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false })
        }
    }
    runBlocking {
        val yt = Yt(httpClient)
        yt.client.locale = YouTubeLocale(gl = "US", hl = "en")
        try {
            when (cmd) {
                "search" -> {
                    val q = args.getOrNull(1) ?: "Rick Astley Never Gonna Give You Up"
                    yt.search(q).take(5).forEach { t ->
                        log("TRACK id=${t.videoId} title='${t.title}' artist='${t.artist}' album='${t.album}' thumb=${t.thumbnail}")
                    }
                }
                "raw" -> {
                    val q = args.getOrNull(1) ?: "Never Gonna Give You Up"
                    val response = yt.client.search(YouTubeClient.WEB_REMIX, query = q)
                    val root = json.parseToJsonElement(response.bodyAsText())
                    val nodes = findAll(root, "musicResponsiveListItemRenderer")
                    nodes.take(3).forEachIndexed { i, n ->
                        val obj = n.jsonObject
                        log("ITEM $i keys=${obj.keys}")
                        val fc = obj["flexColumns"]
                        log("ITEM $i flexColumns=" + fc.toString().take(700))
                    }
                }
                "extract" -> {
                    val id = args.getOrNull(1) ?: "dQw4w9WgXcQ"
                    val s = yt.extract(id)
                    log("STREAM client=${s.clientName} profile=${s.profileId} mime=${s.mimeType} codecs=${s.codecs} itag=${s.itag} bitrate=${s.bitrate} len=${s.contentLengthBytes} url=${s.audioUrl?.take(90)} sabr=${s.sabrBootstrap != null} expires=${s.expiresAt} dur=${s.mediaMetadata?.durationSeconds}")
                }
                "play" -> {
                    val id = args.getOrNull(1) ?: "dQw4w9WgXcQ"
                    val secs = args.getOrNull(2)?.toDoubleOrNull() ?: 20.0
                    val s = yt.extract(id)
                    log("STREAM client=${s.clientName} mime=${s.mimeType} itag=${s.itag} bitrate=${s.bitrate} dur=${s.mediaMetadata?.durationSeconds} urlPresent=${s.audioUrl != null} sabr=${s.sabrBootstrap != null} headers=${s.headers}")
                    val p = buildPlayer(yt, s, 0.0, true, secs)
                    log("CMD=" + p.command.joinToString(" "))
                    p.start()
                    val end = System.currentTimeMillis() + (secs * 1000).toLong() + 15000
                    while (!p.exited && System.currentTimeMillis() < end) {
                        Thread.sleep(3000)
                        log("progress pos=${p.lastPositionSec}s total=${p.totalDurationSec}s")
                    }
                    val ok = p.awaitExit(10)
                    log("finished=$ok exit=${p.exitCode} lastPos=${p.lastPositionSec}s total=${p.totalDurationSec}s")
                    log("stderrTail=" + p.stderrTail.takeLast(600))
                    p.stop()
                }
                "seek" -> {
                    val id = args.getOrNull(1) ?: "dQw4w9WgXcQ"
                    val s = yt.extract(id)
                    log("STREAM client=${s.clientName} dur=${s.mediaMetadata?.durationSeconds}")
                    for (target in listOf(0.0, 60.0, 150.0, 30.0)) {
                        val p = buildPlayer(yt, s, target, true, 6.0)
                        p.start()
                        Thread.sleep(9000)
                        log("SEEK target=${target}s reportedPos=${p.lastPositionSec}s total=${p.totalDurationSec}s tail=" + p.stderrTail.takeLast(300).replace("\n", " | "))
                        p.stop()
                        Thread.sleep(500)
                    }
                }
                "seekverify" -> {
                    val id = args.getOrNull(1) ?: "dQw4w9WgXcQ"
                    val s = yt.extract(id)
                    val url = s.audioUrl ?: error("no direct url")
                    log("STREAM client=${s.clientName} dur=${s.mediaMetadata?.durationSeconds} len=${s.contentLengthBytes}")
                    val local = java.io.File("/tmp/opencode/track-local.webm")
                    if (!local.exists() || local.length() != s.contentLengthBytes) {
                        val ok = runCatching {
                            java.net.URI(url).toURL().openStream().use { ins ->
                                local.outputStream().use { out -> ins.copyTo(out) }
                            }
                        }.isSuccess
                        log("download ok=$ok bytes=${local.length()}")
                    } else {
                        log("download cached bytes=${local.length()}")
                    }
                    for (t in listOf(0.0, 30.0, 90.0, 180.0)) {
                        val net = pcmSha(url, t)
                        val loc = pcmSha(local.absolutePath, t)
                        log("SEEK_ABS t=${t}s netSha=$net localSha=$loc match=${net == loc}")
                    }
                }
                "sabr" -> {
                    val id = args.getOrNull(1) ?: "dQw4w9WgXcQ"
                    val stream = yt.extract(id, AudioQuality.HIGH)
                        ?: error("extract null")
                    val bootstrap = stream.sabrBootstrap
                    log("STREAM sabr=${bootstrap != null} client=${stream.clientName} mime=${stream.mimeType}")
                    if (bootstrap == null) {
                        val forced = yt.extractor.extract(id, ContentHints(wantVideo = false, sabrFirst = true), emptySet(), AudioQuality.HIGH)
                            ?: error("extract forced null")
                        val fBootstrap = forced.sabrBootstrap
                        log("FORCED sabr=${fBootstrap != null} client=${forced.clientName}")
                        if (forced.sabrBootstrap == null) { log("ABORT: no sabr bootstrap"); return@runBlocking }
                        val cmd = sabrCommand(true, null)
                        val p = FfPlayer(cmd) { out ->
                            val st2 = SabrAudioStream(httpClient = yt.httpClient, bootstrap = fBootstrap!!, initialPlayerTimeMs = 0)
                            st2.bytes().collect { bytes -> out.write(bytes); out.flush() }
                        }
                        p.start()
                        Thread.sleep(15000)
                        log("SABR pos=${p.lastPositionSec}s exited=${p.exited} tail=" + p.stderrTail.takeLast(400).replace("\n", " | "))
                        p.stop()
                        return@runBlocking
                    }
                    val cmd = sabrCommand(true, null)
                    val p = FfPlayer(cmd) { out ->
                        val st2 = SabrAudioStream(httpClient = yt.httpClient, bootstrap = bootstrap, initialPlayerTimeMs = 0)
                        st2.bytes().collect { bytes -> out.write(bytes); out.flush() }
                    }
                    p.start()
                    Thread.sleep(15000)
                    log("SABR pos=${p.lastPositionSec}s exited=${p.exited} tail=" + p.stderrTail.takeLast(400).replace("\n", " | "))
                    p.stop()
                }
                "suite" -> runSuite(yt)
                else -> log("unknown command $cmd")
            }
        } finally {
            yt.client.close()
            httpClient.close()
        }
    }
}

suspend fun runSuite(yt: Yt) {
    log("ENV java=${System.getProperty("java.version")} os=${System.getProperty("os.name")} ${System.getProperty("os.version")} lib=innertubex-desktop:v0.11.0")
    log("ENV ffmpeg=" + runCatching { ProcessBuilder("ffmpeg", "-version").start().inputStream.bufferedReader().readLine() }.getOrDefault("n/a"))
    log("=== STEP 1: anonymous search ===")
    val tracks = yt.search("Rick Astley Never Gonna Give You Up")
    tracks.take(5).forEach { t ->
        log("TRACK id=${t.videoId} title='${t.title}' artist='${t.artist}' album='${t.album}' typeDur='${t.type}/${t.duration}' thumb=${t.thumbnail.take(60)}")
    }
    if (tracks.isEmpty()) {
        log("ABORT: no tracks")
        return
    }
    val t1 = tracks.first()
    log("=== STEP 2: extract (anonymous, no token provider) ===")
    val stream = yt.extract(t1.videoId)
    log("STREAM client=${stream.clientName} profile=${stream.profileId} mime=${stream.mimeType} codecs=${stream.codecs} itag=${stream.itag} bitrate=${stream.bitrate} contentLength=${stream.contentLengthBytes} expiresAt=${stream.expiresAt}")
    log("STREAM audioUrlPresent=${stream.audioUrl != null} sabrPresent=${stream.sabrBootstrap != null} headers=${stream.headers.keys}")
    log("STREAM metadata title='${stream.mediaMetadata?.title}' artist='${stream.mediaMetadata?.author}' duration=${stream.mediaMetadata?.durationSeconds}")

    log("=== STEP 3: thumbnail fetch ===")
    if (t1.thumbnail.isNotEmpty()) {
        val resp = withContext(Dispatchers.IO) {
            runCatching {
                java.net.URI(t1.thumbnail).toURL().openStream().use { it.readBytes().size }
            }
        }
        log("THUMBNAIL bytes=$resp")
    }

    log("=== STEP 4: play 20s from start ===")
    val player = buildPlayer(yt, stream, startSec = 0.0, realtime = true, limitSec = 20.0)
    player.start()
    Thread.sleep(23_000)
    log("PLAY pos=${player.lastPositionSec}s total=${player.totalDurationSec}s exited=${player.exited}")
    player.stop()

    log("=== STEP 5: pause / resume ===")
    val p2 = buildPlayer(yt, stream, startSec = 0.0, realtime = true, limitSec = null)
    p2.start()
    Thread.sleep(6_000)
    val beforePause = p2.lastPositionSec
    p2.pause()
    Thread.sleep(4_000)
    val duringPause = p2.lastPositionSec
    p2.resume()
    Thread.sleep(4_000)
    val afterResume = p2.lastPositionSec
    log("PAUSE before=${beforePause}s during=${duringPause}s afterResume=${afterResume}s frozen=${duringPause - beforePause < 1.0} advanced=${afterResume > duringPause}")
    p2.stop()

    log("=== STEP 6: seek backward then forward ===")
    val back = buildPlayer(yt, stream, startSec = 30.0, realtime = true, limitSec = 8.0)
    back.start()
    Thread.sleep(11_000)
    log("SEEK_BACK pos=${back.lastPositionSec}s (expected ~38s)")
    back.stop()

    val fwd = buildPlayer(yt, stream, startSec = 120.0, realtime = true, limitSec = 8.0)
    fwd.start()
    Thread.sleep(11_000)
    log("SEEK_FORWARD pos=${fwd.lastPositionSec}s (expected ~128s)")
    fwd.stop()

    log("=== STEP 7: track switch ===")
    val t2 = tracks.getOrNull(1) ?: Track("kJQP7kiw5Fk", "Despacito", "Luis Fonsi", "", "Song", "", "")
    if (t2 != null) {
        val s2 = yt.extract(t2.videoId)
        log("TRACK2 id=${t2.videoId} title='${t2.title}' client=${s2.clientName} itag=${s2.itag}")
        val p3 = buildPlayer(yt, s2, startSec = 0.0, realtime = true, limitSec = 15.0)
        p3.start()
        Thread.sleep(18_000)
        log("SWITCH pos=${p3.lastPositionSec}s title='${s2.mediaMetadata?.title}'")
        p3.stop()
    }

    log("=== STEP 8: full track continuous playback ===")
    val expected = stream.mediaMetadata?.durationSeconds
    log("FULL expectedDuration=${expected}s")
    val full = buildPlayer(yt, stream, startSec = 0.0, realtime = true, limitSec = null)
    full.start()
    val deadline = System.currentTimeMillis() + ((expected ?: 240L) + 90) * 1000
    while (!full.exited && System.currentTimeMillis() < deadline) {
        Thread.sleep(5_000)
        log("FULL progress pos=${full.lastPositionSec}s")
    }
    val finished = full.awaitExit(30)
    log("FULL finished=$finished exitCode=${full.exitCode} finalPos=${full.lastPositionSec}s total=${full.totalDurationSec}s expected=${expected}s")
    full.stop()
    log("=== SUITE DONE ===")
}

fun buildPlayer(yt: Yt, stream: ExtractedStream, startSec: Double, realtime: Boolean, limitSec: Double?): FfPlayer {
    val sabr = stream.sabrBootstrap
    return if (sabr != null) {
        val cmd = sabrCommand(realtime, limitSec)
        FfPlayer(cmd) { out ->
            val stream2 = SabrAudioStream(
                httpClient = yt.httpClient,
                bootstrap = sabr,
                initialPlayerTimeMs = (startSec * 1000).toLong(),
            )
            stream2.bytes().collect { bytes -> out.write(bytes); out.flush() }
        }
    } else {
        val url = stream.audioUrl ?: error("no audio url and no sabr bootstrap")
        FfPlayer(directCommand(url, stream.headers, startSec, realtime, limitSec))
    }
}
