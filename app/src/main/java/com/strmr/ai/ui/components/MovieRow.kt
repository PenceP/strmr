package com.strmr.ai.ui.components

import android.util.Log
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
fun MovieRow(
    title: String,
    movieRowState: MovieRowState,
    isRowFocused: Boolean,
    selectedItemIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    onItemClick: (MovieRowItem) -> Unit,
    onLoadMore: () -> Unit,
    onKeyEvent: (android.view.KeyEvent) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    val posterWidth = 115.dp
    val posterSpacing = 12.dp
    val selectorStart = StrmrConstants.Dimensions.Icons.EXTRA_LARGE

    // Auto-scroll to keep selected item aligned with selector (like episodes)
    LaunchedEffect(selectedItemIndex, isRowFocused) {
        if (movieRowState.movies.isNotEmpty() && selectedItemIndex in 0 until movieRowState.movies.size && isRowFocused) {
            listState.animateScrollToItem(selectedItemIndex)
        }
    }

    // Focus management 
    LaunchedEffect(isRowFocused) {
        if (isRowFocused) {
            kotlinx.coroutines.delay(50)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w("MovieRow", "Failed to request focus: ${e.message}")
            }
        }
    }

    // Load more when near end
    LaunchedEffect(selectedItemIndex, movieRowState.movies.size) {
        if (selectedItemIndex >= movieRowState.movies.size - 5 && movieRowState.hasMore && !movieRowState.isLoading) {
            Log.d("MovieRow", "🔄 Loading more movies for row: $title")
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
            color = if (isRowFocused) {
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

        // Movies row with fixed selector overlay (like episodes)
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
                        isFocused = focusState.isFocused
                    }
                    .onKeyEvent { keyEvent ->
                        onKeyEvent(keyEvent.nativeKeyEvent)
                    }
            ) {
                itemsIndexed(movieRowState.movies) { index, movie ->
                    val itemAlpha = when {
                        !isRowFocused -> 0.6f
                        isRowFocused && index == selectedItemIndex -> 1f
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
                            isSelected = index == selectedItemIndex,
                            isFocused = isRowFocused,
                            onClick = {
                                onSelectionChanged(index)
                                onItemClick(movie)
                            }
                        )
                    }
                }

                // Loading indicator at end
                if (movieRowState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(210.dp), // 140 * 3/2 aspect ratio
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = StrmrConstants.Colors.PRIMARY_BLUE,
                                modifier = Modifier.width(24.dp)
                            )
                        }
                    }
                }

                // Add spacers for smooth scrolling (like episodes)
                repeat(3) {
                    item {
                        Spacer(modifier = Modifier.width(posterWidth + posterSpacing))
                    }
                }
            }

            // Fixed selector overlay (fades when row not focused)
            if (movieRowState.movies.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(start = selectorStart)
                        .width(posterWidth)
                        .height(210.dp) // 2:3 aspect ratio height
                        .graphicsLayer {
                            alpha = if (isRowFocused && isFocused) 1f else 0.3f
                        }
                ) {
                    // The border is handled by the MoviePosterCard itself
                }
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