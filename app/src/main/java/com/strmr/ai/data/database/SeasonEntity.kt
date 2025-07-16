package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seasons")
data class SeasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val showTmdbId: Int,
    val seasonNumber: Int,
    val name: String?,
    val overview: String?,
    val posterUrl: String?,
    val episodeCount: Int,
    val airDate: String?,
    val lastUpdated: Long = 0L // for cache expiry
) 