package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.repository.MovieRepository
import com.strmr.ai.domain.repository.TvShowRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Use case for clearing obsolete data (like null logos)
 * This removes business logic from MainActivity
 */
class ClearObsoleteDataUseCase
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
        private val tvShowRepository: TvShowRepository,
    ) {
        /**
         * Clear all obsolete data from the system
         * This includes null logos and other cleanup tasks
         */
        suspend operator fun invoke(): Result<Unit> {
            return try {
                coroutineScope {
                    // Run cleanup operations in parallel for better performance
                    val movieCleanup = async { movieRepository.clearObsoleteData() }
                    val tvShowCleanup = async { tvShowRepository.clearObsoleteData() }

                    // Wait for both to complete
                    movieCleanup.await()
                    tvShowCleanup.await()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
