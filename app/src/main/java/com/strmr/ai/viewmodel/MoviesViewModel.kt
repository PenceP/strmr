package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.ui.screens.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MoviesViewModel(
    private val movieRepository: MovieRepository,
    private val tmdbApiService: TmdbApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState<MovieEntity>())
    val uiState = _uiState.asStateFlow()

    private var initialLoadComplete = false

    init {
        loadMovies()
        refreshAllMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            var firstDbEmission = false
            combine(
                movieRepository.getTrendingMovies(),
                movieRepository.getPopularMovies()
            ) { trending, popular ->
                UiState(
                    isLoading = !initialLoadComplete || !firstDbEmission,
                    mediaRows = mapOf(
                        "Trending Movies" to trending,
                        "Popular Movies" to popular
                    )
                )
            }.collect {
                firstDbEmission = true
                _uiState.value = it
            }
        }
    }

    private fun refreshAllMovies() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                movieRepository.refreshTrendingMovies()
                movieRepository.refreshPopularMovies()
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error refreshing movies", e)
            } finally {
                initialLoadComplete = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onMovieSelected(rowIndex: Int, movieIndex: Int) {
        val currentState = _uiState.value
        val rowTitles = currentState.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIndex) ?: return
        val movies = currentState.mediaRows[rowTitle] ?: return
        val selectedMovie = movies.getOrNull(movieIndex) ?: return

        // Check if logo is already cached
        if (selectedMovie.logoUrl == null) {
            Log.d("MoviesViewModel", "🎯 Fetching logo for '${selectedMovie.title}' (TMDB: ${selectedMovie.tmdbId})")
            fetchAndUpdateLogo(selectedMovie)
        } else {
            Log.d("MoviesViewModel", "✅ Logo already cached for '${selectedMovie.title}'")
        }
    }

    fun loadMore(rowIdx: Int, itemIdx: Int, totalItems: Int) {
        val rowTitles = _uiState.value.mediaRows.keys.toList()
        val rowTitle = rowTitles.getOrNull(rowIdx) ?: return
        when (rowTitle) {
            "Trending Movies" -> viewModelScope.launch { movieRepository.loadMoreTrendingMovies() }
            "Popular Movies" -> viewModelScope.launch { movieRepository.loadMorePopularMovies() }
        }
    }

    private fun fetchAndUpdateLogo(movie: MovieEntity) {
        viewModelScope.launch {
            try {
                Log.d("MoviesViewModel", "📡 Fetching logo from TMDB API for '${movie.title}' (TMDB: ${movie.tmdbId})")
                val images = withContext(Dispatchers.IO) {
                    tmdbApiService.getMovieImages(movie.tmdbId)
                }
                
                // Extract logo URL from images response
                val logoUrl = extractLogoUrl(images)
                Log.d("MoviesViewModel", "🎨 Logo URL for '${movie.title}': $logoUrl")
                
                // Save logo URL to database
                movieRepository.updateMovieLogo(movie.tmdbId, logoUrl)
                
            } catch (e: Exception) {
                Log.w("MoviesViewModel", "❌ Error fetching logo for tmdbId=${movie.tmdbId}", e)
                // Save null to database to avoid repeated failed attempts
                movieRepository.updateMovieLogo(movie.tmdbId, null)
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