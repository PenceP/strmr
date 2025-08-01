package com.strmr.ai.data.repository

import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.mapper.MovieMapper
import com.strmr.ai.domain.model.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.strmr.ai.data.MovieRepository as LegacyMovieRepository

class MovieRepositoryImplTest {
    private lateinit var legacyRepository: LegacyMovieRepository
    private lateinit var movieMapper: MovieMapper
    private lateinit var movieRepositoryImpl: MovieRepositoryImpl

    @Before
    fun setup() {
        legacyRepository = mock()
        movieMapper = mock()
        movieRepositoryImpl = MovieRepositoryImpl(legacyRepository, movieMapper)
    }

    @Test
    fun `getMovie returns mapped movie when entity exists`() =
        runTest {
            // Given
            val movieId = MovieId(123)
            val movieEntity = createMockMovieEntity(123, "Test Movie")
            val domainMovie = createMockDomainMovie(123, "Test Movie")

            whenever(legacyRepository.getMovieByTmdbId(123)).thenReturn(movieEntity)
            whenever(movieMapper.mapToDomain(movieEntity)).thenReturn(domainMovie)

            // When
            val result = movieRepositoryImpl.getMovie(movieId)

            // Then
            assertNotNull(result)
            assertEquals("Test Movie", result?.title)
        }

    @Test
    fun `getMovie returns null when entity does not exist`() =
        runTest {
            // Given
            val movieId = MovieId(999)
            whenever(legacyRepository.getMovieByTmdbId(999)).thenReturn(null)

            // When
            val result = movieRepositoryImpl.getMovie(movieId)

            // Then
            assertNull(result)
        }

    @Test
    fun `getTrendingMovies returns mapped flow`() =
        runTest {
            // Given
            val entities = listOf(createMockMovieEntity(1, "Movie 1"))
            val domainMovies = listOf(createMockDomainMovie(1, "Movie 1"))
            val entitiesFlow = flowOf(entities)

            whenever(legacyRepository.getTrendingMovies()).thenReturn(entitiesFlow)
            whenever(movieMapper.mapToDomain(entities[0])).thenReturn(domainMovies[0])

            // When
            val result = movieRepositoryImpl.getTrendingMovies()

            // Then
            assertNotNull(result)
        }

    @Test
    fun `getMovieByTmdbId returns mapped movie when entity exists`() =
        runTest {
            // Given
            val tmdbId = TmdbId(456)
            val movieEntity = createMockMovieEntity(456, "TMDB Movie")
            val domainMovie = createMockDomainMovie(456, "TMDB Movie")

            whenever(legacyRepository.getMovieByTmdbId(456)).thenReturn(movieEntity)
            whenever(movieMapper.mapToDomain(movieEntity)).thenReturn(domainMovie)

            // When
            val result = movieRepositoryImpl.getMovieByTmdbId(tmdbId)

            // Then
            assertNotNull(result)
            assertEquals("TMDB Movie", result?.title)
        }

    private fun createMockMovieEntity(
        id: Int,
        title: String,
    ): MovieEntity {
        return MovieEntity(
            tmdbId = id,
            imdbId = "tt$id",
            title = title,
            posterUrl = "https://example.com/poster$id.jpg",
            backdropUrl = "https://example.com/backdrop$id.jpg",
            overview = "Test overview",
            rating = 8.0f,
            logoUrl = null,
            traktRating = null,
            traktVotes = null,
            year = 2023,
            releaseDate = "2023-01-01",
            runtime = 120,
            genres = listOf("Action"),
            cast = emptyList(),
            similar = emptyList(),
            belongsToCollection = null,
            lastUpdated = System.currentTimeMillis(),
        )
    }

    private fun createMockDomainMovie(
        id: Int,
        title: String,
    ): Movie {
        return Movie(
            id = MovieId(id),
            tmdbId = TmdbId(id),
            imdbId = ImdbId("tt$id"),
            title = title,
            overview = "Test overview",
            year = 2023,
            releaseDate = null,
            runtime = Runtime(120),
            rating = Rating(tmdbRating = 8.0f),
            genres = listOf(Genre("Action")),
            images =
                MediaImages(
                    posterUrl = "https://example.com/poster$id.jpg",
                    backdropUrl = "https://example.com/backdrop$id.jpg",
                    logoUrl = null,
                ),
            cast = emptyList(),
            collection = null,
            similarMovies = emptyList(),
            lastUpdated = System.currentTimeMillis(),
        )
    }
}
