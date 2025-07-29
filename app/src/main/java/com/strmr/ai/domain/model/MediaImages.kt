package com.strmr.ai.domain.model

/**
 * Domain model for media images (posters, backdrops, logos)
 */
data class MediaImages(
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val logoUrl: String? = null
) {
    /**
     * Get the best available image for display
     */
    val primaryImageUrl: String?
        get() = posterUrl ?: backdropUrl

    /**
     * Check if we have any image data
     */
    val hasImages: Boolean
        get() = posterUrl != null || backdropUrl != null || logoUrl != null
}