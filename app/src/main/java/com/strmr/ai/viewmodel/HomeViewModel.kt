package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.*
import com.strmr.ai.domain.usecase.GetContinueWatchingUseCase
import com.strmr.ai.domain.usecase.CheckTraktAuthorizationUseCase
import com.strmr.ai.domain.usecase.FetchMediaLogoUseCase
import com.strmr.ai.presentation.state.HomeUiState
import com.strmr.ai.presentation.state.ContinueWatchingState
import com.strmr.ai.presentation.state.TraktListsState
import com.strmr.ai.presentation.state.TraktAuthState
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
    private val omdbRepository: OmdbRepository,
    // Clean architecture use cases (optional for gradual migration)
    private val getContinueWatchingUseCase: GetContinueWatchingUseCase,
    private val checkTraktAuthorizationUseCase: CheckTraktAuthorizationUseCase,
    private val fetchMediaLogoUseCase: FetchMediaLogoUseCase
) : ViewModel() {

    private val _continueWatching = MutableStateFlow<List<HomeMediaItem>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    private val _networks = MutableStateFlow<List<com.strmr.ai.data.NetworkInfo>>(emptyList())
    val networks = _networks.asStateFlow()

    private val _isContinueWatchingLoading = MutableStateFlow(true)
    val isContinueWatchingLoading = _isContinueWatchingLoading.asStateFlow()

    private val _isNetworksLoading = MutableStateFlow(true)
    val isNetworksLoading = _isNetworksLoading.asStateFlow()

    private val _isTraktAuthorized = MutableStateFlow(false)
    val isTraktAuthorized = _isTraktAuthorized.asStateFlow()

    private val _traktLists = MutableStateFlow<List<HomeMediaItem.Collection>>(emptyList())
    val traktLists = _traktLists.asStateFlow()

    private val _isTraktListsLoading = MutableStateFlow(false)
    val isTraktListsLoading = _isTraktListsLoading.asStateFlow()

    init {
        observeContinueWatching()
        refreshContinueWatching()
        loadNetworks()
        // Don't load Trakt lists in init - wait for authorization
        // loadTraktLists()
    }

    private fun observeContinueWatching() {
        viewModelScope.launch {
            homeRepository.getContinueWatching().collectLatest { continueWatchingEntities ->
                _isContinueWatchingLoading.value = false
                Log.d("HomeViewModel", "🔍 Received ${continueWatchingEntities.size} entities from database")
                
                val mappedItems = continueWatchingEntities
                    .filter { entity ->
                        val hasValidProgress = entity.progress != null && entity.progress in 1f..95f
                        val isNext = entity.isNextEpisode
                        val shouldInclude = hasValidProgress || isNext
                        
                        Log.d("HomeViewModel", "Filtering ${entity.movieTitle ?: entity.showTitle}: progress=${entity.progress}, isNext=$isNext, include=$shouldInclude")
                        shouldInclude
                    }
                    .mapNotNull { entity ->
                        when (entity.type) {
                            "movie" -> {
                                entity.movieTmdbId?.let { tmdbId ->
                                    try {
                                        val movieEntity = movieRepository.getOrFetchMovieWithLogo(tmdbId)
                                        var altBackdropUrl: String? = null
                                        if (movieEntity != null) {
                                            // Move image fetching to background to avoid blocking
                                            try {
                                                val images = movieRepository.getMovieImages(tmdbId)
                                                altBackdropUrl = images?.backdrops?.getOrNull(1)?.file_path?.let { path -> "https://image.tmdb.org/t/p/w780$path" }
                                            } catch (e: Exception) {
                                                Log.w("HomeViewModel", "Failed to load alt backdrop for movie $tmdbId", e)
                                            }
                                        }
                                        movieEntity?.let { movieEnt ->
                                            HomeMediaItem.Movie(movieEnt, entity.progress, altBackdropUrl)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("HomeViewModel", "Failed to load movie $tmdbId", e)
                                        null
                                    }
                                }
                            }
                            "episode" -> {
                                entity.showTmdbId?.let { showTmdbId ->
                                    try {
                                        tvShowRepository.getOrFetchTvShowWithLogo(showTmdbId)?.let { tvShowEntity ->
                                            var episodeImageUrl: String? = null
                                            var episodeOverview: String? = null
                                            var episodeAirDate: String? = null
                                            val season = entity.episodeSeason
                                            val episode = entity.episodeNumber
                                            
                                            Log.d("HomeViewModel", "🎬 Continue watching item: ${tvShowEntity.title} S${season}E${episode} (${if (entity.isNextEpisode) "next episode" else "in progress"})")
                                            
                                            if (season != null && episode != null) {
                                                try {
                                                    // Batch episode data fetching for better performance
                                                    val episodeDetails = tvShowRepository.getEpisodeDetails(showTmdbId, season, episode)
                                                    episodeImageUrl = episodeDetails?.still_path?.let { "https://image.tmdb.org/t/p/w780$it" }
                                                    episodeOverview = episodeDetails?.overview
                                                    // Get episode air date from the database
                                                    val episodeEntity = tvShowRepository.getEpisodeByDetails(showTmdbId, season, episode)
                                                    episodeAirDate = episodeEntity?.airDate
                                                } catch (e: Exception) {
                                                    Log.w("HomeViewModel", "Failed to load episode details for $showTmdbId S${season}E${episode}", e)
                                                }
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
                                    } catch (e: Exception) {
                                        Log.e("HomeViewModel", "Failed to load TV show $showTmdbId", e)
                                        null
                                    }
                                }
                            }
                            else -> null
                        }
                    }
                Log.d("HomeViewModel", "🔍 After filtering: ${continueWatchingEntities.filter { entity -> (entity.progress != null && entity.progress in 1f..95f) || entity.isNextEpisode }.size} items remain")
                Log.d("HomeViewModel", "🔍 After mapping: ${mappedItems.size} items final")
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
        _networks.value = networkSection?.networks ?: emptyList<com.strmr.ai.data.NetworkInfo>()
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
    
    private fun checkTraktAuthorization() {
        viewModelScope.launch {
            try {
                val isAuthorized = accountRepository.isAccountValid("trakt")
                _isTraktAuthorized.value = isAuthorized
                Log.d("HomeViewModel", "🔑 Trakt authorization status: $isAuthorized")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error checking Trakt authorization", e)
                _isTraktAuthorized.value = false
            }
        }
    }
    
    fun refreshTraktAuthorization() {
        checkTraktAuthorization()
    }
    
    private fun loadTraktLists() {
        viewModelScope.launch {
            try {
                Log.d("HomeViewModel", "🔄 Starting loadTraktLists()")
                _isTraktListsLoading.value = true
                
                // Check if user is authorized first
                val isAuthorized = accountRepository.isAccountValid("trakt")
                _isTraktAuthorized.value = isAuthorized
                Log.d("HomeViewModel", "🔑 Trakt authorization check result: $isAuthorized")
                
                if (isAuthorized) {
                    // Create the Trakt Lists collection items from HOME.json configuration
                    val traktCollections = listOf(
                        HomeMediaItem.Collection(
                            id = "movie_collection",
                            name = "Movie Collection",
                            backgroundImageUrl = "drawable://trakt_likedlist",
                            nameDisplayMode = "Visible",
                            dataUrl = "https://api.trakt.tv/sync/collection/movies"
                        ),
                        HomeMediaItem.Collection(
                            id = "movie_watchlist", 
                            name = "Movie Watchlist",
                            backgroundImageUrl = "drawable://trakt_watchlist",
                            nameDisplayMode = "Visible",
                            dataUrl = "https://api.trakt.tv/sync/watchlist/movies"
                        ),
                        HomeMediaItem.Collection(
                            id = "tv_collection",
                            name = "TV Collection", 
                            backgroundImageUrl = "drawable://trakt_likedlist",
                            nameDisplayMode = "Visible",
                            dataUrl = "https://api.trakt.tv/sync/collection/shows"
                        ),
                        HomeMediaItem.Collection(
                            id = "tv_watchlist",
                            name = "TV Watchlist",
                            backgroundImageUrl = "drawable://trakt_watchlist", 
                            nameDisplayMode = "Visible",
                            dataUrl = "https://api.trakt.tv/sync/watchlist/shows"
                        )
                    )
                    
                    _traktLists.value = traktCollections
                    Log.d("HomeViewModel", "✅ Loaded ${traktCollections.size} Trakt lists")
                } else {
                    _traktLists.value = emptyList()
                    Log.d("HomeViewModel", "🔒 Trakt not authorized - no lists loaded")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "❌ Error loading Trakt lists", e)
                _traktLists.value = emptyList()
                _isTraktAuthorized.value = false
            } finally {
                _isTraktListsLoading.value = false
            }
        }
    }
    
    fun refreshTraktLists() {
        Log.d("HomeViewModel", "🔄 refreshTraktLists() called - reloading Trakt lists")
        loadTraktLists()
    }

    // =================
    // CLEAN ARCHITECTURE METHODS
    // =================

    // Clean architecture unified UI state
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    /**
     * Load continue watching using clean architecture
     * This demonstrates improved architecture with proper error handling
     */
    fun loadContinueWatchingWithCleanArchitecture() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "🏗️ Loading continue watching with clean architecture")
            
            _uiState.value = _uiState.value.copy(
                continueWatching = ContinueWatchingState.Loading
            )
            
            getContinueWatchingUseCase.refreshContinueWatching()
                .onSuccess { items ->
                    Log.d("HomeViewModel", "✅ Successfully loaded ${items.size} continue watching items")
                    _uiState.value = _uiState.value.copy(
                        continueWatching = ContinueWatchingState.Success(items)
                    )
                }
                .onFailure { exception ->
                    Log.e("HomeViewModel", "❌ Failed to load continue watching", exception)
                    _uiState.value = _uiState.value.copy(
                        continueWatching = ContinueWatchingState.Error(
                            exception.message ?: "Failed to load continue watching"
                        )
                    )
                }
        }
    }

    /**
     * Check Trakt authorization using clean architecture
     */
    fun checkTraktAuthorizationWithCleanArchitecture() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "🏗️ Checking Trakt authorization with clean architecture")
            
            _uiState.value = _uiState.value.copy(
                traktAuthorization = TraktAuthState.Checking
            )
            
            checkTraktAuthorizationUseCase()
                .onSuccess { isAuthorized ->
                    Log.d("HomeViewModel", "✅ Trakt authorization check: $isAuthorized")
                    _uiState.value = _uiState.value.copy(
                        traktAuthorization = if (isAuthorized) {
                            TraktAuthState.Authorized
                        } else {
                            TraktAuthState.NotAuthorized
                        }
                    )
                    
                    // If authorized, load Trakt lists
                    if (isAuthorized) {
                        loadTraktListsWithCleanArchitecture()
                    }
                }
                .onFailure { exception ->
                    Log.e("HomeViewModel", "❌ Failed to check Trakt authorization", exception)
                    _uiState.value = _uiState.value.copy(
                        traktAuthorization = TraktAuthState.NotAuthorized
                    )
                }
        }
    }

    /**
     * Load Trakt lists using clean architecture
     */
    private fun loadTraktListsWithCleanArchitecture() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "🏗️ Loading Trakt lists with clean architecture")
            
            _uiState.value = _uiState.value.copy(
                traktLists = TraktListsState.Loading
            )
            
            // TODO: Implement TraktListsUseCase when needed
            // For now, just set success with empty list
            _uiState.value = _uiState.value.copy(
                traktLists = TraktListsState.Success(emptyList())
            )
        }
    }

    /**
     * Refresh all data using clean architecture
     */
    fun refreshAllWithCleanArchitecture() {
        Log.d("HomeViewModel", "🔄 Refreshing all data with clean architecture")
        loadContinueWatchingWithCleanArchitecture()
        checkTraktAuthorizationWithCleanArchitecture()
    }

    /**
     * Clear error for a specific section
     */
    fun clearError(section: String) {
        when (section) {
            "continue_watching" -> {
                if (_uiState.value.continueWatching is ContinueWatchingState.Error) {
                    _uiState.value = _uiState.value.copy(
                        continueWatching = ContinueWatchingState.Success(emptyList())
                    )
                }
            }
            "trakt_lists" -> {
                if (_uiState.value.traktLists is TraktListsState.Error) {
                    _uiState.value = _uiState.value.copy(
                        traktLists = TraktListsState.NotLoaded
                    )
                }
            }
        }
        Log.d("HomeViewModel", "🧹 Cleared error for section: $section")
    }
} 