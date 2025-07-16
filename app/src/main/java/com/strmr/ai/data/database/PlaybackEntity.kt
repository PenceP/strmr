package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.strmr.ai.data.database.converters.ListConverter

@Entity(tableName = "playback")
@TypeConverters(ListConverter::class)
data class PlaybackEntity(
    @PrimaryKey val id: Long, // Trakt playback id
    val progress: Float,
    val pausedAt: String,
    val type: String,
    val movieTitle: String? = null,
    val movieTmdbId: Int? = null,
    val showTitle: String? = null,
    val showTmdbId: Int? = null,
    val episodeTitle: String? = null,
    val episodeSeason: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTmdbId: Int? = null
) 