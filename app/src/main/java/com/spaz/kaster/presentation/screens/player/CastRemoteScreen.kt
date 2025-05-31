package com.spaz.kaster.presentation.screens.player

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CastRemoteScreen() {
    val context = LocalContext.current
    val castContext = CastContext.getSharedInstance(context)
    val session: CastSession? = castContext.sessionManager.currentCastSession
    val remoteMediaClient: RemoteMediaClient? = session?.remoteMediaClient

    var duration by remember { mutableLongStateOf(0L) }
    var position by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // Actualiza el estado del reproductor periódicamente
    LaunchedEffect(remoteMediaClient) {
        while (true) {
            remoteMediaClient?.let { client ->
                duration = client.mediaStatus?.mediaInfo?.streamDuration ?: 0L
                position = client.approximateStreamPosition
                isPlaying = client.isPlaying
                title = client.mediaInfo?.metadata?.getString(MediaMetadata.KEY_TITLE) ?: ""
                deviceName = session.castDevice?.friendlyName ?: "Chromecast"
                // Cargar carátula si existe
                val images = client.mediaInfo?.metadata?.images
                if (!images.isNullOrEmpty()) {
                    try {
                        val url = images[0].url.toString()
                        val bitmap = BitmapFactory.decodeStream(java.net.URL(url).openStream())
                        imageBitmap = bitmap?.asImageBitmap()
                    } catch (_: Exception) {
                    }
                }
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transmitiendo en ",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = deviceName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
        // Carátula
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.Gray, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        // Nombre del video
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        // Botones de Cast y Subtítulos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = {
                remoteMediaClient?.stop()
                Toast.makeText(context, "Transmisión detenida", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Default.Cast,
                    contentDescription = "Detener transmisión",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { /* TODO: Subtítulos */ }) {
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = "Subtítulos",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Barra de progreso y tiempo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(position),
                fontSize = 14.sp,
                modifier = Modifier.width(60.dp)
            )
            Slider(
                value = if (duration > 0) position / duration.toFloat() else 0f,
                onValueChange = { value ->
                    val seekTo = (value * duration).toLong()
                    val options = MediaSeekOptions.Builder()
                        .setPosition(seekTo)
                        .build()
                    remoteMediaClient?.seek(options)
                },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatTime(duration),
                fontSize = 14.sp,
                modifier = Modifier.width(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Controles principales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val options = MediaSeekOptions.Builder()
                    .setPosition(position - 15000)
                    .build()
                remoteMediaClient?.seek(options)
            }) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Retroceder 15s",
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {
                if (isPlaying) remoteMediaClient?.pause() else remoteMediaClient?.play()
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {
                val options = MediaSeekOptions.Builder()
                    .setPosition(position + 15000)
                    .build()
                remoteMediaClient?.seek(options)
            }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Avanzar 15s",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    else
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
} 