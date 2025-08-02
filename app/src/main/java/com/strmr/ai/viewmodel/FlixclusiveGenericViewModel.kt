package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
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
 * Generic Flixclusive-style ViewModel that can handle any media data source (movies/TV shows)
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
        private val _tvShowStates = mutableMapOf<String, MutableStateFlow<List<TvShowEntity>>>()
        private val _paginationStates = mutableMapOf<String, MutableStateFlow<PaginationStateInfo>>()
        private val _loadJobs = mutableMapOf<String, Job?>()

        fun getMoviesFlow(dataSourceId: String) =
            _movieStates.getOrPut(dataSourceId) {
                MutableStateFlow(emptyList())
            }.asStateFlow()

        fun getTvShowsFlow(dataSourceId: String) =
            _tvShowStates.getOrPut(dataSourceId) {
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
         * This version immediately populates UI flows with cached data if available
         */
        fun initializeDataSource(config: DataSourceConfig) {
            val isInitialized =
                when (config.mediaType.toString().lowercase()) {
                    "movie" -> _movieStates.containsKey(config.id)
                    "tvshow" -> _tvShowStates.containsKey(config.id)
                    else -> false
                }

            if (isInitialized) {
                Log.d("FlixclusiveGenericViewModel", "📊 DataSource ${config.id} already initialized")
                return
            }

            Log.d("FlixclusiveGenericViewModel", "🏗️ Initializing data source: ${config.id} (${config.mediaType})")

            // Create flows for this data source
            when (config.mediaType.toString().lowercase()) {
                "movie" -> getMoviesFlow(config.id)
                "tvshow" -> getTvShowsFlow(config.id)
            }
            getPaginationFlow(config.id)

            // IMMEDIATE: Try to populate with cached data first (synchronous for instant UI)
            viewModelScope.launch {
                try {
                    when (config.mediaType.toString().lowercase()) {
                        "movie" -> {
                            // Try to get cached data immediately
                            val cachedMovies = genericRepository.getMoviesFromDataSource(config).first()
                            if (cachedMovies.isNotEmpty()) {
                                Log.d("FlixclusiveGenericViewModel", "✅ ${config.id}: Loaded ${cachedMovies.size} cached movies instantly")
                                _movieStates[config.id]?.value = cachedMovies
                                _paginationStates[config.id]?.value =
                                    PaginationStateInfo(
                                        canPaginate =
                                            when (config.id) {
                                                "trending", "popular" -> true
                                                else -> false
                                            },
                                        pagingState = PagingState.IDLE,
                                        currentPage = 1,
                                    )
                            } else {
                                Log.d("FlixclusiveGenericViewModel", "🔄 ${config.id}: No cache found, loading from API...")
                                // Show loading state immediately
                                _paginationStates[config.id]?.value =
                                    PaginationStateInfo(
                                        canPaginate = true,
                                        pagingState = PagingState.LOADING,
                                        currentPage = 1,
                                    )
                                // Load data from API and populate UI flows
                                loadMovies(config)
                            }
                        }
                        "tvshow" -> {
                            // Try to get cached data immediately
                            val cachedTvShows = genericRepository.getTvShowsFromDataSource(config).first()
                            if (cachedTvShows.isNotEmpty()) {
                                Log.d(
                                    "FlixclusiveGenericViewModel",
                                    "✅ ${config.id}: Loaded ${cachedTvShows.size} cached TV shows instantly",
                                )
                                _tvShowStates[config.id]?.value = cachedTvShows
                                _paginationStates[config.id]?.value =
                                    PaginationStateInfo(
                                        canPaginate =
                                            when (config.id) {
                                                "trending", "popular" -> true
                                                else -> false
                                            },
                                        pagingState = PagingState.IDLE,
                                        currentPage = 1,
                                    )
                            } else {
                                Log.d("FlixclusiveGenericViewModel", "🔄 ${config.id}: No cache found, loading from API...")
                                // Show loading state immediately
                                _paginationStates[config.id]?.value =
                                    PaginationStateInfo(
                                        canPaginate = true,
                                        pagingState = PagingState.LOADING,
                                        currentPage = 1,
                                    )
                                // Load data from API and populate UI flows
                                loadTvShows(config)
                            }
                        }
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
                                currentPage = if (dataSourceId in listOf("trending", "popular")) 2 else 1,
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
                                currentPage = page + 1,
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

        /**
         * Load TV shows for a specific data source
         */
        fun loadTvShows(config: DataSourceConfig) {
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

            Log.d("FlixclusiveGenericViewModel", "🚀 $dataSourceId: Loading TV shows")

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
                        genericRepository.refreshTvShowDataSource(config)

                        // Get updated data from database
                        val updatedTvShows = genericRepository.getTvShowsFromDataSource(config).first()

                        _tvShowStates[dataSourceId]?.value = updatedTvShows
                        _paginationStates[dataSourceId]?.value =
                            PaginationStateInfo(
                                canPaginate =
                                    when (dataSourceId) {
                                        "trending", "popular" -> true // These can paginate
                                        else -> false // Lists are static
                                    },
                                pagingState = PagingState.IDLE,
                                currentPage = if (dataSourceId in listOf("trending", "popular")) 2 else 1,
                            )

                        Log.d("FlixclusiveGenericViewModel", "✅ $dataSourceId: Load complete: ${updatedTvShows.size} total TV shows")
                    } catch (e: Exception) {
                        Log.e("FlixclusiveGenericViewModel", "❌ $dataSourceId: Error loading TV shows", e)
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
         * Paginate TV shows for a specific data source (for trending/popular)
         */
        fun paginateTvShows(
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
                        genericRepository.loadTvDataSourcePage(config, page)

                        // Get updated data from database
                        val updatedTvShows = genericRepository.getTvShowsFromDataSource(config).first()

                        _tvShowStates[dataSourceId]?.value = updatedTvShows
                        _paginationStates[dataSourceId]?.value =
                            PaginationStateInfo(
                                canPaginate = true,
                                pagingState = PagingState.IDLE,
                                currentPage = page + 1,
                            )

                        Log.d("FlixclusiveGenericViewModel", "✅ $dataSourceId: Pagination complete: ${updatedTvShows.size} total TV shows")
                    } catch (e: Exception) {
                        Log.e("FlixclusiveGenericViewModel", "❌ $dataSourceId: Error paginating TV shows", e)
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
