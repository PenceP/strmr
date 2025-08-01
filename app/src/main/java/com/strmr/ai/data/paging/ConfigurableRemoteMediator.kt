package com.strmr.ai.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.ui.theme.StrmrConstants

/**
 * A RemoteMediator that implements row-focused pagination.
 * Key features:
 * - Loads page 1 when cache is empty
 * - Only loads next pages when the row is focused
 * - Relies on PagingConfig prefetchDistance for load triggering
 * - Implements staggered refresh for cached data
 */
@OptIn(ExperimentalPagingApi::class)
class ConfigurableRemoteMediator<T : Any>(
    private val config: DataSourceConfig,
    private val database: StrmrDatabase,
    private val genericRepository: GenericTraktRepository,
    private val isMovie: Boolean,
    private val isRowFocused: () -> Boolean, // Function to check if this row has focus
    private val getCurrentPosition: () -> Int = { 0 }, // Function to get current position in row (for logging only)
    private val getTotalItems: () -> Int = { 0 }, // Function to get total items in row (for logging only)
) : RemoteMediator<Int, T>() {
    override suspend fun initialize(): InitializeAction {
        // Check if data source is empty
        val isEmpty =
            if (isMovie) {
                genericRepository.isMovieDataSourceEmpty(config)
            } else {
                genericRepository.isTvDataSourceEmpty(config)
            }

        return if (isEmpty) {
            Log.d("ConfigurableRemoteMediator", "📥 Data source ${config.title} is empty, will load page 1")
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            Log.d("ConfigurableRemoteMediator", "✅ Data source ${config.title} has cached data")
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, T>,
    ): MediatorResult {
        return try {
            when (loadType) {
                LoadType.REFRESH -> {
                    // Only refresh if cache is empty
                    val isEmpty =
                        if (isMovie) {
                            genericRepository.isMovieDataSourceEmpty(config)
                        } else {
                            genericRepository.isTvDataSourceEmpty(config)
                        }

                    if (isEmpty) {
                        Log.d("ConfigurableRemoteMediator", "📥 Loading page 1 for empty ${config.title}")
                        loadPage1FromApi()
                    } else {
                        Log.d("ConfigurableRemoteMediator", "✅ Skip refresh - ${config.title} has cached data")
                        MediatorResult.Success(endOfPaginationReached = true)
                    }
                }
                LoadType.PREPEND -> {
                    // Never prepend
                    MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    // Check if row is focused (temporarily disabled for testing)
                    val rowFocused = isRowFocused()
                    Log.d("ConfigurableRemoteMediator", "🎯 Row focus status for ${config.title}: $rowFocused")
                    // TODO: Re-enable focus check after testing
                    // if (!isRowFocused()) {
                    //     Log.d("ConfigurableRemoteMediator", "⏸️ Skip append - ${config.title} is not focused")
                    //     return MediatorResult.Success(endOfPaginationReached = false)
                    // }

                    // Get actual database count
                    val dbCount =
                        if (isMovie) {
                            genericRepository.getMovieDataSourceCount(config)
                        } else {
                            genericRepository.getTvDataSourceCount(config)
                        }

                    // Calculate next page based on actual page size from config
                    val pageSize = StrmrConstants.Paging.PAGE_SIZE // Should match PagingConfig pageSize
                    val nextPage = (dbCount / pageSize) + 1

                    // Log current position for debugging
                    val currentPosition = getCurrentPosition()
                    val totalItems = getTotalItems()
                    Log.d("ConfigurableRemoteMediator", "📥 Loading page $nextPage for ${config.title}")
                    Log.d("ConfigurableRemoteMediator", "🔍 Position check: getCurrentPosition()=$currentPosition")
                    Log.d("ConfigurableRemoteMediator", "🔍 Total items: getTotalItems()=$totalItems, dbCount=$dbCount")
                    Log.d(
                        "ConfigurableRemoteMediator",
                        "📊 Position summary: ${currentPosition + 1}/$dbCount from callbacks vs $totalItems total",
                    )

                    // Check if we should actually load more data - load when within 6 items of end
                    // (aligned with Flixclusive buffer pattern)
                    val itemsRemaining = dbCount - currentPosition - 1
                    if (itemsRemaining > 6) {
                        Log.d("ConfigurableRemoteMediator", "⏸️ Skip append - still $itemsRemaining items remaining (threshold: 6)")
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }

                    Log.d("ConfigurableRemoteMediator", "🚀 Triggering load - only $itemsRemaining items remaining")

                    loadPageFromApi(nextPage)
                }
            }
        } catch (e: Exception) {
            Log.e("ConfigurableRemoteMediator", "❌ Error loading ${config.title}", e)
            MediatorResult.Error(e)
        }
    }

    private suspend fun loadPage1FromApi(): MediatorResult {
        return database.withTransaction {
            // Load page 1 from API
            if (isMovie) {
                genericRepository.refreshMovieDataSource(config)
            } else {
                genericRepository.refreshTvDataSource(config)
            }

            Log.d("ConfigurableRemoteMediator", "✅ Loaded page 1 for ${config.title}")
            MediatorResult.Success(endOfPaginationReached = false) // More pages available
        }
    }

    private suspend fun loadPageFromApi(page: Int): MediatorResult {
        return try {
            // Load specific page from API WITHOUT database transaction to prevent invalidation
            val itemsLoaded =
                if (isMovie) {
                    genericRepository.loadMovieDataSourcePage(config, page)
                } else {
                    genericRepository.loadTvDataSourcePage(config, page)
                }

            Log.d("ConfigurableRemoteMediator", "✅ Loaded page $page for ${config.title}, items: $itemsLoaded")

            // Check if this was a partial page (end of pagination)
            val isEndOfPagination = itemsLoaded < StrmrConstants.Paging.PAGE_SIZE
            MediatorResult.Success(endOfPaginationReached = isEndOfPagination)
        } catch (e: Exception) {
            Log.e("ConfigurableRemoteMediator", "❌ Error loading page $page", e)
            MediatorResult.Error(e)
        }
    }
}
