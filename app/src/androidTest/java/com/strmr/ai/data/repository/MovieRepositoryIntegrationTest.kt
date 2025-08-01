package com.strmr.ai.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.mapper.MovieMapper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.strmr.ai.data.MovieRepository as DataMovieRepository

@RunWith(AndroidJUnit4::class)
class MovieRepositoryIntegrationTest {
    private lateinit var database: StrmrDatabase
    private lateinit var dataRepository: DataMovieRepository
    private lateinit var movieMapper: MovieMapper
    private lateinit var movieRepositoryImpl: MovieRepositoryImpl

    @Before
    fun setup() {
        database =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                StrmrDatabase::class.java,
            ).allowMainThreadQueries().build()

        dataRepository =
            DataMovieRepository(
                movieDao = database.movieDao(),
                // Not needed for this integration test
                tmdbApiService = null,
                // Not needed for this integration test
                omdbRepository = null,
                // Not needed for this integration test
                traktApiService = null,
            )

        movieMapper = MovieMapper()
        movieRepositoryImpl = MovieRepositoryImpl(dataRepository, movieMapper)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getTrendingMoviesIntegrationTest() =
        runTest {
            // Given - Insert test data directly into database
            val testMovies =
                listOf(
                    createTestMovieEntity(1, "Trending Movie 1").copy(trendingOrder = 1),
                    createTestMovieEntity(2, "Trending Movie 2").copy(trendingOrder = 2),
                    createTestMovieEntity(3, "Regular Movie").copy(trendingOrder = null),
                )
            database.movieDao().insertMovies(testMovies)

            // When
            val result = movieRepositoryImpl.getTrendingMovies()

            // Then
            assertNotNull(result)
            assertEquals(2, result?.size) // Only movies with trendingOrder should be returned
            assertEquals("Trending Movie 1", result?.get(0)?.title)
            assertEquals("Trending Movie 2", result?.get(1)?.title)
        }

    @Test
    fun getMovieDetailsIntegrationTest() =
        runTest {
            // Given - Insert test data with complex relationships
            val testMovie =
                createTestMovieEntity(123, "Complex Movie").copy(
                    genres = listOf("Action", "Thriller"),
                    cast =
                        listOf(
                            com.strmr.ai.data.Actor(
                                id = 1,
                                name = "Test Actor",
                                character = "Hero",
                                profilePath = "/actor.jpg",
                                order = 0,
                            ),
                        ),
                    similarMovies =
                        listOf(
                            com.strmr.ai.data.SimilarContent(
                                id = 456,
                                title = "Similar Movie",
                                overview = "Similar plot",
                                releaseDate = "2023-01-01",
                                posterPath = "/similar.jpg",
                                backdropPath = "/similar_bg.jpg",
                                voteAverage = 7.5,
                                voteCount = 800,
                                popularity = 90.0,
                                adult = false,
                                originalTitle = "Similar Movie",
                                originalLanguage = "en",
                                genreIds = listOf(1, 2),
                            ),
                        ),
                )
            database.movieDao().insertMovie(testMovie)

            // When
            val result = movieRepositoryImpl.getMovieDetails(123)

            // Then
            assertNotNull(result)
            assertEquals("Complex Movie", result?.title)
            assertEquals(2, result?.genres?.size)
            assertTrue(result?.genres?.any { it.name == "Action" } == true)
            assertTrue(result?.genres?.any { it.name == "Thriller" } == true)
            assertEquals(1, result?.cast?.size)
            assertEquals("Test Actor", result?.cast?.get(0)?.person?.name)
            assertEquals("Hero", result?.cast?.get(0)?.character)
            assertEquals(1, result?.similarMovies?.size)
            assertEquals("Similar Movie", result?.similarMovies?.get(0)?.title)
        }

    @Test
    fun dataLayerTodomainLayerMappingIntegrationTest() =
        runTest {
            // Given - Complex movie with all possible fields populated
            val complexMovie =
                createTestMovieEntity(999, "Full Feature Movie").copy(
                    overview = "Comprehensive movie overview",
                    voteAverage = 9.2f,
                    voteCount = 5000,
                    runtime = 150,
                    year = 2024,
                    imdbId = "tt9999999",
                    belongsToCollection =
                        com.strmr.ai.data.BelongsToCollection(
                            id = 1,
                            name = "Epic Collection",
                            posterPath = "/collection.jpg",
                            backdropPath = "/collection_bg.jpg",
                        ),
                )
            database.movieDao().insertMovie(complexMovie)

            // When
            val domainMovie = movieRepositoryImpl.getMovieDetails(999)

            // Then - Verify all fields are correctly mapped
            assertNotNull(domainMovie)
            assertEquals("Full Feature Movie", domainMovie?.title)
            assertEquals("Comprehensive movie overview", domainMovie?.overview)
            assertEquals(9.2f, domainMovie?.rating?.primaryRating ?: 0f, 0.01f)
            assertEquals(150, domainMovie?.runtime?.minutes)
            assertEquals(2024, domainMovie?.year)
            assertEquals("tt9999999", domainMovie?.imdbId?.value)
            assertNotNull(domainMovie?.collection)
            assertEquals("Epic Collection", domainMovie?.collection?.name)
        }

    @Test
    fun repositoryReturnsNullForNonExistentMovie() =
        runTest {
            // When - Try to get a movie that doesn't exist
            val result = movieRepositoryImpl.getMovieDetails(99999)

            // Then
            assertNull(result)
        }

    @Test
    fun repositoryHandlesEmptyTrendingMovies() =
        runTest {
            // Given - No trending movies in database (all have null trendingOrder)
            val regularMovies =
                listOf(
                    createTestMovieEntity(1, "Regular Movie 1"),
                    createTestMovieEntity(2, "Regular Movie 2"),
                )
            database.movieDao().insertMovies(regularMovies)

            // When
            val result = movieRepositoryImpl.getTrendingMovies()

            // Then
            assertTrue(result?.isEmpty() == true)
        }

    @Test
    fun repositoryPreservesOrderFromDatabase() =
        runTest {
            // Given - Movies with specific trending order
            val trendingMovies =
                listOf(
                    createTestMovieEntity(1, "Third Place").copy(trendingOrder = 3),
                    createTestMovieEntity(2, "First Place").copy(trendingOrder = 1),
                    createTestMovieEntity(3, "Second Place").copy(trendingOrder = 2),
                )
            database.movieDao().insertMovies(trendingMovies)

            // When
            val result = movieRepositoryImpl.getTrendingMovies()

            // Then - Verify correct ordering is preserved
            assertNotNull(result)
            assertEquals(3, result?.size)
            assertEquals("First Place", result?.get(0)?.title)
            assertEquals("Second Place", result?.get(1)?.title)
            assertEquals("Third Place", result?.get(2)?.title)
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
