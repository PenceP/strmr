package com.strmr.ai.data.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.strmr.ai.data.TraktApiService
import com.strmr.ai.ui.theme.StrmrConstants
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(AndroidJUnit4::class)
class TraktApiServiceIntegrationTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var traktApiService: TraktApiService

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val retrofit =
            Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        traktApiService = retrofit.create(TraktApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getTrendingMoviesReturnsExpectedData() =
        runTest {
            // Given
            val mockResponse =
                """
                [
                    {
                        "watchers": 100,
                        "movie": {
                            "title": "Test Movie",
                            "year": 2023,
                            "ids": {
                                "trakt": 1,
                                "slug": "test-movie-2023",
                                "tmdb": 123,
                                "imdb": "tt1234567"
                            },
                            "tagline": "Test tagline",
                            "overview": "Test overview",
                            "released": "2023-01-01",
                            "runtime": 120,
                            "country": "US",
                            "trailer": "https://youtube.com/watch?v=test",
                            "homepage": "https://testmovie.com",
                            "status": "released",
                            "rating": 8.5,
                            "votes": 1000,
                            "comment_count": 50,
                            "language": "en",
                            "genres": ["action", "thriller"],
                            "certification": "PG-13"
                        }
                    }
                ]
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(mockResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = traktApiService.getTrendingMovies(page = 1, limit = StrmrConstants.Paging.PAGE_SIZE)

            // Then
            assertNotNull(result)
            assertEquals(1, result.size)
            assertEquals("Test Movie", result[0].movie.title)
            assertEquals(2023, result[0].movie.year)
            assertEquals(100, result[0].watchers)

            // Verify request details
            val request = mockWebServer.takeRequest()
            assertEquals("/movies/trending?page=1&limit=${StrmrConstants.Paging.PAGE_SIZE}", request.path)
            assertEquals("GET", request.method)
        }

    @Test
    fun getTrendingTvShowsReturnsExpectedData() =
        runTest {
            // Given
            val mockResponse =
                """
                [
                    {
                        "watchers": 150,
                        "show": {
                            "title": "Test Show",
                            "year": 2023,
                            "ids": {
                                "trakt": 2,
                                "slug": "test-show-2023",
                                "tmdb": 456,
                                "imdb": "tt2345678"
                            },
                            "tagline": "Test show tagline",
                            "overview": "Test show overview",
                            "first_aired": "2023-01-01T00:00:00.000Z",
                            "airs": {
                                "day": "Sunday",
                                "time": "21:00",
                                "timezone": "America/New_York"
                            },
                            "runtime": 45,
                            "certification": "TV-MA",
                            "network": "HBO",
                            "country": "US",
                            "trailer": "https://youtube.com/watch?v=testshow",
                            "homepage": "https://testshow.com",
                            "status": "ended",
                            "rating": 9.0,
                            "votes": 2000,
                            "comment_count": 100,
                            "language": "en",
                            "genres": ["drama", "thriller"],
                            "aired_episodes": 24
                        }
                    }
                ]
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(mockResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = traktApiService.getTrendingTvShows(page = 1, limit = StrmrConstants.Paging.PAGE_SIZE)

            // Then
            assertNotNull(result)
            assertEquals(1, result.size)
            assertEquals("Test Show", result[0].show.title)
            assertEquals(2023, result[0].show.year)
            assertEquals(150, result[0].watchers)

            // Verify request details
            val request = mockWebServer.takeRequest()
            assertEquals("/shows/trending?page=1&limit=${StrmrConstants.Paging.PAGE_SIZE}", request.path)
            assertEquals("GET", request.method)
        }

    @Test
    fun getMovieDetailsReturnsExpectedData() =
        runTest {
            // Given
            val mockResponse =
                """
                {
                    "title": "Detailed Movie",
                    "year": 2023,
                    "ids": {
                        "trakt": 123,
                        "slug": "detailed-movie-2023",
                        "tmdb": 789,
                        "imdb": "tt3456789"
                    },
                    "tagline": "Detailed tagline",
                    "overview": "Detailed overview",
                    "released": "2023-06-01",
                    "runtime": 135,
                    "country": "US",
                    "trailer": "https://youtube.com/watch?v=detailed",
                    "homepage": "https://detailedmovie.com",
                    "status": "released",
                    "rating": 8.7,
                    "votes": 1500,
                    "comment_count": 75,
                    "language": "en",
                    "genres": ["action", "adventure", "sci-fi"],
                    "certification": "PG-13",
                    "updated_at": "2023-06-01T12:00:00.000Z"
                }
                """.trimIndent()

            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(mockResponse)
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = traktApiService.getMovieDetails(789)

            // Then
            assertNotNull(result)
            assertEquals("Detailed Movie", result.title)
            assertEquals(2023, result.year)
            assertEquals(789, result.ids.tmdb)
            assertEquals("tt3456789", result.ids.imdb)
            assertEquals(135, result.runtime)
            assertEquals(8.7, result.rating, 0.01)
            assertEquals(3, result.genres.size)
            assertTrue(result.genres.contains("action"))
            assertTrue(result.genres.contains("adventure"))
            assertTrue(result.genres.contains("sci-fi"))

            // Verify request details
            val request = mockWebServer.takeRequest()
            assertEquals("/movies/789", request.path)
            assertEquals("GET", request.method)
        }

    @Test
    fun apiErrorHandling() =
        runTest {
            // Given - API returns error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .setBody("{\"error\": \"Not Found\"}")
                    .addHeader("Content-Type", "application/json"),
            )

            // When & Then - Should throw exception
            try {
                traktApiService.getMovieDetails(99999)
                fail("Expected exception was not thrown")
            } catch (e: Exception) {
                // Expected behavior - API call should fail
                assertTrue(e.message?.contains("404") == true || e is retrofit2.HttpException)
            }
        }

    @Test
    fun apiTimeoutHandling() =
        runTest {
            // Given - API takes too long to respond
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{}")
                    // Simulate timeout
                    .setBodyDelay(30, java.util.concurrent.TimeUnit.SECONDS),
            )

            // When & Then - Should handle timeout gracefully
            try {
                traktApiService.getTrendingMovies()
                fail("Expected timeout exception was not thrown")
            } catch (e: Exception) {
                // Expected behavior - should timeout
                assertTrue(e is java.net.SocketTimeoutException || e.message?.contains("timeout") == true)
            }
        }

    @Test
    fun rateLimitHandling() =
        runTest {
            // Given - API returns rate limit error
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setBody("{\"error\": \"Rate limit exceeded\"}")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Retry-After", "60"),
            )

            // When & Then - Should handle rate limiting
            try {
                traktApiService.getTrendingMovies()
                fail("Expected rate limit exception was not thrown")
            } catch (e: Exception) {
                // Expected behavior - should handle rate limiting
                assertTrue(e is retrofit2.HttpException)
                val httpException = e as retrofit2.HttpException
                assertEquals(429, httpException.code())
            }
        }

    @Test
    fun malformedJsonHandling() =
        runTest {
            // Given - API returns malformed JSON
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("{ invalid json }")
                    .addHeader("Content-Type", "application/json"),
            )

            // When & Then - Should handle malformed JSON gracefully
            try {
                traktApiService.getTrendingMovies()
                fail("Expected JSON parsing exception was not thrown")
            } catch (e: Exception) {
                // Expected behavior - should fail to parse JSON
                assertTrue(
                    e is com.google.gson.JsonSyntaxException ||
                        e.cause is com.google.gson.JsonSyntaxException,
                )
            }
        }

    @Test
    fun emptyResponseHandling() =
        runTest {
            // Given - API returns empty array
            mockWebServer.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("[]")
                    .addHeader("Content-Type", "application/json"),
            )

            // When
            val result = traktApiService.getTrendingMovies()

            // Then - Should handle empty response gracefully
            assertNotNull(result)
            assertTrue(result.isEmpty())
        }
}
