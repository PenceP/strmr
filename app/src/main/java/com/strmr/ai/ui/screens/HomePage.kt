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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.LandscapeMediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.viewmodel.HomeMediaItem
import com.strmr.ai.viewmodel.HomeViewModel
import androidx.compose.material3.Text
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.strmr.ai.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import coil.compose.rememberAsyncImagePainter
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.strmr.ai.ui.components.rememberSelectionManager
import androidx.compose.runtime.rememberCoroutineScope
import com.strmr.ai.utils.DateFormatter
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.config.RowConfig
import com.strmr.ai.data.NetworkInfo
import androidx.compose.ui.platform.LocalContext
import com.strmr.ai.ui.theme.StrmrConstants

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
    val cast: List<String>? = null
)

// Data class for collections (removed hardcoded collections - now loaded from JSON)

@Composable
fun HomeMediaRow(
    title: String,
    mediaItems: List<Any>,
    selectedIndex: Int,
    isRowSelected: Boolean = true,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onUpDown: ((Int) -> Unit)? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    showOverlays: Boolean = false,
    rowHeight: Dp = 120.dp,
    onItemClick: ((Any) -> Unit)? = null,
    cardType: String = "portrait"
) {
    // Use UnifiedMediaRow with EpisodeView-style left-aligned navigation
    UnifiedMediaRow(
        config = MediaRowConfig(
            title = title,
            dataSource = DataSource.RegularList(mediaItems),
            selectedIndex = selectedIndex,
            isRowSelected = isRowSelected,
            onSelectionChanged = onSelectionChanged,
            onUpDown = onUpDown,
            onLeftBoundary = onLeftBoundary,
            onItemClick = onItemClick,
            onContentFocusChanged = onContentFocusChanged,
            focusRequester = focusRequester,
            cardType = if (cardType == "landscape") CardType.LANDSCAPE else CardType.PORTRAIT,
            itemWidth = if (cardType == "landscape") 200.dp else 120.dp,
            itemSpacing = 12.dp, // Use EpisodeView spacing
            contentPadding = PaddingValues(horizontal = 48.dp), // Use EpisodeView padding
            itemContent = { mediaItem, isSelected ->
                when (mediaItem) {
                    is HomeMediaItem.Movie -> LandscapeMediaCard(
                        title = mediaItem.movie.title,
                        landscapeUrl = mediaItem.movie.backdropUrl,
                        logoUrl = mediaItem.movie.logoUrl,
                        progress = mediaItem.progress ?: 0f,
                        isSelected = isSelected,
                        onClick = {
                            Log.d("HomeMediaRow", "🎯 Movie item clicked")
                            onSelectionChanged(selectedIndex)
                        }
                    )
                    is HomeMediaItem.TvShow -> LandscapeMediaCard(
                        title = mediaItem.show.title,
                        landscapeUrl = mediaItem.episodeImageUrl ?: mediaItem.show.backdropUrl,
                        logoUrl = mediaItem.show.logoUrl,
                        progress = mediaItem.progress ?: 0f,
                        isSelected = isSelected,
                        onClick = {
                            Log.d("HomeMediaRow", "🎯 TvShow item clicked")
                            onSelectionChanged(selectedIndex)
                        },
                        bottomRightLabel = if (mediaItem.season != null && mediaItem.episode != null) {
                            if (mediaItem.isNextEpisode) "Next: S${mediaItem.season}: E${mediaItem.episode}" 
                            else "S${mediaItem.season}: E${mediaItem.episode}"
                        } else null
                    )
                    is com.strmr.ai.data.NetworkInfo -> LandscapeMediaCard(
                        title = mediaItem.name,
                        landscapeUrl = mediaItem.posterUrl,
                        logoUrl = null,
                        isSelected = isSelected,
                        onClick = {
                            Log.d("HomeMediaRow", "🎯 Network item clicked")
                            onSelectionChanged(selectedIndex)
                        }
                    )
                    is HomeMediaItem.Collection -> {
                        if (cardType == "landscape") {
                            LandscapeMediaCard(
                                title = if (mediaItem.nameDisplayMode != "Hidden") mediaItem.name else "",
                                landscapeUrl = mediaItem.backgroundImageUrl,
                                logoUrl = null,
                                isSelected = isSelected,
                                onClick = {
                                    Log.d("HomeMediaRow", "🎯 Collection item clicked")
                                    onSelectionChanged(selectedIndex)
                                }
                            )
                        } else {
                            // Poster card for collections
                            MediaCard(
                                title = if (mediaItem.nameDisplayMode != "Hidden") mediaItem.name else "",
                                posterUrl = mediaItem.backgroundImageUrl,
                                isSelected = isSelected,
                                onClick = {
                                    Log.d("HomeMediaRow", "🎯 Collection poster item clicked")
                                    onSelectionChanged(selectedIndex)
                                }
                            )
                        }
                    }
                    else -> {
                        // Fallback for unknown types to prevent crashes
                        Log.w("HomeMediaRow", "⚠️ Unknown media item type: ${mediaItem::class.simpleName}")
                        MediaCard(
                            title = "Unknown Item",
                            posterUrl = null,
                            isSelected = isSelected,
                            onClick = {
                                Log.d("HomeMediaRow", "🎯 Unknown item clicked")
                                onSelectionChanged(selectedIndex)
                            }
                        )
                    }
                }
            }
        ),
        modifier = modifier
    )
}


@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((String, Int, Int?, Int?) -> Unit)? = null,
    onNavigateToIntermediateView: ((String, String, String, String?, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isContinueWatchingLoading by viewModel.isContinueWatchingLoading.collectAsState()
    val isNetworksLoading by viewModel.isNetworksLoading.collectAsState()
    val traktLists by viewModel.traktLists.collectAsState()
    val isTraktListsLoading by viewModel.isTraktListsLoading.collectAsState()
    
    // Debug logging for Trakt Lists
    LaunchedEffect(traktLists.size, isTraktListsLoading) {
        Log.d("HomePage", "🔍 Trakt Lists state: size=${traktLists.size}, loading=$isTraktListsLoading")
    }
    
    // Initialize Trakt lists when HomePage loads
    LaunchedEffect(Unit) {
        Log.d("HomePage", "🚀 Initializing Trakt Lists on HomePage load")
        viewModel.refreshTraktLists()
    }
    
    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()
    
    // Row position memory - tracks last position in each row by row title
    val rowPositionMemory = remember { mutableMapOf<String, Int>() }
    
    // Load configuration from JSON
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    var collections by remember { mutableStateOf<List<HomeMediaItem.Collection>>(emptyList()) }
    var networks by remember { mutableStateOf<List<com.strmr.ai.data.NetworkInfo>>(emptyList()) }
    var directors by remember { mutableStateOf<List<HomeMediaItem.Collection>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("HOME")
        pageConfiguration?.let { config ->
            collections = configLoader.getCollectionsFromConfig(config)
            networks = configLoader.getNetworksFromConfig(config)
            directors = configLoader.getDirectorsFromConfig(config)
        }
    }
    
    // Update SelectionManager with focus state from MainActivity
    LaunchedEffect(isContentFocused) {
        Log.d("HomePage", "🎯 External focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }
    
    // Create media rows dynamically based on configuration
    // Rebuild rows when data changes
    val mediaRows = remember(continueWatching, traktLists, networks, collections, directors, pageConfiguration) { 
        mutableMapOf<String, List<Any>>() 
    }
    val rowConfigs = remember(continueWatching, traktLists, networks, collections, directors, pageConfiguration) { 
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
                    if (continueWatching.isNotEmpty()) {
                        mediaRows[rowConfig.title] = continueWatching
                        rowConfigs[rowConfig.title] = rowConfig
                    }
                    
                    // Add Trakt Lists row if authorized and has items
                    if (traktLists.isNotEmpty()) {
                        // Create a synthetic row config for Trakt Lists
                        val traktListsConfig = RowConfig(
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
                            nestedItems = null
                        )
                        mediaRows["Trakt Lists"] = traktLists
                        rowConfigs["Trakt Lists"] = traktListsConfig
                        Log.d("HomePage", "✅ Added Trakt Lists row with ${traktLists.size} items")
                    } else {
                        Log.d("HomePage", "🔒 Trakt Lists row hidden - no items or not authorized")
                    }
                }
                "networks" -> {
                    if (networks.isNotEmpty()) {
                        mediaRows[rowConfig.title] = networks
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

    val rowTitles = mediaRows.keys.toList()
    val rows = mediaRows.values.toList()
    val rowCount = rowTitles.size
    val focusRequesters = remember(rowTitles) { List(rowTitles.size) { FocusRequester() } }

    // Optimized OMDb ratings prefetch - only for visible items and with batching
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(rows) {
        // Only prefetch for first row items to reduce startup overhead
        val firstRowItems = rows.firstOrNull() ?: emptyList()
        val imdbIds = firstRowItems.mapNotNull { item ->
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
                        viewModel.getOmdbRatings(imdbId)
                    } catch (e: Exception) {
                        Log.e("HomePage", "Failed to fetch OMDb ratings for $imdbId", e)
                    }
                }
            }
        }
    }

    // Initialize focus state when HomePage loads and ensure first row is selected
    LaunchedEffect(Unit) {
        Log.d("HomePage", "🎯 HomePage composition started")
        // Ensure we start with the first row selected and content focused
        if (!selectionManager.isContentFocused) {
            Log.d("HomePage", "🎯 Initializing content focus on HomePage startup")
            selectionManager.updateContentFocus(true)
            onContentFocusChanged?.invoke(true)
        }
        // Ensure first row is selected
        if (selectionManager.selectedRowIndex != 0) {
            selectionManager.updateSelection(0, 0)
        }
        // Request focus on the first available row when composition completes
        Log.d("HomePage", "🎯 Requesting focus on first row")
        if (focusRequesters.isNotEmpty() && rows.isNotEmpty()) {
            try {
                focusRequesters[0].requestFocus()
                Log.d("HomePage", "🎯 Successfully requested initial focus")
            } catch (e: Exception) {
                Log.w("HomePage", "🚨 Failed to request initial focus: ${e.message}")
            }
        }
    }

    // Handle focus changes when selectedRowIndex changes
    LaunchedEffect(selectionManager.selectedRowIndex, focusRequesters.size) {
        val index = selectionManager.selectedRowIndex
        if (index >= 0 && index < focusRequesters.size && index < rows.size) {
            try {
                // Add delay to ensure composition is complete (like EpisodeView)
                kotlinx.coroutines.delay(100)
                focusRequesters[index].requestFocus()
                Log.d("HomePage", "🎯 Successfully requested focus on row $index")
            } catch (e: Exception) {
                Log.w("HomePage", "🚨 Failed to request focus on row $index: ${e.message}")
            }
        } else {
            Log.w("HomePage", "🚨 Invalid focus request: index=$index, focusRequesters.size=${focusRequesters.size}, rows.size=${rows.size}")
        }
    }

    val validRowIndex = if (selectionManager.selectedRowIndex >= rows.size) 0 else selectionManager.selectedRowIndex
    val selectedRow = rows.getOrNull(validRowIndex) ?: emptyList<Any>()
    val selectedItem = selectedRow.getOrNull(selectionManager.selectedItemIndex)

    val heroLogoRefreshTrigger = remember { mutableStateOf(0) }
    val heroData = when (selectedItem) {
        is HomeMediaItem.Movie -> with(selectedItem.movie) {
            HeroData(selectedItem.altBackdropUrl ?: backdropUrl, title, logoUrl, year, DateFormatter.formatMovieDate(releaseDate), runtime, genres, rating, overview, cast.mapNotNull { it.name })
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
                    if (episodeAirDate != null) DateFormatter.formatEpisodeDate(episodeAirDate) else DateFormatter.formatTvShowDateRange(firstAirDate, lastAirDate),
                    runtime,
                    genres,
                    rating,
                    episodeOverview ?: overview,
                    cast.mapNotNull { it.name }
                )
            }
        }
        is com.strmr.ai.data.NetworkInfo -> {
            HeroData(
                backdropUrl = selectedItem.posterUrl,
                title = selectedItem.name,
                logoUrl = selectedItem.posterUrl
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
    LaunchedEffect(selectionManager.selectedRowIndex, selectionManager.selectedItemIndex, continueWatching.size, networks.size, collections.size) {
        Log.d("HomePage", "🔄 Selection updated: rowIndex=$validRowIndex, itemIndex=${selectionManager.selectedItemIndex}")
        Log.d("HomePage", "📊 Data: continueWatching=${continueWatching.size}, networks=${networks.size}")
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

    // Unified layout: wallpaper is always the base background
    Box(modifier = modifier.fillMaxSize()) {
        // Wallpaper background (fills entire screen)
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 8.dp),
            contentScale = ContentScale.Crop
        )

        // Check if current row should show hero based on configuration
        val currentRowTitle = rowTitles.getOrNull(validRowIndex)
        val shouldShowHero = pageConfiguration?.let { config ->
            currentRowTitle?.let { title ->
                val rowConfig = config.rows.find { it.title == title }
                rowConfig?.showHero == true
            }
        } ?: false
        val backdropUrl = if (shouldShowHero) heroData.backdropUrl else null

        // OMDb ratings state for hero
        var omdbRatings by remember(selectedItem) { mutableStateOf<OmdbResponse?>(null) }
        LaunchedEffect(selectedItem) {
            if (shouldShowHero) {
                val imdbId = when (selectedItem) {
                    is HomeMediaItem.Movie -> selectedItem.movie.imdbId
                    is HomeMediaItem.TvShow -> selectedItem.show.imdbId
                    else -> null
                }
                if (!imdbId.isNullOrBlank()) {
                    omdbRatings = withContext(Dispatchers.IO) {
                        viewModel.getOmdbRatings(imdbId)
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
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 8.dp),
                contentScale = ContentScale.Crop,
                alpha = 1f
            )
            // Dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Hero section (based on configuration)
        if (shouldShowHero) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = navBarWidth, top = 0.dp, bottom = 16.dp)
                    .align(Alignment.TopStart)
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
                            }
                        )
                    }
                )
            }
        }

        // All rows section (always visible, overlaying wallpaper)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 39.dp)
        ) {
            Spacer(modifier = Modifier.height(if (shouldShowHero) 290.dp else 32.dp)) // Dynamic space for hero overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 0.dp, bottom = 32.dp)
            ) {
                for ((rowIndex, rowTitle) in rowTitles.withIndex()) {
                    val rowItems = rows.getOrNull(rowIndex) ?: emptyList()
                    
                    // Get row configuration for this row
                    val rowConfig = rowConfigs[rowTitle] ?: pageConfiguration?.rows?.find { it.title == rowTitle }
                    val rowHeight = rowConfig?.cardHeight?.dp ?: 140.dp
                    
                    // Check if this row should show loading state based on configuration
                    val isLoading = rowConfig?.showLoading == true && when (rowConfig?.type) {
                        "continue_watching" -> isContinueWatchingLoading
                        "networks" -> isNetworksLoading
                        "nested_items" -> if (rowTitle == "Trakt Lists") isTraktListsLoading else false
                        else -> false
                    }
                    
                    Log.d("HomePage", "🎬 Rendering row $rowIndex: '$rowTitle' with ${rowItems.size} items, loading: $isLoading")
                    
                    if (isLoading) {
                        // Show skeleton loading state based on configuration
                        val skeletonCardType = when (rowConfig?.cardType) {
                            "landscape" -> SkeletonCardType.LANDSCAPE
                            "portrait" -> SkeletonCardType.PORTRAIT
                            else -> SkeletonCardType.PORTRAIT
                        }
                        MediaRowSkeleton(
                            title = rowTitle,
                            cardCount = 6,
                            cardType = skeletonCardType,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        // Show actual content
                        HomeMediaRow(
                            modifier = Modifier.padding(bottom = 16.dp),
                            title = rowTitle,
                            mediaItems = rowItems,
                            selectedIndex = if (rowIndex == validRowIndex) selectionManager.selectedItemIndex else 0,
                            isRowSelected = rowIndex == validRowIndex,
                            onSelectionChanged = { newIndex ->
                                if (rowIndex == validRowIndex) {
                                    selectionManager.updateSelection(validRowIndex, newIndex)
                                    
                                    // Update position memory for current row when selection changes
                                    val currentRowTitle = rowTitles.getOrNull(validRowIndex)
                                    if (currentRowTitle != null) {
                                        rowPositionMemory[currentRowTitle] = newIndex
                                        Log.d("HomePage", "💾 Updated position $newIndex for row '$currentRowTitle'")
                                    }
                                }
                            },
                            focusRequester = if (rowIndex == validRowIndex) focusRequesters.getOrNull(rowIndex) else null,
                            onUpDown = { direction ->
                                val newRowIndex = validRowIndex + direction
                                if (newRowIndex >= 0 && newRowIndex < rows.size) {
                                    val currentRowTitle = rowTitles.getOrNull(validRowIndex)
                                    val newRowTitle = rowTitles.getOrNull(newRowIndex)
                                    
                                    Log.d("HomePage", "🎯 Row navigation: $validRowIndex($currentRowTitle) -> $newRowIndex($newRowTitle), direction=$direction")
                                    
                                    // Save current position in memory for the row we're leaving
                                    if (currentRowTitle != null) {
                                        val currentItemIndex = selectionManager.selectedItemIndex
                                        rowPositionMemory[currentRowTitle] = currentItemIndex
                                        Log.d("HomePage", "💾 Saved position $currentItemIndex for row '$currentRowTitle'")
                                    }
                                    
                                    // Get target position from memory or use default
                                    val newRowItems = rows.getOrNull(newRowIndex) ?: emptyList()
                                    val newItemIndex = if (newRowTitle != null && rowPositionMemory.containsKey(newRowTitle)) {
                                        // Use remembered position, but clamp to row bounds
                                        val rememberedPosition = rowPositionMemory[newRowTitle]!!
                                        val clampedPosition = if (newRowItems.isNotEmpty()) {
                                            minOf(rememberedPosition, newRowItems.size - 1)
                                        } else {
                                            0
                                        }
                                        Log.d("HomePage", "🧠 Recalling position $rememberedPosition -> $clampedPosition for row '$newRowTitle'")
                                        clampedPosition
                                    } else {
                                        // First time visiting this row, start at position 0
                                        Log.d("HomePage", "🆕 First visit to row '$newRowTitle', starting at position 0")
                                        0
                                    }
                                    
                                    Log.d("HomePage", "🎯 Updating selection: row $validRowIndex->$newRowIndex, item ${selectionManager.selectedItemIndex}->$newItemIndex (row size: ${newRowItems.size})")
                                    selectionManager.updateSelection(newRowIndex, newItemIndex)
                                    // Ensure content focus is maintained during row transitions
                                    selectionManager.updateContentFocus(true)
                                }
                            },
                            isContentFocused = selectionManager.isContentFocused,
                            onContentFocusChanged = { focused ->
                                selectionManager.updateContentFocus(focused)
                                onContentFocusChanged?.invoke(focused)
                            },
                            onLeftBoundary = if (rowIndex == validRowIndex) onLeftBoundary else null,
                            showOverlays = rowConfig?.displayOptions?.showOverlays == true,
                            rowHeight = rowHeight,
                            cardType = rowConfig?.cardType ?: "portrait",
                            onItemClick = if (rowConfig?.displayOptions?.clickable == true) { item ->
                                when (item) {
                                    is HomeMediaItem.Movie -> onNavigateToDetails?.invoke("movie", item.movie.tmdbId, null, null)
                                    is HomeMediaItem.TvShow -> {
                                        Log.d("HomePage", "🎯 DEBUG: Navigating to TvShow details - show: ${item.show.title}, season: ${item.season}, episode: ${item.episode}")
                                        onNavigateToDetails?.invoke("tvshow", item.show.tmdbId, item.season, item.episode)
                                    }
                                    is com.strmr.ai.data.NetworkInfo -> {
                                        // Use id as fallback if name is empty (common for networks with hidden names)
                                        val displayName = if (item.name.isBlank()) item.id else item.name
                                        Log.d("HomePage", "🎯 DEBUG: Navigating to Network intermediate view - network: $displayName (id: ${item.id})")
                                        // Check if this is a Trakt list (nested item) or a regular network
                                        val viewType = if (item.dataUrl?.contains("api.trakt.tv") == true) {
                                            "trakt_list"
                                        } else {
                                            "network"
                                        }
                                        onNavigateToIntermediateView?.invoke(viewType, item.id, displayName, item.posterUrl, item.dataUrl)
                                    }
                                    is HomeMediaItem.Collection -> {
                                        // Use id as fallback if name is empty (defensive programming)
                                        val displayName = if (item.name.isBlank()) item.id else item.name
                                        Log.d("HomePage", "🎯 DEBUG: Navigating to Collection intermediate view - collection: $displayName (id: ${item.id})")
                                        val viewType = when (rowConfig?.type) {
                                            "collections" -> "collection"
                                            "directors" -> "director"
                                            else -> "collection"
                                        }
                                        onNavigateToIntermediateView?.invoke(viewType, item.id, displayName, item.backgroundImageUrl, item.dataUrl)
                                    }
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Up arrow (drawn on top)
        if (validRowIndex > 0) {
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

        // Down arrow (drawn on bottom)
        if (validRowIndex < rowCount - 1) {
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