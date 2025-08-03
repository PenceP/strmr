package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.MovieRowItem
import com.strmr.ai.viewmodel.MovieRowState

@Composable
fun NetflixMovieRow(
    rowIndex: Int,
    title: String,
    movieRowState: MovieRowState,
    isFocused: Boolean,
    selectedIndex: Int,
    scrollPosition: Int,
    onSelectionChanged: (Int) -> Unit,
    onScrollPositionChanged: (Int) -> Unit,
    onMovieClick: (MovieRowItem) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateDown: () -> Unit,
    onNavigateLeft: (Boolean) -> Unit, // Boolean indicates if can go further left
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var isInternallyFocused by remember { mutableStateOf(false) }

    val posterWidth = 115.dp
    val posterSpacing = 12.dp
    val selectorStart = StrmrConstants.Dimensions.Icons.EXTRA_LARGE

    // Restore scroll position when row becomes focused
    LaunchedEffect(isFocused) {
        if (isFocused && movieRowState.movies.isNotEmpty()) {
            // Request focus after small delay
            kotlinx.coroutines.delay(50)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w("NetflixMovieRow", "Failed to request focus: ${e.message}")
            }
        }
    }

    // Smooth scroll to keep selected item in static focus position
    LaunchedEffect(selectedIndex) {
        if (isFocused && movieRowState.movies.isNotEmpty() && selectedIndex in 0 until movieRowState.movies.size) {
            // Always scroll to position selected item at static focus spot
            listState.animateScrollToItem(selectedIndex)
        }
    }

    // Load more when approaching end
    LaunchedEffect(selectedIndex, movieRowState.movies.size) {
        if (selectedIndex >= movieRowState.movies.size - 5 && movieRowState.hasMore && !movieRowState.isLoading) {
            onLoadMore()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Row title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) {
                StrmrConstants.Colors.TEXT_PRIMARY
            } else {
                StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = 0.7f)
            },
            fontSize = 24.sp,
            modifier = Modifier.padding(
                start = selectorStart,
                bottom = 16.dp
            )
        )

        // Movies row with static focus position
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(posterSpacing),
                contentPadding = PaddingValues(
                    start = selectorStart,
                    end = selectorStart
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isInternallyFocused = focusState.isFocused
                    }
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
                                    if (selectedIndex < movieRowState.movies.size - 1) {
                                        onSelectionChanged(selectedIndex + 1)
                                    } else if (movieRowState.hasMore) {
                                        onLoadMore()
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                    movieRowState.movies.getOrNull(selectedIndex)?.let { movie ->
                                        onMovieClick(movie)
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_BACK -> {
                                    onNavigateBack()
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                itemsIndexed(movieRowState.movies) { index, movie ->
                    val itemAlpha = when {
                        !isFocused -> 0.6f
                        isFocused && index == selectedIndex -> 1f
                        else -> 0.6f
                    }

                    Box(
                        modifier = Modifier
                            .width(posterWidth)
                            .graphicsLayer {
                                alpha = itemAlpha
                            }
                    ) {
                        MoviePosterCard(
                            movie = movie,
                            isSelected = index == selectedIndex,
                            isFocused = isFocused,
                            onClick = {
                                onSelectionChanged(index)
                                onMovieClick(movie)
                            }
                        )
                    }
                }

                // Loading indicator
                if (movieRowState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(172.dp), // 2:3 aspect ratio
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

            // Static focus indicator (border overlay)
            if (movieRowState.movies.isNotEmpty() && isFocused) {
                Box(
                    modifier = Modifier
                        .padding(start = selectorStart)
                        .width(posterWidth)
                        .height(172.dp) // 2:3 aspect ratio
                        .border(
                            width = 3.dp,
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .graphicsLayer {
                            alpha = if (isInternallyFocused) 1f else 0.3f
                        }
                )
            }
        }

        // Error message
        movieRowState.error?.let { error ->
            Text(
                text = error,
                color = StrmrConstants.Colors.ERROR_RED,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(
                    start = selectorStart,
                    top = 8.dp
                )
            )
        }
    }
}