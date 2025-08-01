package com.strmr.ai.data

data class SimilarContent(
    val tmdbId: Int,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val rating: Float?,
    val year: Int?,
    val mediaType: String, // "movie" or "tv"
)
