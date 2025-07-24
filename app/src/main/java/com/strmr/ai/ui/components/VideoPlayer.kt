package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * ExoPlayer-based video player component for Android TV
 * Optimized for trailer playback with D-pad navigation
 */
@UnstableApi
@Composable
fun VideoPlayer(
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
    var useVlcFallback by remember { mutableStateOf(false) }
    var useSystemFallback by remember { mutableStateOf(false) }
    
    // Create ExoPlayer instance with custom data source for YouTube URLs
    val exoPlayer = remember {
        // Check if this is a YouTube/googlevideo URL or proxy service URL
        val isYouTubeUrl = videoUrl.contains("googlevideo.com") || 
                          videoUrl.contains("youtube.com") ||
                          videoUrl.contains("invidious") ||
                          videoUrl.contains("piped") ||
                          videoUrl.contains("youtube-nocookie.com")
        
        // Create track selector to handle codec selection better
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredVideoMimeTypes(
                        // Prefer standard codecs over Dolby Vision
                        "video/avc",      // H.264
                        "video/hevc",     // H.265/HEVC (standard)
                        "video/x-vnd.on2.vp9", // VP9
                        "video/av01"      // AV1
                    )
                    .setAllowVideoMixedDecoderSupportAdaptiveness(true)
                    .setAllowAudioMixedDecoderSupportAdaptiveness(true)
                    .build()
            )
        }
        
        // Create load control with larger buffer for smoother playback
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS * 2, // Double max buffer
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()
        
        val player = if (isYouTubeUrl) {
            // Create OkHttpClient with necessary configuration for YouTube
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            
            // Create OkHttpDataSource.Factory with YouTube-compatible headers
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setDefaultRequestProperties(mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Accept-Encoding" to "gzip, deflate",
                    "Connection" to "keep-alive",
                    "Origin" to "https://www.youtube.com",
                    "Referer" to "https://www.youtube.com/"
                ))
            
            // Create media source factory with our custom data source
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Build ExoPlayer with custom configuration
            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        } else {
            // Standard ExoPlayer for non-YouTube URLs
            ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build()
        }
        
        player.apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = autoPlay
            
            // Add listener for player events
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPlayerReady = true
                            hasError = false
                            Log.d("VideoPlayer", "✅ Player ready for URL: $videoUrl")
                            
                            // Force playback to start if autoPlay is enabled
                            if (autoPlay && !isPlaying) {
                                Log.d("VideoPlayer", "🎬 Auto-starting playback")
                                play()
                            }
                        }
                        Player.STATE_ENDED -> {
                            Log.d("VideoPlayer", "🏁 Video playback ended")
                            onVideoEnded?.invoke()
                        }
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    Log.d("VideoPlayer", "▶️ Playing state changed: $isPlaying")
                }
                
                override fun onRenderedFirstFrame() {
                    Log.d("VideoPlayer", "🖼️ First frame rendered")
                    
                    // If autoplay is enabled but player isn't playing, force start
                    if (autoPlay && !isPlaying) {
                        Log.d("VideoPlayer", "🎬 First frame rendered but not playing - forcing start")
                        play()
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("VideoPlayer", "❌ ExoPlayer error: ${error.message}", error)
                    
                    // Check for codec/format issues that VLC might handle better
                    val isCodecError = error.message?.contains("format_supported=NO_EXCEEDS_CAPABILITIES") == true ||
                                      error.message?.contains("NO_EXCEEDS_CAPABILITIES") == true ||
                                      error.message?.contains("MediaCodecVideoRenderer error") == true ||
                                      error.message?.contains("MediaCodecVideoDecoderException") == true ||
                                      error.message?.contains("Decoder failed") == true
                    
                    if (isCodecError) {
                        Log.w("VideoPlayer", "🔄 Codec error detected, switching to VLC player")
                        useVlcFallback = true
                        this@apply.release()
                        return
                    }
                    
                    // For non-codec errors, show error message
                    hasError = true
                    errorMessage = when {
                        // Access errors
                        error.cause?.message?.contains("403") == true -> {
                            "Video access denied. The URL may have expired."
                        }
                        // Proxy service errors
                        error.cause?.message?.contains("404") == true && 
                        (videoUrl.contains("invidious") || videoUrl.contains("piped")) -> {
                            "Proxy service unavailable. Please try again."
                        }
                        else -> error.message ?: "Unknown playback error"
                    }
                    
                    onPlayerError?.invoke(errorMessage)
                }
            })
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(exoPlayer) {
        onDispose {
            if (!useVlcFallback) {
                Log.d("VideoPlayer", "🔄 Disposing ExoPlayer")
                exoPlayer.release()
            }
        }
    }
    
    // Delayed autoplay fallback - sometimes video surface needs time to be ready
    LaunchedEffect(isPlayerReady, autoPlay) {
        if (isPlayerReady && autoPlay && !exoPlayer.isPlaying) {
            Log.d("VideoPlayer", "⏰ Delayed autoplay check - waiting 500ms for surface")
            delay(500)
            if (!exoPlayer.isPlaying) {
                Log.d("VideoPlayer", "🎬 Forcing delayed autoplay")
                exoPlayer.play()
            }
        }
    }
    
    // If System fallback is needed, use System MediaPlayer
    if (useSystemFallback) {
        Log.d("VideoPlayer", "📺 Using System MediaPlayer as final fallback")
        SystemVideoPlayer(
            videoUrl = videoUrl,
            modifier = modifier,
            onPlayerError = onPlayerError,
            onVideoEnded = onVideoEnded,
            autoPlay = autoPlay
        )
        return
    }
    
    // If VLC fallback is needed, use VLC player instead
    if (useVlcFallback) {
        Log.d("VideoPlayer", "📺 Using VLC player as fallback for codec support")
        VlcVideoPlayer(
            videoUrl = videoUrl,
            modifier = modifier,
            onPlayerError = { error ->
                Log.w("VideoPlayer", "VLC also failed, trying System MediaPlayer")
                useSystemFallback = true
            },
            onVideoEnded = onVideoEnded,
            autoPlay = autoPlay
        )
        return
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            hasError -> {
                // Error state
                VideoPlayerError(
                    message = errorMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !isPlayerReady -> {
                // Loading state
                VideoPlayerLoading(
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Player ready - show video
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            controllerAutoShow = true
                            controllerHideOnTouch = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                            
                            // Enable focus for Android TV navigation
                            isFocusable = true
                            isFocusableInTouchMode = true
                            
                            // Request focus to ensure player is ready for input
                            requestFocus()
                            
                            Log.d("VideoPlayer", "🎬 PlayerView created and configured")
                        }
                    },
                    update = { playerView ->
                        // Ensure the player view is properly focused and ready
                        if (isPlayerReady && autoPlay && !exoPlayer.isPlaying) {
                            Log.d("VideoPlayer", "🔄 Update: Ensuring playback starts")
                            playerView.requestFocus()
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            when {
                                keyEvent.key == Key.DirectionCenter && keyEvent.type == KeyEventType.KeyDown -> {
                                    // Play/pause with center button
                                    if (exoPlayer.isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        exoPlayer.play()
                                    }
                                    true
                                }
                                keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown -> {
                                    // Let back button be handled by parent
                                    false
                                }
                                else -> false
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerLoading(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text = "Loading trailer...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun VideoPlayerError(
    message: String,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .padding(32.dp)
                .focusRequester(focusRequester)
                .focusable(),
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
                    text = "Unable to play trailer",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Press BACK to return",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}