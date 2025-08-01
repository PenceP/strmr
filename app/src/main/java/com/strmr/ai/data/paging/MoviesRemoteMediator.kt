package com.strmr.ai.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TraktApiService
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.StrmrDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class MoviesRemoteMediator(
    private val contentType: ContentType,
    private val database: StrmrDatabase,
    private val traktApi: TraktApiService,
    private val tmdbApi: TmdbApiService,
    private val movieRepository: MovieRepository,
) : RemoteMediator<Int, MovieEntity>() {
    enum class ContentType {
        TRENDING,
        POPULAR,
    }

    override suspend fun initialize(): InitializeAction {
        // Check if we need to refresh - for now, always refresh on initialize
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MovieEntity>,
    ): MediatorResult {
        return try {
            val page =
                when (loadType) {
                    LoadType.REFRESH -> 1
                    LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                    LoadType.APPEND -> {
                        val lastItem = state.lastItemOrNull()
                        if (lastItem == null) {
                            1
                        } else {
                            // Calculate next page based on current data size
                            val currentSize =
                                when (contentType) {
                                    ContentType.TRENDING -> database.movieDao().getTrendingMoviesCount()
                                    ContentType.POPULAR -> database.movieDao().getPopularMoviesCount()
                                }
                            (currentSize / 20) + 1
                        }
                    }
                }

            Log.d("MoviesRemoteMediator", "📄 Loading ${contentType.name} movies page $page")

            val movies = fetchMoviesFromApi(page)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    // Clear existing data for refresh
                    when (contentType) {
                        ContentType.TRENDING -> database.movieDao().clearTrendingOrder()
                        ContentType.POPULAR -> database.movieDao().clearPopularOrder()
                    }
                }

                // Insert new movies
                database.movieDao().insertMovies(movies)
            }

            Log.d("MoviesRemoteMediator", "✅ Loaded ${movies.size} ${contentType.name} movies for page $page")

            MediatorResult.Success(
                endOfPaginationReached = movies.isEmpty(),
            )
        } catch (e: Exception) {
            Log.e("MoviesRemoteMediator", "❌ Error loading ${contentType.name} movies", e)
            MediatorResult.Error(e)
        }
    }

    private suspend fun fetchMoviesFromApi(page: Int): List<MovieEntity> {
        return withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.TRENDING -> {
                    val trending = traktApi.getTrendingMovies(page = page, limit = 20)
                    trending.mapIndexedNotNull { index, trendingMovie ->
                        movieRepository.mapTraktMovieToEntity(
                            trendingMovie.movie,
                            trendingOrder = ((page - 1) * 20) + index,
                        )
                    }
                }
                ContentType.POPULAR -> {
                    val popular = traktApi.getPopularMovies(page = page, limit = 20)
                    popular.mapIndexedNotNull { index, movie ->
                        movieRepository.mapTraktMovieToEntity(
                            movie,
                            popularOrder = ((page - 1) * 20) + index,
                        )
                    }
                }
            }
        }
    }
}
