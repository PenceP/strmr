package com.strmr.ai.ui.components

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
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

/**
 * VLC-based video player component for Android TV
 * Provides better codec support including Dolby Vision
 */
@Composable
fun VlcVideoPlayer(
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
    
    val libVLC = remember {
        LibVLC(context, arrayListOf(
            "--aout=opensles",
            "--audio-time-stretch",
            "--network-caching=3000",
            "--file-caching=3000",
            "--http-caching=3000",
            "--no-drop-late-frames",
            "--no-skip-frames",
            "--rtsp-tcp",
            "--no-lua",
            "--no-stats",
            // Use android_window for video output
            "--vout=android_window", 
            // Enable hardware acceleration
            "--avcodec-hw=any",
            // HTTP/HTTPS settings
            "--http-user-agent=VLC/4.0.0 LibVLC/4.0.0",
            "--http-reconnect",
            // Reduce logging
            "--verbose=1"
        ))
    }
    
    val mediaPlayer = remember {
        MediaPlayer(libVLC).apply {
            setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        isPlayerReady = true
                        isBuffering = false
                        hasError = false
                        Log.d("VlcVideoPlayer", "✅ VLC Player playing")
                    }
                    MediaPlayer.Event.Buffering -> {
                        val percent = event.buffering
                        isBuffering = percent < 100
                        if (percent % 25 == 0f || percent == 100f) { // Log every 25% and 100%
                            Log.d("VlcVideoPlayer", "⏳ Buffering: $percent%")
                        }
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d("VlcVideoPlayer", "🏁 Video playback ended")
                        onVideoEnded?.invoke()
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        hasError = true
                        errorMessage = "VLC playback error - video format may not be supported"
                        Log.e("VlcVideoPlayer", "❌ VLC Player error: $errorMessage")
                        onPlayerError?.invoke(errorMessage)
                    }
                    MediaPlayer.Event.Vout -> {
                        Log.d("VlcVideoPlayer", "🎥 Video output ready: ${event.voutCount} outputs")
                        if (event.voutCount > 0) {
                            isPlayerReady = true
                            isBuffering = false
                        }
                    }
                    MediaPlayer.Event.Opening -> {
                        Log.d("VlcVideoPlayer", "📂 Opening media...")
                        isBuffering = true
                    }
                    MediaPlayer.Event.Stopped -> {
                        Log.d("VlcVideoPlayer", "⏹️ VLC Player stopped")
                        isPlayerReady = false
                    }
                    MediaPlayer.Event.Paused -> {
                        Log.d("VlcVideoPlayer", "⏸️ VLC Player paused")
                    }
                    else -> {
                        Log.v("VlcVideoPlayer", "VLC Event: ${event.type}")
                    }
                }
            }
        }
    }
    
    DisposableEffect(mediaPlayer) {
        onDispose {
            Log.d("VlcVideoPlayer", "🔄 Disposing VLC Player")
            mediaPlayer.stop()
            mediaPlayer.release()
            libVLC.release()
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasError -> {
                VideoPlayerError(
                    message = errorMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).apply {
                            holder.setFormat(android.graphics.PixelFormat.RGBX_8888)
                            
                            mediaPlayer.vlcVout.setVideoView(this)
                            mediaPlayer.vlcVout.attachViews()
                            
                            val media = Media(libVLC, Uri.parse(videoUrl))
                            // Enable hardware decoding
                            media.setHWDecoderEnabled(true, false)
                            // Add specific options for better compatibility
                            media.addOption(":network-caching=2000")
                            media.addOption(":file-caching=2000")
                            media.addOption(":clock-jitter=0")
                            media.addOption(":clock-synchro=0")
                            
                            mediaPlayer.media = media
                            media.release()
                            
                            if (autoPlay) {
                                mediaPlayer.play()
                            }
                            
                            Log.d("VlcVideoPlayer", "🎬 VLC SurfaceView created for URL: $videoUrl")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.pause()
                                    } else {
                                        mediaPlayer.play()
                                    }
                                    true
                                }
                                keyEvent.key == Key.DirectionRight && keyEvent.type == KeyEventType.KeyDown -> {
                                    mediaPlayer.time = mediaPlayer.time + 10000 // Skip 10 seconds
                                    true
                                }
                                keyEvent.key == Key.DirectionLeft && keyEvent.type == KeyEventType.KeyDown -> {
                                    mediaPlayer.time = mediaPlayer.time - 10000 // Rewind 10 seconds
                                    true
                                }
                                else -> false
                            }
                        },
                    onRelease = {
                        mediaPlayer.vlcVout.detachViews()
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
                
                // Simple playback controls overlay
                if (isPlayerReady && !mediaPlayer.isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.8f)
                            )
                        ) {
                            Text(
                                text = "Press OK to play/pause\n← → to seek",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
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
private fun VideoPlayerError(
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
            }
        }
    }
}