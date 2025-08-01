package com.strmr.ai.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strmr.ai.data.Actor
import com.strmr.ai.data.BelongsToCollection
import com.strmr.ai.data.SimilarContent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovieDaoTest {
    private lateinit var database: StrmrDatabase
    private lateinit var movieDao: MovieDao

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                StrmrDatabase::class.java,
            ).allowMainThreadQueries().build()

        movieDao = database.movieDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetMovie() =
        runTest {
            // Given
            val movie = createTestMovieEntity(1, "Test Movie")

            // When
            movieDao.insertMovie(movie)
            val retrievedMovie = movieDao.getMovieById(1)

            // Then
            assertNotNull(retrievedMovie)
            assertEquals("Test Movie", retrievedMovie?.title)
            assertEquals(1, retrievedMovie?.tmdbId)
        }

    @Test
    fun insertMultipleMoviesAndGetAll() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "Movie 1"),
                    createTestMovieEntity(2, "Movie 2"),
                    createTestMovieEntity(3, "Movie 3"),
                )

            // When
            movieDao.insertMovies(movies)
            val allMovies = movieDao.getAllMovies()

            // Then
            assertEquals(3, allMovies.size)
            assertTrue(allMovies.any { it.title == "Movie 1" })
            assertTrue(allMovies.any { it.title == "Movie 2" })
            assertTrue(allMovies.any { it.title == "Movie 3" })
        }

    @Test
    fun getTrendingMoviesReturnsCorrectOrder() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "Movie 1").copy(trendingOrder = 3),
                    createTestMovieEntity(2, "Movie 2").copy(trendingOrder = 1),
                    createTestMovieEntity(3, "Movie 3").copy(trendingOrder = 2),
                )

            // When
            movieDao.insertMovies(movies)
            val trendingMovies = movieDao.getTrendingMovies()

            // Then
            assertEquals(3, trendingMovies.size)
            assertEquals("Movie 2", trendingMovies[0].title) // trendingOrder = 1
            assertEquals("Movie 3", trendingMovies[1].title) // trendingOrder = 2
            assertEquals("Movie 1", trendingMovies[2].title) // trendingOrder = 3
        }

    @Test
    fun getPopularMoviesReturnsCorrectOrder() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "Movie 1").copy(popularityOrder = 2),
                    createTestMovieEntity(2, "Movie 2").copy(popularityOrder = 1),
                    createTestMovieEntity(3, "Movie 3").copy(popularityOrder = 3),
                )

            // When
            movieDao.insertMovies(movies)
            val popularMovies = movieDao.getPopularMovies()

            // Then
            assertEquals(3, popularMovies.size)
            assertEquals("Movie 2", popularMovies[0].title) // popularityOrder = 1
            assertEquals("Movie 1", popularMovies[1].title) // popularityOrder = 2
            assertEquals("Movie 3", popularMovies[2].title) // popularityOrder = 3
        }

    @Test
    fun updateMovieUpdatesExistingRecord() =
        runTest {
            // Given
            val originalMovie = createTestMovieEntity(1, "Original Title")
            movieDao.insertMovie(originalMovie)

            // When
            val updatedMovie = originalMovie.copy(title = "Updated Title", voteAverage = 9.0f)
            movieDao.updateMovie(updatedMovie)
            val retrievedMovie = movieDao.getMovieById(1)

            // Then
            assertNotNull(retrievedMovie)
            assertEquals("Updated Title", retrievedMovie?.title)
            assertEquals(9.0f, retrievedMovie?.voteAverage ?: 0f, 0.01f)
        }

    @Test
    fun deleteMovieRemovesRecord() =
        runTest {
            // Given
            val movie = createTestMovieEntity(1, "Test Movie")
            movieDao.insertMovie(movie)

            // When
            movieDao.deleteMovie(movie)
            val retrievedMovie = movieDao.getMovieById(1)

            // Then
            assertNull(retrievedMovie)
        }

    @Test
    fun searchMoviesFindsMatches() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "The Dark Knight"),
                    createTestMovieEntity(2, "Dark Phoenix"),
                    createTestMovieEntity(3, "Bright Light"),
                )
            movieDao.insertMovies(movies)

            // When
            val searchResults = movieDao.searchMovies("Dark")

            // Then
            assertEquals(2, searchResults.size)
            assertTrue(searchResults.any { it.title == "The Dark Knight" })
            assertTrue(searchResults.any { it.title == "Dark Phoenix" })
            assertFalse(searchResults.any { it.title == "Bright Light" })
        }

    @Test
    fun getMoviesByGenreReturnsCorrectMovies() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "Action Movie").copy(genres = listOf("Action", "Thriller")),
                    createTestMovieEntity(2, "Comedy Movie").copy(genres = listOf("Comedy")),
                    createTestMovieEntity(3, "Action Comedy").copy(genres = listOf("Action", "Comedy")),
                )
            movieDao.insertMovies(movies)

            // When
            val actionMovies = movieDao.getMoviesByGenre("Action")

            // Then
            assertEquals(2, actionMovies.size)
            assertTrue(actionMovies.any { it.title == "Action Movie" })
            assertTrue(actionMovies.any { it.title == "Action Comedy" })
            assertFalse(actionMovies.any { it.title == "Comedy Movie" })
        }

    @Test
    fun getMoviesByYearRangeReturnsCorrectMovies() =
        runTest {
            // Given
            val movies =
                listOf(
                    createTestMovieEntity(1, "Old Movie").copy(year = 1999),
                    createTestMovieEntity(2, "2000s Movie").copy(year = 2005),
                    createTestMovieEntity(3, "Recent Movie").copy(year = 2023),
                )
            movieDao.insertMovies(movies)

            // When
            val moviesInRange = movieDao.getMoviesByYearRange(2000, 2020)

            // Then
            assertEquals(1, moviesInRange.size)
            assertEquals("2000s Movie", moviesInRange[0].title)
        }

    @Test
    fun insertMovieWithComplexDataPreservesAllFields() =
        runTest {
            // Given
            val complexMovie =
                createTestMovieEntity(1, "Complex Movie").copy(
                    cast =
                        listOf(
                            Actor(
                                id = 1,
                                name = "Actor One",
                                character = "Hero",
                                profilePath = "/actor1.jpg",
                                order = 0,
                            ),
                            Actor(
                                id = 2,
                                name = "Actor Two",
                                character = "Villain",
                                profilePath = "/actor2.jpg",
                                order = 1,
                            ),
                        ),
                    similarMovies =
                        listOf(
                            SimilarContent(
                                id = 101,
                                title = "Similar Movie",
                                overview = "Similar overview",
                                releaseDate = "2023-01-01",
                                posterPath = "/similar.jpg",
                                backdropPath = "/similar_bg.jpg",
                                voteAverage = 8.0,
                                voteCount = 1000,
                                popularity = 100.0,
                                adult = false,
                                originalTitle = "Similar Movie",
                                originalLanguage = "en",
                                genreIds = listOf(1, 2),
                            ),
                        ),
                    belongsToCollection =
                        BelongsToCollection(
                            id = 1,
                            name = "Test Collection",
                            posterPath = "/collection.jpg",
                            backdropPath = "/collection_bg.jpg",
                        ),
                )

            // When
            movieDao.insertMovie(complexMovie)
            val retrievedMovie = movieDao.getMovieById(1)

            // Then
            assertNotNull(retrievedMovie)
            assertEquals(2, retrievedMovie?.cast?.size)
            assertEquals("Actor One", retrievedMovie?.cast?.get(0)?.name)
            assertEquals("Hero", retrievedMovie?.cast?.get(0)?.character)
            assertEquals(1, retrievedMovie?.similarMovies?.size)
            assertEquals("Similar Movie", retrievedMovie?.similarMovies?.get(0)?.title)
            assertNotNull(retrievedMovie?.belongsToCollection)
            assertEquals("Test Collection", retrievedMovie?.belongsToCollection?.name)
        }

    private fun createTestMovieEntity(
        id: Int,
        title: String,
    ): MovieEntity {
        return MovieEntity(
            tmdbId = id,
            title = title,
            overview = "Test overview for $title",
            releaseDate = "2023-01-01",
            posterUrl = "https://image.tmdb.org/poster$id.jpg",
            backdropUrl = "https://image.tmdb.org/backdrop$id.jpg",
            logoUrl = null,
            voteAverage = 8.0f,
            voteCount = 1000,
            popularity = 100.0f,
            adult = false,
            video = false,
            originalTitle = title,
            originalLanguage = "en",
            genreIds = listOf(1, 2),
            genres = listOf("Action", "Drama"),
            runtime = 120,
            budget = 1000000L,
            revenue = 5000000L,
            homepage = null,
            imdbId = "tt123456$id",
            status = "Released",
            tagline = "Test tagline",
            belongsToCollection = null,
            productionCompanies = emptyList(),
            productionCountries = emptyList(),
            spokenLanguages = emptyList(),
            cast = emptyList(),
            crew = emptyList(),
            images = null,
            videos = emptyList(),
            similarMovies = emptyList(),
            year = 2023,
            creationTimestamp = System.currentTimeMillis(),
            popularityOrder = null,
            trendingOrder = null,
            topRatedOrder = null,
        )
    }
}
