package com.strmr.ai.data.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts YouTube player configuration including STS (signatureTimestamp)
 * This is required for some videos to get working URLs
 */
@Singleton
class YouTubePlayerConfig @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    
    // Cache for player config
    private var cachedConfig: PlayerConfig? = null
    private var lastCacheTime: Long = 0
    private val CACHE_DURATION_MS = 3600000L // 1 hour
    
    companion object {
        private const val TAG = "YouTubePlayerConfig"
        private const val YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        
        // Patterns to extract player config
        private val STS_PATTERN = """"sts"\s*:\s*(\d+)""".toRegex()
        private val PLAYER_URL_PATTERN = """"jsUrl"\s*:\s*"([^"]+)"""".toRegex()
        private val VISITOR_DATA_PATTERN = """"visitorData"\s*:\s*"([^"]+)"""".toRegex()
        private val SESSION_INDEX_PATTERN = """"sessionIndex"\s*:\s*(\d+)""".toRegex()
    }
    
    data class PlayerConfig(
        val sts: String,
        val playerUrl: String,
        val visitorData: String?,
        val sessionIndex: String?
    )
    
    /**
     * Get current player configuration
     */
    suspend fun getPlayerConfig(): PlayerConfig? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Check cache
        if (cachedConfig != null && (now - lastCacheTime) < CACHE_DURATION_MS) {
            Log.d(TAG, "📦 Using cached player config")
            return@withContext cachedConfig
        }
        
        try {
            Log.d(TAG, "🔍 Fetching fresh player config")
            
            val request = Request.Builder()
                .url(YOUTUBE_WATCH_URL)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "❌ Failed to fetch YouTube page: ${response.code}")
                return@withContext null
            }
            
            val html = response.body?.string() ?: return@withContext null
            
            // Extract STS (signature timestamp)
            val stsMatch = STS_PATTERN.find(html)
            val sts = stsMatch?.groupValues?.get(1) ?: run {
                Log.e(TAG, "❌ Could not find STS in page")
                return@withContext null
            }
            
            // Extract player URL
            val playerUrlMatch = PLAYER_URL_PATTERN.find(html)
            val playerUrl = playerUrlMatch?.groupValues?.get(1)?.let { 
                if (it.startsWith("/")) "https://www.youtube.com$it" else it
            } ?: run {
                Log.e(TAG, "❌ Could not find player URL")
                return@withContext null
            }
            
            // Extract visitor data (optional but helpful)
            val visitorDataMatch = VISITOR_DATA_PATTERN.find(html)
            val visitorData = visitorDataMatch?.groupValues?.get(1)
            
            // Extract session index (optional)
            val sessionIndexMatch = SESSION_INDEX_PATTERN.find(html)
            val sessionIndex = sessionIndexMatch?.groupValues?.get(1)
            
            val config = PlayerConfig(
                sts = sts,
                playerUrl = playerUrl,
                visitorData = visitorData,
                sessionIndex = sessionIndex
            )
            
            Log.d(TAG, "✅ Extracted player config: STS=$sts")
            
            // Cache the result
            cachedConfig = config
            lastCacheTime = now
            
            return@withContext config
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching player config", e)
            return@withContext null
        }
    }
    
    /**
     * Extract player URL from watch page HTML
     */
    suspend fun getPlayerUrl(): String? {
        val config = getPlayerConfig()
        return config?.playerUrl
    }
    
    /**
     * Get signature timestamp
     */
    suspend fun getSignatureTimestamp(): String? {
        val config = getPlayerConfig()
        return config?.sts
    }
}