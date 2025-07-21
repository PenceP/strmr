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
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.focusable

/**
 * A paging-aware media row that handles infinite scrolling for TV shows.
 * This component automatically loads more items when approaching the end of the list.
 */
@Composable
fun PagingTvShowRow(
    title: String,
    pagingFlow: Flow<PagingData<TvShowEntity>>,
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
    onItemClick: ((TvShowEntity) -> Unit)? = null
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
    
    // Log paging state changes
    LaunchedEffect(lazyPagingItems.loadState) {
        when (val refresh = lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Log.d("PagingTvShowRow", "📥 Loading initial data for '$title'")
            }
            is LoadState.Error -> {
                Log.e("PagingTvShowRow", "❌ Error loading '$title'", refresh.error)
            }
            is LoadState.NotLoading -> {
                Log.d("PagingTvShowRow", "✅ Initial load complete for '$title', items: ${lazyPagingItems.itemCount}")
            }
        }
        
        when (val append = lazyPagingItems.loadState.append) {
            is LoadState.Loading -> {
                Log.d("PagingTvShowRow", "📥 Loading next page for '$title'")
            }
            is LoadState.Error -> {
                Log.e("PagingTvShowRow", "❌ Error loading next page for '$title'", append.error)
            }
            is LoadState.NotLoading -> {
                if (append.endOfPaginationReached) {
                    Log.d("PagingTvShowRow", "📄 No more pages for '$title'")
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
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp)
            )
            
            // Media items row
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 50.dp),
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
                                    Log.d("PagingTvShowRow", "🎯 Focus changed for '$title': ${focusState.hasFocus}")
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
                                        onSelectionChanged(selectedIndex - 1)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(selectedIndex - 1)
                                        }
                                        true
                                    } else false
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (selectedIndex < lazyPagingItems.itemCount - 1) {
                                        onSelectionChanged(selectedIndex + 1)
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(selectedIndex + 1)
                                        }
                                        
                                        // Log position for debugging
                                        val remainingItems = lazyPagingItems.itemCount - selectedIndex - 1
                                        Log.d("PagingTvShowRow", "📊 Row '$title' - Position: ${selectedIndex + 1}/${lazyPagingItems.itemCount}, Remaining: $remainingItems")
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
                    lazyPagingItems[index]?.let { show ->
                        val isSelected = index == selectedIndex && isRowSelected
                        
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaCard(
                                title = show.title,
                                posterUrl = show.posterUrl,
                                isSelected = isSelected,
                                onClick = {
                                    onItemClick?.invoke(show)
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
                
                // Loading indicator at the end
                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaCardSkeleton()
                        }
                    }
                }
            }
        }
    }
}