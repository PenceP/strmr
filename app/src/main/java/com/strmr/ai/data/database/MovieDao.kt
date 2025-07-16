package com.strmr.ai.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
} 