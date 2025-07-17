package com.strmr.ai.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction

@Dao
interface TvShowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvShows(shows: List<TvShowEntity>)

    @Query("SELECT * FROM tv_shows WHERE trendingOrder IS NOT NULL ORDER BY trendingOrder ASC")
    fun getTrendingTvShows(): Flow<List<TvShowEntity>>
    
    @Query("SELECT * FROM tv_shows WHERE popularOrder IS NOT NULL ORDER BY popularOrder ASC")
    fun getPopularTvShows(): Flow<List<TvShowEntity>>
    
    @Query("SELECT * FROM tv_shows")
    fun getTvShowsPagingSource(): PagingSource<Int, TvShowEntity>
    
    @Query("SELECT * FROM tv_shows LIMIT :pageSize OFFSET :offset")
    suspend fun getTvShowsPage(pageSize: Int, offset: Int): List<TvShowEntity>
    
    @Query("SELECT COUNT(*) FROM tv_shows")
    suspend fun getTvShowCount(): Int
    
    @Query("SELECT * FROM tv_shows WHERE tmdbId = :tmdbId")
    suspend fun getTvShowByTmdbId(tmdbId: Int): TvShowEntity?
    
    @Query("UPDATE tv_shows SET trendingOrder = NULL")
    suspend fun clearTrendingOrder()
    
    @Query("UPDATE tv_shows SET popularOrder = NULL")
    suspend fun clearPopularOrder()

    @Transaction
    suspend fun updateTrendingTvShows(shows: List<TvShowEntity>) {
        clearTrendingOrder()
        insertTvShows(shows)
    }
    
    @Transaction
    suspend fun updatePopularTvShows(shows: List<TvShowEntity>) {
        clearPopularOrder()
        insertTvShows(shows)
    }
    
    @Query("UPDATE tv_shows SET logoUrl = :logoUrl WHERE tmdbId = :tmdbId")
    suspend fun updateTvShowLogo(tmdbId: Int, logoUrl: String?)
    
    @Query("UPDATE tv_shows SET logoUrl = NULL WHERE logoUrl IS NULL")
    suspend fun clearNullLogos()
    
    @Query("SELECT COUNT(*) FROM tv_shows WHERE trendingOrder IS NOT NULL")
    suspend fun getTrendingTvShowsCount(): Int
    
    @Query("SELECT COUNT(*) FROM tv_shows WHERE popularOrder IS NOT NULL")
    suspend fun getPopularTvShowsCount(): Int
    
    @Query("SELECT * FROM tv_shows WHERE trendingOrder IS NOT NULL ORDER BY trendingOrder ASC")
    fun getTrendingTvShowsPagingSource(): PagingSource<Int, TvShowEntity>
    
    @Query("SELECT * FROM tv_shows WHERE popularOrder IS NOT NULL ORDER BY popularOrder ASC")
    fun getPopularTvShowsPagingSource(): PagingSource<Int, TvShowEntity>
} 