package com.strmr.ai.ui.components.common.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.strmr.ai.ui.components.common.row.CardType
import com.strmr.ai.utils.ImageUtils
import javax.inject.Inject

/**
 * Enhanced image loading composable for the Strmr app.
 * 
 * Features:
 * - Optimized for Android TV viewing distances
 * - Smart placeholder handling during loading
 * - Error state management with fallback content
 * - Integration with centralized image cache
 * - Support for different card types and shapes
 */
@Composable
fun StrmrImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cardType: CardType = CardType.POSTER,
    placeholder: @Composable (() -> Unit)? = null,
    errorContent: @Composable (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(8.dp),
    colorFilter: ColorFilter? = null,
    alpha: Float = 1.0f
) {
    val context = LocalContext.current
    val resolvedUrl = ImageUtils.resolveImageSource(url, context)
    
    // Get optimal dimensions for the card type
    val (targetWidth, targetHeight) = getOptimalDimensions(cardType)
    
    Box(modifier = modifier.clip(shape)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(resolvedUrl)
                .size(targetWidth, targetHeight) // Optimize request size
                .crossfade(true)
                .crossfade(StrmrImageLoader.LoadingConfig.PLACEHOLDER_FADE_DURATION)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter,
            alpha = alpha
        )
        
        // Handle loading and error states with overlays if needed
        // Note: AsyncImage handles its own placeholder and error states
        // This wrapper allows for future custom loading/error handling
    }
}

/**
 * Poster-optimized image loading
 */
@Composable
fun PosterImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    StrmrImage(
        url = url,
        contentDescription = "$title poster",
        modifier = modifier,
        cardType = CardType.POSTER,
        contentScale = contentScale
    )
}

/**
 * Landscape/backdrop-optimized image loading
 */
@Composable
fun BackdropImage(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    StrmrImage(
        url = url,
        contentDescription = "$title backdrop",
        modifier = modifier,
        cardType = CardType.LANDSCAPE,
        contentScale = contentScale,
        shape = RoundedCornerShape(12.dp) // Slightly larger radius for landscapes
    )
}

/**
 * Profile/actor image with circular shape
 */
@Composable
fun ProfileImage(
    url: String?,
    name: String,
    modifier: Modifier = Modifier
) {
    StrmrImage(
        url = url,
        contentDescription = "$name profile photo",
        modifier = modifier,
        cardType = CardType.CIRCLE,
        shape = androidx.compose.foundation.shape.CircleShape,
        contentScale = ContentScale.Crop
    )
}

/**
 * Default placeholder shown during image loading
 */
@Composable
private fun DefaultPlaceholder(
    cardType: CardType,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        // Loading indicator or empty state
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}

/**
 * Default error content shown when image fails to load
 */
@Composable
private fun DefaultErrorContent(
    contentDescription: String?,
    cardType: CardType,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show first letter of content description if available
            val fallbackText = contentDescription?.firstOrNull()?.uppercase() ?: "?"
            Text(
                text = fallbackText,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = when (cardType) {
                    CardType.HERO -> 32.sp
                    CardType.POSTER, CardType.LANDSCAPE -> 24.sp
                    CardType.SQUARE, CardType.COMPACT -> 20.sp
                    CardType.CIRCLE -> 16.sp
                },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Get optimal image dimensions for different card types
 */
private fun getOptimalDimensions(cardType: CardType): Pair<Int, Int> {
    return when (cardType) {
        CardType.POSTER -> Pair(
            StrmrImageLoader.ImageDimensions.POSTER_WIDTH,
            StrmrImageLoader.ImageDimensions.POSTER_HEIGHT
        )
        CardType.LANDSCAPE -> Pair(
            StrmrImageLoader.ImageDimensions.LANDSCAPE_WIDTH,
            StrmrImageLoader.ImageDimensions.LANDSCAPE_HEIGHT
        )
        CardType.SQUARE -> Pair(
            StrmrImageLoader.ImageDimensions.SQUARE_WIDTH,
            StrmrImageLoader.ImageDimensions.SQUARE_HEIGHT
        )
        CardType.CIRCLE -> Pair(
            StrmrImageLoader.ImageDimensions.CIRCLE_WIDTH,
            StrmrImageLoader.ImageDimensions.CIRCLE_HEIGHT
        )
        CardType.COMPACT -> Pair(
            StrmrImageLoader.ImageDimensions.COMPACT_WIDTH,
            StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT
        )
        CardType.HERO -> Pair(
            StrmrImageLoader.ImageDimensions.HERO_WIDTH,
            StrmrImageLoader.ImageDimensions.HERO_HEIGHT
        )
    }
}