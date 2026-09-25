package com.bizzotto.bitto

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.BufferedReader
import java.io.InputStreamReader

data class Channel(val name: String, val url: String, val external: Boolean = false)

enum class Screen { HOME, CATEGORIES, CHANNELS, PLAYER }

enum class Category(val title: String, val asset: String, val emoji: String) {
    MOVIES("Películas y Cine", "movies.m3u", "🎬"),
    SPORTS("Deportes", "sports.m3u", "⚽"),
    KIDS_NEWS("Infantiles y Noticias", "kids_news.m3u", "🧒"),
    LIVE("Te veo en vivo", "live.m3u", "🔴")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityHolder.app = application
        setContent { BittoApp() }
    }
}

@Composable
fun BittoApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var category by remember { mutableStateOf<Category?>(null) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    val context = LocalContext.current

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF070707)) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onYoutube = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/"))) },
                    onIptv = { screen = Screen.CATEGORIES }
                )
                Screen.CATEGORIES -> CategoryScreen(
                    onBack = { screen = Screen.HOME },
                    onCategory = { category = it; screen = Screen.CHANNELS }
                )
                Screen.CHANNELS -> ChannelScreen(
                    category = category!!,
                    onBack = { screen = Screen.CATEGORIES },
                    onChannel = { selectedChannel = it; screen = Screen.PLAYER }
                )
                Screen.PLAYER -> PlayerScreen(
                    channel = selectedChannel!!,
                    onBack = { screen = Screen.CHANNELS }
                )
            }
        }
    }
}

@Composable
fun Header(title: String, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Text("‹", fontSize = 42.sp, modifier = Modifier.clickable { onBack() }.padding(end = 16.dp))
        }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("BITTO", fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeScreen(onYoutube: () -> Unit, onIptv: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bizzotto TV Familiar", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        Text("BITTO", fontSize = 18.sp, color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Image(
            painter = painterResource(com.bizzotto.bitto.R.drawable.padres),
            contentDescription = "Foto familiar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(0.72f).weight(1f).clip(RoundedCornerShape(24.dp))
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            BigButton("▶  YouTube", onYoutube)
            BigButton("📺  TV / IPTV", onIptv)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.width(250.dp).height(64.dp).onKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) { onClick(); true } else false
    }) { Text(text, fontSize = 19.sp) }
}

@Composable
fun CategoryScreen(onBack: () -> Unit, onCategory: (Category) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Reproductor IPTV", onBack)
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            items(Category.entries) { cat ->
                Card(Modifier.fillMaxWidth().height(150.dp).clickable { onCategory(cat) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(cat.emoji, fontSize = 42.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(cat.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelScreen(category: Category, onBack: () -> Unit, onChannel: (Channel) -> Unit) {
    val channels = remember(category) { loadChannels(category.asset) }
    Column(Modifier.fillMaxSize()) {
        Header(category.title, onBack)
        if (channels.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay canales cargados todavía. Reemplazá la lista M3U de esta categoría por una fuente autorizada.", fontSize = 18.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(220.dp), modifier = Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(channels) { channel ->
                    Card(Modifier.height(85.dp).clickable { onChannel(channel) }, colors = CardDefaults.cardColors(containerColor = Color(0xFF202020))) {
                        Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) { Text(channel.name, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    if (channel.external) {
        LaunchedEffect(channel.url) {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(channel.url)))
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header(channel.name, onBack)
            Spacer(Modifier.height(60.dp))
            Text("Abriendo la plataforma oficial…", fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            Text("Este canal requiere su propio sitio o suscripción oficial.", fontSize = 17.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(channel.url))) }) {
                Text("Abrir nuevamente")
            }
        }
    } else {
        val player = remember(channel.url) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(channel.url))
                prepare()
                playWhenReady = true
            }
        }
        DisposableEffect(player) { onDispose { player.release() } }
        Column(Modifier.fillMaxSize()) {
            Header(channel.name, onBack)
            AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } }, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp))
        }
    }
}

fun loadChannels(asset: String): List<Channel> {
    return try {
        val input = MainActivityHolder.app.assets.open("lists/$asset")
        val lines = BufferedReader(InputStreamReader(input)).readLines()
        val result = mutableListOf<Channel>()
        var name: String? = null
        for (line in lines) {
            val s = line.trim()
            if (s.startsWith("#EXTINF")) name = s.substringAfter(",", "Canal").trim()
            else if (s.isNotEmpty() && !s.startsWith("#") && name != null) { result += Channel(name!!, s, !s.contains(".m3u8") && !s.contains(".m3u")); name = null }
        }
        result
    } catch (_: Exception) { emptyList() }
}

object MainActivityHolder { lateinit var app: android.app.Application }
