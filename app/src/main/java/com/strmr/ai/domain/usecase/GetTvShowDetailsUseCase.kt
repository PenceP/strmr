package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.TvShow
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.repository.TvShowRepository
import javax.inject.Inject

/**
 * Use case for getting TV show details with automatic caching and fetching logic
 * Replaces the loadTvShow logic currently in DetailsViewModel
 */
class GetTvShowDetailsUseCase @Inject constructor(
    private val tvShowRepository: TvShowRepository
) {
    /**
     * Get TV show details, fetching from remote if not available locally
     * Returns Result to handle errors properly
     */
    suspend operator fun invoke(tmdbId: TmdbId): Result<TvShow> {
        return try {
            // Try to get from local database first
            var tvShow = tvShowRepository.getTvShowByTmdbId(tmdbId)
            
            // If not found locally, refresh from remote
            if (tvShow == null) {
                tvShowRepository.refreshTvShowDetails(tvShow?.id ?: com.strmr.ai.domain.model.TvShowId(tmdbId.value))
                    .onSuccess { 
                        tvShow = tvShowRepository.getTvShowByTmdbId(tmdbId)
                    }
                    .onFailure { error ->
                        return Result.failure(error)
                    }
            }
            
            tvShow?.let { Result.success(it) } 
                ?: Result.failure(Exception("TV show not found: ${tmdbId.value}"))
                
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}