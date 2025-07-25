package com.strmr.ai.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for intermediate view caching operations
 */
@Dao
interface IntermediateViewDao {
    
    // ======================== IntermediateViewEntity Operations ========================
    
    @Query("SELECT * FROM intermediate_views WHERE id = :id")
    suspend fun getIntermediateView(id: String): IntermediateViewEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntermediateView(view: IntermediateViewEntity)
    
    @Query("DELETE FROM intermediate_views WHERE id = :id")
    suspend fun deleteIntermediateView(id: String)
    
    @Query("SELECT * FROM intermediate_views WHERE viewType = :viewType ORDER BY lastUpdated DESC")
    suspend fun getIntermediateViewsByType(viewType: String): List<IntermediateViewEntity>
    
    @Query("DELETE FROM intermediate_views WHERE lastUpdated < :expiry")
    suspend fun deleteExpiredViews(expiry: Long)
    
    // ======================== IntermediateViewItemEntity Operations ========================
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntermediateViewItems(items: List<IntermediateViewItemEntity>)
    
    @Query("DELETE FROM intermediate_view_items WHERE intermediateViewId = :intermediateViewId")
    suspend fun deleteIntermediateViewItems(intermediateViewId: String)
    
    @Query("""
        SELECT * FROM intermediate_view_items 
        WHERE intermediateViewId = :intermediateViewId 
        ORDER BY orderIndex ASC
    """)
    suspend fun getIntermediateViewItems(intermediateViewId: String): List<IntermediateViewItemEntity>
    
    // ======================== Combined Queries for UI ========================
    
    /**
     * Get cached movies for an intermediate view, ordered by their position in the list
     */
    @Query("""
        SELECT m.* FROM movies m
        INNER JOIN intermediate_view_items ivi ON m.tmdbId = ivi.tmdbId
        WHERE ivi.intermediateViewId = :intermediateViewId 
        AND ivi.mediaType = 'movie'
        ORDER BY ivi.orderIndex ASC
    """)
    suspend fun getCachedMoviesForView(intermediateViewId: String): List<MovieEntity>
    
    /**
     * Get cached TV shows for an intermediate view, ordered by their position in the list
     */
    @Query("""
        SELECT t.* FROM tv_shows t
        INNER JOIN intermediate_view_items ivi ON t.tmdbId = ivi.tmdbId
        WHERE ivi.intermediateViewId = :intermediateViewId 
        AND ivi.mediaType = 'show'
        ORDER BY ivi.orderIndex ASC
    """)
    suspend fun getCachedTvShowsForView(intermediateViewId: String): List<TvShowEntity>
    
    /**
     * Check if a view is cached and not expired
     */
    @Query("""
        SELECT COUNT(*) > 0 FROM intermediate_views 
        WHERE id = :id AND lastUpdated > :expiry
    """)
    suspend fun isViewCachedAndValid(id: String, expiry: Long): Boolean
    
    /**
     * Get the count of cached items for a view
     */
    @Query("SELECT COUNT(*) FROM intermediate_view_items WHERE intermediateViewId = :intermediateViewId")
    suspend fun getCachedItemCount(intermediateViewId: String): Int
    
    // ======================== Maintenance Operations ========================
    
    @Query("DELETE FROM intermediate_view_items WHERE addedAt < :expiry")
    suspend fun deleteExpiredItems(expiry: Long)
    
    @Query("""
        DELETE FROM intermediate_views 
        WHERE id NOT IN (
            SELECT DISTINCT intermediateViewId FROM intermediate_view_items
        )
    """)
    suspend fun deleteOrphanedViews()
}