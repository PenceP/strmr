package com.strmr.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.viewmodel.GenericMoviesViewModel
import com.strmr.ai.viewmodel.FlixclusiveTrendingViewModel
import com.strmr.ai.viewmodel.FlixclusivePopularViewModel
import com.strmr.ai.viewmodel.FlixclusiveTopMoviesViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import com.strmr.ai.data.database.MovieEntity
import androidx.compose.ui.focus.FocusRequester
import android.util.Log
import com.strmr.ai.ui.components.rememberSelectionManager
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import coil.compose.AsyncImage
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaCardSkeleton
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.data.OmdbResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.strmr.ai.ui.components.PaginationStateInfo

// Data class to hold row information
data class MovieRowData(
    val title: String,
    val movies: List<MovieEntity>,
    val paginationState: PaginationStateInfo,
    val showHero: Boolean
)

@Composable
fun MoviesPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((Int) -> Unit)?
) {
    val context = LocalContext.current
    val viewModel: GenericMoviesViewModel = hiltViewModel()
    
    // ✅ NEW: Flixclusive-style ViewModels for all rows
    val trendingViewModel: FlixclusiveTrendingViewModel = hiltViewModel()
    val popularViewModel: FlixclusivePopularViewModel = hiltViewModel()
    val topMoviesViewModel: FlixclusiveTopMoviesViewModel = hiltViewModel()
    
    // Load configuration
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("MOVIES")
    }
    
    val logoUrls by viewModel.logoUrls.collectAsState()
    
    // ✅ NEW: Flixclusive data for all rows
    val trendingMovies by trendingViewModel.movies.collectAsStateWithLifecycle()
    val trendingPaginationState by trendingViewModel.paginationState.collectAsStateWithLifecycle()
    
    val popularMovies by popularViewModel.movies.collectAsStateWithLifecycle()
    val popularPaginationState by popularViewModel.paginationState.collectAsStateWithLifecycle()
    
    val topMovies by topMoviesViewModel.movies.collectAsStateWithLifecycle()
    val topMoviesPaginationState by topMoviesViewModel.paginationState.collectAsStateWithLifecycle()
    
    // ✅ NEW: Multi-row management
    val rowData = remember(trendingMovies, popularMovies, topMovies) {
        listOf(
            MovieRowData("Trending", trendingMovies, trendingPaginationState, true),
            MovieRowData("Popular", popularMovies, popularPaginationState, true),
            MovieRowData("Top Movies of the Week", topMovies, topMoviesPaginationState, true)
        )
    }
    
    // ✅ NEW: Row and item selection state
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }
    
    // Update external focus when content focus changes
    LaunchedEffect(isContentFocused) {
        Log.d("MoviesPage", "🎯 External focus changed: isContentFocused=$isContentFocused")
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    // Get currently selected row and item
    val currentRow = rowData.getOrNull(selectedRowIndex)
    val selectedItem = currentRow?.movies?.getOrNull(selectedItemIndex)
    
    Log.d("MoviesPage", "🎬 Current selection: row=${currentRow?.title} (${selectedRowIndex}) item=${selectedItem?.title} (${selectedItemIndex})")
    
    // Update focused row in ViewModel when it changes
    LaunchedEffect(currentRow?.title) {
        currentRow?.title?.let { title ->
            viewModel.updateFocusedRow(title)
        }
    }

    // Check if current row should show hero based on configuration
    val shouldShowHero = currentRow?.showHero == true

    // Get backdrop URL for background
    val backdropUrl = if (shouldShowHero) {
        selectedItem?.backdropUrl.also {
            Log.d("MoviesPage", "🎨 Hero backdrop URL: $it for ${selectedItem?.title}")
        }
    } else null

    // OMDb ratings for hero section
    var omdbRatings by remember(selectedItem, selectedRowIndex, selectedItemIndex) { mutableStateOf<OmdbResponse?>(null) }
    LaunchedEffect(selectedItem, selectedRowIndex, selectedItemIndex) {
        if (shouldShowHero && selectedItem != null) {
            val imdbId = selectedItem.imdbId
            if (!imdbId.isNullOrBlank()) {
                omdbRatings = withContext(Dispatchers.IO) {
                    viewModel.fetchOmdbRatings(imdbId)
                }
            } else {
                omdbRatings = null
            }
        } else {
            omdbRatings = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop image as the main background
        if (shouldShowHero && !backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                    .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
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

        // ✅ NEW: Main content with vertical column for all rows
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 1.dp)
        ) {
            // Hero section (based on configuration)
            if (shouldShowHero && selectedItem != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.49f)
                        .padding(start = navBarWidth-2.dp)
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = selectedItem.title,
                                logoUrl = logoUrls[selectedItem.tmdbId] ?: selectedItem.logoUrl,
                                year = selectedItem.year,
                                formattedDate = selectedItem.releaseDate,
                                runtime = selectedItem.runtime,
                                genres = selectedItem.genres,
                                rating = selectedItem.rating,
                                overview = selectedItem.overview,
                                cast = selectedItem.cast.map { it.name ?: "" },
                                omdbRatings = omdbRatings,
                                onFetchLogo = {
                                    viewModel.fetchAndUpdateLogo(selectedItem)
                                }
                            )
                        }
                    )
                }
            }

            // ✅ NEW: All rows section with vertical scrolling
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (shouldShowHero) 0.51f else 1f)
            ) {
                val columnState = rememberLazyListState()
                
                LazyColumn(
                    state = columnState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        count = rowData.size,
                        key = { index -> rowData[index].title }
                    ) { rowIndex ->
                        val row = rowData[rowIndex]
                        val isRowSelected = selectedRowIndex == rowIndex
                        
                        UnifiedMediaRow(
                            config = MediaRowConfig(
                                title = row.title,
                                dataSource = DataSource.RegularList(
                                    items = row.movies,
                                    paginationState = row.paginationState
                                ),
                                onItemClick = { movie ->
                                    if (movie is MovieEntity) {
                                        onNavigateToDetails?.invoke(movie.tmdbId)
                                    }
                                },
                                onPaginate = { page ->
                                    Log.d("MoviesPage", "🚀 Pagination triggered for ${row.title} page $page")
                                    when (row.title) {
                                        "Trending" -> trendingViewModel.paginateMovies(page)
                                        "Popular" -> popularViewModel.paginateMovies(page)
                                        "Top Movies of the Week" -> topMoviesViewModel.loadMovies()
                                    }
                                },
                                cardType = CardType.PORTRAIT,
                                itemWidth = 120.dp,
                                itemSpacing = 12.dp,
                                itemContent = { movie, isFocused ->
                                    MediaCard(
                                        title = movie.title,
                                        posterUrl = movie.posterUrl,
                                        isSelected = isFocused,
                                        onClick = {
                                            if (movie is MovieEntity) {
                                                onNavigateToDetails?.invoke(movie.tmdbId)
                                            }
                                        }
                                    )
                                },
                                // ✅ NEW: Handle row and item selection
                                onSelectionChanged = { itemIndex ->
                                    selectedRowIndex = rowIndex
                                    selectedItemIndex = itemIndex
                                    Log.d("MoviesPage", "🎯 Selection changed: row=$rowIndex, item=$itemIndex")
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Navigation arrows for vertical row navigation
        if (selectedRowIndex > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Navigate up",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Down arrow (shown when there are more rows below)
        if (selectedRowIndex < rowData.size - 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Navigate down",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
} 