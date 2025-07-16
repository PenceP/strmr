package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.MovieDao
import com.strmr.ai.data.database.MovieEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.strmr.ai.utils.DateFormatter

class MovieRepository(
    private val movieDao: MovieDao,
    private val traktApi: TraktApiService,
    private val tmdbApi: TmdbApiService
) {
    private val detailsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    private var currentTrendingPage = 0
    private var currentPopularPage = 0
    private val pageSize = 20

    fun getTrendingMovies(): Flow<List<MovieEntity>> = movieDao.getTrendingMovies()
    fun getPopularMovies(): Flow<List<MovieEntity>> = movieDao.getPopularMovies()
    
    fun getTrendingMoviesPagingSource() = movieDao.getMoviesPagingSource()
    
    suspend fun loadMoreTrendingMovies() {
        currentTrendingPage++
        refreshTrendingMovies()
    }
    
    suspend fun loadMorePopularMovies() {
        currentPopularPage++
        refreshPopularMovies()
    }

    suspend fun refreshTrendingMovies() {
        withContext(Dispatchers.IO) {
            val limit = pageSize
            val page = currentTrendingPage + 1 // Trakt API uses 1-based page numbers
            val trending = traktApi.getTrendingMovies(page = page, limit = limit).mapIndexedNotNull { index, trendingMovie ->
                val tmdbId = trendingMovie.movie.ids.tmdb ?: return@mapIndexedNotNull null
                val traktId = trendingMovie.movie.ids.trakt ?: return@mapIndexedNotNull null
                val cached = movieDao.getMovieByTmdbId(tmdbId)
                val now = System.currentTimeMillis()
                val actualIndex = currentTrendingPage * pageSize + index
                if (cached == null || now - cached.lastUpdated > detailsExpiryMs) {
                    fetchAndMapToEntity(trendingMovie.movie, trendingOrder = actualIndex)?.copy(lastUpdated = now)
                } else {
                    cached.copy(trendingOrder = actualIndex)
                }
            }
            Log.d("MovieRepository", "Fetched trending page $page, got IDs: ${trending.map { it.tmdbId }}")
            if (currentTrendingPage == 0) {
                movieDao.updateTrendingMovies(trending)
            } else {
                movieDao.insertMovies(trending)
            }
        }
    }

    suspend fun refreshPopularMovies() {
        withContext(Dispatchers.IO) {
            val limit = pageSize
            val page = currentPopularPage + 1 // Trakt API uses 1-based page numbers
            val popular = traktApi.getPopularMovies(page = page, limit = limit).mapIndexedNotNull { index, movie ->
                val tmdbId = movie.ids.tmdb ?: return@mapIndexedNotNull null
                val traktId = movie.ids.trakt ?: return@mapIndexedNotNull null
                val cached = movieDao.getMovieByTmdbId(tmdbId)
                val now = System.currentTimeMillis()
                val actualIndex = currentPopularPage * pageSize + index
                if (cached == null || now - cached.lastUpdated > detailsExpiryMs) {
                    fetchAndMapToEntity(movie, popularOrder = actualIndex)?.copy(lastUpdated = now)
                } else {
                    cached.copy(popularOrder = actualIndex)
                }
            }
            Log.d("MovieRepository", "Fetched popular page $page, got IDs: ${popular.map { it.tmdbId }}")
            if (currentPopularPage == 0) {
                movieDao.updatePopularMovies(popular)
            } else {
                movieDao.insertMovies(popular)
            }
        }
    }

    private suspend fun fetchAndMapToEntity(
        movie: Movie,
        trendingOrder: Int? = null,
        popularOrder: Int? = null
    ): MovieEntity? {
        val tmdbId = movie.ids.tmdb ?: return null
        val traktId = movie.ids.trakt ?: return null
        val imdbId = movie.ids.imdb
        return try {
            val details = tmdbApi.getMovieDetails(tmdbId)
            val credits = tmdbApi.getMovieCredits(tmdbId)
            val traktRatings = traktApi.getMovieRatings(traktId)
            val cached = movieDao.getMovieByTmdbId(tmdbId)
            MovieEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.title ?: movie.title,
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = cached?.logoUrl, // Preserve existing logo
                traktRating = traktRatings.rating,
                traktVotes = traktRatings.votes,
                year = DateFormatter.extractYear(details.release_date),
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                trendingOrder = trendingOrder ?: cached?.trendingOrder, // Preserve other order
                popularOrder = popularOrder ?: cached?.popularOrder
            )
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching details for ${movie.title}", e)
            null
        }
    }

    suspend fun getOrFetchMovie(tmdbId: Int): MovieEntity? {
        val cachedMovie = movieDao.getMovieByTmdbId(tmdbId)
        if (cachedMovie != null) {
            return cachedMovie
        }
        return try {
            val details = tmdbApi.getMovieDetails(tmdbId)
            val credits = tmdbApi.getMovieCredits(tmdbId)
            val imdbId = details.imdb_id
            val entity = MovieEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.title ?: "",
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = null, // Will be fetched on demand
                traktRating = 0f, // No Trakt rating available here
                traktVotes = 0,   // No Trakt votes available here
                year = DateFormatter.extractYear(details.release_date),
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                }
            )
            movieDao.insertMovies(listOf(entity))
            entity
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching movie with tmdbId $tmdbId", e)
            null
        }
    }

    suspend fun getMovieByTmdbId(tmdbId: Int): MovieEntity? {
        return movieDao.getMovieByTmdbId(tmdbId)
    }

    suspend fun updateMovieLogo(tmdbId: Int, logoUrl: String?) {
        movieDao.updateMovieLogo(tmdbId, logoUrl)
    }

    suspend fun getOrFetchMovieWithLogo(tmdbId: Int): MovieEntity? {
        val cachedMovie = movieDao.getMovieByTmdbId(tmdbId)
        if (cachedMovie != null && !cachedMovie.logoUrl.isNullOrBlank()) {
            return cachedMovie
        }
        // Fetch logo from TMDB if missing
        return try {
            val details = tmdbApi.getMovieDetails(tmdbId)
            val credits = tmdbApi.getMovieCredits(tmdbId)
            val images = tmdbApi.getMovieImages(tmdbId)
            val logo = images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
                ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }
            val logoUrl = logo?.file_path?.let { "https://image.tmdb.org/t/p/w500$it" }
            val imdbId = details.imdb_id
            if (!logoUrl.isNullOrBlank()) {
                movieDao.updateMovieLogo(tmdbId, logoUrl)
            }
            val entity = MovieEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.title ?: "",
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = logoUrl,
                traktRating = cachedMovie?.traktRating,
                traktVotes = cachedMovie?.traktVotes,
                year = DateFormatter.extractYear(details.release_date),
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                trendingOrder = cachedMovie?.trendingOrder,
                popularOrder = cachedMovie?.popularOrder
            )
            movieDao.insertMovies(listOf(entity))
            entity
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching logo for movie $tmdbId", e)
            cachedMovie
        }
    }

    suspend fun getMovieImages(tmdbId: Int): TmdbImagesResponse? {
        return try {
            tmdbApi.getMovieImages(tmdbId)
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching images for movie $tmdbId", e)
            null
        }
    }
} 