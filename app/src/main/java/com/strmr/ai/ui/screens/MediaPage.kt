package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.data.OmdbResponse
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
import com.strmr.ai.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun <T> MediaPage(
    uiState: UiState<T>,
    modifier: Modifier = Modifier,
    getOmdbRatings: suspend (String) -> OmdbResponse? = { null },
    onItemClick: ((T) -> Unit)? = null,
) where T : Any {
    val rowTitles = uiState.mediaRows.keys.toList()
    val rows = uiState.mediaRows.values.toList()
    
    // Simple state for selected item (for hero display)
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }
    
    val selectedRow = rows.getOrNull(selectedRowIndex) ?: emptyList<T>()
    val selectedItem = selectedRow.getOrNull(selectedItemIndex)

    // Prefetch OMDb ratings for all items
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(rows) {
        rows.flatten().forEach { item ->
            val imdbId = when (item) {
                is com.strmr.ai.data.database.MovieEntity -> item.imdbId
                is com.strmr.ai.data.database.TvShowEntity -> item.imdbId
                is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> item.movie.imdbId
                is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> item.show.imdbId
                else -> null
            }
            if (!imdbId.isNullOrBlank()) {
                coroutineScope.launch {
                    getOmdbRatings(imdbId)
                }
            }
        }
    }

    // Add navBarWidth and navBarWidthPx for gradient alignment
    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    Box(
        modifier = modifier.fillMaxSize(),
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
                    Text(
                        text = uiState.errorMessage ?: "Unknown error",
                        color = Color.Red,
                        fontSize = 16.sp,
                    )
                }
            }
            !uiState.isLoading && !uiState.isError && (rows.isEmpty() || rows.all { (it as? List<*>)?.isEmpty() != false }) -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No items found",
                        color = Color.White,
                        fontSize = 16.sp,
                    )
                }
            }
            else -> {
                // Backdrop image as the main background
                val backdropUrl = selectedItem?.getBackdropUrl()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart,
                ) {
                    // Minimal test: only backdrop and gradient overlay
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
                                        startX = 0f, // Should start at very left edge
                                        endX = 2200f,
                                    ),
                                ),
                    )
                }

                // Wide, soft horizontal gradient overlay from left edge (behind nav bar) to main area (HOME style)
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
                                    startX = -navBarWidthPx, // Start at very left edge, behind nav bar
                                    endX = 1200f,
                                ),
                            ),
                )
                // (Optional) Scrim overlay if needed for readability
                // Box(
                //     modifier = Modifier
                //         .fillMaxSize()
                //         .background(Color.Black.copy(alpha = 0.5f))
                // )
                // Simplified layout with hero and LazyColumn
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = navBarWidth)
                ) {
                    // Hero section (fixed at top)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        val selectedImdbId = when (selectedItem) {
                            is com.strmr.ai.data.database.MovieEntity -> selectedItem.imdbId
                            is com.strmr.ai.data.database.TvShowEntity -> selectedItem.imdbId
                            is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> selectedItem.movie.imdbId
                            is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> selectedItem.show.imdbId
                            else -> null
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
                                    )
                                }
                            },
                        )
                    }
                    
                    // Rows section with LazyColumn
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowTitles.forEachIndexed { rowIndex, rowTitle ->
                            val rowItems = rows.getOrNull(rowIndex) as? List<T> ?: emptyList()
                            if (rowItems.isNotEmpty()) {
                                item(key = rowTitle) {
                                    UnifiedMediaRow(
                                        config = MediaRowConfig(
                                            title = rowTitle,
                                            dataSource = DataSource.RegularList(rowItems),
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
                                        )
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

// Extension functions for type-safe property access
fun Any.getBackdropUrl(): String? =
    when (this) {
        is com.strmr.ai.data.database.MovieEntity -> this.backdropUrl
        is com.strmr.ai.data.database.TvShowEntity -> this.backdropUrl
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> this.movie.backdropUrl
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> this.show.backdropUrl
        is com.strmr.ai.data.NetworkInfo -> this.posterUrl
        else -> null
    }

fun Any.getMediaDetails(): MediaDetailsData =
    when (this) {
        is com.strmr.ai.data.database.MovieEntity ->
            MediaDetailsData(
                title = this.title,
                logoUrl = this.logoUrl,
                year = this.year,
                runtime = this.runtime,
                genres = this.genres,
                rating = this.rating,
                overview = this.overview,
                cast = this.cast.mapNotNull { it.name },
                releaseDate = this.releaseDate?.let { DateFormatter.formatMovieDate(it) },
            )
        is com.strmr.ai.data.database.TvShowEntity ->
            MediaDetailsData(
                title = this.title,
                logoUrl = this.logoUrl,
                year = this.year,
                runtime = this.runtime,
                genres = this.genres,
                rating = this.rating,
                overview = this.overview,
                cast = this.cast.mapNotNull { it.name },
                releaseDate = DateFormatter.formatTvShowDateRange(this.firstAirDate, this.lastAirDate),
            )
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie ->
            MediaDetailsData(
                title = this.movie.title,
                logoUrl = this.movie.logoUrl,
                year = this.movie.year,
                runtime = this.movie.runtime,
                genres = this.movie.genres,
                rating = this.movie.rating,
                overview = this.movie.overview,
                cast = this.movie.cast.mapNotNull { it.name },
                releaseDate = this.movie.releaseDate?.let { DateFormatter.formatMovieDate(it) },
            )
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow ->
            MediaDetailsData(
                title = this.show.title,
                logoUrl = this.show.logoUrl,
                year = this.show.year,
                runtime = this.show.runtime,
                genres = this.show.genres,
                rating = this.show.rating,
                overview = this.show.overview,
                cast = this.show.cast.mapNotNull { it.name },
                releaseDate = DateFormatter.formatTvShowDateRange(this.show.firstAirDate, this.show.lastAirDate),
            )
        is com.strmr.ai.data.NetworkInfo ->
            MediaDetailsData(
                title = this.name,
                logoUrl = this.posterUrl,
                year = null,
                runtime = null,
                genres = null,
                rating = null,
                overview = null,
                cast = null,
                releaseDate = null,
            )
        else -> MediaDetailsData()
    }

// Data class for media details
data class MediaDetailsData(
    val title: String? = null,
    val logoUrl: String? = null,
    val year: Int? = null,
    val runtime: Int? = null,
    val genres: List<String>? = null,
    val rating: Float? = null,
    val overview: String? = null,
    val cast: List<String>? = null,
    val omdbRatings: OmdbResponse? = null,
    val releaseDate: String? = null,
)
