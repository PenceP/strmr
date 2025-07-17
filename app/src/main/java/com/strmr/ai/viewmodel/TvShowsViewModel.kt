package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.screens.UiState
import com.strmr.ai.ui.screens.PagingUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TvShowsViewModel @Inject constructor(
    private val tvShowRepository: TvShowRepository,
    private val tmdbApiService: TmdbApiService,
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
            try {
                Log.d("TvShowsViewModel", "📡 Fetching logo from TMDB API for '${show.title}' (TMDB: ${show.tmdbId})")
                val images = withContext(Dispatchers.IO) {
                    tmdbApiService.getTvShowImages(show.tmdbId)
                }
                
                // Extract logo URL from images response
                val logoUrl = extractLogoUrl(images)
                Log.d("TvShowsViewModel", "🎨 Logo URL for '${show.title}': $logoUrl")
                
                // Save logo URL to database (null means no logo available, not an error)
                tvShowRepository.updateTvShowLogo(show.tmdbId, logoUrl)
                
            } catch (e: Exception) {
                Log.w("TvShowsViewModel", "❌ Error fetching logo for tmdbId=${show.tmdbId}", e)
                // Don't save null on error - this allows retries
                // Only save null when we successfully get a response but no logo is available
            }
        }
    }

    private fun extractLogoUrl(images: com.strmr.ai.data.TmdbImagesResponse): String? {
        Log.d("TvShowsViewModel", "🔍 Extracting logo from ${images.logos.size} available logos")
        
        if (images.logos.isEmpty()) {
            Log.d("TvShowsViewModel", "❌ No logos available")
            return null
        }
        
        // Log all available logos for debugging
        images.logos.forEachIndexed { index, logo ->
            Log.d("TvShowsViewModel", "🔍 Logo $index: iso=${logo.iso_639_1}, path=${logo.file_path}")
        }
        
        // Prefer English logos, then any logo with a valid path
        val selectedLogo = images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
            ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }
        
        val logoUrl = selectedLogo?.file_path?.let { "https://image.tmdb.org/t/p/original$it" }
        
        Log.d("TvShowsViewModel", "✅ Selected logo: iso=${selectedLogo?.iso_639_1}, path=${selectedLogo?.file_path}")
        Log.d("TvShowsViewModel", "✅ Final logo URL: $logoUrl")
        
        return logoUrl
    }
} 