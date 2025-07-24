package com.strmr.ai.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * System MediaPlayer-based video player component for Android TV
 * Last resort fallback when ExoPlayer and VLC both fail
 */
@Composable
fun SystemVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlayerError: ((String) -> Unit)? = null,
    onVideoEnded: (() -> Unit)? = null,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    var isPlayerReady by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isBuffering by remember { mutableStateOf(true) }
    
    val mediaPlayer = remember {
        MediaPlayer().apply {
            setOnPreparedListener {
                isPlayerReady = true
                isBuffering = false
                hasError = false
                Log.d("SystemVideoPlayer", "✅ System MediaPlayer prepared")
                if (autoPlay) {
                    start()
                }
            }
            
            setOnBufferingUpdateListener { _, percent ->
                isBuffering = percent < 100
                if (percent % 25 == 0) {
                    Log.d("SystemVideoPlayer", "⏳ Buffering: $percent%")
                }
            }
            
            setOnCompletionListener {
                Log.d("SystemVideoPlayer", "🏁 Video playback completed")
                onVideoEnded?.invoke()
            }
            
            setOnErrorListener { _, what, extra ->
                hasError = true
                errorMessage = "System MediaPlayer error (what=$what, extra=$extra)"
                Log.e("SystemVideoPlayer", "❌ $errorMessage")
                onPlayerError?.invoke(errorMessage)
                true
            }
            
            setOnInfoListener { _, what, extra ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        isBuffering = true
                        Log.d("SystemVideoPlayer", "⏳ Buffering started")
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        isBuffering = false
                        Log.d("SystemVideoPlayer", "✅ Buffering ended")
                    }
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        isPlayerReady = true
                        isBuffering = false
                        Log.d("SystemVideoPlayer", "🎥 Video rendering started")
                    }
                }
                true
            }
        }
    }
    
    DisposableEffect(mediaPlayer) {
        onDispose {
            Log.d("SystemVideoPlayer", "🔄 Disposing System MediaPlayer")
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                Log.w("SystemVideoPlayer", "Error releasing MediaPlayer: ${e.message}")
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasError -> {
                SystemVideoPlayerError(
                    message = errorMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.addCallback(object : android.view.SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                                    try {
                                        mediaPlayer.setDisplay(holder)
                                        mediaPlayer.setDataSource(ctx, Uri.parse(videoUrl))
                                        mediaPlayer.prepareAsync()
                                        Log.d("SystemVideoPlayer", "🎬 System MediaPlayer setup for: $videoUrl")
                                    } catch (e: Exception) {
                                        hasError = true
                                        errorMessage = "Failed to setup MediaPlayer: ${e.message}"
                                        Log.e("SystemVideoPlayer", errorMessage, e)
                                    }
                                }
                                
                                override fun surfaceChanged(
                                    holder: android.view.SurfaceHolder,
                                    format: Int,
                                    width: Int,
                                    height: Int
                                ) {
                                    Log.d("SystemVideoPlayer", "📐 Surface changed: ${width}x${height}")
                                }
                                
                                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                                    Log.d("SystemVideoPlayer", "🔻 Surface destroyed")
                                }
                            })
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                                    try {
                                        if (mediaPlayer.isPlaying) {
                                            mediaPlayer.pause()
                                        } else {
                                            mediaPlayer.start()
                                        }
                                    } catch (e: Exception) {
                                        Log.w("SystemVideoPlayer", "Error controlling playback: ${e.message}")
                                    }
                                    true
                                }
                                keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                                    try {
                                        val currentPos = mediaPlayer.currentPosition
                                        val duration = mediaPlayer.duration
                                        val newPos = minOf(currentPos + 10000, duration)
                                        mediaPlayer.seekTo(newPos)
                                    } catch (e: Exception) {
                                        Log.w("SystemVideoPlayer", "Error seeking: ${e.message}")
                                    }
                                    true
                                }
                                keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                                    try {
                                        val currentPos = mediaPlayer.currentPosition
                                        val newPos = maxOf(currentPos - 10000, 0)
                                        mediaPlayer.seekTo(newPos)
                                    } catch (e: Exception) {
                                        Log.w("SystemVideoPlayer", "Error seeking: ${e.message}")
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                )
                
                // Loading/Buffering overlay
                if (isBuffering || !isPlayerReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isBuffering) "Buffering..." else "Loading video...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Simple controls overlay
                if (isPlayerReady && !isBuffering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Card(
                            modifier = Modifier
                                .padding(16.dp)
                                .background(Color.Transparent),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = "OK: Play/Pause • ←→: Seek • Back: Exit",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemVideoPlayerError(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Unable to play video",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "This video format may not be supported on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}