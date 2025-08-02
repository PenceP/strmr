package com.strmr.ai.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.domain.repository.ContinueWatchingItem
import com.strmr.ai.domain.usecase.CheckTraktAuthorizationUseCase
import com.strmr.ai.domain.usecase.ConvertContinueWatchingToHomeMediaItemsUseCase
import com.strmr.ai.domain.usecase.FetchMediaLogoUseCase
import com.strmr.ai.domain.usecase.GetContinueWatchingUseCase
import com.strmr.ai.presentation.state.ContinueWatchingState
import com.strmr.ai.presentation.state.TraktAuthState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var accountRepository: AccountRepository
    private lateinit var movieRepository: MovieRepository
    private lateinit var tvShowRepository: TvShowRepository
    private lateinit var homeRepository: HomeRepository
    private lateinit var omdbRepository: OmdbRepository
    private lateinit var getContinueWatchingUseCase: GetContinueWatchingUseCase
    private lateinit var checkTraktAuthorizationUseCase: CheckTraktAuthorizationUseCase
    private lateinit var fetchMediaLogoUseCase: FetchMediaLogoUseCase
    private lateinit var convertContinueWatchingUseCase: ConvertContinueWatchingToHomeMediaItemsUseCase

    private lateinit var homeViewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        accountRepository = mock()
        movieRepository = mock()
        tvShowRepository = mock()
        homeRepository = mock()
        omdbRepository = mock()
        getContinueWatchingUseCase = mock()
        checkTraktAuthorizationUseCase = mock()
        fetchMediaLogoUseCase = mock()
        convertContinueWatchingUseCase = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            accountRepository = accountRepository,
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            homeRepository = homeRepository,
            omdbRepository = omdbRepository,
            getContinueWatchingUseCase = getContinueWatchingUseCase,
            checkTraktAuthorizationUseCase = checkTraktAuthorizationUseCase,
            fetchMediaLogoUseCase = fetchMediaLogoUseCase,
            convertContinueWatchingToHomeMediaItemsUseCase = convertContinueWatchingUseCase,
        )
    }

    @Test
    fun `initial state has loading continue watching`() =
        runTest {
            // Given
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.success(emptyList()))
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(false))

            // When
            homeViewModel = createViewModel()
            advanceUntilIdle()

            // Then
            homeViewModel.uiState.test {
                val initialState = awaitItem()
                assertTrue(initialState.continueWatching is ContinueWatchingState.Loading)
            }
        }

    @Test
    fun `checkTraktAuthorization updates state to authorized when use case returns true`() =
        runTest {
            // Given
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.success(emptyList()))
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(true))
            whenever(convertContinueWatchingUseCase(emptyList())).thenReturn(emptyList())

            homeViewModel = createViewModel()
            advanceUntilIdle()

            // When called (happens during init)

            // Then
            homeViewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.traktAuthorization is TraktAuthState.Authorized)
            }
        }

    @Test
    fun `checkTraktAuthorization updates state to not authorized when use case returns false`() =
        runTest {
            // Given
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.success(emptyList()))
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(false))

            homeViewModel = createViewModel()
            advanceUntilIdle()

            // Then
            homeViewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.traktAuthorization is TraktAuthState.NotAuthorized)
            }
        }

    @Test
    fun `continue watching loading succeeds with data`() =
        runTest {
            // Given
            val continueWatchingItems = listOf<ContinueWatchingItem>()
            val homeMediaItems = listOf<HomeMediaItem>()
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.success(continueWatchingItems))
            whenever(convertContinueWatchingUseCase(continueWatchingItems)).thenReturn(homeMediaItems)
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(false))

            // When
            homeViewModel = createViewModel()
            advanceUntilIdle()

            // Then
            homeViewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.continueWatching is ContinueWatchingState.Success)
            }
        }

    @Test
    fun `continue watching loading fails with error`() =
        runTest {
            // Given
            val exception = RuntimeException("Use case error")
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.failure(exception))
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(false))

            // When
            homeViewModel = createViewModel()
            advanceUntilIdle()

            // Then
            homeViewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.continueWatching is ContinueWatchingState.Error)
                assertEquals("Use case error", (state.continueWatching as ContinueWatchingState.Error).message)
            }
        }

    @Test
    fun `legacy flows are properly initialized`() =
        runTest {
            // Given
            whenever(getContinueWatchingUseCase.refreshContinueWatching()).thenReturn(Result.success(emptyList()))
            whenever(checkTraktAuthorizationUseCase()).thenReturn(Result.success(false))

            // When
            homeViewModel = createViewModel()
            advanceUntilIdle()

            // Then
            homeViewModel.continueWatching.test {
                val items = awaitItem()
                assertTrue(items.isEmpty())
            }

            homeViewModel.isTraktAuthorized.test {
                val isAuthorized = awaitItem()
                assertFalse(isAuthorized)
            }
        }
}
