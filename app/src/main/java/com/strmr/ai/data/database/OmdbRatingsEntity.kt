package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "omdb_ratings")
data class OmdbRatingsEntity(
    @PrimaryKey val imdbId: String,
    val omdbJson: String,
    val lastFetched: Long
) 