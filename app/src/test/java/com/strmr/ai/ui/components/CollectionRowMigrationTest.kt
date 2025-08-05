package com.strmr.ai.ui.components

import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import com.strmr.ai.data.CollectionMovie
import com.strmr.ai.ui.components.common.events.EventHandler
import com.strmr.ai.ui.components.common.events.MediaType
import com.strmr.ai.ui.components.common.row.MediaRowConfigs
import com.strmr.ai.ui.components.common.row.CollectionMovieMediaRowItem

/**
 * Test migration from old CollectionRow to new MediaRow-based system
 */
class CollectionRowMigrationTest {
    
    @Test
    fun `CollectionMovieMediaRowItem - given CollectionMovie - should map properties correctly`() {
        // Given
        val testMovie = CollectionMovie(
            id = 550,
            title = "Fight Club",
            original_title = "Fight Club",
            overview = "A ticking-time-bomb insomniac...",
            poster_path = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
            backdrop_path = "/52AfXWuXCHn3UjD17rBruA9f5qb.jpg",
            media_type = "movie",
            original_language = "en",
            genre_ids = listOf(18, 53, 35),
            popularity = 61.411,
            release_date = "1999-10-15",
            video = false,
            vote_average = 8.433,
            vote_count = 26280
        )
        
        // When
        val mediaRowItem = CollectionMovieMediaRowItem(testMovie)
        
        // Then
        assertEquals("550", mediaRowItem.id)
        assertEquals("Fight Club", mediaRowItem.title)
        assertEquals("https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", mediaRowItem.imageUrl)
        assertEquals(MediaType.MOVIE, mediaRowItem.mediaType)
        assertEquals("1999", mediaRowItem.subtitle)
        assertEquals(1999, mediaRowItem.year)
        assertEquals(8.433f, mediaRowItem.rating)
        assertNull(mediaRowItem.progress)
        assertFalse(mediaRowItem.isWatched)
        assertFalse(mediaRowItem.isFavorite)
    }
    
    @Test
    fun `CollectionMovieMediaRowItem - given movie without poster - should handle null poster gracefully`() {
        // Given
        val testMovie = CollectionMovie(
            id = 1,
            title = "Test Movie",
            original_title = "Test Movie",
            overview = null,
            poster_path = null,
            backdrop_path = null,
            media_type = "movie",
            original_language = "en",
            genre_ids = emptyList(),
            popularity = 0.0,
            release_date = "2023-01-01",
            video = false,
            vote_average = 5.0,
            vote_count = 10
        )
        
        // When
        val mediaRowItem = CollectionMovieMediaRowItem(testMovie)
        
        // Then
        assertNull(mediaRowItem.imageUrl)
        assertEquals("Test Movie", mediaRowItem.title)
        assertEquals("2023", mediaRowItem.subtitle)
        assertEquals(2023, mediaRowItem.year)
    }
    
    @Test
    fun `CollectionMovieMediaRowItem - given movie without release date - should handle null date gracefully`() {
        // Given
        val testMovie = CollectionMovie(
            id = 2,
            title = "Undated Movie",
            original_title = "Undated Movie",
            overview = null,
            poster_path = "/test.jpg",
            backdrop_path = null,
            media_type = "movie",
            original_language = "en",
            genre_ids = emptyList(),
            popularity = 0.0,
            release_date = null,
            video = false,
            vote_average = 7.5,
            vote_count = 100
        )
        
        // When
        val mediaRowItem = CollectionMovieMediaRowItem(testMovie)
        
        // Then
        assertNull(mediaRowItem.subtitle)
        assertNull(mediaRowItem.year)
        assertEquals(7.5f, mediaRowItem.rating)
    }
    
    @Test
    fun `collectionMovieRow config - given movies list - should create correct configuration`() {
        // Given
        val movies = listOf(
            createTestCollectionMovie(1, "Movie 1"),
            createTestCollectionMovie(2, "Movie 2"),
            createTestCollectionMovie(3, "Movie 3")
        )
        val mockEventHandler = mock<EventHandler>()
        val focusKey = "test_collection"
        
        // When
        val config = MediaRowConfigs.collectionMovieRow(
            items = movies,
            focusKey = focusKey,
            eventHandler = mockEventHandler
        )
        
        // Then
        assertEquals("Part of Collection", config.title)
        assertEquals(3, config.items.size)
        assertEquals(focusKey, config.focusMemoryKey)
        assertEquals(mockEventHandler, config.eventHandler)
        assertEquals(MediaType.MOVIE, config.mediaType)
        assertNotNull(config.itemMapper)
        assertNotNull(config.analyticsConfig)
        assertEquals("collection_movie_row", config.analyticsConfig?.category)
    }
    
    @Test
    fun `collectionMovieRow config - given more than 10 movies - should limit to 10 items`() {
        // Given
        val movies = (1..15).map { createTestCollectionMovie(it, "Movie $it") }
        val mockEventHandler = mock<EventHandler>()
        
        // When
        val config = MediaRowConfigs.collectionMovieRow(
            items = movies,
            focusKey = "test_collection",
            eventHandler = mockEventHandler
        )
        
        // Then
        assertEquals(10, config.items.size)
        assertEquals("Movie 1", config.items.first().title)
        assertEquals("Movie 10", config.items.last().title)
    }
    
    @Test
    fun `collectionMovieRow config - item mapper - should convert CollectionMovie correctly`() {
        // Given
        val testMovie = createTestCollectionMovie(123, "Test Movie", "2023-05-15", 8.5)
        val movies = listOf(testMovie)
        val mockEventHandler = mock<EventHandler>()
        
        // When
        val config = MediaRowConfigs.collectionMovieRow(
            items = movies,
            focusKey = "test_collection",
            eventHandler = mockEventHandler
        )
        
        val mappedItem = config.itemMapper?.invoke(testMovie)
        
        // Then
        assertNotNull(mappedItem)
        assertEquals("123", mappedItem?.id)
        assertEquals("Test Movie", mappedItem?.title)
        assertEquals("2023", mappedItem?.subtitle)
        assertEquals(2023, mappedItem?.year)
        assertEquals(8.5f, mappedItem?.rating)
        assertEquals(MediaType.MOVIE, mappedItem?.mediaType)
    }
    
    @Test
    fun `EventHandler integration - item click - should map itemId to CollectionMovie correctly`() {
        // Given
        val movie1 = createTestCollectionMovie(100, "Movie 1")
        val movie2 = createTestCollectionMovie(200, "Movie 2")
        val movies = listOf(movie1, movie2)
        
        var clickedMovie: CollectionMovie? = null
        val onItemClick: (CollectionMovie) -> Unit = { movie -> clickedMovie = movie }
        
        // Create EventHandler similar to how CollectionRowNew would create it
        val eventHandler = object : EventHandler {
            override fun onItemClick(itemId: Int, mediaType: MediaType) {
                val movie = movies.find { it.id == itemId }
                movie?.let { onItemClick(it) }
            }
            
            override fun onItemLongPress(itemId: Int, mediaType: MediaType) {}
            override fun onItemFocus(itemId: Int, mediaType: MediaType) {}
            override fun onItemFocusLost(itemId: Int, mediaType: MediaType) {}
        }
        
        // When
        eventHandler.onItemClick(200, MediaType.MOVIE)
        
        // Then
        assertNotNull(clickedMovie)
        assertEquals(movie2, clickedMovie)
        assertEquals("Movie 2", clickedMovie?.title)
    }
    
    @Test
    fun `EventHandler integration - item click with non-existent ID - should handle gracefully`() {
        // Given
        val movies = listOf(createTestCollectionMovie(100, "Movie 1"))
        
        var clickedMovie: CollectionMovie? = null
        val onItemClick: (CollectionMovie) -> Unit = { movie -> clickedMovie = movie }
        
        val eventHandler = object : EventHandler {
            override fun onItemClick(itemId: Int, mediaType: MediaType) {
                val movie = movies.find { it.id == itemId }
                movie?.let { onItemClick(it) }
            }
            
            override fun onItemLongPress(itemId: Int, mediaType: MediaType) {}
            override fun onItemFocus(itemId: Int, mediaType: MediaType) {}
            override fun onItemFocusLost(itemId: Int, mediaType: MediaType) {}
        }
        
        // When
        eventHandler.onItemClick(999, MediaType.MOVIE) // Non-existent ID
        
        // Then
        assertNull(clickedMovie) // Should not crash, should handle gracefully
    }
    
    // Helper functions
    private fun createTestCollectionMovie(
        id: Int,
        title: String,
        releaseDate: String? = "2023-01-01",
        voteAverage: Double = 7.0
    ): CollectionMovie {
        return CollectionMovie(
            id = id,
            title = title,
            original_title = title,
            overview = "Test overview for $title",
            poster_path = "/test_poster_$id.jpg",
            backdrop_path = "/test_backdrop_$id.jpg",
            media_type = "movie",
            original_language = "en",
            genre_ids = listOf(18, 35),
            popularity = 50.0,
            release_date = releaseDate,
            video = false,
            vote_average = voteAverage,
            vote_count = 1000
        )
    }
}