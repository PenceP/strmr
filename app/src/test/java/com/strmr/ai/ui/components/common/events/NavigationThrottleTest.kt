package com.strmr.ai.ui.components.common.events

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class NavigationThrottleTest {
    
    @Before
    fun setup() {
        NavigationThrottle.reset()
    }
    
    @Test
    fun `canNavigate - first call - should return true`() {
        // When
        val result = NavigationThrottle.canNavigate()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `canNavigate - rapid successive calls - should throttle second call`() {
        // Given
        NavigationThrottle.canNavigate() // First call
        
        // When - immediate second call
        val result = NavigationThrottle.canNavigate()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `canNavigate - after throttle delay - should allow navigation`() {
        // Given
        NavigationThrottle.canNavigate() // First call
        
        // When - wait for throttle to expire
        Thread.sleep(90) // Just over the 88ms throttle
        val result = NavigationThrottle.canNavigate()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `reset - should allow immediate navigation`() {
        // Given
        NavigationThrottle.canNavigate() // First call
        assertFalse(NavigationThrottle.canNavigate()) // Should be throttled
        
        // When
        NavigationThrottle.reset()
        val result = NavigationThrottle.canNavigate()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `getRemainingThrottleTime - immediately after navigation - should return positive value`() {
        // Given
        NavigationThrottle.canNavigate()
        
        // When
        val remainingTime = NavigationThrottle.getRemainingThrottleTime()
        
        // Then
        assertTrue("Remaining time should be positive", remainingTime > 0)
        assertTrue("Remaining time should be <= 88ms", remainingTime <= 88)
    }
    
    @Test
    fun `getRemainingThrottleTime - when navigation allowed - should return zero`() {
        // Given - reset to ensure no throttle
        NavigationThrottle.reset()
        
        // When
        val remainingTime = NavigationThrottle.getRemainingThrottleTime()
        
        // Then
        assertEquals(0L, remainingTime)
    }
}