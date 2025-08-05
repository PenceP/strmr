package com.strmr.ai.ui.components.common.image

import android.content.Context
import coil.request.ImageRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for preloading images into cache to improve user experience.
 * 
 * Preloading ensures images are cached before they're needed, reducing
 * loading times when users scroll through content.
 */
@Singleton 
class ImagePreloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: StrmrImageLoader
) {
    private val preloadScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Preload a list of image URLs into cache
     * 
     * @param urls List of image URLs to preload
     * @param priority Priority level for preload requests
     */
    fun preloadImages(
        urls: List<String>,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ) {
        if (urls.isEmpty()) return
        
        preloadScope.launch {
            urls.forEach { url ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .apply {
                            when (priority) {
                                PreloadPriority.HIGH -> {
                                    // Use smaller size for high priority preloads
                                    size(StrmrImageLoader.ImageDimensions.COMPACT_WIDTH, 
                                         StrmrImageLoader.ImageDimensions.COMPACT_HEIGHT)
                                }
                                PreloadPriority.NORMAL -> {
                                    // Use poster size as default
                                    size(StrmrImageLoader.ImageDimensions.POSTER_WIDTH,
                                         StrmrImageLoader.ImageDimensions.POSTER_HEIGHT)
                                }
                                PreloadPriority.LOW -> {
                                    // Use original size, lowest priority
                                    // No size specification - let Coil decide
                                }
                            }
                        }
                        .build()
                    
                    imageLoader.imageLoader.enqueue(request)
                } catch (e: Exception) {
                    // Silently fail for preload requests - don't crash the app
                    // In production, could log to analytics for monitoring
                }
            }
        }
    }
    
    /**
     * Preload images for a specific media collection
     * 
     * @param mediaItems List of media items with image URLs
     * @param extractImageUrl Function to extract image URL from media item
     */
    fun <T> preloadMediaImages(
        mediaItems: List<T>,
        extractImageUrl: (T) -> String?,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ) {
        val urls = mediaItems.mapNotNull(extractImageUrl)
        preloadImages(urls, priority)
    }
    
    /**
     * Preload poster images from TMDB URLs
     * 
     * @param posterPaths List of TMDB poster paths (e.g., "/abc123.jpg")
     * @param priority Priority level for preload requests
     */
    fun preloadTmdbPosters(
        posterPaths: List<String?>,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ) {
        val urls = posterPaths.mapNotNull { path ->
            path?.let { "https://image.tmdb.org/t/p/w500$it" }
        }
        preloadImages(urls, priority)
    }
    
    /**
     * Preload backdrop images from TMDB URLs
     * 
     * @param backdropPaths List of TMDB backdrop paths (e.g., "/abc123.jpg")
     * @param priority Priority level for preload requests
     */
    fun preloadTmdbBackdrops(
        backdropPaths: List<String?>,
        priority: PreloadPriority = PreloadPriority.NORMAL
    ) {
        val urls = backdropPaths.mapNotNull { path ->
            path?.let { "https://image.tmdb.org/t/p/w1280$it" }
        }
        preloadImages(urls, priority)
    }
    
    /**
     * Clear all cached images to free up memory
     * Should be used sparingly, typically only on memory pressure
     */
    fun clearCache() {
        imageLoader.imageLoader.memoryCache?.clear()
        imageLoader.imageLoader.diskCache?.clear()
    }
    
    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): CacheStats {
        val memoryCache = imageLoader.imageLoader.memoryCache
        val diskCache = imageLoader.imageLoader.diskCache
        
        return CacheStats(
            memoryCacheSize = memoryCache?.size?.toLong() ?: 0L,
            memoryCacheMaxSize = memoryCache?.maxSize?.toLong() ?: 0L,
            diskCacheSize = diskCache?.size?.toLong() ?: 0L,
            diskCacheMaxSize = diskCache?.maxSize?.toLong() ?: 0L
        )
    }
}

/**
 * Priority levels for image preloading
 */
enum class PreloadPriority {
    HIGH,    // For images likely to be seen soon (next row)
    NORMAL,  // For images that might be seen (2-3 rows ahead)
    LOW      // For images unlikely to be seen (background preload)
}

/**
 * Cache statistics for monitoring
 */
data class CacheStats(
    val memoryCacheSize: Long,
    val memoryCacheMaxSize: Long,
    val diskCacheSize: Long,
    val diskCacheMaxSize: Long
) {
    val memoryCacheUsagePercent: Float
        get() = if (memoryCacheMaxSize > 0) {
            (memoryCacheSize.toFloat() / memoryCacheMaxSize.toFloat()) * 100f
        } else 0f
    
    val diskCacheUsagePercent: Float 
        get() = if (diskCacheMaxSize > 0) {
            (diskCacheSize.toFloat() / diskCacheMaxSize.toFloat()) * 100f
        } else 0f
}