package com.strmr.ai.data

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.strmr.ai.data.models.*
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
        private const val PREFS_NAME = "scraper_encrypted_prefs"
        private const val KEY_DEBRID_PROVIDER = "debrid_provider"
        private const val KEY_DEBRID_API_KEY = "debrid_api_key"
        private const val KEY_PREFERRED_SCRAPER = "preferred_scraper"
        private const val KEY_QUALITY_PREFERENCE = "quality_preference"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Debrid Service Management
    fun saveDebridConfiguration(provider: String, apiKey: String) {
        encryptedPrefs.edit().apply {
            putString(KEY_DEBRID_PROVIDER, provider)
            putString(KEY_DEBRID_API_KEY, apiKey)
            apply()
        }
    }
    
    fun getDebridConfiguration(): DebridConfiguration? {
        val provider = encryptedPrefs.getString(KEY_DEBRID_PROVIDER, null) ?: return null
        val apiKey = encryptedPrefs.getString(KEY_DEBRID_API_KEY, null) ?: return null
        
        return DebridConfiguration(
            provider = when (provider) {
                "premiumize" -> DebridProvider("premiumize", "Premiumize", null)
                "realdebrid" -> DebridProvider("realdebrid", "Real-Debrid", null)
                "alldebrid" -> DebridProvider("alldebrid", "AllDebrid", null)
                else -> DebridProvider(provider, provider, null)
            },
            apiKey = apiKey,
            email = null,
            isAuthenticated = true
        )
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
    
    // Stream Fetching
    suspend fun getStreamsForMedia(
        imdbId: String,
        mediaType: String,
        season: Int? = null,
        episode: Int? = null
    ): Flow<List<Stream>> = flow {
        val debridConfig = getDebridConfiguration()
        val preferredScraper = encryptedPrefs.getString(KEY_PREFERRED_SCRAPER, "torrentio") ?: "torrentio"
        
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
                "torrentio" -> fetchTorrentioStreams(scraperId, mediaType, debridConfig)
                "comet" -> fetchCometStreams(scraperId, mediaType, debridConfig)
                else -> fetchTorrentioStreams(scraperId, mediaType, debridConfig)
            }
            
            if (streams.isNotEmpty()) {
                emit(filterAndSortStreams(streams))
            } else {
                // Fallback to secondary scraper
                Log.d(TAG, "Primary scraper returned no results, trying fallback")
                val fallbackStreams = when (preferredScraper) {
                    "torrentio" -> fetchCometStreams(scraperId, mediaType, debridConfig)
                    else -> fetchTorrentioStreams(scraperId, mediaType, debridConfig)
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
        debridConfig: DebridConfiguration?
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val config = if (debridConfig != null) {
                buildTorrentioConfig(debridConfig)
            } else {
                null
            }
            
            val response = if (config != null) {
                torrentioApi.getStreams(config, type, id)
            } else {
                torrentioApi.getDefaultStreams(type, id)
            }
            
            Log.d(TAG, "Torrentio returned ${response.streams.size} streams")
            
            // If using debrid, check cache availability
            if (debridConfig?.provider?.id == "premiumize") {
                checkPremiumizeCache(response.streams, debridConfig.apiKey ?: "")
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
        debridConfig: DebridConfiguration?
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            if (debridConfig == null) {
                Log.w(TAG, "Comet requires debrid configuration")
                return@withContext emptyList<Stream>()
            }
            
            val config = buildCometConfig(debridConfig)
            val response = cometApi.getStreams(config, type, id)
            
            Log.d(TAG, "Comet returned ${response.streams.size} streams")
            response.streams
        } catch (e: Exception) {
            Log.e(TAG, "Comet API error", e)
            emptyList()
        }
    }
    
    private suspend fun checkPremiumizeCache(
        streams: List<Stream>,
        apiKey: String
    ): List<Stream> = withContext(Dispatchers.IO) {
        try {
            val hashes = streams.mapNotNull { it.infoHash }.distinct()
            if (hashes.isEmpty()) return@withContext streams
            
            val cacheResult = premiumizeApi.checkCache("Bearer $apiKey", hashes)
            
            // Mark cached streams
            streams.map { stream ->
                val index = hashes.indexOf(stream.infoHash)
                if (index >= 0 && index < cacheResult.response.size && cacheResult.response[index]) {
                    stream.copy(
                        behaviorHints = stream.behaviorHints?.copy(
                            proxyHeaders = mapOf("X-Cached" to "true")
                        )
                    )
                } else {
                    stream
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Premiumize cache", e)
            streams
        }
    }
    
    private fun filterAndSortStreams(streams: List<Stream>): List<Stream> {
        val qualityPref = encryptedPrefs.getString(KEY_QUALITY_PREFERENCE, "1080p") ?: "1080p"
        
        return streams
            .filter { stream ->
                // Filter out CAM/TS releases
                val title = stream.title?.lowercase() ?: ""
                !title.contains("cam") && !title.contains("ts") && !title.contains("telesync")
            }
            .sortedWith(compareBy(
                // Prioritize cached streams
                { it.behaviorHints?.proxyHeaders?.get("X-Cached") != "true" },
                // Then by quality match
                { it.displayQuality != qualityPref },
                // Then by quality order
                { 
                    when (it.displayQuality) {
                        "4K" -> 0
                        "1080p" -> 1
                        "720p" -> 2
                        "480p" -> 3
                        else -> 4
                    }
                },
                // Then by seeders
                { -(it.seeders ?: 0) }
            ))
    }
    
    private fun buildTorrentioConfig(debridConfig: DebridConfiguration): String {
        // Build the Torrentio configuration string that includes the Premiumize API key
        // Format: providers=yts+eztv+rarbg|debridservice=premiumize|apikey=YOUR_API_KEY
        val parts = mutableListOf<String>()
        
        // Default providers
        parts.add("providers=yts+eztv+rarbg+1337x+thepiratebay")
        
        // Add debrid service
        parts.add("debridservice=${debridConfig.provider.id}")
        
        // Add API key - this is critical for Premiumize to work
        debridConfig.apiKey?.let {
            parts.add("apikey=$it")
        }
        
        // Add quality filter
        val qualityPref = getQualityPreference()
        parts.add("qualityfilter=$qualityPref")
        
        // Sort by quality
        parts.add("sort=quality")
        
        Log.d(TAG, "Built Torrentio config: ${parts.joinToString("|")}")
        return parts.joinToString("|")
    }
    
    private fun buildCometConfig(debridConfig: DebridConfiguration): String {
        // Comet uses a different config format
        return "${debridConfig.provider.id}=${debridConfig.apiKey}"
    }
    
    // User Preferences
    fun setPreferredScraper(scraper: String) {
        encryptedPrefs.edit().putString(KEY_PREFERRED_SCRAPER, scraper).apply()
    }
    
    fun setQualityPreference(quality: String) {
        encryptedPrefs.edit().putString(KEY_QUALITY_PREFERENCE, quality).apply()
    }
    
    fun getQualityPreference(): String {
        return encryptedPrefs.getString(KEY_QUALITY_PREFERENCE, "1080p") ?: "1080p"
    }
}