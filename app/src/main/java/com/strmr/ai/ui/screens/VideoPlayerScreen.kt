package com.strmr.ai.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.strmr.ai.data.YouTubeExtractor
import com.strmr.ai.ui.components.CleanVideoPlayer
import com.strmr.ai.ui.components.YouTubeWebPlayer

/**
 * Full-screen video player screen for trailers
 * Uses WebView-based YouTube player for reliable in-app playback
 */
@UnstableApi
@Composable
fun VideoPlayerScreen(
    videoUrl: String,
    title: String = "Trailer",
    onBack: () -> Unit,
    youtubeExtractor: YouTubeExtractor,
) {
    val context = LocalContext.current
    var showFallback by remember { mutableStateOf(false) }
    var useWebViewPlayer by remember { mutableStateOf(false) }
    var actualVideoUrl by remember { mutableStateOf(videoUrl) }
    var isProcessing by remember { mutableStateOf(false) } // Start with false since we'll use WebView primarily
    val isYouTubeUrl = remember { youtubeExtractor.isYouTubeUrl(videoUrl) }
    val videoId =
        remember {
            if (isYouTubeUrl) youtubeExtractor.extractVideoId(videoUrl) else null
        }

    LaunchedEffect(videoUrl) {
        Log.d("VideoPlayerScreen", "🎬 Opening video player for: $videoUrl")
        Log.d("VideoPlayerScreen", "📺 Is YouTube URL: $isYouTubeUrl")
        Log.d("VideoPlayerScreen", "🆔 Video ID: $videoId")

        if (isYouTubeUrl && videoId != null) {
            // For YouTube URLs, use WebView player as primary method
            Log.d("VideoPlayerScreen", "🌐 Using WebView-based YouTube player")
            useWebViewPlayer = true
            showFallback = false
        } else if (!isYouTubeUrl) {
            // Direct video URL - play in ExoPlayer
            Log.d("VideoPlayerScreen", "🎬 Using ExoPlayer for direct video URL")
            actualVideoUrl = videoUrl
            useWebViewPlayer = false
            showFallback = false
        } else {
            // YouTube URL but no video ID - show fallback
            Log.d("VideoPlayerScreen", "⚠️ Invalid YouTube URL, showing fallback")
            showFallback = true
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onKeyEvent { keyEvent ->
                    when {
                        keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown -> {
                            onBack()
                            true
                        }
                        else -> false
                    }
                },
    ) {
        when {
            isProcessing -> {
                // Loading state while processing YouTube URL
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isYouTubeUrl) "Processing YouTube trailer..." else "Loading trailer...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            showFallback && isYouTubeUrl -> {
                // YouTube fallback UI when WebView fails
                YouTubeFallbackUI(
                    videoUrl = videoUrl,
                    videoId = videoId,
                    title = title,
                    onPlayExternal = {
                        try {
                            // Try YouTube app first
                            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                            youtubeIntent.setPackage("com.google.android.youtube")
                            context.startActivity(youtubeIntent)
                        } catch (e: Exception) {
                            // Fallback to browser
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                context.startActivity(browserIntent)
                            } catch (e2: Exception) {
                                Log.e("VideoPlayerScreen", "❌ Could not open YouTube URL", e2)
                            }
                        }
                    },
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            useWebViewPlayer && videoId != null -> {
                // Use WebView-based YouTube player for YouTube URLs
                YouTubeWebPlayer(
                    videoId = videoId,
                    modifier = Modifier.fillMaxSize(),
                    onError = { error ->
                        Log.e("VideoPlayerScreen", "❌ WebView player error: $error")
                        showFallback = true
                    },
                    onReady = {
                        Log.d("VideoPlayerScreen", "✅ WebView player ready")
                    },
                    onBack = onBack,
                )
            }
            else -> {
                // Use CleanVideoPlayer for direct video URLs
                CleanVideoPlayer(
                    videoUrl = actualVideoUrl,
                    modifier = Modifier.fillMaxSize(),
                    onPlayerError = { error ->
                        Log.e("VideoPlayerScreen", "❌ ExoPlayer error: $error")
                        if (isYouTubeUrl) {
                            // If it was a YouTube URL and ExoPlayer failed, show fallback
                            showFallback = true
                        }
                    },
                    onVideoEnded = {
                        Log.d("VideoPlayerScreen", "🏁 Video ended, returning to previous screen")
                        onBack()
                    },
                )
            }
        }
    }
}

@Composable
private fun YouTubeFallbackUI(
    videoUrl: String,
    videoId: String?,
    title: String,
    onPlayExternal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.8f)
                    .focusRequester(focusRequester),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                // Video icon
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = "Video",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp),
                )

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                // Info text
                Text(
                    text = "This trailer is hosted on YouTube. Click below to open it in the YouTube app or browser.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        onClick = onPlayExternal,
                        modifier = Modifier.focusable(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Watch Trailer")
                    }

                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.focusable(),
                    ) {
                        Text("Back")
                    }
                }

                // Helpful text
                Text(
                    text = "Tip: Install the YouTube app for the best viewing experience",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
