package com.strmr.ai.ui.components.common.events

/**
 * Navigation throttling utility for Android TV applications.
 * 
 * Implements the standard 88ms throttle pattern used by Android TV applications
 * to prevent overwhelming navigation events during rapid D-pad usage.
 * 
 * This helps prevent:
 * - Focus jumping too quickly between items
 * - Image loading being overwhelmed during fast scrolling
 * - Performance issues from excessive recomposition
 * - User interface lag during rapid navigation
 */
object NavigationThrottle {
    private var lastNavigationTime = 0L
    private const val NAVIGATION_THROTTLE_MS = 88L // Standard Android TV throttle timing
    
    /**
     * Checks if navigation is allowed based on the throttle timing.
     * 
     * @return true if enough time has passed since last navigation, false otherwise
     */
    fun canNavigate(): Boolean {
        val now = System.currentTimeMillis()
        return if (now - lastNavigationTime >= NAVIGATION_THROTTLE_MS) {
            lastNavigationTime = now
            true
        } else {
            false
        }
    }
    
    /**
     * Resets the throttle timer, allowing immediate navigation.
     * Useful when transitioning between different navigation contexts.
     */
    fun reset() {
        lastNavigationTime = 0L
    }
    
    /**
     * Gets the remaining throttle time in milliseconds.
     * 
     * @return milliseconds until next navigation is allowed, or 0 if navigation is allowed now
     */
    fun getRemainingThrottleTime(): Long {
        val now = System.currentTimeMillis()
        val elapsed = now - lastNavigationTime
        return if (elapsed >= NAVIGATION_THROTTLE_MS) {
            0L
        } else {
            NAVIGATION_THROTTLE_MS - elapsed
        }
    }
}