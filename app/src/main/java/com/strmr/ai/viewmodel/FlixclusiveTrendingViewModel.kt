package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.ui.components.PaginationStateInfo
import com.strmr.ai.ui.components.PagingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Flixclusive-style pagination ViewModel for Trending Movies
 * Replaces complex Paging3 with simple, predictable custom pagination
 */
@HiltViewModel
class FlixclusiveTrendingViewModel @Inject constructor(
    private val genericRepository: GenericTraktRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<MovieEntity>>(emptyList())
    val movies = _movies.asStateFlow()

    private val _paginationState = MutableStateFlow(
        PaginationStateInfo(
            canPaginate = true,
            pagingState = PagingState.LOADING,
            currentPage = 1
        )
    )
    val paginationState = _paginationState.asStateFlow()

    private var paginationJob: Job? = null

    private val dataSourceConfig = DataSourceConfig(
        id = "trending",
        title = "Trending",
        endpoint = "movies/trending",
        mediaType = MediaType.MOVIE,
        cacheKey = "trending_movies"
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Check if we have cached data
                val cachedMovies = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()
                if (cachedMovies.isNotEmpty()) {
                    Log.d("FlixclusiveTrendingViewModel", "✅ Loaded ${cachedMovies.size} cached movies")
                    _movies.value = cachedMovies
                    
                    // If we have less than a full page (50), we should refresh to get latest data
                    if (cachedMovies.size <= 50) {
                        Log.d("FlixclusiveTrendingViewModel", "🔄 Cached data incomplete (${cachedMovies.size} <= 50), refreshing...")
                        _paginationState.value = _paginationState.value.copy(
                            pagingState = PagingState.LOADING,
                            canPaginate = true,
                            currentPage = 1
                        )
                        paginateMovies(1) // Refresh first page
                    } else {
                        _paginationState.value = _paginationState.value.copy(
                            pagingState = PagingState.IDLE,
                            canPaginate = true // Always allow pagination unless we hit max limit
                        )
                    }
                } else {
                    // Load first page  
                    paginateMovies(1)
                }
            } catch (e: Exception) {
                Log.e("FlixclusiveTrendingViewModel", "❌ Error loading initial data", e)
                _paginationState.value = _paginationState.value.copy(
                    pagingState = PagingState.ERROR
                )
            }
        }
    }

    fun paginateMovies(page: Int) {
        // Prevent concurrent requests
        if (paginationJob?.isActive == true) {
            Log.d("FlixclusiveTrendingViewModel", "⏸️ Pagination already in progress, skipping page $page")
            return
        }

        // Don't paginate if we can't or if we're already loading/error state
        val currentState = _paginationState.value
        if (!currentState.canPaginate && currentState.pagingState != PagingState.ERROR) {
            Log.d(
                "FlixclusiveTrendingViewModel",
                "🚫 Pagination not allowed, current state: ${currentState.pagingState}"
            )
            return
        }

        // Don't paginate the same page twice unless it's a refresh (page 1) or error retry
        if (page == currentState.currentPage && currentState.pagingState != PagingState.ERROR && page != 1) {
            Log.d("FlixclusiveTrendingViewModel", "🔄 Page $page already loaded, skipping")
            return
        }

        Log.d("FlixclusiveTrendingViewModel", "🚀 Starting pagination for page $page")
        
        paginationJob = viewModelScope.launch {
            try {
                _paginationState.value = _paginationState.value.copy(
                    pagingState = if (page == 1) PagingState.LOADING else PagingState.PAGINATING
                )

                if (page == 1) {
                    // Refresh first page
                    genericRepository.refreshMovieDataSource(dataSourceConfig)
                } else {
                    // Load additional page
                    val itemsLoaded = genericRepository.loadMovieDataSourcePage(dataSourceConfig, page)
                    Log.d("FlixclusiveTrendingViewModel", "📥 Loaded $itemsLoaded items for page $page")
                }

                // Get updated data from database
                val updatedMovies = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()
                val canPaginate = updatedMovies.size % 50 == 0 && updatedMovies.size < 500 // Max 10 pages

                _movies.value = updatedMovies
                _paginationState.value = PaginationStateInfo(
                    canPaginate = canPaginate,
                    pagingState = if (canPaginate) PagingState.IDLE else PagingState.PAGINATING_EXHAUST,
                    currentPage = if (canPaginate) page + 1 else page
                )

                Log.d("FlixclusiveTrendingViewModel", "✅ Pagination complete: ${updatedMovies.size} total movies, canPaginate=$canPaginate, nextPage=${_paginationState.value.currentPage}")

            } catch (e: Exception) {
                Log.e("FlixclusiveTrendingViewModel", "❌ Error paginating page $page", e)
                _paginationState.value = _paginationState.value.copy(
                    pagingState = PagingState.ERROR
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        paginationJob?.cancel()
    }
}