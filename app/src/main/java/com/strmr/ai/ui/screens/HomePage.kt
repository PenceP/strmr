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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.LandscapeMediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRow
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext

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
    showOverlays: Boolean = false,
    rowHeight: Dp = 120.dp,
    onItemClick: ((Any) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Centering logic for landscape cards
    val rowWidthDp = 900.dp
    val posterSpacingDp = 20.dp // Increased from 12.dp
    val posterWidthDp = 160.dp // Landscape width
    val selectedPosterWidthDp = 176.dp // Slightly larger when selected
    val posterWidthPx = with(density) { posterWidthDp.roundToPx() }
    val selectedPosterWidthPx = with(density) { selectedPosterWidthDp.roundToPx() }
    val rowWidthPx = with(density) { rowWidthDp.roundToPx() }
    val posterSpacingPx = with(density) { posterSpacingDp.roundToPx() }
    
    // Calculate how many posters fit in the row
    val postersPerRow = (rowWidthPx - posterSpacingPx) / (posterWidthPx + posterSpacingPx)
    val centerIndex = (postersPerRow / 2).toInt()

    fun getOffsetForIndex(index: Int, totalItems: Int): Int {
        return when {
            index == 0 -> 0 // First poster: left-aligned
            index <= centerIndex -> {
                // Gradual centering: move toward center as we scroll right
                val progress = index.toFloat() / centerIndex.toFloat()
                val centerOffset = -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
                (centerOffset * progress).toInt()
            }
            else -> {
                // Locked to center for all other positions
                -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
            }
        }
    }

    // Request focus when this row becomes selected and is composed
    if (isRowSelected && focusRequester != null && isContentFocused) {
        LaunchedEffect(isRowSelected, isContentFocused) {
            focusRequester.requestFocus()
        }
    }

    // Initialize row position when focus is first given
    LaunchedEffect(isRowSelected, isContentFocused, selectedIndex) {
        if (isRowSelected && isContentFocused && mediaItems.isNotEmpty()) {
            Log.d("HomeMediaRow", "🎯 Initializing row position: selectedIndex=$selectedIndex, items=${mediaItems.size}")
            // Ensure the selected item is properly positioned
            val offset = getOffsetForIndex(selectedIndex, mediaItems.size)
            listState.scrollToItem(selectedIndex, offset)
        }
    }

    // Synchronize scroll position with selection changes
    LaunchedEffect(selectedIndex) {
        if (isRowSelected && mediaItems.isNotEmpty()) {
            Log.d("HomeMediaRow", "🔄 Syncing scroll position for selectedIndex=$selectedIndex")
            val offset = getOffsetForIndex(selectedIndex, mediaItems.size)
            listState.scrollToItem(selectedIndex, offset)
        }
    }

    Column(
        modifier = modifier
            .padding(start = 8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            state = listState,
            modifier = Modifier
                .width(900.dp)
                .height(rowHeight)
                .onFocusChanged { 
                    Log.d("HomeMediaRow", "🎯 Focus changed for '$title': ${it.isFocused}")
                    onContentFocusChanged?.invoke(it.isFocused) 
                }
                .focusRequester(focusRequester ?: FocusRequester())
                .focusable(enabled = true)
                .onKeyEvent { event ->
                    Log.d("HomeMediaRow", "🎯 Key event for '$title': keyCode=${event.nativeKeyEvent.keyCode}, isRowSelected=$isRowSelected, focusRequester=${focusRequester != null}")
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && isRowSelected && focusRequester != null) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (selectedIndex > 0) {
                                    onSelectionChanged(selectedIndex - 1)
                                    true
                                } else {
                                    false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (selectedIndex < mediaItems.size - 1) {
                                    onSelectionChanged(selectedIndex + 1)
                                    true
                                } else {
                                    false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                onUpDown?.invoke(-1)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onUpDown?.invoke(1)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER, 
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                // Handle click without race condition
                                val mediaItem = mediaItems.getOrNull(selectedIndex)
                                if (mediaItem != null) {
                                    Log.d("HomeMediaRow", "🎯 Enter pressed on item $selectedIndex")
                                    onItemClick?.invoke(mediaItem)
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items(mediaItems.size) { i ->
                val mediaItem = mediaItems[i]
                val isSelected = i == selectedIndex && isRowSelected
                
                when (mediaItem) {
                    is HomeMediaItem.Movie -> LandscapeMediaCard(
                        title = mediaItem.movie.title,
                        landscapeUrl = mediaItem.movie.backdropUrl,
                        logoUrl = mediaItem.movie.logoUrl,
                        progress = mediaItem.progress,
                        isSelected = isSelected,
                        onClick = {
                            // Only update selection, don't trigger navigation here
                            Log.d("HomeMediaRow", "🎯 Movie item clicked: $i")
                            onSelectionChanged(i)
                        }
                    )
                    is HomeMediaItem.TvShow -> LandscapeMediaCard(
                        title = mediaItem.show.title,
                        landscapeUrl = mediaItem.episodeImageUrl ?: mediaItem.show.backdropUrl,
                        logoUrl = mediaItem.show.logoUrl,
                        progress = mediaItem.progress,
                        isSelected = isSelected,
                        onClick = {
                            // Only update selection, don't trigger navigation here
                            Log.d("HomeMediaRow", "🎯 TvShow item clicked: $i")
                            onSelectionChanged(i)
                        },
                        bottomRightLabel = if (mediaItem.season != null && mediaItem.episode != null) "S${mediaItem.season}: E${mediaItem.episode}" else null
                    )
                    is com.strmr.ai.data.NetworkInfo -> LandscapeMediaCard(
                        title = mediaItem.name,
                        landscapeUrl = mediaItem.posterUrl,
                        logoUrl = null,
                        isSelected = isSelected,
                        onClick = {
                            // Only update selection, don't trigger navigation here
                            Log.d("HomeMediaRow", "🎯 Network item clicked: $i")
                            onSelectionChanged(i)
                        }
                    )
                    is HomeMediaItem.Collection -> MediaCard(
                        title = if (mediaItem.nameDisplayMode != "Hidden") mediaItem.name else "",
                        posterUrl = mediaItem.backgroundImageUrl,
                        isSelected = isSelected,
                        onClick = {
                            // Only update selection, don't trigger navigation here
                            Log.d("HomeMediaRow", "🎯 Collection item clicked: $i")
                            onSelectionChanged(i)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onNavigateToDetails: ((String, Int, Int?, Int?) -> Unit)? = null
) {
    val context = LocalContext.current
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isContinueWatchingLoading by viewModel.isContinueWatchingLoading.collectAsState()
    val isNetworksLoading by viewModel.isNetworksLoading.collectAsState()
    
    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()
    
    // Load configuration from JSON
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    var collections by remember { mutableStateOf<List<HomeMediaItem.Collection>>(emptyList()) }
    var networks by remember { mutableStateOf<List<com.strmr.ai.data.NetworkInfo>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("HOME")
        pageConfiguration?.let { config ->
            collections = configLoader.getCollectionsFromConfig(config)
            networks = configLoader.getNetworksFromConfig(config)
        }
    }
    
    // Update SelectionManager with focus state from MainActivity
    LaunchedEffect(isContentFocused) {
        Log.d("HomePage", "🎯 External focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }
    
    // Create media rows dynamically based on configuration
    val mediaRows = mutableMapOf<String, List<Any>>()
    pageConfiguration?.let { config ->
        val enabledRows = ConfigurationLoader(context).getEnabledRowsSortedByOrder(config)
        
        for (rowConfig in enabledRows) {
            when (rowConfig.type) {
                "continue_watching" -> {
                    if (continueWatching.isNotEmpty()) {
                        mediaRows[rowConfig.title] = continueWatching
                    }
                }
                "networks" -> {
                    if (networks.isNotEmpty()) {
                        mediaRows[rowConfig.title] = networks
                    }
                }
                "collections" -> {
                    if (collections.isNotEmpty()) {
                        mediaRows[rowConfig.title] = collections
                    }
                }
            }
        }
    }

    val rowTitles = mediaRows.keys.toList()
    val rows = mediaRows.values.toList()
    val rowCount = rowTitles.size
    val focusRequesters = remember(rowTitles) { List(rowTitles.size) { FocusRequester() } }

    // Prefetch OMDb ratings for all visible items
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
                    viewModel.getOmdbRatings(imdbId)
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
        focusRequesters.getOrNull(0)?.requestFocus()
    }

    // Handle focus changes when selectedRowIndex changes
    LaunchedEffect(selectionManager.selectedRowIndex) {
        focusRequesters.getOrNull(selectionManager.selectedRowIndex)?.requestFocus()
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
            modifier = Modifier.fillMaxSize(),
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
                modifier = Modifier.fillMaxSize(),
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
                .padding(start = navBarWidth)
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
                    val rowConfig = pageConfiguration?.rows?.find { it.title == rowTitle }
                    val rowHeight = rowConfig?.cardHeight?.dp ?: 140.dp
                    
                    // Check if this row should show loading state based on configuration
                    val isLoading = rowConfig?.showLoading == true && when (rowConfig.type) {
                        "continue_watching" -> isContinueWatchingLoading
                        "networks" -> isNetworksLoading
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
                                }
                            },
                            focusRequester = if (rowIndex == validRowIndex) focusRequesters.getOrNull(rowIndex) else null,
                            onUpDown = { direction ->
                                val newRowIndex = validRowIndex + direction
                                if (newRowIndex >= 0 && newRowIndex < rows.size) {
                                    Log.d("HomePage", "🎯 Row navigation: $validRowIndex -> $newRowIndex, maintaining focus")
                                    selectionManager.updateSelection(newRowIndex, 0)
                                    // Ensure content focus is maintained during row transitions
                                    selectionManager.updateContentFocus(true)
                                }
                            },
                            isContentFocused = selectionManager.isContentFocused,
                            onContentFocusChanged = { focused ->
                                selectionManager.updateContentFocus(focused)
                                onContentFocusChanged?.invoke(focused)
                            },
                            showOverlays = rowConfig?.displayOptions?.showOverlays == true,
                            rowHeight = rowHeight,
                            onItemClick = if (rowConfig?.displayOptions?.clickable == true) { item ->
                                when (item) {
                                    is HomeMediaItem.Movie -> onNavigateToDetails?.invoke("movie", item.movie.tmdbId, null, null)
                                    is HomeMediaItem.TvShow -> {
                                        Log.d("HomePage", "🎯 DEBUG: Navigating to TvShow details - show: ${item.show.title}, season: ${item.season}, episode: ${item.episode}")
                                        onNavigateToDetails?.invoke("tvshow", item.show.tmdbId, item.season, item.episode)
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