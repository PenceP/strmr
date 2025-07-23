package com.strmr.ai.data.models

import com.google.gson.annotations.SerializedName

// Scraper Configuration Models
data class ScraperManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val catalogs: List<ScraperCatalog>,
    val resources: List<ScraperResource>,
    val types: List<String>,
    val background: String?,
    val logo: String?,
    val behaviorHints: BehaviorHints
)

data class ScraperCatalog(
    val id: String,
    val name: String,
    val type: String,
    val extra: List<CatalogExtra>?
)

data class CatalogExtra(
    val name: String
)

data class ScraperResource(
    val name: String,
    val types: List<String>,
    val idPrefixes: List<String>
)

data class BehaviorHints(
    val configurable: Boolean,
    val configurationRequired: Boolean
)

// Stream Response Models
data class StreamResponse(
    val streams: List<Stream>
)

data class Stream(
    val name: String?,
    val title: String?,
    val infoHash: String?,
    val fileIdx: Int?,
    val url: String?,
    val behaviorHints: StreamBehaviorHints?,
    @SerializedName("sources")
    val sources: List<String>?,
    val debridService: String?,
    val quality: String?,
    val size: String?,
    val seeders: Int?,
    val peers: Int?,
    val uploadDate: String?
) {
    // Computed properties for UI display
    val displayQuality: String
        get() = when {
            title?.contains("4K", ignoreCase = true) == true -> "4K"
            title?.contains("2160p", ignoreCase = true) == true -> "4K"
            title?.contains("1080p", ignoreCase = true) == true -> "1080p"
            title?.contains("720p", ignoreCase = true) == true -> "720p"
            title?.contains("480p", ignoreCase = true) == true -> "480p"
            else -> "SD"
        }
    
    val displaySize: String
        get() = size ?: title?.let { extractSize(it) } ?: "Unknown"
    
    val displayName: String
        get() {
            // Prefer title (actual torrent name) over generic name
            val baseName = title ?: name ?: "Unknown Source"
            
            // If we have Premiumize processing indicators, keep them
            return if (behaviorHints?.proxyHeaders?.containsKey("X-Cached") == true) {
                val prefix = "[PM+]"
                if (baseName.startsWith(prefix)) baseName else "$prefix $baseName"
            } else {
                baseName
            }
        }
    
    private fun extractSize(title: String): String {
        val sizeRegex = """(\d+\.?\d*)\s*(GB|MB)""".toRegex(RegexOption.IGNORE_CASE)
        return sizeRegex.find(title)?.value ?: "Unknown"
    }
}

data class StreamBehaviorHints(
    val bingeGroup: String?,
    val proxyHeaders: Map<String, String>?
)

// Debrid Service Models
data class DebridProvider(
    val id: String,
    val name: String,
    val icon: String?,
    val supported: Boolean = true
)

data class DebridConfiguration(
    val provider: DebridProvider,
    val apiKey: String?,
    val email: String?,
    val isAuthenticated: Boolean = false
)

// Premiumize Specific Models
data class PremiumizeUser(
    @SerializedName("customer_id")
    val customerId: String,
    val email: String,
    @SerializedName("premium_until")
    val premiumUntil: Long,
    @SerializedName("space_used")
    val spaceUsed: Long,
    @SerializedName("limit_used")
    val limitUsed: Long
)

data class PremiumizeTransfer(
    val id: String,
    val name: String,
    val status: String,
    val progress: Float,
    @SerializedName("file_id")
    val fileId: String?
)

data class PremiumizeCache(
    val response: List<Boolean>,
    val transcoded: List<Boolean>,
    val filename: List<String>,
    val filesize: List<Long>
)

// Configuration Builder for Torrentio
data class TorrentioConfig(
    val providers: List<String> = listOf("yts", "eztv", "rarbg", "1337x", "thepiratebay"),
    val qualityFilter: List<String> = listOf("4k", "1080p", "720p"),
    val debridProvider: String? = null,
    val debridApiKey: String? = null,
    val sortBy: String = "quality",
    val minSize: String? = null,
    val maxSize: String? = null,
    val excludeKeywords: List<String> = emptyList()
) {
    fun toConfigString(): String {
        val parts = mutableListOf<String>()
        
        if (providers.isNotEmpty()) {
            parts.add("providers=${providers.joinToString("+")}")
        }
        
        if (qualityFilter.isNotEmpty()) {
            parts.add("qualityfilter=${qualityFilter.joinToString("|")}")
        }
        
        debridProvider?.let { parts.add("debridservice=$it") }
        debridApiKey?.let { parts.add("apikey=$it") }
        
        parts.add("sort=$sortBy")
        
        return parts.joinToString("|")
    }
}

// Comet Configuration
data class CometConfig(
    val debridService: String,
    val debridApiKey: String,
    val indexers: List<String> = listOf("bitsearch", "eztv", "thepiratebay", "therarbg", "yts"),
    val maxResults: Int = 50,
    val resultFormat: String = "simple",
    val torrentAddedDate: Boolean = true,
    val torrentSortBySeeders: Boolean = true,
    val removeTrash: Boolean = true
)