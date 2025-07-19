package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.youtube.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SmartTube-based YouTube URL extractor
 * Provides real YouTube video URL extraction functionality
 */
@Singleton
class YouTubeExtractor @Inject constructor(
    private val innerTubeClient: YouTubeInnerTubeClient,
    private val formatSelector: YouTubeFormatSelector,
    private val streamUrlResolver: YouTubeStreamUrlResolver,
    private val proxyExtractor: YouTubeProxyExtractor
) {
    
    /**
     * Extract direct video URL from YouTube URL
     * Uses SmartTube-based extraction with proxy fallback for reliability
     */
    suspend fun extractDirectUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("YouTubeExtractor", "🔍 Extracting direct URL using SmartTube approach: $youtubeUrl")
            
            val videoId = extractVideoId(youtubeUrl)
            if (videoId == null) {
                Log.w("YouTubeExtractor", "❌ Could not extract video ID")
                return@withContext null
            }
            
            // First try SmartTube approach with InnerTube API
            val playerResponse = innerTubeClient.getPlayerResponse(videoId)
            if (playerResponse != null && formatSelector.hasPlayableFormats(playerResponse.streamingData)) {
                // Select best format
                val selectedFormats = formatSelector.selectBestFormat(playerResponse.streamingData, "720p")
                if (selectedFormats != null) {
                    // Resolve stream URLs
                    val resolvedUrls = streamUrlResolver.resolveStreamUrls(selectedFormats)
                    if (resolvedUrls != null && streamUrlResolver.validateStreamUrls(resolvedUrls)) {
                        val qualityInfo = streamUrlResolver.getStreamQualityInfo(resolvedUrls)
                        Log.d("YouTubeExtractor", "✅ Successfully extracted ${qualityInfo.videoQuality} video URL via InnerTube")
                        return@withContext resolvedUrls.videoUrl
                    }
                }
            }
            
            // If InnerTube approach fails, try proxy extraction (Stremio approach)
            Log.d("YouTubeExtractor", "⚠️ InnerTube extraction failed, trying proxy extraction...")
            val proxyUrl = proxyExtractor.extractVideoUrl(youtubeUrl)
            if (proxyUrl != null) {
                Log.d("YouTubeExtractor", "✅ Successfully extracted URL via proxy service")
                return@withContext proxyUrl
            }
            
            Log.w("YouTubeExtractor", "❌ All extraction methods failed")
            null
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "❌ Error during URL extraction", e)
            null
        }
    }
    
    /**
     * Extract detailed video information including multiple quality options
     */
    suspend fun extractVideoInfo(youtubeUrl: String): YouTubeVideoInfo? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("YouTubeExtractor", "📋 Extracting video info for: $youtubeUrl")
            
            val videoId = extractVideoId(youtubeUrl)
            if (videoId == null) {
                Log.w("YouTubeExtractor", "❌ Could not extract video ID")
                return@withContext null
            }
            
            val playerResponse = innerTubeClient.getPlayerResponse(videoId)
            if (playerResponse == null) {
                Log.w("YouTubeExtractor", "❌ Failed to get player response")
                return@withContext null
            }
            
            val availableQualities = formatSelector.getAvailableQualities(playerResponse.streamingData)
            
            YouTubeVideoInfo(
                videoId = videoId,
                title = playerResponse.videoDetails?.title ?: "Unknown Title",
                duration = playerResponse.videoDetails?.lengthSeconds,
                availableQualities = availableQualities,
                hasPlayableFormats = formatSelector.hasPlayableFormats(playerResponse.streamingData)
            )
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "❌ Error extracting video info", e)
            null
        }
    }
    
    /**
     * Extract video URL with specific quality preference
     */
    suspend fun extractDirectUrlWithQuality(youtubeUrl: String, preferredQuality: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("YouTubeExtractor", "🎯 Extracting $preferredQuality URL for: $youtubeUrl")
            
            val videoId = extractVideoId(youtubeUrl)
            if (videoId == null) {
                Log.w("YouTubeExtractor", "❌ Could not extract video ID")
                return@withContext null
            }
            
            val playerResponse = innerTubeClient.getPlayerResponse(videoId)
            if (playerResponse == null) {
                Log.w("YouTubeExtractor", "❌ Failed to get player response")
                return@withContext null
            }
            
            val selectedFormats = formatSelector.selectBestFormat(playerResponse.streamingData, preferredQuality)
            if (selectedFormats == null) {
                Log.w("YouTubeExtractor", "❌ Failed to select format for quality: $preferredQuality")
                return@withContext null
            }
            
            val resolvedUrls = streamUrlResolver.resolveStreamUrls(selectedFormats)
            resolvedUrls?.videoUrl
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "❌ Error extracting URL with quality", e)
            null
        }
    }
    
    /**
     * Extract video ID from YouTube URL
     */
    fun extractVideoId(youtubeUrl: String): String? {
        Log.d("YouTubeExtractor", "🔍 Extracting video ID from: $youtubeUrl")
        
        return try {
            val patterns = listOf(
                "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)".toRegex(),
                "youtube\\.com/embed/([\\w-]+)".toRegex(),
                "youtube\\.com/v/([\\w-]+)".toRegex()
            )
            
            for (pattern in patterns) {
                val match = pattern.find(youtubeUrl)
                if (match != null) {
                    val videoId = match.groupValues[1]
                    Log.d("YouTubeExtractor", "✅ Extracted video ID: $videoId")
                    return videoId
                }
            }
            
            Log.w("YouTubeExtractor", "⚠️ Could not extract video ID from URL")
            null
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "❌ Error extracting video ID", e)
            null
        }
    }
    
    /**
     * Check if URL is a YouTube URL
     */
    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }
    
    /**
     * Get YouTube thumbnail URL for a video ID
     */
    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }
    
    /**
     * Get YouTube watch URL for a video ID
     */
    fun getWatchUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
}

/**
 * Data class for YouTube video information
 */
data class YouTubeVideoInfo(
    val videoId: String,
    val title: String,
    val duration: String?,
    val availableQualities: List<String>,
    val hasPlayableFormats: Boolean
)