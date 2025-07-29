package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.repository.SearchRepository
import com.strmr.ai.domain.repository.SearchResults
import javax.inject.Inject

/**
 * Use case for searching across all content types
 * Encapsulates search business logic and provides unified interface
 */
class SearchContentUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    /**
     * Search for content across all sources and types
     * @param query Search query string
     * @return Result containing SearchResults with movies, TV shows, and people
     */
    suspend operator fun invoke(query: String): Result<SearchResults> {
        return try {
            if (query.isBlank()) {
                return Result.success(SearchResults())
            }
            
            if (query.length < 2) {
                // Too short query, return empty results
                return Result.success(SearchResults())
            }
            
            val results = searchRepository.searchMultiSource(query.trim())
            Result.success(results)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search only for movies
     */
    suspend fun searchMovies(query: String) = try {
        if (query.length >= 2) {
            Result.success(searchRepository.searchMovies(query.trim()))
        } else {
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Search only for TV shows
     */
    suspend fun searchTvShows(query: String) = try {
        if (query.length >= 2) {
            Result.success(searchRepository.searchTvShows(query.trim()))
        } else {
            Result.success(emptyList())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}