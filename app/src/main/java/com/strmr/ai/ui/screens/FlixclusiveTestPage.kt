package com.strmr.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.ui.components.*
import com.strmr.ai.viewmodel.FlixclusiveTrendingViewModel
import com.strmr.ai.ui.components.MediaCard

/**
 * Test page demonstrating Flixclusive-style pagination
 * This replaces the complex Paging3 system with simple, predictable pagination
 */
@Composable
fun FlixclusiveTestPage(
    modifier: Modifier = Modifier,
    viewModel: FlixclusiveTrendingViewModel = hiltViewModel()
) {
    val movies by viewModel.movies.collectAsStateWithLifecycle()
    val paginationState by viewModel.paginationState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Single row demonstrating Flixclusive pagination
        UnifiedMediaRow(
            config = MediaRowConfig(
                title = "Trending Movies (Flixclusive Style)",
                dataSource = DataSource.RegularList(
                    items = movies,
                    paginationState = paginationState
                ),
                onItemClick = { movie ->
                    // Handle movie click
                },
                onPaginate = { page ->
                    viewModel.paginateMovies(page)
                },
                itemContent = { movie, isFocused ->
                    MediaCard(
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        isSelected = isFocused,
                        onClick = { }
                    )
                }
            )
        )
    }
}