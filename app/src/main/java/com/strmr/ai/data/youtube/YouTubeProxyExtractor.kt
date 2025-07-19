package com.strmr.ai.data.youtube

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube extractor using proxy/external services approach
 * Similar to how Stremio handles YouTube content
 */
@Singleton
class YouTubeProxyExtractor @Inject constructor() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    companion object {
        // Public YouTube extraction services that apps like Stremio use
        // These handle the extraction server-side and return playable URLs
        private val EXTRACTION_SERVICES = listOf(
            // Invidious instances - open source YouTube frontend
            "https://invidious.fdn.fr",
            "https://invidious.privacyredirect.com",
            "https://inv.nadeko.net",
            
            // Piped API instances - privacy-friendly YouTube frontend
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.adminforge.de",
            
            // YouTube embed approach with no-cookie domain
            "https://www.youtube-nocookie.com"
        )
    }
    
    /**
     * Extract video URL using proxy services
     */
    suspend fun extractVideoUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(youtubeUrl) ?: return@withContext null
        
        Log.d("YouTubeProxyExtractor", "🌐 Attempting extraction via proxy services for: $videoId")
        
        // Try different extraction methods
        return@withContext tryInvidiousExtraction(videoId)
            ?: tryPipedExtraction(videoId)
            ?: tryEmbedExtraction(videoId)
    }
    
    /**
     * Try extraction using Invidious API
     */
    private suspend fun tryInvidiousExtraction(videoId: String): String? {
        for (instance in EXTRACTION_SERVICES.filter { it.contains("invidious") }) {
            try {
                Log.d("YouTubeProxyExtractor", "🔍 Trying Invidious instance: $instance")
                
                val url = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val videoData = gson.fromJson(json, JsonObject::class.java)
                    
                    // Extract adaptive formats or format streams
                    val adaptiveFormats = videoData.getAsJsonArray("adaptiveFormats")
                    val formatStreams = videoData.getAsJsonArray("formatStreams")
                    
                    // Try to find a suitable format
                    formatStreams?.forEach { element ->
                        val format = element.asJsonObject
                        val url = format.get("url")?.asString
                        val quality = format.get("qualityLabel")?.asString
                        
                        if (!url.isNullOrEmpty() && (quality?.contains("720p") == true || quality?.contains("360p") == true)) {
                            Log.d("YouTubeProxyExtractor", "✅ Found stream via Invidious: $quality")
                            return url
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("YouTubeProxyExtractor", "❌ Invidious instance failed: $instance", e)
            }
        }
        return null
    }
    
    /**
     * Try extraction using Piped API
     */
    private suspend fun tryPipedExtraction(videoId: String): String? {
        for (instance in EXTRACTION_SERVICES.filter { it.contains("piped") }) {
            try {
                Log.d("YouTubeProxyExtractor", "🔍 Trying Piped instance: $instance")
                
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val streamData = gson.fromJson(json, JsonObject::class.java)
                    
                    // Extract video streams
                    val videoStreams = streamData.getAsJsonArray("videoStreams")
                    
                    videoStreams?.forEach { element ->
                        val stream = element.asJsonObject
                        val url = stream.get("url")?.asString
                        val quality = stream.get("quality")?.asString
                        
                        if (!url.isNullOrEmpty() && (quality == "720p" || quality == "360p")) {
                            Log.d("YouTubeProxyExtractor", "✅ Found stream via Piped: $quality")
                            return url
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("YouTubeProxyExtractor", "❌ Piped instance failed: $instance", e)
            }
        }
        return null
    }
    
    /**
     * Try YouTube embed approach as fallback
     */
    private fun tryEmbedExtraction(videoId: String): String? {
        // Return embed URL that might work in some cases
        Log.d("YouTubeProxyExtractor", "🎥 Using YouTube embed fallback")
        return "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&modestbranding=1"
    }
    
    /**
     * Extract video ID from YouTube URL
     */
    private fun extractVideoId(youtubeUrl: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)".toRegex(),
            "youtube\\.com/embed/([\\w-]+)".toRegex(),
            "youtube\\.com/v/([\\w-]+)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(youtubeUrl)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return null
    }
    
    /**
     * Get direct MP4 stream using y2mate-style services
     * This is similar to what some apps use as a last resort
     */
    suspend fun getDirectMp4Stream(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Some apps use services that convert YouTube to direct MP4
            // This is a placeholder for such services
            Log.d("YouTubeProxyExtractor", "🎬 Attempting direct MP4 extraction")
            
            // Return null for now - in production you'd integrate with
            // services that provide direct MP4 URLs
            return@withContext null
        } catch (e: Exception) {
            Log.e("YouTubeProxyExtractor", "❌ Direct MP4 extraction failed", e)
            null
        }
    }
}