package com.strmr.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.R
import com.strmr.ai.viewmodel.UpdateViewModel
import kotlinx.coroutines.delay

enum class SplashState {
    CHECKING_UPDATE,
    DOWNLOADING_UPDATE,
    LOADING_POSTERS,
    COMPLETE
}

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel()
) {
    val updateUiState by updateViewModel.uiState.collectAsState()
    var splashState by remember { mutableStateOf(SplashState.CHECKING_UPDATE) }
    var loadingProgress by remember { mutableStateOf(0f) }
    
    // Pulsating animation for text
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )

    // Add timeout for update check
    LaunchedEffect(Unit) {
        delay(5000) // 5 second timeout
        if (splashState == SplashState.CHECKING_UPDATE) {
            // Skip update check and go directly to loading posters
            splashState = SplashState.LOADING_POSTERS
        }
    }

    // Handle splash flow
    LaunchedEffect(updateUiState, splashState) {
        when (splashState) {
            SplashState.CHECKING_UPDATE -> {
                // Wait for update check to complete or timeout
                if (!updateUiState.isLoading) {
                    if (updateUiState.updateInfo?.hasUpdate == true) {
                        // Start download automatically
                        updateViewModel.downloadAndInstallUpdate()
                        splashState = SplashState.DOWNLOADING_UPDATE
                    } else {
                        // No update or check failed, move to loading posters
                        splashState = SplashState.LOADING_POSTERS
                    }
                }
            }
            
            SplashState.DOWNLOADING_UPDATE -> {
                // Monitor download progress
                loadingProgress = updateUiState.downloadProgress / 100f
                
                // If download completes or fails, the app will handle installation
                // We don't need to wait for installation completion
                if (!updateUiState.isDownloading) {
                    // The update worker will handle the installation
                    // We can proceed to the app
                    splashState = SplashState.LOADING_POSTERS
                }
            }
            
            SplashState.LOADING_POSTERS -> {
                // Simulate poster loading with animation
                for (i in 1..10) {
                    delay(100)
                    loadingProgress = i / 10f
                }
                splashState = SplashState.COMPLETE
            }
            
            SplashState.COMPLETE -> {
                // Small delay before transitioning
                delay(300)
                onSplashComplete()
            }
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Wallpaper background with blur
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD)
        )
        
        // Dark overlay for better text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo/title
            Text(
                text = "STRMR",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                color = Color.White,
                modifier = Modifier.padding(bottom = 48.dp)
            )
            
            // Status text
            Text(
                text = when (splashState) {
                    SplashState.CHECKING_UPDATE -> "Checking For Update"
                    SplashState.DOWNLOADING_UPDATE -> {
                        if (updateUiState.downloadStatus != null) {
                            updateUiState.downloadStatus!!
                        } else {
                            "Downloading Update ${(updateUiState.downloadProgress)}%"
                        }
                    }
                    SplashState.LOADING_POSTERS -> "Loading Posters"
                    SplashState.COMPLETE -> "Ready"
                },
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .padding(bottom = 24.dp)
            )
            
            // Progress indicator
            if (splashState == SplashState.DOWNLOADING_UPDATE || splashState == SplashState.LOADING_POSTERS) {
                LinearProgressIndicator(
                    progress = { loadingProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            } else if (splashState == SplashState.CHECKING_UPDATE) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Version info if update is being downloaded
            if (splashState == SplashState.DOWNLOADING_UPDATE && updateUiState.updateInfo != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Updating to version ${updateUiState.updateInfo?.latestVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}