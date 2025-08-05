package com.strmr.ai.ui.components.common.events

/**
 * Standard event handler interface for consistent event handling across components.
 * 
 * This interface provides a unified way to handle user interactions with media content,
 * ensuring consistent behavior and easy testing throughout the application.
 */
interface EventHandler {
    /**
     * Handles click/selection events on media items
     */
    fun onItemClick(itemId: Int, itemType: MediaType)
    
    /**
     * Handles focus events when items gain focus
     */
    fun onItemFocus(itemId: Int, itemType: MediaType)
    
    /**
     * Handles focus lost events when items lose focus
     */
    fun onItemFocusLost(itemId: Int, itemType: MediaType)
    
    /**
     * Handles long press events for context menus
     */
    fun onItemLongPress(itemId: Int, itemType: MediaType)
}

/**
 * Specialized event handler for row-based navigation
 */
interface RowEventHandler : EventHandler {
    /**
     * Handles events when a row gains focus
     */
    fun onRowFocus(rowId: String, rowType: RowType)
    
    /**
     * Handles events when a row loses focus
     */
    fun onRowFocusLost(rowId: String, rowType: RowType)
    
    /**
     * Handles scroll events within rows
     */
    fun onRowScroll(rowId: String, position: Int, totalItems: Int)
    
    /**
     * Handles events when reaching the end of a row (for pagination)
     */
    fun onRowEndReached(rowId: String, rowType: RowType)
}

/**
 * Event handler for navigation-specific events
 */
interface NavigationEventHandler {
    /**
     * Handles navigation to detail screens
     */
    fun navigateToDetails(mediaId: Int, mediaType: MediaType)
    
    /**
     * Handles navigation to collection screens
     */
    fun navigateToCollection(collectionId: Int, collectionType: CollectionType)
    
    /**
     * Handles back navigation events
     */
    fun navigateBack()
    
    /**
     * Handles navigation to search
     */
    fun navigateToSearch(query: String = "")
}

/**
 * Event handler for playback-related events
 */
interface PlaybackEventHandler {
    /**
     * Handles play/resume events
     */
    fun onPlay(mediaId: Int, mediaType: MediaType, resumePosition: Long = 0L)
    
    /**
     * Handles pause events
     */
    fun onPause(mediaId: Int, currentPosition: Long)
    
    /**
     * Handles stop events
     */
    fun onStop(mediaId: Int, currentPosition: Long)
    
    /**
     * Handles seek events
     */
    fun onSeek(mediaId: Int, position: Long)
    
    /**
     * Handles playback completion events
     */
    fun onPlaybackComplete(mediaId: Int, finalPosition: Long)
}

/**
 * Comprehensive event handler that combines all event types
 */
interface UnifiedEventHandler : 
    RowEventHandler, 
    NavigationEventHandler, 
    PlaybackEventHandler

/**
 * Enum representing different types of media content
 */
enum class MediaType {
    MOVIE,
    TV_SHOW,
    EPISODE,
    PERSON,
    COLLECTION
}

/**
 * Enum representing different types of rows
 */
enum class RowType {
    TRENDING,
    POPULAR,
    TOP_RATED,
    CONTINUE_WATCHING,
    WATCHLIST,
    SIMILAR,
    RECOMMENDATIONS,
    COLLECTION,
    SEARCH_RESULTS,
    CUSTOM
}

/**
 * Enum representing different types of collections
 */
enum class CollectionType {
    MOVIE_COLLECTION,
    TV_SERIES,
    WATCHLIST,
    FAVORITES,
    CUSTOM_LIST
}

/**
 * Default implementation that provides no-op implementations for optional events
 */
abstract class BaseEventHandler : UnifiedEventHandler {
    
    // Required implementations - must be overridden
    abstract override fun onItemClick(itemId: Int, itemType: MediaType)
    abstract override fun navigateToDetails(mediaId: Int, mediaType: MediaType)
    
    // Optional implementations with default no-op behavior
    override fun onItemFocus(itemId: Int, itemType: MediaType) {}
    override fun onItemFocusLost(itemId: Int, itemType: MediaType) {}
    override fun onItemLongPress(itemId: Int, itemType: MediaType) {}
    
    override fun onRowFocus(rowId: String, rowType: RowType) {}
    override fun onRowFocusLost(rowId: String, rowType: RowType) {}
    override fun onRowScroll(rowId: String, position: Int, totalItems: Int) {}
    override fun onRowEndReached(rowId: String, rowType: RowType) {}
    
    override fun navigateToCollection(collectionId: Int, collectionType: CollectionType) {}
    override fun navigateBack() {}
    override fun navigateToSearch(query: String) {}
    
    override fun onPlay(mediaId: Int, mediaType: MediaType, resumePosition: Long) {}
    override fun onPause(mediaId: Int, currentPosition: Long) {}
    override fun onStop(mediaId: Int, currentPosition: Long) {}
    override fun onSeek(mediaId: Int, position: Long) {}
    override fun onPlaybackComplete(mediaId: Int, finalPosition: Long) {}
}

/**
 * Event data class for passing event information
 */
data class MediaEvent(
    val itemId: Int,
    val itemType: MediaType,
    val timestamp: Long = System.currentTimeMillis(),
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Event data class for row events
 */
data class RowEvent(
    val rowId: String,
    val rowType: RowType,
    val position: Int = 0,
    val totalItems: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)