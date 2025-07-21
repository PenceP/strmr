package com.strmr.ai.ui.components

/**
 * Interface that defines the common properties needed for media items in paging rows.
 * Both MovieEntity and TvShowEntity can implement this interface.
 */
interface MediaItem {
    val tmdbId: Int
    val title: String
    val posterUrl: String?
}