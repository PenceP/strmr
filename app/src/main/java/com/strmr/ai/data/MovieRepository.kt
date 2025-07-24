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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.strmr.ai.utils.DateFormatter
import com.strmr.ai.ui.theme.StrmrConstants

class MovieRepository(
    private val movieDao: MovieDao,
    private val collectionDao: CollectionDao,
    traktApi: TraktApiService,
    tmdbApi: TmdbApiService,
    database: StrmrDatabase,
    traktRatingsDao: TraktRatingsDao,
    private val trailerService: TrailerService,
    private val tmdbEnrichmentService: TmdbEnrichmentService
) : BaseMediaRepository<MovieEntity, Movie, TrendingMovie>(
    traktApi, tmdbApi, database, traktRatingsDao
) {
    // These are now inherited from BaseMediaRepository

    fun getTrendingMovies(): Flow<List<MovieEntity>> = movieDao.getTrendingMovies()
    fun getPopularMovies(): Flow<List<MovieEntity>> = movieDao.getPopularMovies()
    

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
        val dataSourceId = when {
            trendingOrder != null -> "trending"
            popularOrder != null -> "popular"
            else -> null
        }
        val orderValue = trendingOrder ?: popularOrder
        
        return tmdbEnrichmentService.enrichMovie(
            movie = movie,
            dataSourceId = dataSourceId,
            orderValue = orderValue
        )
    }

    suspend fun getOrFetchMovie(tmdbId: Int): MovieEntity? {
        val cachedMovie = movieDao.getMovieByTmdbId(tmdbId)
        if (cachedMovie != null) {
            return cachedMovie
        }
        
        return try {
            // Create a minimal Movie object for enrichment
            val basicMovie = Movie(
                title = "", // Will be filled by enrichment service
                year = null,
                ids = MovieIds(tmdb = tmdbId, trakt = null, imdb = null, slug = null)
            )
            
            val entity = tmdbEnrichmentService.enrichMovie(basicMovie)
            entity?.let {
                movieDao.insertMovies(listOf(it))
            }
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
        
        // Use TmdbEnrichmentService to fetch with logo
        return try {
            val entity = tmdbEnrichmentService.enrichMovieWithLogo(tmdbId)
            entity?.let {
                movieDao.insertMovies(listOf(it))
            }
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
            val similarContent = similarResponse.results.take(StrmrConstants.UI.MAX_SIMILAR_ITEMS).map { item ->
                SimilarContent(
                    tmdbId = item.id,
                    title = item.title ?: "",
                    posterUrl = item.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                    backdropUrl = item.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
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

    suspend fun getTraktListMovies(username: String, listSlug: String): List<MovieEntity> {
        Log.d("MovieRepository", "🎬 Fetching Trakt list movies: $username/$listSlug")
        
        return try {
            val listItems = traktApi.getUserListItems(username, listSlug)
            Log.d("MovieRepository", "✅ Received ${listItems.size} items from Trakt list")
            
            // Filter only movies and convert to MovieEntity
            val movies = listItems.filter { it.type == "movie" && it.movie != null }
            Log.d("MovieRepository", "🎬 Found ${movies.size} movies in list")
            
            val movieEntities = movies.mapNotNull { listItem ->
                listItem.movie?.let { movie ->
                    try {
                        fetchAndMapToEntity(movie)
                    } catch (e: Exception) {
                        Log.e("MovieRepository", "Error mapping movie ${movie.title}", e)
                        null
                    }
                }
            }
            
            Log.d("MovieRepository", "✅ Successfully converted ${movieEntities.size} movies to entities")
            
            // Insert/update in database
            if (movieEntities.isNotEmpty()) {
                movieDao.insertMovies(movieEntities)
            }
            
            movieEntities
        } catch (e: Exception) {
            Log.e("MovieRepository", "Error fetching Trakt list $username/$listSlug", e)
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
    
    /**
     * Get official trailer URL for a movie
     */
    suspend fun getMovieTrailer(tmdbId: Int): String? {
        return trailerService.getMovieTrailer(tmdbId)
    }
} 