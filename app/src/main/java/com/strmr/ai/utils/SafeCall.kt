package com.strmr.ai.utils

/**
 * Safe execution wrapper that catches exceptions and returns null on failure
 * Used primarily for TV focus management where APIs can be unstable
 */
inline fun <T> safeCall(action: () -> T): T? {
    return try {
        action()
    } catch (e: Exception) {
        null
    }
}
