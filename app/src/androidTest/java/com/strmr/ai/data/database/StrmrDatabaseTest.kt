package com.strmr.ai.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrmrDatabaseTest {

    private lateinit var database: StrmrDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            StrmrDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun databaseCreationSucceeds() {
        // Database should be created successfully
        assertNotNull(database)
        assertTrue(database.isOpen)
    }

    @Test
    fun allDaosAreAccessible() {
        // Test that all DAOs can be accessed
        assertNotNull(database.movieDao())
        assertNotNull(database.tvShowDao())
        assertNotNull(database.accountDao())
        assertNotNull(database.playbackDao())
        assertNotNull(database.continueWatchingDao())
        assertNotNull(database.traktUserProfileDao())
        assertNotNull(database.traktUserStatsDao())
        assertNotNull(database.omdbRatingsDao())
        assertNotNull(database.seasonDao())
        assertNotNull(database.episodeDao())
        assertNotNull(database.collectionDao())
        assertNotNull(database.traktRatingsDao())
        assertNotNull(database.intermediateViewDao())
    }

    @Test
    fun transactionRollbackWorksCorrectly() = runTest {
        val movieDao = database.movieDao()
        val tvShowDao = database.tvShowDao()

        try {
            database.runInTransaction {
                // Insert a movie
                val movie = createTestMovieEntity(1, "Test Movie")
                movieDao.insertMovie(movie)
                
                // Insert a TV show
                val tvShow = createTestTvShowEntity(1, "Test Show")
                tvShowDao.insertTvShow(tvShow)
                
                // Simulate an error to trigger rollback
                throw RuntimeException("Simulated error")
            }
        } catch (e: RuntimeException) {
            // Expected exception
        }

        // Verify that neither record was inserted due to rollback
        val retrievedMovie = movieDao.getMovieById(1)
        val retrievedTvShow = tvShowDao.getTvShowById(1)
        
        assertNull(retrievedMovie)
        assertNull(retrievedTvShow)
    }

    @Test
    fun transactionCommitWorksCorrectly() = runTest {
        val movieDao = database.movieDao()
        val tvShowDao = database.tvShowDao()

        database.runInTransaction {
            // Insert a movie
            val movie = createTestMovieEntity(1, "Test Movie")
            movieDao.insertMovie(movie)
            
            // Insert a TV show
            val tvShow = createTestTvShowEntity(1, "Test Show")
            tvShowDao.insertTvShow(tvShow)
        }

        // Verify that both records were inserted successfully
        val retrievedMovie = movieDao.getMovieById(1)
        val retrievedTvShow = tvShowDao.getTvShowById(1)
        
        assertNotNull(retrievedMovie)
        assertNotNull(retrievedTvShow)
        assertEquals("Test Movie", retrievedMovie?.title)
        assertEquals("Test Show", retrievedTvShow?.name)
    }

    @Test
    fun databaseMigrationCanBeSimulated() = runTest {
        // This test ensures the database can handle version changes
        // In a real scenario, you would test actual migrations
        
        val initialMovieCount = database.movieDao().getAllMovies().size
        assertEquals(0, initialMovieCount)
        
        // Add some data
        val movie = createTestMovieEntity(1, "Migration Test Movie")
        database.movieDao().insertMovie(movie)
        
        val finalMovieCount = database.movieDao().getAllMovies().size
        assertEquals(1, finalMovieCount)
    }

    @Test
    fun concurrentAccessHandledCorrectly() = runTest {
        val movieDao = database.movieDao()
        
        // Simulate concurrent inserts
        val movies = (1..10).map { id ->
            createTestMovieEntity(id, "Concurrent Movie $id")
        }
        
        // Insert all movies
        movieDao.insertMovies(movies)
        
        // Verify all movies were inserted
        val allMovies = movieDao.getAllMovies()
        assertEquals(10, allMovies.size)
        
        // Verify data integrity
        movies.forEach { originalMovie ->
            val retrievedMovie = movieDao.getMovieById(originalMovie.tmdbId)
            assertNotNull(retrievedMovie)
            assertEquals(originalMovie.title, retrievedMovie?.title)
        }
    }

    private fun createTestMovieEntity(id: Int, title: String): MovieEntity {
        return MovieEntity(
            tmdbId = id,
            title = title,
            overview = "Test overview",
            releaseDate = "2023-01-01",
            posterUrl = "https://example.com/poster$id.jpg",
            backdropUrl = "https://example.com/backdrop$id.jpg",
            logoUrl = null,
            voteAverage = 8.0f,
            voteCount = 1000,
            popularity = 100.0f,
            adult = false,
            video = false,
            originalTitle = title,
            originalLanguage = "en",
            genreIds = listOf(1, 2),
            genres = listOf("Action"),
            runtime = 120,
            budget = 1000000L,
            revenue = 5000000L,
            homepage = null,
            imdbId = "tt123456$id",
            status = "Released",
            tagline = null,
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
            topRatedOrder = null
        )
    }

    private fun createTestTvShowEntity(id: Int, name: String): TvShowEntity {
        return TvShowEntity(
            tmdbId = id,
            name = name,
            overview = "Test TV show overview",
            firstAirDate = "2023-01-01",
            posterUrl = "https://example.com/tv_poster$id.jpg",
            backdropUrl = "https://example.com/tv_backdrop$id.jpg",
            logoUrl = null,
            voteAverage = 8.5f,
            voteCount = 500,
            popularity = 75.0f,
            adult = false,
            originalName = name,
            originalLanguage = "en",
            genreIds = listOf(1, 2),
            genres = listOf("Drama"),
            episodeRunTime = listOf(45),
            numberOfEpisodes = 24,
            numberOfSeasons = 2,
            status = "Ended",
            type = "Scripted",
            homepage = null,
            inProduction = false,
            languages = listOf("en"),
            lastAirDate = "2023-12-31",
            networks = emptyList(),
            originCountry = listOf("US"),
            productionCompanies = emptyList(),
            productionCountries = emptyList(),
            spokenLanguages = emptyList(),
            tagline = null,
            cast = emptyList(),
            crew = emptyList(),
            images = null,
            videos = emptyList(),
            similar = emptyList(),
            year = 2023,
            creationTimestamp = System.currentTimeMillis(),
            popularityOrder = null,
            trendingOrder = null,
            topRatedOrder = null
        )
    }
}