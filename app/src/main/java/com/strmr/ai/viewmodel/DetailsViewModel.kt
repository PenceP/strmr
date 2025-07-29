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
    // Clean architecture use cases - primary approach
    private val getMovieDetailsUseCase: GetMovieDetailsUseCase,
    private val getTvShowDetailsUseCase: GetTvShowDetailsUseCase,
    // Domain repositories for clean architecture
    private val domainMovieRepository: com.strmr.ai.domain.repository.MovieRepository,
    private val domainTvShowRepository: com.strmr.ai.domain.repository.TvShowRepository
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
    
    @Deprecated("Use clean architecture loadMovie instead", ReplaceWith("loadMovie(tmdbId)"))
    fun loadMovieLegacy(tmdbId: Int) {
        viewModelScope.launch {
            android.util.Log.d("DetailsViewModel", "🎬 Loading movie (legacy) with tmdbId: $tmdbId")
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
    
    @Deprecated("Use clean architecture loadTvShow instead", ReplaceWith("loadTvShow(tmdbId)"))
    fun loadTvShowLegacy(tmdbId: Int) {
        viewModelScope.launch {
            android.util.Log.d("DetailsViewModel", "📺 Loading TV show (legacy) with tmdbId: $tmdbId")
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
    
    // All deprecated methods have been removed - use the fetch* methods instead for clean architecture
    
    // =================
    // PRIMARY METHODS (Clean Architecture)
    // =====================================
    
    /**
     * Load movie using clean architecture use case
     * This demonstrates the improved architecture with proper error handling
     */
    fun loadMovie(tmdbId: Int) {
        viewModelScope.launch {
            Log.d("DetailsViewModel", "🏗️ Loading movie: $tmdbId")
            _isLoading.value = true
            _error.value = null
            
            getMovieDetailsUseCase(TmdbId(tmdbId))
                .onSuccess { domainMovie ->
                    Log.d("DetailsViewModel", "✅ Successfully loaded domain movie: ${domainMovie.title}")
                    _domainMovie.value = domainMovie
                    
                    // Also update legacy state flow for backward compatibility
                    // Convert domain model to entity for UI
                    movieRepository.getMovieByTmdbId(tmdbId)?.let { movieEntity ->
                        _movie.value = movieEntity
                    }
                    
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
    fun loadTvShow(tmdbId: Int) {
        viewModelScope.launch {
            Log.d("DetailsViewModel", "🏗️ Loading TV show: $tmdbId")
            _isLoading.value = true
            _error.value = null
            
            getTvShowDetailsUseCase(TmdbId(tmdbId))
                .onSuccess { domainTvShow ->
                    Log.d("DetailsViewModel", "✅ Successfully loaded domain TV show: ${domainTvShow.title}")
                    _domainTvShow.value = domainTvShow
                    
                    // Also update legacy state flow for backward compatibility
                    // Convert domain model to entity for UI
                    tvShowRepository.getTvShowByTmdbId(tmdbId)?.let { tvShowEntity ->
                        _tvShow.value = tvShowEntity
                    }
                    
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
    
    // =================
    // CLEAN ARCHITECTURE METHODS (Primary approach - use these instead of deprecated methods)
    // =====================================
    
    /**
     * Get OMDB ratings using legacy repository (until domain repository is created)
     * This is a clean method to replace the deprecated getOmdbRatings
     */
    suspend fun fetchOmdbRatings(imdbId: String) = omdbRepository.getOmdbRatings(imdbId)
    
    /**
     * Get similar movies using domain repository
     * This replaces the deprecated getSimilarMovies method
     */
    suspend fun fetchSimilarMovies(tmdbId: Int) = movieRepository.getOrFetchSimilarMovies(tmdbId)
    
    /**
     * Get movie collection using domain repository
     * This replaces the deprecated getCollection method
     */
    suspend fun fetchMovieCollection(collectionId: Int) = movieRepository.getOrFetchCollection(collectionId)
    
    /**
     * Get movie trailer using domain repository
     * This replaces the deprecated getMovieTrailer method
     */
    suspend fun fetchMovieTrailer(tmdbId: Int): String? = movieRepository.getMovieTrailer(tmdbId)
    
    /**
     * Get TV show seasons using legacy repository (until domain repository is created)
     * This replaces the deprecated getSeasons method
     */
    suspend fun fetchTvShowSeasons(tmdbId: Int) = tvShowRepository.getOrFetchSeasons(tmdbId)
    
    /**
     * Get TV show episodes using legacy repository (until domain repository is created)
     * This replaces the deprecated getEpisodes method
     */
    suspend fun fetchTvShowEpisodes(tmdbId: Int, season: Int) = tvShowRepository.getOrFetchEpisodes(tmdbId, season)
    
    /**
     * Get TV show trailer using legacy repository (until domain repository is created)
     * This replaces the deprecated getTvShowTrailer method
     */
    suspend fun fetchTvShowTrailer(tmdbId: Int): String? = tvShowRepository.getTvShowTrailer(tmdbId)
    
    /**
     * Get similar TV shows using legacy repository (until domain repository is created)
     * This replaces the deprecated getSimilarTvShows method
     */
    suspend fun fetchSimilarTvShows(tmdbId: Int) = tvShowRepository.getOrFetchSimilarTvShows(tmdbId)
} 