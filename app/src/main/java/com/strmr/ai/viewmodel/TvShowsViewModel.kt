package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.screens.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TvShowsViewModel(
    private val tvShowRepository: TvShowRepository,
    private val tmdbApiService: TmdbApiService
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState<TvShowEntity>())
    val uiState = _uiState.asStateFlow()
    private var initialLoadComplete = false

    init {
        loadTvShows()
        refreshAllTvShows()
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

    private fun fetchAndUpdateLogo(show: TvShowEntity) {
        viewModelScope.launch {
            try {
                Log.d("TvShowsViewModel", "📡 Fetching logo from TMDB API for '${show.title}' (TMDB: ${show.tmdbId})")
                val images = withContext(Dispatchers.IO) {
                    tmdbApiService.getTvShowImages(show.tmdbId)
                }
                
                // Extract logo URL from images response
                val logoUrl = extractLogoUrl(images)
                Log.d("TvShowsViewModel", "🎨 Logo URL for '${show.title}': $logoUrl")
                
                // Save logo URL to database
                tvShowRepository.updateTvShowLogo(show.tmdbId, logoUrl)
                
            } catch (e: Exception) {
                Log.w("TvShowsViewModel", "❌ Error fetching logo for tmdbId=${show.tmdbId}", e)
                // Save null to database to avoid repeated failed attempts
                tvShowRepository.updateTvShowLogo(show.tmdbId, null)
            }
        }
    }

    private fun extractLogoUrl(images: com.strmr.ai.data.TmdbImagesResponse): String? {
        return images.logos
            .filter { it.iso_639_1 == "en" || it.iso_639_1 == null }
            .firstOrNull()
            ?.file_path
            ?.let { "https://image.tmdb.org/t/p/original$it" }
    }
} 