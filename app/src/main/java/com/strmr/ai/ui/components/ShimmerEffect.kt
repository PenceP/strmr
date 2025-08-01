package com.strmr.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for skeleton loading states
 */
@Composable
fun Modifier.shimmer(): Modifier {
    val shimmerColors =
        listOf(
            Color.Gray.copy(alpha = 0.3f),
            Color.Gray.copy(alpha = 0.1f),
            Color.Gray.copy(alpha = 0.3f),
        )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1200,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_translate",
    )

    return this.background(
        brush =
            Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(x = translateAnimation - 200f, y = translateAnimation - 200f),
                end = Offset(x = translateAnimation, y = translateAnimation),
            ),
    )
}

/**
 * Shimmer placeholder box with rounded corners
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(cornerRadius.dp))
                .shimmer(),
    )
}

/**
 * Shimmer placeholder for text with specific height
 */
@Composable
fun ShimmerText(
    modifier: Modifier = Modifier,
    height: Int = 16,
    cornerRadius: Int = 4,
) {
    ShimmerBox(
        modifier = modifier.height(height.dp),
        cornerRadius = cornerRadius,
    )
}
