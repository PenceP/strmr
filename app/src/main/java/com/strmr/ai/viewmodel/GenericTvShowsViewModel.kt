package com.strmr.ai.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.strmr.ai.ui.theme.StrmrConstants
import androidx.paging.cachedIn
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OnboardingService
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.TvShowEntity
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
 * Fully JSON-driven TV Shows ViewModel that replaces hardcoded data sources
 * Uses GenericTraktRepository for dynamic data loading
 */
@HiltViewModel
class GenericTvShowsViewModel @Inject constructor(
    private val genericRepository: GenericTraktRepository,
    private val fetchLogoUseCase: FetchLogoUseCase,
    private val omdbRepository: OmdbRepository,
    onboardingService: OnboardingService
) : BaseConfigurableViewModel<TvShowEntity>(genericRepository, MediaType.TV_SHOW, onboardingService) {
    
    // Track logo URLs separately for immediate UI updates
    private val _logoUrls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val logoUrls = _logoUrls.asStateFlow()
    
    @OptIn(androidx.paging.ExperimentalPagingApi::class)
    override fun createPagingFlow(
        config: DataSourceConfig, 
        isRowFocused: () -> Boolean,
        getCurrentPosition: () -> Int,
        getTotalItems: () -> Int
    ): Flow<PagingData<TvShowEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = StrmrConstants.Paging.PAGE_SIZE_SMALL,
                enablePlaceholders = false,
                prefetchDistance = StrmrConstants.Paging.PREFETCH_DISTANCE_STANDARD,  // Prefetch when within 10 items of the end
                initialLoadSize = StrmrConstants.Paging.PAGE_SIZE_SMALL  // Load same size for initial load
            ),
            remoteMediator = com.strmr.ai.data.paging.ConfigurableRemoteMediator(
                config = config,
                database = genericRepository.database,
                genericRepository = genericRepository,
                isMovie = false,
                isRowFocused = isRowFocused,
                getCurrentPosition = getCurrentPosition,
                getTotalItems = getTotalItems
            ),
            pagingSourceFactory = {
                genericRepository.getTvShowsPagingFromDataSource(config)
            }
        ).flow.cachedIn(viewModelScope)
    }
    
    override suspend fun loadMultipleDataSources(dataSources: List<Pair<String, DataSourceConfig>>) {
        val configs = dataSources.map { it.second }
        val dataFlow = genericRepository.getMultipleTvDataSources(configs)
        
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
        genericRepository.refreshTvDataSource(config)
    }
    
    // TV Show-specific methods
    fun onTvShowSelected(rowIndex: Int, showIndex: Int) {
        val currentState = uiState.value
        val rowTitles = currentState.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIndex) ?: return
        val shows = currentState.mediaRows[rowTitle] ?: return
        val selectedShow = shows.getOrNull(showIndex) ?: return

        // Fetch logo if missing
        if (selectedShow.logoUrl == null) {
            fetchAndUpdateLogo(selectedShow)
        }
    }
    
    fun fetchAndUpdateLogo(show: TvShowEntity) {
        viewModelScope.launch {
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(show.tmdbId, LogoMediaType.TV_SHOW)
            if (logoUrl != null) {
                // Update logo in database
                genericRepository.updateTvShowLogo(show.tmdbId, logoUrl)
                
                // Update logo URL map for immediate UI update
                _logoUrls.value = _logoUrls.value + (show.tmdbId to logoUrl)
                
                // Force refresh of the current UI state for immediate update
                updateTvShowInUiState(show.copy(logoUrl = logoUrl))
                
                Log.d("GenericTvShowsViewModel", "✅ Updated logo for ${show.title}: $logoUrl")
            }
        }
    }
    
    private fun updateTvShowInUiState(updatedShow: TvShowEntity) {
        val currentState = _uiState.value
        val updatedRows = currentState.mediaRows.mapValues { (_, shows) ->
            shows.map { show ->
                if (show.tmdbId == updatedShow.tmdbId) updatedShow else show
            }
        }
        _uiState.value = currentState.copy(mediaRows = updatedRows)
    }
    
    // Deprecated getOmdbRatings removed - use fetchOmdbRatings instead
    
    /**
     * Clean architecture method to fetch OMDB ratings
     * This replaces the deprecated getOmdbRatings method
     */
    suspend fun fetchOmdbRatings(imdbId: String): OmdbResponse? = omdbRepository.getOmdbRatings(imdbId)
    
    override suspend fun isDataSourceEmpty(config: DataSourceConfig): Boolean {
        return genericRepository.isTvDataSourceEmpty(config)
    }
}