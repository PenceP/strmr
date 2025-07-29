package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.model.Movie
import com.strmr.ai.domain.repository.MovieRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetTrendingMoviesUseCaseTest {

    private lateinit var movieRepository: MovieRepository
    private lateinit var getTrendingMoviesUseCase: GetTrendingMoviesUseCase

    @Before
    fun setup() {
        movieRepository = mock()
        getTrendingMoviesUseCase = GetTrendingMoviesUseCase(movieRepository)
    }

    @Test
    fun `getTrendingMoviesFlow returns flow from repository`() = runTest {
        // Given
        val movies = listOf<Movie>()
        val moviesFlow = flowOf(movies)
        whenever(movieRepository.getTrendingMovies()).thenReturn(moviesFlow)

        // When
        val result = getTrendingMoviesUseCase.getTrendingMoviesFlow()

        // Then
        assertEquals(moviesFlow, result)
    }

    @Test
    fun `refreshTrendingMovies returns success result`() = runTest {
        // Given
        whenever(movieRepository.refreshTrendingMovies()).thenReturn(Result.success(Unit))

        // When
        val result = getTrendingMoviesUseCase.refreshTrendingMovies()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `refreshTrendingMovies returns failure result`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        whenever(movieRepository.refreshTrendingMovies()).thenReturn(Result.failure(exception))

        // When
        val result = getTrendingMoviesUseCase.refreshTrendingMovies()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}