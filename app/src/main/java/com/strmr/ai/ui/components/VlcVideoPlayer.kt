package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * TEMPORARILY DISABLED - VLC-based video player component
 * LibVLC dependency removed due to network issues with maven.videolan.org
 * TODO: Re-enable when LibVLC dependency is restored
 */
@Composable
fun VlcVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onPlayerError: ((String) -> Unit)? = null,
    onVideoEnded: (() -> Unit)? = null,
    autoPlay: Boolean = true,
) {
    // VLC Player temporarily disabled - show error message
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VLC Player Temporarily Unavailable",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LibVLC dependency temporarily removed.\nPlease use ExoPlayer for video playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
