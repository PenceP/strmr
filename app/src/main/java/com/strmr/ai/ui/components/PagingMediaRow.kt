package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.strmr.ai.data.database.MovieEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator

/**
 * A paging-aware media row that handles infinite scrolling for movies.
 * This component automatically loads more items when approaching the end of the list.
 */
@Composable
fun PagingMediaRow(
    title: String,
    pagingFlow: Flow<PagingData<MovieEntity>>,
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
    onItemClick: ((MovieEntity) -> Unit)? = null,
    onPositionChanged: ((Int, Int) -> Unit)? = null  // Reports (currentPosition, totalItems)
) {
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var localHasFocus by remember { mutableStateOf(false) }
    
    // Sync scroll position when selected index changes
    LaunchedEffect(selectedIndex, lazyPagingItems.itemCount) {
        if (selectedIndex >= 0 && selectedIndex < lazyPagingItems.itemCount) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
    
    // Report position changes
    LaunchedEffect(selectedIndex, lazyPagingItems.itemCount) {
        if (isRowSelected && lazyPagingItems.itemCount > 0) {
            onPositionChanged?.invoke(selectedIndex, lazyPagingItems.itemCount)
        }
    }
    
    // Log paging state changes
    LaunchedEffect(lazyPagingItems.loadState) {
        when (val refresh = lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Log.d("PagingMediaRow", "📥 Loading initial data for '$title'")
            }
            is LoadState.Error -> {
                Log.e("PagingMediaRow", "❌ Error loading '$title'", refresh.error)
            }
            is LoadState.NotLoading -> {
                Log.d("PagingMediaRow", "✅ Initial load complete for '$title', items: ${lazyPagingItems.itemCount}")
            }
        }
        
        when (val append = lazyPagingItems.loadState.append) {
            is LoadState.Loading -> {
                Log.d("PagingMediaRow", "📥 Loading next page for '$title'")
            }
            is LoadState.Error -> {
                Log.e("PagingMediaRow", "❌ Error loading next page for '$title'", append.error)
            }
            is LoadState.NotLoading -> {
                if (append.endOfPaginationReached) {
                    Log.d("PagingMediaRow", "📄 No more pages for '$title'")
                }
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // Title
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 60.dp, vertical = 8.dp)
            )
            
            // Media items row
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 60.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isRowSelected && focusRequester != null) {
                            Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    localHasFocus = focusState.hasFocus
                                    if (focusState.hasFocus) {
                                        onContentFocusChanged?.invoke(true)
                                    }
                                    Log.d("PagingMediaRow", "🎯 Focus changed for '$title': ${focusState.hasFocus}")
                                }
                        } else {
                            Modifier
                        }
                    )
                    .focusable(enabled = isRowSelected)
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && isRowSelected) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (selectedIndex > 0) {
                                        val newIndex = selectedIndex - 1
                                        onSelectionChanged(newIndex)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(newIndex)
                                        }
                                        
                                        // Report position change
                                        onPositionChanged?.invoke(newIndex, lazyPagingItems.itemCount)
                                        
                                        // Proactive loading check even when going left
                                        val currentIdx = newIndex
                                        val numLoadedItems = lazyPagingItems.itemCount
                                        if (currentIdx + 6 >= numLoadedItems) {
                                            Log.d("PagingMediaRow", "🚀 Proactive load trigger (left): currentIdx($currentIdx) + 6 >= numLoadedItems($numLoadedItems)")
                                            // Force Paging3 to load more by accessing an item near the end
                                            val triggerIndex = minOf(currentIdx + 3, numLoadedItems - 1)
                                            lazyPagingItems[triggerIndex] // This access triggers Paging3 to load more
                                        }
                                        
                                        true
                                    } else false
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (selectedIndex < lazyPagingItems.itemCount - 1) {
                                        val newIndex = selectedIndex + 1
                                        onSelectionChanged(newIndex)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(newIndex)
                                        }
                                        
                                        // Log position for debugging
                                        val remainingItems = lazyPagingItems.itemCount - newIndex - 1
                                        val item = lazyPagingItems[newIndex]
                                        Log.d("PagingMediaRow", "📊 Row '$title' - Position: ${newIndex + 1}/${lazyPagingItems.itemCount}, Remaining: $remainingItems")
                                        Log.d("PagingMediaRow", "   Selected: ${item?.title}")
                                        
                                        // Report position change
                                        onPositionChanged?.invoke(newIndex, lazyPagingItems.itemCount)
                                        
                                        // Proactive loading check: if (currentIdx + 6) > numLoadedItems, trigger load
                                        val currentIdx = newIndex
                                        val numLoadedItems = lazyPagingItems.itemCount
                                        if (currentIdx + 6 >= numLoadedItems) {
                                            Log.d("PagingMediaRow", "🚀 Proactive load trigger: currentIdx($currentIdx) + 6 >= numLoadedItems($numLoadedItems)")
                                            // Force Paging3 to load more by accessing an item near the end
                                            val triggerIndex = minOf(currentIdx + 3, numLoadedItems - 1)
                                            lazyPagingItems[triggerIndex] // This access triggers Paging3 to load more
                                        }
                                        
                                        true
                                    } else false
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                    onUpDown?.invoke(-1)
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    onUpDown?.invoke(1)
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                                android.view.KeyEvent.KEYCODE_ENTER -> {
                                    lazyPagingItems[selectedIndex]?.let { item ->
                                        onItemClick?.invoke(item)
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems[index]?.tmdbId ?: index }
                ) { index ->
                    lazyPagingItems[index]?.let { movie ->
                        val isSelected = index == selectedIndex && isRowSelected
                        
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaCard(
                                title = movie.title,
                                posterUrl = movie.posterUrl,
                                isSelected = isSelected,
                                onClick = {
                                    onItemClick?.invoke(movie)
                                }
                            )
                        }
                    } ?: run {
                        // Placeholder while loading
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(240.dp)
                        ) {
                            MediaCardSkeleton()
                        }
                    }
                }
                
                // Handle different load states
                when (lazyPagingItems.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(240.dp),
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
                                    .width(160.dp)
                                    .height(240.dp),
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
    }
}