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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
class MovieRepositoryTest {
    
    private lateinit var movieDao: MovieDao
    private lateinit var collectionDao: CollectionDao
    private lateinit var traktApi: TraktApiService
    private lateinit var tmdbApi: TmdbApiService
    private lateinit var database: StrmrDatabase
    private lateinit var traktRatingsDao: TraktRatingsDao
    private lateinit var trailerService: TrailerService
    private lateinit var tmdbEnrichmentService: TmdbEnrichmentService
    
    private lateinit var movieRepository: MovieRepository
    
    @Before
    fun setup() {
        movieDao = mock()
        collectionDao = mock()
        traktApi = mock()
        tmdbApi = mock()
        database = mock()
        traktRatingsDao = mock()
        trailerService = mock()
        tmdbEnrichmentService = mock()
        
        movieRepository = MovieRepository(
            movieDao = movieDao,
            collectionDao = collectionDao,
            traktApi = traktApi,
            tmdbApi = tmdbApi,
            database = database,
            traktRatingsDao = traktRatingsDao,
            trailerService = trailerService,
            tmdbEnrichmentService = tmdbEnrichmentService
        )
    }
    
    @Test
    fun `getTrendingMovies - returns flow of trending movies from dao`() = runTest {
        // Given
        val expectedMovies = listOf(
            createTestMovieEntity(1, "Movie 1"),
            createTestMovieEntity(2, "Movie 2")
        )
        whenever(movieDao.getTrendingMovies()).thenReturn(flowOf(expectedMovies))
        
        // When
        val result = movieRepository.getTrendingMovies().first()
        
        // Then
        assertEquals(expectedMovies, result)
        verify(movieDao).getTrendingMovies()
    }
    
    @Test
    fun `getPopularMovies - returns flow of popular movies from dao`() = runTest {
        // Given
        val expectedMovies = listOf(
            createTestMovieEntity(3, "Popular Movie 1"),
            createTestMovieEntity(4, "Popular Movie 2")
        )
        whenever(movieDao.getPopularMovies()).thenReturn(flowOf(expectedMovies))
        
        // When
        val result = movieRepository.getPopularMovies().first()
        
        // Then
        assertEquals(expectedMovies, result)
        verify(movieDao).getPopularMovies()
    }
    
    @Test
    fun `getOrFetchMovieWithLogo - returns cached movie with logo when available`() = runTest {
        // Given
        val tmdbId = 123
        val cachedMovie = createTestMovieEntity(tmdbId, "Cached Movie", logoUrl = "logo.jpg")
        whenever(movieDao.getMovieByTmdbId(tmdbId)).thenReturn(cachedMovie)
        
        // When
        val result = movieRepository.getOrFetchMovieWithLogo(tmdbId)
        
        // Then
        assertEquals(cachedMovie, result)
        verify(movieDao).getMovieByTmdbId(tmdbId)
    }
    
    // Helper function to create test MovieEntity
    private fun createTestMovieEntity(
        tmdbId: Int,
        title: String,
        logoUrl: String? = null
    ): MovieEntity {
        return MovieEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster_$tmdbId.jpg",
            backdropUrl = "backdrop_$tmdbId.jpg",
            overview = "Test overview for $title",
            rating = 7.5f,
            logoUrl = logoUrl,
            traktRating = null,
            traktVotes = null,
            year = 2024,
            releaseDate = "2024-01-01",
            runtime = 120,
            genres = listOf("Action", "Adventure"),
            cast = emptyList(),
            similar = emptyList(),
            belongsToCollection = null,
            trendingOrder = null,
            popularOrder = null,
            nowPlayingOrder = null,
            upcomingOrder = null,
            topRatedOrder = null,
            topMoviesWeekOrder = null,
            dataSourceOrders = emptyMap(),
            lastUpdated = System.currentTimeMillis()
        )
    }
}