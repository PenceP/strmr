package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.MediaType
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.PaginationStateInfo
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.FlixclusiveGenericViewModel
import com.strmr.ai.viewmodel.GenericTvShowsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data class to hold row information
data class TvShowRowData(
    val title: String,
    val tvShows: List<TvShowEntity>,
    val paginationState: PaginationStateInfo,
    val showHero: Boolean,
)

@Composable
fun TvShowsPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((Int) -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: GenericTvShowsViewModel = hiltViewModel()

    // Load configuration
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("TV")
    }

    val logoUrls by viewModel.logoUrls.collectAsState()

    // ✅ NEW: Use single generic ViewModel for all rows
    val tvShowsViewModel: FlixclusiveGenericViewModel = hiltViewModel()

    // Data source configurations
    val dataSourceConfigs =
        remember {
            listOf(
                DataSourceConfig(
                    id = "trending",
                    title = "Trending",
                    endpoint = "shows/trending",
                    mediaType = MediaType.TV_SHOW,
                    cacheKey = "trending_tv_shows",
                ),
                DataSourceConfig(
                    id = "popular",
                    title = "Popular",
                    endpoint = "shows/popular",
                    mediaType = MediaType.TV_SHOW,
                    cacheKey = "popular_tv_shows",
                ),
            )
        }

    // Initialize all data sources and wait for them to be populated
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("TvShowsPage", "🚀 Starting initialization of ${dataSourceConfigs.size} data sources")
        dataSourceConfigs.forEach { config ->
            Log.d("TvShowsPage", "🔄 Initializing ${config.id}...")
            tvShowsViewModel.initializeDataSource(config)
        }
        // Small delay to ensure async flows are populated
        kotlinx.coroutines.delay(100)
        isInitialized = true
        Log.d("TvShowsPage", "✅ All data sources initialized")
    }

    // Collect data from generic ViewModel for each row
    val trendingTvShows by tvShowsViewModel.getTvShowsFlow("trending").collectAsStateWithLifecycle()
    val trendingPaginationState by tvShowsViewModel.getPaginationFlow("trending").collectAsStateWithLifecycle()

    val popularTvShows by tvShowsViewModel.getTvShowsFlow("popular").collectAsStateWithLifecycle()
    val popularPaginationState by tvShowsViewModel.getPaginationFlow("popular").collectAsStateWithLifecycle()

    // ✅ NEW: Multi-row management with all rows
    val rowData =
        remember(trendingTvShows, popularTvShows) {
            val rows =
                listOf(
                    TvShowRowData("Trending", trendingTvShows, trendingPaginationState, true),
                    TvShowRowData("Popular", popularTvShows, popularPaginationState, true),
                )
            Log.d("TvShowsPage", "📺 Created ${rows.size} rows: ${rows.map { "${it.title}(${it.tvShows.size} TV shows)" }}")
            rows
        }

    // ✅ NEW: Row and item selection state
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }

    // Update external focus when content focus changes
    LaunchedEffect(isContentFocused) {
        Log.d("TvShowsPage", "🎯 External focus changed: isContentFocused=$isContentFocused")
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    // Get currently selected row and item
    val currentRow = rowData.getOrNull(selectedRowIndex)
    val selectedItem = currentRow?.tvShows?.getOrNull(selectedItemIndex)

    Log.d(
        "TvShowsPage",
        "📺 Current selection: row=${currentRow?.title} ($selectedRowIndex) item=${selectedItem?.title} ($selectedItemIndex)",
    )

    // Update focused row in ViewModel when it changes
    LaunchedEffect(currentRow?.title) {
        currentRow?.title?.let { title ->
            viewModel.updateFocusedRow(title)
        }
    }

    // Check if current row should show hero based on configuration
    val shouldShowHero = currentRow?.showHero == true

    // Get backdrop URL for background
    val backdropUrl =
        if (shouldShowHero) {
            selectedItem?.backdropUrl.also {
                Log.d("TvShowsPage", "🎨 Hero backdrop URL: $it for ${selectedItem?.title}")
            }
        } else {
            null
        }

    // OMDb ratings for hero section
    var omdbRatings by remember(selectedItem, selectedRowIndex, selectedItemIndex) { mutableStateOf<OmdbResponse?>(null) }
    LaunchedEffect(selectedItem, selectedRowIndex, selectedItemIndex) {
        if (shouldShowHero && selectedItem != null) {
            val imdbId = selectedItem.imdbId
            if (!imdbId.isNullOrBlank()) {
                omdbRatings =
                    withContext(Dispatchers.IO) {
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
        modifier = Modifier.fillMaxSize(),
    ) {
        // Only render content after initialization is complete
        if (!isInitialized) {
            // Show loading state while initializing
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
            return@Box
        }
        // Backdrop image as the main background
        if (shouldShowHero && !backdropUrl.isNullOrBlank()) {
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

        // ✅ NEW: Main content with vertical column for all rows
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 1.dp),
        ) {
            // Hero section (based on configuration)
            if (shouldShowHero && selectedItem != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(0.49f)
                            .padding(start = navBarWidth - 2.dp),
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = selectedItem.title,
                                logoUrl = logoUrls[selectedItem.tmdbId] ?: selectedItem.logoUrl,
                                year = selectedItem.year,
                                formattedDate = selectedItem.firstAirDate,
                                runtime = null, // TV shows don't have runtime
                                genres = selectedItem.genres,
                                rating = selectedItem.rating,
                                overview = selectedItem.overview,
                                cast = selectedItem.cast.map { it.name ?: "" },
                                omdbRatings = omdbRatings,
                                onFetchLogo = {
                                    viewModel.fetchAndUpdateLogo(selectedItem)
                                },
                            )
                        },
                    )
                }
            }

            // ✅ NEW: All rows section with vertical scrolling
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(if (shouldShowHero) 0.51f else 1f),
            ) {
                val columnState = rememberLazyListState()

                LazyColumn(
                    state = columnState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    flingBehavior = com.strmr.ai.ui.components.rememberThrottledFlingBehavior(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        count = rowData.size,
                        key = { index -> rowData[index].title },
                    ) { rowIndex ->
                        val row = rowData[rowIndex]
                        val isRowSelected = selectedRowIndex == rowIndex

                        UnifiedMediaRow(
                            config =
                                MediaRowConfig(
                                    title = row.title,
                                    dataSource =
                                        DataSource.RegularList(
                                            items = row.tvShows,
                                            paginationState = row.paginationState,
                                        ),
                                    onItemClick = { tvShow ->
                                        if (tvShow is TvShowEntity) {
                                            onNavigateToDetails?.invoke(tvShow.tmdbId)
                                        }
                                    },
                                    onPaginate = { page ->
                                        Log.d("TvShowsPage", "🚀 Pagination triggered for ${row.title} page $page")
                                        val config = dataSourceConfigs.find { it.title == row.title }
                                        if (config != null) {
                                            when (config.id) {
                                                "trending", "popular" -> tvShowsViewModel.paginateTvShows(config, page)
                                                else -> tvShowsViewModel.loadTvShows(config)
                                            }
                                        }
                                    },
                                    cardType = CardType.PORTRAIT,
                                    itemWidth = 120.dp,
                                    itemSpacing = 12.dp,
                                    itemContent = { tvShow, isFocused ->
                                        MediaCard(
                                            title = tvShow.title,
                                            posterUrl = tvShow.posterUrl,
                                            isSelected = isFocused,
                                            onClick = {
                                                if (tvShow is TvShowEntity) {
                                                    onNavigateToDetails?.invoke(tvShow.tmdbId)
                                                }
                                            },
                                        )
                                    },
                                    // ✅ NEW: Handle row and item selection
                                    onSelectionChanged = { itemIndex ->
                                        selectedRowIndex = rowIndex
                                        selectedItemIndex = itemIndex
                                        Log.d("TvShowsPage", "🎯 Selection changed: row=$rowIndex, item=$itemIndex")
                                    },
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Navigation arrows for vertical row navigation
        if (selectedRowIndex > 0) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Navigate up",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // Down arrow (shown when there are more rows below)
        if (selectedRowIndex < rowData.size - 1) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Navigate down",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
