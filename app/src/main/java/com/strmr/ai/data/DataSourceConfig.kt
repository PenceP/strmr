package com.strmr.ai.data

/**
 * Generic data source configuration - now populated from JSON instead of hardcoded
 */
data class DataSourceConfig(
    val id: String,
    val title: String,
    val endpoint: String,
    val mediaType: MediaType,
    val cacheKey: String,
    val enabled: Boolean = true,
    val order: Int = 0
)

enum class MediaType {
    MOVIE, TV_SHOW
}