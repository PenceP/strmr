package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.MoviePosterCard
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import com.strmr.ai.ui.utils.focusOnMount
import com.strmr.ai.ui.utils.useLocalCurrentRoute
import com.strmr.ai.ui.utils.useLocalLastFocusedItemPerDestination
import com.strmr.ai.viewmodel.MoviesViewModel
import kotlinx.coroutines.delay

@Composable
fun MoviesPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((Int) -> Unit)?,
) {
    WithFocusProviders(route = "movies") {
        val viewModel: MoviesViewModel = hiltViewModel()

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val trendingMovies by viewModel.trendingMovies.collectAsStateWithLifecycle()
        val popularMovies by viewModel.popularMovies.collectAsStateWithLifecycle()

        // Simple state management
        var focusedRowIndex by remember { mutableStateOf(0) }
        var isPageFocused by remember { mutableStateOf(false) }

        // Netflix-style: Remember selected index for each row
        var trendingSelectedIndex by remember { mutableStateOf(0) }
        var popularSelectedIndex by remember { mutableStateOf(0) }

        val columnState = rememberLazyListState()

        // Get focus state for restoration checks
        val lastFocusedItemPerDestination = useLocalLastFocusedItemPerDestination()

        // Handle external focus
        LaunchedEffect(isContentFocused) {
            if (isContentFocused && !isPageFocused) {
                Log.d(
                    "MoviesPage",
                    "🎯 Content focus received from external source, activating page focus"
                )
                isPageFocused = true
                onContentFocusChanged?.invoke(true)

                // Small delay to ensure UI is ready, then try to restore saved focus
                delay(100)

                // Check if we have saved focus to restore
                val currentRoute = "movies"
                val hasSavedFocus = lastFocusedItemPerDestination[currentRoute] != null

                if (hasSavedFocus) {
                    Log.d(
                        "MoviesPage",
                        "🔄 Has saved focus for restoration: ${lastFocusedItemPerDestination[currentRoute]}"
                    )
                } else {
                    Log.d("MoviesPage", "🆕 No saved focus, will use default focus behavior")
                }
            }
        }

        // Scroll to focused row
        LaunchedEffect(focusedRowIndex) {
            columnState.animateScrollToItem(focusedRowIndex)
        }

        // Get current selected movie for background
        val currentSelectedMovie = when (focusedRowIndex) {
            0 -> trendingMovies.movies.getOrNull(trendingSelectedIndex)
            1 -> popularMovies.movies.getOrNull(popularSelectedIndex)
            else -> null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StrmrConstants.Colors.BACKGROUND_DARK)
        ) {
            // Background
            currentSelectedMovie?.backdropUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Loading
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StrmrConstants.Colors.TEXT_PRIMARY)
                }
                return@Box
            }

            // Content
            LazyColumn(
                state = columnState,
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(vertical = 40.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Trending Movies
                item {
                    SimpleMovieRow(
                        title = "Trending Movies",
                        movies = trendingMovies.movies,
                        isLoading = trendingMovies.isLoading,
                        hasMore = trendingMovies.hasMore,
                        isFocused = isPageFocused && focusedRowIndex == 0,
                        selectedIndex = trendingSelectedIndex,
                        onSelectionChanged = { trendingSelectedIndex = it },
                        onMovieClick = { movie ->
                            onNavigateToDetails?.invoke(movie.id)
                        },
                        onLoadMore = { viewModel.loadTrendingMovies() },
                        onNavigateUp = {
                            // Already at top
                        },
                        onNavigateDown = {
                            if (popularMovies.movies.isNotEmpty()) focusedRowIndex = 1
                        },
                        onNavigateLeft = { canGoLeft ->
                            if (!canGoLeft) {
                                isPageFocused = false
                                onContentFocusChanged?.invoke(false)
                                onLeftBoundary?.invoke()
                            }
                        },
                        onBack = {
                            isPageFocused = false
                            onContentFocusChanged?.invoke(false)
                            onLeftBoundary?.invoke()
                        },
                        rowKey = "trending_row"
                    )
                }

                // Popular Movies
                item {
                    SimpleMovieRow(
                        title = "Popular Movies",
                        movies = popularMovies.movies,
                        isLoading = popularMovies.isLoading,
                        hasMore = popularMovies.hasMore,
                        isFocused = isPageFocused && focusedRowIndex == 1,
                        selectedIndex = popularSelectedIndex,
                        onSelectionChanged = { popularSelectedIndex = it },
                        onMovieClick = { movie ->
                            onNavigateToDetails?.invoke(movie.id)
                        },
                        onLoadMore = { viewModel.loadPopularMovies() },
                        onNavigateUp = {
                            if (trendingMovies.movies.isNotEmpty()) focusedRowIndex = 0
                        },
                        onNavigateDown = { /* Already at bottom */ },
                        onNavigateLeft = { canGoLeft ->
                            if (!canGoLeft) {
                                isPageFocused = false
                                onContentFocusChanged?.invoke(false)
                                onLeftBoundary?.invoke()
                            }
                        },
                        onBack = {
                            isPageFocused = false
                            onContentFocusChanged?.invoke(false)
                            onLeftBoundary?.invoke()
                        },
                        rowKey = "popular_row"
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleMovieRow(
    title: String,
    movies: List<com.strmr.ai.viewmodel.MovieRowItem>,
    isLoading: Boolean,
    hasMore: Boolean,
    isFocused: Boolean,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    onMovieClick: (com.strmr.ai.viewmodel.MovieRowItem) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onNavigateLeft: (Boolean) -> Unit,
    onBack: () -> Unit,
    rowKey: String
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var isInternallyFocused by remember { mutableStateOf(false) }
    val lastFocusedItemPerDestination = useLocalLastFocusedItemPerDestination()
    val currentRouteFromHook = useLocalCurrentRoute()

    val posterWidth = 115.dp
    val posterSpacing = 12.dp
    val focusPosition = StrmrConstants.Dimensions.Icons.EXTRA_LARGE

    // Request focus when row becomes focused
    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(100) // Increased delay to ensure selection is restored first
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures
            }
        }
    }

    // Additional focus request when selection changes on focused row (for restoration)
    LaunchedEffect(selectedIndex, isFocused) {
        if (isFocused && movies.isNotEmpty()) {
            delay(50)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus request failures
            }
        }
    }

    // Scroll to keep selected poster in static focus position
    LaunchedEffect(selectedIndex, isFocused) {
        if (isFocused && movies.isNotEmpty() && selectedIndex in 0 until movies.size) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    // Load more when near end
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= movies.size - 5 && hasMore && !isLoading) {
            onLoadMore()
        }
    }

    // Also handle initial positioning when row becomes focused with pre-set selectedIndex
    LaunchedEffect(isFocused, movies.size) {
        if (isFocused && movies.isNotEmpty() && selectedIndex > 0) {
            // Small delay to ensure the row is ready, then position the selected poster
            delay(150) // Increased delay
            listState.scrollToItem(selectedIndex)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) StrmrConstants.Colors.TEXT_PRIMARY
            else StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = 0.7f),
            fontSize = 24.sp,
            modifier = Modifier.padding(start = focusPosition, bottom = 16.dp)
        )

        // Row with static focus overlay
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(posterSpacing),
                contentPadding = PaddingValues(start = focusPosition, end = focusPosition),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { isInternallyFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            when (keyEvent.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                    onNavigateUp()
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    onNavigateDown()
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (selectedIndex > 0) {
                                        onSelectionChanged(selectedIndex - 1)
                                        onNavigateLeft(true)
                                    } else {
                                        onNavigateLeft(false)
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (selectedIndex < movies.size - 1) {
                                        onSelectionChanged(selectedIndex + 1)
                                    } else if (hasMore) {
                                        onLoadMore()
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                    movies.getOrNull(selectedIndex)?.let { movie ->
                                        // Save the focus key manually before navigation (same as onClick)
                                        val focusKey = "${rowKey}_movie_${movie.id}"
                                        val currentRoute = "movies"
                                        Log.d(
                                            "MoviesPage",
                                            " Route debugging - useLocalCurrentRoute(): $currentRouteFromHook, hardcoded: $currentRoute"
                                        )
                                        lastFocusedItemPerDestination[currentRoute] = focusKey
                                        Log.d(
                                            "MoviesPage",
                                            "🎬 Movie selected via DPAD_CENTER: ${movie.title} (ID: ${movie.id})"
                                        )
                                        Log.d(
                                            "MoviesPage",
                                            "🔑 Manually saved focus key via DPAD: $focusKey for route: $currentRoute"
                                        )
                                        Log.d(
                                            "MoviesPage",
                                            "🗂️ Current focus map contents: $lastFocusedItemPerDestination"
                                        )
                                        onMovieClick(movie)
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_BACK -> {
                                    onBack()
                                    true
                                }

                                else -> false
                            }
                        } else false
                    }
            ) {
                itemsIndexed(movies) { index, movie ->
                    val alpha = when {
                        !isFocused -> 0.6f
                        index == selectedIndex -> 1f
                        else -> 0.6f
                    }

                    val posterFocusRequester = remember { FocusRequester() }
                    var isPosterFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .graphicsLayer { this.alpha = alpha }
                            .focusRequester(posterFocusRequester)
                            .focusOnMount(
                                itemKey = "${rowKey}_movie_${movie.id}",
                                onFocus = {
                                    onSelectionChanged(index)
                                    Log.d(
                                        "MoviesPage",
                                        "🎯 Focus gained via focusOnMount for movie: ${movie.title} (ID: ${movie.id}), key: ${rowKey}_movie_${movie.id}"
                                    )
                                    // Remove the focus request here to prevent infinite loop
                                    // The FocusHelper will handle focus restoration
                                }
                            )
                            .focusable()
                            .onFocusChanged { focusState ->
                                isPosterFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    onSelectionChanged(index)
                                    Log.d(
                                        "MoviesPage",
                                        "🎬 Movie poster focused: ${movie.title} (index: $index), key: ${rowKey}_movie_${movie.id}"
                                    )
                                }
                            }
                    ) {
                        MoviePosterCard(
                            movie = movie,
                            isSelected = index == selectedIndex,
                            isFocused = isPosterFocused, // Use individual poster focus state
                            onClick = {
                                onSelectionChanged(index)
                                // Save the focus key manually before navigation
                                val focusKey = "${rowKey}_movie_${movie.id}"
                                val currentRoute = "movies" // Force to movies for debugging
                                Log.d(
                                    "MoviesPage",
                                    "🔍 Route debugging - useLocalCurrentRoute(): $currentRouteFromHook, hardcoded: $currentRoute"
                                )
                                lastFocusedItemPerDestination[currentRoute] = focusKey
                                Log.d(
                                    "MoviesPage",
                                    "🎬 Movie clicked: ${movie.title} (ID: ${movie.id})"
                                )
                                Log.d(
                                    "MoviesPage",
                                    "🔑 Manually saved focus key: $focusKey for route: $currentRoute"
                                )
                                Log.d(
                                    "MoviesPage",
                                    "🗂️ Current focus map contents: $lastFocusedItemPerDestination"
                                )
                                onMovieClick(movie)
                            }
                        )
                    }
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(172.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = StrmrConstants.Colors.PRIMARY_BLUE,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }
                }

                // Spacers for smooth scrolling
                repeat(3) {
                    item {
                        Spacer(modifier = Modifier.width(posterWidth + posterSpacing))
                    }
                }
            }

            // Static focus border overlay is no longer needed since we use individual poster focus
        }
    }
}
