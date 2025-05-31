package com.spaz.kaster.presentation.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Launcher para solicitar permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kaster") },
                actions = {
                    if (uiState.isCastAvailable) {
                        IconButton(onClick = { /* TODO: Implementar selección de dispositivo Cast */

                        }) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Transmitir"
                            )
                        }
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
                        onVideoClick = { video -> /* TODO: Implementar reproducción */ }
                    )
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
            if (video.thumbnail != null) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop,
                    onError = { /* Si falla, no hace nada, se muestra el icono abajo */ }
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