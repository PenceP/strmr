package com.strmr.ai.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

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
    suspend fun getTvShowsPage(
        pageSize: Int,
        offset: Int,
    ): List<TvShowEntity>

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
    suspend fun updateTvShowLogo(
        tmdbId: Int,
        logoUrl: String?,
    )

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

    // ✅ NEW: Paged methods for MediaPagingSource
    @Query("SELECT * FROM tv_shows WHERE trendingOrder IS NOT NULL ORDER BY trendingOrder ASC LIMIT :limit OFFSET :offset")
    suspend fun getTrendingTvShowsPaged(
        limit: Int,
        offset: Int,
    ): List<TvShowEntity>

    @Query("SELECT * FROM tv_shows WHERE popularOrder IS NOT NULL ORDER BY popularOrder ASC LIMIT :limit OFFSET :offset")
    suspend fun getPopularTvShowsPaged(
        limit: Int,
        offset: Int,
    ): List<TvShowEntity>

    // === GENERIC DATA SOURCE METHODS ===

    /**
     * Generic method to get TV shows from any data source using raw query
     */
    @RawQuery(observedEntities = [TvShowEntity::class])
    fun getTvShowsFromDataSource(query: SupportSQLiteQuery): Flow<List<TvShowEntity>>

    /**
     * Generic paging source for any data source
     */
    @RawQuery(observedEntities = [TvShowEntity::class])
    fun getTvShowsPagingFromDataSource(query: SupportSQLiteQuery): PagingSource<Int, TvShowEntity>

    /**
     * Generic method to clear any data source field
     */
    @RawQuery
    suspend fun clearDataSourceField(query: SupportSQLiteQuery): Int

    /**
     * Generic method to get TV shows from any data source with pagination
     */
    @RawQuery
    suspend fun getTvShowsFromDataSourcePaged(query: SupportSQLiteQuery): List<TvShowEntity>

    /**
     * Generic method to count TV shows in a data source
     */
    @RawQuery
    suspend fun getCountFromDataSource(query: SupportSQLiteQuery): Int
}
