package com.strmr.ai.ui.components

import android.util.Log
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems

/**
 * Handles logging of paging load states
 * Single Responsibility: Load state monitoring and logging
 */
@Composable
fun <T : MediaItem> LoadStateLogger(
    lazyPagingItems: LazyPagingItems<T>,
    title: String,
    logTag: String
) {
    LaunchedEffect(lazyPagingItems.loadState) {
        when (val refresh = lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Log.d(logTag, "📥 Loading initial data for '$title'")
            }
            is LoadState.Error -> {
                Log.e(logTag, "❌ Error loading '$title'", refresh.error)
            }
            is LoadState.NotLoading -> {
                Log.d(logTag, "✅ Initial load complete for '$title', items: ${lazyPagingItems.itemCount}")
            }
        }
        
        when (val append = lazyPagingItems.loadState.append) {
            is LoadState.Loading -> {
                Log.d(logTag, "📥 Loading next page for '$title'")
            }
            is LoadState.Error -> {
                Log.e(logTag, "❌ Error loading next page for '$title'", append.error)
            }
            is LoadState.NotLoading -> {
                if (append.endOfPaginationReached) {
                    Log.d(logTag, "📄 No more pages for '$title'")
                }
            }
        }
    }
}