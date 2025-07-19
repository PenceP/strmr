package com.strmr.ai.data.youtube

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube InnerTube API client for extracting video information
 * Based on SmartTube's approach to YouTube data extraction
 */
@Singleton
class YouTubeInnerTubeClient @Inject constructor(
    private val playerConfig: YouTubePlayerConfig
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val jsonParser = JsonParser()
    
    companion object {
        private const val INNERTUBE_API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
        private const val INNERTUBE_PLAYER_ENDPOINT = "https://www.youtube.com/youtubei/v1/player"
        
        // Client configurations for different YouTube clients
        // Based on SmartTube and yt-dlp's working configurations
        private const val CLIENT_NAME_ANDROID = "ANDROID"
        private const val CLIENT_VERSION_ANDROID = "20.10.38" // Updated version that works
        private const val CLIENT_NAME_ANDROID_VR = "ANDROID_VR"
        private const val CLIENT_VERSION_ANDROID_VR = "1.62.27"
        private const val CLIENT_NAME_IOS = "IOS"
        private const val CLIENT_VERSION_IOS = "20.10.2"
        private const val CLIENT_NAME_WEB = "WEB"
        private const val CLIENT_VERSION_WEB = "2.20240304.00.00"
        private const val CLIENT_NAME_ANDROID_EMBEDDED = "ANDROID_EMBEDDED_PLAYER"
        private const val CLIENT_VERSION_ANDROID_EMBEDDED = "20.10.38"
        
        private const val USER_AGENT_ANDROID = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"
        private const val USER_AGENT_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus/1.62.27 (Linux; U; Android 14; Quest 3 Build/UQ3A.231001.001) gzip"
        private const val USER_AGENT_IOS = "com.google.ios.youtube/20.10.2 (iPhone16,1; U; CPU iOS 17_5_1 like Mac OS X)"
        private const val USER_AGENT_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }
    
    /**
     * Extract player response using InnerTube API
     */
    suspend fun getPlayerResponse(videoId: String): PlayerResponse? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("InnerTubeClient", "🔍 Getting player response for video: $videoId")
            
            // Try different client types until one works
            // Order based on reliability from SmartTube/yt-dlp research
            val clientConfigs = listOf(
                ClientConfig(CLIENT_NAME_ANDROID, CLIENT_VERSION_ANDROID, USER_AGENT_ANDROID),
                ClientConfig(CLIENT_NAME_ANDROID_VR, CLIENT_VERSION_ANDROID_VR, USER_AGENT_ANDROID_VR),
                ClientConfig(CLIENT_NAME_IOS, CLIENT_VERSION_IOS, USER_AGENT_IOS),
                ClientConfig(CLIENT_NAME_ANDROID_EMBEDDED, CLIENT_VERSION_ANDROID_EMBEDDED, USER_AGENT_ANDROID),
                ClientConfig(CLIENT_NAME_WEB, CLIENT_VERSION_WEB, USER_AGENT_WEB)
            )
            
            for (config in clientConfigs) {
                val response = fetchPlayerResponse(videoId, config)
                if (response != null) {
                    Log.d("InnerTubeClient", "✅ Successfully got player response using ${config.clientName}")
                    return@withContext response
                }
            }
            
            Log.w("InnerTubeClient", "❌ All client configurations failed")
            null
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "❌ Error getting player response", e)
            null
        }
    }
    
    private suspend fun fetchPlayerResponse(videoId: String, clientConfig: ClientConfig): PlayerResponse? {
        return try {
            val requestBody = buildInnerTubeRequest(videoId, clientConfig)
            val url = "$INNERTUBE_PLAYER_ENDPOINT?key=$INNERTUBE_API_KEY"
            
            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("User-Agent", clientConfig.userAgent)
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Origin", "https://www.youtube.com")
                .header("Referer", "https://www.youtube.com/watch?v=$videoId")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("InnerTubeClient", "❌ Request failed with ${clientConfig.clientName}: ${response.code}")
                return null
            }
            
            val responseBody = response.body?.string() ?: return null
            val jsonResponse = jsonParser.parse(responseBody).asJsonObject
            
            parsePlayerResponse(jsonResponse)
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "❌ Error fetching with ${clientConfig.clientName}", e)
            null
        }
    }
    
    private fun buildInnerTubeRequest(videoId: String, clientConfig: ClientConfig): String {
        val context = JsonObject().apply {
            add("client", JsonObject().apply {
                addProperty("clientName", clientConfig.clientName)
                addProperty("clientVersion", clientConfig.clientVersion)
                addProperty("hl", "en")
                addProperty("gl", "US")
                addProperty("utcOffsetMinutes", 0)
                
                when (clientConfig.clientName) {
                    CLIENT_NAME_ANDROID -> {
                        addProperty("androidSdkVersion", 34)
                        addProperty("platform", "MOBILE")
                        addProperty("osName", "Android")
                        addProperty("osVersion", "14")
                    }
                    CLIENT_NAME_ANDROID_VR -> {
                        addProperty("androidSdkVersion", 34)
                        addProperty("platform", "MOBILE")
                        addProperty("deviceMake", "Oculus")
                        addProperty("deviceModel", "Quest 3")
                        addProperty("osName", "Android")
                        addProperty("osVersion", "14")
                    }
                    CLIENT_NAME_IOS -> {
                        addProperty("platform", "MOBILE")
                        addProperty("deviceMake", "Apple")
                        addProperty("deviceModel", "iPhone16,1")
                        addProperty("osName", "iPhone")
                        addProperty("osVersion", "17.5.1")
                    }
                    CLIENT_NAME_ANDROID_EMBEDDED -> {
                        addProperty("androidSdkVersion", 34)
                        addProperty("platform", "MOBILE")
                        addProperty("clientScreen", "EMBED")
                    }
                    CLIENT_NAME_WEB -> {
                        addProperty("platform", "DESKTOP")
                        addProperty("browserName", "Chrome")
                        addProperty("browserVersion", "122.0.0.0")
                    }
                }
            })
            
            add("user", JsonObject().apply {
                addProperty("lockedSafetyMode", false)
            })
            
            add("request", JsonObject().apply {
                addProperty("useSsl", true)
                add("internalExperimentFlags", JsonObject())
            })
        }
        
        val requestJson = JsonObject().apply {
            add("context", context)
            addProperty("videoId", videoId)
            addProperty("racyCheckOk", true)
            addProperty("contentCheckOk", true)
            
            // Additional parameters used by SmartTube/yt-dlp
            add("playbackContext", JsonObject().apply {
                add("contentPlaybackContext", JsonObject().apply {
                    addProperty("html5Preference", "HTML5_PREF_WANTS")
                    addProperty("lactMilliseconds", "-1")
                    addProperty("referer", "https://www.youtube.com/watch?v=$videoId")
                    // Use dynamic STS from player config if available
                    val sts = runBlocking { playerConfig.getSignatureTimestamp() }
                    addProperty("signatureTimestamp", sts?.toIntOrNull() ?: 20554)
                    addProperty("autoCaptionsDefaultOn", false)
                    addProperty("autoplay", true)
                })
            })
            
            // For some clients, this helps bypass restrictions
            if (clientConfig.clientName == CLIENT_NAME_ANDROID || 
                clientConfig.clientName == CLIENT_NAME_ANDROID_VR ||
                clientConfig.clientName == CLIENT_NAME_IOS) {
                add("attestationRequest", JsonObject().apply {
                    addProperty("omitBotguardData", true)
                })
            }
            
            // Add service integration for better URL quality
            add("serviceIntegrityDimensions", JsonObject().apply {
                addProperty("poToken", "null")
            })
        }
        
        return gson.toJson(requestJson)
    }
    
    private fun parsePlayerResponse(jsonResponse: JsonObject): PlayerResponse? {
        return try {
            val playabilityStatus = jsonResponse.getAsJsonObject("playabilityStatus")
            val status = playabilityStatus?.get("status")?.asString
            
            if (status != "OK") {
                Log.w("InnerTubeClient", "❌ Playability status: $status")
                val reason = playabilityStatus?.get("reason")?.asString
                Log.w("InnerTubeClient", "❌ Reason: $reason")
                return null
            }
            
            val streamingData = jsonResponse.getAsJsonObject("streamingData")
            if (streamingData == null) {
                Log.w("InnerTubeClient", "❌ No streaming data found")
                return null
            }
            
            val videoDetails = jsonResponse.getAsJsonObject("videoDetails")
            
            PlayerResponse(
                streamingData = parseStreamingData(streamingData),
                videoDetails = parseVideoDetails(videoDetails)
            )
        } catch (e: Exception) {
            Log.e("InnerTubeClient", "❌ Error parsing player response", e)
            null
        }
    }
    
    private fun parseStreamingData(streamingData: JsonObject): StreamingData {
        val adaptiveFormats = mutableListOf<Format>()
        val formats = mutableListOf<Format>()
        
        // Parse adaptive formats (separate video/audio)
        streamingData.getAsJsonArray("adaptiveFormats")?.forEach { element ->
            val formatObj = element.asJsonObject
            adaptiveFormats.add(parseFormat(formatObj))
        }
        
        // Parse regular formats (combined video+audio)
        streamingData.getAsJsonArray("formats")?.forEach { element ->
            val formatObj = element.asJsonObject
            formats.add(parseFormat(formatObj))
        }
        
        return StreamingData(
            adaptiveFormats = adaptiveFormats,
            formats = formats,
            expiresInSeconds = streamingData.get("expiresInSeconds")?.asString,
            hlsManifestUrl = streamingData.get("hlsManifestUrl")?.asString,
            dashManifestUrl = streamingData.get("dashManifestUrl")?.asString
        )
    }
    
    private fun parseFormat(formatObj: JsonObject): Format {
        return Format(
            itag = formatObj.get("itag")?.asInt ?: 0,
            url = formatObj.get("url")?.asString,
            signatureCipher = formatObj.get("signatureCipher")?.asString,
            mimeType = formatObj.get("mimeType")?.asString ?: "",
            bitrate = formatObj.get("bitrate")?.asInt ?: 0,
            width = formatObj.get("width")?.asInt,
            height = formatObj.get("height")?.asInt,
            lastModified = formatObj.get("lastModified")?.asString,
            contentLength = formatObj.get("contentLength")?.asString,
            quality = formatObj.get("quality")?.asString,
            qualityLabel = formatObj.get("qualityLabel")?.asString,
            projectionType = formatObj.get("projectionType")?.asString ?: "RECTANGULAR",
            averageBitrate = formatObj.get("averageBitrate")?.asInt,
            audioQuality = formatObj.get("audioQuality")?.asString,
            approxDurationMs = formatObj.get("approxDurationMs")?.asString,
            audioSampleRate = formatObj.get("audioSampleRate")?.asString,
            audioChannels = formatObj.get("audioChannels")?.asInt
        )
    }
    
    private fun parseVideoDetails(videoDetails: JsonObject?): VideoDetails? {
        if (videoDetails == null) return null
        
        return VideoDetails(
            videoId = videoDetails.get("videoId")?.asString ?: "",
            title = videoDetails.get("title")?.asString ?: "",
            lengthSeconds = videoDetails.get("lengthSeconds")?.asString,
            channelId = videoDetails.get("channelId")?.asString,
            shortDescription = videoDetails.get("shortDescription")?.asString,
            thumbnail = null // TODO: Parse thumbnail data if needed
        )
    }
    
    private data class ClientConfig(
        val clientName: String,
        val clientVersion: String,
        val userAgent: String
    )
}

/**
 * Data classes for YouTube player response
 */
data class PlayerResponse(
    val streamingData: StreamingData,
    val videoDetails: VideoDetails?
)

data class StreamingData(
    val adaptiveFormats: List<Format>,
    val formats: List<Format>,
    val expiresInSeconds: String?,
    val hlsManifestUrl: String?,
    val dashManifestUrl: String?
)

data class Format(
    val itag: Int,
    val url: String?,
    val signatureCipher: String?,
    val mimeType: String,
    val bitrate: Int,
    val width: Int?,
    val height: Int?,
    val lastModified: String?,
    val contentLength: String?,
    val quality: String?,
    val qualityLabel: String?,
    val projectionType: String,
    val averageBitrate: Int?,
    val audioQuality: String?,
    val approxDurationMs: String?,
    val audioSampleRate: String?,
    val audioChannels: Int?
)

data class VideoDetails(
    val videoId: String,
    val title: String,
    val lengthSeconds: String?,
    val channelId: String?,
    val shortDescription: String?,
    val thumbnail: Any? // TODO: Define thumbnail structure if needed
)