package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.TvShow
import com.strmr.ai.domain.repository.TvShowRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting trending TV shows with optional refresh
 * Encapsulates the business logic for trending content management
 */
class GetTrendingTvShowsUseCase
    @Inject
    constructor(
        private val tvShowRepository: TvShowRepository,
    ) {
        /**
         * Get trending TV shows as a Flow for reactive UI updates
         */
        fun getTrendingTvShowsFlow(): Flow<List<TvShow>> {
            return tvShowRepository.getTrendingTvShows()
        }

        /**
         * Refresh trending TV shows from remote source
         * Returns Result to handle network errors properly
         */
        suspend fun refreshTrendingTvShows(): Result<Unit> {
            return tvShowRepository.refreshTrendingTvShows()
        }

        /**
         * Get trending TV shows with automatic refresh if data is stale
         * @param forceRefresh Force refresh even if data is recent
         */
        suspend fun getTrendingTvShowsWithRefresh(forceRefresh: Boolean = false): Result<Flow<List<TvShow>>> {
            return try {
                if (forceRefresh) {
                    refreshTrendingTvShows()
                        .onFailure { return Result.failure(it) }
                }
                Result.success(getTrendingTvShowsFlow())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
