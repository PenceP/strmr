package com.strmr.ai.data

import androidx.paging.PagingSource
import androidx.sqlite.db.SimpleSQLiteQuery
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.DataSourceQueryBuilder
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

/**
 * Service responsible for database operations related to data sources
 * 
 * Single Responsibility: Database queries, counts, and paging operations
 * Keeps database logic separate from API calls and data transformation
 */
@Singleton
class DataSourceService @Inject constructor(
    private val database: StrmrDatabase
) {
    
    companion object {
        private const val TAG = "DataSourceService"
    }
    
    // ======================== MOVIE DATA SOURCE OPERATIONS ========================
    
    /**
     * Get movies from any data source using generic queries
     */
    fun getMoviesFromDataSource(config: DataSourceConfig): Flow<List<MovieEntity>> {
        val query = DataSourceQueryBuilder.buildDataSourceQuery("movies", config.id)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        Log.d(TAG, "🎬 Getting movies from data source: ${config.title}")
        return database.movieDao().getMoviesFromDataSource(sqlQuery)
    }
    
    /**
     * Get paging source for movies from any data source
     */
    fun getMoviesPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, MovieEntity> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = SimpleSQLiteQuery(
            "SELECT * FROM movies WHERE $fieldName IS NOT NULL ORDER BY $fieldName ASC"
        )
        
        Log.d(TAG, "🏭 Creating movie PagingSource for ${config.title} with field: $fieldName")
        return database.movieDao().getMoviesPagingFromDataSource(query)
    }
    
    /**
     * Check if a movie data source is empty
     */
    suspend fun isMovieDataSourceEmpty(config: DataSourceConfig): Boolean {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = "SELECT COUNT(*) FROM movies WHERE $fieldName IS NOT NULL"
        val sqlQuery = SimpleSQLiteQuery(query)
        val count = database.movieDao().getCountFromDataSource(sqlQuery)
        
        Log.d(TAG, "📊 Movie data source ${config.title} count: $count (empty: ${count == 0})")
        return count == 0
    }
    
    /**
     * Get count of movies in a data source
     */
    suspend fun getMovieDataSourceCount(config: DataSourceConfig): Int {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = "SELECT COUNT(*) FROM movies WHERE $fieldName IS NOT NULL"
        val sqlQuery = SimpleSQLiteQuery(query)
        val count = database.movieDao().getCountFromDataSource(sqlQuery)
        
        Log.d(TAG, "🔢 Movie data source ${config.title} count: $count")
        return count
    }
    
    // ======================== TV SHOW DATA SOURCE OPERATIONS ========================
    
    /**
     * Get TV shows from any data source using generic queries
     */
    fun getTvShowsFromDataSource(config: DataSourceConfig): Flow<List<TvShowEntity>> {
        val query = DataSourceQueryBuilder.buildDataSourceQuery("tv_shows", config.id)
        val sqlQuery = SimpleSQLiteQuery(query)
        
        Log.d(TAG, "📺 Getting TV shows from data source: ${config.title}")
        return database.tvShowDao().getTvShowsFromDataSource(sqlQuery)
    }
    
    /**
     * Get paging source for TV shows from any data source
     */
    fun getTvShowsPagingFromDataSource(config: DataSourceConfig): PagingSource<Int, TvShowEntity> {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = SimpleSQLiteQuery(
            "SELECT * FROM tv_shows WHERE $fieldName IS NOT NULL ORDER BY $fieldName ASC"
        )
        
        Log.d(TAG, "🏭 Creating TV show PagingSource for ${config.title} with field: $fieldName")
        return database.tvShowDao().getTvShowsPagingFromDataSource(query)
    }
    
    /**
     * Check if a TV data source is empty
     */
    suspend fun isTvDataSourceEmpty(config: DataSourceConfig): Boolean {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = "SELECT COUNT(*) FROM tv_shows WHERE $fieldName IS NOT NULL"
        val sqlQuery = SimpleSQLiteQuery(query)
        val count = database.tvShowDao().getCountFromDataSource(sqlQuery)
        
        Log.d(TAG, "📊 TV data source ${config.title} count: $count (empty: ${count == 0})")
        return count == 0
    }
    
    /**
     * Get count of TV shows in a data source
     */
    suspend fun getTvDataSourceCount(config: DataSourceConfig): Int {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val query = "SELECT COUNT(*) FROM tv_shows WHERE $fieldName IS NOT NULL"
        val sqlQuery = SimpleSQLiteQuery(query)
        val count = database.tvShowDao().getCountFromDataSource(sqlQuery)
        
        Log.d(TAG, "🔢 TV data source ${config.title} count: $count")
        return count
    }
    
    // ======================== DATABASE OPERATIONS ========================
    
    /**
     * Insert movies into database with transaction safety
     */
    suspend fun insertMovies(movies: List<MovieEntity>) {
        if (movies.isNotEmpty()) {
            database.movieDao().insertMovies(movies)
            Log.d(TAG, "✅ Inserted ${movies.size} movies into database")
        }
    }
    
    /**
     * Insert TV shows into database with transaction safety
     */
    suspend fun insertTvShows(tvShows: List<TvShowEntity>) {
        if (tvShows.isNotEmpty()) {
            database.tvShowDao().insertTvShows(tvShows)
            Log.d(TAG, "✅ Inserted ${tvShows.size} TV shows into database")
        }
    }
    
    /**
     * Update existing movie with new data source order, preserving other fields
     */
    suspend fun updateMovieDataSourceOrder(
        existing: MovieEntity,
        config: DataSourceConfig,
        newOrder: Int?
    ) {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val updatedEntity = when (fieldName) {
            "trendingOrder" -> existing.copy(trendingOrder = newOrder)
            "popularOrder" -> existing.copy(popularOrder = newOrder)
            "topMoviesWeekOrder" -> existing.copy(topMoviesWeekOrder = newOrder)
            else -> existing
        }
        database.movieDao().insertMovies(listOf(updatedEntity))
        Log.d(TAG, "🔄 Updated movie ${existing.title} with $fieldName = $newOrder")
    }
    
    /**
     * Update existing TV show with new data source order, preserving other fields
     */
    suspend fun updateTvShowDataSourceOrder(
        existing: TvShowEntity,
        config: DataSourceConfig,
        newOrder: Int?
    ) {
        val fieldName = DataSourceQueryBuilder.getDataSourceField(config.id)
        val updatedEntity = when (fieldName) {
            "trendingOrder" -> existing.copy(trendingOrder = newOrder)
            "popularOrder" -> existing.copy(popularOrder = newOrder)
            else -> existing
        }
        database.tvShowDao().insertTvShows(listOf(updatedEntity))
        Log.d(TAG, "🔄 Updated TV show ${existing.title} with $fieldName = $newOrder")
    }
    
    /**
     * Get existing movie by TMDB ID
     */
    suspend fun getMovieByTmdbId(tmdbId: Int): MovieEntity? {
        return database.movieDao().getMovieByTmdbId(tmdbId)
    }
    
    /**
     * Get existing TV show by TMDB ID
     */
    suspend fun getTvShowByTmdbId(tmdbId: Int): TvShowEntity? {
        return database.tvShowDao().getTvShowByTmdbId(tmdbId)
    }
    
    /**
     * Update movie logo URL
     */
    suspend fun updateMovieLogo(tmdbId: Int, logoUrl: String?) {
        database.movieDao().updateMovieLogo(tmdbId, logoUrl)
        Log.d(TAG, "🖼️ Updated movie logo for TMDB ID: $tmdbId")
    }
    
    /**
     * Update TV show logo URL
     */
    suspend fun updateTvShowLogo(tmdbId: Int, logoUrl: String?) {
        database.tvShowDao().updateTvShowLogo(tmdbId, logoUrl)
        Log.d(TAG, "🖼️ Updated TV show logo for TMDB ID: $tmdbId")
    }
}