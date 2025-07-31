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
 * Flixclusive-style ViewModel for Top Movies of the Week
 * Uses simple list loading (no pagination needed for curated lists)
 */
@HiltViewModel
class FlixclusiveTopMoviesViewModel @Inject constructor(
    private val genericRepository: GenericTraktRepository
) : ViewModel() {

    private val _movies = MutableStateFlow<List<MovieEntity>>(emptyList())
    val movies = _movies.asStateFlow()

    private val _paginationState = MutableStateFlow(
        PaginationStateInfo(
            canPaginate = false, // Lists don't need pagination
            pagingState = PagingState.LOADING,
            currentPage = 1
        )
    )
    val paginationState = _paginationState.asStateFlow()

    private var loadJob: Job? = null

    private val dataSourceConfig = DataSourceConfig(
        id = "top_movies_week",
        title = "Top Movies of the Week",
        endpoint = "users/garycrawfordgc/lists/top-movies-of-the-week/items",
        mediaType = MediaType.MOVIE,
        cacheKey = "top_movies_week"
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
                    Log.d("FlixclusiveTopMoviesViewModel", "✅ Loaded ${cachedMovies.size} cached movies")
                    _movies.value = cachedMovies
                    _paginationState.value = _paginationState.value.copy(
                        pagingState = PagingState.IDLE,
                        canPaginate = false // Lists are static
                    )
                } else {
                    // Load the list
                    loadMovies()
                }
            } catch (e: Exception) {
                Log.e("FlixclusiveTopMoviesViewModel", "❌ Error loading initial data", e)
                _paginationState.value = _paginationState.value.copy(
                    pagingState = PagingState.ERROR
                )
            }
        }
    }

    fun loadMovies() {
        // Prevent concurrent requests
        if (loadJob?.isActive == true) {
            Log.d("FlixclusiveTopMoviesViewModel", "⏸️ Load already in progress")
            return
        }

        // Don't reload if we're already loading/error state (unless it's an error retry)
        val currentState = _paginationState.value
        if (currentState.pagingState == PagingState.LOADING) {
            Log.d(
                "FlixclusiveTopMoviesViewModel",
                "🚫 Load not allowed, current state: ${currentState.pagingState}"
            )
            return
        }

        Log.d("FlixclusiveTopMoviesViewModel", "🚀 Loading top movies list")
        
        loadJob = viewModelScope.launch {
            try {
                _paginationState.value = _paginationState.value.copy(
                    pagingState = PagingState.LOADING
                )

                // Refresh the list
                genericRepository.refreshMovieDataSource(dataSourceConfig)

                // Get updated data from database
                val updatedMovies = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()

                _movies.value = updatedMovies
                _paginationState.value = PaginationStateInfo(
                    canPaginate = false, // Lists don't paginate
                    pagingState = PagingState.IDLE,
                    currentPage = 1
                )

                Log.d("FlixclusiveTopMoviesViewModel", "✅ Load complete: ${updatedMovies.size} total movies")

            } catch (e: Exception) {
                Log.e("FlixclusiveTopMoviesViewModel", "❌ Error loading movies", e)
                _paginationState.value = _paginationState.value.copy(
                    pagingState = PagingState.ERROR
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadJob?.cancel()
    }
}