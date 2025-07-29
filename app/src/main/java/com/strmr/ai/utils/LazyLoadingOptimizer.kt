package com.strmr.ai.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Lazy loading optimizer for heavy UI components
 * Provides utilities to optimize memory usage and performance for LazyRow/LazyColumn
 */
object LazyLoadingOptimizer {
    
    // Default values optimized for Android TV
    const val DEFAULT_PREFETCH_DISTANCE = 3
    const val DEFAULT_VISIBLE_THRESHOLD = 5
    val DEFAULT_ITEM_SPACING = 16.dp
    
    /**
     * Optimized LazyListState configuration for streaming content
     */
    @Composable
    fun rememberOptimizedLazyListState(
        initialFirstVisibleItemIndex: Int = 0,
        initialFirstVisibleItemScrollOffset: Int = 0,
        prefetchStrategy: PrefetchStrategy = PrefetchStrategy.Conservative
    ): LazyListState {
        return androidx.compose.foundation.lazy.rememberLazyListState(
            initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset
        )
    }
    
    /**
     * Monitor visible items to optimize loading/unloading
     */
    @Composable
    fun rememberVisibleItemsObserver(
        listState: LazyListState,
        totalItemCount: Int,
        onVisibleRangeChanged: (IntRange) -> Unit = {}
    ): VisibleItemsInfo {
        return remember(listState, totalItemCount) {
            VisibleItemsInfo(listState, totalItemCount, onVisibleRangeChanged)
        }
    }
    
    /**
     * Determines if an item should be rendered based on its position
     * Uses viewport-based culling for memory optimization
     */
    @Composable
    fun shouldRenderItem(
        itemIndex: Int,
        listState: LazyListState,
        bufferSize: Int = DEFAULT_VISIBLE_THRESHOLD
    ): Boolean {
        val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
        if (visibleItemsInfo.isEmpty()) return itemIndex < DEFAULT_VISIBLE_THRESHOLD
        
        val firstVisible = visibleItemsInfo.first().index
        val lastVisible = visibleItemsInfo.last().index
        
        return itemIndex in (firstVisible - bufferSize)..(lastVisible + bufferSize)
    }
    
    /**
     * Optimized content padding for better scrolling performance
     */
    fun getOptimizedContentPadding(
        itemWidth: Dp,
        screenWidth: Dp = 1920.dp // Android TV standard
    ): PaddingValues {
        val horizontalPadding = maxOf(48.dp, (screenWidth - itemWidth * 3) / 8)
        return PaddingValues(
            horizontal = horizontalPadding,
            vertical = 24.dp
        )
    }
}

/**
 * Prefetch strategies for different content types
 */
enum class PrefetchStrategy {
    Conservative,  // Minimal prefetching for memory-constrained scenarios
    Balanced,      // Default strategy for most content
    Aggressive     // Maximum prefetching for smooth scrolling (use with caution)
}

/**
 * Information about visible items in a lazy list
 */
class VisibleItemsInfo(
    private val listState: LazyListState,
    private val totalItemCount: Int,
    private val onVisibleRangeChanged: (IntRange) -> Unit
) {
    @Composable
    fun observeVisibleRange(): IntRange {
        val visibleRange by produceState(
            initialValue = 0..0,
            key1 = listState
        ) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                .map { visibleItems ->
                    if (visibleItems.isEmpty()) {
                        0..0
                    } else {
                        visibleItems.first().index..visibleItems.last().index
                    }
                }
                .distinctUntilChanged()
                .collect { range ->
                    value = range
                    onVisibleRangeChanged(range)
                }
        }
        return visibleRange
    }
    
    @Composable
    fun getVisibleItemsCount(): Int {
        return remember(listState) {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.size
            }
        }.value
    }
    
    @Composable
    fun isNearEnd(threshold: Int = 3): Boolean {
        return remember(listState, totalItemCount) {
            derivedStateOf {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItemCount - lastVisible <= threshold
            }
        }.value
    }
}

/**
 * Optimized key generation for LazyRow/LazyColumn items
 * Helps with recomposition optimization
 */
object LazyItemKeyOptimizer {
    
    /**
     * Generate stable keys for media items
     */
    fun generateMediaKey(mediaId: String, mediaType: String): String {
        return "${mediaType}_${mediaId}"
    }
    
    /**
     * Generate keys for loading states
     */
    fun generateLoadingKey(index: Int): String {
        return "loading_$index"
    }
    
    /**
     * Generate keys for error states
     */
    fun generateErrorKey(errorType: String): String {
        return "error_$errorType"
    }
}

/**
 * Memory optimization utilities for heavy UI components
 */
object LazyMemoryOptimizer {
    
    /**
     * Calculate optimal number of items to preload based on available memory
     */
    fun calculateOptimalPreloadCount(
        itemSizeBytes: Long,
        availableMemoryMB: Long = 50 // Conservative estimate
    ): Int {
        val availableBytes = availableMemoryMB * 1024 * 1024
        val optimalCount = (availableBytes / itemSizeBytes).toInt()
        return optimalCount.coerceIn(3, 20) // Reasonable bounds
    }
    
    /**
     * Memory-conscious item disposal strategy
     */
    @Composable
    fun rememberItemDisposal(
        currentVisibleRange: IntRange,
        totalItems: Int,
        disposeThreshold: Int = 20
    ): Set<Int> {
        return remember(currentVisibleRange, totalItems) {
            val itemsToDispose = mutableSetOf<Int>()
            
            // Dispose items far from current viewport
            for (i in 0 until totalItems) {
                val distanceFromVisible = minOf(
                    kotlin.math.abs(i - currentVisibleRange.first),
                    kotlin.math.abs(i - currentVisibleRange.last)
                )
                
                if (distanceFromVisible > disposeThreshold) {
                    itemsToDispose.add(i)
                }
            }
            
            itemsToDispose
        }
    }
}