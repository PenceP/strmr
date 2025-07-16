// File: ui/components/MediaRow.kt
package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun <T> MediaRow(
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
    itemSpacing: Dp = 18.dp, // Increased from 12.dp
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) where T : Any {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Debouncing variables to prevent rapid focus changes
    var lastFocusChangeTime by remember { mutableStateOf(0L) }
    val focusDebounceDelay = 50L // 50ms debounce

    // Monitor selected index for paging
    LaunchedEffect(selectedIndex, mediaItems.size) {
        if (isRowSelected && mediaItems.size - selectedIndex <= 4) {
            Log.d("MediaRow", "📄 Near end of list (${mediaItems.size - selectedIndex} items left), triggering load more")
            onLoadMore?.invoke()
        }
    }

    // Improved scroll timing with completion delay
    suspend fun scrollToIndexSafe(newIndex: Int) {
        try {
            if (newIndex >= 0 && newIndex < mediaItems.size) {
                Log.d("MediaRow", "Scrolling to index: $newIndex")
                listState.animateScrollToItem(newIndex)
                delay(100) // Ensure scroll completes
            }
        } catch (e: Exception) {
            Log.e("MediaRow", "Error scrolling to index $newIndex: ${e.message}")
        }
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Row title
        Text(
            text = title,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp) // Increased to fit 1.1x poster
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && isRowSelected) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (selectedIndex > 0) {
                                    onSelectionChanged(selectedIndex - 1)
                                    scope.launch {
                                        scrollToIndexSafe(selectedIndex - 1)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (selectedIndex < mediaItems.size - 1) {
                                    onSelectionChanged(selectedIndex + 1)
                                    scope.launch {
                                        scrollToIndexSafe(selectedIndex + 1)
                                    }
                                    true
                                } else {
                                    false
                                }
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
                                // Handle click without race condition
                                val item = mediaItems.getOrNull(selectedIndex)
                                if (item != null) {
                                    Log.d("MediaRow", "🎯 Enter pressed on item $selectedIndex ('${item.getTitle()}')")
                                    onItemClick?.invoke(item)
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            itemsIndexed(mediaItems) { index, item ->
                val isSelected = isRowSelected && index == selectedIndex

                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .onFocusChanged { fs ->
                                if (fs.isFocused && isRowSelected) {
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastFocusChangeTime > focusDebounceDelay) {
                                        lastFocusChangeTime = currentTime
                                        Log.d("MediaRow", "Focused item $index ('${item.getTitle()}')")
                                        onSelectionChanged(index)
                                        scope.launch {
                                            scrollToIndexSafe(index)
                                        }
                                    }
                                }
                            }
                            .focusable(enabled = isRowSelected),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        itemContent(item, isSelected)
                    }
                }
            }
        }
    }
}

// Helpers for title/poster extraction:
fun Any.getTitle(): String = when (this) {
    is com.strmr.ai.data.database.MovieEntity       -> this.title
    is com.strmr.ai.data.database.TvShowEntity      -> this.title
    is com.strmr.ai.viewmodel.HomeMediaItem.Movie   -> this.movie.title
    is com.strmr.ai.viewmodel.HomeMediaItem.TvShow  -> this.show.title
    is com.strmr.ai.data.NetworkInfo                -> this.name
    else                                            -> "Unknown"
}

fun Any.getPosterUrl(): String? = when (this) {
    is com.strmr.ai.data.database.MovieEntity       -> this.posterUrl
    is com.strmr.ai.data.database.TvShowEntity      -> this.posterUrl
    is com.strmr.ai.viewmodel.HomeMediaItem.Movie   -> this.movie.posterUrl
    is com.strmr.ai.viewmodel.HomeMediaItem.TvShow  -> this.show.posterUrl
    is com.strmr.ai.data.NetworkInfo                -> this.posterUrl
    else                                            -> null
}
