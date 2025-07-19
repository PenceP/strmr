package com.strmr.ai.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.*

/**
 * WebView-based YouTube player that embeds videos directly
 * This approach is similar to how Stremio handles YouTube content
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null,
    onReady: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    DisposableEffect(videoId) {
        Log.d("YouTubeWebPlayer", "🌐 Initializing WebView player for video: $videoId")
        onDispose {
            Log.d("YouTubeWebPlayer", "🔄 Disposing WebView player")
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.key == androidx.compose.ui.input.key.Key.Back && 
                    keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown -> {
                        onBack?.invoke()
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Enable JavaScript
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        // Enable hardware acceleration
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        
                        // Set user agent to mimic a TV browser
                        userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 SMART-TV"
                    }
                    
                    // Set background color
                    setBackgroundColor(Color.BLACK)
                    
                    // WebView client to handle navigation
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            Log.d("YouTubeWebPlayer", "🔗 URL loading: $url")
                            
                            // Allow YouTube embed URLs
                            if (url.contains("youtube.com/embed") || 
                                url.contains("youtube-nocookie.com/embed") ||
                                url.contains("googlevideo.com")) {
                                return false
                            }
                            
                            // Block external navigation
                            return true
                        }
                        
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("YouTubeWebPlayer", "✅ Page loaded: $url")
                            isLoading = false
                            
                            // Inject CSS to hide YouTube UI elements and make it fullscreen
                            view?.evaluateJavascript("""
                                (function() {
                                    var style = document.createElement('style');
                                    style.innerHTML = `
                                        body { margin: 0; padding: 0; overflow: hidden; background: black; }
                                        .ytp-chrome-top, .ytp-gradient-top { display: none !important; }
                                        .ytp-pause-overlay { display: none !important; }
                                        .ytp-related-videos { display: none !important; }
                                        iframe { position: fixed; top: 0; left: 0; width: 100%; height: 100%; border: none; }
                                        .video-ads, .ytp-ad-module { display: none !important; }
                                    `;
                                    document.head.appendChild(style);
                                    
                                    // Auto-play and maximize player
                                    var player = document.querySelector('video');
                                    if (player) {
                                        player.play();
                                    }
                                })();
                            """.trimIndent()) { result ->
                                Log.d("YouTubeWebPlayer", "🎨 CSS injection result: $result")
                            }
                        }
                    }
                    
                    // Chrome client for JavaScript console messages
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            Log.d("YouTubeWebPlayer", "📝 Console: ${consoleMessage?.message()}")
                            return true
                        }
                    }
                    
                    // Build the embed HTML
                    val embedHtml = buildYouTubeEmbedHtml(videoId)
                    
                    // Load the HTML
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        embedHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    
                    Log.d("YouTubeWebPlayer", "🎬 Loading YouTube embed for video: $videoId")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Loading Trailer...",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Build HTML for YouTube embed with TV-optimized settings
 */
private fun buildYouTubeEmbedHtml(videoId: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                html, body { 
                    width: 100%; 
                    height: 100%; 
                    background: black; 
                    overflow: hidden;
                }
                #player-container {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                }
                #player {
                    width: 100%;
                    height: 100%;
                }
            </style>
        </head>
        <body>
            <div id="player-container">
                <div id="player"></div>
            </div>
            
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
                
                var player;
                function onYouTubeIframeAPIReady() {
                    console.log('YouTube API Ready');
                    player = new YT.Player('player', {
                        height: '100%',
                        width: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': 1,
                            'controls': 1,
                            'rel': 0,
                            'showinfo': 0,
                            'modestbranding': 1,
                            'playsinline': 1,
                            'fs': 1,
                            'cc_load_policy': 0,
                            'iv_load_policy': 3,
                            'autohide': 1,
                            'enablejsapi': 1,
                            'origin': 'https://www.youtube.com'
                        },
                        events: {
                            'onReady': onPlayerReady,
                            'onStateChange': onPlayerStateChange,
                            'onError': onPlayerError
                        }
                    });
                }
                
                function onPlayerReady(event) {
                    console.log('Player ready');
                    event.target.setPlaybackQuality('hd720');
                    event.target.playVideo();
                }
                
                function onPlayerStateChange(event) {
                    console.log('Player state:', event.data);
                    if (event.data == YT.PlayerState.ENDED) {
                        console.log('Video ended');
                    }
                }
                
                function onPlayerError(event) {
                    console.error('Player error:', event.data);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}