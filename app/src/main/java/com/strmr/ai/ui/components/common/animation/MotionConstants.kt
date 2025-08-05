package com.strmr.ai.ui.components.common.animation

import androidx.compose.animation.core.CubicBezierEasing

/**
 * Motion constants for Android TV applications following Material Design guidelines
 * and Netflix-style streaming app animation patterns.
 * 
 * These constants provide consistent, polished motion throughout the application
 * with timing curves optimized for TV viewing distances and D-pad navigation.
 */
object MotionConstants {
    
    // Standard Material Design motion curves adapted for TV
    
    /**
     * Standard easing curve for general animations.
     * Used for: Basic transitions, property changes, loading states
     */
    val Standard = CubicBezierEasing(0.2f, 0.1f, 0.0f, 1.0f)
    
    /**
     * Browse easing curve optimized for row and content navigation.
     * Used for: Row scrolling, card focus changes, horizontal navigation
     */
    val Browse = CubicBezierEasing(0.18f, 1.0f, 0.22f, 1.0f)
    
    /**
     * Enter easing curve for focus entering animations.
     * Used for: Card scale-up on focus, selection highlights, hover states
     */
    val Enter = CubicBezierEasing(0.12f, 1.0f, 0.40f, 1.0f)
    
    /**
     * Exit easing curve for focus leaving animations.
     * Used for: Card scale-down on focus loss, deselection, fade-outs
     */
    val Exit = CubicBezierEasing(0.40f, 1.0f, 0.12f, 1.0f)
    
    // Animation durations (in milliseconds)
    
    /**
     * Standard duration for most animations.
     * Use for: General transitions, property animations
     */
    const val DURATION_STANDARD = 300
    
    /**
     * Browse duration for navigation animations.
     * Use for: Row scrolling, horizontal navigation, card selection
     */
    const val DURATION_BROWSE = 250
    
    /**
     * Enter duration for focus entering animations.
     * Use for: Focus highlights, scale-up effects, appearing content
     */
    const val DURATION_ENTER = 200
    
    /**
     * Exit duration for focus leaving animations.
     * Use for: Focus removal, scale-down effects, disappearing content
     */
    const val DURATION_EXIT = 150
    
    // Specialized durations for specific use cases
    
    /**
     * Quick duration for micro-interactions.
     * Use for: Button presses, ripple effects, immediate feedback
     */
    const val DURATION_QUICK = 100
    
    /**
     * Slow duration for dramatic effects.
     * Use for: Page transitions, loading screens, reveal animations
     */
    const val DURATION_SLOW = 500
    
    // Scale factors for focus animations
    
    /**
     * Default scale factor for focused cards.
     * Provides subtle emphasis without being distracting.
     */
    const val FOCUS_SCALE = 1.05f
    
    /**
     * Prominent scale factor for hero content or primary selections.
     * Use sparingly for maximum impact.
     */
    const val FOCUS_SCALE_PROMINENT = 1.1f
    
    /**
     * Subtle scale factor for minimal focus indication.
     * Use for dense content or secondary interactions.
     */
    const val FOCUS_SCALE_SUBTLE = 1.02f
    
    // Alpha values for fade animations
    
    /**
     * Disabled alpha for inactive content.
     */
    const val ALPHA_DISABLED = 0.38f
    
    /**
     * Medium alpha for secondary content.
     */
    const val ALPHA_MEDIUM = 0.60f
    
    /**
     * High alpha for active but not primary content.
     */
    const val ALPHA_HIGH = 0.87f
    
    /**
     * Full alpha for primary content.
     */
    const val ALPHA_FULL = 1.0f
}