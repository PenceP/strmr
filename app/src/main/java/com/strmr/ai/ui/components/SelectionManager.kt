package com.strmr.ai.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay

/**
 * Centralized selection manager to prevent race conditions
 * and ensure consistent selection behavior across the app
 */
@Composable
fun rememberSelectionManager(
    initialRowIndex: Int = 0,
    initialItemIndex: Int = 0
): SelectionManager {
    return remember {
        SelectionManager(initialRowIndex, initialItemIndex)
    }
}

class SelectionManager(
    initialRowIndex: Int,
    initialItemIndex: Int
) {
    private var _selectedRowIndex by mutableStateOf(initialRowIndex)
    private var _selectedItemIndex by mutableStateOf(initialItemIndex)
    private var _isContentFocused by mutableStateOf(false)
    private var _isUpdating by mutableStateOf(false)
    
    val selectedRowIndex: Int get() = _selectedRowIndex
    val selectedItemIndex: Int get() = _selectedItemIndex
    val isContentFocused: Boolean get() = _isContentFocused
    
    /**
     * Update selection without triggering navigation
     * This should be used for focus changes and keyboard navigation
     */
    fun updateSelection(rowIndex: Int, itemIndex: Int) {
        if (!_isUpdating) {
            _isUpdating = true
            _selectedRowIndex = rowIndex
            _selectedItemIndex = itemIndex
            _isUpdating = false
        }
    }
    
    /**
     * Update content focus state
     */
    fun updateContentFocus(focused: Boolean) {
        _isContentFocused = focused
    }
    
    /**
     * Check if an item is currently selected
     */
    fun isItemSelected(rowIndex: Int, itemIndex: Int): Boolean {
        return _selectedRowIndex == rowIndex && _selectedItemIndex == itemIndex && _isContentFocused
    }
    
    /**
     * Check if a row is currently selected
     */
    fun isRowSelected(rowIndex: Int): Boolean {
        return _selectedRowIndex == rowIndex && _isContentFocused
    }
    
    /**
     * Reset selection to initial state
     */
    fun reset() {
        _selectedRowIndex = 0
        _selectedItemIndex = 0
        _isContentFocused = false
    }
}

/**
 * Composable wrapper for selection state
 */
@Composable
fun SelectionState(
    selectionManager: SelectionManager,
    content: @Composable () -> Unit
) {
    content()
} 