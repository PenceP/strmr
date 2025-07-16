package com.strmr.ai.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OmdbRatingsDao {
    @Query("SELECT * FROM omdb_ratings WHERE imdbId = :imdbId LIMIT 1")
    suspend fun getOmdbRatings(imdbId: String): OmdbRatingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOmdbRatings(entity: OmdbRatingsEntity)
} 