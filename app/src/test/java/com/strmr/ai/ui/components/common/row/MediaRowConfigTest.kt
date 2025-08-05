package com.strmr.ai.ui.components.common.row

import androidx.paging.PagingSource
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import com.strmr.ai.ui.components.common.events.EventHandler
import com.strmr.ai.ui.components.common.events.MediaType

class MediaRowConfigTest {
    
    private val mockEventHandler: EventHandler = mock()
    
    @Test
    fun `MediaRowConfig - given basic parameters - should create with defaults`() {
        // Given
        val title = "Test Row"
        val items = listOf("item1", "item2", "item3")
        val focusKey = "test_row_key"
        
        // When
        val config = MediaRowConfig(
            title = title,
            items = items,
            focusMemoryKey = focusKey,
            eventHandler = mockEventHandler,
            mediaType = MediaType.MOVIE
        )
        
        // Then
        assertEquals(title, config.title)
        assertEquals(items, config.items)
        assertEquals(focusKey, config.focusMemoryKey)
        assertEquals(mockEventHandler, config.eventHandler)
        assertEquals(MediaType.MOVIE, config.mediaType)
        
        // Verify defaults
        assertEquals(CardType.POSTER, config.cardType)
        assertNull(config.pagingSource)
        assertNull(config.maxItemsToShow)
        assertTrue(config.enableLongPress)
        assertEquals(500L, config.longPressTimeout)
        assertTrue(config.enableFastScrolling)
        assertTrue(config.showPlaceholdersDuringScroll)
        assertNull(config.itemMapper)
        assertNull(config.contentProvider)
        assertNull(config.analyticsConfig)
    }
    
    @Test
    fun `MediaRowConfig - given custom parameters - should override defaults`() {
        // Given
        val mockPagingSource: PagingSource<Int, String> = mock()
        val customAnalytics = AnalyticsConfig("custom_category")
        
        // When
        val config = MediaRowConfig(
            title = "Custom Row",
            items = emptyList(),
            pagingSource = mockPagingSource,
            cardType = CardType.LANDSCAPE,
            focusMemoryKey = "custom_key",
            eventHandler = mockEventHandler,
            mediaType = MediaType.TV_SHOW,
            maxItemsToShow = 5,
            enableLongPress = false,
            longPressTimeout = 1000L,
            enableFastScrolling = false,
            showPlaceholdersDuringScroll = false,
            analyticsConfig = customAnalytics
        )
        
        // Then
        assertEquals(mockPagingSource, config.pagingSource)
        assertEquals(CardType.LANDSCAPE, config.cardType)
        assertEquals(MediaType.TV_SHOW, config.mediaType)
        assertEquals(5, config.maxItemsToShow)
        assertFalse(config.enableLongPress)
        assertEquals(1000L, config.longPressTimeout)
        assertFalse(config.enableFastScrolling)
        assertFalse(config.showPlaceholdersDuringScroll)
        assertEquals(customAnalytics, config.analyticsConfig)
    }
    
    @Test
    fun `CardType enum - should contain all expected values`() {
        // When/Then - verify all card types exist
        val cardTypes = CardType.values()
        
        assertTrue("Should contain POSTER", cardTypes.contains(CardType.POSTER))
        assertTrue("Should contain LANDSCAPE", cardTypes.contains(CardType.LANDSCAPE))
        assertTrue("Should contain SQUARE", cardTypes.contains(CardType.SQUARE))
        assertTrue("Should contain CIRCLE", cardTypes.contains(CardType.CIRCLE))
        assertTrue("Should contain COMPACT", cardTypes.contains(CardType.COMPACT))
        assertTrue("Should contain HERO", cardTypes.contains(CardType.HERO))
    }
    
    @Test
    fun `AnalyticsConfig - given parameters - should create correctly`() {
        // Given
        val category = "test_category"
        val customProps = mapOf("key1" to "value1", "key2" to 123)
        
        // When
        val analytics = AnalyticsConfig(
            category = category,
            trackClicks = false,
            trackFocus = true,
            trackScrolling = true,
            customProperties = customProps
        )
        
        // Then
        assertEquals(category, analytics.category)
        assertFalse(analytics.trackClicks)
        assertTrue(analytics.trackFocus)
        assertTrue(analytics.trackScrolling)
        assertEquals(customProps, analytics.customProperties)
    }
    
    @Test
    fun `AnalyticsConfig - given defaults - should have expected defaults`() {
        // Given
        val category = "default_test"
        
        // When
        val analytics = AnalyticsConfig(category)
        
        // Then
        assertEquals(category, analytics.category)
        assertTrue(analytics.trackClicks)
        assertFalse(analytics.trackFocus)
        assertFalse(analytics.trackScrolling)
        assertTrue(analytics.customProperties.isEmpty())
    }
    
    @Test
    fun `MediaRowConfigs movieRow - should create movie configuration`() {
        // Given
        val title = "Popular Movies"
        val items = listOf("movie1", "movie2")
        val focusKey = "popular_movies"
        
        // When
        val config = MediaRowConfigs.movieRow(title, items, focusKey, mockEventHandler)
        
        // Then
        assertEquals(title, config.title)
        assertEquals(items, config.items)
        assertEquals(focusKey, config.focusMemoryKey)
        assertEquals(CardType.POSTER, config.cardType)
        assertEquals(MediaType.MOVIE, config.mediaType)
        assertEquals(mockEventHandler, config.eventHandler)
        
        // Verify analytics
        assertNotNull(config.analyticsConfig)
        assertEquals("movie_row", config.analyticsConfig?.category)
    }
    
    @Test
    fun `MediaRowConfigs tvShowRow - should create TV show configuration`() {
        // Given
        val title = "Trending TV Shows"
        val items = listOf("show1", "show2")
        val focusKey = "trending_shows"
        
        // When
        val config = MediaRowConfigs.tvShowRow(title, items, focusKey, mockEventHandler)
        
        // Then
        assertEquals(title, config.title)
        assertEquals(items, config.items)
        assertEquals(CardType.POSTER, config.cardType)
        assertEquals(MediaType.TV_SHOW, config.mediaType)
        
        // Verify analytics
        assertNotNull(config.analyticsConfig)
        assertEquals("tv_show_row", config.analyticsConfig?.category)
    }
    
    @Test
    fun `MediaRowConfigs continueWatchingRow - should create continue watching configuration`() {
        // Given
        val items = listOf("movie1", "episode1")
        val focusKey = "continue_watching"
        
        // When
        val config = MediaRowConfigs.continueWatchingRow(items, focusKey, mockEventHandler)
        
        // Then
        assertEquals("Continue Watching", config.title)
        assertEquals(items, config.items)
        assertEquals(CardType.LANDSCAPE, config.cardType)
        assertEquals(MediaType.MOVIE, config.mediaType) // Mixed content defaults to MOVIE
        assertTrue(config.enableLongPress)
        
        // Verify analytics with focus tracking
        assertNotNull(config.analyticsConfig)
        assertEquals("continue_watching", config.analyticsConfig?.category)
        assertTrue(config.analyticsConfig?.trackFocus ?: false)
    }
    
    @Test
    fun `MediaRowConfigs collectionRow - should create collection configuration`() {
        // Given
        val title = "Marvel Movies"
        val items = listOf("ironman", "thor", "hulk")
        val focusKey = "marvel_collection"
        val maxItems = 8
        
        // When
        val config = MediaRowConfigs.collectionRow(title, items, focusKey, mockEventHandler, maxItems)
        
        // Then
        assertEquals(title, config.title)
        assertEquals(items, config.items)
        assertEquals(CardType.POSTER, config.cardType)
        assertEquals(MediaType.COLLECTION, config.mediaType)
        assertEquals(maxItems, config.maxItemsToShow)
        
        // Verify analytics
        assertNotNull(config.analyticsConfig)
        assertEquals("collection_row", config.analyticsConfig?.category)
    }
    
    @Test
    fun `MediaRowConfigs pagedRow - should create paged configuration`() {
        // Given
        val title = "All Movies"
        val mockPagingSource: PagingSource<Int, String> = mock()
        val cardType = CardType.LANDSCAPE
        val mediaType = MediaType.MOVIE
        val focusKey = "all_movies_paged"
        
        // When
        val config = MediaRowConfigs.pagedRow(
            title, mockPagingSource, cardType, mediaType, focusKey, mockEventHandler
        )
        
        // Then
        assertEquals(title, config.title)
        assertEquals(mockPagingSource, config.pagingSource)
        assertEquals(cardType, config.cardType)
        assertEquals(mediaType, config.mediaType)
        assertEquals(focusKey, config.focusMemoryKey)
        assertTrue(config.enableFastScrolling)
        assertTrue(config.showPlaceholdersDuringScroll)
        
        // Verify analytics with scrolling tracking
        assertNotNull(config.analyticsConfig)
        assertEquals("movie_paged_row", config.analyticsConfig?.category)
        assertTrue(config.analyticsConfig?.trackScrolling ?: false)
    }
    
    @Test
    fun `MediaRowItem interface - should provide default implementations`() {
        // Given
        val testItem = object : MediaRowItem {
            override val id = "test_id"
            override val title = "Test Title"
            override val imageUrl = "https://example.com/image.jpg"
            override val mediaType = MediaType.MOVIE
        }
        
        // Then - verify default implementations
        assertNull(testItem.subtitle)
        assertNull(testItem.year)
        assertNull(testItem.rating)
        assertNull(testItem.progress)
        assertFalse(testItem.isWatched)
        assertFalse(testItem.isFavorite)
    }
    
    @Test
    fun `MediaRowItem interface - should allow overriding defaults`() {
        // Given
        val testItem = object : MediaRowItem {
            override val id = "test_id"
            override val title = "Test Movie"
            override val imageUrl = "https://example.com/poster.jpg"
            override val mediaType = MediaType.MOVIE
            override val subtitle = "Action Adventure"
            override val year = 2024
            override val rating = 8.5f
            override val progress = 0.75f
            override val isWatched = true
            override val isFavorite = true
        }
        
        // Then - verify overridden values
        assertEquals("Action Adventure", testItem.subtitle)
        assertEquals(2024, testItem.year)
        assertEquals(8.5f, testItem.rating, 0.01f)
        assertEquals(0.75f, testItem.progress, 0.01f)
        assertTrue(testItem.isWatched)
        assertTrue(testItem.isFavorite)
    }
}