package com.strmr.ai.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.database.TraktRatingsEntity
import com.strmr.ai.ui.theme.StrmrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Base repository class that provides common functionality for media repositories
 * @param EntityType The database entity type (MovieEntity, TvShowEntity)
 * @param TraktType The Trakt API model type (Movie, Show)
 * @param TrendingType The Trakt trending wrapper type (TrendingMovie, TrendingShow)
 */
abstract class BaseMediaRepository<EntityType : Any, TraktType : Any, TrendingType : Any>(
    protected val traktApi: TraktApiService,
    protected val tmdbApi: TmdbApiService,
    protected val database: StrmrDatabase,
    protected val traktRatingsDao: TraktRatingsDao,
) {
    protected val detailsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    protected val ratingsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    protected var currentTrendingPage = 0
    protected var currentPopularPage = 0
    protected val pageSize = StrmrConstants.Paging.PAGE_SIZE

    /**
     * Generic content refresh method that handles pagination and caching
     */
    protected suspend fun refreshContent(
        contentType: ContentType,
        fetchTrendingFromTrakt: suspend (Int, Int) -> List<TrendingType>,
        fetchPopularFromTrakt: suspend (Int, Int) -> List<TraktType>,
        mapTrendingToEntity: suspend (TrendingType, Int) -> EntityType?,
        mapPopularToEntity: suspend (TraktType, Int) -> EntityType?,
    ) {
        withContext(Dispatchers.IO) {
            val limit = pageSize
            val page =
                when (contentType) {
                    ContentType.TRENDING -> {
                        currentTrendingPage++
                        currentTrendingPage
                    }
                    ContentType.POPULAR -> {
                        currentPopularPage++
                        currentPopularPage
                    }
                }

            val entities =
                when (contentType) {
                    ContentType.TRENDING -> {
                        val trending = fetchTrendingFromTrakt(page, limit)
                        trending.mapIndexedNotNull { index, item ->
                            val actualIndex = (page - 1) * pageSize + index
                            mapTrendingToEntity(item, actualIndex)?.let { entity ->
                                updateEntityTimestamp(entity)
                            }
                        }
                    }
                    ContentType.POPULAR -> {
                        val popular = fetchPopularFromTrakt(page, limit)
                        popular.mapIndexedNotNull { index, item ->
                            val actualIndex = (page - 1) * pageSize + index
                            mapPopularToEntity(item, actualIndex)?.let { entity ->
                                updateEntityTimestamp(entity)
                            }
                        }
                    }
                }

            Log.d(getLogTag(), "Fetched ${contentType.name.lowercase()} page $page, got ${entities.size} items")

            if (page == 1) {
                when (contentType) {
                    ContentType.TRENDING -> updateTrendingItems(entities)
                    ContentType.POPULAR -> updatePopularItems(entities)
                }
            } else {
                insertItems(entities)
            }
        }
    }

    /**
     * Creates a generic pager for content
     */
    @OptIn(ExperimentalPagingApi::class)
    protected fun createPager(
        contentType: ContentType,
        remoteMediator: Any, // Will be cast to RemoteMediator in concrete implementations
        pagingSourceFactory: () -> PagingSource<Int, EntityType>,
    ): Flow<PagingData<EntityType>> {
        @Suppress("UNCHECKED_CAST")
        return Pager<Int, EntityType>(
            config =
                PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 5,
                ),
            remoteMediator = remoteMediator as androidx.paging.RemoteMediator<Int, EntityType>?,
            pagingSourceFactory = pagingSourceFactory,
        ).flow
    }

    // Abstract methods for DAO operations that concrete repositories must implement
    abstract suspend fun insertItems(items: List<EntityType>)

    abstract suspend fun updateTrendingItems(items: List<EntityType>)

    abstract suspend fun updatePopularItems(items: List<EntityType>)

    abstract suspend fun getItemByTmdbId(tmdbId: Int): EntityType?

    abstract suspend fun updateItemLogo(
        tmdbId: Int,
        logoUrl: String?,
    )

    abstract suspend fun clearNullLogos()

    /**
     * Generic Trakt ratings handling
     */
    open suspend fun getTraktRatings(traktId: Int): TraktRatingsEntity? {
        val cached = traktRatingsDao.getRatings(traktId)
        val now = System.currentTimeMillis()
        if (cached != null && now - cached.updatedAt < ratingsExpiryMs) {
            return cached
        }
        return cached // Return stale cache if API fails, let concrete implementations handle API calls
    }

    /**
     * Save Trakt ratings to database
     */
    protected suspend fun saveTraktRatings(
        traktId: Int,
        rating: Float,
        votes: Int,
    ) {
        val entity =
            TraktRatingsEntity(
                traktId = traktId,
                rating = rating,
                votes = votes,
                updatedAt = System.currentTimeMillis(),
            )
        traktRatingsDao.insertOrUpdate(entity)
    }

    // Abstract methods that concrete repositories must implement
    abstract fun getLogTag(): String

    abstract suspend fun getTmdbId(item: TraktType): Int?

    abstract suspend fun getTraktId(item: TraktType): Int?

    abstract suspend fun getTmdbIdFromTrending(item: TrendingType): Int?

    abstract suspend fun getTraktIdFromTrending(item: TrendingType): Int?

    abstract suspend fun updateEntityTimestamp(entity: EntityType): EntityType
}

enum class ContentType {
    TRENDING,
    POPULAR,
}
