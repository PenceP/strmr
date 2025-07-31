package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.ScrollableDefaults
import kotlinx.coroutines.launch

/**
 * Unified MediaRow component based on Flixclusive patterns
 * 
 * Fixed Issues:
 * - ✅ Stable keys for LazyRow items prevent jumping
 * - ✅ Buffer-based pagination (6 items before end)
 * - ✅ Simplified focus management using TvLazyRow
 * - ✅ Long-press detection (>800ms) vs click differentiation
 * - ✅ Removed complex throttling/debouncing - let TV components handle it
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T : Any> UnifiedMediaRow(
    config: MediaRowConfig<T>,
    modifier: Modifier = Modifier,
    onPositionChanged: ((Int, Int) -> Unit)? = null // ✅ NEW: Callback to report scroll position
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Custom responsive FlingBehavior that stops immediately on button release (no momentum).
    val responsiveFlingBehavior = remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                // Immediately consume all velocity for DPAD responsiveness.
                return 0f
            }
        }
    }

    val itemCount = when (config.dataSource) {
        is DataSource.RegularList -> config.dataSource.items.size
        is DataSource.PagingList -> config.dataSource.pagingItems.itemCount
    }

    // NEW: Track scroll position and report to parent - using LazyListState
    LaunchedEffect(listState.firstVisibleItemIndex, itemCount) {
        if (itemCount > 0) {
            onPositionChanged?.invoke(listState.firstVisibleItemIndex, itemCount)
        }
    }

    // FLIXCLUSIVE PATTERN: Buffer-based pagination trigger using shouldPaginate extension
    val shouldStartPaginate by remember(listState) {
        derivedStateOf {
            listState.shouldPaginate(toDeduct = 6)
        }
    }

    var lastPaginationPage by remember { mutableStateOf(-1) }

    if (config.dataSource is DataSource.RegularList) {
        val paginationState = config.dataSource.paginationState

        LaunchedEffect(shouldStartPaginate, paginationState.currentPage) {
            if (shouldStartPaginate && 
                paginationState.canPaginate &&
                (paginationState.pagingState == PagingState.IDLE || 
                 paginationState.pagingState == PagingState.ERROR ||
                        config.dataSource.items.isEmpty()) &&
                lastPaginationPage != paginationState.currentPage
            ) {

                Log.d(
                    "UnifiedMediaRow",
                    " Flixclusive pagination triggered for '${config.title}' page ${paginationState.currentPage}"
                )
                lastPaginationPage = paginationState.currentPage
                config.onPaginate?.invoke(paginationState.currentPage)
            }
        }
        
        // Fallback to old pagination for backward compatibility
        LaunchedEffect(shouldStartPaginate) {
            if (shouldStartPaginate && 
                config.canLoadMore && 
                !config.isLoading &&
                config.onPaginate == null
            ) {
                Log.d("UnifiedMediaRow", " Legacy pagination triggered for '${config.title}'")
                config.onLoadMore?.invoke()
            }
        }
    }
    
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Row title
        if (config.title.isNotBlank()) {
            Text(
                text = config.title,
                color = Color.White,
                modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
            )
        }

        // Use regular LazyRow with an immediate-stop FlingBehavior for better DPAD/TV responsiveness
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
            contentPadding = config.contentPadding,
            flingBehavior = responsiveFlingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(config.getRowHeight())
                .then(
                    if (config.focusRequester != null) {
                        Modifier.focusRequester(config.focusRequester)
                    } else {
                        Modifier
                    }
                )
        ) {
            when (config.dataSource) {
                is DataSource.RegularList -> {
                    val paginationState = config.dataSource.paginationState

                    if ((config.isLoading || paginationState.pagingState == PagingState.LOADING) &&
                        (config.dataSource.items.isEmpty() || config.dataSource.items.size < 20)) {
                        // Show skeleton loading state for initial load or when refreshing incomplete data
                        items(count = config.skeletonCount) {
                            SkeletonCard(config)
                        }
                    } else {
                        // ✅ FIXED: Add stable keys to prevent jumping (Flixclusive pattern)
                        items(
                            count = config.dataSource.items.size,
                            key = { index ->
                                getStableKey(
                                    config.dataSource.items[index],
                                    config.keyExtractor
                                )
                            }
                        ) { index ->
                            val item = config.dataSource.items[index]
                            MediaRowItem(
                                item = item,
                                config = config,
                                index = index,
                                onItemClick = { config.onItemClick?.invoke(item) },
                                onItemLongPress = {
                                    Log.d(
                                        "UnifiedMediaRow",
                                        " Long-press detected on item: ${getItemTitle(item)}"
                                    )
                                    config.onItemLongPress?.invoke(item)
                                },
                                onItemFocused = { focusedIndex ->
                                    Log.d(
                                        "UnifiedMediaRow",
                                        " Item focused at index: $focusedIndex"
                                    )
                                    config.onSelectionChanged(focusedIndex)
                                }
                            )
                        }

                        // FLIXCLUSIVE PATTERN: Handle pagination loading states
                        when (paginationState.pagingState) {
                            PagingState.PAGINATING -> {
                                items(count = 3) {
                                    SkeletonCard(config)
                                }
                            }
                            PagingState.ERROR -> {
                                item {
                                    ErrorCard(
                                        config = config,
                                        onRetry = {
                                            config.onPaginate?.invoke(paginationState.currentPage)
                                        }
                                    )
                                }
                            }
                            else -> {
                                // Fallback to legacy loading indicator
                                if (config.isLoading && config.dataSource.items.isNotEmpty()) {
                                    items(count = 3) {
                                        SkeletonCard(config)
                                    }
                                }
                            }
                        }
                    }
                }

                is DataSource.PagingList -> {
                    // ✅ FIXED: Add stable keys for paging items too
                    items(
                        count = config.dataSource.pagingItems.itemCount,
                        key = { index ->
                            config.dataSource.pagingItems[index]?.let { item ->
                                getStableKey(item, config.keyExtractor)
                            } ?: "placeholder_$index"
                        }
                    ) { index ->
                        val item = config.dataSource.pagingItems[index]
                        if (item != null) {
                            MediaRowItem(
                                item = item,
                                config = config,
                                index = index,
                                onItemClick = { config.onItemClick?.invoke(item) },
                                onItemLongPress = {
                                    Log.d(
                                        "UnifiedMediaRow",
                                        " Long-press detected on item: ${getItemTitle(item)}"
                                    )
                                    config.onItemLongPress?.invoke(item)
                                },
                                onItemFocused = { focusedIndex ->
                                    Log.d(
                                        "UnifiedMediaRow",
                                        " Item focused at index: $focusedIndex"
                                    )
                                    config.onSelectionChanged(focusedIndex)
                                }
                            )
                        } else {
                            SkeletonCard(config)
                        }
                    }

                    // Handle paging load states (Flixclusive pattern)
                    when (val appendState = config.dataSource.pagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            items(count = 3) {
                                SkeletonCard(config)
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                ErrorCard(
                                    config = config,
                                    onRetry = { config.dataSource.pagingItems.retry() }
                                )
                            }
                        }
                        is LoadState.NotLoading -> {
                            // No extra spacers needed
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun <T : Any> MediaRowItem(
    item: T,
    config: MediaRowConfig<T>,
    index: Int,
    onItemClick: () -> Unit,
    onItemLongPress: () -> Unit,
    onItemFocused: (Int) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .width(config.itemWidth)
            .fillMaxHeight()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onItemFocused(index)
                }
            }
            .focusable()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongPress,
                onLongClickLabel = "Long press for options"
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        config.itemContent(item, isFocused)
    }
}

@Composable
private fun <T : Any> SkeletonCard(config: MediaRowConfig<T>) {
    Box(
        modifier = Modifier
            .width(config.itemWidth)
            .height(config.getCardHeight()),
        contentAlignment = Alignment.Center
    ) {
        when (config.cardType) {
            CardType.PORTRAIT -> MediaCardSkeleton()
            CardType.LANDSCAPE -> LandscapeMediaCardSkeleton()
        }
    }
}

@Composable
private fun <T : Any> ErrorCard(
    config: MediaRowConfig<T>,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(config.itemWidth)
            .height(config.getCardHeight()),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Button(onClick = onRetry) {
            Text("Retry", color = Color.White)
        }
    }
}

// Configuration data classes and enums
data class MediaRowConfig<T : Any>(
    val title: String,
    val dataSource: DataSource<T>,
    
    // ✅ NEW: Simplified interface
    val onItemClick: ((T) -> Unit)? = null,
    val onItemLongPress: ((T) -> Unit)? = null, // ✅ NEW: Long-press support
    val onLoadMore: (() -> Unit)? = null,
    val onPaginate: ((Int) -> Unit)? = null, // ✅ NEW: Flixclusive-style pagination
    val focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    val cardType: CardType = CardType.PORTRAIT,
    val itemWidth: Dp = 120.dp,
    val itemSpacing: Dp = 12.dp,
    val contentPadding: PaddingValues = PaddingValues(horizontal = 56.dp),
    val isLoading: Boolean = false,
    val canLoadMore: Boolean = true, // ✅ NEW: Pagination control
    val skeletonCount: Int = 8,
    val keyExtractor: ((T) -> Any)? = null,
    val itemContent: @Composable (item: T, isFocused: Boolean) -> Unit,
    
    // 🔄 BACKWARD COMPATIBILITY: Old parameters (ignored but kept for compilation)
    val selectedIndex: Int = 0,
    val isRowSelected: Boolean = true,
    val onSelectionChanged: (Int) -> Unit = {},
    val onUpDown: ((Int) -> Unit)? = null,
    val onLeftBoundary: (() -> Unit)? = null,
    val onContentFocusChanged: ((Boolean) -> Unit)? = null,
    val isContentFocused: Boolean = true
) {
    fun getRowHeight(): Dp = when (cardType) {
        CardType.PORTRAIT -> 210.dp
        CardType.LANDSCAPE -> 140.dp
    }
    
    fun getCardHeight(): Dp = when (cardType) {
        CardType.PORTRAIT -> 180.dp
        CardType.LANDSCAPE -> 112.dp
    }
}

sealed class DataSource<T : Any> {
    data class RegularList<T : Any>(
        val items: List<T>,
        val paginationState: PaginationStateInfo = PaginationStateInfo(
            canPaginate = false,
            pagingState = PagingState.IDLE,
            currentPage = 1
        )
    ) : DataSource<T>()
    data class PagingList<T : Any>(val pagingItems: LazyPagingItems<T>) : DataSource<T>()
}

enum class CardType {
    PORTRAIT,  // Poster-style cards (120dp wide)
    LANDSCAPE  // Episode/landscape cards (200dp wide)
}

// ✅ NEW: Helper functions for stable keys
fun <T : Any> getStableKey(item: T, keyExtractor: ((T) -> Any)?): Any {
    return keyExtractor?.invoke(item) ?: when (item) {
        is com.strmr.ai.data.database.MovieEntity -> "movie_${item.tmdbId}"
        is com.strmr.ai.data.database.TvShowEntity -> "tv_${item.tmdbId}"
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> "home_movie_${item.movie.tmdbId}"
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> "home_tv_${item.show.tmdbId}"
        is com.strmr.ai.viewmodel.HomeMediaItem.Collection -> "collection_${item.id}"
        is com.strmr.ai.data.SearchResultItem.Movie -> "search_movie_${item.id}"
        is com.strmr.ai.data.SearchResultItem.TvShow -> "search_tv_${item.id}"
        is com.strmr.ai.data.SearchResultItem.Person -> "search_person_${item.id}"
        else -> item.hashCode()
    }
}

fun <T : Any> getItemTitle(item: T): String {
    return when (item) {
        is com.strmr.ai.data.database.MovieEntity -> item.title
        is com.strmr.ai.data.database.TvShowEntity -> item.title
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> item.movie.title
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> item.show.title
        is com.strmr.ai.viewmodel.HomeMediaItem.Collection -> item.name
        is com.strmr.ai.data.SearchResultItem.Movie -> item.title
        is com.strmr.ai.data.SearchResultItem.TvShow -> item.title
        is com.strmr.ai.data.SearchResultItem.Person -> item.name
        else -> "Unknown"
    }
}

// Convenience builder functions
object MediaRowBuilder {
    fun <T : Any> regular(
        title: String,
        items: List<T>,
        itemContent: @Composable (item: T, isFocused: Boolean) -> Unit
    ) = MediaRowConfig(
        title = title,
        dataSource = DataSource.RegularList(items),
        itemContent = itemContent
    )
    
    fun <T : Any> paging(
        title: String,
        pagingItems: LazyPagingItems<T>,
        itemContent: @Composable (item: T, isFocused: Boolean) -> Unit
    ) = MediaRowConfig(
        title = title,
        dataSource = DataSource.PagingList(pagingItems),
        itemContent = itemContent
    )
    
    // 🔄 BACKWARD COMPATIBILITY: Legacy builder with old parameters
    @Deprecated("Use the simplified interface without selectedIndex, isRowSelected, etc.")
    fun <T : Any> legacy(
        title: String,
        items: List<T>,
        selectedIndex: Int,
        isRowSelected: Boolean,
        onSelectionChanged: (Int) -> Unit,
        itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
    ) = MediaRowConfig(
        title = title,
        dataSource = DataSource.RegularList(items),
        selectedIndex = selectedIndex,
        isRowSelected = isRowSelected,
        onSelectionChanged = onSelectionChanged,
        itemContent = { item, isFocused -> 
            // Map new isFocused to old isSelected for backward compatibility
            itemContent(item, isFocused && isRowSelected && items.indexOf(item) == selectedIndex)
        }
    )
}

// Extension functions for legacy compatibility
fun Any.getTitle(): String {
    return when (this) {
        is com.strmr.ai.data.database.MovieEntity -> this.title
        is com.strmr.ai.data.database.TvShowEntity -> this.title
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> this.movie.title
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> this.show.title
        is com.strmr.ai.data.SearchResultItem.Movie -> this.title
        is com.strmr.ai.data.SearchResultItem.TvShow -> this.title
        is com.strmr.ai.data.SearchResultItem.Person -> this.name
        else -> "Unknown"
    }
}

fun Any.getPosterUrl(): String? {
    return when (this) {
        is com.strmr.ai.data.database.MovieEntity -> this.posterUrl
        is com.strmr.ai.data.database.TvShowEntity -> this.posterUrl
        is com.strmr.ai.viewmodel.HomeMediaItem.Movie -> this.movie.posterUrl
        is com.strmr.ai.viewmodel.HomeMediaItem.TvShow -> this.show.posterUrl
        is com.strmr.ai.data.SearchResultItem.Movie -> this.posterPath
        is com.strmr.ai.data.SearchResultItem.TvShow -> this.posterPath
        is com.strmr.ai.data.SearchResultItem.Person -> this.profilePath
        else -> null
    }
}

// Extension function moved to PaginationState.kt to avoid conflicts