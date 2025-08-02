package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.R
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.config.RowConfig
import com.strmr.ai.data.NetworkInfo
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.presentation.state.ContinueWatchingState
import com.strmr.ai.presentation.state.TraktAuthState
import com.strmr.ai.presentation.state.TraktListsState
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.LandscapeMediaCard
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import com.strmr.ai.utils.ComposeOptimizationUtils
import com.strmr.ai.utils.DateFormatter
import com.strmr.ai.utils.OptimizedLaunchedEffect
import com.strmr.ai.viewmodel.HomeMediaItem
import com.strmr.ai.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class HeroData(
    val backdropUrl: String? = null,
    val title: String? = null,
    val logoUrl: String? = null,
    val year: Int? = null,
    val formattedDate: String? = null, // New field for formatted date
    val runtime: Int? = null,
    val genres: List<String>? = null,
    val rating: Float? = null,
    val overview: String? = null,
    val cast: List<String>? = null,
)

// Data class for collections (removed hardcoded collections - now loaded from JSON)

// Removed HomeMediaRow - now using UnifiedMediaRow directly in HomePage

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((String, Int, Int?, Int?) -> Unit)? = null,
    onNavigateToIntermediateView: ((String, String, String, String?, String?) -> Unit)? = null,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isNetworksLoading by viewModel.isNetworksLoading.collectAsState()

    // Extract data from clean architecture state
    val isContinueWatchingLoading = uiState.continueWatching is ContinueWatchingState.Loading

    // Get the converted continue watching items from ViewModel
    val continueWatching by viewModel.continueWatchingHomeItems.collectAsState()

    val traktLists =
        remember(uiState.traktLists) {
            (uiState.traktLists as? TraktListsState.Success)?.lists ?: emptyList()
        }
    val isTraktListsLoading = uiState.traktLists is TraktListsState.Loading
    val isTraktAuthorized = uiState.traktAuthorization is TraktAuthState.Authorized

    // Debug logging for Trakt Lists
    LaunchedEffect(traktLists.size, isTraktListsLoading) {
        Log.d("HomePage", "🔍 Trakt Lists state: size=${traktLists.size}, loading=$isTraktListsLoading")
    }

    // Initialize Trakt authorization and lists when HomePage loads
    LaunchedEffect(Unit) {
        Log.d("HomePage", "🚀 Initializing Trakt authorization and lists on HomePage load")
        viewModel.refreshTraktAuthorization()
        viewModel.refreshTraktLists()
    }

    // Simplified state management - no complex selection needed
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }

    // Load configuration from JSON
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    var collections by remember { mutableStateOf<List<HomeMediaItem.Collection>>(emptyList()) }
    var networkInfos by remember { mutableStateOf<List<com.strmr.ai.data.NetworkInfo>>(emptyList()) }
    var directors by remember { mutableStateOf<List<HomeMediaItem.Collection>>(emptyList()) }

    OptimizedLaunchedEffect(Unit, operation = "loadPageConfiguration") {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("HOME")
        pageConfiguration?.let { config ->
            collections = configLoader.getCollectionsFromConfig(config)
            networkInfos = configLoader.getNetworksFromConfig(config)
            directors = configLoader.getDirectorsFromConfig(config)
        }
    }

    // Simple focus handling - no complex selection manager needed

    // Create media rows dynamically based on configuration - optimized with expensive calculation helper
    val mediaRows =
        ComposeOptimizationUtils.rememberExpensiveCalculation(
            continueWatching,
            traktLists,
            networkInfos,
            collections,
            directors,
            pageConfiguration,
        ) {
            mutableMapOf<String, List<Any>>()
        }
    val rowConfigs =
        ComposeOptimizationUtils.rememberExpensiveCalculation(
            continueWatching,
            traktLists,
            networkInfos,
            collections,
            directors,
            pageConfiguration,
        ) {
            mutableMapOf<String, RowConfig>()
        }

    // Clear and rebuild rows when data changes
    mediaRows.clear()
    rowConfigs.clear()

    pageConfiguration?.let { config ->
        val enabledRows = ConfigurationLoader(context).getEnabledRowsSortedByOrder(config)

        for (rowConfig in enabledRows) {
            when (rowConfig.type) {
                "continue_watching" -> {
                    // Only show continue watching if Trakt is authorized
                    if (isTraktAuthorized && continueWatching.isNotEmpty()) {
                        mediaRows[rowConfig.title] = continueWatching
                        rowConfigs[rowConfig.title] = rowConfig
                    }

                    // Add Trakt Lists row if authorized and has items
                    if (isTraktAuthorized && traktLists.isNotEmpty()) {
                        // Create a synthetic row config for Trakt Lists
                        val traktListsConfig =
                            RowConfig(
                                id = "trakt_lists",
                                title = "Trakt Lists",
                                type = "nested_items",
                                cardType = "landscape",
                                cardHeight = 120,
                                showHero = false,
                                showLoading = false,
                                order = 2,
                                enabled = true,
                                displayOptions = rowConfig.displayOptions,
                                nestedRows = null,
                                nestedItems = null,
                            )
                        mediaRows["Trakt Lists"] = traktLists
                        rowConfigs["Trakt Lists"] = traktListsConfig
                        Log.d("HomePage", "✅ Added Trakt Lists row with ${traktLists.size} items")
                    } else {
                        Log.d(
                            "HomePage",
                            "🔒 Continue watching and Trakt Lists rows hidden - Trakt not authorized (authorized: $isTraktAuthorized)",
                        )
                    }
                }
                "networks" -> {
                    if (networkInfos.isNotEmpty()) {
                        mediaRows[rowConfig.title] = networkInfos
                        rowConfigs[rowConfig.title] = rowConfig
                    }
                }
                "collections" -> {
                    if (collections.isNotEmpty()) {
                        mediaRows[rowConfig.title] = collections
                        rowConfigs[rowConfig.title] = rowConfig
                    }
                }
                "directors" -> {
                    if (directors.isNotEmpty()) {
                        mediaRows[rowConfig.title] = directors
                        rowConfigs[rowConfig.title] = rowConfig
                    }
                }
            }
        }
    }

    // Simple row data calculation
    val rowTitles = mediaRows.keys.toList()
    val rows = mediaRows.values.toList()
    val validRowIndex = if (selectedRowIndex >= rows.size) 0 else selectedRowIndex

    // Optimized OMDb ratings prefetch - only for visible items and with batching
    val coroutineScope = rememberCoroutineScope()
    OptimizedLaunchedEffect(rows, operation = "prefetchOMDbRatings") {
        // Only prefetch for first row items to reduce startup overhead
        val firstRowItems = rows.firstOrNull() ?: emptyList()
        val imdbIds =
            firstRowItems.mapNotNull { item ->
                when (item) {
                    is com.strmr.ai.data.database.MovieEntity -> item.imdbId
                    is com.strmr.ai.data.database.TvShowEntity -> item.imdbId
                    is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> item.movie.imdbId
                    is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> item.show.imdbId
                    else -> null
                }
            }.filter { it.isNotBlank() }

        // Batch the requests to avoid creating too many coroutines
        if (imdbIds.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                imdbIds.forEach { imdbId ->
                    try {
                        viewModel.fetchOmdbRatings(imdbId)
                    } catch (e: Exception) {
                        Log.e("HomePage", "Failed to fetch OMDb ratings for $imdbId", e)
                    }
                }
            }
        }
    }

    // Simple initialization
    LaunchedEffect(Unit) {
        Log.d("HomePage", "🎯 HomePage composition started")
    }

    val selectedRow = rows.getOrNull(validRowIndex) ?: emptyList<Any>()
    val selectedItem = selectedRow.getOrNull(selectedItemIndex)

    val heroLogoRefreshTrigger = remember { mutableStateOf(0) }
    val heroData =
        when (selectedItem) {
            is HomeMediaItem.Movie ->
                with(selectedItem.movie) {
                    HeroData(
                        selectedItem.altBackdropUrl ?: backdropUrl, title, logoUrl, year,
                        DateFormatter.formatMovieDate(
                            releaseDate,
                        ),
                        runtime, genres, rating, overview,
                        cast.mapNotNull {
                            it.name
                        },
                    )
                }
            is HomeMediaItem.TvShow -> {
                val episodeOverview = selectedItem.episodeOverview
                val episodeAirDate = selectedItem.episodeAirDate
                with(selectedItem.show) {
                    HeroData(
                        backdropUrl,
                        title,
                        logoUrl,
                        year,
                        // For continue watching, show episode air date if available, otherwise show show date range
                        if (episodeAirDate != null) {
                            DateFormatter.formatEpisodeDate(
                                episodeAirDate,
                            )
                        } else {
                            DateFormatter.formatTvShowDateRange(firstAirDate, lastAirDate)
                        },
                        runtime,
                        genres,
                        rating,
                        episodeOverview ?: overview,
                        cast.mapNotNull { it.name },
                    )
                }
            }
            is com.strmr.ai.data.NetworkInfo -> {
                HeroData(
                    backdropUrl = selectedItem.posterUrl,
                    title = selectedItem.name,
                    logoUrl = selectedItem.posterUrl,
                )
            }
            else -> HeroData()
        }

    // If heroData is a movie and logo is missing, fetch logo in background
    LaunchedEffect(selectedItem, heroLogoRefreshTrigger.value) {
        val movie = (selectedItem as? HomeMediaItem.Movie)?.movie
        if (movie != null && movie.logoUrl.isNullOrBlank()) {
            val found = viewModel.fetchAndCacheMovieLogo(movie.tmdbId)
            if (found) {
                heroLogoRefreshTrigger.value++
            }
        }
        val show = (selectedItem as? HomeMediaItem.TvShow)?.show
        if (show != null && show.logoUrl.isNullOrBlank()) {
            val found = viewModel.fetchAndCacheTvShowLogo(show.tmdbId)
            if (found) {
                heroLogoRefreshTrigger.value++
            }
        }
    }

    // Debug logging
    LaunchedEffect(
        selectedRowIndex,
        selectedItemIndex,
        continueWatching.size,
        networkInfos.size,
        collections.size,
    ) {
        Log.d("HomePage", "🔄 Selection updated: rowIndex=$validRowIndex, itemIndex=$selectedItemIndex")
        Log.d("HomePage", "📊 Data: continueWatching=${continueWatching.size}, networks=${networkInfos.size}")
        Log.d("HomePage", "📦 Collections size: ${collections.size}")
        Log.d("HomePage", "⚙️ Configuration loaded: ${pageConfiguration != null}")
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    LaunchedEffect(rowTitles) {
        Log.d("HomePage", "Row titles: $rowTitles")
        Log.d("HomePage", "📋 Total rows: ${rows.size}")
        for ((index, row) in rows.withIndex()) {
            Log.d("HomePage", "Row $index (${rowTitles.getOrNull(index)}): ${row.size} items")
        }
    }

    // Focus management for when content is focused
    val focusRequester = remember { FocusRequester() }
    val lazyColumnState = rememberLazyListState()

    // Request focus when isContentFocused changes to true
    LaunchedEffect(isContentFocused) {
        if (isContentFocused) {
            Log.d("HomePage", "🎯 Content focused, requesting focus on first item")
            // Small delay to ensure UI is ready
            delay(100)
            focusRequester.requestFocus()
        }
    }

    // Simple focus tracking
    LaunchedEffect(selectedRowIndex, selectedItemIndex, isContentFocused) {
        if (isContentFocused) {
            Log.d("HomePage", "🎯 Focus on row $selectedRowIndex, item $selectedItemIndex")
        }
    }

    // Wrap with focus providers (Flixclusive pattern)
    WithFocusProviders("home") {
        // Unified layout: wallpaper is always the base background
        Box(modifier = modifier.fillMaxSize()) {
            // Wallpaper background (fills entire screen)
            Image(
                painter = painterResource(id = R.drawable.wallpaper),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
            )

            // Check if current row should show hero based on configuration
            val currentRowTitle = rowTitles.getOrNull(validRowIndex)
            val shouldShowHero =
                pageConfiguration?.let { config ->
                    currentRowTitle?.let { title ->
                        val rowConfig = config.rows.find { it.title == title }
                        rowConfig?.showHero == true
                    }
                } ?: false
            // Only use backdrop if showHero is true for current row
            val backdropUrl = if (shouldShowHero) heroData.backdropUrl else null

            // OMDb ratings state for hero
            var omdbRatings by ComposeOptimizationUtils.rememberExpensiveCalculation(selectedItem) {
                mutableStateOf<OmdbResponse?>(null)
            }
            LaunchedEffect(selectedItem) {
                if (shouldShowHero) {
                    val imdbId =
                        when (selectedItem) {
                            is HomeMediaItem.Movie -> selectedItem.movie.imdbId
                            is HomeMediaItem.TvShow -> selectedItem.show.imdbId
                            else -> null
                        }
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
            if (shouldShowHero && !backdropUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backdropUrl,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                    contentScale = ContentScale.Crop,
                    alpha = 1f,
                )
                // Dark overlay for readability
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                )
            }

            // Main content with Column layout like MoviesPage
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 1.dp),
            ) {
                // Hero section (based on configuration)
                if (shouldShowHero) {
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
                                    title = heroData.title,
                                    logoUrl = heroData.logoUrl,
                                    year = heroData.year,
                                    formattedDate = heroData.formattedDate,
                                    runtime = heroData.runtime,
                                    genres = heroData.genres,
                                    rating = heroData.rating,
                                    overview = heroData.overview,
                                    cast = heroData.cast,
                                    omdbRatings = omdbRatings, // <-- pass ratings
                                    extraContent = {
                                        if (selectedItem is HomeMediaItem.TvShow && selectedItem.season != null && selectedItem.episode != null) {
                                            Text(
                                                text = "S${selectedItem.season}: E${selectedItem.episode}",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 16.sp,
                                            )
                                        }
                                    },
                                )
                            },
                        )
                    }
                }

                // All rows section with fixed positioning (like MoviesPage)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(if (shouldShowHero) 0.51f else 1f),
                ) {
                    LazyColumn(
                        state = lazyColumnState,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            count = rowTitles.size,
                            key = { index -> rowTitles[index] }
                        ) { rowIndex ->
                            val rowTitle = rowTitles[rowIndex]
                            val rowItems = rows.getOrNull(rowIndex) ?: emptyList()
                            val rowConfig = rowConfigs[rowTitle] ?: pageConfiguration?.rows?.find { it.title == rowTitle }

                            // Check if this row should show loading state
                            val isLoading =
                                rowConfig?.showLoading == true &&
                                    when (rowConfig?.type) {
                                        "continue_watching" -> isContinueWatchingLoading
                                        "networks" -> isNetworksLoading
                                        "nested_items" -> if (rowTitle == "Trakt Lists") isTraktListsLoading else false
                                        else -> false
                                    }

                            if (isLoading) {
                            val skeletonCardType =
                                when (rowConfig?.cardType) {
                                    "landscape" -> SkeletonCardType.LANDSCAPE
                                    "portrait" -> SkeletonCardType.PORTRAIT
                                    else -> SkeletonCardType.PORTRAIT
                                }
                            MediaRowSkeleton(
                                title = rowTitle,
                                cardCount = 6,
                                cardType = skeletonCardType,
                            )
                        } else if (rowItems.isNotEmpty()) {
                            UnifiedMediaRow(
                                config =
                                    MediaRowConfig(
                                        title = rowTitle,
                                        dataSource = DataSource.RegularList(rowItems),
                                        cardType = if (rowConfig?.cardType == "landscape") CardType.LANDSCAPE else CardType.PORTRAIT,
                                        itemWidth = if (rowConfig?.cardType == "landscape") 200.dp else 120.dp,
                                        itemSpacing = 12.dp,
                                        contentPadding =
                                            if (rowIndex == 0) {
                                                // First row gets different padding to prevent jumping
                                                PaddingValues(horizontal = 56.dp, vertical = 0.dp)
                                            } else {
                                                PaddingValues(horizontal = 56.dp)
                                            },
                                        focusRequester = if (rowIndex == 0) focusRequester else null,
                                        onSelectionChanged = { itemIndex ->
                                            selectedRowIndex = rowIndex
                                            selectedItemIndex = itemIndex
                                            onContentFocusChanged?.invoke(true)
                                        },
                                        onLeftBoundary = onLeftBoundary,
                                        onItemClick =
                                            if (rowConfig?.displayOptions?.clickable == true) {
                                                { item ->
                                                    when (item) {
                                                        is HomeMediaItem.Movie ->
                                                            onNavigateToDetails?.invoke(
                                                                "movie",
                                                                item.movie.tmdbId,
                                                                null,
                                                                null,
                                                            )
                                                        is HomeMediaItem.TvShow ->
                                                            onNavigateToDetails?.invoke(
                                                                "tvshow",
                                                                item.show.tmdbId,
                                                                item.season,
                                                                item.episode,
                                                            )
                                                        is com.strmr.ai.data.NetworkInfo -> {
                                                            val displayName = if (item.name.isBlank()) item.id else item.name
                                                            val viewType = if (item.dataUrl?.contains("api.trakt.tv") == true) "trakt_list" else "network"
                                                            onNavigateToIntermediateView?.invoke(
                                                                viewType,
                                                                item.id,
                                                                displayName,
                                                                item.posterUrl,
                                                                item.dataUrl,
                                                            )
                                                        }
                                                        is HomeMediaItem.Collection -> {
                                                            val displayName = if (item.name.isBlank()) item.id else item.name
                                                            val viewType =
                                                                when (rowConfig?.type) {
                                                                    "collections" -> "collection"
                                                                    "directors" -> "director"
                                                                    else -> "collection"
                                                                }
                                                            onNavigateToIntermediateView?.invoke(
                                                                viewType,
                                                                item.id,
                                                                displayName,
                                                                item.backgroundImageUrl,
                                                                item.dataUrl,
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                null
                                            },
                                        itemContent = { mediaItem, isFocused ->
                                            when (mediaItem) {
                                                is HomeMediaItem.Movie -> {
                                                    if (rowConfig?.cardType == "landscape") {
                                                        LandscapeMediaCard(
                                                            title = mediaItem.movie.title,
                                                            landscapeUrl = mediaItem.movie.backdropUrl,
                                                            logoUrl = mediaItem.movie.logoUrl,
                                                            progress = mediaItem.progress ?: 0f,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                        )
                                                    } else {
                                                        MediaCard(
                                                            title = mediaItem.movie.title,
                                                            posterUrl = mediaItem.movie.posterUrl,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                        )
                                                    }
                                                }
                                                is HomeMediaItem.TvShow -> {
                                                    if (rowConfig?.cardType == "landscape") {
                                                        LandscapeMediaCard(
                                                            title = mediaItem.show.title,
                                                            landscapeUrl = mediaItem.episodeImageUrl ?: mediaItem.show.backdropUrl,
                                                            logoUrl = mediaItem.show.logoUrl,
                                                            progress = mediaItem.progress ?: 0f,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                            bottomRightLabel =
                                                                if (mediaItem.season != null && mediaItem.episode != null) {
                                                                    if (mediaItem.isNextEpisode) {
                                                                        "Next: S${mediaItem.season}: E${mediaItem.episode}"
                                                                    } else {
                                                                        "S${mediaItem.season}: E${mediaItem.episode}"
                                                                    }
                                                                } else {
                                                                    null
                                                                },
                                                        )
                                                    } else {
                                                        MediaCard(
                                                            title = mediaItem.show.title,
                                                            posterUrl = mediaItem.show.posterUrl,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                        )
                                                    }
                                                }
                                                is com.strmr.ai.data.NetworkInfo -> {
                                                    LandscapeMediaCard(
                                                        title = mediaItem.name,
                                                        landscapeUrl = mediaItem.posterUrl,
                                                        logoUrl = null,
                                                        isSelected = isFocused,
                                                        onClick = { },
                                                    )
                                                }
                                                is HomeMediaItem.Collection -> {
                                                    if (rowConfig?.cardType == "landscape") {
                                                        LandscapeMediaCard(
                                                            title = if (mediaItem.nameDisplayMode != "Hidden") mediaItem.name else "",
                                                            landscapeUrl = mediaItem.backgroundImageUrl,
                                                            logoUrl = null,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                        )
                                                    } else {
                                                        MediaCard(
                                                            title = if (mediaItem.nameDisplayMode != "Hidden") mediaItem.name else "",
                                                            posterUrl = mediaItem.backgroundImageUrl,
                                                            isSelected = isFocused,
                                                            onClick = { },
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    MediaCard(
                                                        title = "Unknown Item",
                                                        posterUrl = null,
                                                        isSelected = isFocused,
                                                        onClick = { },
                                                    )
                                                }
                                            }
                                        },
                                    ),
                                rowIndex = rowIndex,
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}
