package com.strmr.ai.data

import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.DataSourceQueryBuilder
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.BelongsToCollection
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import com.strmr.ai.domain.usecase.MediaType as LogoMediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic repository that dynamically fetches data based on DataSourceConfig
 * Eliminates the need for hardcoded getXMovies() methods
 */
@Singleton
class GenericTraktRepository @Inject constructor(
    private val database: StrmrDatabase,
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val fetchLogoUseCase: FetchLogoUseCase
) {
    
    /**
     * Get movies from any data source using generic queries
     */
    fun getMoviesFromDataSource(config: DataSourceConfig): Flow<List<MovieEntity>> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = DataSourceQueryBuilder.buildDataSourceQuery("movies", fieldName)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        return database.movieDao().getMoviesFromDataSource(sqlQuery)
    }
    
    /**
     * Get TV shows from any data source using generic queries
     */
    fun getTvShowsFromDataSource(config: DataSourceConfig): Flow<List<TvShowEntity>> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = DataSourceQueryBuilder.buildDataSourceQuery("tv_shows", fieldName)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        return database.tvShowDao().getTvShowsFromDataSource(sqlQuery)
    }
    
    /**
     * Get paging source for movies from any data source
     */
    fun getMoviesPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, MovieEntity> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = DataSourceQueryBuilder.buildDataSourceQuery("movies", fieldName)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        return database.movieDao().getMoviesPagingFromDataSource(sqlQuery)
    }
    
    /**
     * Get paging source for TV shows from any data source
     */
    fun getTvShowsPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, TvShowEntity> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = DataSourceQueryBuilder.buildDataSourceQuery("tv_shows", fieldName)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        return database.tvShowDao().getTvShowsPagingFromDataSource(sqlQuery)
    }
    
    /**
     * Get multiple data sources for movies
     */
    fun getMultipleMovieDataSources(configs: List<DataSourceConfig>): Flow<Map<String, List<MovieEntity>>> {
        val flows = configs.map { config ->
            config.title to getMoviesFromDataSource(config)
        }
        
        return combine(flows.map { it.second }) { arrays ->
            flows.mapIndexed { index, (title, _) ->
                title to arrays[index]
            }.toMap()
        }
    }
    
    /**
     * Get multiple data sources for TV shows
     */
    fun getMultipleTvDataSources(configs: List<DataSourceConfig>): Flow<Map<String, List<TvShowEntity>>> {
        val flows = configs.map { config ->
            config.title to getTvShowsFromDataSource(config)
        }
        
        return combine(flows.map { it.second }) { arrays ->
            flows.mapIndexed { index, (title, _) ->
                title to arrays[index]
            }.toMap()
        }
    }
    /**
     * Refresh any movie data source generically
     */
    suspend fun refreshMovieDataSource(config: DataSourceConfig) {
        try {
            Log.d("GenericTraktRepository", "🔄 Refreshing movie data source: ${config.endpoint}")
            
            val response = when {
                config.endpoint == "movies/trending" -> traktApiService.getTrendingMovies()
                config.endpoint == "movies/popular" -> traktApiService.getPopularMovies()
                config.endpoint.startsWith("users/") && config.endpoint.endsWith("/items") -> {
                    // Extract username and list slug from endpoint like "users/garycrawfordgc/lists/top-movies-of-the-week/items"
                    val parts = config.endpoint.split("/")
                    if (parts.size >= 5 && parts[2] == "lists") {
                        val username = parts[1]
                        val listSlug = parts[3]
                        traktApiService.getUserListItems(username, listSlug)
                    } else {
                        Log.w("GenericTraktRepository", "⚠️ Invalid user list endpoint format: ${config.endpoint}")
                        return
                    }
                }
                else -> {
                    Log.w("GenericTraktRepository", "⚠️ Unknown movie endpoint: ${config.endpoint}")
                    return
                }
            }
            
            // Clear existing data for this source
            val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
            val clearQuery = DataSourceQueryBuilder.buildClearDataSourceQuery("movies", fieldName)
            database.movieDao().clearDataSourceField(SimpleSQLiteQuery(clearQuery))
            
            // Transform and insert new data with TMDB enrichment  
            val entities = when {
                config.endpoint == "movies/trending" -> {
                    (response as List<TrendingMovie>).mapIndexedNotNull { index, item ->
                        enrichMovieWithTmdbData(item.movie, config.id, index + 1)
                    }
                }
                config.endpoint == "movies/popular" -> {
                    (response as List<Movie>).mapIndexedNotNull { index, item ->
                        enrichMovieWithTmdbData(item, config.id, index + 1)
                    }
                }
                config.endpoint.startsWith("users/") && config.endpoint.endsWith("/items") -> {
                    (response as List<TraktListItem>).mapIndexedNotNull { index, item ->
                        item.movie?.let { movie ->
                            enrichMovieWithTmdbData(movie, config.id, index + 1)
                        }
                    }
                }
                else -> emptyList()
            }
            
            if (entities.isNotEmpty()) {
                database.movieDao().insertMovies(entities)
                Log.d("GenericTraktRepository", "✅ Refreshed ${entities.size} movies for ${config.id}")
            }
            
        } catch (e: Exception) {
            Log.e("GenericTraktRepository", "❌ Error refreshing movie data source ${config.id}", e)
        }
    }
    
    /**
     * Refresh any TV show data source generically
     */
    suspend fun refreshTvDataSource(config: DataSourceConfig) {
        try {
            Log.d("GenericTraktRepository", "🔄 Refreshing TV data source: ${config.endpoint}")
            
            val response = when (config.endpoint) {
                "shows/trending" -> traktApiService.getTrendingTvShows()
                "shows/popular" -> traktApiService.getPopularTvShows()
                else -> {
                    Log.w("GenericTraktRepository", "⚠️ Unknown TV endpoint: ${config.endpoint}")
                    return
                }
            }
            
            // Clear existing data for this source
            val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
            val clearQuery = DataSourceQueryBuilder.buildClearDataSourceQuery("tv_shows", fieldName)
            database.tvShowDao().clearDataSourceField(SimpleSQLiteQuery(clearQuery))
            
            // Transform and insert new data with TMDB enrichment
            val entities = when (config.endpoint) {
                "shows/trending" -> {
                    (response as List<TrendingShow>).mapIndexedNotNull { index, item ->
                        enrichTvShowWithTmdbData(item.show, trendingOrder = index + 1)
                    }
                }
                "shows/popular" -> {
                    (response as List<Show>).mapIndexedNotNull { index, item ->
                        enrichTvShowWithTmdbData(item, popularOrder = index + 1)
                    }
                }
                else -> emptyList()
            }
            
            if (entities.isNotEmpty()) {
                database.tvShowDao().insertTvShows(entities)
                Log.d("GenericTraktRepository", "✅ Refreshed ${entities.size} TV shows for ${config.id}")
            }
            
        } catch (e: Exception) {
            Log.e("GenericTraktRepository", "❌ Error refreshing TV data source ${config.id}", e)
        }
    }
    
    /**
     * Enrich movie data with TMDB details (posters, backdrops, overview, etc.)
     */
    private suspend fun enrichMovieWithTmdbData(
        movie: Movie,
        dataSourceId: String,
        order: Int
    ): MovieEntity? {
        val tmdbId = movie.ids.tmdb ?: return null
        return try {
            val details = tmdbApiService.getMovieDetails(tmdbId)
            val credits = tmdbApiService.getMovieCredits(tmdbId)
            
            // Fetch logo
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, LogoMediaType.MOVIE)
            
            MovieEntity(
                tmdbId = tmdbId,
                imdbId = movie.ids.imdb,
                title = details.title ?: movie.title,
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                year = movie.year,
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres?.map { it.name } ?: emptyList(),
                cast = credits.cast?.take(10)?.map { 
                    Actor(id = it.id, name = it.name, character = it.character, profilePath = it.profile_path)
                } ?: emptyList(),
                // Dynamic order assignment based on data source
                trendingOrder = if (dataSourceId == "trending") order else null,
                popularOrder = if (dataSourceId == "popular") order else null,
                nowPlayingOrder = if (dataSourceId == "now_playing") order else null,
                upcomingOrder = if (dataSourceId == "upcoming") order else null,
                topRatedOrder = if (dataSourceId == "top_rated") order else null,
                topMoviesWeekOrder = if (dataSourceId == "top_movies_week") order else null,
                // Additional fields with logo
                logoUrl = logoUrl,
                traktRating = null,
                traktVotes = null,
                belongsToCollection = details.belongs_to_collection?.let {
                    BelongsToCollection(id = it.id, name = it.name, poster_path = it.poster_path, backdrop_path = it.backdrop_path)
                },
                similar = emptyList()
            )
        } catch (e: Exception) {
            Log.e("GenericTraktRepository", "❌ Error enriching movie ${movie.title}", e)
            null
        }
    }
    
    /**
     * Enrich TV show data with TMDB details (posters, backdrops, overview, etc.)
     */
    private suspend fun enrichTvShowWithTmdbData(
        show: Show,
        trendingOrder: Int? = null,
        popularOrder: Int? = null,
        topRatedOrder: Int? = null,
        airingTodayOrder: Int? = null,
        onTheAirOrder: Int? = null
    ): TvShowEntity? {
        val tmdbId = show.ids.tmdb ?: return null
        return try {
            val details = tmdbApiService.getTvShowDetails(tmdbId)
            val credits = tmdbApiService.getTvShowCredits(tmdbId)
            
            // Fetch logo
            val logoUrl = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, LogoMediaType.TV_SHOW)
            
            TvShowEntity(
                tmdbId = tmdbId,
                imdbId = show.ids.imdb,
                title = details.name ?: show.title,
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                year = show.year,
                firstAirDate = details.first_air_date,
                lastAirDate = details.last_air_date,
                runtime = details.episode_run_time?.firstOrNull(),
                genres = details.genres?.map { it.name } ?: emptyList(),
                cast = credits.cast?.take(10)?.map { 
                    Actor(id = it.id, name = it.name, character = it.character, profilePath = it.profile_path)
                } ?: emptyList(),
                trendingOrder = trendingOrder,
                popularOrder = popularOrder,
                topRatedOrder = topRatedOrder,
                airingTodayOrder = airingTodayOrder,
                onTheAirOrder = onTheAirOrder,
                // Additional fields with logo
                logoUrl = logoUrl,
                traktRating = null,
                traktVotes = null,
                similar = emptyList()
            )
        } catch (e: Exception) {
            Log.e("GenericTraktRepository", "❌ Error enriching TV show ${show.title}", e)
            null
        }
    }
}

// Extension functions to convert to entities (kept for backward compatibility)
private fun Movie.toMovieEntity(
    trendingOrder: Int? = null, 
    popularOrder: Int? = null,
    nowPlayingOrder: Int? = null,
    upcomingOrder: Int? = null,
    topRatedOrder: Int? = null
): MovieEntity {
    return MovieEntity(
        tmdbId = this.ids.tmdb ?: 0,
        imdbId = this.ids.imdb,
        title = this.title,
        year = this.year,
        trendingOrder = trendingOrder,
        popularOrder = popularOrder,
        nowPlayingOrder = nowPlayingOrder,
        upcomingOrder = upcomingOrder,
        topRatedOrder = topRatedOrder,
        // Other fields with defaults
        posterUrl = null,
        backdropUrl = null,
        overview = null,
        rating = 0f,
        logoUrl = null,
        traktRating = null,
        traktVotes = null,
        releaseDate = null,
        runtime = null,
        genres = emptyList(),
        cast = emptyList(),
        belongsToCollection = null
    )
}

private fun Show.toTvShowEntity(
    trendingOrder: Int? = null, 
    popularOrder: Int? = null,
    topRatedOrder: Int? = null,
    airingTodayOrder: Int? = null,
    onTheAirOrder: Int? = null
): TvShowEntity {
    return TvShowEntity(
        tmdbId = this.ids.tmdb ?: 0,
        imdbId = this.ids.imdb,
        title = this.title,
        year = this.year,
        trendingOrder = trendingOrder,
        popularOrder = popularOrder,
        topRatedOrder = topRatedOrder,
        airingTodayOrder = airingTodayOrder,
        onTheAirOrder = onTheAirOrder,
        // Other fields with defaults
        posterUrl = null,
        backdropUrl = null,
        overview = null,
        rating = null,
        logoUrl = null,
        traktRating = null,
        traktVotes = null,
        firstAirDate = null,
        lastAirDate = null,
        runtime = null,
        genres = emptyList(),
        cast = emptyList(),
        similar = emptyList()
    )
}