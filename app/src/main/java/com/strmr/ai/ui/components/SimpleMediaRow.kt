package com.strmr.ai.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp

/**
 * Simple, standardized media row following Android TV best practices
 * Uses standard LazyRow with simple focus management and graceful pagination
 */
@Composable
fun <T : Any> SimpleMediaRow(
    title: String,
    items: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onItemClick: ((T) -> Unit)? = null,
    onUpDown: ((Int) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    itemWidth: androidx.compose.ui.unit.Dp = 120.dp,
    itemSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 56.dp),
    isLoading: Boolean = false,
    hasMorePages: Boolean = false,
    onLoadMore: (() -> Unit)? = null,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Track when we're near the end for pagination
    var isNearEnd by remember { mutableStateOf(false) }
    var isPaginationInProgress by remember { mutableStateOf(false) }

    // Auto-scroll to selected item when row becomes active, but not during pagination
    LaunchedEffect(selectedIndex, isRowSelected) {
        if (isRowSelected && selectedIndex in items.indices && !isPaginationInProgress) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    // Handle pagination trigger - check if we're near the end
    LaunchedEffect(selectedIndex, items.size, isLoading) {
        if (items.isNotEmpty() && hasMorePages && !isLoading) {
            val nearEndThreshold = 3 // Trigger when 3 items from end
            isNearEnd = selectedIndex >= (items.size - nearEndThreshold)

            if (isNearEnd && !isPaginationInProgress) {
                isPaginationInProgress = true
                onLoadMore?.invoke()
            }
        }

        // Reset pagination state when loading completes
        if (!isLoading && isPaginationInProgress) {
            isPaginationInProgress = false
        }
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Row title
        if (title.isNotBlank()) {
            Text(
                text = title,
                color = Color.White,
                modifier = Modifier.padding(start = 56.dp, bottom = 4.dp)
            )
        }

        // Media row
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            contentPadding = contentPadding,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp) // Standard TV row height
                .then(
                    if (isRowSelected && focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .focusable(enabled = isRowSelected)
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                        isRowSelected &&
                        !isPaginationInProgress // Prevent navigation during pagination
                    ) {
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
                                // Don't navigate past current items during loading
                                val maxIndex = if (isPaginationInProgress) {
                                    items.size - 1 // Stop at last available item
                                } else {
                                    items.size - 1
                                }

                                if (selectedIndex < maxIndex) {
                                    onSelectionChanged(selectedIndex + 1)
                                }
                                true
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
                                items.getOrNull(selectedIndex)?.let { item ->
                                    onItemClick?.invoke(item)
                                }
                                true
                            }

                            else -> false
                        }
                    } else false
                }
        ) {
            itemsIndexed(
                items = items,
                key = { index, _ -> "item_$index" }
            ) { index, item ->
                SimpleMediaRowItem(
                    item = item,
                    index = index,
                    isSelected = isRowSelected && index == selectedIndex && !isPaginationInProgress,
                    itemWidth = itemWidth,
                    onFocusChanged = { focused ->
                        if (focused && isRowSelected && index != selectedIndex && !isPaginationInProgress) {
                            onSelectionChanged(index)
                        }
                    },
                    itemContent = itemContent
                )
            }

            // Show loading indicator if we have more pages to load
            if (hasMorePages && (isLoading || isPaginationInProgress)) {
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Loading...",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Add trailing spacers for smooth scrolling (only when not loading)
            if (!isPaginationInProgress) {
                repeat(3) {
                    item(key = "spacer_$it") {
                        Spacer(modifier = Modifier.width(itemWidth + itemSpacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun <T : Any> SimpleMediaRowItem(
    item: T,
    index: Int,
    isSelected: Boolean,
    itemWidth: androidx.compose.ui.unit.Dp,
    onFocusChanged: (Boolean) -> Unit,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(itemWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .focusable(),
            contentAlignment = Alignment.BottomCenter
        ) {
            itemContent(item, isSelected)
        }
    }
}