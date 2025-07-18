package com.strmr.ai.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService
) {
    
    /**
     * Performs multi-source search across Trakt and TMDB
     * Returns combined results organized by type
     */
    suspend fun searchMultiSource(query: String): SearchResults {
        return withContext(Dispatchers.IO) {
            Log.d("SearchRepository", "🔍 Searching for: '$query'")
            
            try {
                // Perform searches in parallel
                val traktMoviesDeferred = async { searchTraktMovies(query) }
                val traktShowsDeferred = async { searchTraktShows(query) }
                val traktPeopleDeferred = async { searchTraktPeople(query) }
                
                val tmdbMoviesDeferred = async { searchTmdbMovies(query) }
                val tmdbShowsDeferred = async { searchTmdbShows(query) }
                val tmdbPeopleDeferred = async { searchTmdbPeople(query) }
                
                // Wait for all searches to complete
                val traktMovies = traktMoviesDeferred.await()
                val traktShows = traktShowsDeferred.await()
                val traktPeople = traktPeopleDeferred.await()
                
                val tmdbMovies = tmdbMoviesDeferred.await()
                val tmdbShows = tmdbShowsDeferred.await()
                val tmdbPeople = tmdbPeopleDeferred.await()
                
                // Combine and deduplicate results
                val combinedMovies = combineMovieResults(traktMovies, tmdbMovies)
                val combinedShows = combineShowResults(traktShows, tmdbShows)
                val combinedPeople = combinePeopleResults(traktPeople, tmdbPeople)
                
                Log.d("SearchRepository", "🎬 Search results: ${combinedMovies.size} movies, ${combinedShows.size} shows, ${combinedPeople.size} people")
                Log.d("SearchRepository", "🔍 People search details: Trakt=${traktPeople.size}, TMDB=${tmdbPeople.size}, Combined=${combinedPeople.size}")
                
                SearchResults(
                    movies = combinedMovies,
                    tvShows = combinedShows,
                    people = combinedPeople
                )
            } catch (e: Exception) {
                Log.e("SearchRepository", "❌ Search failed: ${e.message}", e)
                SearchResults(emptyList(), emptyList(), emptyList())
            }
        }
    }
    
    private suspend fun searchTraktMovies(query: String): List<TraktSearchResult> {
        return try {
            traktApiService.searchMovies(query, limit = 10)
        } catch (e: Exception) {
            Log.w("SearchRepository", "Trakt movie search failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun searchTraktShows(query: String): List<TraktSearchResult> {
        return try {
            traktApiService.searchTvShows(query, limit = 10)
        } catch (e: Exception) {
            Log.w("SearchRepository", "Trakt show search failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun searchTraktPeople(query: String): List<TraktSearchResult> {
        return try {
            traktApiService.searchPeople(query, limit = 10)
        } catch (e: Exception) {
            Log.w("SearchRepository", "Trakt people search failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun searchTmdbMovies(query: String): List<TmdbSearchResult> {
        return try {
            tmdbApiService.searchMovies(query).results
        } catch (e: Exception) {
            Log.w("SearchRepository", "TMDB movie search failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun searchTmdbShows(query: String): List<TmdbSearchResult> {
        return try {
            tmdbApiService.searchTvShows(query).results
        } catch (e: Exception) {
            Log.w("SearchRepository", "TMDB show search failed: ${e.message}")
            emptyList()
        }
    }
    
    private suspend fun searchTmdbPeople(query: String): List<TmdbSearchResult> {
        return try {
            tmdbApiService.searchPeople(query).results
        } catch (e: Exception) {
            Log.w("SearchRepository", "TMDB people search failed: ${e.message}")
            emptyList()
        }
    }
    
    private fun combineMovieResults(
        traktResults: List<TraktSearchResult>,
        tmdbResults: List<TmdbSearchResult>
    ): List<SearchResultItem> {
        val combinedResults = mutableListOf<SearchResultItem>()
        
        // Add Trakt results
        traktResults.forEach { traktResult ->
            traktResult.movie?.let { movie ->
                combinedResults.add(
                    SearchResultItem.Movie(
                        id = movie.ids.tmdb ?: 0,
                        title = movie.title,
                        year = movie.year,
                        posterPath = null, // Trakt doesn't provide poster paths
                        backdropPath = null,
                        overview = null,
                        rating = null,
                        source = "trakt",
                        traktId = movie.ids.trakt,
                        tmdbId = movie.ids.tmdb,
                        imdbId = movie.ids.imdb
                    )
                )
            }
        }
        
        // Add TMDB results (with deduplication)
        tmdbResults.forEach { tmdbResult ->
            val existingItem = combinedResults.find { 
                it is SearchResultItem.Movie && it.tmdbId == tmdbResult.id 
            }
            
            if (existingItem == null) {
                combinedResults.add(
                    SearchResultItem.Movie(
                        id = tmdbResult.id,
                        title = tmdbResult.title ?: "",
                        year = tmdbResult.release_date?.take(4)?.toIntOrNull(),
                        posterPath = tmdbResult.poster_path,
                        backdropPath = tmdbResult.backdrop_path,
                        overview = tmdbResult.overview,
                        rating = tmdbResult.vote_average,
                        source = "tmdb",
                        traktId = null,
                        tmdbId = tmdbResult.id,
                        imdbId = null
                    )
                )
            } else if (existingItem is SearchResultItem.Movie) {
                // Merge TMDB data into existing Trakt result
                val mergedItem = existingItem.copy(
                    posterPath = tmdbResult.poster_path,
                    backdropPath = tmdbResult.backdrop_path,
                    overview = tmdbResult.overview,
                    rating = tmdbResult.vote_average,
                    source = "combined"
                )
                val index = combinedResults.indexOf(existingItem)
                combinedResults[index] = mergedItem
            }
        }
        
        return combinedResults.take(20) // Limit to 20 results
    }
    
    private fun combineShowResults(
        traktResults: List<TraktSearchResult>,
        tmdbResults: List<TmdbSearchResult>
    ): List<SearchResultItem> {
        val combinedResults = mutableListOf<SearchResultItem>()
        
        // Add Trakt results
        traktResults.forEach { traktResult ->
            traktResult.show?.let { show ->
                combinedResults.add(
                    SearchResultItem.TvShow(
                        id = show.ids.tmdb ?: 0,
                        title = show.title,
                        year = show.year,
                        posterPath = null,
                        backdropPath = null,
                        overview = null,
                        rating = null,
                        source = "trakt",
                        traktId = show.ids.trakt,
                        tmdbId = show.ids.tmdb,
                        imdbId = show.ids.imdb
                    )
                )
            }
        }
        
        // Add TMDB results (with deduplication)
        tmdbResults.forEach { tmdbResult ->
            val existingItem = combinedResults.find { 
                it is SearchResultItem.TvShow && it.tmdbId == tmdbResult.id 
            }
            
            if (existingItem == null) {
                combinedResults.add(
                    SearchResultItem.TvShow(
                        id = tmdbResult.id,
                        title = tmdbResult.name ?: "",
                        year = tmdbResult.first_air_date?.take(4)?.toIntOrNull(),
                        posterPath = tmdbResult.poster_path,
                        backdropPath = tmdbResult.backdrop_path,
                        overview = tmdbResult.overview,
                        rating = tmdbResult.vote_average,
                        source = "tmdb",
                        traktId = null,
                        tmdbId = tmdbResult.id,
                        imdbId = null
                    )
                )
            } else if (existingItem is SearchResultItem.TvShow) {
                // Merge TMDB data into existing Trakt result
                val mergedItem = existingItem.copy(
                    posterPath = tmdbResult.poster_path,
                    backdropPath = tmdbResult.backdrop_path,
                    overview = tmdbResult.overview,
                    rating = tmdbResult.vote_average,
                    source = "combined"
                )
                val index = combinedResults.indexOf(existingItem)
                combinedResults[index] = mergedItem
            }
        }
        
        return combinedResults.take(20) // Limit to 20 results
    }
    
    private fun combinePeopleResults(
        traktResults: List<TraktSearchResult>,
        tmdbResults: List<TmdbSearchResult>
    ): List<SearchResultItem> {
        val combinedResults = mutableListOf<SearchResultItem>()
        
        // Add Trakt results
        traktResults.forEach { traktResult ->
            traktResult.person?.let { person ->
                combinedResults.add(
                    SearchResultItem.Person(
                        id = person.ids.tmdb ?: 0,
                        name = person.name,
                        profilePath = null,
                        knownForDepartment = null,
                        knownFor = emptyList(),
                        source = "trakt",
                        traktId = person.ids.trakt,
                        tmdbId = person.ids.tmdb,
                        imdbId = person.ids.imdb
                    )
                )
            }
        }
        
        // Add TMDB results (with deduplication)
        tmdbResults.forEach { tmdbResult ->
            val existingItem = combinedResults.find { 
                it is SearchResultItem.Person && it.tmdbId == tmdbResult.id 
            }
            
            if (existingItem == null) {
                combinedResults.add(
                    SearchResultItem.Person(
                        id = tmdbResult.id,
                        name = tmdbResult.name ?: "",
                        profilePath = tmdbResult.profile_path,
                        knownForDepartment = tmdbResult.known_for_department,
                        knownFor = tmdbResult.known_for?.map { knownFor ->
                            SearchResultItem.KnownForItem(
                                id = knownFor.id,
                                title = knownFor.title ?: knownFor.name ?: "",
                                mediaType = knownFor.media_type,
                                posterPath = knownFor.poster_path
                            )
                        } ?: emptyList(),
                        source = "tmdb",
                        traktId = null,
                        tmdbId = tmdbResult.id,
                        imdbId = null
                    )
                )
            } else if (existingItem is SearchResultItem.Person) {
                // Merge TMDB data into existing Trakt result
                val mergedItem = existingItem.copy(
                    profilePath = tmdbResult.profile_path,
                    knownForDepartment = tmdbResult.known_for_department,
                    knownFor = tmdbResult.known_for?.map { knownFor ->
                        SearchResultItem.KnownForItem(
                            id = knownFor.id,
                            title = knownFor.title ?: knownFor.name ?: "",
                            mediaType = knownFor.media_type,
                            posterPath = knownFor.poster_path
                        )
                    } ?: emptyList(),
                    source = "combined"
                )
                val index = combinedResults.indexOf(existingItem)
                combinedResults[index] = mergedItem
            }
        }
        
        return combinedResults.take(20) // Limit to 20 results
    }
}

data class SearchResults(
    val movies: List<SearchResultItem>,
    val tvShows: List<SearchResultItem>,
    val people: List<SearchResultItem>
)

sealed class SearchResultItem {
    data class Movie(
        val id: Int,
        val title: String,
        val year: Int?,
        val posterPath: String?,
        val backdropPath: String?,
        val overview: String?,
        val rating: Float?,
        val source: String,
        val traktId: Int?,
        val tmdbId: Int?,
        val imdbId: String?
    ) : SearchResultItem()
    
    data class TvShow(
        val id: Int,
        val title: String,
        val year: Int?,
        val posterPath: String?,
        val backdropPath: String?,
        val overview: String?,
        val rating: Float?,
        val source: String,
        val traktId: Int?,
        val tmdbId: Int?,
        val imdbId: String?
    ) : SearchResultItem()
    
    data class Person(
        val id: Int,
        val name: String,
        val profilePath: String?,
        val knownForDepartment: String?,
        val knownFor: List<KnownForItem>,
        val source: String,
        val traktId: Int?,
        val tmdbId: Int?,
        val imdbId: String?
    ) : SearchResultItem()
    
    data class KnownForItem(
        val id: Int,
        val title: String,
        val mediaType: String,
        val posterPath: String?
    )
}