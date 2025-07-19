package com.strmr.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val omdbRepository: OmdbRepository
) : ViewModel() {
    
    private val _movie = MutableStateFlow<MovieEntity?>(null)
    val movie = _movie.asStateFlow()
    
    private val _tvShow = MutableStateFlow<TvShowEntity?>(null)
    val tvShow = _tvShow.asStateFlow()
    
    fun loadMovie(tmdbId: Int) {
        viewModelScope.launch {
            _movie.value = movieRepository.getMovieByTmdbId(tmdbId)
        }
    }
    
    fun loadTvShow(tmdbId: Int) {
        viewModelScope.launch {
            _tvShow.value = tvShowRepository.getTvShowByTmdbId(tmdbId)
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
} 