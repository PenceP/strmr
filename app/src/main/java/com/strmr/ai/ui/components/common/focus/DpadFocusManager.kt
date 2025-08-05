package com.strmr.ai.ui.components.common.focus

import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DpadFocusManager handles focus state management for Android TV D-pad navigation.
 * 
 * This manager provides:
 * - Focus memory and restoration
 * - Focus state tracking across rows and items
 * - Integration with DpadRecyclerView
 * - Throttled focus changes for performance
 * 
 * Key features:
 * - Remembers last focused item per row
 * - Handles focus restoration when returning to screens
 * - Provides focus state observables for UI updates
 * - Integrates with NavigationThrottle for smooth navigation
 */
class DpadFocusManager {
    
    // Internal state for focus tracking
    private val _currentFocusedItem = MutableStateFlow<FocusedItem?>(null)
    private val _focusHistory = mutableMapOf<String, FocusedItem>()
    
    // Public state observables
    val currentFocusedItem: StateFlow<FocusedItem?> = _currentFocusedItem.asStateFlow()
    
    /**
     * Updates the currently focused item and stores it in history
     */
    fun updateFocus(rowId: String, itemIndex: Int, itemId: Int) {
        val focusedItem = FocusedItem(
            rowId = rowId,
            itemIndex = itemIndex,
            itemId = itemId,
            timestamp = System.currentTimeMillis()
        )
        
        _currentFocusedItem.value = focusedItem
        _focusHistory[rowId] = focusedItem
    }
    
    /**
     * Gets the last focused item for a specific row
     */
    fun getLastFocusedItem(rowId: String): FocusedItem? {
        return _focusHistory[rowId]
    }
    
    /**
     * Gets the last focused item index for a specific row
     */
    fun getLastFocusedIndex(rowId: String): Int {
        return _focusHistory[rowId]?.itemIndex ?: 0
    }
    
    /**
     * Clears focus for a specific row
     */
    fun clearRowFocus(rowId: String) {
        _focusHistory.remove(rowId)
        
        // Clear current focus if it belongs to this row
        _currentFocusedItem.value?.let { current ->
            if (current.rowId == rowId) {
                _currentFocusedItem.value = null
            }
        }
    }
    
    /**
     * Clears all focus history
     */
    fun clearAllFocus() {
        _focusHistory.clear()
        _currentFocusedItem.value = null
    }
    
    /**
     * Gets all rows that have focus history
     */
    fun getRowsWithFocusHistory(): Set<String> {
        return _focusHistory.keys.toSet()
    }
    
    /**
     * Checks if a specific row has focus history
     */
    fun hasRowFocusHistory(rowId: String): Boolean {
        return _focusHistory.containsKey(rowId)
    }
    
    /**
     * Gets the number of items in focus history
     */
    fun getFocusHistorySize(): Int {
        return _focusHistory.size
    }
    
    /**
     * Restores focus to the last focused item in a row if it exists
     */
    fun restoreFocusToRow(rowId: String): FocusedItem? {
        val lastFocused = _focusHistory[rowId]
        if (lastFocused != null) {
            _currentFocusedItem.value = lastFocused.copy(
                timestamp = System.currentTimeMillis()
            )
        }
        return lastFocused
    }
    
    /**
     * Creates a snapshot of current focus state for restoration
     */
    fun createFocusSnapshot(): FocusSnapshot {
        return FocusSnapshot(
            currentFocus = _currentFocusedItem.value,
            focusHistory = _focusHistory.toMap(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Restores focus state from a snapshot
     */
    fun restoreFromSnapshot(snapshot: FocusSnapshot) {
        _currentFocusedItem.value = snapshot.currentFocus
        _focusHistory.clear()
        _focusHistory.putAll(snapshot.focusHistory)
    }
}

/**
 * Data class representing a focused item
 */
data class FocusedItem(
    val rowId: String,
    val itemIndex: Int,
    val itemId: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data class representing a focus state snapshot
 */
data class FocusSnapshot(
    val currentFocus: FocusedItem?,
    val focusHistory: Map<String, FocusedItem>,
    val timestamp: Long
)

/**
 * Composable that provides a DpadFocusManager to the composition tree
 */
@Composable
fun rememberDpadFocusManager(): DpadFocusManager {
    return remember { DpadFocusManager() }
}

/**
 * Focus state for individual items within rows
 */
@Stable
class ItemFocusState(
    val focusRequester: FocusRequester = FocusRequester(),
    private val onFocusChanged: (Boolean) -> Unit = {}
) {
    private var _isFocused by mutableStateOf(false)
    
    val isFocused: Boolean get() = _isFocused
    
    fun updateFocus(focused: Boolean) {
        if (_isFocused != focused) {
            _isFocused = focused
            onFocusChanged(focused)
        }
    }
    
    fun requestFocus() {
        focusRequester.requestFocus()
    }
}

/**
 * Factory for creating ItemFocusState instances
 */
@Composable
fun rememberItemFocusState(
    onFocusChanged: (Boolean) -> Unit = {}
): ItemFocusState {
    return remember {
        ItemFocusState(onFocusChanged = onFocusChanged)
    }
}

/**
 * Helper class for managing focus across multiple rows
 */
class RowFocusCoordinator(
    private val focusManager: DpadFocusManager
) {
    private val rowFocusStates = mutableMapOf<String, ItemFocusState>()
    
    fun getRowFocusState(rowId: String): ItemFocusState {
        return rowFocusStates.getOrPut(rowId) {
            ItemFocusState { focused ->
                if (focused) {
                    // When a row gains focus, try to restore the last focused item
                    focusManager.restoreFocusToRow(rowId)
                }
            }
        }
    }
    
    fun clearRowFocusState(rowId: String) {
        rowFocusStates.remove(rowId)
        focusManager.clearRowFocus(rowId)
    }
    
    fun clearAllRowFocusStates() {
        rowFocusStates.clear()
        focusManager.clearAllFocus()
    }
}

/**
 * Composable that provides a RowFocusCoordinator
 */
@Composable
fun rememberRowFocusCoordinator(
    focusManager: DpadFocusManager = rememberDpadFocusManager()
): RowFocusCoordinator {
    return remember(focusManager) {
        RowFocusCoordinator(focusManager)
    }
}