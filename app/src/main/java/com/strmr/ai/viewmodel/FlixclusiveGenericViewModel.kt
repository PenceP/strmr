package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
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
 * Generic Flixclusive-style ViewModel that can handle any movie data source
 * Replaces individual ViewModels (Trending, Popular, TopMovies, Recommended)
 */
@HiltViewModel
class FlixclusiveGenericViewModel
    @Inject
    constructor(
        private val genericRepository: GenericTraktRepository,
    ) : ViewModel() {
        // Map to hold multiple data source states
        private val _movieStates = mutableMapOf<String, MutableStateFlow<List<MovieEntity>>>()
        private val _paginationStates = mutableMapOf<String, MutableStateFlow<PaginationStateInfo>>()
        private val _loadJobs = mutableMapOf<String, Job?>()

        fun getMoviesFlow(dataSourceId: String) =
            _movieStates.getOrPut(dataSourceId) {
                MutableStateFlow(emptyList())
            }.asStateFlow()

        fun getPaginationFlow(dataSourceId: String) =
            _paginationStates.getOrPut(dataSourceId) {
                MutableStateFlow(
                    PaginationStateInfo(
                        canPaginate = true, // Allow initial load trigger
                        pagingState = PagingState.IDLE,
                        currentPage = 1,
                    ),
                )
            }.asStateFlow()

        /**
         * Initialize a data source - call this when you need a specific row
         */
        fun initializeDataSource(config: DataSourceConfig) {
            if (_movieStates.containsKey(config.id)) {
                Log.d("FlixclusiveGenericViewModel", "📊 DataSource ${config.id} already initialized")
                return
            }

            Log.d("FlixclusiveGenericViewModel", "🏗️ Initializing data source: ${config.id}")

            // Create flows for this data source
            getMoviesFlow(config.id)
            getPaginationFlow(config.id)

            // Load initial data if we have cached data
            viewModelScope.launch {
                try {
                    val cachedMovies = genericRepository.getMoviesFromDataSource(config).first()
                    if (cachedMovies.isNotEmpty()) {
                        Log.d("FlixclusiveGenericViewModel", "✅ ${config.id}: Loaded ${cachedMovies.size} cached movies")
                        _movieStates[config.id]?.value = cachedMovies
                        _paginationStates[config.id]?.value = _paginationStates[config.id]?.value?.copy(
                            pagingState = PagingState.IDLE,
                            canPaginate =
                                when (config.id) {
                                    "trending", "popular" -> true // These can paginate
                                    else -> false // Lists are static
                                },
                        ) ?: PaginationStateInfo(
                            canPaginate = false,
                            pagingState = PagingState.IDLE,
                            currentPage = 1,
                        )
                    } else {
                        // Trigger initial load
                        loadMovies(config)
                    }
                } catch (e: Exception) {
                    Log.e("FlixclusiveGenericViewModel", "❌ ${config.id}: Error loading initial data", e)
                    _paginationStates[config.id]?.value = _paginationStates[config.id]?.value?.copy(
                        pagingState = PagingState.ERROR,
                    ) ?: PaginationStateInfo(
                        canPaginate = true,
                        pagingState = PagingState.ERROR,
                        currentPage = 1,
                    )
                }
            }
        }

        /**
         * Load movies for a specific data source
         */
        fun loadMovies(config: DataSourceConfig) {
            val dataSourceId = config.id

            // Prevent concurrent requests
            if (_loadJobs[dataSourceId]?.isActive == true) {
                Log.d("FlixclusiveGenericViewModel", "⏸️ $dataSourceId: Load already in progress")
                return
            }

            // Don't reload if we're already loading (unless it's an error retry)
            val currentState = _paginationStates[dataSourceId]?.value
            if (currentState?.pagingState == PagingState.LOADING) {
                Log.d("FlixclusiveGenericViewModel", "🚫 $dataSourceId: Load not allowed, current state: ${currentState.pagingState}")
                return
            }

            Log.d("FlixclusiveGenericViewModel", "🚀 $dataSourceId: Loading movies")

            _loadJobs[dataSourceId] =
                viewModelScope.launch {
                    try {
                        _paginationStates[dataSourceId]?.value = _paginationStates[dataSourceId]?.value?.copy(
                            pagingState = PagingState.LOADING,
                        ) ?: PaginationStateInfo(
                            canPaginate = true,
                            pagingState = PagingState.LOADING,
                            currentPage = 1,
                        )

                        // Refresh the data source
                        genericRepository.refreshMovieDataSource(config)

                        // Get updated data from database
                        val updatedMovies = genericRepository.getMoviesFromDataSource(config).first()

                        _movieStates[dataSourceId]?.value = updatedMovies
                        _paginationStates[dataSourceId]?.value =
                            PaginationStateInfo(
                                canPaginate =
                                    when (dataSourceId) {
                                        "trending", "popular" -> true // These can paginate
                                        else -> false // Lists are static
                                    },
                                pagingState = PagingState.IDLE,
                                currentPage = if (dataSourceId in listOf("trending", "popular")) 1 else 1,
                            )

                        Log.d("FlixclusiveGenericViewModel", "✅ $dataSourceId: Load complete: ${updatedMovies.size} total movies")
                    } catch (e: Exception) {
                        Log.e("FlixclusiveGenericViewModel", "❌ $dataSourceId: Error loading movies", e)
                        _paginationStates[dataSourceId]?.value = _paginationStates[dataSourceId]?.value?.copy(
                            pagingState = PagingState.ERROR,
                        ) ?: PaginationStateInfo(
                            canPaginate = true,
                            pagingState = PagingState.ERROR,
                            currentPage = 1,
                        )
                    }
                }
        }

        /**
         * Paginate movies for a specific data source (for trending/popular)
         */
        fun paginateMovies(
            config: DataSourceConfig,
            page: Int,
        ) {
            val dataSourceId = config.id

            // Only trending and popular support pagination
            if (dataSourceId !in listOf("trending", "popular")) {
                Log.d("FlixclusiveGenericViewModel", "🚫 $dataSourceId: Pagination not supported")
                return
            }

            // Prevent concurrent requests
            if (_loadJobs[dataSourceId]?.isActive == true) {
                Log.d("FlixclusiveGenericViewModel", "⏸️ $dataSourceId: Pagination already in progress")
                return
            }

            val currentState = _paginationStates[dataSourceId]?.value
            if (currentState?.pagingState == PagingState.PAGINATING) {
                Log.d("FlixclusiveGenericViewModel", "🚫 $dataSourceId: Already paginating")
                return
            }

            Log.d("FlixclusiveGenericViewModel", "📄 $dataSourceId: Paginating to page $page")

            _loadJobs[dataSourceId] =
                viewModelScope.launch {
                    try {
                        _paginationStates[dataSourceId]?.value = currentState?.copy(
                            pagingState = PagingState.PAGINATING,
                            currentPage = page,
                        ) ?: PaginationStateInfo(
                            canPaginate = true,
                            pagingState = PagingState.PAGINATING,
                            currentPage = page,
                        )

                        // Load next page
                        genericRepository.loadMovieDataSourcePage(config, page)

                        // Get updated data from database
                        val updatedMovies = genericRepository.getMoviesFromDataSource(config).first()

                        _movieStates[dataSourceId]?.value = updatedMovies
                        _paginationStates[dataSourceId]?.value =
                            PaginationStateInfo(
                                canPaginate = true,
                                pagingState = PagingState.IDLE,
                                currentPage = page,
                            )

                        Log.d("FlixclusiveGenericViewModel", "✅ $dataSourceId: Pagination complete: ${updatedMovies.size} total movies")
                    } catch (e: Exception) {
                        Log.e("FlixclusiveGenericViewModel", "❌ $dataSourceId: Error paginating movies", e)
                        _paginationStates[dataSourceId]?.value = currentState?.copy(
                            pagingState = PagingState.ERROR,
                        ) ?: PaginationStateInfo(
                            canPaginate = true,
                            pagingState = PagingState.ERROR,
                            currentPage = page,
                        )
                    }
                }
        }

        override fun onCleared() {
            super.onCleared()
            _loadJobs.values.forEach { it?.cancel() }
        }
    }
