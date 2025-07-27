package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Refactored PagingMediaRow with better separation of concerns
 * 
 * Key improvements:
 * - Reduced from 15+ parameters to a single config object
 * - Extracted key event handling to separate class
 * - Separated load state logging
 * - Isolated media row rendering logic
 * - Better testability through smaller components
 */
@Composable
fun <T : MediaItem> RefactoredPagingMediaRow(
    pagingFlow: Flow<PagingData<T>>,
    config: PagingMediaRowConfig<T>,
    modifier: Modifier = Modifier
) {
    val lazyPagingItems = pagingFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Handle scroll synchronization
    ScrollSyncHandler(
        selectedIndex = config.selectedIndex,
        itemCount = lazyPagingItems.itemCount,
        listState = listState
    )
    
    // Handle position reporting
    PositionReporter(
        selectedIndex = config.selectedIndex,
        itemCount = lazyPagingItems.itemCount,
        isRowSelected = config.isRowSelected,
        onPositionChanged = config.onPositionChanged
    )
    
    // Handle load state logging
    LoadStateLogger(
        lazyPagingItems = lazyPagingItems,
        title = config.title,
        logTag = config.logTag
    )
    
    Box(modifier = modifier.fillMaxWidth()) {
        Column {
            // Title
            Text(
                text = config.title,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
            
            // Media items row
            MediaRowContent(
                config = config,
                lazyPagingItems = lazyPagingItems,
                listState = listState,
                coroutineScope = coroutineScope
            )
        }
    }
}

@Composable
private fun ScrollSyncHandler(
    selectedIndex: Int,
    itemCount: Int,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LaunchedEffect(selectedIndex, itemCount) {
        if (selectedIndex >= 0 && selectedIndex < itemCount) {
            listState.animateScrollToItem(selectedIndex)
        }
    }
}

@Composable
private fun PositionReporter(
    selectedIndex: Int,
    itemCount: Int,
    isRowSelected: Boolean,
    onPositionChanged: ((Int, Int) -> Unit)?
) {
    LaunchedEffect(selectedIndex, itemCount) {
        if (isRowSelected && itemCount > 0) {
            onPositionChanged?.invoke(selectedIndex, itemCount)
        }
    }
}

/**
 * Maintains backward compatibility with the original PagingMediaRow API
 * All existing usage should work without changes
 */
@Composable
fun <T : MediaItem> PagingMediaRow(
    title: String,
    pagingFlow: Flow<PagingData<T>>,
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
    logTag: String = "PagingMediaRow"
) {
    val config = PagingMediaRowConfig(
        title = title,
        selectedIndex = selectedIndex,
        isRowSelected = isRowSelected,
        onSelectionChanged = onSelectionChanged,
        focusRequester = focusRequester,
        onUpDown = onUpDown,
        isContentFocused = isContentFocused,
        onContentFocusChanged = onContentFocusChanged,
        currentRowIndex = currentRowIndex,
        totalRowCount = totalRowCount,
        onItemClick = onItemClick,
        onPositionChanged = onPositionChanged,
        logTag = logTag,
        onLeftBoundary = onLeftBoundary
    )
    
    RefactoredPagingMediaRow(
        pagingFlow = pagingFlow,
        config = config,
        modifier = modifier
    )
}