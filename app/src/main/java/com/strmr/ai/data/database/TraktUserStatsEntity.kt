package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trakt_user_stats")
data class TraktUserStatsEntity(
    @PrimaryKey val username: String,
    val moviesJson: String,
    val showsJson: String,
    val seasonsJson: String,
    val episodesJson: String,
    val networkJson: String,
    val ratingsJson: String
) 