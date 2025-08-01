package com.strmr.ai.data.mapper

import com.strmr.ai.data.Actor
import com.strmr.ai.data.BelongsToCollection
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.domain.model.MovieId
import com.strmr.ai.domain.model.TmdbId
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MovieMapperTest {
    private lateinit var movieMapper: MovieMapper

    @Before
    fun setup() {
        movieMapper = MovieMapper()
    }

    @Test
    fun `mapToDomain maps MovieEntity to Movie correctly`() {
        // Given
        val movieEntity =
            MovieEntity(
                tmdbId = 123,
                imdbId = "tt1234567",
                title = "Test Movie",
                posterUrl = "https://image.tmdb.org/poster.jpg",
                backdropUrl = "https://image.tmdb.org/backdrop.jpg",
                overview = "Test overview",
                rating = 8.5f,
                logoUrl = "https://image.tmdb.org/logo.jpg",
                traktRating = null,
                traktVotes = null,
                year = 2023,
                releaseDate = "2023-01-01",
                runtime = 120,
                genres = listOf("Action", "Drama"),
                cast =
                    listOf(
                        Actor(
                            id = 1,
                            name = "Test Actor",
                            character = "Test Character",
                            profilePath = "/actor_profile.jpg",
                        ),
                    ),
                similar =
                    listOf(
                        SimilarContent(
                            tmdbId = 456,
                            title = "Similar Movie",
                            posterUrl = "/similar_poster.jpg",
                            backdropUrl = "/similar_backdrop.jpg",
                            rating = 7.5f,
                            year = 2023,
                            mediaType = "movie",
                        ),
                    ),
                belongsToCollection =
                    BelongsToCollection(
                        id = 1,
                        name = "Test Collection",
                        poster_path = "/collection_poster.jpg",
                        backdrop_path = "/collection_backdrop.jpg",
                    ),
                lastUpdated = System.currentTimeMillis(),
            )

        // When
        val domainMovie = movieMapper.mapToDomain(movieEntity)

        // Then
        assertNotNull(domainMovie)
        assertEquals(MovieId(123), domainMovie.id)
        assertEquals(TmdbId(123), domainMovie.tmdbId)
        assertEquals("Test Movie", domainMovie.title)
        assertEquals("Test overview", domainMovie.overview)
        assertEquals(8.5f, domainMovie.rating.primaryRating ?: 0f, 0.01f)
        assertEquals(2023, domainMovie.year)
        assertEquals("https://image.tmdb.org/poster.jpg", domainMovie.images.posterUrl)
        assertEquals("https://image.tmdb.org/backdrop.jpg", domainMovie.images.backdropUrl)
        assertEquals("https://image.tmdb.org/logo.jpg", domainMovie.images.logoUrl)
        assertEquals(2, domainMovie.genres.size)
        assertEquals("Action", domainMovie.genres[0].name)
        assertEquals("Drama", domainMovie.genres[1].name)
        assertEquals(1, domainMovie.cast.size)
        assertEquals("Test Actor", domainMovie.cast[0].person.name)
        assertEquals("Test Character", domainMovie.cast[0].character)
        assertEquals(1, domainMovie.similarMovies.size)
        assertEquals("Similar Movie", domainMovie.similarMovies[0].title)
        assertNotNull(domainMovie.collection)
        assertEquals("Test Collection", domainMovie.collection?.name)
    }

    @Test
    fun `mapToDomain handles null values correctly`() {
        // Given
        val movieEntity =
            MovieEntity(
                tmdbId = 123,
                imdbId = null,
                title = "Test Movie",
                posterUrl = null,
                backdropUrl = null,
                overview = null,
                rating = null,
                logoUrl = null,
                traktRating = null,
                traktVotes = null,
                year = null,
                releaseDate = null,
                runtime = null,
                genres = emptyList(),
                cast = emptyList(),
                similar = emptyList(),
                belongsToCollection = null,
                lastUpdated = System.currentTimeMillis(),
            )

        // When
        val domainMovie = movieMapper.mapToDomain(movieEntity)

        // Then
        assertNotNull(domainMovie)
        assertEquals("Test Movie", domainMovie.title)
        assertNull(domainMovie.imdbId)
        assertNull(domainMovie.overview)
        assertNull(domainMovie.images.posterUrl)
        assertNull(domainMovie.images.backdropUrl)
        assertNull(domainMovie.images.logoUrl)
        assertNull(domainMovie.year)
        assertNull(domainMovie.runtime)
        assertTrue(domainMovie.genres.isEmpty())
        assertTrue(domainMovie.cast.isEmpty())
        assertTrue(domainMovie.similarMovies.isEmpty())
        assertNull(domainMovie.collection)
    }

    @Test
    fun `mapToDomain with empty collections returns empty lists`() {
        // Given
        val movieEntity =
            MovieEntity(
                tmdbId = 123,
                imdbId = "tt1234567",
                title = "Test Movie",
                posterUrl = "https://example.com/poster.jpg",
                backdropUrl = "https://example.com/backdrop.jpg",
                overview = "Test overview",
                rating = 8.0f,
                logoUrl = null,
                traktRating = null,
                traktVotes = null,
                year = 2023,
                releaseDate = "2023-01-01",
                runtime = 120,
                genres = emptyList(),
                cast = emptyList(),
                similar = emptyList(),
                belongsToCollection = null,
                lastUpdated = System.currentTimeMillis(),
            )

        // When
        val domainMovie = movieMapper.mapToDomain(movieEntity)

        // Then
        assertNotNull(domainMovie)
        assertTrue(domainMovie.genres.isEmpty())
        assertTrue(domainMovie.cast.isEmpty())
        assertTrue(domainMovie.similarMovies.isEmpty())
    }

    @Test
    fun `mapToDomain correctly maps runtime`() {
        // Given
        val movieEntity =
            MovieEntity(
                tmdbId = 123,
                imdbId = "tt1234567",
                title = "Test Movie",
                posterUrl = "https://example.com/poster.jpg",
                backdropUrl = "https://example.com/backdrop.jpg",
                overview = "Test overview",
                rating = 8.0f,
                logoUrl = null,
                traktRating = null,
                traktVotes = null,
                year = 2023,
                releaseDate = "2023-01-01",
                runtime = 150,
                genres = emptyList(),
                cast = emptyList(),
                similar = emptyList(),
                belongsToCollection = null,
                lastUpdated = System.currentTimeMillis(),
            )

        // When
        val domainMovie = movieMapper.mapToDomain(movieEntity)

        // Then
        assertNotNull(domainMovie.runtime)
        assertEquals(150, domainMovie.runtime?.minutes)
    }
}
