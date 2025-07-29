package com.strmr.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import com.strmr.ai.domain.model.Movie as DomainMovie
import com.strmr.ai.domain.model.TvShow as DomainTvShow
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.usecase.GetMovieDetailsUseCase
import com.strmr.ai.domain.usecase.GetTvShowDetailsUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val omdbRepository: OmdbRepository,
    // Clean architecture use cases (optional for gradual migration)
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val getTvShowDetailsUseCase: GetTvShowDetailsUseCase
) : ViewModel() {
    
    // Legacy entity state flows (for backward compatibility)
    private val _movie = MutableStateFlow<MovieEntity?>(null)
    val movie = _movie.asStateFlow()
    
    private val _tvShow = MutableStateFlow<TvShowEntity?>(null)
    val tvShow = _tvShow.asStateFlow()
    
    // Clean architecture domain model state flows
    private val _domainMovie = MutableStateFlow<DomainMovie?>(null)
    val domainMovie = _domainMovie.asStateFlow()
    
    private val _domainTvShow = MutableStateFlow<DomainTvShow?>(null)
    val domainTvShow = _domainTvShow.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    
    fun loadMovie(tmdbId: Int) {
        viewModelScope.launch {
            android.util.Log.d("DetailsViewModel", "🎬 Loading movie with tmdbId: $tmdbId")
            var movieEntity = movieRepository.getMovieByTmdbId(tmdbId)
            
            // If movie not in database, fetch it
            if (movieEntity == null) {
                android.util.Log.d("DetailsViewModel", "🌐 Movie not in database, fetching from API")
                movieEntity = movieRepository.getOrFetchMovie(tmdbId)
            }
            
            android.util.Log.d("DetailsViewModel", "🎬 Movie loaded: ${movieEntity?.title ?: "null"}")
            _movie.value = movieEntity
        }
    }
    
    fun loadTvShow(tmdbId: Int) {
        viewModelScope.launch {
            android.util.Log.d("DetailsViewModel", "📺 Loading TV show with tmdbId: $tmdbId")
            var tvShowEntity = tvShowRepository.getTvShowByTmdbId(tmdbId)
            
            // If TV show not in database, fetch it
            if (tvShowEntity == null) {
                android.util.Log.d("DetailsViewModel", "🌐 TV show not in database, fetching from API")
                tvShowEntity = tvShowRepository.getOrFetchTvShow(tmdbId)
            }
            
            android.util.Log.d("DetailsViewModel", "📺 TV show loaded: ${tvShowEntity?.title ?: "null"}")
            _tvShow.value = tvShowEntity
        }
    }
    
    suspend fun getOmdbRatings(imdbId: String) = omdbRepository.getOmdbRatings(imdbId)
    
    suspend fun getSimilarMovies(tmdbId: Int) = movieRepository.getOrFetchSimilarMovies(tmdbId)
    
    suspend fun getSimilarTvShows(tmdbId: Int) = tvShowRepository.getOrFetchSimilarTvShows(tmdbId)
    
    suspend fun getCollection(collectionId: Int) = movieRepository.getOrFetchCollection(collectionId)
    
    suspend fun getSeasons(tmdbId: Int) = tvShowRepository.getOrFetchSeasons(tmdbId)
    
    suspend fun getEpisodes(tmdbId: Int, season: Int) = tvShowRepository.getOrFetchEpisodes(tmdbId, season)
    
    suspend fun getMovieTrailer(tmdbId: Int): String? = movieRepository.getMovieTrailer(tmdbId)
    
    suspend fun getTvShowTrailer(tmdbId: Int): String? = tvShowRepository.getTvShowTrailer(tmdbId)
    
    // =================
    // CLEAN ARCHITECTURE METHODS
    // =================
    
    /**
     * Load movie using clean architecture use case
     * This demonstrates the improved architecture with proper error handling
     */
    fun loadMovieWithCleanArchitecture(tmdbId: Int) {
        viewModelScope.launch {
            Log.d("DetailsViewModel", "🏗️ Loading movie with clean architecture: $tmdbId")
            _isLoading.value = true
            _error.value = null
            
            getMovieDetailsUseCase(TmdbId(tmdbId))
                .onSuccess { domainMovie ->
                    Log.d("DetailsViewModel", "✅ Successfully loaded domain movie: ${domainMovie.title}")
                    _domainMovie.value = domainMovie
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    Log.e("DetailsViewModel", "❌ Failed to load movie: ${exception.message}", exception)
                    _error.value = exception.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Load TV show using clean architecture use case
     * This demonstrates the improved architecture with proper error handling
     */
    fun loadTvShowWithCleanArchitecture(tmdbId: Int) {
        viewModelScope.launch {
            Log.d("DetailsViewModel", "🏗️ Loading TV show with clean architecture: $tmdbId")
            _isLoading.value = true
            _error.value = null
            
            getTvShowDetailsUseCase(TmdbId(tmdbId))
                .onSuccess { domainTvShow ->
                    Log.d("DetailsViewModel", "✅ Successfully loaded domain TV show: ${domainTvShow.title}")
                    _domainTvShow.value = domainTvShow
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    Log.e("DetailsViewModel", "❌ Failed to load TV show: ${exception.message}", exception)
                    _error.value = exception.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
} 