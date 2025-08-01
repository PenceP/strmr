package com.strmr.ai.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube URL utility for WebView-based playback
 * Provides YouTube URL parsing and video ID extraction
 */
@Singleton
class YouTubeExtractor
    @Inject
    constructor() {
        /**
         * Extract video ID from YouTube URL
         */
        fun extractVideoId(youtubeUrl: String): String? {
            Log.d("YouTubeExtractor", "🔍 Extracting video ID from: $youtubeUrl")

            return try {
                val patterns =
                    listOf(
                        "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)".toRegex(),
                        "youtube\\.com/embed/([\\w-]+)".toRegex(),
                        "youtube\\.com/v/([\\w-]+)".toRegex(),
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
