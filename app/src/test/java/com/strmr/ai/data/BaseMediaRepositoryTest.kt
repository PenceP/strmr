package com.strmr.ai.data

import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.database.TraktRatingsEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class BaseMediaRepositoryTest {
    private lateinit var traktApi: TraktApiService
    private lateinit var tmdbApi: TmdbApiService
    private lateinit var database: StrmrDatabase
    private lateinit var traktRatingsDao: TraktRatingsDao
    private lateinit var repository: TestBaseMediaRepository

    @Before
    fun setup() {
        traktApi = mock()
        tmdbApi = mock()
        database = mock()
        traktRatingsDao = mock()

        repository = TestBaseMediaRepository(traktApi, tmdbApi, database, traktRatingsDao)
    }

    @Test
    fun `getTraktRatings returns cached ratings when not expired`() =
        runTest {
            // Given
            val traktId = 123
            val cachedRating =
                TraktRatingsEntity(
                    traktId = traktId,
                    rating = 8.5f,
                    votes = 1000,
                    updatedAt = System.currentTimeMillis() - 1000, // 1 second ago
                )
            whenever(traktRatingsDao.getRatings(traktId)).thenReturn(cachedRating)

            // When
            val result = repository.getTraktRatings(traktId)

            // Then
            assertEquals(cachedRating, result)
        }

    @Test
    fun `getTraktRatings returns stale cache when expired`() =
        runTest {
            // Given
            val traktId = 123
            val expiredRating =
                TraktRatingsEntity(
                    traktId = traktId,
                    rating = 8.5f,
                    votes = 1000,
                    updatedAt = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L), // 8 days ago
                )
            whenever(traktRatingsDao.getRatings(traktId)).thenReturn(expiredRating)

            // When
            val result = repository.getTraktRatings(traktId)

            // Then
            assertEquals(expiredRating, result) // Should return stale cache
        }

    // Test implementation of BaseMediaRepository
    private class TestBaseMediaRepository(
        traktApi: TraktApiService,
        tmdbApi: TmdbApiService,
        database: StrmrDatabase,
        traktRatingsDao: TraktRatingsDao,
    ) : BaseMediaRepository<MovieEntity, Movie, TrendingMovie>(
            traktApi,
            tmdbApi,
            database,
            traktRatingsDao,
        ) {
        override fun getLogTag(): String = "TestRepository"

        override suspend fun getTmdbId(item: Movie): Int? = item.ids.tmdb

        override suspend fun getTraktId(item: Movie): Int? = item.ids.trakt

        override suspend fun getTmdbIdFromTrending(item: TrendingMovie): Int? = item.movie.ids.tmdb

        override suspend fun getTraktIdFromTrending(item: TrendingMovie): Int? = item.movie.ids.trakt

        override suspend fun updateEntityTimestamp(entity: MovieEntity): MovieEntity = entity.copy(lastUpdated = System.currentTimeMillis())

        // Implement abstract DAO methods
        override suspend fun insertItems(items: List<MovieEntity>) {}

        override suspend fun updateTrendingItems(items: List<MovieEntity>) {}

        override suspend fun updatePopularItems(items: List<MovieEntity>) {}

        override suspend fun getItemByTmdbId(tmdbId: Int): MovieEntity? = null

        override suspend fun updateItemLogo(
            tmdbId: Int,
            logoUrl: String?,
        ) {}

        override suspend fun clearNullLogos() {}
    }
}
