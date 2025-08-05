package com.strmr.ai.ui.components.common.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PlaceholderCard displays a gray placeholder with title text during super speed scrolling
 * when media posters haven't loaded yet. This provides immediate visual feedback and prevents
 * blank spaces during rapid navigation.
 * 
 * Optimized for Android TV viewing distances and follows Material Design principles.
 */
@Composable
fun PlaceholderCard(
    title: String,
    modifier: Modifier = Modifier,
    width: Int = 150,
    height: Int = 225,
    cornerRadius: Int = 8,
    backgroundColor: Color = Color(0xFF424242), // Material Design gray-700
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(width = width.dp, height = height.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        // Title text centered in the placeholder
        Text(
            text = title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Specialized placeholder for movie posters (2:3 aspect ratio)
 */
@Composable
fun MoviePlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    PlaceholderCard(
        title = title,
        modifier = modifier,
        width = 150,
        height = 225 // 2:3 aspect ratio for movie posters
    )
}

/**
 * Specialized placeholder for TV show posters (2:3 aspect ratio)
 */
@Composable
fun TvShowPlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    PlaceholderCard(
        title = title,
        modifier = modifier,
        width = 150,
        height = 225 // 2:3 aspect ratio for TV show posters
    )
}

/**
 * Specialized placeholder for backdrop/landscape images (16:9 aspect ratio)
 */
@Composable
fun BackdropPlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    PlaceholderCard(
        title = title,
        modifier = modifier,
        width = 300,
        height = 169, // 16:9 aspect ratio for backdrops
        cornerRadius = 12
    )
}

/**
 * Compact placeholder for smaller row items
 */
@Composable
fun CompactPlaceholderCard(
    title: String,
    modifier: Modifier = Modifier
) {
    PlaceholderCard(
        title = title,
        modifier = modifier,
        width = 120,
        height = 180,
        cornerRadius = 6
    )
}