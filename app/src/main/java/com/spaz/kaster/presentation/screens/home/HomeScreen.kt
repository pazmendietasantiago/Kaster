package com.spaz.kaster.presentation.screens.home

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.mediarouter.app.MediaRouteButton
import androidx.navigation.NavController
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner as axComposeLocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = axComposeLocalLifecycleOwner.current
    val context = LocalContext.current
    var castContext by remember { mutableStateOf<CastContext?>(null) }
    val session: CastSession? = castContext?.sessionManager?.currentCastSession
    val remoteMediaClient: RemoteMediaClient? = session?.remoteMediaClient
    var miniTitle by remember { mutableStateOf("") }
    var miniIsPlaying by remember { mutableStateOf(false) }
    var miniDuration by remember { mutableStateOf(0L) }
    var miniPosition by remember { mutableStateOf(0L) }

    // Inicializar CastContext solo una vez
    LaunchedEffect(Unit) {
        castContext = CastContext.getSharedInstance(context)
        viewModel.setCastContext(context)
    }

    // Recargar videos al volver a la pantalla
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.hasStoragePermission) {
                viewModel.reloadVideos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Actualiza el mini reproductor periódicamente
    LaunchedEffect(remoteMediaClient) {
        while (true) {
            remoteMediaClient?.let { client ->
                miniTitle =
                    client.mediaInfo?.metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)
                        ?: ""
                miniIsPlaying = client.isPlaying
                miniDuration = client.mediaStatus?.mediaInfo?.streamDuration ?: 0L
                miniPosition = client.approximateStreamPosition
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kaster") },
                actions = {
                    // Botón de Cast
                    if (castContext != null) {
                        AndroidView(
                            factory = { context ->
                                val themedContext = ContextThemeWrapper(
                                    context,
                                    androidx.appcompat.R.style.Theme_AppCompat_DayNight_NoActionBar
                                )
                                MediaRouteButton(themedContext).apply {
                                    CastButtonFactory.setUpMediaRouteButton(themedContext, this)
                                }
                            },
                            modifier = Modifier
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                !uiState.hasStoragePermission -> {
                    PermissionRequest(
                        onRequestPermission = {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                    Manifest.permission.READ_MEDIA_VIDEO
                                else
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }
                    )
                }

                uiState.videos.isEmpty() -> {
                    EmptyContent()
                }

                else -> {
                    VideoList(
                        videos = uiState.videos,
                        onVideoClick = { video ->
                            if (uiState.isCastAvailable) {
                                viewModel.transmitVideoToCast(video)
                                Toast.makeText(
                                    context,
                                    "Transmitiendo a Chromecast...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("cast_remote")
                            } else {
                                navController.navigate("player?uri=${video.path}&name=${video.name}")
                            }
                        }
                    )
                }
            }
            // Mini reproductor
            if (uiState.isCastAvailable && remoteMediaClient != null && miniTitle.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { navController.navigate("cast_remote") },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = miniTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Slider(
                                value = if (miniDuration > 0) miniPosition / miniDuration.toFloat() else 0f,
                                onValueChange = { value ->
                                    val seekTo = (value * miniDuration).toLong()
                                    val options =
                                        com.google.android.gms.cast.MediaSeekOptions.Builder()
                                            .setPosition(seekTo)
                                            .build()
                                    remoteMediaClient.seek(options)
                                },
                                modifier = Modifier.height(16.dp)
                            )
                        }
                        IconButton(onClick = {
                            if (miniIsPlaying) remoteMediaClient.pause() else remoteMediaClient.play()
                        }) {
                            Icon(
                                imageVector = if (miniIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (miniIsPlaying) "Pausar" else "Reproducir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Se necesita permiso para acceder a los videos",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Solicitar permiso")
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay videos disponibles",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun VideoList(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(videos) { video ->
            VideoItem(
                video = video,
                onClick = { onVideoClick(video) }
            )
        }
    }
}

@Composable
private fun VideoItem(
    video: Video,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnailBitmap by remember(video.path) { mutableStateOf<Bitmap?>(null) }

    // Intentar cargar el thumbnail solo una vez
    LaunchedEffect(video.path) {
        try {
            val bitmap = context.contentResolver.loadThumbnail(
                video.path.toUri(),
                Size(120, 120),
                null
            )

            thumbnailBitmap = bitmap
        } catch (e: Exception) {
            thumbnailBitmap = null

            Toast.makeText(context, e.message, Toast.LENGTH_SHORT)
                .show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.VideoFile,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = video.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = video.duration,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 