package com.strmr.ai.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.strmr.ai.data.TraktApiService
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.database.MovieDao
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowDao
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

class MediaPagingSource(
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val movieDao: MovieDao? = null,
    private val tvShowDao: TvShowDao? = null,
    private val mediaType: MediaType
) : PagingSource<Int, Any>() {

    enum class MediaType {
        MOVIES, TV_SHOWS
    }

    override fun getRefreshKey(state: PagingState<Int, Any>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Any> {
        return try {
            val page = params.key ?: 1
            val pageSize = params.loadSize

            Log.d("MediaPagingSource", "📄 Loading page $page for ${mediaType.name}")

            val items = when (mediaType) {
                MediaType.MOVIES -> loadMovies(page, pageSize)
                MediaType.TV_SHOWS -> loadTvShows(page, pageSize)
            }

            val nextKey = if (items.isEmpty()) null else page + 1
            val prevKey = if (page == 1) null else page - 1

            Log.d("MediaPagingSource", "✅ Loaded ${items.size} items for page $page")

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            Log.e("MediaPagingSource", "❌ Error loading page ${params.key}", e)
            LoadResult.Error(e)
        }
    }

    private suspend fun loadMovies(page: Int, pageSize: Int): List<MovieEntity> {
        Log.d("MediaPagingSource", "🎬 Loading movies: page=$page, pageSize=$pageSize")
        
        // Calculate offset for pagination
        val offset = (page - 1) * pageSize
        
        return movieDao?.getTrendingMoviesPaged(limit = pageSize, offset = offset) ?: emptyList()
    }

    private suspend fun loadTvShows(page: Int, pageSize: Int): List<TvShowEntity> {
        Log.d("MediaPagingSource", "📺 Loading TV shows: page=$page, pageSize=$pageSize")
        
        // Calculate offset for pagination
        val offset = (page - 1) * pageSize
        
        return tvShowDao?.getTrendingTvShowsPaged(limit = pageSize, offset = offset) ?: emptyList()
    }
} 