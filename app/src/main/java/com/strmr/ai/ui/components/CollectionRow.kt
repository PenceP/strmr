package com.strmr.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.data.CollectionMovie
import com.strmr.ai.ui.components.CenteredMediaRow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border

@Composable
fun CollectionRow(
    collectionMovies: List<CollectionMovie>,
    onItemClick: (CollectionMovie) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null
) {
    if (collectionMovies.isEmpty()) return
    
    val title = "Part of Collection"
    
    CenteredMediaRow(
        title = title,
        mediaItems = collectionMovies.take(10),
        selectedIndex = selectedIndex,
        isRowSelected = isRowSelected,
        onSelectionChanged = onSelectionChanged,
        onUpDown = onUpDown,
        onItemClick = { movie -> onItemClick(movie as CollectionMovie) },
        modifier = modifier,
        itemWidth = 120.dp,
        itemSpacing = 16.dp,
        rowHeight = 200.dp,
        focusRequester = focusRequester,
        isContentFocused = isContentFocused,
        onContentFocusChanged = onContentFocusChanged,
        itemContent = { movie, isSelected ->
            CollectionMovieCard(
                movie = movie as CollectionMovie,
                onClick = { onItemClick(movie) },
                isSelected = isSelected
            )
        }
    )
}

@Composable
fun CollectionMovieCard(
    movie: CollectionMovie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val baseWidth = 120.dp
    val baseHeight = 180.dp
    val targetWidth = if (isSelected) baseWidth * 1.2f else baseWidth
    val targetHeight = if (isSelected) baseHeight * 1.1f else baseHeight
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = tween(durationMillis = 200))
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = tween(durationMillis = 200))

    Column(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.Transparent else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Poster image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (!movie.poster_path.isNullOrBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${movie.poster_path}",
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = movie.title.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 24.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        // Year and rating
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            movie.release_date?.let { date ->
                val year = date.take(4) // Extract year from YYYY-MM-DD
                Text(
                    text = year,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
            Text(
                text = String.format("%.1f", movie.vote_average),
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
} 