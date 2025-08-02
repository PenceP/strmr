package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaHeroSkeleton
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.getPosterUrl
import com.strmr.ai.ui.components.getTitle
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun <T : Any> MediaPagingPage(
    pagingUiState: PagingUiState<T>,
    modifier: Modifier = Modifier,
    onItemClick: ((T) -> Unit)? = null,
    getOmdbRatings: suspend (String) -> OmdbResponse? = { null },
    onFetchLogo: ((T) -> Unit)? = null,
) {
    val rowTitles = pagingUiState.mediaRows.keys.toList()

    // Simple state for selected item (for hero display)
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }

    // Get the selected item for hero section
    val selectedRowFlow = pagingUiState.mediaRows.values.toList().getOrNull(selectedRowIndex)
    val selectedRowItems = selectedRowFlow?.collectAsLazyPagingItems()
    val selectedItem = selectedRowItems?.itemSnapshotList?.getOrNull(selectedItemIndex)

    // OMDb ratings for hero section
    val selectedImdbId =
        selectedItem?.let { item ->
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
                omdbRatings =
                    withContext(Dispatchers.IO) {
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
    val backdropUrl =
        selectedItem?.let { item ->
            when (item) {
                is com.strmr.ai.data.database.MovieEntity -> item.backdropUrl
                is com.strmr.ai.data.database.TvShowEntity -> item.backdropUrl
                else -> null
            }
        }

    WithFocusProviders("media_paging") {
        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // Backdrop image as the main background
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.1f
                                scaleY = 1.1f
                            }
                            .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                    contentScale = ContentScale.Crop,
                    alpha = 1f,
                )

                // Gradient overlay for readability
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Black.copy(alpha = 0.7f),
                                            Color.Black.copy(alpha = 0.3f),
                                        ),
                                    startX = 0f,
                                    endX = 2200f,
                                ),
                            ),
                )
            }

            // Wide, soft horizontal gradient overlay from left edge (behind nav bar) to main area
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        Color.Black,
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                    ),
                                startX = -navBarWidthPx,
                                endX = 1200f,
                            ),
                        ),
            )

            // Simplified layout with hero and LazyColumn
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = navBarWidth),
            ) {
                // Hero section (fixed height at top)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(300.dp),
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
                                        },
                                    )
                                }
                            },
                        )
                    } else {
                        MediaHeroSkeleton()
                    }
                }

                // Rows section with LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowTitles.forEachIndexed { rowIndex, rowTitle ->
                        val pagingFlow = pagingUiState.mediaRows[rowTitle]

                        if (pagingFlow != null) {
                            item(key = rowTitle) {
                                val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

                                if (lazyPagingItems.itemCount > 0) {
                                    UnifiedMediaRow(
                                        config =
                                            MediaRowConfig(
                                                title = rowTitle,
                                                dataSource = DataSource.PagingList(lazyPagingItems),
                                                cardType = CardType.PORTRAIT,
                                                itemWidth = 120.dp,
                                                itemSpacing = 12.dp,
                                                contentPadding = PaddingValues(horizontal = 48.dp),
                                                onItemClick = onItemClick,
                                                itemContent = { item, isSelected ->
                                                    MediaCard(
                                                        title = item.getTitle(),
                                                        posterUrl = item.getPosterUrl(),
                                                        isSelected = isSelected,
                                                        onClick = { onItemClick?.invoke(item) },
                                                    )
                                                },
                                            ),
                                        rowIndex = rowIndex,
                                    )
                                } else {
                                    MediaRowSkeleton(
                                        title = rowTitle,
                                        cardCount = 8,
                                        cardType = SkeletonCardType.PORTRAIT,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
