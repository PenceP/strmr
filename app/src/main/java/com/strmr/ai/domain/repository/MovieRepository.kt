package com.strmr.ai.domain.repository

import com.strmr.ai.domain.model.Collection
import com.strmr.ai.domain.model.Movie
import com.strmr.ai.domain.model.MovieId
import com.strmr.ai.domain.model.SimilarMovie
import com.strmr.ai.domain.model.TmdbId
import kotlinx.coroutines.flow.Flow

/**
 * Clean domain repository interface for movies
 * Implementation will be in data layer, removing direct dependencies on database/network
 */
interface MovieRepository {
    // Core movie access
    suspend fun getMovie(id: MovieId): Movie?

    suspend fun getMovieByTmdbId(tmdbId: TmdbId): Movie?

    // Collections (trending, popular, etc)
    fun getTrendingMovies(): Flow<List<Movie>>

    fun getPopularMovies(): Flow<List<Movie>>

    fun getNowPlayingMovies(): Flow<List<Movie>>

    fun getUpcomingMovies(): Flow<List<Movie>>

    fun getTopRatedMovies(): Flow<List<Movie>>

    // Search and discovery
    suspend fun searchMovies(query: String): List<Movie>

    suspend fun getSimilarMovies(movieId: MovieId): List<SimilarMovie>

    // Collections
    suspend fun getCollection(collectionId: Int): Collection?

    // Content refresh and sync
    suspend fun refreshTrendingMovies(): Result<Unit>

    suspend fun refreshPopularMovies(): Result<Unit>

    suspend fun refreshMovieDetails(movieId: MovieId): Result<Movie>

    // User-specific data
    suspend fun getTraktListMovies(
        username: String,
        listSlug: String,
    ): List<Movie>

    // Utility
    suspend fun getMovieTrailer(movieId: MovieId): String?

    suspend fun clearObsoleteData()
}
