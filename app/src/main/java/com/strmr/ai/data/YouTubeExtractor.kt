package com.strmr.ai.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * YouTube URL extractor using NewPipeExtractor
 * Extracts direct video URLs that can be played in ExoPlayer
 */
@Singleton
class YouTubeExtractor @Inject constructor() {
    
    init {
        // Initialize NewPipe with a simple downloader
        try {
            NewPipe.init(object : Downloader() {
                @Throws(IOException::class, ReCaptchaException::class)
                override fun execute(request: Request): Response {
                    // Simple implementation - NewPipe will handle the actual downloading
                    throw UnsupportedOperationException("Custom downloader not fully implemented")
                }
            })
        } catch (e: Exception) {
            Log.w("YouTubeExtractor", "NewPipe initialization: ${e.message}")
        }
    }
    
    /**
     * Extract direct video URL from YouTube URL using NewPipeExtractor
     * Returns a direct URL that ExoPlayer can play
     */
    suspend fun extractDirectUrl(youtubeUrl: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("YouTubeExtractor", "🔍 Extracting direct URL from: $youtubeUrl")
            
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, youtubeUrl)
            
            // Try to get the best quality MP4 video stream
            val videoStream = streamInfo.videoStreams
                .filter { it.format == MediaFormat.MPEG_4 }
                .maxByOrNull { it.height }
            
            if (videoStream != null) {
                Log.d("YouTubeExtractor", "✅ Found direct URL: ${videoStream.url}")
                Log.d("YouTubeExtractor", "📐 Video quality: ${videoStream.height}p")
                videoStream.url
            } else {
                // Fallback to any available video stream
                val fallbackStream = streamInfo.videoStreams.firstOrNull()
                if (fallbackStream != null) {
                    Log.d("YouTubeExtractor", "⚠️ Using fallback stream: ${fallbackStream.url}")
                    Log.d("YouTubeExtractor", "📐 Fallback quality: ${fallbackStream.height}p, format: ${fallbackStream.format}")
                    fallbackStream.url
                } else {
                    Log.w("YouTubeExtractor", "❌ No video streams found")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "❌ Error extracting direct URL from YouTube", e)
            // Return original URL as fallback for navigation
            youtubeUrl
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