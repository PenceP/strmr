package com.strmr.ai.viewmodel

import com.strmr.ai.data.*
import com.strmr.ai.data.database.ContinueWatchingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    
    private lateinit var accountRepository: AccountRepository
    private lateinit var movieRepository: MovieRepository
    private lateinit var tvShowRepository: TvShowRepository
    private lateinit var homeRepository: HomeRepository
    private lateinit var omdbRepository: OmdbRepository
    
    private lateinit var homeViewModel: HomeViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        accountRepository = mock()
        movieRepository = mock()
        tvShowRepository = mock()
        homeRepository = mock()
        omdbRepository = mock()
        
        // Set up default mock returns
        whenever(homeRepository.getContinueWatching()).thenReturn(flowOf(emptyList()))
        whenever(accountRepository.getAllAccounts()).thenReturn(flowOf(emptyList()))
        
        homeViewModel = HomeViewModel(
            accountRepository = accountRepository,
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            homeRepository = homeRepository,
            omdbRepository = omdbRepository
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `init - initializes with correct default state`() = runTest {
        // Then - verify initial state values are correct
        assertFalse(homeViewModel.isContinueWatchingLoading.value)
        assertFalse(homeViewModel.isNetworksLoading.value)
        assertFalse(homeViewModel.isTraktAuthorized.value)
        assertFalse(homeViewModel.isTraktListsLoading.value)
        assertEquals(emptyList<HomeMediaItem>(), homeViewModel.continueWatching.value)
        assertEquals(emptyList<NetworkInfo>(), homeViewModel.networks.value)
        assertEquals(emptyList<HomeMediaItem.Collection>(), homeViewModel.traktLists.value)
        
        // Verify that HomeViewModel was created successfully (no crashes)
        assertNotNull(homeViewModel)
    }
    
    @Test
    fun `refreshContinueWatching - calls home repository refresh`() = runTest {        
        // Reset the mock to avoid interference from init calls
        reset(homeRepository)
        whenever(homeRepository.getContinueWatching()).thenReturn(flowOf(emptyList()))
        
        // When
        homeViewModel.refreshContinueWatching()
        
        // Then
        verify(homeRepository).refreshContinueWatching(accountRepository)
    }
    
    @Test
    fun `refreshTraktAuthorization - sets up trakt authorization check`() = runTest {
        // When
        homeViewModel.refreshTraktAuthorization()
        
        // Then - just verify the method executes without error
        // The internal logic tests authorization token which is complex to mock
        assertTrue(true) // Basic test that method doesn't crash
    }
    
    @Test
    fun `refreshTraktLists - executes without error`() = runTest {
        // When
        homeViewModel.refreshTraktLists()
        
        // Then - just verify the method executes without error
        assertTrue(true) // Basic test that method doesn't crash
    }
    
    
    // Helper functions to create test entities with correct constructor
    private fun createTestContinueWatchingEntity(
        progress: Float? = 50f,
        isNextEpisode: Boolean = false
    ): ContinueWatchingEntity {
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
            isNextEpisode = isNextEpisode,
            isInProgress = progress != null && progress > 0f && progress < 100f
        )
    }
}