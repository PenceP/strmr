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
    traktApi: TraktApiService,
    tmdbApi: TmdbApiService,
    database: StrmrDatabase,
    traktRatingsDao: TraktRatingsDao
) : BaseMediaRepository<MovieEntity, Movie, TrendingMovie>(
    traktApi, tmdbApi, database, traktRatingsDao
) {
    // These are now inherited from BaseMediaRepository

    fun getTrendingMovies(): Flow<List<MovieEntity>> = movieDao.getTrendingMovies()
    fun getPopularMovies(): Flow<List<MovieEntity>> = movieDao.getPopularMovies()
    
    @OptIn(ExperimentalPagingApi::class)
    fun getTrendingMoviesPager(): Flow<PagingData<MovieEntity>> {
        return createPager(
            contentType = ContentType.TRENDING,
            remoteMediator = MoviesRemoteMediator(
                contentType = MoviesRemoteMediator.ContentType.TRENDING,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                movieRepository = this
            ),
            pagingSourceFactory = { movieDao.getTrendingMoviesPagingSource() }
        )
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getPopularMoviesPager(): Flow<PagingData<MovieEntity>> {
        return createPager(
            contentType = ContentType.POPULAR,
            remoteMediator = MoviesRemoteMediator(
                contentType = MoviesRemoteMediator.ContentType.POPULAR,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                movieRepository = this
            ),
            pagingSourceFactory = { movieDao.getPopularMoviesPagingSource() }
        )
    }
    
    suspend fun loadMoreTrendingMovies() {
        refreshContent(
            contentType = ContentType.TRENDING,
            fetchTrendingFromTrakt = { page, limit -> traktApi.getTrendingMovies(page, limit) },
            fetchPopularFromTrakt = { _, _ -> emptyList() }, // Not used for trending
            mapTrendingToEntity = { trendingMovie, order -> 
                fetchAndMapToEntity(trendingMovie.movie, trendingOrder = order)
            },
            mapPopularToEntity = { _, _ -> null } // Not used for trending
        )
    }
    
    suspend fun loadMorePopularMovies() {
        refreshContent(
            contentType = ContentType.POPULAR,
            fetchTrendingFromTrakt = { _, _ -> emptyList() }, // Not used for popular
            fetchPopularFromTrakt = { page, limit -> traktApi.getPopularMovies(page, limit) },
            mapTrendingToEntity = { _, _ -> null }, // Not used for popular
            mapPopularToEntity = { movie, order -> 
                fetchAndMapToEntity(movie, popularOrder = order)
            }
        )
    }

    suspend fun refreshTrendingMovies() = loadMoreTrendingMovies()
    suspend fun refreshPopularMovies() = loadMorePopularMovies()

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
            
            // Use cached Trakt ratings instead of direct API call
            val traktRatings = getTraktRatings(traktId)
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
                traktRating = traktRatings?.rating,
                traktVotes = traktRatings?.votes,
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

    // These methods are now inherited from BaseMediaRepository:
    // - getItemByTmdbId() -> getMovieByTmdbId()
    // - updateItemLogo() -> updateMovieLogo()  
    // - clearNullLogos()
    
    suspend fun getMovieByTmdbId(tmdbId: Int): MovieEntity? = getItemByTmdbId(tmdbId)
    suspend fun updateMovieLogo(tmdbId: Int, logoUrl: String?) = updateItemLogo(tmdbId, logoUrl)

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

    // Override getTraktRatings to handle movie-specific API calls
    suspend override fun getTraktRatings(traktId: Int): TraktRatingsEntity? {
        val cached = super.getTraktRatings(traktId)
        if (cached != null) return cached
        
        // Fetch from API if not cached or expired
        return try {
            val api = traktApi.getMovieRatings(traktId)
            saveTraktRatings(traktId, api.rating, api.votes)
            super.getTraktRatings(traktId)
        } catch (e: Exception) {
            null
        }
    }

    // Implement abstract methods from BaseMediaRepository
    override fun getLogTag(): String = "MovieRepository"
    
    override suspend fun getTmdbId(item: Movie): Int? = item.ids.tmdb
    
    override suspend fun getTraktId(item: Movie): Int? = item.ids.trakt
    
    override suspend fun getTmdbIdFromTrending(item: TrendingMovie): Int? = item.movie.ids.tmdb
    
    override suspend fun getTraktIdFromTrending(item: TrendingMovie): Int? = item.movie.ids.trakt
    
    override suspend fun updateEntityTimestamp(entity: MovieEntity): MovieEntity = 
        entity.copy(lastUpdated = System.currentTimeMillis())
    
    // Implement abstract DAO methods from BaseMediaRepository
    override suspend fun insertItems(items: List<MovieEntity>) = movieDao.insertMovies(items)
    override suspend fun updateTrendingItems(items: List<MovieEntity>) = movieDao.updateTrendingMovies(items)
    override suspend fun updatePopularItems(items: List<MovieEntity>) = movieDao.updatePopularMovies(items)
    override suspend fun getItemByTmdbId(tmdbId: Int): MovieEntity? = movieDao.getMovieByTmdbId(tmdbId)
    override suspend fun updateItemLogo(tmdbId: Int, logoUrl: String?) = movieDao.updateMovieLogo(tmdbId, logoUrl)
    override suspend fun clearNullLogos() = movieDao.clearNullLogos()
} 