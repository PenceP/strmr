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
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import android.util.Log
import androidx.compose.ui.input.key.*
import com.strmr.ai.utils.DeviceCapabilities

/**
 * Clean video player implementation following CloudStream's approach
 * Adheres to SOLID principles and removes "janky" autoplay handling
 * 
 * Single Responsibility: Handles video playback with clean state management
 */
@UnstableApi
@Composable
fun CleanVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlayerError: ((String) -> Unit)? = null,
    onVideoEnded: (() -> Unit)? = null,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    var playerState by remember { mutableStateOf<VideoPlayerState>(VideoPlayerState.Loading) }
    var useVlcFallback by remember { mutableStateOf(false) }
    var useSystemFallback by remember { mutableStateOf(false) }
    
    // Create player with optimized configuration (Dependency Inversion)
    val exoPlayer = remember {
        createOptimizedPlayer(context, videoUrl, autoPlay) { error ->
            when {
                isCodecError(error) -> {
                    Log.w("CleanVideoPlayer", "🔄 Codec error, switching to VLC")
                    useVlcFallback = true
                }
                else -> {
                    playerState = VideoPlayerState.Error(error.errorCodeName)
                    onPlayerError?.invoke(error.errorCodeName)
                }
            }
        }
    }
    
    // Simple state management (Single Responsibility)
    LaunchedEffect(exoPlayer) {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> playerState = VideoPlayerState.Ready
                    Player.STATE_ENDED -> {
                        playerState = VideoPlayerState.Ended
                        onVideoEnded?.invoke()
                    }
                }
            }
        })
    }
    
    // Cleanup (Open/Closed Principle - easy to extend)
    DisposableEffect(exoPlayer) {
        onDispose {
            if (!useVlcFallback && !useSystemFallback) {
                Log.d("CleanVideoPlayer", "🔄 Disposing ExoPlayer")
                exoPlayer.release()
            }
        }
    }
    
    // Render appropriate player (Interface Segregation)
    when {
        useSystemFallback -> SystemVideoPlayer(
            videoUrl = videoUrl,
            modifier = modifier,
            onPlayerError = onPlayerError,
            onVideoEnded = onVideoEnded,
            autoPlay = autoPlay
        )
        useVlcFallback -> VlcVideoPlayer(
            videoUrl = videoUrl,
            modifier = modifier,
            onPlayerError = { 
                Log.w("CleanVideoPlayer", "VLC failed, using system player")
                useSystemFallback = true
            },
            onVideoEnded = onVideoEnded,
            autoPlay = autoPlay
        )
        else -> ExoPlayerView(
            player = exoPlayer,
            playerState = playerState,
            modifier = modifier
        )
    }
}

/**
 * Creates an optimized ExoPlayer instance
 * Single Responsibility: Player creation and configuration
 */
@UnstableApi
private fun createOptimizedPlayer(
    context: android.content.Context,
    videoUrl: String,
    autoPlay: Boolean,
    onError: (PlaybackException) -> Unit
): ExoPlayer {
    Log.d("CleanVideoPlayer", "🎬 Creating optimized player for: $videoUrl")
    
    // Custom track selector for better codec handling
    val trackSelector = DefaultTrackSelector(context).apply {
        setParameters(
            buildUponParameters()
                .setPreferredVideoMimeTypes(
                    "video/avc",      // H.264 - most compatible
                    "video/hevc",     // H.265
                    "video/x-vnd.on2.vp9", // VP9
                )
                .build()
        )
    }
    
    return ExoPlayer.Builder(context)
        .setRenderersFactory(CustomRenderersFactory(context))
        .setTrackSelector(trackSelector)
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            playWhenReady = autoPlay // Clean autoplay - no manual intervention
            prepare()
            
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("CleanVideoPlayer", "❌ Player error: ${error.errorCodeName}")
                    onError(error)
                }
            })
        }
}

/**
 * ExoPlayer view component
 * Single Responsibility: Renders ExoPlayer with proper controls
 */
@Composable
private fun ExoPlayerView(
    player: ExoPlayer,
    playerState: VideoPlayerState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (playerState) {
            is VideoPlayerState.Loading -> LoadingIndicator()
            is VideoPlayerState.Error -> ErrorDisplay(playerState.message)
            is VideoPlayerState.Ready, is VideoPlayerState.Ended -> {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            controllerAutoShow = true
                            
                            // Android TV optimizations
                            if (DeviceCapabilities.isHighEndAndroidTV()) {
                                controllerHideOnTouch = false
                            }
                            
                            Log.d("CleanVideoPlayer", "📺 PlayerView configured")
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .focusable()
                        .onKeyEvent { keyEvent ->
                            handleKeyEvent(keyEvent, player)
                        }
                )
            }
        }
    }
}

/**
 * Key event handling for Android TV
 * Single Responsibility: Handle remote control input
 */
private fun handleKeyEvent(keyEvent: KeyEvent, player: ExoPlayer): Boolean {
    if (keyEvent.type != KeyEventType.KeyDown) return false
    
    return when (keyEvent.key) {
        Key.DirectionCenter -> {
            if (player.isPlaying) player.pause() else player.play()
            true
        }
        Key.Back -> false // Let parent handle
        else -> false
    }
}

/**
 * Check if error is codec-related
 * Single Responsibility: Error classification
 */
private fun isCodecError(error: PlaybackException): Boolean {
    val message = error.errorCodeName.lowercase()
    return message.contains("codec") || 
           message.contains("decoder") || 
           message.contains("no_exceeds_capabilities") ||
           error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
}

/**
 * Player state sealed class (Interface Segregation)
 */
sealed class VideoPlayerState {
    object Loading : VideoPlayerState()
    object Ready : VideoPlayerState()
    object Ended : VideoPlayerState()
    data class Error(val message: String) : VideoPlayerState()
}

/**
 * Loading indicator component
 * Single Responsibility: Show loading state
 */
@Composable
private fun LoadingIndicator() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Loading video...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
    }
}

/**
 * Error display component
 * Single Responsibility: Show error state
 */
@Composable
private fun ErrorDisplay(message: String) {
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
            Text(
                text = "Playback Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}