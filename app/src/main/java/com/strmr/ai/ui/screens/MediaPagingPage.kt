package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.strmr.ai.ui.components.PagingCenteredMediaRow
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.data.OmdbResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import com.strmr.ai.ui.components.MediaHeroSkeleton
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType

@Composable
fun <T : Any> MediaPagingPage(
    pagingUiState: PagingUiState<T>,
    selectedRowIndex: Int,
    selectedItemIndex: Int,
    onItemSelected: (Int, Int) -> Unit,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onUpDown: ((Int) -> Unit)? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    getOmdbRatings: suspend (String) -> OmdbResponse? = { null },
    onFetchLogo: ((T) -> Unit)? = null
) {
    val rowTitles = pagingUiState.mediaRows.keys.toList()
    val rowCount = rowTitles.size

    LaunchedEffect(selectedRowIndex, selectedItemIndex) {
        onItemSelected(selectedRowIndex, selectedItemIndex)
    }

    // Get the selected item for hero section
    val selectedRowFlow = pagingUiState.mediaRows.values.toList().getOrNull(selectedRowIndex)
    val selectedRowItems = selectedRowFlow?.collectAsLazyPagingItems()
    val selectedItem = selectedRowItems?.itemSnapshotList?.getOrNull(selectedItemIndex)

    // OMDb ratings for hero section
    val selectedImdbId = selectedItem?.let { item ->
        when (item) {
            is com.strmr.ai.data.database.MovieEntity -> item.imdbId
            is com.strmr.ai.data.database.TvShowEntity -> item.imdbId
            else -> null
        }
    }
    var omdbRatings by remember(selectedImdbId) { mutableStateOf<OmdbResponse?>(null) }
    
    LaunchedEffect(selectedImdbId) {
        if (!selectedImdbId.isNullOrBlank()) {
            try {
                omdbRatings = withContext(Dispatchers.IO) {
                    getOmdbRatings(selectedImdbId)
                }
            } catch (_: Exception) {
                omdbRatings = null
            }
        } else {
            omdbRatings = null
        }
    }

    // Navigation bar width for layout
    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    // Get backdrop URL for background
    val backdropUrl = selectedItem?.let { item ->
        when (item) {
            is com.strmr.ai.data.database.MovieEntity -> item.backdropUrl
            is com.strmr.ai.data.database.TvShowEntity -> item.backdropUrl
            else -> null
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Backdrop image as the main background
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                    .blur(radius = 8.dp),
                contentScale = ContentScale.Crop,
                alpha = 1f
            )
            
            // Gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            startX = 0f,
                            endX = 2200f
                        )
                    )
            )
        }

        // Wide, soft horizontal gradient overlay from left edge (behind nav bar) to main area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = -navBarWidthPx,
                        endX = 1200f
                    )
                )
        )

        // Main content (no vertical scroll - fixed layout)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = navBarWidth)
        ) {
            // Hero section (top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.49f)
            ) {
                if (selectedItem != null) {
                    MediaHero(
                        mediaDetails = {
                            selectedItem?.let { item ->
                                val details = item.getMediaDetails()
                                MediaDetails(
                                    title = details.title,
                                    logoUrl = details.logoUrl,
                                    year = details.year,
                                    formattedDate = details.releaseDate,
                                    runtime = details.runtime,
                                    genres = details.genres,
                                    rating = details.rating,
                                    overview = details.overview,
                                    cast = details.cast,
                                    omdbRatings = omdbRatings,
                                    onFetchLogo = {
                                        onFetchLogo?.invoke(item)
                                    }
                                )
                            }
                        }
                    )
                } else {
                    MediaHeroSkeleton()
                }
            }

            // Active row section with indicators - simplified without AnimatedContent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.51f)
            ) {
                // Render only the selected row (like HomePage approach)
                val rowTitle = rowTitles.getOrNull(selectedRowIndex) ?: ""
                val pagingFlow = pagingUiState.mediaRows[rowTitle]
                
                if (pagingFlow != null) {
                    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
                    
                    if (lazyPagingItems.itemCount > 0) {
                        PagingCenteredMediaRow(
                            title = rowTitle,
                            items = lazyPagingItems,
                            selectedIndex = selectedItemIndex,
                            onItemSelected = { itemIndex ->
                                onItemSelected(selectedRowIndex, itemIndex)
                            },
                            onSelectionChanged = { newIndex ->
                                onSelectionChanged(newIndex)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = focusRequester,
                            onUpDown = { direction ->
                                val newRowIndex = selectedRowIndex + direction
                                if (newRowIndex in 0 until rowCount) {
                                    Log.d("MediaPagingPage", "🎯 Row navigation: $selectedRowIndex -> $newRowIndex, maintaining content focus")
                                    onItemSelected(newRowIndex, 0)
                                    // Ensure content focus is maintained during row transitions
                                    onContentFocusChanged?.invoke(true)
                                }
                            },
                            isContentFocused = isContentFocused,
                            onContentFocusChanged = onContentFocusChanged,
                            onItemClick = onItemClick
                        )
                    } else {
                        // Show skeleton when no items loaded yet
                        MediaRowSkeleton(
                            title = rowTitle,
                            cardCount = 8,
                            cardType = SkeletonCardType.PORTRAIT,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Up/down indicators
                if (selectedRowIndex > 0) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(32.dp)
                    )
                }
                if (selectedRowIndex < rowCount - 1) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Down",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(32.dp)
                    )
                }
            }
        }
    }
} 