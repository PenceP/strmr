package com.strmr.ai.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Query("SELECT * FROM movies WHERE trendingOrder IS NOT NULL ORDER BY trendingOrder ASC")
    fun getTrendingMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE popularOrder IS NOT NULL ORDER BY popularOrder ASC")
    fun getPopularMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE tmdbId = :tmdbId")
    suspend fun getMovieByTmdbId(tmdbId: Int): MovieEntity?
    
    @Query("SELECT * FROM movies")
    fun getMoviesPagingSource(): PagingSource<Int, MovieEntity>
    
    @Query("SELECT * FROM movies LIMIT :pageSize OFFSET :offset")
    suspend fun getMoviesPage(pageSize: Int, offset: Int): List<MovieEntity>
    
    @Query("SELECT COUNT(*) FROM movies")
    suspend fun getMovieCount(): Int
    
    @Query("UPDATE movies SET logoUrl = :logoUrl WHERE tmdbId = :tmdbId")
    suspend fun updateMovieLogo(tmdbId: Int, logoUrl: String?)

    @Query("UPDATE movies SET logoUrl = NULL WHERE logoUrl IS NULL")
    suspend fun clearNullLogos()

    @Query("UPDATE movies SET trendingOrder = NULL")
    suspend fun clearTrendingOrder()

    @Query("UPDATE movies SET popularOrder = NULL")
    suspend fun clearPopularOrder()

    @Transaction
    suspend fun updateTrendingMovies(movies: List<MovieEntity>) {
        clearTrendingOrder()
        insertMovies(movies)
    }

    @Transaction
    suspend fun updatePopularMovies(movies: List<MovieEntity>) {
        clearPopularOrder()
        insertMovies(movies)
    }
    
    @Query("SELECT COUNT(*) FROM movies WHERE trendingOrder IS NOT NULL")
    suspend fun getTrendingMoviesCount(): Int
    
    @Query("SELECT COUNT(*) FROM movies WHERE popularOrder IS NOT NULL")
    suspend fun getPopularMoviesCount(): Int
    
    @Query("SELECT * FROM movies WHERE trendingOrder IS NOT NULL ORDER BY trendingOrder ASC")
    fun getTrendingMoviesPagingSource(): PagingSource<Int, MovieEntity>
    
    @Query("SELECT * FROM movies WHERE popularOrder IS NOT NULL ORDER BY popularOrder ASC")
    fun getPopularMoviesPagingSource(): PagingSource<Int, MovieEntity>
    
    // === GENERIC DATA SOURCE METHODS ===
    
    /**
     * Generic method to get movies from any data source using raw query
     */
    @RawQuery(observedEntities = [MovieEntity::class])
    fun getMoviesFromDataSource(query: SupportSQLiteQuery): Flow<List<MovieEntity>>
    
    /**
     * Generic paging source for any data source
     */
    @RawQuery(observedEntities = [MovieEntity::class])
    fun getMoviesPagingFromDataSource(query: SupportSQLiteQuery): PagingSource<Int, MovieEntity>
    
    /**
     * Generic method to clear any data source field
     */
    @RawQuery
    suspend fun clearDataSourceField(query: SupportSQLiteQuery): Int
    
    /**
     * Generic method to get movies from any data source with pagination
     */
    @RawQuery
    suspend fun getMoviesFromDataSourcePaged(query: SupportSQLiteQuery): List<MovieEntity>
    
    /**
     * Generic method to count movies in a data source
     */
    @RawQuery
    suspend fun getCountFromDataSource(query: SupportSQLiteQuery): Int
} 