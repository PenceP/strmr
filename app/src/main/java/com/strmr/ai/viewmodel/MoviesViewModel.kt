package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TmdbMovie
import com.strmr.ai.ui.theme.StrmrConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MoviesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class MovieRowState(
    val movies: List<MovieRowItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val currentPage: Int = 1,
    val error: String? = null,
)

data class MovieRowItem(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float?,
    val releaseDate: String?,
    val overview: String?,
)

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val tmdbApiService: TmdbApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    private val _trendingMovies = MutableStateFlow(MovieRowState())
    val trendingMovies: StateFlow<MovieRowState> = _trendingMovies.asStateFlow()

    private val _popularMovies = MutableStateFlow(MovieRowState())
    val popularMovies: StateFlow<MovieRowState> = _popularMovies.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load first pages of both trending and popular
                loadTrendingMovies(reset = true)
                loadPopularMovies(reset = true)
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error loading initial data", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load movies: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun loadTrendingMovies(reset: Boolean = false) {
        if (_trendingMovies.value.isLoading) return

        viewModelScope.launch {
            try {
                val currentState = _trendingMovies.value
                val page = if (reset) 1 else currentState.currentPage + 1

                _trendingMovies.value = currentState.copy(isLoading = true, error = null)

                val response = tmdbApiService.getTrendingMovies(page = page)
                val newMovies = response.results.map { tmdbMovie ->
                    tmdbMovie.toMovieRowItem()
                }

                val updatedMovies = if (reset) {
                    newMovies
                } else {
                    currentState.movies + newMovies
                }

                _trendingMovies.value = MovieRowState(
                    movies = updatedMovies,
                    isLoading = false,
                    hasMore = page < response.total_pages,
                    currentPage = page,
                    error = null
                )

                Log.d("MoviesViewModel", "🎬 Loaded trending movies page $page: ${newMovies.size} movies")
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error loading trending movies", e)
                _trendingMovies.value = _trendingMovies.value.copy(
                    isLoading = false,
                    error = "Failed to load trending movies: ${e.message}"
                )
            }
        }
    }

    fun loadPopularMovies(reset: Boolean = false) {
        if (_popularMovies.value.isLoading) return

        viewModelScope.launch {
            try {
                val currentState = _popularMovies.value
                val page = if (reset) 1 else currentState.currentPage + 1

                _popularMovies.value = currentState.copy(isLoading = true, error = null)

                val response = tmdbApiService.getPopularMovies(page = page)
                val newMovies = response.results.map { tmdbMovie ->
                    tmdbMovie.toMovieRowItem()
                }

                val updatedMovies = if (reset) {
                    newMovies
                } else {
                    currentState.movies + newMovies
                }

                _popularMovies.value = MovieRowState(
                    movies = updatedMovies,
                    isLoading = false,
                    hasMore = page < response.total_pages,
                    currentPage = page,
                    error = null
                )

                Log.d("MoviesViewModel", "🎬 Loaded popular movies page $page: ${newMovies.size} movies")
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error loading popular movies", e)
                _popularMovies.value = _popularMovies.value.copy(
                    isLoading = false,
                    error = "Failed to load popular movies: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        _trendingMovies.value = _trendingMovies.value.copy(error = null)
        _popularMovies.value = _popularMovies.value.copy(error = null)
    }
}

private fun TmdbMovie.toMovieRowItem(): MovieRowItem {
    return MovieRowItem(
        id = id,
        title = title,
        posterUrl = poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
        backdropUrl = backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
        rating = vote_average,
        releaseDate = release_date,
        overview = overview
    )
}