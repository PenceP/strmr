package com.strmr.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.ui.components.*
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.viewmodel.FlixclusiveGenericViewModel
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.MediaType

/**
 * Test page demonstrating Flixclusive-style pagination
 * This replaces the complex Paging3 system with simple, predictable pagination
 */
@Composable
fun FlixclusiveTestPage(
    modifier: Modifier = Modifier,
    viewModel: FlixclusiveGenericViewModel = hiltViewModel(),
) {
    // Configure trending movies data source
    val trendingConfig = remember {
        DataSourceConfig(
            id = "trending",
            title = "Trending",
            endpoint = "movies/trending",
            mediaType = MediaType.MOVIE,
            cacheKey = "trending"
        )
    }
    
    // Initialize data source
    LaunchedEffect(Unit) {
        viewModel.initializeDataSource(trendingConfig)
    }
    
    val movies by viewModel.getMoviesFlow("trending").collectAsStateWithLifecycle()
    val paginationState by viewModel.getPaginationFlow("trending").collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        // Single row demonstrating Flixclusive pagination
        UnifiedMediaRow(
            config =
                MediaRowConfig(
                    title = "Trending Movies (Flixclusive Style)",
                    dataSource =
                        DataSource.RegularList(
                            items = movies,
                            paginationState = paginationState,
                        ),
                    onItemClick = { movie ->
                        // Handle movie click
                    },
                    onPaginate = { page ->
                        viewModel.paginateMovies(trendingConfig, page)
                    },
                    itemContent = { movie, isFocused ->
                        MediaCard(
                            title = movie.title,
                            posterUrl = movie.posterUrl,
                            isSelected = isFocused,
                            onClick = { },
                        )
                    },
                ),
        )
    }
}
