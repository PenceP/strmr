package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "continue_watching")
data class ContinueWatchingEntity(
    @PrimaryKey val id: String, // Generated unique ID
    val type: String, // "movie" or "episode"
    val lastWatchedAt: String,
    val progress: Float?, // For in-progress items, null for next episodes
    
    // Movie data (when type = "movie")
    val movieTitle: String? = null,
    val movieTmdbId: Int? = null,
    val movieTraktId: Int? = null,
    val movieYear: Int? = null,
    
    // Show data (when type = "episode")
    val showTitle: String? = null,
    val showTmdbId: Int? = null,
    val showTraktId: Int? = null,
    val showYear: Int? = null,
    
    // Current/Next Episode data
    val episodeTitle: String? = null,
    val episodeSeason: Int? = null,
    val episodeNumber: Int? = null,
    val episodeTmdbId: Int? = null,
    val episodeTraktId: Int? = null,
    
    // Flags to distinguish between current and next episode
    val isNextEpisode: Boolean = false, // true if this represents the next episode to watch
    val isInProgress: Boolean = false   // true if this is currently being watched
)