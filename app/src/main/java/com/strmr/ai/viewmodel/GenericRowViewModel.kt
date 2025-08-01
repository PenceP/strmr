package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.config.GenericRowConfiguration
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.ui.components.PaginationStateInfo
import com.strmr.ai.ui.components.PagingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Generic ViewModel for all row types in the application
 * Replaces individual specialized ViewModels with a single, configurable implementation
 * Incorporates the improved Flixclusive-style pagination logic
 */
class GenericRowViewModel constructor(
    private val genericRepository: GenericTraktRepository,
    private val configuration: GenericRowConfiguration,
) : ViewModel() {
    private val _items = MutableStateFlow<List<MovieEntity>>(emptyList())
    val items = _items.asStateFlow()

    private val _paginationState =
        MutableStateFlow(
            PaginationStateInfo(
                canPaginate = configuration.isPaginated,
                pagingState = PagingState.LOADING,
                currentPage = 1,
            ),
        )
    val paginationState = _paginationState.asStateFlow()

    private var loadingJob: Job? = null
    private var lastLoadedPage = 0 // Track the last successfully loaded page

    private val dataSourceConfig =
        DataSourceConfig(
            id = configuration.id,
            title = configuration.title,
            endpoint = configuration.endpoint,
            mediaType = configuration.mediaType,
            cacheKey = configuration.cacheKey,
        )

    init {
        Log.d("GenericRowViewModel", "<� Initializing ${configuration.title} (${configuration.id})")
        Log.d("GenericRowViewModel", "=� Config: paginated=${configuration.isPaginated}, endpoint=${configuration.endpoint}")
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Check if we have cached data
                val cachedItems = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()
                if (cachedItems.isNotEmpty()) {
                    Log.d("GenericRowViewModel", " ${configuration.title}: Loaded ${cachedItems.size} cached items")
                    _items.value = cachedItems

                    if (configuration.isPaginated) {
                        // Handle paginated content
                        if (cachedItems.size <= configuration.pageSize) {
                            Log.d(
                                "GenericRowViewModel",
                                "= ${configuration.title}: Cached data incomplete (${cachedItems.size} <= ${configuration.pageSize}), refreshing...",
                            )
                            lastLoadedPage = if (cachedItems.size == configuration.pageSize) 1 else 0
                            _paginationState.value =
                                _paginationState.value.copy(
                                    pagingState = PagingState.LOADING,
                                    canPaginate = true,
                                    currentPage = 1,
                                )
                            loadPage(1) // Refresh first page
                        } else {
                            // Calculate how many pages we have loaded based on cached data
                            lastLoadedPage = (cachedItems.size + configuration.pageSize - 1) / configuration.pageSize // Round up
                            _paginationState.value =
                                _paginationState.value.copy(
                                    pagingState = PagingState.IDLE,
                                    canPaginate = true,
                                    currentPage = lastLoadedPage + 1,
                                )
                        }
                    } else {
                        // Handle static content (non-paginated)
                        _paginationState.value =
                            _paginationState.value.copy(
                                pagingState = PagingState.IDLE,
                                canPaginate = false,
                            )
                    }
                } else {
                    // No cached data, load from API
                    if (configuration.isPaginated) {
                        loadPage(1)
                    } else {
                        loadStaticData()
                    }
                }
            } catch (e: Exception) {
                Log.e("GenericRowViewModel", "L ${configuration.title}: Error loading initial data", e)
                _paginationState.value =
                    _paginationState.value.copy(
                        pagingState = PagingState.ERROR,
                    )
            }
        }
    }

    /**
     * Load a specific page (for paginated content)
     */
    fun loadPage(page: Int) {
        if (!configuration.isPaginated) {
            Log.w("GenericRowViewModel", "� ${configuration.title}: loadPage called on non-paginated row")
            return
        }

        // Prevent concurrent requests
        if (loadingJob?.isActive == true) {
            Log.d("GenericRowViewModel", "� ${configuration.title}: Load already in progress, skipping page $page")
            return
        }

        // Don't paginate if we can't or if we're already loading/error state
        val currentState = _paginationState.value
        if (!currentState.canPaginate && currentState.pagingState != PagingState.ERROR) {
            Log.d("GenericRowViewModel", "=� ${configuration.title}: Pagination not allowed, current state: ${currentState.pagingState}")
            return
        }

        // Don't paginate the same page twice unless it's a refresh (page 1) or error retry
        if (page <= lastLoadedPage && currentState.pagingState != PagingState.ERROR && page != 1) {
            Log.d("GenericRowViewModel", "= ${configuration.title}: Page $page already loaded (lastLoaded=$lastLoadedPage), skipping")
            return
        }

        Log.d("GenericRowViewModel", "=� ${configuration.title}: Starting pagination for page $page")

        loadingJob =
            viewModelScope.launch {
                try {
                    _paginationState.value =
                        _paginationState.value.copy(
                            pagingState = if (page == 1) PagingState.LOADING else PagingState.PAGINATING,
                        )

                    if (page == 1) {
                        // Refresh first page
                        genericRepository.refreshMovieDataSource(dataSourceConfig)
                    } else {
                        // Load additional page
                        val itemsLoaded = genericRepository.loadMovieDataSourcePage(dataSourceConfig, page)
                        Log.d("GenericRowViewModel", "=� ${configuration.title}: Loaded $itemsLoaded items for page $page")
                    }

                    // Get updated data from database
                    val updatedItems = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()

                    // Check if we got a full page of results
                    val previousSize = _items.value.size
                    val itemsLoaded = if (page == 1) updatedItems.size else updatedItems.size - previousSize
                    val gotFullPage = itemsLoaded >= configuration.pageSize

                    // Can paginate if we got a full page and haven't hit the max limit
                    val totalPagesLoaded = (updatedItems.size + configuration.pageSize - 1) / configuration.pageSize
                    val canPaginate = gotFullPage && totalPagesLoaded < configuration.maxPages

                    _items.value = updatedItems
                    lastLoadedPage = page // Update last loaded page on success
                    _paginationState.value =
                        PaginationStateInfo(
                            canPaginate = canPaginate,
                            pagingState = if (canPaginate) PagingState.IDLE else PagingState.PAGINATING_EXHAUST,
                            currentPage = if (canPaginate) page + 1 else page,
                        )

                    Log.d("GenericRowViewModel", " ${configuration.title}: Pagination complete: ${updatedItems.size} total items, canPaginate=$canPaginate, nextPage=${_paginationState.value.currentPage}, lastLoaded=$lastLoadedPage")
                } catch (e: Exception) {
                    Log.e("GenericRowViewModel", "L ${configuration.title}: Error paginating page $page", e)
                    _paginationState.value =
                        _paginationState.value.copy(
                            pagingState = PagingState.ERROR,
                        )
                }
            }
    }

    /**
     * Load static data (for non-paginated content)
     */
    fun loadStaticData() {
        // Prevent concurrent requests
        if (loadingJob?.isActive == true) {
            Log.d("GenericRowViewModel", "� ${configuration.title}: Load already in progress")
            return
        }

        // Don't reload if we're already loading (unless it's an error retry)
        val currentState = _paginationState.value
        if (currentState.pagingState == PagingState.LOADING) {
            Log.d("GenericRowViewModel", "=� ${configuration.title}: Load not allowed, current state: ${currentState.pagingState}")
            return
        }

        Log.d("GenericRowViewModel", "=� ${configuration.title}: Loading static data")

        loadingJob =
            viewModelScope.launch {
                try {
                    _paginationState.value =
                        _paginationState.value.copy(
                            pagingState = PagingState.LOADING,
                        )

                    // Refresh the data
                    genericRepository.refreshMovieDataSource(dataSourceConfig)

                    // Get updated data from database
                    val updatedItems = genericRepository.getMoviesFromDataSource(dataSourceConfig).first()

                    _items.value = updatedItems
                    _paginationState.value =
                        PaginationStateInfo(
                            canPaginate = false, // Static lists don't paginate
                            pagingState = PagingState.IDLE,
                            currentPage = 1,
                        )

                    Log.d("GenericRowViewModel", " ${configuration.title}: Load complete: ${updatedItems.size} total items")
                } catch (e: Exception) {
                    Log.e("GenericRowViewModel", "L ${configuration.title}: Error loading data", e)
                    _paginationState.value =
                        _paginationState.value.copy(
                            pagingState = PagingState.ERROR,
                        )
                }
            }
    }

    /**
     * Force refresh the data
     */
    fun refresh() {
        Log.d("GenericRowViewModel", "= ${configuration.title}: Forcing refresh")
        if (configuration.isPaginated) {
            lastLoadedPage = 0
            loadPage(1)
        } else {
            loadStaticData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadingJob?.cancel()
        Log.d("GenericRowViewModel", ">� ${configuration.title}: ViewModel cleared")
    }
}
