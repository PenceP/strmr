package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Example showing how to use SimpleMediaRow with pagination support
 * This replaces the overengineered EnhancedUnifiedMediaRow approach
 */
@Composable
fun SimpleMediaRowExample(
    movies: List<Any>, // Your movie/content list
    isLoading: Boolean = false,
    hasMorePages: Boolean = true,
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Simple selection management
    val selectionManager = rememberSelectionManager()
    val focusRequester = remember { FocusRequester() }

    // Replace complex scroll state with simple approach
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier) {
        // Multiple rows example with pagination
        SimpleMediaRow(
            title = "Trending Movies",
            items = movies,
            selectedIndex = if (selectionManager.selectedRowIndex == 0) selectionManager.selectedItemIndex else 0,
            isRowSelected = selectionManager.selectedRowIndex == 0,
            onSelectionChanged = { index ->
                selectionManager.updateSelection(0, index)
            },
            onItemClick = { movie ->
                // Handle item click
            },
            onUpDown = { direction ->
                if (direction == 1 && selectionManager.selectedRowIndex < 2) { // Assuming 3 rows total
                    selectionManager.updateSelection(selectionManager.selectedRowIndex + 1, 0)
                } else if (direction == -1 && selectionManager.selectedRowIndex > 0) {
                    selectionManager.updateSelection(selectionManager.selectedRowIndex - 1, 0)
                }
            },
            focusRequester = if (selectionManager.selectedRowIndex == 0) focusRequester else null,
            isLoading = isLoading,
            hasMorePages = hasMorePages,
            onLoadMore = onLoadMore,
            itemContent = { movie, isSelected ->
                // Your movie card composable
                MovieCard(movie = movie, isSelected = isSelected)
            },
        )

        SimpleMediaRow(
            title = "Popular Movies",
            items = movies,
            selectedIndex = if (selectionManager.selectedRowIndex == 1) selectionManager.selectedItemIndex else 0,
            isRowSelected = selectionManager.selectedRowIndex == 1,
            onSelectionChanged = { index ->
                selectionManager.updateSelection(1, index)
            },
            onUpDown = { direction ->
                if (direction == 1 && selectionManager.selectedRowIndex < 2) {
                    selectionManager.updateSelection(selectionManager.selectedRowIndex + 1, 0)
                } else if (direction == -1 && selectionManager.selectedRowIndex > 0) {
                    selectionManager.updateSelection(selectionManager.selectedRowIndex - 1, 0)
                }
            },
            isLoading = isLoading,
            hasMorePages = hasMorePages,
            onLoadMore = onLoadMore,
            itemContent = { movie, isSelected ->
                MovieCard(movie = movie, isSelected = isSelected)
            },
        )
    }
}

@Composable
private fun MovieCard(
    movie: Any,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(120.dp, 180.dp)
                .padding(if (isSelected) 2.dp else 4.dp),
    ) {
        // Your movie card content here
        Text(
            text = "Movie Card",
            color = if (isSelected) Color.Yellow else Color.White,
        )
    }
}

/**
 * Enhanced migration guide with pagination support:
 *
 * FROM: EnhancedUnifiedMediaRow to SimpleMediaRow
 *
 * 1. Remove all ScrollState, ManageScrollStateUseCase, ScrollStateRepository
 * 2. Replace EnhancedUnifiedMediaRow with SimpleMediaRow
 * 3. Use simple SelectionManager instead of complex state management
 * 4. Use standard LazyRow (following Google's updated guidance)
 * 5. Use rememberSaveable for focus restoration across navigation
 * 6. Add pagination support with isLoading, hasMorePages, onLoadMore
 *
 * Pagination Features:
 * - Automatic loading trigger when near end (3 items from end)
 * - Loading indicator displayed at end of list during pagination
 * - Navigation blocked during pagination to prevent UI glitches
 * - Smooth transition when new items are loaded
 * - Focus maintained at current position during loading
 *
 * Benefits:
 * - Follows Android TV official patterns
 * - Much simpler and more maintainable
 * - Better performance (as seen in Google's own samples)
 * - Less code and complexity
 * - Easier to debug and understand
 */
