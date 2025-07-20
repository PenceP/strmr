package com.strmr.ai.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import com.strmr.ai.domain.usecase.MediaType as LogoMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

/**
 * Fully JSON-driven Movies ViewModel that replaces hardcoded data sources
 * Uses GenericTraktRepository for dynamic data loading
 */
@HiltViewModel
class GenericMoviesViewModel @Inject constructor(
    private val genericRepository: GenericTraktRepository,
    private val fetchLogoUseCase: FetchLogoUseCase,
    private val omdbRepository: OmdbRepository
) : BaseConfigurableViewModel<MovieEntity>(genericRepository, MediaType.MOVIE) {
    
    // Track logo URLs separately for immediate UI updates
    private val _logoUrls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val logoUrls = _logoUrls.asStateFlow()
    
    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun createPagingFlow(
        config: DataSourceConfig, 
        isRowFocused: () -> Boolean,
        getCurrentPosition: () -> Int,
        getTotalItems: () -> Int
    ): Flow<PagingData<MovieEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,  // Larger page size for TV app
                enablePlaceholders = false,
                prefetchDistance = 10,  // Much smaller to prevent excessive loading
                initialLoadSize = 50,  // Same as page size to avoid over-loading
                maxSize = 200,  // Limit memory usage to prevent excessive caching
                jumpThreshold = Int.MAX_VALUE  // Disable jump threshold to prevent unnecessary loads
            ),
            remoteMediator = com.strmr.ai.data.paging.ConfigurableRemoteMediator(
                config = config,
                database = genericRepository.database,
                genericRepository = genericRepository,
                isMovie = true,
                isRowFocused = isRowFocused,
                getCurrentPosition = getCurrentPosition,
                getTotalItems = getTotalItems
            ),
            pagingSourceFactory = {
                genericRepository.getMoviesPagingFromDataSource(config)
            }
        ).flow.cachedIn(viewModelScope)
    }
    
    override suspend fun loadMultipleDataSources(dataSources: List<Pair<String, DataSourceConfig>>) {
        val configs = dataSources.map { it.second }
        val dataFlow = genericRepository.getMultipleMovieDataSources(configs)
        
        dataFlow.collect { dataMap ->
            val mediaRows = dataSources.associate { (title, config) ->
                title to (dataMap[config.title] ?: emptyList())
            }
            
            _uiState.value = uiState.value.copy(
                isLoading = false,
                mediaRows = mediaRows
            )
        }
    }
    
    override suspend fun refreshDataSource(config: DataSourceConfig) {
        genericRepository.refreshMovieDataSource(config)
    }
    
    // Movie-specific methods
    fun onMovieSelected(rowIndex: Int, movieIndex: Int) {
        val currentState = uiState.value
        val rowTitles = currentState.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIndex) ?: return
        val movies = currentState.mediaRows[rowTitle] ?: return
        val selectedMovie = movies.getOrNull(movieIndex) ?: return

        // Fetch logo if missing
        if (selectedMovie.logoUrl == null) {
            fetchAndUpdateLogo(selectedMovie)
        }
    }
    
    fun fetchAndUpdateLogo(movie: MovieEntity) {
        viewModelScope.launch {
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(movie.tmdbId, LogoMediaType.MOVIE)
            if (logoUrl != null) {
                // Update logo in database
                genericRepository.updateMovieLogo(movie.tmdbId, logoUrl)
                
                // Update logo URL map for immediate UI update
                _logoUrls.value = _logoUrls.value + (movie.tmdbId to logoUrl)
                
                // Also update regular UI state
                updateMovieInUiState(movie.copy(logoUrl = logoUrl))
                
                Log.d("GenericMoviesViewModel", "✅ Updated logo for ${movie.title}: $logoUrl")
            }
        }
    }
    
    private fun updateMovieInUiState(updatedMovie: MovieEntity) {
        val currentState = _uiState.value
        val updatedRows = currentState.mediaRows.mapValues { (_, movies) ->
            movies.map { movie ->
                if (movie.tmdbId == updatedMovie.tmdbId) updatedMovie else movie
            }
        }
        _uiState.value = currentState.copy(mediaRows = updatedRows)
    }
    
    suspend fun getOmdbRatings(imdbId: String): OmdbResponse? = omdbRepository.getOmdbRatings(imdbId)
    
    override suspend fun isDataSourceEmpty(config: DataSourceConfig): Boolean {
        return genericRepository.isMovieDataSourceEmpty(config)
    }
}