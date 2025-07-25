package com.strmr.ai.viewmodel

import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity

sealed class HomeMediaItem {
    data class Movie(val movie: MovieEntity, val progress: Float?, val altBackdropUrl: String? = null) : HomeMediaItem()
    data class TvShow(
        val show: TvShowEntity,
        val progress: Float?,
        val episodeImageUrl: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val episodeOverview: String? = null,
        val episodeAirDate: String? = null,
        val isNextEpisode: Boolean = false
    ) : HomeMediaItem()
    data class Collection(
        val id: String,
        val name: String,
        val backgroundImageUrl: String,
        val nameDisplayMode: String,
        val dataUrl: String? = null
    ) : HomeMediaItem()
} 