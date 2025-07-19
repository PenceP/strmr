package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContinueWatchingDao {
    
    @Query("SELECT * FROM continue_watching ORDER BY lastWatchedAt DESC")
    fun getContinueWatchingItems(): Flow<List<ContinueWatchingEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContinueWatchingItems(items: List<ContinueWatchingEntity>)
    
    @Query("DELETE FROM continue_watching")
    suspend fun clearContinueWatching()
}