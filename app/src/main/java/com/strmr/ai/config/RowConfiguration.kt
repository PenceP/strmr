package com.strmr.ai.config

import com.google.gson.annotations.SerializedName

/**
 * Configuration for page rows loaded from JSON files
 */
data class PageConfiguration(
    @SerializedName("version")
    val version: String,
    @SerializedName("rows")
    val rows: List<RowConfig>,
    @SerializedName("collections")
    val collections: List<CollectionConfig>? = null,
    @SerializedName("networks")
    val networks: List<NetworkConfig>? = null
)

/**
 * Configuration for a single row
 */
data class RowConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("dataSource")
    val dataSource: String,
    @SerializedName("cardType")
    val cardType: String,
    @SerializedName("cardHeight")
    val cardHeight: Int,
    @SerializedName("showHero")
    val showHero: Boolean,
    @SerializedName("showLoading")
    val showLoading: Boolean,
    @SerializedName("order")
    val order: Int,
    @SerializedName("enabled")
    val enabled: Boolean,
    @SerializedName("displayOptions")
    val displayOptions: DisplayOptions,
    @SerializedName("traktConfig")
    val traktConfig: TraktConfig? = null,
    @SerializedName("nestedRows")
    val nestedRows: List<RowConfig>? = null,
    @SerializedName("nestedItems")
    val nestedItems: List<NestedItemConfig>? = null
)

/**
 * Display options for a row
 */
data class DisplayOptions(
    @SerializedName("showOverlays")
    val showOverlays: Boolean,
    @SerializedName("showProgress")
    val showProgress: Boolean,
    @SerializedName("showEpisodeInfo")
    val showEpisodeInfo: Boolean? = null,
    @SerializedName("clickable")
    val clickable: Boolean,
    @SerializedName("supportedTypes")
    val supportedTypes: List<String>
)

/**
 * Configuration for a collection item
 */
data class CollectionConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("backgroundImageUrl")
    val backgroundImageUrl: String,
    @SerializedName("nameDisplayMode")
    val nameDisplayMode: String
)

/**
 * Configuration for Trakt list rows
 */
data class TraktConfig(
    @SerializedName("username")
    val username: String,
    @SerializedName("listSlug")
    val listSlug: String,
    @SerializedName("sortBy")
    val sortBy: String,
    @SerializedName("sortOrder")
    val sortOrder: String,
    @SerializedName("apiEndpoint")
    val apiEndpoint: String
)

/**
 * Configuration for a network item
 */
data class NetworkConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("backgroundImageUrl")
    val backgroundImageUrl: String,
    @SerializedName("nameDisplayMode")
    val nameDisplayMode: String,
    @SerializedName("dataUrl")
    val dataUrl: String?
)

/**
 * Configuration for a nested item within a row
 */
data class NestedItemConfig(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("backgroundImageUrl")
    val backgroundImageUrl: String,
    @SerializedName("nameDisplayMode")
    val nameDisplayMode: String,
    @SerializedName("dataUrl")
    val dataUrl: String,
    @SerializedName("type")
    val type: String // movies/shows
)


/**
 * Enum for row types
 */
enum class RowType {
    CONTINUE_WATCHING,
    NETWORKS,
    COLLECTIONS,
    PAGING,
    TRAKT_LIST;
    
    companion object {
        fun fromString(type: String): RowType {
            return when (type.lowercase()) {
                "continue_watching" -> CONTINUE_WATCHING
                "networks" -> NETWORKS
                "collections" -> COLLECTIONS
                "paging" -> PAGING
                "trakt_list" -> TRAKT_LIST
                else -> throw IllegalArgumentException("Unknown row type: $type")
            }
        }
    }
}

/**
 * Enum for card types
 */
enum class CardType {
    PORTRAIT,
    LANDSCAPE;
    
    companion object {
        fun fromString(type: String): CardType {
            return when (type.lowercase()) {
                "portrait" -> PORTRAIT
                "landscape" -> LANDSCAPE
                else -> throw IllegalArgumentException("Unknown card type: $type")
            }
        }
    }
}