package com.strmr.ai.ui.components.common.image

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.strmr.ai.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized image loading configuration for the Strmr app.
 * 
 * Optimized for Android TV with:
 * - Large memory cache for smooth scrolling through rows
 * - Generous disk cache for movie/TV show posters
 * - Crossfade animations for professional appearance
 * - Debug logging for development
 */
@Singleton
class StrmrImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Main ImageLoader instance configured for optimal performance on Android TV
     */
    val imageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                // Use 25% of available memory for image cache
                // On Android TV this provides smooth scrolling through large poster rows
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                // 100MB disk cache - enough for hundreds of movie posters
                .maxSizeBytes(100 * 1024 * 1024)
                .build()
        }
        // Enable crossfade for smooth image loading transitions
        .crossfade(true)
        .crossfade(300) // 300ms crossfade duration
        // Respect HTTP cache headers from image servers
        .respectCacheHeaders(true)
        // Configure cache policies for optimal performance
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        // Enable debug logging in debug builds
        .apply {
            if (BuildConfig.DEBUG) {
                logger(DebugLogger())
            }
        }
        .build()
    
    /**
     * Image dimensions for different card types - used for request optimization
     */
    object ImageDimensions {
        // Standard poster dimensions (2:3 aspect ratio)
        const val POSTER_WIDTH = 300
        const val POSTER_HEIGHT = 450
        
        // Landscape/backdrop dimensions (16:9 aspect ratio)
        const val LANDSCAPE_WIDTH = 533
        const val LANDSCAPE_HEIGHT = 300
        
        // Square dimensions (1:1 aspect ratio)
        const val SQUARE_WIDTH = 300
        const val SQUARE_HEIGHT = 300
        
        // Circular profile images
        const val CIRCLE_WIDTH = 200
        const val CIRCLE_HEIGHT = 200
        
        // Compact card dimensions
        const val COMPACT_WIDTH = 200
        const val COMPACT_HEIGHT = 300
        
        // Hero card dimensions (larger posters)
        const val HERO_WIDTH = 400
        const val HERO_HEIGHT = 600
    }
    
    /**
     * Common image loading parameters for different contexts
     */
    object LoadingConfig {
        // Placeholder fade-in duration
        const val PLACEHOLDER_FADE_DURATION = 200
        
        // Error retry count for failed loads
        const val ERROR_RETRY_COUNT = 2
        
        // Network timeout for image requests
        const val NETWORK_TIMEOUT_MS = 10000L
    }
}

