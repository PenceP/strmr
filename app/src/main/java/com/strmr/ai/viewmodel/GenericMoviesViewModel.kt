package com.strmr.ai.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.DataSourceRegistry
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import com.strmr.ai.domain.usecase.MediaType as LogoMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    
    override fun createPagingFlow(config: DataSourceConfig): Flow<PagingData<MovieEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
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
            // TODO: Update movie logo in database through generic repository
        }
    }
    
    suspend fun getOmdbRatings(imdbId: String): OmdbResponse? = omdbRepository.getOmdbRatings(imdbId)
}