package com.strmr.ai.integration

import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import com.strmr.ai.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * Integration tests for navigation flows between screens.
 * Tests the interaction between ViewModels and repositories during navigation.
 */
@ExperimentalCoroutinesApi
class NavigationIntegrationTest {
    
    // ViewModels
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var detailsViewModel: DetailsViewModel
    
    // Repositories (mocked)
    private lateinit var accountRepository: AccountRepository
    private lateinit var movieRepository: MovieRepository
    private lateinit var tvShowRepository: TvShowRepository
    private lateinit var homeRepository: HomeRepository
    private lateinit var omdbRepository: OmdbRepository
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock repositories
        accountRepository = mock()
        movieRepository = mock()
        tvShowRepository = mock()
        homeRepository = mock()
        omdbRepository = mock()
        
        // Set up default mock returns
        whenever(homeRepository.getContinueWatching()).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.getAllAccounts()).thenReturn(flowOf(emptyList()))
        
        // Create ViewModels
        homeViewModel = HomeViewModel(
            accountRepository = accountRepository,
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            homeRepository = homeRepository,
            omdbRepository = omdbRepository
        )
        
        detailsViewModel = DetailsViewModel(
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            omdbRepository = omdbRepository
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `navigation flow - home to movie details loads correctly`() = runTest {
        // Given - movie exists in repository
        val movieTmdbId = 123
        val movieEntity = createTestMovieEntity(movieTmdbId, "Test Movie")
        whenever(movieRepository.getMovieByTmdbId(movieTmdbId)).thenReturn(movieEntity)
        
        // When - navigate from home to movie details
        detailsViewModel.loadMovie(movieTmdbId)
        
        // Then - details are loaded correctly
        assertEquals(movieEntity, detailsViewModel.movie.value)
        assertNull(detailsViewModel.tvShow.value) // TV show should remain null
        
        // Verify correct repository calls
        verify(movieRepository).getMovieByTmdbId(movieTmdbId)
        verify(movieRepository, never()).getOrFetchMovie(any()) // Should use cached version
    }
    
    @Test
    fun `navigation flow - home to tv show details loads correctly`() = runTest {
        // Given - TV show exists in repository
        val showTmdbId = 456
        val tvShowEntity = createTestTvShowEntity(showTmdbId, "Test Show")
        whenever(tvShowRepository.getTvShowByTmdbId(showTmdbId)).thenReturn(tvShowEntity)
        
        // When - navigate from home to TV show details
        detailsViewModel.loadTvShow(showTmdbId)
        
        // Then - details are loaded correctly
        assertEquals(tvShowEntity, detailsViewModel.tvShow.value)
        assertNull(detailsViewModel.movie.value) // Movie should remain null
        
        // Verify correct repository calls
        verify(tvShowRepository).getTvShowByTmdbId(showTmdbId)
        verify(tvShowRepository, never()).getOrFetchTvShow(any()) // Should use cached version
    }
    
    @Test
    fun `navigation flow - details to similar content navigation`() = runTest {
        // Given - movie is loaded and has similar content
        val originalMovieId = 123
        val similarMovieId = 789
        val originalMovie = createTestMovieEntity(originalMovieId, "Original Movie")
        val similarMovie = createTestMovieEntity(similarMovieId, "Similar Movie")
        val similarContent = listOf(
            SimilarContent(
                tmdbId = similarMovieId,
                title = "Similar Movie",
                posterUrl = "similar_poster.jpg",
                backdropUrl = "similar_backdrop.jpg", 
                rating = 7.5f,
                year = 2023,
                mediaType = "movie"
            )
        )
        
        whenever(movieRepository.getMovieByTmdbId(originalMovieId)).thenReturn(originalMovie)
        whenever(movieRepository.getOrFetchSimilarMovies(originalMovieId)).thenReturn(similarContent)
        whenever(movieRepository.getMovieByTmdbId(similarMovieId)).thenReturn(similarMovie)
        
        // When - load original movie, get similar content, then navigate to similar movie
        detailsViewModel.loadMovie(originalMovieId)
        val similar = detailsViewModel.getSimilarMovies(originalMovieId)
        detailsViewModel.loadMovie(similarMovieId)
        
        // Then - navigation completed successfully
        assertEquals(similarMovie, detailsViewModel.movie.value) // Last loaded movie (similar movie)
        assertEquals(similarContent, similar) // Similar content fetched
        
        // Verify the navigation flow
        verify(movieRepository).getMovieByTmdbId(originalMovieId)
        verify(movieRepository).getOrFetchSimilarMovies(originalMovieId)
        verify(movieRepository).getMovieByTmdbId(similarMovieId)
    }
    
    @Test
    fun `navigation flow - handles missing content gracefully`() = runTest {
        // Given - content doesn't exist in repository
        val nonExistentId = 999
        whenever(movieRepository.getMovieByTmdbId(nonExistentId)).thenReturn(null)
        whenever(movieRepository.getOrFetchMovie(nonExistentId)).thenReturn(null)
        
        // When - attempt to navigate to non-existent content
        detailsViewModel.loadMovie(nonExistentId)
        
        // Then - should handle gracefully without crashing
        assertNull(detailsViewModel.movie.value)
        
        // Verify attempted to fetch from both cache and API
        verify(movieRepository).getMovieByTmdbId(nonExistentId)
        verify(movieRepository).getOrFetchMovie(nonExistentId)
    }
    
    @Test
    fun `navigation flow - continue watching to details maintains state`() = runTest {
        // Given - continue watching item exists
        val continueWatchingEntities = listOf(
            createTestContinueWatchingEntity(progress = 45f)
        )
        val movieEntity = createTestMovieEntity(123, "Continue Watching Movie")
        
        whenever(homeRepository.getContinueWatching()).thenReturn(flowOf(continueWatchingEntities))
        whenever(movieRepository.getOrFetchMovieWithLogo(123)).thenReturn(movieEntity)
        whenever(movieRepository.getMovieByTmdbId(123)).thenReturn(movieEntity)
        
        // Create new home viewmodel to trigger continue watching loading
        val homeVM = HomeViewModel(
            accountRepository = accountRepository,
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            homeRepository = homeRepository,
            omdbRepository = omdbRepository
        )
        
        // When - navigate from continue watching to details
        detailsViewModel.loadMovie(123)
        
        // Then - both ViewModels have consistent state
        assertEquals(movieEntity, detailsViewModel.movie.value)
        
        // Verify data consistency
        verify(movieRepository, atLeastOnce()).getOrFetchMovieWithLogo(123) // Home view loading
        verify(movieRepository).getMovieByTmdbId(123) // Details view loading
    }
    
    @Test
    fun `navigation performance - multiple rapid navigation calls handled correctly`() = runTest {
        // Given - multiple movies in repository
        val movie1 = createTestMovieEntity(1, "Movie 1")
        val movie2 = createTestMovieEntity(2, "Movie 2")
        val movie3 = createTestMovieEntity(3, "Movie 3")
        
        whenever(movieRepository.getMovieByTmdbId(1)).thenReturn(movie1)
        whenever(movieRepository.getMovieByTmdbId(2)).thenReturn(movie2)
        whenever(movieRepository.getMovieByTmdbId(3)).thenReturn(movie3)
        
        // When - rapid navigation between movies
        detailsViewModel.loadMovie(1)
        detailsViewModel.loadMovie(2)
        detailsViewModel.loadMovie(3)
        
        // Then - final state should be the last movie
        assertEquals(movie3, detailsViewModel.movie.value)
        
        // Verify all calls were made (no throttling in ViewModels)
        verify(movieRepository).getMovieByTmdbId(1)
        verify(movieRepository).getMovieByTmdbId(2)
        verify(movieRepository).getMovieByTmdbId(3)
    }
    
    // Helper functions
    private fun createTestMovieEntity(tmdbId: Int = 123, title: String = "Test Movie"): MovieEntity {
        return MovieEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster.jpg",
            backdropUrl = "backdrop.jpg",
            overview = "Test overview",
            rating = 8.5f,
            logoUrl = "logo.jpg",
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
    
    private fun createTestTvShowEntity(tmdbId: Int = 456, title: String = "Test Show"): TvShowEntity {
        return TvShowEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster.jpg",
            backdropUrl = "backdrop.jpg",
            overview = "Test overview",
            rating = 8.5f,
            logoUrl = "logo.jpg",
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
    
    private fun createTestContinueWatchingEntity(progress: Float = 50f): ContinueWatchingEntity {
        return ContinueWatchingEntity(
            id = "test_$progress",
            type = "movie",
            lastWatchedAt = "2024-01-01T12:00:00Z",
            progress = progress,
            movieTitle = "Test Movie",
            movieTmdbId = 123,
            movieTraktId = 456,
            movieYear = 2024,
            showTitle = null,
            showTmdbId = null,
            showTraktId = null,
            showYear = null,
            episodeTitle = null,
            episodeSeason = null,
            episodeNumber = null,
            episodeTmdbId = null,
            episodeTraktId = null,
            isNextEpisode = false,
            isInProgress = progress > 0f && progress < 100f
        )
    }
}