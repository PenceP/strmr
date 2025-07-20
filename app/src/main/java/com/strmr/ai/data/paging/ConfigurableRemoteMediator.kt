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
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity

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
    private val isRowFocused: () -> Boolean,  // Function to check if this row has focus
    private val getCurrentPosition: () -> Int = { 0 },  // Function to get current position in row (for logging only)
    private val getTotalItems: () -> Int = { 0 }  // Function to get total items in row (for logging only)
) : RemoteMediator<Int, T>() {
    

    override suspend fun initialize(): InitializeAction {
        // Check if data source is empty
        val isEmpty = if (isMovie) {
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
        state: PagingState<Int, T>
    ): MediatorResult {
        return try {
            when (loadType) {
                LoadType.REFRESH -> {
                    // Only refresh if cache is empty
                    val isEmpty = if (isMovie) {
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
                    // Check if row is focused
                    if (!isRowFocused()) {
                        Log.d("ConfigurableRemoteMediator", "⏸️ Skip append - ${config.title} is not focused")
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    
                    // Get actual database count
                    val dbCount = if (isMovie) {
                        genericRepository.getMovieDataSourceCount(config)
                    } else {
                        genericRepository.getTvDataSourceCount(config)
                    }
                    
                    // Calculate next page based on actual page size from config
                    val pageSize = 50  // Should match PagingConfig pageSize
                    val nextPage = (dbCount / pageSize) + 1
                    
                    // Log current position for debugging
                    val currentPosition = getCurrentPosition()
                    Log.d("ConfigurableRemoteMediator", "📥 Loading page $nextPage for ${config.title} (position ${currentPosition + 1}/$dbCount)")
                    
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
            MediatorResult.Success(endOfPaginationReached = false)  // More pages available
        }
    }
    
    private suspend fun loadPageFromApi(page: Int): MediatorResult {
        return database.withTransaction {
            // Load specific page from API
            if (isMovie) {
                genericRepository.loadMovieDataSourcePage(config, page)
            } else {
                genericRepository.loadTvDataSourcePage(config, page)
            }
            
            Log.d("ConfigurableRemoteMediator", "✅ Loaded page $page for ${config.title}")
            
            // Always indicate more pages might be available unless we know for sure
            MediatorResult.Success(endOfPaginationReached = false)
        }
    }
}