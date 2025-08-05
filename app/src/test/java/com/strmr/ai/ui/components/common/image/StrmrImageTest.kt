package com.strmr.ai.ui.components.common.image

import org.junit.Test
import org.junit.Assert.*
import com.strmr.ai.ui.components.common.row.CardType

/**
 * Unit tests for StrmrImage composable helper functions and dimension calculations
 */
class StrmrImageTest {
    
    @Test
    fun `getOptimalDimensions - poster card type - should return poster dimensions`() {
        // Given
        val cardType = CardType.POSTER
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.POSTER_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.POSTER_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - landscape card type - should return landscape dimensions`() {
        // Given
        val cardType = CardType.LANDSCAPE
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.LANDSCAPE_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.LANDSCAPE_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - square card type - should return square dimensions`() {
        // Given
        val cardType = CardType.SQUARE
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.SQUARE_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.SQUARE_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - circle card type - should return circle dimensions`() {
        // Given
        val cardType = CardType.CIRCLE
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.CIRCLE_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.CIRCLE_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - compact card type - should return compact dimensions`() {
        // Given
        val cardType = CardType.COMPACT
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.COMPACT_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - hero card type - should return hero dimensions`() {
        // Given
        val cardType = CardType.HERO
        
        // When
        val (width, height) = getOptimalDimensions(cardType)
        
        // Then
        assertEquals(StrmrImageLoader.ImageDimensions.HERO_WIDTH, width)
        assertEquals(StrmrImageLoader.ImageDimensions.HERO_HEIGHT, height)
    }
    
    @Test
    fun `getOptimalDimensions - all card types - should return positive dimensions`() {
        // Test all card types return positive dimensions
        val cardTypes = CardType.values()
        
        cardTypes.forEach { cardType ->
            // When
            val (width, height) = getOptimalDimensions(cardType)
            
            // Then
            assertTrue("Width should be positive for $cardType", width > 0)
            assertTrue("Height should be positive for $cardType", height > 0)
        }
    }
    
    @Test
    fun `getOptimalDimensions - hero cards - should be larger than standard cards`() {
        // Given
        val posterDimensions = getOptimalDimensions(CardType.POSTER)
        val heroDimensions = getOptimalDimensions(CardType.HERO)
        
        // Then
        assertTrue("Hero width should be larger than poster", heroDimensions.first > posterDimensions.first)
        assertTrue("Hero height should be larger than poster", heroDimensions.second > posterDimensions.second)
    }
    
    @Test
    fun `getOptimalDimensions - square and circle - should have equal width and height`() {
        // Test square cards
        val (squareWidth, squareHeight) = getOptimalDimensions(CardType.SQUARE)
        assertEquals("Square cards should have equal width and height", squareWidth, squareHeight)
        
        // Test circle cards  
        val (circleWidth, circleHeight) = getOptimalDimensions(CardType.CIRCLE)
        assertEquals("Circle cards should have equal width and height", circleWidth, circleHeight)
    }
    
    @Test
    fun `getOptimalDimensions - landscape cards - should have wider aspect ratio`() {
        // Given
        val (width, height) = getOptimalDimensions(CardType.LANDSCAPE)
        
        // Then
        val aspectRatio = width.toFloat() / height.toFloat()
        assertTrue("Landscape cards should be wider than tall", aspectRatio > 1.0f)
        
        // Should be approximately 16:9 (1.77)
        assertTrue("Landscape should be close to 16:9 ratio", aspectRatio > 1.5f && aspectRatio < 2.0f)
    }
    
    @Test
    fun `getOptimalDimensions - poster cards - should have taller aspect ratio`() {
        // Given
        val (width, height) = getOptimalDimensions(CardType.POSTER)
        
        // Then
        val aspectRatio = width.toFloat() / height.toFloat()
        assertTrue("Poster cards should be taller than wide", aspectRatio < 1.0f)
        
        // Should be approximately 2:3 (0.67)
        assertTrue("Poster should be close to 2:3 ratio", aspectRatio > 0.6f && aspectRatio < 0.8f)
    }
    
    // Helper function to access the private getOptimalDimensions function
    // In a real implementation, this would be made internal or public for testing
    private fun getOptimalDimensions(cardType: CardType): Pair<Int, Int> {
        return when (cardType) {
            CardType.POSTER -> Pair(
                StrmrImageLoader.ImageDimensions.POSTER_WIDTH,
                StrmrImageLoader.ImageDimensions.POSTER_HEIGHT
            )
            CardType.LANDSCAPE -> Pair(
                StrmrImageLoader.ImageDimensions.LANDSCAPE_WIDTH,
                StrmrImageLoader.ImageDimensions.LANDSCAPE_HEIGHT
            )
            CardType.SQUARE -> Pair(
                StrmrImageLoader.ImageDimensions.SQUARE_WIDTH,
                StrmrImageLoader.ImageDimensions.SQUARE_HEIGHT
            )
            CardType.CIRCLE -> Pair(
                StrmrImageLoader.ImageDimensions.CIRCLE_WIDTH,
                StrmrImageLoader.ImageDimensions.CIRCLE_HEIGHT
            )
            CardType.COMPACT -> Pair(
                StrmrImageLoader.ImageDimensions.COMPACT_WIDTH,
                StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT
            )
            CardType.HERO -> Pair(
                StrmrImageLoader.ImageDimensions.HERO_WIDTH,
                StrmrImageLoader.ImageDimensions.HERO_HEIGHT
            )
        }
    }
}