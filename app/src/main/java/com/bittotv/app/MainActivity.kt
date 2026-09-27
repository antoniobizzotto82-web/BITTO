package com.bittotv.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/countries/ar.m3u"

data class Channel(
    val name: String,
    val group: String,
    val url: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent { BittoTvApp() }
    }
}

@Composable
fun BittoTvApp() {
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<Channel?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF2C9BFF),
            background = Color.Black,
            surface = Color(0xFF101820)
        )
    ) {
        when (screen) {
            "home" -> HomeScreen { screen = "iptv" }
            else -> IptvScreen(
                selected = selected,
                onSelect = { selected = it },
                onBack = {
                    selected = null
                    screen = "home"
                }
            )
        }
    }
}

@Composable
private fun HomeScreen(onIptv: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.main_background),
            contentDescription = "BITTO-TV background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.82f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "BITTO-TV 2026",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onIptv,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D76D7),
                    contentColor = Color.White
                )
            ) {
                Text("📺  REPRODUCTOR IPTV", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Lista argentina cargada automáticamente",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun IptvScreen(
    selected: Channel?,
    onSelect: (Channel) -> Unit,
    onBack: () -> Unit
) {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            channels = downloadPlaylist(PLAYLIST_URL)
        } catch (e: Exception) {
            error = e.message ?: "No se pudo cargar la lista."
        } finally {
            loading = false
        }
    }

    if (selected != null) {
        PlayerScreen(channel = selected, onBack = onBack)
        return
    }

    val filtered = channels.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.group.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B0F))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Inicio", color = Color.White) }
            Text(
                "REPRODUCTOR IPTV",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            label = { Text("Buscar canal") },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            error != null -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No se pudo cargar la lista.", color = Color.White, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(error ?: "", color = Color.LightGray)
                    Spacer(Modifier.height(18.dp))
                    Button(onClick = { reloadKey++ }) {
                        Text("Reintentar")
                    }
                }
            }

            else -> {
                Text(
                    "${filtered.size} canales",
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { channel ->
                        ChannelRow(channel, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onSelect: (Channel) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF151C24))
            .clickable { onSelect(channel) }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0D76D7)),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
            if (channel.group.isNotBlank()) {
                Text(channel.group, color = Color.LightGray, fontSize = 12.sp, maxLines = 1)
            }
        }
        Text("›", color = Color.White, fontSize = 28.sp)
    }
}

@Composable
private fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Canales", color = Color.White) }
            Text(
                channel.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 2
            )
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        Column(Modifier.padding(18.dp)) {
            Text(channel.name, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                "Transmisión desde la lista IPTV argentina",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }
    }
}

private suspend fun downloadPlaylist(url: String): List<Channel> =
    withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "BITTO-TV-2026/1.0")
        }

        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Servidor respondió ${connection.responseCode}")
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseM3u(text)
        } finally {
            connection.disconnect()
        }
    }

private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>()
    val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()

    var pendingName = ""
    var pendingGroup = ""

    for (i in lines.indices) {
        val line = lines[i]
        if (line.startsWith("#EXTINF", ignoreCase = true)) {
            val comma = line.indexOf(',')
            pendingName = if (comma >= 0) line.substring(comma + 1).trim() else "Canal"
            pendingGroup = Regex("""group-title="([^"]*)"""")
                .find(line)?.groupValues?.getOrNull(1).orEmpty()
        } else if (!line.startsWith("#") && pendingName.isNotBlank()) {
            result += Channel(
                name = pendingName,
                group = pendingGroup,
                url = line
            )
            pendingName = ""
            pendingGroup = ""
        }
    }

    return result
}
