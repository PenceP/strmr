package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TraktUserStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: TraktUserStatsEntity)

    @Query("SELECT * FROM trakt_user_stats WHERE username = :username LIMIT 1")
    fun getUserStats(username: String): Flow<TraktUserStatsEntity?>
}
