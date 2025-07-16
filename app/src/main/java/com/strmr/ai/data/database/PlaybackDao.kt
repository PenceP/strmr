package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackItems(items: List<PlaybackEntity>)

    @Query("SELECT * FROM playback ORDER BY pausedAt DESC")
    fun getPlaybackItems(): Flow<List<PlaybackEntity>>

    @Query("DELETE FROM playback")
    suspend fun clearPlaybackItems()
} 