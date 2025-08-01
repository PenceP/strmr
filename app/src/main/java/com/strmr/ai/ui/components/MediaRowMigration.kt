package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems

/**
 * Migration helper functions to replace existing row components with UnifiedMediaRow
 * These maintain API compatibility while using the new unified implementation
 */

/**
 * Drop-in replacement for MediaRow.kt
 */
@Composable
fun <T> MigratedMediaRow(
    title: String,
    mediaItems: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onUpDown: ((Int) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 18.dp,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) where T : Any {
    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = title,
                dataSource = DataSource.RegularList(mediaItems),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onLoadMore = onLoadMore,
                onItemClick = onItemClick,
                itemWidth = itemWidth,
                itemSpacing = itemSpacing,
                contentPadding = PaddingValues(horizontal = 8.dp),
                cardType = CardType.PORTRAIT,
                itemContent = itemContent,
            ),
        modifier = modifier,
    )
}

/**
 * Drop-in replacement for EnhancedMediaRow.kt with loading states
 */
@Composable
fun <T> MigratedEnhancedMediaRow(
    title: String,
    mediaItems: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onUpDown: ((Int) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 18.dp,
    isLoading: Boolean = false,
    loadingCardCount: Int = 8,
    skeletonCardType: SkeletonCardType = SkeletonCardType.PORTRAIT,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) where T : Any {
    if (isLoading && mediaItems.isEmpty()) {
        MediaRowSkeleton(
            title = title,
            cardCount = loadingCardCount,
            itemWidth = itemWidth,
            itemSpacing = itemSpacing,
            cardType = skeletonCardType,
            modifier = modifier,
        )
    } else {
        UnifiedMediaRow(
            config =
                MediaRowConfig(
                    title = title,
                    dataSource = DataSource.RegularList(mediaItems),
                    selectedIndex = selectedIndex,
                    isRowSelected = isRowSelected,
                    onSelectionChanged = onSelectionChanged,
                    onUpDown = onUpDown,
                    onLoadMore = onLoadMore,
                    onItemClick = onItemClick,
                    itemWidth = itemWidth,
                    itemSpacing = itemSpacing,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    cardType = CardType.PORTRAIT,
                    isLoading = isLoading,
                    skeletonCount = loadingCardCount,
                    itemContent = itemContent,
                ),
            modifier = modifier,
        )
    }
}

/**
 * Drop-in replacement for PagingMediaRow.kt
 */
@Composable
fun <T : Any> MigratedPagingMediaRow(
    title: String,
    pagingFlow: androidx.paging.PagingData<T>? = null,
    pagingItems: LazyPagingItems<T>? = null,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onUpDown: ((Int) -> Unit)? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    currentRowIndex: Int = 0,
    totalRowCount: Int = 1,
    onItemClick: ((T) -> Unit)? = null,
    onPositionChanged: ((Int, Int) -> Unit)? = null,
    logTag: String = "PagingMediaRow",
) {
    // Handle both pagingFlow and pagingItems for compatibility
    val items = pagingItems ?: throw IllegalArgumentException("pagingItems must be provided - pagingFlow support requires collectAsLazyPagingItems() at call site")

    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = title,
                dataSource = DataSource.PagingList(items),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onLeftBoundary = onLeftBoundary,
                onItemClick = onItemClick,
                onContentFocusChanged = onContentFocusChanged,
                focusRequester = focusRequester,
                contentPadding = PaddingValues(horizontal = 60.dp),
                cardType = CardType.PORTRAIT,
                keyExtractor = { item ->
                    // Try to extract a unique key from common properties
                    when {
                        item is Any && item::class.java.getDeclaredField("tmdbId") != null -> {
                            try {
                                val field = item::class.java.getDeclaredField("tmdbId")
                                field.isAccessible = true
                                field.get(item)
                            } catch (e: Exception) {
                                item.hashCode()
                            }
                        }
                        else -> item.hashCode()
                    }
                },
                itemContent = { item, isSelected ->
                    MediaCard(
                        title = item.getTitle(),
                        posterUrl = item.getPosterUrl(),
                        isSelected = isSelected,
                        onClick = { onItemClick?.invoke(item) },
                    )
                },
            ),
        modifier = modifier,
    )
}

/**
 * Drop-in replacement for PagingTvShowRow.kt
 */
@Composable
fun MigratedPagingTvShowRow(
    title: String,
    pagingFlow: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<com.strmr.ai.data.database.TvShowEntity>>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onUpDown: ((Int) -> Unit)? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    currentRowIndex: Int = 0,
    totalRowCount: Int = 1,
    onItemClick: ((com.strmr.ai.data.database.TvShowEntity) -> Unit)? = null,
) {
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()

    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = title,
                dataSource = DataSource.PagingList(lazyPagingItems),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onItemClick = onItemClick,
                onContentFocusChanged = onContentFocusChanged,
                focusRequester = focusRequester,
                contentPadding = PaddingValues(horizontal = 60.dp),
                cardType = CardType.PORTRAIT,
                keyExtractor = { show -> show.tmdbId },
                itemContent = { show, isSelected ->
                    MediaCard(
                        title = show.title,
                        posterUrl = show.posterUrl,
                        isSelected = isSelected,
                        onClick = { onItemClick?.invoke(show) },
                    )
                },
            ),
        modifier = modifier,
    )
}

/**
 * Simplified row for episode-style landscape cards
 */
@Composable
fun <T : Any> EpisodeStyleRow(
    title: String,
    items: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onUpDown: ((Int) -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = title,
                dataSource = DataSource.RegularList(items),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onItemClick = onItemClick,
                onContentFocusChanged = onContentFocusChanged,
                focusRequester = focusRequester,
                cardType = CardType.LANDSCAPE,
                itemWidth = 200.dp,
                itemSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 48.dp),
                itemContent = itemContent,
            ),
        modifier = modifier,
    )
}

/**
 * Collection-specific row (replaces CollectionRow.kt)
 */
@Composable
fun MigratedCollectionRow(
    collectionMovies: List<com.strmr.ai.data.CollectionMovie>,
    onItemClick: (com.strmr.ai.data.CollectionMovie) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
) {
    if (collectionMovies.isEmpty()) return

    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = "Part of Collection",
                dataSource = DataSource.RegularList(collectionMovies.take(10)),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onItemClick = onItemClick,
                onContentFocusChanged = onContentFocusChanged,
                focusRequester = focusRequester,
                cardType = CardType.PORTRAIT,
                itemWidth = 90.dp,
                itemSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 48.dp),
                itemContent = { movie, isSelected ->
                    CollectionMovieCard(
                        movie = movie,
                        onClick = { onItemClick(movie) },
                        isSelected = isSelected,
                    )
                },
            ),
        modifier = modifier,
    )
}

/**
 * Similar content row (replaces SimilarContentRow.kt)
 */
@Composable
fun MigratedSimilarContentRow(
    similarContent: List<com.strmr.ai.data.SimilarContent>,
    onItemClick: (com.strmr.ai.data.SimilarContent) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
) {
    if (similarContent.isEmpty()) return

    val title = "Similar ${if (similarContent.firstOrNull()?.mediaType == "movie") "Movies" else "TV Shows"}"

    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = title,
                dataSource = DataSource.RegularList(similarContent.take(10)),
                selectedIndex = selectedIndex,
                isRowSelected = isRowSelected,
                onSelectionChanged = onSelectionChanged,
                onUpDown = onUpDown,
                onItemClick = onItemClick,
                onContentFocusChanged = onContentFocusChanged,
                focusRequester = focusRequester,
                cardType = CardType.PORTRAIT,
                itemWidth = 90.dp,
                itemSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 48.dp),
                itemContent = { content, isSelected ->
                    SimilarContentCard(
                        content = content,
                        onClick = { onItemClick(content) },
                        isSelected = isSelected,
                    )
                },
            ),
        modifier = modifier,
    )
}
