package com.strmr.ai.domain.repository

/**
 * Domain model for unified search results
 */
data class SearchResults(
    val movies: List<SearchMovie> = emptyList(),
    val tvShows: List<SearchTvShow> = emptyList(),
    val people: List<SearchPerson> = emptyList()
)

/**
 * Domain models for search result items
 */
data class SearchMovie(
    val tmdbId: Int,
    val title: String,
    val year: Int? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val rating: Float? = null
)

data class SearchTvShow(
    val tmdbId: Int,
    val title: String,
    val year: Int? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val rating: Float? = null
)

data class SearchPerson(
    val tmdbId: Int,
    val name: String,
    val profileUrl: String? = null,
    val knownFor: String? = null
)

/**
 * Clean domain repository interface for search functionality
 */
interface SearchRepository {
    
    /**
     * Search across multiple sources (Trakt + TMDB) and return unified results
     */
    suspend fun searchMultiSource(query: String): SearchResults
    
    /**
     * Search only movies
     */
    suspend fun searchMovies(query: String): List<SearchMovie>
    
    /**
     * Search only TV shows
     */
    suspend fun searchTvShows(query: String): List<SearchTvShow>
    
    /**
     * Search only people
     */
    suspend fun searchPeople(query: String): List<SearchPerson>
}