package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val homeRepository: HomeRepository,
    private val omdbRepository: OmdbRepository
) : ViewModel() {

    private val _continueWatching = MutableStateFlow<List<HomeMediaItem>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    private val _networks = MutableStateFlow<List<NetworkInfo>>(emptyList())
    val networks = _networks.asStateFlow()

    private val _isContinueWatchingLoading = MutableStateFlow(true)
    val isContinueWatchingLoading = _isContinueWatchingLoading.asStateFlow()

    private val _isNetworksLoading = MutableStateFlow(true)
    val isNetworksLoading = _isNetworksLoading.asStateFlow()

    init {
        observeContinueWatching()
        refreshContinueWatching()
        loadNetworks()
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            homeRepository.getContinueWatching().collectLatest { continueWatchingEntities ->
                _isContinueWatchingLoading.value = false
                val mappedItems = continueWatchingEntities
                    .filter { entity ->
                        // Include items with progress between 1-95% OR next episodes (no progress but has next episode)
                        (entity.progress != null && entity.progress in 1f..95f) || entity.isNextEpisode
                    }
                    .mapNotNull { entity ->
                        when (entity.type) {
                            "movie" -> {
                                entity.movieTmdbId?.let { tmdbId ->
                                    val movieEntity = movieRepository.getOrFetchMovieWithLogo(tmdbId)
                                    var altBackdropUrl: String? = null
                                    if (movieEntity != null) {
                                        try {
                                            val images = movieRepository.getMovieImages(tmdbId)
                                            altBackdropUrl = images?.backdrops?.getOrNull(1)?.file_path?.let { path -> "https://image.tmdb.org/t/p/w780$path" }
                                        } catch (_: Exception) {}
                                    }
                                    movieEntity?.let { movieEnt ->
                                        HomeMediaItem.Movie(movieEnt, entity.progress, altBackdropUrl)
                                    }
                                }
                            }
                            "episode" -> {
                                entity.showTmdbId?.let { showTmdbId ->
                                    tvShowRepository.getOrFetchTvShowWithLogo(showTmdbId)?.let { tvShowEntity ->
                                        var episodeImageUrl: String? = null
                                        var episodeOverview: String? = null
                                        var episodeAirDate: String? = null
                                        val season = entity.episodeSeason
                                        val episode = entity.episodeNumber
                                        
                                        Log.d("HomeViewModel", "🎬 Continue watching item: ${tvShowEntity.title} S${season}E${episode} (${if (entity.isNextEpisode) "next episode" else "in progress"})")
                                        
                                        if (season != null && episode != null) {
                                            try {
                                                val episodeDetails = tvShowRepository.getEpisodeDetails(showTmdbId, season, episode)
                                                episodeImageUrl = episodeDetails?.still_path?.let { "https://image.tmdb.org/t/p/w780$it" }
                                                episodeOverview = episodeDetails?.overview
                                                // Get episode air date from the database
                                                val episodeEntity = tvShowRepository.getEpisodeByDetails(showTmdbId, season, episode)
                                                episodeAirDate = episodeEntity?.airDate
                                            } catch (_: Exception) {}
                                        }
                                        HomeMediaItem.TvShow(
                                            show = tvShowEntity, 
                                            progress = entity.progress, 
                                            episodeImageUrl = episodeImageUrl, 
                                            season = season, 
                                            episode = episode, 
                                            episodeOverview = episodeOverview, 
                                            episodeAirDate = episodeAirDate,
                                            isNextEpisode = entity.isNextEpisode
                                        )
                                    }
                                }
                            }
                            else -> null
                        }
                    }
                _continueWatching.value = mappedItems
            }
        }
    }

    fun refreshContinueWatching() {
        viewModelScope.launch {
            try {
                homeRepository.refreshContinueWatching(accountRepository)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    private fun loadNetworks() {
        val config = homeRepository.getHomeConfig()
        val networkSection = config?.homePage?.rows?.find { it.id == "networks" }
        _networks.value = networkSection?.networks ?: emptyList()
        _isNetworksLoading.value = false
    }

    suspend fun fetchAndCacheMovieLogo(tmdbId: Int): Boolean {
        val updated = movieRepository.getOrFetchMovieWithLogo(tmdbId)
        return updated?.logoUrl?.isNotBlank() == true
    }

    suspend fun fetchAndCacheTvShowLogo(tmdbId: Int): Boolean {
        Log.d("HomeViewModel", "🎬 Attempting to fetch logo for tvShow tmdbId=$tmdbId")
        val updated = tvShowRepository.getOrFetchTvShowWithLogo(tmdbId)
        val hasLogo = updated?.logoUrl?.isNotBlank() == true
        Log.d("HomeViewModel", "🎬 Logo fetch result for tvShow tmdbId=$tmdbId: hasLogo=$hasLogo, logoUrl=${updated?.logoUrl}")
        return hasLogo
    }
    
    suspend fun getOmdbRatings(imdbId: String) = omdbRepository.getOmdbRatings(imdbId)
} 