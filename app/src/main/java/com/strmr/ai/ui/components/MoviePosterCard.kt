package com.strmr.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.MovieRowItem

@Composable
fun MoviePosterCard(
    movie: MovieRowItem,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f) // 2:3 aspect ratio for movie posters
            .clip(RoundedCornerShape(8.dp))
            .background(StrmrConstants.Colors.SURFACE_DARK)
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .graphicsLayer {
                // Scale effect when focused
                val scale = if (isSelected && isFocused) 1.05f else 1f
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isSelected && isFocused) {
                    Modifier.border(
                        width = 3.dp,
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!movie.posterUrl.isNullOrBlank()) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Placeholder for missing poster
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "No poster available",
                tint = StrmrConstants.Colors.TEXT_SECONDARY,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}