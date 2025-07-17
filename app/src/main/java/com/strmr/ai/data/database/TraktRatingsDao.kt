package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TraktRatingsDao {
    @Query("SELECT * FROM trakt_ratings WHERE traktId = :traktId LIMIT 1")
    suspend fun getRatings(traktId: Int): TraktRatingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rating: TraktRatingsEntity)

    @Query("DELETE FROM trakt_ratings WHERE updatedAt < :expiry")
    suspend fun deleteOldRatings(expiry: Long)
} 