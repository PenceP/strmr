package com.strmr.ai.domain.model

import java.time.LocalDate

/**
 * Clean domain model for movies, free from database and network concerns
 */
data class Movie(
    val id: MovieId,
    val tmdbId: TmdbId,
    val imdbId: ImdbId? = null,
    val title: String,
    val overview: String? = null,
    val year: Int? = null,
    val releaseDate: LocalDate? = null,
    val runtime: Runtime? = null,
    val rating: Rating = Rating(),
    val genres: List<Genre> = emptyList(),
    val images: MediaImages = MediaImages(),
    val cast: List<CastMember> = emptyList(),
    val collection: Collection? = null,
    val similarMovies: List<SimilarMovie> = emptyList(),
    val lastUpdated: Long = 0L
) {
    /**
     * Get display title with year if available
     */
    val displayTitle: String
        get() = year?.let { "$title ($it)" } ?: title

    /**
     * Check if this movie has sufficient data for display
     */
    val isComplete: Boolean
        get() = title.isNotBlank() && images.hasImages

    /**
     * Get short overview for preview text
     */
    val shortOverview: String?
        get() = overview?.let { 
            if (it.length > 150) "${it.take(147)}..." else it 
        }
}

/**
 * Simplified model for similar/related movies
 */
data class SimilarMovie(
    val id: MovieId,
    val tmdbId: TmdbId,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val rating: Float? = null
)