package com.strmr.ai.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Clean domain model for TV shows, free from database and network concerns
 */
@Immutable
data class TvShow(
    val id: TvShowId,
    val tmdbId: TmdbId,
    val imdbId: ImdbId? = null,
    val title: String,
    val overview: String? = null,
    val year: Int? = null,
    val firstAirDate: LocalDate? = null,
    val lastAirDate: LocalDate? = null,
    val runtime: Runtime? = null, // Episode runtime
    val rating: Rating = Rating(),
    val genres: List<Genre> = emptyList(),
    val images: MediaImages = MediaImages(),
    val cast: List<CastMember> = emptyList(),
    val similarShows: List<SimilarTvShow> = emptyList(),
    val lastUpdated: Long = 0L,
) {
    /**
     * Get display title with year if available
     */
    val displayTitle: String
        get() = year?.let { "$title ($it)" } ?: title

    /**
     * Check if this show has sufficient data for display
     */
    val isComplete: Boolean
        get() = title.isNotBlank() && images.hasImages

    /**
     * Get short overview for preview text
     */
    val shortOverview: String?
        get() =
            overview?.let {
                if (it.length > 150) "${it.take(147)}..." else it
            }

    /**
     * Determine if show is currently airing
     */
    val isCurrentlyAiring: Boolean
        get() = firstAirDate != null && lastAirDate == null

    /**
     * Get air date range text
     */
    val airDateRange: String?
        get() =
            when {
                firstAirDate == null -> null
                lastAirDate == null -> "${firstAirDate.year} - Present"
                firstAirDate.year == lastAirDate.year -> firstAirDate.year.toString()
                else -> "${firstAirDate.year} - ${lastAirDate.year}"
            }
}

/**
 * Simplified model for similar/related TV shows
 */
@Immutable
data class SimilarTvShow(
    val id: TvShowId,
    val tmdbId: TmdbId,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val rating: Float? = null,
)
