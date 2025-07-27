package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch

/**
 * Unified MediaRow component with EpisodeView-style navigation UX
 * 
 * Features:
 * - Left-aligned scrolling with partial previous item visibility
 * - Support for both regular lists and paging
 * - Portrait and landscape card types
 * - Loading states with skeletons
 * - 80ms navigation throttling
 * - Smooth scroll animations
 * - Focus management with debouncing
 */
@Composable
fun <T : Any> UnifiedMediaRow(
    config: MediaRowConfig<T>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Navigation throttling (EpisodeView pattern)
    var lastNavTime by remember { mutableStateOf(0L) }
    val throttleMs = 80L
    
    // Focus debouncing
    var lastFocusChangeTime by remember { mutableStateOf(0L) }
    val focusDebounceDelay = 50L
    
    // Prevent focus changes from overriding navigation for a short time
    var ignoreNextFocusChange by remember { mutableStateOf(false) }
    
    val itemCount = when (config.dataSource) {
        is DataSource.RegularList -> config.dataSource.items.size
        is DataSource.PagingList -> config.dataSource.pagingItems.itemCount
    }
    
    // Auto-scroll when selection changes (EpisodeView pattern)
    LaunchedEffect(config.selectedIndex, config.isRowSelected) {
        // Only auto-scroll when this row is actually selected to avoid interference
        if (config.isRowSelected && itemCount > 0 && config.selectedIndex in 0 until itemCount) {
            Log.d("UnifiedMediaRow", "🎯 Auto-scrolling '${config.title}' to item ${config.selectedIndex}")
            listState.animateScrollToItem(config.selectedIndex)
        } else if (!config.isRowSelected) {
            Log.d("UnifiedMediaRow", "🎯 Skipping auto-scroll for '${config.title}' - row not selected")
        } else {
            Log.w("UnifiedMediaRow", "🚨 Invalid scroll request for '${config.title}': selectedIndex=${config.selectedIndex}, itemCount=$itemCount, isRowSelected=${config.isRowSelected}")
        }
    }
    
    // Paging trigger for regular lists
    if (config.dataSource is DataSource.RegularList) {
        LaunchedEffect(config.selectedIndex, itemCount) {
            if (config.isRowSelected && itemCount - config.selectedIndex <= 4) {
                Log.d("UnifiedMediaRow", "📄 Near end of list (${itemCount - config.selectedIndex} items left), triggering load more")
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
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
        
        // Media row with EpisodeView-style navigation
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(config.itemSpacing),
            contentPadding = config.contentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .height(config.getRowHeight())
                .then(
                    if (config.isRowSelected && config.focusRequester != null) {
                        Modifier
                            .focusRequester(config.focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.hasFocus) {
                                    config.onContentFocusChanged?.invoke(true)
                                }
                                Log.d("UnifiedMediaRow", "🎯 Focus changed for '${config.title}': ${focusState.hasFocus}")
                            }
                    } else {
                        Modifier
                    }
                )
                .focusable(enabled = config.isRowSelected)
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && config.isRowSelected) {
                        val now = System.currentTimeMillis()
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (now - lastNavTime > throttleMs) {
                                    if (config.selectedIndex > 0) {
                                        config.onSelectionChanged(config.selectedIndex - 1)
                                        lastNavTime = now
                                    } else {
                                        config.onLeftBoundary?.invoke()
                                    }
                                }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (now - lastNavTime > throttleMs) {
                                    if (config.selectedIndex < itemCount - 1) {
                                        config.onSelectionChanged(config.selectedIndex + 1)
                                        lastNavTime = now
                                    }
                                }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                Log.d("UnifiedMediaRow", "🎯 UP pressed in '${config.title}', selectedIndex=${config.selectedIndex}")
                                ignoreNextFocusChange = true
                                config.onUpDown?.invoke(-1)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                Log.d("UnifiedMediaRow", "🎯 DOWN pressed in '${config.title}', selectedIndex=${config.selectedIndex}")
                                ignoreNextFocusChange = true
                                config.onUpDown?.invoke(1)
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER -> {
                                val item = when (config.dataSource) {
                                    is DataSource.RegularList -> config.dataSource.items.getOrNull(config.selectedIndex)
                                    is DataSource.PagingList -> config.dataSource.pagingItems[config.selectedIndex]
                                }
                                item?.let { config.onItemClick?.invoke(it) }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            when (config.dataSource) {
                is DataSource.RegularList -> {
                    if (config.isLoading && config.dataSource.items.isEmpty()) {
                        // Show skeleton loading state
                        items(config.skeletonCount) {
                            SkeletonCard(config)
                        }
                    } else {
                        itemsIndexed(config.dataSource.items) { index, item ->
                            MediaRowItem(
                                item = item,
                                index = index,
                                config = config,
                                onFocusChanged = { focusState ->
                                    if (focusState.isFocused && config.isRowSelected) {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastFocusChangeTime > focusDebounceDelay) {
                                            lastFocusChangeTime = currentTime
                                            
                                            // Check if we should ignore this focus change
                                            if (ignoreNextFocusChange) {
                                                Log.d("UnifiedMediaRow", "🚫 Ignoring focus change to item $index - navigation in progress")
                                                ignoreNextFocusChange = false
                                            } else if (config.selectedIndex != index) {
                                                Log.d("UnifiedMediaRow", "🎯 Focus changed to item $index (was ${config.selectedIndex})")
                                                config.onSelectionChanged(index)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        // Add spacers for partial previous item visibility (EpisodeView pattern)
                        // Calculate how many spacers needed for last item to scroll fully left
                        val itemWidthWithSpacing = config.itemWidth + config.itemSpacing
                        val screenWidth = 1920.dp // TV screen width
                        val paddingStart = config.contentPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                        val availableWidth = screenWidth - paddingStart
                        val spacersNeeded = (availableWidth / itemWidthWithSpacing).toInt() - 1
                        
                        repeat(maxOf(spacersNeeded, 3)) {
                            item {
                                Spacer(modifier = Modifier.width(itemWidthWithSpacing))
                            }
                        }
                    }
                }
                
                is DataSource.PagingList -> {
                    items(
                        count = config.dataSource.pagingItems.itemCount,
                        key = { index -> 
                            config.dataSource.pagingItems[index]?.let { item ->
                                config.keyExtractor?.invoke(item) ?: index
                            } ?: index
                        }
                    ) { index ->
                        val item = config.dataSource.pagingItems[index]
                        if (item != null) {
                            MediaRowItem(
                                item = item,
                                index = index,
                                config = config,
                                onFocusChanged = { focusState ->
                                    if (focusState.isFocused && config.isRowSelected) {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastFocusChangeTime > focusDebounceDelay) {
                                            lastFocusChangeTime = currentTime
                                            
                                            // Check if we should ignore this focus change
                                            if (ignoreNextFocusChange) {
                                                Log.d("UnifiedMediaRow", "🚫 Ignoring focus change to item $index - navigation in progress")
                                                ignoreNextFocusChange = false
                                            } else if (config.selectedIndex != index) {
                                                Log.d("UnifiedMediaRow", "🎯 Focus changed to item $index (was ${config.selectedIndex})")
                                                config.onSelectionChanged(index)
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            SkeletonCard(config)
                        }
                    }
                    
                    // Handle paging load states
                    when (val appendState = config.dataSource.pagingItems.loadState.append) {
                        is LoadState.Loading -> {
                            item {
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
                            // Add spacers for partial previous item visibility (EpisodeView pattern)
                            val itemWidthWithSpacing = config.itemWidth + config.itemSpacing
                            val screenWidth = 1920.dp // TV screen width
                            val paddingStart = config.contentPadding.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                            val availableWidth = screenWidth - paddingStart
                            val spacersNeeded = (availableWidth / itemWidthWithSpacing).toInt() - 1
                            
                            repeat(maxOf(spacersNeeded, 3)) {
                                item {
                                    Spacer(modifier = Modifier.width(itemWidthWithSpacing))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Any> MediaRowItem(
    item: T,
    index: Int,
    config: MediaRowConfig<T>,
    onFocusChanged: (androidx.compose.ui.focus.FocusState) -> Unit
) {
    val isSelected = config.isRowSelected && index == config.selectedIndex

    Box(
        modifier = Modifier
            .width(config.itemWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .onFocusChanged(onFocusChanged)
                .focusable(enabled = config.isRowSelected),
            contentAlignment = Alignment.BottomCenter
        ) {
            config.itemContent(item, isSelected)
        }
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
    val selectedIndex: Int,
    val isRowSelected: Boolean,
    val onSelectionChanged: (Int) -> Unit,
    val onItemClick: ((T) -> Unit)? = null,
    val onUpDown: ((Int) -> Unit)? = null,
    val onLoadMore: (() -> Unit)? = null,
    val onLeftBoundary: (() -> Unit)? = null,
    val onContentFocusChanged: ((Boolean) -> Unit)? = null,
    val focusRequester: FocusRequester? = null,
    val cardType: CardType = CardType.PORTRAIT,
    val itemWidth: Dp = 120.dp,
    val itemSpacing: Dp = 12.dp,
    val contentPadding: PaddingValues = PaddingValues(horizontal = 48.dp),
    val isLoading: Boolean = false,
    val skeletonCount: Int = 8,
    val keyExtractor: ((T) -> Any)? = null,
    val itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
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
    data class RegularList<T : Any>(val items: List<T>) : DataSource<T>()
    data class PagingList<T : Any>(val pagingItems: LazyPagingItems<T>) : DataSource<T>()
}

enum class CardType {
    PORTRAIT,  // Poster-style cards (120dp wide)
    LANDSCAPE  // Episode/landscape cards (200dp wide)
}

// Convenience builder functions
object MediaRowBuilder {
    fun <T : Any> regular(
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
        itemContent = itemContent
    )
    
    fun <T : Any> paging(
        title: String,
        pagingItems: LazyPagingItems<T>,
        selectedIndex: Int,
        isRowSelected: Boolean,
        onSelectionChanged: (Int) -> Unit,
        itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
    ) = MediaRowConfig(
        title = title,
        dataSource = DataSource.PagingList(pagingItems),
        selectedIndex = selectedIndex,
        isRowSelected = isRowSelected,
        onSelectionChanged = onSelectionChanged,
        itemContent = itemContent
    )
}