package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import com.strmr.ai.domain.usecase.MediaType
import com.strmr.ai.ui.screens.UiState
import com.strmr.ai.ui.screens.PagingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvShowsViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val fetchLogoUseCase: FetchLogoUseCase,
    private val omdbRepository: OmdbRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState<TvShowEntity>())
    val uiState = _uiState.asStateFlow()
    
    private val _pagingUiState = MutableStateFlow(PagingUiState<TvShowEntity>())
    val pagingUiState = _pagingUiState.asStateFlow()
    
    private var initialLoadComplete = false

    init {
        setupPaging()
        loadTvShows()
        refreshAllTvShows()
    }
    
    private fun setupPaging() {
        _pagingUiState.value = PagingUiState(
            mediaRows = mapOf(
                "Trending TV Shows" to tvShowRepository.getTrendingTvShowsPager().cachedIn(viewModelScope),
                "Popular TV Shows" to tvShowRepository.getPopularTvShowsPager().cachedIn(viewModelScope)
            )
        )
    }

    private fun loadTvShows() {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            var firstDbEmission = false
            combine(
                tvShowRepository.getTrendingTvShows(),
                tvShowRepository.getPopularTvShows()
            ) { trending, popular ->
                val mediaRows = mutableMapOf<String, List<TvShowEntity>>()
                if (trending.isNotEmpty()) mediaRows["Trending TV Shows"] = trending
                if (popular.isNotEmpty()) mediaRows["Popular TV Shows"] = popular
                UiState(
                    isLoading = !initialLoadComplete || !firstDbEmission,
                    mediaRows = mediaRows
                )
            }.collect { uiState ->
                firstDbEmission = true
                _uiState.value = uiState
            }
        }
    }

    private fun refreshAllTvShows() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                tvShowRepository.refreshTrendingTvShows()
                tvShowRepository.refreshPopularTvShows()
            } catch (e: Exception) {
                Log.e("TvShowsViewModel", "Error refreshing TV shows", e)
            } finally {
                initialLoadComplete = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onTvShowSelected(rowIndex: Int, showIndex: Int) {
        val currentState = _uiState.value
        val rowTitles = currentState.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIndex) ?: return
        val shows = currentState.mediaRows[rowTitle] ?: return
        val selectedShow = shows.getOrNull(showIndex) ?: return

        // Check if logo is already cached
        if (selectedShow.logoUrl == null) {
            Log.d("TvShowsViewModel", "🎯 Fetching logo for '${selectedShow.title}' (TMDB: ${selectedShow.tmdbId})")
            fetchAndUpdateLogo(selectedShow)
        } else {
            Log.d("TvShowsViewModel", "✅ Logo already cached for '${selectedShow.title}'")
        }
    }

    fun loadMore(rowIdx: Int, itemIdx: Int, totalItems: Int) {
        val rowTitles = _uiState.value.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIdx) ?: return
        when (rowTitle) {
            "Trending TV Shows" -> viewModelScope.launch { tvShowRepository.loadMoreTrendingTvShows() }
            "Popular TV Shows" -> viewModelScope.launch { tvShowRepository.loadMorePopularTvShows() }
        }
    }

    suspend fun getOmdbRatings(imdbId: String): OmdbResponse? = omdbRepository.getOmdbRatings(imdbId)

    suspend fun clearNullLogos() = tvShowRepository.clearNullLogos()

    private fun fetchAndUpdateLogo(show: TvShowEntity) {
        viewModelScope.launch {
            Log.d("TvShowsViewModel", "🎯 Fetching logo for '${show.title}' (TMDB: ${show.tmdbId})")
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(show.tmdbId, MediaType.TV_SHOW)
            tvShowRepository.updateTvShowLogo(show.tmdbId, logoUrl)
        }
    }
} 