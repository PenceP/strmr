package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.repository.MovieRepository
import com.strmr.ai.domain.repository.TvShowRepository
import javax.inject.Inject

/**
 * Use case for fetching and caching media logos
 * Encapsulates the logo fetching logic currently in HomeViewModel
 */
class FetchMediaLogoUseCase
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
        private val tvShowRepository: TvShowRepository,
    ) {
        /**
         * Fetch and cache movie logo
         */
        suspend fun fetchMovieLogo(tmdbId: TmdbId): Result<Boolean> {
            return try {
                val movie = movieRepository.getMovieByTmdbId(tmdbId)
                val hasLogo = movie?.images?.logoUrl != null
                Result.success(hasLogo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Fetch and cache TV show logo
         */
        suspend fun fetchTvShowLogo(tmdbId: TmdbId): Result<Boolean> {
            return try {
                val tvShow = tvShowRepository.getTvShowByTmdbId(tmdbId)
                val hasLogo = tvShow?.images?.logoUrl != null
                Result.success(hasLogo)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
