package com.strmr.ai.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.strmr.ai.data.database.MovieDao
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.CollectionDao
import com.strmr.ai.data.database.CollectionEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.database.TraktRatingsEntity
import com.strmr.ai.data.paging.MoviesRemoteMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.strmr.ai.utils.DateFormatter

class MovieRepository(
    private val movieDao: MovieDao,
    private val collectionDao: CollectionDao,
    private val traktApi: TraktApiService,
    private val tmdbApi: TmdbApiService,
    private val database: StrmrDatabase,
    private val traktRatingsDao: TraktRatingsDao // <-- Inject DAO
) {
    private val detailsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    private val ratingsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    private var currentTrendingPage = 0
    private var currentPopularPage = 0
    private val pageSize = 20

    fun getTrendingMovies(): Flow<List<MovieEntity>> = movieDao.getTrendingMovies()
    fun getPopularMovies(): Flow<List<MovieEntity>> = movieDao.getPopularMovies()
    
    @OptIn(ExperimentalPagingApi::class)
    fun getTrendingMoviesPager(): Flow<PagingData<MovieEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            remoteMediator = MoviesRemoteMediator(
                contentType = MoviesRemoteMediator.ContentType.TRENDING,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                movieRepository = this
            ),
            pagingSourceFactory = { movieDao.getTrendingMoviesPagingSource() }
        ).flow
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getPopularMoviesPager(): Flow<PagingData<MovieEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                prefetchDistance = 5
            ),
            remoteMediator = MoviesRemoteMediator(
                contentType = MoviesRemoteMediator.ContentType.POPULAR,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                movieRepository = this
            ),
            pagingSourceFactory = { movieDao.getPopularMoviesPagingSource() }
        ).flow
    }
    
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

    suspend fun mapTraktMovieToEntity(
        movie: Movie,
        trendingOrder: Int? = null,
        popularOrder: Int? = null
    ): MovieEntity? {
        return fetchAndMapToEntity(movie, trendingOrder, popularOrder)
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
            
            // Fetch collection data if available
            val collection = details.belongs_to_collection?.let { belongsToCollection ->
                getOrFetchCollection(belongsToCollection.id)
            }
            
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
                belongsToCollection = details.belongs_to_collection,
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
            Log.d("MovieRepository", "🎬 Movie details fetched for TMDB ID: $tmdbId")
            Log.d("MovieRepository", "🎬 Movie title: ${details.title}")
            Log.d("MovieRepository", "🎬 Belongs to collection: ${details.belongs_to_collection}")
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
                },
                belongsToCollection = details.belongs_to_collection
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

    suspend fun clearNullLogos() {
        movieDao.clearNullLogos()
    }

    suspend fun getOrFetchCollection(collectionId: Int): CollectionEntity? {
        Log.d("MovieRepository", "🔍 getOrFetchCollection called for collectionId: $collectionId")
        val cachedCollection = collectionDao.getCollectionById(collectionId)
        if (cachedCollection != null) {
            Log.d("MovieRepository", "✅ Returning cached collection: ${cachedCollection.name} with ${cachedCollection.parts.size} parts")
            return cachedCollection
        }
        
        Log.d("MovieRepository", "📡 Collection not cached, fetching from API")
        return try {
            val collection = tmdbApi.getCollectionDetails(collectionId)
            Log.d("MovieRepository", "✅ API response received: ${collection.name} with ${collection.parts.size} parts")
            val entity = CollectionEntity(
                id = collection.id,
                name = collection.name,
                overview = collection.overview,
                posterPath = collection.poster_path,
                backdropPath = collection.backdrop_path,
                parts = collection.parts,
                lastUpdated = System.currentTimeMillis()
            )
            collectionDao.insertCollection(entity)
            Log.d("MovieRepository", "✅ Collection saved to database: ${entity.name}")
            entity
        } catch (e: Exception) {
            Log.e("MovieRepository", "❌ Error fetching collection with id $collectionId", e)
            null
        }
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
            Log.d("MovieRepository", "🎬 Movie details fetched for TMDB ID: $tmdbId (with logo)")
            Log.d("MovieRepository", "🎬 Movie title: ${details.title}")
            Log.d("MovieRepository", "🎬 Belongs to collection: ${details.belongs_to_collection}")
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
                belongsToCollection = details.belongs_to_collection,
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

    suspend fun getOrFetchSimilarMovies(tmdbId: Int): List<SimilarContent> {
        val cachedMovie = movieDao.getMovieByTmdbId(tmdbId)
        if (cachedMovie != null && cachedMovie.similar.isNotEmpty()) {
            return cachedMovie.similar
        }
        
        return try {
            val similarResponse = tmdbApi.getSimilarMovies(tmdbId)
            val similarContent = similarResponse.results.take(10).map { item ->
                SimilarContent(
                    tmdbId = item.id,
                    title = item.title ?: "",
                    posterUrl = item.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                    backdropUrl = item.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                    rating = item.vote_average,
                    year = DateFormatter.extractYear(item.release_date),
                    mediaType = "movie"
                )
            }
            
            // Update the movie entity with similar content
            cachedMovie?.let { movie ->
                val updatedMovie = movie.copy(similar = similarContent)
                movieDao.insertMovies(listOf(updatedMovie))
            }
            
            similarContent
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching similar movies for $tmdbId", e)
            emptyList()
        }
    }

    suspend fun getTraktRatings(traktId: Int): TraktRatingsEntity? {
        val cached = traktRatingsDao.getRatings(traktId)
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.updatedAt < ratingsExpiryMs) {
            return cached
        }
        // Fetch from API
        return try {
            val api = traktApi.getMovieRatings(traktId)
            val entity = TraktRatingsEntity(
                traktId = traktId,
                rating = api.rating,
                votes = api.votes,
                updatedAt = now
            )
            traktRatingsDao.insertOrUpdate(entity)
            entity
        } catch (e: Exception) {
            // If API fails, return stale cache if available
            cached
        }
    }
} 