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
import android.content.Context
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val fetchLogoUseCase: FetchLogoUseCase,
    private val omdbRepository: OmdbRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState<MovieEntity>())
    val uiState = _uiState.asStateFlow()
    
    private val _pagingUiState = MutableStateFlow(PagingUiState<MovieEntity>())
    val pagingUiState = _pagingUiState.asStateFlow()

    private var initialLoadComplete = false
    private var pageConfiguration: PageConfiguration? = null
    private val _traktListMovies = MutableStateFlow<Map<String, List<MovieEntity>>>(emptyMap())
    val traktListMovies = _traktListMovies.asStateFlow()

    init {
        loadConfiguration()
        setupPaging()
        loadMovies()
        refreshAllMovies()
    }
    
    private fun loadConfiguration() {
        viewModelScope.launch {
            try {
                val configLoader = ConfigurationLoader(context)
                pageConfiguration = configLoader.loadPageConfiguration("MOVIES")
                pageConfiguration?.let { config ->
                    loadTraktLists(config)
                }
            } catch (e: Exception) {
                Log.e("MoviesViewModel", "Error loading configuration", e)
            }
        }
    }

    private fun setupPaging() {
        _pagingUiState.value = PagingUiState(
            mediaRows = mapOf(
                "Trending" to movieRepository.getTrendingMoviesPager().cachedIn(viewModelScope),
                "Popular" to movieRepository.getPopularMoviesPager().cachedIn(viewModelScope)
            )
        )
    }

    private fun loadTraktLists(config: PageConfiguration) {
        viewModelScope.launch {
            val traktRows = config.rows.filter { it.type == "trakt_list" }
            val traktListData = mutableMapOf<String, List<MovieEntity>>()
            
            for (row in traktRows) {
                try {
                    val traktConfig = row.traktConfig
                    if (traktConfig != null) {
                        Log.d("MoviesViewModel", "Loading Trakt list: ${row.title}")
                        val movies = movieRepository.getTraktListMovies(
                            traktConfig.username,
                            traktConfig.listSlug
                        )
                        traktListData[row.title] = movies
                        Log.d("MoviesViewModel", "Loaded ${movies.size} movies for ${row.title}")
                    }
                } catch (e: Exception) {
                    Log.e("MoviesViewModel", "Error loading Trakt list ${row.title}", e)
                }
            }
            
            _traktListMovies.value = traktListData
        }
    }

    private fun loadMovies() {
        viewModelScope.launch {
            _uiState.value = UiState(isLoading = true)
            var firstDbEmission = false
            combine(
                movieRepository.getTrendingMovies(),
                movieRepository.getPopularMovies(),
                _traktListMovies
            ) { trending, popular, traktLists ->
                val mediaRows = mutableMapOf<String, List<MovieEntity>>()
                
                // Add rows based on configuration order
                pageConfiguration?.let { config ->
                    val enabledRows = config.rows.filter { it.enabled }.sortedBy { it.order }
                    for (row in enabledRows) {
                        when (row.title) {
                            "Trending" -> mediaRows["Trending"] = trending
                            "Popular" -> mediaRows["Popular"] = popular
                            else -> {
                                // Check if it's a Trakt list
                                traktLists[row.title]?.let { movies ->
                                    mediaRows[row.title] = movies
                                }
                            }
                        }
                    }
                } ?: run {
                    // Fallback if no configuration
                    mediaRows["Trending"] = trending
                    mediaRows["Popular"] = popular
                    mediaRows.putAll(traktLists)
                }
                
                UiState(
                    isLoading = !initialLoadComplete || !firstDbEmission,
                    mediaRows = mediaRows
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

    fun fetchAndUpdateLogo(movie: MovieEntity) {
        viewModelScope.launch {
            Log.d("MoviesViewModel", "🎯 Fetching logo for '${movie.title}' (TMDB: ${movie.tmdbId})")
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(movie.tmdbId, MediaType.MOVIE)
            movieRepository.updateMovieLogo(movie.tmdbId, logoUrl)
        }
    }
}