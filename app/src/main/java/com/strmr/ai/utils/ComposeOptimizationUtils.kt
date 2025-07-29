package com.strmr.ai.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import kotlinx.coroutines.CoroutineScope

/**
 * Utility functions and composables for Compose performance optimization
 * Focus on proper remember usage, derivedStateOf, and expensive operation handling
 */
object ComposeOptimizationUtils {
    
    private const val TAG = "ComposeOptimization"
    
    /**
     * Optimized way to remember expensive calculations with proper key tracking
     */
    @Composable
    fun <T> rememberExpensiveCalculation(
        vararg keys: Any?,
        calculation: () -> T
    ): T {
        return remember(*keys) {
            Log.d(TAG, "🧮 Computing expensive calculation with keys: ${keys.contentToString()}")
            calculation()
        }
    }
    
    /**
     * Remember a derived state that depends on other states but is expensive to compute
     */
    @Composable
    fun <T> rememberDerivedState(
        vararg keys: Any?,
        calculation: () -> T
    ): State<T> {
        return remember(*keys) {
            derivedStateOf {
                Log.v(TAG, "🔄 Recomputing derived state")
                calculation()
            }
        }
    }
    
    /**
     * Optimized list transformation that only recomputes when source data changes
     */
    @Composable
    fun <T, R> rememberTransformedList(
        sourceList: List<T>,
        transform: (T) -> R
    ): List<R> {
        return remember(sourceList) {
            Log.d(TAG, "📝 Transforming list of ${sourceList.size} items")
            sourceList.map(transform)
        }
    }
    
    /**
     * Remember a grouped/filtered list that only recomputes when source changes
     */
    @Composable
    fun <T> rememberFilteredList(
        sourceList: List<T>,
        predicate: (T) -> Boolean
    ): List<T> {
        return remember(sourceList, predicate) {
            Log.d(TAG, "🔍 Filtering list of ${sourceList.size} items")
            sourceList.filter(predicate)
        }
    }
    
    /**
     * Remember a grouped list that only recomputes when source changes
     */
    @Composable
    fun <T, K> rememberGroupedList(
        sourceList: List<T>,
        keySelector: (T) -> K
    ): Map<K, List<T>> {
        return remember(sourceList, keySelector) {
            Log.d(TAG, "📊 Grouping list of ${sourceList.size} items")
            sourceList.groupBy(keySelector)
        }
    }
    
    /**
     * Remember a sorted list that only recomputes when source changes
     */
    @Composable
    fun <T> rememberSortedList(
        sourceList: List<T>,
        comparator: Comparator<T>
    ): List<T> {
        return remember(sourceList, comparator) {
            Log.d(TAG, "📈 Sorting list of ${sourceList.size} items")
            sourceList.sortedWith(comparator)
        }
    }
    
    /**
     * Remember Focus requesters for a dynamic list
     */
    @Composable
    fun rememberFocusRequesters(count: Int): List<androidx.compose.ui.focus.FocusRequester> {
        return remember(count) {
            Log.d(TAG, "🎯 Creating $count focus requesters")
            List(count) { androidx.compose.ui.focus.FocusRequester() }
        }
    }
    
    /**
     * Remember a mutable map that only recreates when keys change
     */
    @Composable
    fun <K, V> rememberMutableMap(
        vararg dependencies: Any?
    ): MutableMap<K, V> {
        return remember(*dependencies) {
            Log.d(TAG, "🗺️ Creating new mutable map")
            mutableMapOf()
        }
    }
    
    /**
     * Optimized image URL processing that only recomputes when needed
     */
    @Composable
    fun rememberOptimizedImageUrl(
        baseUrl: String?,
        width: Int? = null,
        height: Int? = null
    ): String? {
        return remember(baseUrl, width, height) {
            if (baseUrl.isNullOrBlank()) {
                null
            } else {
                // Process image URL with dimensions if provided
                when {
                    width != null && height != null -> "${baseUrl}?w=${width}&h=${height}"
                    width != null -> "${baseUrl}?w=${width}"
                    height != null -> "${baseUrl}?h=${height}"
                    else -> baseUrl
                }
            }
        }
    }
    
    /**
     * Remember computed display text that only updates when dependencies change
     */
    @Composable
    fun rememberDisplayText(
        title: String?,
        year: Int? = null,
        fallback: String = "Unknown"
    ): String {
        return remember(title, year, fallback) {
            when {
                title.isNullOrBlank() -> fallback
                year != null -> "$title ($year)"
                else -> title
            }
        }
    }
}

/**
 * Performance-optimized LaunchedEffect that only executes when specific keys change
 */
@Composable
fun OptimizedLaunchedEffect(
    vararg keys: Any?,
    operation: String,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(*keys) {
        Log.d("ComposeOptimization", "🚀 Executing LaunchedEffect: $operation")
        block()
    }
}

/**
 * Disposable effect with enhanced logging for debugging
 */
@Composable
fun OptimizedDisposableEffect(
    vararg keys: Any?,
    operation: String,
    effect: DisposableEffectScope.() -> DisposableEffectResult
) {
    DisposableEffect(*keys) {
        Log.d("ComposeOptimization", "📌 Creating DisposableEffect: $operation")
        val result = effect()
        onDispose {
            Log.d("ComposeOptimization", "🧹 Disposing DisposableEffect: $operation")
            result.dispose()
        }
    }
}

/**
 * Extension function to create stable keys for remember operations
 */
fun createStableKey(vararg components: Any?): String {
    return components.joinToString("_") { it?.toString() ?: "null" }
}