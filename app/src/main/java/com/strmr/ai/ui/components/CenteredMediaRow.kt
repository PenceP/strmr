package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon

@Composable
fun <T> CenteredMediaRow(
    title: String,
    mediaItems: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onUpDown: ((Int) -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 16.dp,
    rowHeight: Dp = 200.dp,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    currentRowIndex: Int = 0,
    totalRowCount: Int = 1,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) where T : Any {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    // Debouncing variables to prevent rapid focus changes
    var lastFocusChangeTime by remember { mutableStateOf(0L) }
    val focusDebounceDelay = 50L // 50ms debounce

    // Centering logic (same as HomeMediaRow)
    val rowWidthDp = 900.dp
    val posterSpacingDp = itemSpacing
    val posterWidthDp = itemWidth
    val selectedPosterWidthDp = itemWidth * 1.2f // Larger when selected (matches MediaCard)
    val posterWidthPx = with(density) { posterWidthDp.roundToPx() }
    val selectedPosterWidthPx = with(density) { selectedPosterWidthDp.roundToPx() }
    val rowWidthPx = with(density) { rowWidthDp.roundToPx() }
    val posterSpacingPx = with(density) { posterSpacingDp.roundToPx() }
    
    // Calculate how many posters fit in the row
    val postersPerRow = (rowWidthPx - posterSpacingPx) / (posterWidthPx + posterSpacingPx)
    val centerIndex = (postersPerRow / 2).toInt()

    fun getOffsetForIndex(index: Int, totalItems: Int): Int {
        return when {
            index == 0 -> 0 // First poster: left-aligned
            index <= centerIndex -> {
                // Gradual centering: move toward center as we scroll right
                val progress = index.toFloat() / centerIndex.toFloat()
                val centerOffset = -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
                (centerOffset * progress).toInt()
            }
            else -> {
                // Locked to center for all other positions
                -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
            }
        }
    }

    // Request focus when this row becomes selected and is composed
    if (isRowSelected && focusRequester != null && isContentFocused) {
        LaunchedEffect(isRowSelected, isContentFocused) {
            focusRequester.requestFocus()
        }
    }

    // Initialize row position when focus is first given
    LaunchedEffect(isRowSelected, isContentFocused, selectedIndex) {
        if (isRowSelected && isContentFocused && mediaItems.isNotEmpty()) {
            Log.d("CenteredMediaRow", "🎯 Initializing row position: selectedIndex=$selectedIndex, items=${mediaItems.size}")
            // Ensure the selected item is properly positioned
            val offset = getOffsetForIndex(selectedIndex, mediaItems.size)
            listState.scrollToItem(selectedIndex, offset)
        }
    }

    // Synchronize scroll position with selection changes
    LaunchedEffect(selectedIndex) {
        if (isRowSelected && mediaItems.isNotEmpty()) {
            Log.d("CenteredMediaRow", "🔄 Syncing scroll position for selectedIndex=$selectedIndex")
            val offset = getOffsetForIndex(selectedIndex, mediaItems.size)
            listState.scrollToItem(selectedIndex, offset)
        }
    }

    // Monitor selected index for pagination
    LaunchedEffect(selectedIndex, mediaItems.size) {
        if (isRowSelected && mediaItems.size - selectedIndex <= 4) {
            Log.d("CenteredMediaRow", "📄 Near end of list for '$title' (${mediaItems.size - selectedIndex} items left), triggering load more")
            onLoadMore?.invoke()
        }
    }

    Column(modifier = modifier.padding(vertical = 1.dp)) {
        // Row title
        Text(
            text = title,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, bottom = 0.dp)
        )
        Spacer(Modifier.height(0.dp))
        
        // Larger container for row and navigation indicators
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight + 40.dp) // Extra space for arrows and scaling
        ) {
            // Row content centered in the container
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .align(Alignment.Center)
                    .onFocusChanged {
                        Log.d("CenteredMediaRow", "🎯 Focus changed for '$title': ${it.isFocused}")
                        onContentFocusChanged?.invoke(it.isFocused)
                    }
                    .focusRequester(focusRequester ?: FocusRequester())
                    .focusable(enabled = isRowSelected)
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && isRowSelected) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    if (selectedIndex > 0) {
                                        onSelectionChanged(selectedIndex - 1)
                                    } else {
                                        onLeftBoundary?.invoke()
                                    }
                                    true
                                }

                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (selectedIndex < mediaItems.size - 1) {
                                        onSelectionChanged(selectedIndex + 1)
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
                                        Log.d(
                                            "CenteredMediaRow",
                                            "🎯 Enter pressed on item $selectedIndex"
                                        )
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
                val isSelected = isRowSelected && index == selectedIndex && isContentFocused
                val dynamicWidth = if (isSelected) itemWidth * 1.2f else itemWidth

                Box(
                    modifier = Modifier
                        .width(dynamicWidth)
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
                                        Log.d("CenteredMediaRow", "Focused item $index")
                                        onSelectionChanged(index)
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
        
        // Up/down navigation indicators positioned in the container
        if (currentRowIndex > 0 && isRowSelected) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Up",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(32.dp)
            )
        }
        if (currentRowIndex < totalRowCount - 1 && isRowSelected) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Down",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(32.dp)
            )
        }
    }
    }
} 