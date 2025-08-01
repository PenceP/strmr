package com.strmr.ai.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.strmr.ai.ui.theme.StrmrConstants
import kotlin.math.abs

/**
 * Creates a throttled FlingBehavior that limits scroll speed to improve TV remote responsiveness
 *
 * @param throttleMs Minimum time between scroll actions in milliseconds (default: 80ms)
 * @param momentumDamping Factor to reduce scroll momentum (0.0 = no momentum, 1.0 = full momentum)
 * @param minVelocityThreshold Minimum velocity required to trigger scrolling
 */
@Composable
fun rememberThrottledFlingBehavior(
    throttleMs: Long = StrmrConstants.Animation.SCROLL_THROTTLE_MS,
    momentumDamping: Float = 0.3f,
    minVelocityThreshold: Float = 100f,
): FlingBehavior {
    var lastScrollTime by remember { mutableStateOf(0L) }

    return remember(throttleMs, momentumDamping, minVelocityThreshold) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                val now = System.currentTimeMillis()
                val timeSinceLastScroll = now - lastScrollTime

                // Throttle scroll speed to prevent too-fast scrolling
                if (timeSinceLastScroll < throttleMs) {
                    // Too fast - consume velocity without scrolling
                    return 0f
                }

                lastScrollTime = now

                // Apply momentum damping for better TV control
                val dampedVelocity = initialVelocity * momentumDamping

                if (abs(dampedVelocity) < minVelocityThreshold) {
                    // Very low velocity - just stop
                    return 0f
                }

                // Allow controlled scrolling with reduced momentum
                return scrollBy(dampedVelocity * 0.1f)
            }
        }
    }
}

/**
 * Creates a completely momentum-free FlingBehavior for immediate stop behavior
 * Useful for DPAD-only navigation where momentum is unwanted
 */
@Composable
fun rememberImmediateStopFlingBehavior(): FlingBehavior {
    return remember {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                // Immediately consume all velocity - no momentum
                return 0f
            }
        }
    }
}
