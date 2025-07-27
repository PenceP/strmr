package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.focusable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.CoroutineScope

/**
 * Renders the actual media row content with lazy loading
 * Single Responsibility: Media item display and layout
 */
@Composable
fun <T : MediaItem> MediaRowContent(
    config: PagingMediaRowConfig<T>,
    lazyPagingItems: LazyPagingItems<T>,
    listState: LazyListState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    var localHasFocus by remember { mutableStateOf(false) }
    val keyEventHandler = remember(config, lazyPagingItems, listState, coroutineScope) {
        KeyEventHandler(config, lazyPagingItems, listState, coroutineScope)
    }
    
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (config.isRowSelected && config.focusRequester != null) {
                    Modifier
                        .focusRequester(config.focusRequester)
                        .onFocusChanged { focusState ->
                            localHasFocus = focusState.hasFocus
                            if (focusState.hasFocus) {
                                config.onContentFocusChanged?.invoke(true)
                            }
                            android.util.Log.d(
                                config.logTag,
                                "🎯 Focus changed for '${config.title}': ${focusState.hasFocus}"
                            )
                        }
                } else {
                    Modifier
                }
            )
            .focusable(enabled = config.isRowSelected)
            .onKeyEvent { event ->
                keyEventHandler.handleKeyEvent(event)
            }
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = { index -> lazyPagingItems[index]?.tmdbId ?: index }
        ) { index ->
            MediaItemSlot(
                mediaItem = lazyPagingItems[index],
                isSelected = index == config.selectedIndex && config.isRowSelected && config.isContentFocused,
                onItemClick = config.onItemClick
            )
        }
        
        // Handle append load states
        when (val appendState = lazyPagingItems.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { lazyPagingItems.retry() }
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }
            is LoadState.NotLoading -> {
                // Nothing to show
            }
        }
    }
}

@Composable
private fun <T : MediaItem> MediaItemSlot(
    mediaItem: T?,
    isSelected: Boolean,
    onItemClick: ((T) -> Unit)?
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        mediaItem?.let { item ->
            MediaCard(
                title = item.title,
                posterUrl = item.posterUrl,
                isSelected = isSelected,
                onClick = {
                    onItemClick?.invoke(item)
                }
            )
        } ?: run {
            // Placeholder while loading
            MediaCardSkeleton()
        }
    }
}

