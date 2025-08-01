package com.strmr.ai.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.R
import com.strmr.ai.data.OnboardingState
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.OnboardingViewModel

/**
 * Elegant onboarding screen with database pre-population
 * Similar to Discord's loading experience with fun messages
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val progress by viewModel.onboardingProgress.collectAsStateWithLifecycle()

    // Start onboarding when screen is first composed
    LaunchedEffect(Unit) {
        viewModel.startOnboarding()
    }

    // Navigate away when onboarding is complete
    LaunchedEffect(progress.state) {
        if (progress.state == OnboardingState.COMPLETED) {
            kotlinx.coroutines.delay(1500) // Show completion message briefly
            onOnboardingComplete()
        }
    }

    OnboardingContent(
        progress = progress,
        onRetry = { viewModel.startOnboarding() },
        onSkip = onOnboardingComplete,
    )
}

@Composable
private fun OnboardingContent(
    progress: com.strmr.ai.data.OnboardingProgress,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Wallpaper background with blur
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
            contentScale = ContentScale.Crop,
        )

        // Dark overlay for better text readability
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(48.dp),
        ) {
            // App Logo/Title
            Text(
                text = "STRMR",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 8.sp,
            )

            // Spacer(modifier = Modifier.height(16.dp))

            // Text(
            //    text = "Your Android TV Streaming Companion",
            //    fontSize = 16.sp,
            //    color = Color.White.copy(alpha = 0.7f),
            //    textAlign = TextAlign.Center
            // )

            Spacer(modifier = Modifier.height(48.dp))

            // Main loading message with animation
            AnimatedLoadingMessage(
                message = progress.message,
                state = progress.state,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Progress bar
            AnimatedProgressBar(
                progress = progress.progress,
                isError = progress.state == OnboardingState.ERROR,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current task (more technical info)
            progress.currentTask?.let { task ->
                Text(
                    text = task,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error handling
            if (progress.state == OnboardingState.ERROR) {
                ErrorActions(
                    onRetry = onRetry,
                    onSkip = onSkip,
                    error = progress.error,
                )
            }
        }

        // Skip button (top right)
        if (progress.state != OnboardingState.COMPLETED && progress.state != OnboardingState.ERROR) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                TextButton(
                    onClick = onSkip,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.6f),
                        ),
                ) {
                    Text("Skip", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun AnimatedLoadingMessage(
    message: String,
    state: OnboardingState,
) {
    // Pulsing animation for loading states
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "alpha",
    )

    val textColor =
        when (state) {
            OnboardingState.ERROR -> Color(0xFFff6b6b)
            OnboardingState.COMPLETED -> Color(0xFF51cf66)
            else -> Color.White.copy(alpha = alpha)
        }

    Text(
        text = message,
        fontSize = 18.sp,
        color = textColor,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
}

@Composable
private fun AnimatedProgressBar(
    progress: Float,
    isError: Boolean,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "progress",
    )

    Column {
        // Progress bar
        Box(
            modifier =
                Modifier
                    .width(300.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush =
                                if (isError) {
                                    Brush.horizontalGradient(listOf(Color(0xFFff6b6b), Color(0xFFff6b6b)))
                                } else {
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                Color(0xFF667eea),
                                                Color(0xFF764ba2),
                                            ),
                                    )
                                },
                        ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress percentage
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ErrorActions(
    onRetry: () -> Unit,
    onSkip: () -> Unit,
    error: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        error?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color(0xFFff6b6b).copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = onRetry,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                border =
                    ButtonDefaults.outlinedButtonBorder.copy(
                        brush =
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
                            ),
                    ),
            ) {
                Text("Retry")
            }

            TextButton(
                onClick = onSkip,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f),
                    ),
            ) {
                Text("Continue Anyway")
            }
        }
    }
}
