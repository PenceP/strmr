package com.strmr.ai.ui.components.common.image

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for StrmrImageLoader configuration constants and dimensions
 */
class ImageLoaderTest {
    
    @Test
    fun `ImageDimensions - poster dimensions should follow 2to3 aspect ratio`() {
        // Given
        val width = StrmrImageLoader.ImageDimensions.POSTER_WIDTH
        val height = StrmrImageLoader.ImageDimensions.POSTER_HEIGHT
        
        // Then
        val aspectRatio = width.toFloat() / height.toFloat()
        val expected2to3Ratio = 2f / 3f
        
        assertTrue(
            "Poster aspect ratio should be approximately 2:3",
            kotlin.math.abs(aspectRatio - expected2to3Ratio) < 0.01f
        )
    }
    
    @Test
    fun `ImageDimensions - landscape dimensions should follow 16to9 aspect ratio`() {
        // Given
        val width = StrmrImageLoader.ImageDimensions.LANDSCAPE_WIDTH
        val height = StrmrImageLoader.ImageDimensions.LANDSCAPE_HEIGHT
        
        // Then
        val aspectRatio = width.toFloat() / height.toFloat()
        val expected16to9Ratio = 16f / 9f
        
        assertTrue(
            "Landscape aspect ratio should be approximately 16:9",
            kotlin.math.abs(aspectRatio - expected16to9Ratio) < 0.1f
        )
    }
    
    @Test
    fun `ImageDimensions - square dimensions should be equal`() {
        // Given
        val width = StrmrImageLoader.ImageDimensions.SQUARE_WIDTH
        val height = StrmrImageLoader.ImageDimensions.SQUARE_HEIGHT
        
        // Then
        assertEquals("Square dimensions should be equal", width, height)
    }
    
    @Test
    fun `ImageDimensions - circle dimensions should be equal`() {
        // Given
        val width = StrmrImageLoader.ImageDimensions.CIRCLE_WIDTH
        val height = StrmrImageLoader.ImageDimensions.CIRCLE_HEIGHT
        
        // Then
        assertEquals("Circle dimensions should be equal", width, height)
    }
    
    @Test
    fun `LoadingConfig - should have reasonable timeout values`() {
        // Given/When
        val placeholderFade = StrmrImageLoader.LoadingConfig.PLACEHOLDER_FADE_DURATION
        val retryCount = StrmrImageLoader.LoadingConfig.ERROR_RETRY_COUNT
        val networkTimeout = StrmrImageLoader.LoadingConfig.NETWORK_TIMEOUT_MS
        
        // Then
        assertTrue("Placeholder fade should be reasonable (50-500ms)", placeholderFade in 50..500)
        assertTrue("Retry count should be reasonable (1-3)", retryCount in 1..3)
        assertTrue("Network timeout should be reasonable (5-30 seconds)", networkTimeout in 5000..30000)
    }
    
    @Test
    fun `ImageDimensions - hero cards should be larger than standard posters`() {
        // Given
        val standardWidth = StrmrImageLoader.ImageDimensions.POSTER_WIDTH
        val standardHeight = StrmrImageLoader.ImageDimensions.POSTER_HEIGHT
        val heroWidth = StrmrImageLoader.ImageDimensions.HERO_WIDTH
        val heroHeight = StrmrImageLoader.ImageDimensions.HERO_HEIGHT
        
        // Then
        assertTrue("Hero width should be larger than standard poster", heroWidth > standardWidth)
        assertTrue("Hero height should be larger than standard poster", heroHeight > standardHeight)
    }
    
    @Test
    fun `ImageDimensions - compact cards should be reasonable size`() {
        // Given
        val compactWidth = StrmrImageLoader.ImageDimensions.COMPACT_WIDTH
        val compactHeight = StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT
        
        // Then
        assertTrue("Compact width should be reasonable", compactWidth in 100..300)
        assertTrue("Compact height should be reasonable", compactHeight in 150..400)
    }
    
    @Test
    fun `ImageDimensions - all dimensions should be positive`() {
        // Test all dimension constants are positive
        val dimensions = listOf(
            StrmrImageLoader.ImageDimensions.POSTER_WIDTH,
            StrmrImageLoader.ImageDimensions.POSTER_HEIGHT,
            StrmrImageLoader.ImageDimensions.LANDSCAPE_WIDTH,
            StrmrImageLoader.ImageDimensions.LANDSCAPE_HEIGHT,
            StrmrImageLoader.ImageDimensions.SQUARE_WIDTH,
            StrmrImageLoader.ImageDimensions.SQUARE_HEIGHT,
            StrmrImageLoader.ImageDimensions.CIRCLE_WIDTH,
            StrmrImageLoader.ImageDimensions.CIRCLE_HEIGHT,
            StrmrImageLoader.ImageDimensions.COMPACT_WIDTH,
            StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT,
            StrmrImageLoader.ImageDimensions.HERO_WIDTH,
            StrmrImageLoader.ImageDimensions.HERO_HEIGHT
        )
        
        dimensions.forEach { dimension ->
            assertTrue("All dimensions should be positive: $dimension", dimension > 0)
        }
    }
}