package com.strmr.ai.domain.repository

import com.strmr.ai.domain.model.SimilarTvShow
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.model.TvShow
import com.strmr.ai.domain.model.TvShowId
import kotlinx.coroutines.flow.Flow

/**
 * Clean domain repository interface for TV shows
 * Implementation will be in data layer, removing direct dependencies on database/network
 */
interface TvShowRepository {
    // Core TV show access
    suspend fun getTvShow(id: TvShowId): TvShow?

    suspend fun getTvShowByTmdbId(tmdbId: TmdbId): TvShow?

    // Collections (trending, popular, etc)
    fun getTrendingTvShows(): Flow<List<TvShow>>

    fun getPopularTvShows(): Flow<List<TvShow>>

    fun getAiringTodayTvShows(): Flow<List<TvShow>>

    fun getOnTheAirTvShows(): Flow<List<TvShow>>

    fun getTopRatedTvShows(): Flow<List<TvShow>>

    // Search and discovery
    suspend fun searchTvShows(query: String): List<TvShow>

    suspend fun getSimilarTvShows(tvShowId: TvShowId): List<SimilarTvShow>

    // Content refresh and sync
    suspend fun refreshTrendingTvShows(): Result<Unit>

    suspend fun refreshPopularTvShows(): Result<Unit>

    suspend fun refreshTvShowDetails(tvShowId: TvShowId): Result<TvShow>

    // Episodes and seasons (basic interface, can be expanded later)
    suspend fun getSeasonCount(tvShowId: TvShowId): Int

    suspend fun getEpisodeCount(
        tvShowId: TvShowId,
        seasonNumber: Int,
    ): Int

    // Utility
    suspend fun getTvShowTrailer(tvShowId: TvShowId): String?

    suspend fun clearObsoleteData()
}
