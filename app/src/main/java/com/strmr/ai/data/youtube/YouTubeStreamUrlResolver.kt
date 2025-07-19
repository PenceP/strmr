package com.strmr.ai.data.youtube

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves YouTube stream URLs from format data
 * Handles signature decryption and URL construction
 */
@Singleton
class YouTubeStreamUrlResolver @Inject constructor(
    private val signatureDecryptor: YouTubeSignatureDecryptor,
    private val nParamTransformer: YouTubeNParamTransformer
) {
    
    /**
     * Resolve stream URLs from selected formats
     */
    suspend fun resolveStreamUrls(selectedFormats: SelectedFormats): ResolvedStreamUrls? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("StreamUrlResolver", "🔗 Resolving stream URLs")
            
            val videoUrl = resolveFormatUrl(selectedFormats.videoFormat)
            if (videoUrl == null) {
                Log.w("StreamUrlResolver", "❌ Failed to resolve video URL")
                return@withContext null
            }
            
            val audioUrl = if (selectedFormats.audioFormat != null) {
                resolveFormatUrl(selectedFormats.audioFormat)
            } else null
            
            if (!selectedFormats.isCombined && audioUrl == null) {
                Log.w("StreamUrlResolver", "❌ Failed to resolve audio URL for adaptive format")
                return@withContext null
            }
            
            Log.d("StreamUrlResolver", "✅ Successfully resolved stream URLs")
            ResolvedStreamUrls(
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                isCombined = selectedFormats.isCombined,
                videoFormat = selectedFormats.videoFormat,
                audioFormat = selectedFormats.audioFormat
            )
        } catch (e: Exception) {
            Log.e("StreamUrlResolver", "❌ Error resolving stream URLs", e)
            null
        }
    }
    
    private suspend fun resolveFormatUrl(format: Format): String? {
        return try {
            // If URL is directly available, process it
            if (!format.url.isNullOrBlank()) {
                Log.d("StreamUrlResolver", "📝 Processing direct URL for itag ${format.itag}")
                // Check if URL needs n-parameter processing to prevent throttling
                return processUrlWithNParameter(format.url)
            }
            
            // If signature cipher is present, decrypt it
            if (!format.signatureCipher.isNullOrBlank()) {
                Log.d("StreamUrlResolver", "🔐 Decrypting signature cipher for itag ${format.itag}")
                val decryptedUrl = signatureDecryptor.decryptSignatureCipher(format.signatureCipher)
                // Process n-parameter if present
                return decryptedUrl?.let { processUrlWithNParameter(it) }
            }
            
            Log.w("StreamUrlResolver", "❌ No URL or cipher available for itag ${format.itag}")
            null
        } catch (e: Exception) {
            Log.e("StreamUrlResolver", "❌ Error resolving URL for itag ${format.itag}", e)
            null
        }
    }
    
    /**
     * Process URL to handle n-parameter which prevents throttling
     * YouTube uses this to detect and throttle bot traffic
     */
    private suspend fun processUrlWithNParameter(url: String): String {
        return try {
            Log.d("StreamUrlResolver", "🔄 Processing n-parameter in URL")
            
            // Extract n-parameter from URL
            val nParam = nParamTransformer.extractNParam(url)
            if (nParam == null) {
                Log.d("StreamUrlResolver", "ℹ️ No n-parameter found in URL")
                return addRateBypass(url)
            }
            
            Log.d("StreamUrlResolver", "📌 Found n-parameter: ${nParam.take(10)}...")
            
            // Transform the n-parameter
            val transformedN = nParamTransformer.transformNParam(nParam)
            if (transformedN == null || transformedN == nParam) {
                Log.w("StreamUrlResolver", "⚠️ N-parameter transformation failed or unchanged")
                return addRateBypass(url)
            }
            
            // Replace n-parameter in URL
            val newUrl = nParamTransformer.replaceNParam(url, nParam, transformedN)
            Log.d("StreamUrlResolver", "✅ Successfully transformed n-parameter")
            
            // Add ratebypass for extra reliability
            addRateBypass(newUrl)
        } catch (e: Exception) {
            Log.e("StreamUrlResolver", "❌ Error processing n-parameter", e)
            addRateBypass(url)
        }
    }
    
    private fun addRateBypass(url: String): String {
        return if (url.contains("?")) {
            "$url&ratebypass=yes"
        } else {
            "$url?ratebypass=yes"
        }
    }
    
    /**
     * Validate that the resolved URLs are accessible
     */
    suspend fun validateStreamUrls(resolvedUrls: ResolvedStreamUrls): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("StreamUrlResolver", "✅ Validating stream URLs")
            
            // For now, we'll assume URLs are valid if they're properly formatted
            // In a full implementation, you might want to send HEAD requests to verify accessibility
            
            val videoUrlValid = isValidStreamUrl(resolvedUrls.videoUrl)
            val audioUrlValid = resolvedUrls.audioUrl?.let { isValidStreamUrl(it) } ?: true
            
            val isValid = videoUrlValid && audioUrlValid
            Log.d("StreamUrlResolver", if (isValid) "✅ URLs validated successfully" else "❌ URL validation failed")
            
            isValid
        } catch (e: Exception) {
            Log.e("StreamUrlResolver", "❌ Error validating URLs", e)
            false
        }
    }
    
    private fun isValidStreamUrl(url: String): Boolean {
        return try {
            // Basic URL validation
            url.isNotBlank() && 
            (url.startsWith("https://") || url.startsWith("http://")) &&
            (url.contains("googlevideo.com") || url.contains("youtube.com"))
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extract quality information from resolved formats
     */
    fun getStreamQualityInfo(resolvedUrls: ResolvedStreamUrls): StreamQualityInfo {
        val videoFormat = resolvedUrls.videoFormat
        val audioFormat = resolvedUrls.audioFormat
        
        return StreamQualityInfo(
            videoQuality = videoFormat.qualityLabel ?: "${videoFormat.height}p",
            videoCodec = extractCodecFromMimeType(videoFormat.mimeType),
            videoBitrate = videoFormat.bitrate,
            audioQuality = audioFormat?.audioQuality,
            audioCodec = audioFormat?.let { extractCodecFromMimeType(it.mimeType) },
            audioBitrate = audioFormat?.bitrate,
            isCombined = resolvedUrls.isCombined
        )
    }
    
    private fun extractCodecFromMimeType(mimeType: String): String? {
        return try {
            // Extract codec from MIME type like "video/mp4; codecs=\"avc1.640028\""
            val codecsRegex = "codecs=\"([^\"]+)\"".toRegex()
            val match = codecsRegex.find(mimeType)
            match?.groupValues?.get(1)?.split(",")?.firstOrNull()?.trim()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Data classes for stream URL resolution
 */
data class ResolvedStreamUrls(
    val videoUrl: String,
    val audioUrl: String?,
    val isCombined: Boolean,
    val videoFormat: Format,
    val audioFormat: Format?
)

data class StreamQualityInfo(
    val videoQuality: String,
    val videoCodec: String?,
    val videoBitrate: Int,
    val audioQuality: String?,
    val audioCodec: String?,
    val audioBitrate: Int?,
    val isCombined: Boolean
)