package com.strmr.ai.ui.components

import androidx.compose.ui.focus.FocusRequester

/**
 * Configuration data class for PagingMediaRow to reduce parameter count
 * Follows the Parameter Object pattern to improve readability
 */
data class PagingMediaRowConfig<T : MediaItem>(
    val title: String,
    val selectedIndex: Int,
    val isRowSelected: Boolean,
    val onSelectionChanged: (Int) -> Unit,
    val focusRequester: FocusRequester? = null,
    val onUpDown: ((Int) -> Unit)? = null,
    val isContentFocused: Boolean = false,
    val onContentFocusChanged: ((Boolean) -> Unit)? = null,
    val currentRowIndex: Int = 0,
    val totalRowCount: Int = 1,
    val onItemClick: ((T) -> Unit)? = null,
    val onPositionChanged: ((Int, Int) -> Unit)? = null,
    val logTag: String = "PagingMediaRow"
)