package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trakt_ratings")
data class TraktRatingsEntity(
    @PrimaryKey val traktId: Int,
    val rating: Float,
    val votes: Int,
    val updatedAt: Long, // epoch millis for cache expiry
)
