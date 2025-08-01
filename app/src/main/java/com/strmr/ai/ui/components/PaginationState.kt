package com.strmr.ai.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.tv.foundation.lazy.list.TvLazyListState

/**
 * Flixclusive-style pagination state management
 * Replaces complex Paging3 library with simple, predictable custom pagination
 */

/**
 * Custom pagination states matching Flixclusive's approach
 */
enum class PagingState {
    LOADING, // Initial load
    ERROR, // Failed to load
    PAGINATING, // Loading more items
    PAGINATING_EXHAUST, // No more items to load
    IDLE, // Ready to paginate
}

/**
 * Pagination state info wrapper
 */
data class PaginationStateInfo(
    val canPaginate: Boolean,
    val pagingState: PagingState,
    val currentPage: Int,
)

/**
 * Flixclusive-style pagination trigger extensions
 */

/**
 * TV version - triggers pagination when within buffer distance of end
 */
fun TvLazyListState.shouldPaginate(toDeduct: Int = 6): Boolean =
    (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -9) >=
        (layoutInfo.totalItemsCount - toDeduct)

/**
 * Mobile/Standard version - more aggressive with scroll forward check
 */
fun LazyListState.shouldPaginate(toDeduct: Int = 6): Boolean {
    val layoutInfo = this.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val totalItems = layoutInfo.totalItemsCount

    // Don't paginate if there are no items or if we're not at the end
    if (totalItems == 0 || visibleItems.isEmpty()) return false

    val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: return false
    val shouldPaginate = lastVisibleIndex >= (totalItems - toDeduct)

    return shouldPaginate && canScrollForward
}
