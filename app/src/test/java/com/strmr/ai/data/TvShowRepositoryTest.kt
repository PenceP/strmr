package com.strmr.ai.data

import com.strmr.ai.data.database.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class TvShowRepositoryTest {
    
    private lateinit var tvShowDao: TvShowDao
    private lateinit var traktApi: TraktApiService
    private lateinit var tmdbApi: TmdbApiService
    private lateinit var seasonDao: SeasonDao
    private lateinit var episodeDao: EpisodeDao
    private lateinit var database: StrmrDatabase
    private lateinit var traktRatingsDao: TraktRatingsDao
    private lateinit var trailerService: TrailerService
    private lateinit var tmdbEnrichmentService: TmdbEnrichmentService
    
    private lateinit var tvShowRepository: TvShowRepository
    
    @Before
    fun setup() {
        tvShowDao = mock()
        traktApi = mock()
        tmdbApi = mock()
        seasonDao = mock()
        episodeDao = mock()
        database = mock()
        traktRatingsDao = mock()
        trailerService = mock()
        tmdbEnrichmentService = mock()
        
        tvShowRepository = TvShowRepository(
            tvShowDao = tvShowDao,
            traktApiService = traktApi,
            tmdbApi = tmdbApi,
            seasonDao = seasonDao,
            episodeDao = episodeDao,
            database = database,
            traktRatingsDao = traktRatingsDao,
            trailerService = trailerService,
            tmdbEnrichmentService = tmdbEnrichmentService
        )
    }
    
    @Test
    fun `getTrendingTvShows - returns flow of trending tv shows from dao`() = runTest {
        // Given
        val expectedShows = listOf(
            createTestTvShowEntity(1, "Show 1"),
            createTestTvShowEntity(2, "Show 2")
        )
        whenever(tvShowDao.getTrendingTvShows()).thenReturn(flowOf(expectedShows))
        
        // When
        val result = tvShowRepository.getTrendingTvShows().first()
        
        // Then
        assertEquals(expectedShows, result)
        verify(tvShowDao).getTrendingTvShows()
    }
    
    @Test
    fun `getPopularTvShows - returns flow of popular tv shows from dao`() = runTest {
        // Given
        val expectedShows = listOf(
            createTestTvShowEntity(3, "Popular Show 1"),
            createTestTvShowEntity(4, "Popular Show 2")
        )
        whenever(tvShowDao.getPopularTvShows()).thenReturn(flowOf(expectedShows))
        
        // When
        val result = tvShowRepository.getPopularTvShows().first()
        
        // Then
        assertEquals(expectedShows, result)
        verify(tvShowDao).getPopularTvShows()
    }
    
    @Test
    fun `getOrFetchTvShow - returns cached tv show when available`() = runTest {
        // Given
        val tmdbId = 123
        val cachedShow = createTestTvShowEntity(tmdbId, "Cached Show")
        whenever(tvShowDao.getTvShowByTmdbId(tmdbId)).thenReturn(cachedShow)
        
        // When
        val result = tvShowRepository.getOrFetchTvShow(tmdbId)
        
        // Then
        assertEquals(cachedShow, result)
        verify(tvShowDao).getTvShowByTmdbId(tmdbId)
    }
    
    // Helper function to create test TvShowEntity
    private fun createTestTvShowEntity(
        tmdbId: Int,
        title: String,
        logoUrl: String? = null
    ): TvShowEntity {
        return TvShowEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster_$tmdbId.jpg",
            backdropUrl = "backdrop_$tmdbId.jpg",
            overview = "Test overview for $title",
            rating = 8.5f,
            logoUrl = logoUrl,
            traktRating = null,
            traktVotes = null,
            year = 2024,
            firstAirDate = "2024-01-01",
            lastAirDate = "2024-12-31",
            runtime = 45,
            genres = listOf("Drama", "Thriller"),
            cast = emptyList(),
            similar = emptyList(),
            trendingOrder = null,
            popularOrder = null,
            topRatedOrder = null,
            airingTodayOrder = null,
            onTheAirOrder = null,
            dataSourceOrders = emptyMap(),
            lastUpdated = System.currentTimeMillis()
        )
    }
}