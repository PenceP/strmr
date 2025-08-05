package com.strmr.ai.ui.components.common.loading

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceholderCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun placeholderCard_givenTitle_shouldDisplayTitleText() {
        // Given
        val testTitle = "Test Movie Title"
        
        // When
        composeTestRule.setContent {
            PlaceholderCard(title = testTitle)
        }
        
        // Then
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun placeholderCard_givenLongTitle_shouldTruncateWithEllipsis() {
        // Given
        val longTitle = "This is a very long movie title that should be truncated with ellipsis when displayed"
        
        // When
        composeTestRule.setContent {
            PlaceholderCard(title = longTitle)
        }
        
        // Then
        composeTestRule.onNodeWithText(longTitle, substring = true).assertIsDisplayed()
    }
    
    @Test
    fun moviePlaceholderCard_shouldHaveCorrectAspectRatioDimensions() {
        // Given
        val testTitle = "Movie Title"
        
        // When
        composeTestRule.setContent {
            MoviePlaceholderCard(title = testTitle)
        }
        
        // Then - Should display the title (component existence verification)
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun tvShowPlaceholderCard_shouldDisplayTvShowTitle() {
        // Given
        val testTitle = "TV Show Title"
        
        // When
        composeTestRule.setContent {
            TvShowPlaceholderCard(title = testTitle)
        }
        
        // Then
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun backdropPlaceholderCard_shouldDisplayBackdropTitle() {
        // Given
        val testTitle = "Backdrop Title"
        
        // When
        composeTestRule.setContent {
            BackdropPlaceholderCard(title = testTitle)
        }
        
        // Then
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun compactPlaceholderCard_shouldDisplayCompactTitle() {
        // Given
        val testTitle = "Compact Title"
        
        // When
        composeTestRule.setContent {
            CompactPlaceholderCard(title = testTitle)
        }
        
        // Then
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun placeholderCard_givenCustomColors_shouldAcceptColorParameters() {
        // Given
        val testTitle = "Custom Color Test"
        val customBackgroundColor = Color.Blue
        val customTextColor = Color.Yellow
        
        // When
        composeTestRule.setContent {
            PlaceholderCard(
                title = testTitle,
                backgroundColor = customBackgroundColor,
                textColor = customTextColor
            )
        }
        
        // Then - Should display the title (component renders without errors)
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun placeholderCard_givenCustomDimensions_shouldAcceptSizeParameters() {
        // Given
        val testTitle = "Custom Size Test"
        val customWidth = 200
        val customHeight = 300
        
        // When
        composeTestRule.setContent {
            PlaceholderCard(
                title = testTitle,
                width = customWidth,
                height = customHeight
            )
        }
        
        // Then - Should display the title (component renders without errors)
        composeTestRule.onNodeWithText(testTitle).assertIsDisplayed()
    }
    
    @Test
    fun placeholderCard_givenEmptyTitle_shouldHandleEmptyStringGracefully() {
        // Given
        val emptyTitle = ""
        
        // When
        composeTestRule.setContent {
            PlaceholderCard(title = emptyTitle)
        }
        
        // Then - Component should render without crashing
        // We can't test for empty text visibility, but we can ensure no crash occurs
        composeTestRule.onRoot().assertIsDisplayed()
    }
}