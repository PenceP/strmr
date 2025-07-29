package com.strmr.ai.domain.model

import kotlin.time.Duration

/**
 * Domain model for media ratings from various sources
 */
data class Rating(
    val tmdbRating: Float? = null,
    val traktRating: Float? = null,
    val traktVotes: Int? = null,
    val imdbRating: Float? = null,
    val rottenTomatoesRating: Int? = null
) {
    /**
     * Get the primary rating to display, preferring Trakt then TMDB
     */
    val primaryRating: Float?
        get() = traktRating ?: tmdbRating

    /**
     * Check if we have any rating data
     */
    val hasRating: Boolean
        get() = tmdbRating != null || traktRating != null || imdbRating != null || rottenTomatoesRating != null
}

/**
 * Domain model for runtime duration
 */
@JvmInline
value class Runtime(val minutes: Int) {
    val duration: Duration
        get() = Duration.parse("${minutes}m")
        
    val displayText: String
        get() = when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
}