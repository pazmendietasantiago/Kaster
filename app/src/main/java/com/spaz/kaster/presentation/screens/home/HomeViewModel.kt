package com.spaz.kaster.presentation.screens.home

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.media.widget.ExpandedControllerActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import com.spaz.kaster.util.LocalHttpServer
import androidx.core.net.toUri


@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var castContext: CastContext? = null
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _uiState.update { it.copy(isCastAvailable = true) }
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            _uiState.update { it.copy(isCastAvailable = false) }
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _uiState.update { it.copy(isCastAvailable = true) }
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _uiState.update { it.copy(isCastAvailable = false) }
        }

        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
    }

    private var localHttpServer: LocalHttpServer? = null

    init {
        checkStoragePermission()
        loadVideos()
    }

    private fun checkStoragePermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }


        _uiState.update { it.copy(hasStoragePermission = hasPermission) }
    }

    fun requestStoragePermission() {
        // TODO: Implementar solicitud de permisos
    }

    private fun loadVideos() {
        viewModelScope.launch {
            val videoList = mutableListOf<Video>()
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val query = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val durationMs = cursor.getLong(durationColumn)
                    val duration = formatDuration(durationMs)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    videoList.add(
                        Video(
                            id = id,
                            name = name,
                            path = contentUri.toString(),
                            duration = duration,
                            thumbnail = null
                        )
                    )
                }
            }
            _uiState.update { it.copy(videos = videoList) }
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasStoragePermission = granted) }
        if (granted) {
            loadVideos()
        }
    }

    fun reloadVideos() {
        loadVideos()
    }

    fun setCastContext(context: Context) {
        castContext = CastContext.getSharedInstance(context)
        castContext?.sessionManager?.addSessionManagerListener(
            sessionManagerListener, CastSession::class.java
        )
        // Estado inicial
        val isActive = castContext?.sessionManager?.currentCastSession != null
        _uiState.update { it.copy(isCastAvailable = isActive) }
    }

    fun transmitVideoToCast(video: Video) {
        val session = castContext?.sessionManager?.currentCastSession ?: return
        val remoteMediaClient = session.remoteMediaClient ?: return
        // Iniciar el servidor HTTP local si no está iniciado
        if (localHttpServer == null) {
            localHttpServer = LocalHttpServer(context)
            localHttpServer?.start()
        }
        // Servir el archivo actual
        localHttpServer?.serveFile(video.path.toUri(), video.name)
        val videoUrl = localHttpServer?.getVideoUrl() ?: return
        val mediaMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, video.name)
        }
        val mediaInfo = MediaInfo.Builder(videoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .setMetadata(mediaMetadata)
            .build()
        val requestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .build()
        remoteMediaClient.load(requestData)
    }
}

data class HomeUiState(
    val hasStoragePermission: Boolean = false,
    val videos: List<Video> = emptyList(),
    val isCastAvailable: Boolean = false
)

data class Video(
    val id: Long,
    val name: String,
    val path: String,
    val duration: String,
    val thumbnail: String? = null
) 