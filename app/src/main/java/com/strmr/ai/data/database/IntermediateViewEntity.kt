package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for caching intermediate view data (Networks, Collections, Directors, Trakt Lists)
 * Stores the association between views and their content items
 */
@Entity(tableName = "intermediate_views")
data class IntermediateViewEntity(
    @PrimaryKey val id: String, // Composite key: "viewType:itemId" e.g., "network:Netflix"
    val viewType: String, // "network", "collection", "director", "trakt_list"
    val itemId: String, // "Netflix", "marvel", "nolan", etc.
    val itemName: String, // Display name
    val itemBackgroundUrl: String?, // Background image URL
    val dataUrl: String?, // API URL for data source
    val lastUpdated: Long, // Timestamp for cache expiry
    val totalItems: Int = 0, // Total number of items available
    val page: Int = 1, // Current cached page
    val pageSize: Int = 20 // Items per page
)

/**
 * Entity for storing the actual content items in intermediate views
 * Links to existing MovieEntity/TvShowEntity via foreign keys
 */
@Entity(
    tableName = "intermediate_view_items",
    primaryKeys = ["intermediateViewId", "mediaType", "tmdbId"]
)
data class IntermediateViewItemEntity(
    val intermediateViewId: String, // Foreign key to IntermediateViewEntity.id
    val mediaType: String, // "movie" or "show"
    val tmdbId: Int, // Foreign key to MovieEntity.tmdbId or TvShowEntity.tmdbId
    val orderIndex: Int, // Position in the list (for maintaining order)
    val addedAt: Long = System.currentTimeMillis() // When this item was cached
)