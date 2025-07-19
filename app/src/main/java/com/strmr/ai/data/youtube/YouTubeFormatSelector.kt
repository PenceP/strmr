package com.strmr.ai.data.youtube

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube format selector for choosing optimal video/audio streams
 * Based on SmartTube's format selection logic
 */
@Singleton
class YouTubeFormatSelector @Inject constructor() {
    
    companion object {
        // Quality preferences (higher number = better quality)
        private val QUALITY_RANKING = mapOf(
            "tiny" to 1,
            "small" to 2,
            "medium" to 3,
            "large" to 4,
            "hd720" to 5,
            "hd1080" to 6,
            "hd1440" to 7,
            "hd2160" to 8
        )
        
        // Preferred video codecs (in order of preference)
        private val PREFERRED_VIDEO_CODECS = listOf("avc1", "av01", "vp9")
        
        // Preferred audio codecs (in order of preference)
        private val PREFERRED_AUDIO_CODECS = listOf("mp4a", "opus")
        
        // Common video itags and their qualities
        private val VIDEO_ITAGS = mapOf(
            18 to "360p",    // mp4 360p
            22 to "720p",    // mp4 720p
            37 to "1080p",   // mp4 1080p
            38 to "3072p",   // mp4 3072p
            43 to "360p",    // webm 360p
            44 to "480p",    // webm 480p
            45 to "720p",    // webm 720p
            46 to "1080p"    // webm 1080p
        )
    }
    
    /**
     * Select the best format for playback based on quality and codec preferences
     */
    fun selectBestFormat(streamingData: StreamingData, preferredQuality: String = "720p"): SelectedFormats? {
        return try {
            Log.d("FormatSelector", "🎯 Selecting best format for quality: $preferredQuality")
            
            // First try to get combined formats (video + audio)
            val combinedFormat = selectBestCombinedFormat(streamingData.formats, preferredQuality)
            if (combinedFormat != null) {
                Log.d("FormatSelector", "✅ Selected combined format: itag=${combinedFormat.itag}")
                return SelectedFormats(
                    videoFormat = combinedFormat,
                    audioFormat = null,
                    isCombined = true
                )
            }
            
            // If no combined format, try adaptive formats (separate video + audio)
            val videoFormat = selectBestVideoFormat(streamingData.adaptiveFormats, preferredQuality)
            val audioFormat = selectBestAudioFormat(streamingData.adaptiveFormats)
            
            if (videoFormat != null && audioFormat != null) {
                Log.d("FormatSelector", "✅ Selected adaptive formats: video_itag=${videoFormat.itag}, audio_itag=${audioFormat.itag}")
                return SelectedFormats(
                    videoFormat = videoFormat,
                    audioFormat = audioFormat,
                    isCombined = false
                )
            }
            
            Log.w("FormatSelector", "❌ No suitable formats found")
            null
        } catch (e: Exception) {
            Log.e("FormatSelector", "❌ Error selecting format", e)
            null
        }
    }
    
    private fun selectBestCombinedFormat(formats: List<Format>, preferredQuality: String): Format? {
        if (formats.isEmpty()) return null
        
        // Filter formats that have both video and audio
        val combinedFormats = formats.filter { format ->
            format.mimeType.contains("video") && !format.mimeType.contains("video/mp4; codecs=\"avc1") ||
            format.itag in VIDEO_ITAGS.keys
        }
        
        if (combinedFormats.isEmpty()) return null
        
        // Find format closest to preferred quality
        val targetHeight = qualityToHeight(preferredQuality)
        
        return combinedFormats
            .filter { it.url != null || it.signatureCipher != null } // Must have a URL
            .sortedWith(compareBy<Format> { format ->
                // Primary sort: Quality closeness
                val height = format.height ?: getHeightFromItag(format.itag)
                if (height != null) {
                    kotlin.math.abs(height - targetHeight)
                } else {
                    Int.MAX_VALUE
                }
            }.thenBy { format ->
                // Secondary sort: Codec preference
                getCodecPreference(format.mimeType, PREFERRED_VIDEO_CODECS)
            }.thenByDescending { format ->
                // Tertiary sort: Bitrate (higher is better)
                format.bitrate
            })
            .firstOrNull()
    }
    
    private fun selectBestVideoFormat(adaptiveFormats: List<Format>, preferredQuality: String): Format? {
        val videoFormats = adaptiveFormats.filter { 
            it.mimeType.startsWith("video/") && (it.url != null || it.signatureCipher != null)
        }
        
        if (videoFormats.isEmpty()) return null
        
        val targetHeight = qualityToHeight(preferredQuality)
        
        return videoFormats
            .sortedWith(compareBy<Format> { format ->
                // Primary sort: Quality closeness
                val height = format.height ?: 720
                kotlin.math.abs(height - targetHeight)
            }.thenBy { format ->
                // Secondary sort: Codec preference
                getCodecPreference(format.mimeType, PREFERRED_VIDEO_CODECS)
            }.thenByDescending { format ->
                // Tertiary sort: Bitrate
                format.bitrate
            })
            .firstOrNull()
    }
    
    private fun selectBestAudioFormat(adaptiveFormats: List<Format>): Format? {
        val audioFormats = adaptiveFormats.filter { 
            it.mimeType.startsWith("audio/") && (it.url != null || it.signatureCipher != null)
        }
        
        if (audioFormats.isEmpty()) return null
        
        return audioFormats
            .sortedWith(compareBy<Format> { format ->
                // Primary sort: Codec preference
                getCodecPreference(format.mimeType, PREFERRED_AUDIO_CODECS)
            }.thenByDescending { format ->
                // Secondary sort: Audio quality
                when (format.audioQuality) {
                    "AUDIO_QUALITY_HIGH" -> 3
                    "AUDIO_QUALITY_MEDIUM" -> 2
                    "AUDIO_QUALITY_LOW" -> 1
                    else -> 0
                }
            }.thenByDescending { format ->
                // Tertiary sort: Bitrate
                format.bitrate
            })
            .firstOrNull()
    }
    
    private fun qualityToHeight(quality: String): Int {
        return when (quality.lowercase()) {
            "144p" -> 144
            "240p" -> 240
            "360p" -> 360
            "480p" -> 480
            "720p" -> 720
            "1080p" -> 1080
            "1440p" -> 1440
            "2160p" -> 2160
            "4320p" -> 4320
            else -> 720 // Default to 720p
        }
    }
    
    private fun getHeightFromItag(itag: Int): Int? {
        return when (itag) {
            160, 278 -> 144   // 144p
            133, 298 -> 240   // 240p
            134, 396 -> 360   // 360p
            135, 397 -> 480   // 480p
            136, 298, 399 -> 720   // 720p
            137, 299, 400 -> 1080  // 1080p
            264, 301, 401 -> 1440  // 1440p
            266, 313, 402 -> 2160  // 2160p
            else -> null
        }
    }
    
    private fun getCodecPreference(mimeType: String, preferredCodecs: List<String>): Int {
        for (i in preferredCodecs.indices) {
            if (mimeType.contains(preferredCodecs[i], ignoreCase = true)) {
                return i
            }
        }
        return Int.MAX_VALUE // Lowest preference for unknown codecs
    }
    
    /**
     * Get available qualities from streaming data
     */
    fun getAvailableQualities(streamingData: StreamingData): List<String> {
        val qualities = mutableSetOf<String>()
        
        // From combined formats
        streamingData.formats.forEach { format ->
            format.qualityLabel?.let { qualities.add(it) }
            format.height?.let { height -> qualities.add("${height}p") }
        }
        
        // From adaptive video formats
        streamingData.adaptiveFormats
            .filter { it.mimeType.startsWith("video/") }
            .forEach { format ->
                format.qualityLabel?.let { qualities.add(it) }
                format.height?.let { height -> qualities.add("${height}p") }
            }
        
        // Sort by quality (lowest to highest)
        return qualities.toList().sortedBy { quality ->
            val height = quality.replace("p", "").toIntOrNull() ?: 0
            height
        }
    }
    
    /**
     * Check if streaming data has any playable formats
     */
    fun hasPlayableFormats(streamingData: StreamingData): Boolean {
        val hasValidCombined = streamingData.formats.any { 
            it.url != null || it.signatureCipher != null 
        }
        
        val hasValidAdaptive = streamingData.adaptiveFormats.any { 
            it.url != null || it.signatureCipher != null 
        }
        
        return hasValidCombined || hasValidAdaptive
    }
}

/**
 * Data class for selected formats
 */
data class SelectedFormats(
    val videoFormat: Format,
    val audioFormat: Format?,
    val isCombined: Boolean
)