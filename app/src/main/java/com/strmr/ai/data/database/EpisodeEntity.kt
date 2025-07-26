package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val showTmdbId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String?,
    val overview: String?,
    val stillUrl: String?,
    val airDate: String?,
    val runtime: Int?,
    val rating: Float?,
    val lastUpdated: Long = 0L // for cache expiry
)