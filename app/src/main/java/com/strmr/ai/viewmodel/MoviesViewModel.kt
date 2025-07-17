package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TraktRatingsEntity
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
class MoviesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val fetchLogoUseCase: FetchLogoUseCase,
    private val omdbRepository: OmdbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState<MovieEntity>())
    val uiState = _uiState.asStateFlow()
    
    private val _pagingUiState = MutableStateFlow(PagingUiState<MovieEntity>())
    val pagingUiState = _pagingUiState.asStateFlow()

    private var initialLoadComplete = false

    init {
        setupPaging()
        loadMovies()
        refreshAllMovies()
    }
    
    private fun setupPaging() {
        _pagingUiState.value = PagingUiState(
            mediaRows = mapOf(
                "Trending Movies" to movieRepository.getTrendingMoviesPager().cachedIn(viewModelScope),
                "Popular Movies" to movieRepository.getPopularMoviesPager().cachedIn(viewModelScope)
            )
        )
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

    suspend fun getOmdbRatings(imdbId: String): OmdbResponse? = omdbRepository.getOmdbRatings(imdbId)

    suspend fun getTraktRatings(traktId: Int): TraktRatingsEntity? = movieRepository.getTraktRatings(traktId)

    suspend fun clearNullLogos() = movieRepository.clearNullLogos()

    private fun fetchAndUpdateLogo(movie: MovieEntity) {
        viewModelScope.launch {
            Log.d("MoviesViewModel", "🎯 Fetching logo for '${movie.title}' (TMDB: ${movie.tmdbId})")
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(movie.tmdbId, MediaType.MOVIE)
            movieRepository.updateMovieLogo(movie.tmdbId, logoUrl)
        }
    }
}