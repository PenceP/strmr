package com.strmr.ai.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TraktApiService
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPagingApi::class)
class TvShowsRemoteMediator(
    private val contentType: ContentType,
    private val database: StrmrDatabase,
    private val traktApi: TraktApiService,
    private val tmdbApi: TmdbApiService,
    private val tvShowRepository: TvShowRepository,
) : RemoteMediator<Int, TvShowEntity>() {
    enum class ContentType {
        TRENDING,
        POPULAR,
    }

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, TvShowEntity>,
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
                            val currentSize =
                                when (contentType) {
                                    ContentType.TRENDING -> database.tvShowDao().getTrendingTvShowsCount()
                                    ContentType.POPULAR -> database.tvShowDao().getPopularTvShowsCount()
                                }
                            (currentSize / 20) + 1
                        }
                    }
                }

            Log.d("TvShowsRemoteMediator", "📄 Loading ${contentType.name} TV shows page $page")

            val tvShows = fetchTvShowsFromApi(page)

            database.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    when (contentType) {
                        ContentType.TRENDING -> database.tvShowDao().clearTrendingOrder()
                        ContentType.POPULAR -> database.tvShowDao().clearPopularOrder()
                    }
                }

                database.tvShowDao().insertTvShows(tvShows)
            }

            Log.d("TvShowsRemoteMediator", "✅ Loaded ${tvShows.size} ${contentType.name} TV shows for page $page")

            MediatorResult.Success(
                endOfPaginationReached = tvShows.isEmpty(),
            )
        } catch (e: Exception) {
            Log.e("TvShowsRemoteMediator", "❌ Error loading ${contentType.name} TV shows", e)
            MediatorResult.Error(e)
        }
    }

    private suspend fun fetchTvShowsFromApi(page: Int): List<TvShowEntity> {
        return withContext(Dispatchers.IO) {
            when (contentType) {
                ContentType.TRENDING -> {
                    val trending = traktApi.getTrendingTvShows(page = page, limit = 20)
                    trending.mapIndexedNotNull { index, trendingShow ->
                        tvShowRepository.mapTraktShowToEntity(
                            trendingShow.show,
                            trendingOrder = ((page - 1) * 20) + index,
                        )
                    }
                }
                ContentType.POPULAR -> {
                    val popular = traktApi.getPopularTvShows(page = page, limit = 20)
                    popular.mapIndexedNotNull { index, show ->
                        tvShowRepository.mapTraktShowToEntity(
                            show,
                            popularOrder = ((page - 1) * 20) + index,
                        )
                    }
                }
            }
        }
    }
}
