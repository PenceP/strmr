package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.getPosterUrl
import com.strmr.ai.ui.components.getTitle
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import com.strmr.ai.viewmodel.IntermediateViewViewModel

@Composable
fun IntermediateViewPage(
    viewType: String, // "network", "collection", or "director"
    itemId: String,
    itemName: String,
    itemBackgroundUrl: String?,
    dataUrl: String?,
    onNavigateToDetails: (String, Int) -> Unit, // mediaType, tmdbId
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: IntermediateViewViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    // State management
    var selectedIndex by remember { mutableStateOf(0) }

    // Load data when screen loads
    LaunchedEffect(viewType, itemId, dataUrl) {
        viewModel.loadContent(viewType, itemId, itemName, itemBackgroundUrl, dataUrl)
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    WithFocusProviders("intermediate_view_${viewType}_$itemId") {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Back) {
                            onNavigateBack()
                            true
                        } else {
                            false
                        }
                    },
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading...",
                                color = Color.White,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
                uiState.isError -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "Unknown error",
                                color = Color.Red,
                                fontSize = 16.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Press BACK to return",
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
                uiState.mediaItems.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "No items found",
                                color = Color.White,
                                fontSize = 16.sp,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Press BACK to return",
                                color = Color.White,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
                else -> {
                    // Background setup similar to MediaPage
                    val selectedItem = uiState.mediaItems.getOrNull(selectedIndex)
                    val backdropUrl =
                        selectedItem?.let { item ->
                            when (item) {
                                is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> item.movie.backdropUrl
                                is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> item.show.backdropUrl
                                else -> null
                            }
                        } ?: itemBackgroundUrl

                    // Backdrop image
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

                    // Gradient overlays
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

                    // Content layout
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(start = 1.dp)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        // Hero section (top 49%)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(0.49f)
                                    .padding(start = navBarWidth - 2.dp),
                        ) {
                            MediaHero(
                                mediaDetails = {
                                    // Create hero details for the collection/network/director
                                    val heroDetails =
                                        when {
                                            selectedItem != null -> {
                                                // Show selected item details
                                                selectedItem.getMediaDetails()
                                            }
                                            else -> {
                                                // Show collection/network/director info
                                                MediaDetailsData(
                                                    title = itemName,
                                                    logoUrl = itemBackgroundUrl,
                                                    overview =
                                                        when (viewType) {
                                                            "network" -> "Content from $itemName"
                                                            "collection" -> "$itemName collection"
                                                            "director" -> "Films by $itemName"
                                                            else -> null
                                                        },
                                                )
                                            }
                                        }

                                    MediaDetails(
                                        title = heroDetails.title,
                                        logoUrl = heroDetails.logoUrl,
                                        year = heroDetails.year,
                                        formattedDate = heroDetails.releaseDate,
                                        runtime = heroDetails.runtime,
                                        genres = heroDetails.genres,
                                        rating = heroDetails.rating,
                                        overview = heroDetails.overview,
                                        cast = heroDetails.cast,
                                        omdbRatings = heroDetails.omdbRatings,
                                    )
                                },
                            )
                        }

                        // Media row section (bottom 51%)
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(0.51f)
                                    .padding(start = 2.dp),
                        ) {
                            UnifiedMediaRow(
                                config =
                                    MediaRowConfig(
                                        title = itemName,
                                        dataSource = DataSource.RegularList(uiState.mediaItems),
                                        selectedIndex = selectedIndex,
                                        isRowSelected = true,
                                        onSelectionChanged = { newIndex ->
                                            selectedIndex = newIndex
                                        },
                                        onLoadMore = {
                                            viewModel.loadMore()
                                        },
                                        cardType = CardType.PORTRAIT,
                                        itemWidth = 120.dp,
                                        itemSpacing = 12.dp,
                                        // contentPadding = PaddingValues(horizontal = 48.dp),
                                        onItemClick = { item ->
                                            val mediaType =
                                                when (item) {
                                                    is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> "movie"
                                                    is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> "tvshow"
                                                    else -> null
                                                }
                                            val tmdbId =
                                                when (item) {
                                                    is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> item.movie.tmdbId
                                                    is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> item.show.tmdbId
                                                    else -> null
                                                }
                                            if (mediaType != null && tmdbId != null) {
                                                android.util.Log.d(
                                                    "IntermediateViewPage",
                                                    "🎯 Navigating to details: mediaType=$mediaType, tmdbId=$tmdbId",
                                                )
                                                onNavigateToDetails(mediaType, tmdbId)
                                            }
                                        },
                                        itemContent = { item, isSelected ->
                                            MediaCard(
                                                title = item.getTitle(),
                                                posterUrl = item.getPosterUrl(),
                                                isSelected = isSelected,
                                                onClick = { /* UnifiedMediaRow handles clicks via onItemClick */ },
                                            )
                                        },
                                    ),
                                rowIndex = 0,
                            )
                        }
                    }
                }
            }
        }
    }
}
