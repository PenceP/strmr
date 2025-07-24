package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.key.KeyEvent
import androidx.paging.compose.LazyPagingItems
import com.strmr.ai.ui.theme.StrmrConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles key events for PagingMediaRow navigation
 * Single Responsibility: Key event processing and navigation logic
 */
class KeyEventHandler<T : MediaItem>(
    private val config: PagingMediaRowConfig<T>,
    private val lazyPagingItems: LazyPagingItems<T>,
    private val listState: LazyListState,
    private val coroutineScope: CoroutineScope
) {
    
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN || !config.isRowSelected) {
            return false
        }
        
        return when (event.nativeKeyEvent.keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> handleLeftNavigation()
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> handleRightNavigation()
            android.view.KeyEvent.KEYCODE_DPAD_UP -> handleUpNavigation()
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> handleDownNavigation()
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> handleSelection()
            else -> false
        }
    }
    
    private fun handleLeftNavigation(): Boolean {
        if (config.selectedIndex <= 0) return false
        
        val newIndex = config.selectedIndex - 1
        updateSelection(newIndex)
        triggerProactiveLoadingIfNeeded(newIndex)
        return true
    }
    
    private fun handleRightNavigation(): Boolean {
        if (config.selectedIndex >= lazyPagingItems.itemCount - 1) return false
        
        val newIndex = config.selectedIndex + 1
        updateSelection(newIndex)
        logNavigationState(newIndex)
        triggerProactiveLoadingIfNeeded(newIndex)
        return true
    }
    
    private fun handleUpNavigation(): Boolean {
        config.onUpDown?.invoke(-1)
        return true
    }
    
    private fun handleDownNavigation(): Boolean {
        config.onUpDown?.invoke(1)
        return true
    }
    
    private fun handleSelection(): Boolean {
        lazyPagingItems[config.selectedIndex]?.let { item ->
            config.onItemClick?.invoke(item)
        }
        return true
    }
    
    private fun updateSelection(newIndex: Int) {
        config.onSelectionChanged(newIndex)
        
        coroutineScope.launch {
            listState.animateScrollToItem(newIndex)
        }
        
        config.onPositionChanged?.invoke(newIndex, lazyPagingItems.itemCount)
    }
    
    private fun logNavigationState(newIndex: Int) {
        val remainingItems = lazyPagingItems.itemCount - newIndex - 1
        val item = lazyPagingItems[newIndex]
        Log.d(config.logTag, "📊 Row '${config.title}' - Position: ${newIndex + 1}/${lazyPagingItems.itemCount}, Remaining: $remainingItems")
        Log.d(config.logTag, "   Selected: ${item?.title}")
    }
    
    private fun triggerProactiveLoadingIfNeeded(currentIndex: Int) {
        val numLoadedItems = lazyPagingItems.itemCount
        if (currentIndex + StrmrConstants.Paging.LOAD_AHEAD_THRESHOLD >= numLoadedItems) {
            Log.d(config.logTag, "🚀 Proactive load trigger: currentIdx($currentIndex) + ${StrmrConstants.Paging.LOAD_AHEAD_THRESHOLD} >= numLoadedItems($numLoadedItems)")
            
            // Force Paging3 to load more by accessing an item near the end
            val triggerIndex = minOf(currentIndex + StrmrConstants.Paging.TRIGGER_OFFSET, numLoadedItems - 1)
            lazyPagingItems[triggerIndex] // This access triggers Paging3 to load more
        }
    }
}