package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val accountRepository: AccountRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _continueWatching = MutableStateFlow<List<HomeMediaItem>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    private val _networks = MutableStateFlow<List<NetworkInfo>>(emptyList())
    val networks = _networks.asStateFlow()

    init {
        observeContinueWatching()
        refreshContinueWatching()
        loadNetworks()
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            homeRepository.getContinueWatching().collectLatest { playbackEntities ->
                val mappedItems = playbackEntities
                    .filter { it.progress in 1f..95f }
                    .mapNotNull { playbackEntity ->
                        when (playbackEntity.type) {
                            "movie" -> {
                                playbackEntity.movieTmdbId?.let { tmdbId ->
                                    val movieEntity = movieRepository.getOrFetchMovieWithLogo(tmdbId)
                                    var altBackdropUrl: String? = null
                                    if (movieEntity != null) {
                                        try {
                                            val images = movieRepository.getMovieImages(tmdbId)
                                            altBackdropUrl = images?.backdrops?.getOrNull(1)?.file_path?.let { path -> "https://image.tmdb.org/t/p/w780$path" }
                                        } catch (_: Exception) {}
                                    }
                                    movieEntity?.let { entity ->
                                        HomeMediaItem.Movie(entity, playbackEntity.progress, altBackdropUrl)
                                    }
                                }
                            }
                            "episode" -> {
                                playbackEntity.showTmdbId?.let { showTmdbId ->
                                    tvShowRepository.getOrFetchTvShowWithLogo(showTmdbId)?.let { tvShowEntity ->
                                        var episodeImageUrl: String? = null
                                        var episodeOverview: String? = null
                                        var episodeAirDate: String? = null
                                        val season = playbackEntity.episodeSeason
                                        val episode = playbackEntity.episodeNumber
                                        
                                        // Debug logging for episode data
                                        Log.d("HomeViewModel", "🎯 DEBUG: PlaybackEntity for ${tvShowEntity.title}: season=$season, episode=$episode")
                                        Log.d("HomeViewModel", "🎯 DEBUG: PlaybackEntity type: ${playbackEntity.type}")
                                        Log.d("HomeViewModel", "🎯 DEBUG: PlaybackEntity showTmdbId: ${playbackEntity.showTmdbId}")
                                        Log.d("HomeViewModel", "🎯 DEBUG: PlaybackEntity episodeSeason: ${playbackEntity.episodeSeason}")
                                        Log.d("HomeViewModel", "🎯 DEBUG: PlaybackEntity episodeNumber: ${playbackEntity.episodeNumber}")
                                        
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
                                        HomeMediaItem.TvShow(tvShowEntity, playbackEntity.progress, episodeImageUrl, season, episode, episodeOverview, episodeAirDate)
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
} 