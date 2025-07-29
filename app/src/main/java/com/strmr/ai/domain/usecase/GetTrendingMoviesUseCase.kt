package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.Movie
import com.strmr.ai.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting trending movies with optional refresh
 * Encapsulates the business logic for trending content management
 */
class GetTrendingMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    /**
     * Get trending movies as a Flow for reactive UI updates
     */
    fun getTrendingMoviesFlow(): Flow<List<Movie>> {
        return movieRepository.getTrendingMovies()
    }
    
    /**
     * Refresh trending movies from remote source
     * Returns Result to handle network errors properly
     */
    suspend fun refreshTrendingMovies(): Result<Unit> {
        return movieRepository.refreshTrendingMovies()
    }
    
    /**
     * Get trending movies with automatic refresh if data is stale
     * @param forceRefresh Force refresh even if data is recent
     */
    suspend fun getTrendingMoviesWithRefresh(forceRefresh: Boolean = false): Result<Flow<List<Movie>>> {
        return try {
            if (forceRefresh) {
                refreshTrendingMovies()
                    .onFailure { return Result.failure(it) }
            }
            Result.success(getTrendingMoviesFlow())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}