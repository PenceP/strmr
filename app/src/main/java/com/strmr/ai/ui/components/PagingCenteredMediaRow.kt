package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.paging.compose.LazyPagingItems
import kotlinx.coroutines.launch

@Composable
fun <T : Any> PagingCenteredMediaRow(
    title: String,
    items: LazyPagingItems<T>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onUpDown: ((Int) -> Unit)? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 16.dp,
    rowHeight: Dp = 200.dp
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Centering logic (same as CenteredMediaRow)
    val rowWidthDp = 900.dp
    val posterSpacingDp = itemSpacing
    val posterWidthDp = itemWidth
    val selectedPosterWidthDp = itemWidth * 1.2f
    val posterWidthPx = with(density) { posterWidthDp.roundToPx() }
    val selectedPosterWidthPx = with(density) { selectedPosterWidthDp.roundToPx() }
    val rowWidthPx = with(density) { rowWidthDp.roundToPx() }
    val posterSpacingPx = with(density) { posterSpacingDp.roundToPx() }
    
    val postersPerRow = (rowWidthPx - posterSpacingPx) / (posterWidthPx + posterSpacingPx)
    val centerIndex = (postersPerRow / 2).toInt()

    fun getOffsetForIndex(index: Int, totalItems: Int): Int {
        return when {
            index == 0 -> 0
            index <= centerIndex -> {
                val progress = index.toFloat() / centerIndex.toFloat()
                val centerOffset = -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
                (centerOffset * progress).toInt()
            }
            else -> {
                -(rowWidthPx / 2 - selectedPosterWidthPx / 2)
            }
        }
    }

    // Request focus when this row becomes selected
    if (focusRequester != null && isContentFocused) {
        LaunchedEffect(isContentFocused) {
            focusRequester.requestFocus()
        }
    }

    // Synchronize scroll position with selection changes
    LaunchedEffect(selectedIndex) {
        if (isContentFocused && items.itemCount > 0) {
            Log.d("PagingCenteredMediaRow", "🔄 Syncing scroll position for selectedIndex=$selectedIndex")
            val offset = getOffsetForIndex(selectedIndex, items.itemCount)
            listState.scrollToItem(selectedIndex, offset)
        }
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Row title
        Text(
            text = title,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .onFocusChanged { 
                    Log.d("PagingCenteredMediaRow", "🎯 Focus changed for '$title': ${it.isFocused}")
                    onContentFocusChanged?.invoke(it.isFocused) 
                }
                .focusRequester(focusRequester ?: FocusRequester())
                .focusable(enabled = isContentFocused)
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN && isContentFocused) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (selectedIndex > 0) {
                                    onSelectionChanged(selectedIndex - 1)
                                    true
                                } else {
                                    false
                                }
                            }
                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (selectedIndex < items.itemCount - 1) {
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
                                val item = items[selectedIndex]
                                if (item != null) {
                                    Log.d("PagingCenteredMediaRow", "🎯 Enter pressed on item $selectedIndex")
                                    onItemClick?.invoke(item)
                                }
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            items(items.itemCount) { index ->
                val item = items[index]
                if (item != null) {
                    val isSelected = index == selectedIndex && isContentFocused
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
                                    if (fs.isFocused && isContentFocused) {
                                        Log.d("PagingCenteredMediaRow", "Focused item $index")
                                        onItemSelected(index)
                                    }
                                }
                                .focusable(enabled = isContentFocused),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            MediaCard(
                                title = item.getTitle(),
                                posterUrl = item.getPosterUrl(),
                                isSelected = isSelected,
                                onClick = { onItemClick?.invoke(item) }
                            )
                        }
                    }
                }
            }
        }
    }
} 