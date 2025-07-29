package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.Movie
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.repository.MovieRepository
import javax.inject.Inject

/**
 * Use case for getting movie details with automatic caching and fetching logic
 * Replaces the loadMovie logic currently in DetailsViewModel
 */
class GetMovieDetailsUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    /**
     * Get movie details, fetching from remote if not available locally
     * Returns Result to handle errors properly
     */
    suspend operator fun invoke(tmdbId: TmdbId): Result<Movie> {
        return try {
            // Try to get from local database first
            var movie = movieRepository.getMovieByTmdbId(tmdbId)
            
            // If not found locally, refresh from remote
            if (movie == null) {
                movieRepository.refreshMovieDetails(movie?.id ?: com.strmr.ai.domain.model.MovieId(tmdbId.value))
                    .onSuccess { 
                        movie = movieRepository.getMovieByTmdbId(tmdbId)
                    }
                    .onFailure { error ->
                        return Result.failure(error)
                    }
            }
            
            movie?.let { Result.success(it) } 
                ?: Result.failure(Exception("Movie not found: ${tmdbId.value}"))
                
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}