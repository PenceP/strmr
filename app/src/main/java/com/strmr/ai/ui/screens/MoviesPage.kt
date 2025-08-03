package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.MovieRow
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.MoviesViewModel

@Composable
fun MoviesPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((Int) -> Unit)?,
) {
    val viewModel: MoviesViewModel = hiltViewModel()
    
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trendingMovies by viewModel.trendingMovies.collectAsStateWithLifecycle()
    val popularMovies by viewModel.popularMovies.collectAsStateWithLifecycle()

    // Focus and selection management
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndices by remember { mutableStateOf(mapOf(0 to 0, 1 to 0)) }
    var focusLevel by remember { mutableStateOf(1) } // 1 = nav bar, 2 = content

    val columnState = rememberLazyListState()

    // Navigation bar width
    val navBarWidth = StrmrConstants.Dimensions.Components.NAV_BAR_WIDTH

    // Handle external focus changes
    LaunchedEffect(isContentFocused) {
        if (isContentFocused && focusLevel == 1) {
            Log.d("MoviesPage", "🎯 Content focused from nav bar, setting focus level to 2")
            focusLevel = 2
        }
    }

    // Notify external focus changes
    LaunchedEffect(focusLevel) {
        Log.d("MoviesPage", "🎯 Focus level changed to: $focusLevel")
        when (focusLevel) {
            1 -> onContentFocusChanged?.invoke(false) // Nav bar focused
            2 -> onContentFocusChanged?.invoke(true)  // Content focused
        }
    }

    // Auto-scroll to selected row
    LaunchedEffect(selectedRowIndex) {
        if (selectedRowIndex >= 0) {
            columnState.animateScrollToItem(selectedRowIndex)
        }
    }

    // Get current selections
    val currentSelectedItemIndex = selectedItemIndices[selectedRowIndex] ?: 0
    val currentSelectedMovie = when (selectedRowIndex) {
        0 -> trendingMovies.movies.getOrNull(currentSelectedItemIndex)
        1 -> popularMovies.movies.getOrNull(currentSelectedItemIndex)
        else -> null
    }

    // Background image from selected movie
    val backdropUrl = currentSelectedMovie?.backdropUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StrmrConstants.Colors.BACKGROUND_DARK)
    ) {
        // Background image
        backdropUrl?.let { url ->
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

        // Dark gradient overlay
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

        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = StrmrConstants.Colors.TEXT_PRIMARY
                )
            }
            return@Box
        }

        // Main content
        LazyColumn(
            state = columnState,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(vertical = 40.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Trending Movies Row
            item {
                MovieRow(
                    title = "Trending Movies",
                    movieRowState = trendingMovies,
                    isRowFocused = focusLevel == 2 && selectedRowIndex == 0,
                    selectedItemIndex = selectedItemIndices[0] ?: 0,
                    onSelectionChanged = { itemIndex ->
                        selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                            put(0, itemIndex)
                        }
                        selectedRowIndex = 0
                    },
                    onItemClick = { movie ->
                        Log.d("MoviesPage", "🎬 Clicked movie: ${movie.title}")
                        onNavigateToDetails?.invoke(movie.id)
                    },
                    onLoadMore = {
                        viewModel.loadTrendingMovies()
                    },
                    onKeyEvent = { keyEvent ->
                        handleKeyEvent(
                            keyEvent = keyEvent,
                            currentRowIndex = selectedRowIndex,
                            currentItemIndex = selectedItemIndices[selectedRowIndex] ?: 0,
                            maxItemIndex = trendingMovies.movies.size - 1,
                            onNavigateUp = {
                                // No row above trending
                            },
                            onNavigateDown = {
                                if (popularMovies.movies.isNotEmpty()) {
                                    selectedRowIndex = 1
                                }
                            },
                            onNavigateLeft = { newIndex ->
                                if (newIndex >= 0) {
                                    selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                                        put(selectedRowIndex, newIndex)
                                    }
                                } else {
                                    // Navigate to nav bar
                                    focusLevel = 1
                                    onLeftBoundary?.invoke()
                                }
                            },
                            onNavigateRight = { newIndex ->
                                selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                                    put(selectedRowIndex, newIndex)
                                }
                                // Trigger load more if near end
                                if (newIndex >= trendingMovies.movies.size - 5) {
                                    viewModel.loadTrendingMovies()
                                }
                            },
                            onSelect = {
                                val movie = trendingMovies.movies.getOrNull(selectedItemIndices[0] ?: 0)
                                movie?.let {
                                    Log.d("MoviesPage", "🎬 Selected movie: ${it.title}")
                                    onNavigateToDetails?.invoke(it.id)
                                }
                            },
                            onBack = {
                                focusLevel = 1
                                onLeftBoundary?.invoke()
                            }
                        )
                    }
                )
            }

            // Popular Movies Row
            item {
                MovieRow(
                    title = "Popular Movies",
                    movieRowState = popularMovies,
                    isRowFocused = focusLevel == 2 && selectedRowIndex == 1,
                    selectedItemIndex = selectedItemIndices[1] ?: 0,
                    onSelectionChanged = { itemIndex ->
                        selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                            put(1, itemIndex)
                        }
                        selectedRowIndex = 1
                    },
                    onItemClick = { movie ->
                        Log.d("MoviesPage", "🎬 Clicked movie: ${movie.title}")
                        onNavigateToDetails?.invoke(movie.id)
                    },
                    onLoadMore = {
                        viewModel.loadPopularMovies()
                    },
                    onKeyEvent = { keyEvent ->
                        handleKeyEvent(
                            keyEvent = keyEvent,
                            currentRowIndex = selectedRowIndex,
                            currentItemIndex = selectedItemIndices[selectedRowIndex] ?: 0,
                            maxItemIndex = popularMovies.movies.size - 1,
                            onNavigateUp = {
                                if (trendingMovies.movies.isNotEmpty()) {
                                    selectedRowIndex = 0
                                }
                            },
                            onNavigateDown = {
                                // No row below popular
                            },
                            onNavigateLeft = { newIndex ->
                                if (newIndex >= 0) {
                                    selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                                        put(selectedRowIndex, newIndex)
                                    }
                                } else {
                                    // Navigate to nav bar
                                    focusLevel = 1
                                    onLeftBoundary?.invoke()
                                }
                            },
                            onNavigateRight = { newIndex ->
                                selectedItemIndices = selectedItemIndices.toMutableMap().apply {
                                    put(selectedRowIndex, newIndex)
                                }
                                // Trigger load more if near end
                                if (newIndex >= popularMovies.movies.size - 5) {
                                    viewModel.loadPopularMovies()
                                }
                            },
                            onSelect = {
                                val movie = popularMovies.movies.getOrNull(selectedItemIndices[1] ?: 0)
                                movie?.let {
                                    Log.d("MoviesPage", "🎬 Selected movie: ${it.title}")
                                    onNavigateToDetails?.invoke(it.id)
                                }
                            },
                            onBack = {
                                focusLevel = 1
                                onLeftBoundary?.invoke()
                            }
                        )
                    }
                )
            }
        }
    }
}

private fun handleKeyEvent(
    keyEvent: android.view.KeyEvent,
    currentRowIndex: Int,
    currentItemIndex: Int,
    maxItemIndex: Int,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onNavigateLeft: (Int) -> Unit,
    onNavigateRight: (Int) -> Unit,
    onSelect: () -> Unit,
    onBack: () -> Unit,
): Boolean {
    // Throttle navigation
    val throttleMs = 80L
    val currentTime = System.currentTimeMillis()
    
    return when (keyEvent.keyCode) {
        android.view.KeyEvent.KEYCODE_DPAD_UP -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onNavigateUp()
            }
            true
        }
        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onNavigateDown()
            }
            true
        }
        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onNavigateLeft(currentItemIndex - 1)
            }
            true
        }
        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onNavigateRight((currentItemIndex + 1).coerceAtMost(maxItemIndex + 20)) // Allow going beyond for infinite scroll
            }
            true
        }
        android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onSelect()
            }
            true
        }
        android.view.KeyEvent.KEYCODE_BACK -> {
            if (keyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                onBack()
            }
            true
        }
        else -> false
    }
}
