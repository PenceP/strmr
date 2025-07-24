package com.strmr.ai.data

import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refactored Generic repository following SOLID principles
 * 
 * Responsibilities:
 * - Orchestrate services (DataSourceService, TmdbEnrichmentService)
 * - Handle API calls and data loading coordination
 * - Manage business logic flow
 * 
 * No longer handles:
 * - Direct database operations (moved to DataSourceService)
 * - TMDB data enrichment (moved to TmdbEnrichmentService)
 * - Low-level database queries (moved to DataSourceService)
 */
@Singleton
class GenericTraktRepository @Inject constructor(
    val database: StrmrDatabase, // Public for ViewModels that need direct access
    private val traktApiService: TraktApiService,
    private val dataSourceService: DataSourceService,
    private val tmdbEnrichmentService: TmdbEnrichmentService,
    private val fetchLogoUseCase: FetchLogoUseCase
) {
    
    companion object {
        private const val TAG = "GenericTraktRepository"
    }
    
    // ======================== DELEGATION TO SERVICES ========================
    
    /**
     * Get movies from any data source - delegates to DataSourceService
     */
    fun getMoviesFromDataSource(config: DataSourceConfig): Flow<List<MovieEntity>> =
        dataSourceService.getMoviesFromDataSource(config)
    
    /**
     * Get TV shows from any data source - delegates to DataSourceService
     */
    fun getTvShowsFromDataSource(config: DataSourceConfig): Flow<List<TvShowEntity>> =
        dataSourceService.getTvShowsFromDataSource(config)
    
    /**
     * Get paging source for movies - delegates to DataSourceService
     */
    fun getMoviesPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, MovieEntity> =
        dataSourceService.getMoviesPagingFromDataSource(config)
    
    /**
     * Get paging source for TV shows - delegates to DataSourceService
     */
    fun getTvShowsPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, TvShowEntity> =
        dataSourceService.getTvShowsPagingFromDataSource(config)
    
    /**
     * Check if movie data source is empty - delegates to DataSourceService
     */
    suspend fun isMovieDataSourceEmpty(config: DataSourceConfig): Boolean =
        dataSourceService.isMovieDataSourceEmpty(config)
    
    /**
     * Check if TV data source is empty - delegates to DataSourceService
     */
    suspend fun isTvDataSourceEmpty(config: DataSourceConfig): Boolean =
        dataSourceService.isTvDataSourceEmpty(config)
    
    /**
     * Get movie data source count - delegates to DataSourceService
     */
    suspend fun getMovieDataSourceCount(config: DataSourceConfig): Int =
        dataSourceService.getMovieDataSourceCount(config)
    
    /**
     * Get TV data source count - delegates to DataSourceService
     */
    suspend fun getTvDataSourceCount(config: DataSourceConfig): Int =
        dataSourceService.getTvDataSourceCount(config)
    
    // ======================== CORE BUSINESS LOGIC ========================
    
    /**
     * Load a specific page for a movie data source
     * Orchestrates API calls, enrichment, and database operations
     */
    suspend fun loadMovieDataSourcePage(config: DataSourceConfig, page: Int): Int {
        return try {
            Log.d(TAG, "📥 Loading page $page for movie data source: ${config.endpoint}")
            
            // Step 1: Fetch data from Trakt API
            val response = fetchMovieDataFromTrakt(config, page)
            if (response.isEmpty()) return 0
            
            // Step 2: Calculate order values
            val pageSize = StrmrConstants.Api.LARGE_PAGE_SIZE
            val baseOrder = (page - 1) * pageSize
            
            // Step 3: Enrich data with TMDB details
            val enrichedEntities = enrichMovieData(response, config, baseOrder)
            if (enrichedEntities.isEmpty()) return 0
            
            // Step 4: Save to database with conflict resolution
            saveMovieEntities(enrichedEntities, config)
            
            Log.d(TAG, "✅ Successfully loaded ${enrichedEntities.size} movies for page $page")
            enrichedEntities.size
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading page $page for ${config.endpoint}", e)
            0
        }
    }
    
    /**
     * Load a specific page for a TV data source
     * Orchestrates API calls, enrichment, and database operations
     */
    suspend fun loadTvDataSourcePage(config: DataSourceConfig, page: Int): Int {
        return try {
            Log.d(TAG, "📥 Loading page $page for TV data source: ${config.endpoint}")
            
            // Step 1: Fetch data from Trakt API
            val response = fetchTvDataFromTrakt(config, page)
            if (response.isEmpty()) return 0
            
            // Step 2: Calculate order values
            val pageSize = StrmrConstants.Api.LARGE_PAGE_SIZE
            val baseOrder = (page - 1) * pageSize
            
            // Step 3: Enrich data with TMDB details
            val enrichedEntities = enrichTvData(response, config, baseOrder)
            if (enrichedEntities.isEmpty()) return 0
            
            // Step 4: Save to database with conflict resolution
            saveTvShowEntities(enrichedEntities, config)
            
            Log.d(TAG, "✅ Successfully loaded ${enrichedEntities.size} TV shows for page $page")
            enrichedEntities.size
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading page $page for ${config.endpoint}", e)
            0
        }
    }
    
    /**
     * Refresh a movie data source (load first page)
     */
    suspend fun refreshMovieDataSource(config: DataSourceConfig) {
        Log.d(TAG, "🔄 Refreshing movie data source: ${config.title}")
        loadMovieDataSourcePage(config, page = 1)
    }
    
    /**
     * Load a limited number of items for onboarding (faster initial load)
     * Fetches full page but only enriches/saves first `limit` items
     */
    suspend fun loadMovieDataSourcePageForOnboarding(
        config: DataSourceConfig, 
        page: Int, 
        limit: Int = 7
    ): Int {
        return try {
            Log.d(TAG, "📥 Loading $limit items for onboarding: ${config.endpoint}")
            
            // Step 1: Fetch data from Trakt API (full page)
            val response = fetchMovieDataFromTrakt(config, page)
            if (response.isEmpty()) return 0
            
            // Step 2: Take only the first `limit` items
            val limitedResponse = response.take(limit)
            
            // Step 3: Calculate order values
            val pageSize = StrmrConstants.Api.LARGE_PAGE_SIZE
            val baseOrder = (page - 1) * pageSize
            
            // Step 4: Enrich limited data with TMDB details
            val enrichedEntities = enrichMovieData(limitedResponse, config, baseOrder)
            if (enrichedEntities.isEmpty()) return 0
            
            // Step 5: Save to database with conflict resolution
            saveMovieEntities(enrichedEntities, config)
            
            Log.d(TAG, "✅ Successfully loaded ${enrichedEntities.size} movies for onboarding")
            enrichedEntities.size
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading onboarding data for ${config.endpoint}", e)
            0
        }
    }
    
    /**
     * Refresh a TV data source (load first page)
     */
    suspend fun refreshTvDataSource(config: DataSourceConfig) {
        Log.d(TAG, "🔄 Refreshing TV data source: ${config.title}")
        loadTvDataSourcePage(config, page = 1)
    }
    
    /**
     * Load a limited number of items for onboarding (faster initial load)
     * Fetches full page but only enriches/saves first `limit` items
     */
    suspend fun loadTvDataSourcePageForOnboarding(
        config: DataSourceConfig, 
        page: Int, 
        limit: Int = 7
    ): Int {
        return try {
            Log.d(TAG, "📥 Loading $limit items for onboarding: ${config.endpoint}")
            
            // Step 1: Fetch data from Trakt API (full page)
            val response = fetchTvDataFromTrakt(config, page)
            if (response.isEmpty()) return 0
            
            // Step 2: Take only the first `limit` items
            val limitedResponse = response.take(limit)
            
            // Step 3: Calculate order values
            val pageSize = StrmrConstants.Api.LARGE_PAGE_SIZE
            val baseOrder = (page - 1) * pageSize
            
            // Step 4: Enrich limited data with TMDB details
            val enrichedEntities = enrichTvData(limitedResponse, config, baseOrder)
            if (enrichedEntities.isEmpty()) return 0
            
            // Step 5: Save to database with conflict resolution
            saveTvShowEntities(enrichedEntities, config)
            
            Log.d(TAG, "✅ Successfully loaded ${enrichedEntities.size} TV shows for onboarding")
            enrichedEntities.size
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading onboarding data for ${config.endpoint}", e)
            0
        }
    }
    
    /**
     * Update movie logo - delegates to services
     */
    suspend fun updateMovieLogo(tmdbId: Int, logoUrl: String?) {
        dataSourceService.updateMovieLogo(tmdbId, logoUrl)
    }
    
    /**
     * Update TV show logo - delegates to services
     */
    suspend fun updateTvShowLogo(tmdbId: Int, logoUrl: String?) {
        dataSourceService.updateTvShowLogo(tmdbId, logoUrl)
    }
    
    // ======================== PRIVATE HELPER METHODS ========================
    
    /**
     * Fetch movie data from Trakt API based on endpoint
     */
    private suspend fun fetchMovieDataFromTrakt(config: DataSourceConfig, page: Int): List<Any> {
        return when {
            config.endpoint == "movies/trending" -> 
                traktApiService.getTrendingMovies(page = page, limit = StrmrConstants.Api.LARGE_PAGE_SIZE)
            config.endpoint == "movies/popular" -> 
                traktApiService.getPopularMovies(page = page, limit = StrmrConstants.Api.LARGE_PAGE_SIZE)
            config.endpoint.startsWith("users/") && config.endpoint.endsWith("/items") -> {
                handleUserListEndpoint(config, page)
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown movie endpoint: ${config.endpoint}")
                emptyList()
            }
        }
    }
    
    /**
     * Fetch TV show data from Trakt API based on endpoint
     */
    private suspend fun fetchTvDataFromTrakt(config: DataSourceConfig, page: Int): List<Any> {
        return when (config.endpoint) {
            "shows/trending" -> traktApiService.getTrendingTvShows(page = page, limit = StrmrConstants.Api.LARGE_PAGE_SIZE)
            "shows/popular" -> traktApiService.getPopularTvShows(page = page, limit = StrmrConstants.Api.LARGE_PAGE_SIZE)
            else -> {
                Log.w(TAG, "⚠️ Unknown TV endpoint: ${config.endpoint}")
                emptyList()
            }
        }
    }
    
    /**
     * Handle user list endpoints with proper validation
     */
    private suspend fun handleUserListEndpoint(config: DataSourceConfig, page: Int): List<Any> {
        val parts = config.endpoint.split("/")
        if (parts.size >= 5 && parts[2] == "lists") {
            val username = parts[1]
            val listSlug = parts[3]
            if (page > 1) {
                Log.w(TAG, "⚠️ User lists may not support pagination")
                return emptyList()
            }
            return traktApiService.getUserListItems(username, listSlug)
        } else {
            Log.w(TAG, "⚠️ Invalid user list endpoint format: ${config.endpoint}")
            return emptyList()
        }
    }
    
    /**
     * Enrich movie data using TmdbEnrichmentService
     */
    private suspend fun enrichMovieData(
        response: List<Any>,
        config: DataSourceConfig,
        baseOrder: Int
    ): List<MovieEntity> {
        return when {
            config.endpoint == "movies/trending" -> {
                (response as? List<TrendingMovie>)?.mapIndexedNotNull { index, trending ->
                    tmdbEnrichmentService.enrichMovie(
                        movie = trending.movie,
                        dataSourceId = config.id,
                        orderValue = baseOrder + index
                    )
                } ?: emptyList()
            }
            config.endpoint == "movies/popular" -> {
                (response as? List<Movie>)?.mapIndexedNotNull { index, movie ->
                    tmdbEnrichmentService.enrichMovie(
                        movie = movie,
                        dataSourceId = config.id,
                        orderValue = baseOrder + index
                    )
                } ?: emptyList()
            }
            config.endpoint.startsWith("users/") && config.endpoint.endsWith("/items") -> {
                // Handle user list items - extract movies from TraktListItem
                (response as? List<TraktListItem>)?.mapIndexedNotNull { index, listItem ->
                    listItem.movie?.let { movie ->
                        tmdbEnrichmentService.enrichMovie(
                            movie = movie,
                            dataSourceId = config.id,
                            orderValue = baseOrder + index
                        )
                    }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    /**
     * Enrich TV show data using TmdbEnrichmentService
     */
    private suspend fun enrichTvData(
        response: List<Any>,
        config: DataSourceConfig,
        baseOrder: Int
    ): List<TvShowEntity> {
        return when (config.endpoint) {
            "shows/trending" -> {
                (response as? List<TrendingShow>)?.mapIndexedNotNull { index, trending ->
                    tmdbEnrichmentService.enrichTvShow(
                        show = trending.show,
                        dataSourceId = config.id,
                        orderValue = baseOrder + index
                    )
                } ?: emptyList()
            }
            "shows/popular" -> {
                (response as? List<Show>)?.mapIndexedNotNull { index, show ->
                    tmdbEnrichmentService.enrichTvShow(
                        show = show,
                        dataSourceId = config.id,
                        orderValue = baseOrder + index
                    )
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }
    
    /**
     * Save movie entities to database with conflict resolution
     */
    private suspend fun saveMovieEntities(
        entities: List<MovieEntity>,
        config: DataSourceConfig
    ) {
        database.withTransaction {
            for (entity in entities) {
                val existing = dataSourceService.getMovieByTmdbId(entity.tmdbId)
                if (existing == null) {
                    dataSourceService.insertMovies(listOf(entity))
                } else {
                    dataSourceService.updateMovieDataSourceOrder(
                        existing = existing,
                        config = config,
                        newOrder = when (config.id) {
                            "trending" -> entity.trendingOrder
                            "popular" -> entity.popularOrder
                            "top_movies_week" -> entity.topMoviesWeekOrder
                            else -> null
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Save TV show entities to database with conflict resolution
     */
    private suspend fun saveTvShowEntities(
        entities: List<TvShowEntity>,
        config: DataSourceConfig
    ) {
        database.withTransaction {
            for (entity in entities) {
                val existing = dataSourceService.getTvShowByTmdbId(entity.tmdbId)
                if (existing == null) {
                    dataSourceService.insertTvShows(listOf(entity))
                } else {
                    dataSourceService.updateTvShowDataSourceOrder(
                        existing = existing,
                        config = config,
                        newOrder = when (config.id) {
                            "trending" -> entity.trendingOrder
                            "popular" -> entity.popularOrder
                            else -> null
                        }
                    )
                }
            }
        }
    }
    
    // ======================== MULTIPLE DATA SOURCES METHODS ========================
    // These methods are used by ViewModels to combine multiple data sources
    
    /**
     * Get multiple movie data sources and combine them
     * Used by GenericMoviesViewModel
     */
    fun getMultipleMovieDataSources(configs: List<DataSourceConfig>): Flow<Map<String, List<MovieEntity>>> {
        return combine(
            configs.map { config ->
                getMoviesFromDataSource(config).map { movies ->
                    config.id to movies
                }
            }
        ) { results ->
            results.toMap()
        }
    }
    
    /**
     * Get multiple TV show data sources and combine them
     * Used by GenericTvShowsViewModel
     */
    fun getMultipleTvDataSources(configs: List<DataSourceConfig>): Flow<Map<String, List<TvShowEntity>>> {
        return combine(
            configs.map { config ->
                getTvShowsFromDataSource(config).map { tvShows ->
                    config.id to tvShows
                }
            }
        ) { results ->
            results.toMap()
        }
    }
}