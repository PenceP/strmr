package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Enhanced MediaRow with loading state support
 */
@Composable
fun <T> EnhancedMediaRow(
    title: String,
    mediaItems: List<T>,
    selectedIndex: Int,
    isRowSelected: Boolean,
    onSelectionChanged: (Int) -> Unit,
    onUpDown: ((Int) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onItemClick: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 18.dp,
    isLoading: Boolean = false,
    loadingCardCount: Int = 8,
    skeletonCardType: SkeletonCardType = SkeletonCardType.PORTRAIT,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) where T : Any {
    
    if (isLoading) {
        // Show skeleton loading state
        MediaRowSkeleton(
            title = title,
            cardCount = loadingCardCount,
            itemWidth = itemWidth,
            itemSpacing = itemSpacing,
            cardType = skeletonCardType,
            modifier = modifier
        )
    } else {
        // Show actual content using original MediaRow
        MediaRow(
            title = title,
            mediaItems = mediaItems,
            selectedIndex = selectedIndex,
            isRowSelected = isRowSelected,
            onSelectionChanged = onSelectionChanged,
            onUpDown = onUpDown,
            onLoadMore = onLoadMore,
            onItemClick = onItemClick,
            modifier = modifier,
            itemWidth = itemWidth,
            itemSpacing = itemSpacing,
            itemContent = itemContent
        )
    }
}

/**
 * Simplified loading state for when you just need to show a skeleton
 */
@Composable
fun MediaRowWithLoading(
    title: String,
    isLoading: Boolean,
    cardCount: Int = 8,
    itemWidth: Dp = 120.dp,
    itemSpacing: Dp = 18.dp,
    cardType: SkeletonCardType = SkeletonCardType.PORTRAIT,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isLoading) {
        MediaRowSkeleton(
            title = title,
            cardCount = cardCount,
            itemWidth = itemWidth,
            itemSpacing = itemSpacing,
            cardType = cardType,
            modifier = modifier
        )
    } else {
        content()
    }
}