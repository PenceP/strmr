package com.strmr.ai.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.strmr.ai.data.models.*
import com.strmr.ai.ui.theme.StrmrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScraperRepository @Inject constructor(
    private val context: Context,
    private val torrentioApi: TorrentioApiService,
    private val cometApi: CometApiService,
    private val premiumizeApi: PremiumizeApiService
) {
    companion object {
        private const val TAG = "ScraperRepository"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        StrmrConstants.Preferences.PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Premiumize API Key Management
    fun savePremiumizeApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(StrmrConstants.Preferences.KEY_PREMIUMIZE_API_KEY, apiKey).apply()
    }
    
    fun getPremiumizeApiKey(): String? {
        return encryptedPrefs.getString(StrmrConstants.Preferences.KEY_PREMIUMIZE_API_KEY, null)
    }
    
    fun clearPremiumizeApiKey() {
        encryptedPrefs.edit().remove(StrmrConstants.Preferences.KEY_PREMIUMIZE_API_KEY).apply()
    }
    
    fun isPremiumizeConfigured(): Boolean {
        return !getPremiumizeApiKey().isNullOrBlank()
    }
    
    suspend fun validatePremiumizeKey(apiKey: String): Boolean {
        return try {
            val user = premiumizeApi.getAccountInfo("Bearer $apiKey")
            Log.d(TAG, "Premiumize validation successful: ${user.email}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Premiumize validation failed", e)
            false
        }
    }
    
    // Simple stream fetching method for ViewModels
    suspend fun getStreams(
        imdbId: String,
        type: String,
        season: Int? = null,
        episode: Int? = null
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getPremiumizeApiKey()
            val preferredScraper = encryptedPrefs.getString(StrmrConstants.Preferences.KEY_PREFERRED_SCRAPER, StrmrConstants.Defaults.PREFERRED_SCRAPER) ?: StrmrConstants.Defaults.PREFERRED_SCRAPER
            
            // Build the ID string for the scraper
            val scraperId = when (type) {
                "movie" -> imdbId
                "series", "tvshow" -> {
                    if (season != null && episode != null) {
                        "$imdbId:$season:$episode"
                    } else {
                        imdbId
                    }
                }
                else -> imdbId
            }
            
            Log.d(TAG, "🔍 Fetching streams for: $scraperId (type: $type, scraper: $preferredScraper)")
            
            // Try primary scraper first
            val streams = when (preferredScraper) {
                StrmrConstants.Defaults.PREFERRED_SCRAPER -> fetchTorrentioStreams(scraperId, type, apiKey)
                "comet" -> fetchCometStreams(scraperId, type, apiKey)
                else -> fetchTorrentioStreams(scraperId, type, apiKey)
            }
            
            if (streams.isNotEmpty()) {
                Log.d(TAG, "✅ Primary scraper found ${streams.size} streams")
                filterAndSortStreams(streams)
            } else {
                // Fallback to secondary scraper
                Log.d(TAG, "⚠️ Primary scraper returned no results, trying fallback")
                val fallbackStreams = when (preferredScraper) {
                    StrmrConstants.Defaults.PREFERRED_SCRAPER -> fetchCometStreams(scraperId, type, apiKey)
                    else -> fetchTorrentioStreams(scraperId, type, apiKey)
                }
                Log.d(TAG, "📺 Fallback scraper found ${fallbackStreams.size} streams")
                filterAndSortStreams(fallbackStreams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching streams", e)
            emptyList()
        }
    }
    
    // Stream Fetching
    suspend fun getStreamsForMedia(
        imdbId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null
    ): Flow<List<Stream>> = flow {
        val apiKey = getPremiumizeApiKey()
        val preferredScraper = encryptedPrefs.getString(StrmrConstants.Preferences.KEY_PREFERRED_SCRAPER, StrmrConstants.Defaults.PREFERRED_SCRAPER) ?: StrmrConstants.Defaults.PREFERRED_SCRAPER
        
        // Build the ID string for the scraper
        val scraperId = when (mediaType) {
            "movie" -> imdbId
            "series", "tvshow" -> {
                if (season != null && episode != null) {
                    "$imdbId:$season:$episode"
                } else {
                    imdbId
                }
            }
            else -> imdbId
        }
        
        Log.d(TAG, "Fetching streams for: $scraperId (type: $mediaType)")
        
        // Try primary scraper first
        try {
            val streams = when (preferredScraper) {
                "torrentio" -> fetchTorrentioStreams(scraperId, mediaType, apiKey)
                "comet" -> fetchCometStreams(scraperId, mediaType, apiKey)
                else -> fetchTorrentioStreams(scraperId, mediaType, apiKey)
            }
            
            if (streams.isNotEmpty()) {
                emit(filterAndSortStreams(streams))
            } else {
                // Fallback to secondary scraper
                Log.d(TAG, "Primary scraper returned no results, trying fallback")
                val fallbackStreams = when (preferredScraper) {
                    "torrentio" -> fetchCometStreams(scraperId, mediaType, apiKey)
                    else -> fetchTorrentioStreams(scraperId, mediaType, apiKey)
                }
                emit(filterAndSortStreams(fallbackStreams))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching streams", e)
            emit(emptyList())
        }
    }
    
    private suspend fun fetchTorrentioStreams(
        id: String,
        type: String,
        apiKey: String?
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val config = if (!apiKey.isNullOrBlank()) {
                buildTorrentioConfig(apiKey)
            } else {
                null
            }
            
            val response = if (config != null) {
                torrentioApi.getStreams(config, type, id)
            } else {
                torrentioApi.getDefaultStreams(type, id)
            }
            
            Log.d(TAG, "Torrentio returned ${response.streams.size} streams")
            
            // Debug: Log the first few streams to understand the structure
            response.streams.take(3).forEachIndexed { index, stream ->
                Log.d(TAG, "🔍 Stream $index debug:")
                Log.d(TAG, "  - name: ${stream.name}")
                Log.d(TAG, "  - title: ${stream.title}")
                Log.d(TAG, "  - url: ${stream.url}")
                Log.d(TAG, "  - infoHash: ${stream.infoHash}")
                Log.d(TAG, "  - sources: ${stream.sources}")
            }
            
            // If using Premiumize, process streams to get direct links
            if (!apiKey.isNullOrBlank()) {
                processPremiumizeStreams(response.streams, apiKey)
            } else {
                response.streams
            }
        } catch (e: Exception) {
            Log.e(TAG, "Torrentio API error", e)
            emptyList()
        }
    }
    
    private suspend fun fetchCometStreams(
        id: String,
        type: String,
        apiKey: String?
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isNullOrBlank()) {
                Log.w(TAG, "Comet requires Premiumize API key")
                return@withContext emptyList<Stream>()
            }
            
            val config = buildCometConfig(apiKey)
            val response = cometApi.getStreams(config, type, id)
            
            Log.d(TAG, "Comet returned ${response.streams.size} streams")
            response.streams
        } catch (e: Exception) {
            Log.e(TAG, "Comet API error", e)
            emptyList()
        }
    }
    
    private suspend fun processPremiumizeStreams(
        streams: List<Stream>,
        apiKey: String
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Processing ${streams.size} streams through Premiumize")
            
            // Check for Torrentio's built-in cache indicators in stream names
            val processedStreams = streams.map { stream ->
                val streamName = stream.name ?: ""
                val isCachedByName = streamName.contains("[PM+]", ignoreCase = true)
                val isNotCachedByName = streamName.contains("[PM]", ignoreCase = true) && !streamName.contains("[PM+]", ignoreCase = true)
                
                when {
                    isCachedByName -> {
                        Log.d(TAG, "✅ Stream marked as cached by Torrentio: ${stream.name}")
                        stream.copy(
                            behaviorHints = stream.behaviorHints?.copy(
                                proxyHeaders = mapOf("X-Cached" to "true", "X-Direct" to "true")
                            ) ?: StreamBehaviorHints(null, mapOf("X-Cached" to "true", "X-Direct" to "true"))
                        )
                    }
                    isNotCachedByName -> {
                        Log.d(TAG, "⏳ Stream marked as not cached by Torrentio: ${stream.name}")
                        stream.copy(
                            behaviorHints = stream.behaviorHints?.copy(
                                proxyHeaders = mapOf("X-Cached" to "false")
                            ) ?: StreamBehaviorHints(null, mapOf("X-Cached" to "false"))
                        )
                    }
                    else -> {
                        // Fallback to hash-based checking if available
                        stream
                    }
                }
            }
            
            // For streams without cache indicators in name, try hash-based checking
            val streamsWithoutCacheInfo = processedStreams.filter { stream ->
                val streamName = stream.name ?: ""
                !streamName.contains("[PM+]", ignoreCase = true) && !streamName.contains("[PM]", ignoreCase = true) && stream.infoHash != null
            }
            
            if (streamsWithoutCacheInfo.isNotEmpty()) {
                val hashes = streamsWithoutCacheInfo.mapNotNull { it.infoHash }.distinct()
                if (hashes.isNotEmpty()) {
                    Log.d(TAG, "🔍 Hash-based cache check for ${hashes.size} streams without cache indicators")
                    
                    val cacheResult = premiumizeApi.checkCache("Bearer $apiKey", hashes)
                    Log.d(TAG, "📦 Cache check result: ${cacheResult.response.count { it }} cached out of ${hashes.size}")
                    
                    // Update streams based on cache results
                    return@withContext processedStreams.map { stream ->
                        if (streamsWithoutCacheInfo.contains(stream) && stream.infoHash != null) {
                            val hashIndex = hashes.indexOf(stream.infoHash)
                            val isCached = hashIndex >= 0 && hashIndex < cacheResult.response.size && cacheResult.response[hashIndex]
                            
                            stream.copy(
                                behaviorHints = stream.behaviorHints?.copy(
                                    proxyHeaders = mapOf("X-Cached" to isCached.toString())
                                ) ?: StreamBehaviorHints(null, mapOf("X-Cached" to isCached.toString()))
                            )
                        } else {
                            stream
                        }
                    }
                }
            }
            
            processedStreams
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing Premiumize streams", e)
            streams
        }
    }
    
    private fun filterAndSortStreams(streams: List<Stream>): List<Stream> {
        return streams
            .filter { stream ->
                // Filter out CAM/TS releases
                val title = stream.title?.lowercase() ?: ""
                !title.contains("cam") && !title.contains("ts") && !title.contains("telesync")
            }
            .sortedWith(compareBy(
                // 1. Prioritize streams with direct URLs (ready to play)
                { stream -> stream.url.isNullOrBlank() },
                // 2. Then prioritize cached streams
                { it.behaviorHints?.proxyHeaders?.get("X-Cached") != "true" },
                // 3. Sort by quality: 4K -> 1080p -> 720p -> 480p -> Unknown
                { 
                    when (it.displayQuality) {
                        "4K" -> 0
                        "1080p" -> 1
                        "720p" -> 2
                        "480p" -> 3
                        else -> 4
                    }
                },
                // 4. Within each quality, sort by file size (largest first)
                { -parseFileSizeInBytes(it.displaySize) }
            ))
    }
    
    private fun parseFileSizeInBytes(sizeString: String): Long {
        return try {
            val regex = """(\d+\.?\d*)\s*(GB|MB|KB|TB)""".toRegex(RegexOption.IGNORE_CASE)
            val matchResult = regex.find(sizeString)
            
            if (matchResult != null) {
                val size = matchResult.groupValues[1].toDoubleOrNull() ?: 0.0
                val unit = matchResult.groupValues[2].uppercase()
                
                when (unit) {
                    "TB" -> (size * 1024 * 1024 * 1024 * 1024).toLong()
                    "GB" -> (size * 1024 * 1024 * 1024).toLong()
                    "MB" -> (size * 1024 * 1024).toLong()
                    "KB" -> (size * 1024).toLong()
                    else -> 0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse file size: $sizeString", e)
            0L
        }
    }
    
    private fun buildTorrentioConfig(apiKey: String): String {
        // Build the Torrentio configuration string for Premiumize
        // Format: premiumize=YOUR_PREMIUMIZE_API_KEY (this gets embedded in the URL path)
        // Full URL example: https://torrentio.strem.fun/premiumize=apikey123/stream/movie/tt1234567.json
        
        val config = "premiumize=$apiKey"
        Log.d(TAG, "Built Torrentio config: $config")
        return config
    }
    
    private fun buildCometConfig(apiKey: String): String {
        // Comet uses a different config format
        val config = "premiumize=$apiKey"
        Log.d(TAG, "Built Comet config: $config")
        return config
    }
    
    // User Preferences
    fun setPreferredScraper(scraper: String) {
        encryptedPrefs.edit().putString(StrmrConstants.Preferences.KEY_PREFERRED_SCRAPER, scraper).apply()
    }
    
    fun setQualityPreference(quality: String) {
        encryptedPrefs.edit().putString(StrmrConstants.Preferences.KEY_QUALITY_PREFERENCE, quality).apply()
    }
    
    fun getQualityPreference(): String {
        return encryptedPrefs.getString(StrmrConstants.Preferences.KEY_QUALITY_PREFERENCE, StrmrConstants.Defaults.QUALITY_PREFERENCE) ?: StrmrConstants.Defaults.QUALITY_PREFERENCE
    }
}