package com.strmr.ai.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.strmr.ai.utils.resolveImageSource

@Composable
fun LandscapeMediaCard(
    title: String,
    landscapeUrl: String?,
    logoUrl: String? = null,
    progress: Float = 0f,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomRightLabel: String? = null
) {
    val cardWidth by animateDpAsState(
        targetValue = if (isSelected) 240.dp else 220.dp,
        animationSpec = tween(durationMillis = 200)
    )
    val cardHeight by animateDpAsState(
        targetValue = if (isSelected) 155.dp else 135.dp,
        animationSpec = tween(durationMillis = 200)
    )

    Column(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Transparent),
            contentAlignment = Alignment.BottomStart
        ) {
            val cardHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val resolvedLandscapeSource = resolveImageSource(landscapeUrl)
            if (resolvedLandscapeSource != null) {
                AsyncImage(
                    model = resolvedLandscapeSource,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay from bottom (black) to halfway up (transparent)
                if (!logoUrl.isNullOrBlank()) {
                    Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.1f)
                                ),
                                startY = cardHeightPx,
                                endY = cardHeightPx / 2f
                            )
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            // Centered logo with placeholder text
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(54.dp)
                    .widthIn(max = 160.dp)
            ) {
                val resolvedLogoSource = resolveImageSource(logoUrl)
                if (resolvedLogoSource != null) {
                    AsyncImage(
                        model = resolvedLogoSource,
                        contentDescription = "$title logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Show title text as placeholder when logo is not available
                    Text(
                        text = title,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
            // Bottom center label (e.g., S:E)
            if (!bottomRightLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = bottomRightLabel,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.0f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (progress > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color.Red.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress / 100f)
                        .background(Color.Red)
                )
            }
        }
    }
} 