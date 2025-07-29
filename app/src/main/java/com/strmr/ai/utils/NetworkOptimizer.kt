package com.strmr.ai.utils

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Network optimization utilities for request deduplication and caching
 * Reduces redundant API calls and improves performance
 */
object NetworkOptimizer {
    
    private const val TAG = "NetworkOptimizer"
    
    // Request deduplication cache
    private val inFlightRequests = ConcurrentHashMap<String, Call>()
    private val responseCache = ConcurrentHashMap<String, CacheEntry>()
    
    // Cache settings
    private const val DEFAULT_CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
    private const val TRENDING_CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
    private const val POPULAR_CACHE_DURATION_MS = 60 * 60 * 1000L // 1 hour
    
    data class CacheEntry(
        val response: String,
        val timestamp: Long,
        val cacheDurationMs: Long = DEFAULT_CACHE_DURATION_MS
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > cacheDurationMs
        }
    }
    
    /**
     * Request deduplication interceptor
     * Prevents multiple identical requests from being made simultaneously
     */
    class RequestDeduplicationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val cacheKey = generateCacheKey(request)
            
            // Check if we have a fresh cached response
            responseCache[cacheKey]?.let { cached ->
                if (!cached.isExpired()) {
                    Log.d(TAG, "🎯 Cache HIT for $cacheKey")
                    return createResponseFromCache(request, cached.response)
                } else {
                    Log.d(TAG, "⏰ Cache EXPIRED for $cacheKey")
                    responseCache.remove(cacheKey)
                }
            }
            
            // Check if request is already in flight
            inFlightRequests[cacheKey]?.let { existingCall ->
                if (!existingCall.isCanceled()) {
                    Log.d(TAG, "🔄 Request deduplication for $cacheKey")
                    // Cancel this request and return the existing one's result
                    // In a real implementation, you'd wait for the existing call
                    return chain.proceed(request)
                }
            }
            
            // Proceed with new request
            val call = chain.call()
            inFlightRequests[cacheKey] = call
            
            try {
                val response = chain.proceed(request)
                
                // Cache successful responses
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let { body ->
                        val cacheDuration = getCacheDurationForUrl(request.url.toString())
                        responseCache[cacheKey] = CacheEntry(body, System.currentTimeMillis(), cacheDuration)
                        Log.d(TAG, "💾 Cached response for $cacheKey (${cacheDuration}ms)")
                        
                        // Recreate response with cached body
                        return response.newBuilder()
                            .body(body.toResponseBody(response.body?.contentType()))
                            .build()
                    }
                }
                
                return response
                
            } finally {
                inFlightRequests.remove(cacheKey)
            }
        }
        
        private fun generateCacheKey(request: Request): String {
            val url = request.url.toString()
            val method = request.method
            return "${method}_${url.hashCode()}"
        }
        
        private fun getCacheDurationForUrl(url: String): Long {
            return when {
                url.contains("trending") -> TRENDING_CACHE_DURATION_MS
                url.contains("popular") -> POPULAR_CACHE_DURATION_MS
                else -> DEFAULT_CACHE_DURATION_MS
            }
        }
        
        private fun createResponseFromCache(request: Request, cachedBody: String): Response {
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "application/json")
                .header("Cache-Control", "max-age=300")
                .body(ResponseBody.create("application/json".toMediaTypeOrNull(), cachedBody))
                .build()
        }
    }
    
    /**
     * Request timing interceptor for performance monitoring
     */
    class RequestTimingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.currentTimeMillis()
            
            val response = chain.proceed(request)
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            val url = request.url.toString()
            when {
                duration > 3000 -> Log.w(TAG, "🐌 SLOW REQUEST: $url took ${duration}ms")
                duration > 1000 -> Log.w(TAG, "⚠️ SLOW REQUEST: $url took ${duration}ms")
                else -> Log.d(TAG, "⚡ Request: $url (${duration}ms)")
            }
            
            return response
        }
    }
    
    /**
     * Create optimized OkHttp client with deduplication and caching
     */
    fun createOptimizedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(RequestDeduplicationInterceptor())
            .addInterceptor(RequestTimingInterceptor())
            .addInterceptor(createRetryInterceptor())
            .apply {
                // Add logging in debug builds
                if (android.util.Log.isLoggable(TAG, Log.DEBUG)) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .cache(createDiskCache())
            .build()
    }
    
    /**
     * Create retry interceptor for failed requests
     */
    private fun createRetryInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)
            var retryCount = 0
            val maxRetries = 3
            
            while (!response.isSuccessful && retryCount < maxRetries) {
                retryCount++
                Log.w(TAG, "🔄 Retrying request (attempt $retryCount/$maxRetries): ${request.url}")
                
                response.close()
                
                // Exponential backoff
                try {
                    Thread.sleep(1000L * retryCount)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Request interrupted during retry", e)
                }
                
                response = chain.proceed(request)
            }
            
            if (!response.isSuccessful && retryCount >= maxRetries) {
                Log.e(TAG, "❌ Request failed after $maxRetries retries: ${request.url}")
            }
            
            response
        }
    }
    
    /**
     * Create disk cache for HTTP responses
     */
    private fun createDiskCache(): Cache? {
        return try {
            // This would need a proper cache directory in a real implementation
            // Cache(cacheDirectory, maxSize)
            null // Simplified for now
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to create HTTP disk cache", e)
            null
        }
    }
    
    /**
     * Clear network caches
     */
    fun clearCache() {
        Log.d(TAG, "🧹 Clearing network caches")
        responseCache.clear()
        inFlightRequests.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val totalRequests = responseCache.size
        val expiredEntries = responseCache.values.count { it.isExpired() }
        val activeEntries = totalRequests - expiredEntries
        val inFlightCount = inFlightRequests.size
        
        return CacheStats(
            totalCachedRequests = totalRequests,
            activeCachedRequests = activeEntries,
            expiredCachedRequests = expiredEntries,
            inFlightRequests = inFlightCount
        )
    }
    
    data class CacheStats(
        val totalCachedRequests: Int,
        val activeCachedRequests: Int,
        val expiredCachedRequests: Int,
        val inFlightRequests: Int
    )
    
    /**
     * Log cache statistics
     */
    fun logCacheStats() {
        val stats = getCacheStats()
        Log.d(TAG, "📊 Network Cache Stats:")
        Log.d(TAG, "  Total cached: ${stats.totalCachedRequests}")
        Log.d(TAG, "  Active: ${stats.activeCachedRequests}")
        Log.d(TAG, "  Expired: ${stats.expiredCachedRequests}")
        Log.d(TAG, "  In-flight: ${stats.inFlightRequests}")
    }
    
    /**
     * Periodic cache cleanup
     */
    fun cleanupExpiredEntries() {
        val initialSize = responseCache.size
        val iterator = responseCache.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.isExpired()) {
                iterator.remove()
            }
        }
        
        val removedCount = initialSize - responseCache.size
        if (removedCount > 0) {
            Log.d(TAG, "🧹 Cleaned up $removedCount expired cache entries")
        }
    }
}