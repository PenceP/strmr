package com.strmr.ai.ui.utils

import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * Safe focus request extension for FocusRequester that handles IllegalStateException
 * when the FocusRequester is not properly initialized or attached to a composable.
 */
fun FocusRequester.safeRequestFocus(tag: String = "FocusManager"): Boolean {
    return try {
        this.requestFocus()
        Log.d(tag, "✅ Focus request successful")
        true
    } catch (e: IllegalStateException) {
        Log.w(tag, "⚠️ Focus request failed - FocusRequester not initialized: ${e.message}")
        false
    } catch (e: Exception) {
        Log.e(tag, "❌ Unexpected error during focus request", e)
        false
    }
}

/**
 * Safe focus request with retry mechanism for navigation scenarios
 */
suspend fun FocusRequester.safeRequestFocusWithRetry(
    tag: String = "FocusManager",
    maxRetries: Int = 2,
    retryDelayMs: Long = 150
): Boolean {
    repeat(maxRetries) { attempt ->
        val success = this.safeRequestFocus("$tag-Attempt${attempt + 1}")
        if (success) {
            return true
        }
        
        if (attempt < maxRetries - 1) {
            Log.d(tag, "🔄 Retrying focus request in ${retryDelayMs}ms (attempt ${attempt + 1}/$maxRetries)")
            delay(retryDelayMs)
        }
    }
    
    Log.w(tag, "❌ All focus request attempts failed after $maxRetries tries")
    return false
}

/**
 * Navigation-safe focus request that accounts for navigation timing
 */
suspend fun FocusRequester.navigationSafeFocusRequest(
    tag: String = "NavigationFocus",
    initialDelayMs: Long = 250
): Boolean {
    Log.d(tag, "🚀 Starting navigation-safe focus request with ${initialDelayMs}ms initial delay")
    
    // Wait for navigation to complete
    delay(initialDelayMs)
    
    // Attempt focus request with retry
    return safeRequestFocusWithRetry(tag, maxRetries = 3, retryDelayMs = 100)
}