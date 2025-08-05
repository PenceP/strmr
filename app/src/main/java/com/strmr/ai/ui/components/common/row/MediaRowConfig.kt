package com.strmr.ai.ui.components.common.row

import androidx.paging.PagingSource
import com.strmr.ai.ui.components.common.events.EventHandler
import com.strmr.ai.ui.components.common.events.MediaType
import com.strmr.ai.data.CollectionMovie

/**
 * Configuration class for MediaRow components using DpadRecyclerView.
 * 
 * This class defines all the properties and behavior for a row of media content,
 * supporting both static lists and paged data sources.
 * 
 * @param T The type of media item (e.g., MovieEntity, TvShowEntity, etc.)
 */
data class MediaRowConfig<T : Any>(
    /**
     * Display title for the row
     */
    val title: String,
    
    /**
     * Static list of items to display (use for small, known datasets)
     */
    val items: List<T> = emptyList(),
    
    /**
     * Paging source for infinite scroll (use for large datasets)
     * When provided, takes precedence over static items
     */
    val pagingSource: PagingSource<Int, T>? = null,
    
    /**
     * Visual style for cards in this row
     */
    val cardType: CardType = CardType.POSTER,
    
    /**
     * Unique key for focus memory restoration
     * Should be consistent across app sessions
     */
    val focusMemoryKey: String,
    
    /**
     * Event handler for user interactions
     */
    val eventHandler: EventHandler,
    
    /**
     * Media type for analytics and navigation
     */
    val mediaType: MediaType,
    
    /**
     * Maximum number of items to show before "View All"
     * Set to null for unlimited display
     */
    val maxItemsToShow: Int? = null,
    
    /**
     * Enable long press interactions
     */
    val enableLongPress: Boolean = true,
    
    /**
     * Long press timeout in milliseconds
     */
    val longPressTimeout: Long = 500L,
    
    /**
     * Enable fast scrolling optimizations
     */
    val enableFastScrolling: Boolean = true,
    
    /**
     * Show placeholder cards during super speed scrolling
     */
    val showPlaceholdersDuringScroll: Boolean = true,
    
    /**
     * Custom item mapping function for data transformation
     */
    val itemMapper: ((T) -> MediaRowItem)? = null,
    
    /**
     * Custom content provider for dynamic content loading
     */
    val contentProvider: MediaRowContentProvider<T>? = null,
    
    /**
     * Analytics tracking configuration
     */
    val analyticsConfig: AnalyticsConfig? = null
)

/**
 * Visual card types for different media content
 */
enum class CardType {
    /**
     * Standard movie/show poster (2:3 aspect ratio)
     */
    POSTER,
    
    /**
     * Landscape/backdrop image (16:9 aspect ratio)
     */
    LANDSCAPE,
    
    /**
     * Square format (1:1 aspect ratio)
     */
    SQUARE,
    
    /**
     * Circular format for person/profile images
     */
    CIRCLE,
    
    /**
     * Compact card for dense layouts
     */
    COMPACT,
    
    /**
     * Hero card for featured content (larger size)
     */
    HERO
}

/**
 * Standard media row item interface
 */
interface MediaRowItem {
    val id: String
    val title: String
    val imageUrl: String?
    val mediaType: MediaType
    
    /**
     * Additional metadata for display
     */
    val subtitle: String? get() = null
    val year: Int? get() = null
    val rating: Float? get() = null
    val progress: Float? get() = null // For continue watching
    val isWatched: Boolean get() = false
    val isFavorite: Boolean get() = false
}

/**
 * Content provider interface for dynamic content loading
 */
interface MediaRowContentProvider<T : Any> {
    /**
     * Load initial content for the row
     */
    suspend fun loadInitialContent(): List<T>
    
    /**
     * Load more content for pagination
     */
    suspend fun loadMoreContent(offset: Int, limit: Int): List<T>
    
    /**
     * Refresh the content (pull-to-refresh)
     */
    suspend fun refreshContent(): List<T>
    
    /**
     * Get the total count if known, or null for unknown
     */
    suspend fun getTotalCount(): Int?
}

/**
 * Analytics configuration for tracking user interactions
 */
data class AnalyticsConfig(
    /**
     * Category for analytics events
     */
    val category: String,
    
    /**
     * Track item clicks
     */
    val trackClicks: Boolean = true,
    
    /**
     * Track item focus events
     */
    val trackFocus: Boolean = false,
    
    /**
     * Track scroll events
     */
    val trackScrolling: Boolean = false,
    
    /**
     * Custom properties to include with events
     */
    val customProperties: Map<String, Any> = emptyMap()
)

/**
 * Factory functions for common MediaRowConfig configurations
 */
object MediaRowConfigs {
    
    /**
     * Standard movie poster row configuration
     */
    fun <T : Any> movieRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: EventHandler
    ): MediaRowConfig<T> = MediaRowConfig(
        title = title,
        items = items,
        cardType = CardType.POSTER,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = MediaType.MOVIE,
        analyticsConfig = AnalyticsConfig(category = "movie_row")
    )
    
    /**
     * TV show poster row configuration
     */
    fun <T : Any> tvShowRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: EventHandler
    ): MediaRowConfig<T> = MediaRowConfig(
        title = title,
        items = items,
        cardType = CardType.POSTER,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = MediaType.TV_SHOW,
        analyticsConfig = AnalyticsConfig(category = "tv_show_row")
    )
    
    /**
     * Continue watching row with progress indicators
     */
    fun <T : Any> continueWatchingRow(
        items: List<T>,
        focusKey: String,
        eventHandler: EventHandler
    ): MediaRowConfig<T> = MediaRowConfig(
        title = "Continue Watching",
        items = items,
        cardType = CardType.LANDSCAPE,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = MediaType.MOVIE, // Mixed content
        enableLongPress = true,
        analyticsConfig = AnalyticsConfig(
            category = "continue_watching",
            trackFocus = true
        )
    )
    
    /**
     * Collection row configuration
     */
    fun <T : Any> collectionRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: EventHandler,
        maxItems: Int = 10
    ): MediaRowConfig<T> = MediaRowConfig(
        title = title,
        items = items,
        cardType = CardType.POSTER,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = MediaType.COLLECTION,
        maxItemsToShow = maxItems,
        analyticsConfig = AnalyticsConfig(category = "collection_row")
    )
    
    /**
     * Paged row configuration for large datasets
     */
    fun <T : Any> pagedRow(
        title: String,
        pagingSource: PagingSource<Int, T>,
        cardType: CardType,
        mediaType: MediaType,
        focusKey: String,
        eventHandler: EventHandler
    ): MediaRowConfig<T> = MediaRowConfig(
        title = title,
        pagingSource = pagingSource,
        cardType = cardType,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = mediaType,
        enableFastScrolling = true,
        showPlaceholdersDuringScroll = true,
        analyticsConfig = AnalyticsConfig(
            category = "${mediaType.name.lowercase()}_paged_row",
            trackScrolling = true
        )
    )
    
    /**
     * CollectionMovie-specific row configuration
     */
    fun collectionMovieRow(
        items: List<CollectionMovie>,
        focusKey: String,
        eventHandler: EventHandler
    ): MediaRowConfig<CollectionMovie> = MediaRowConfig(
        title = "Part of Collection",
        items = items.take(10),
        cardType = CardType.POSTER,
        focusMemoryKey = focusKey,
        eventHandler = eventHandler,
        mediaType = MediaType.MOVIE,
        itemMapper = { movie -> CollectionMovieMediaRowItem(movie) },
        analyticsConfig = AnalyticsConfig(category = "collection_movie_row")
    )
}

/**
 * Adapter to convert CollectionMovie to MediaRowItem
 */
data class CollectionMovieMediaRowItem(
    private val movie: CollectionMovie
) : MediaRowItem {
    override val id: String = movie.id.toString()
    override val title: String = movie.title
    override val imageUrl: String? = movie.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" }
    override val mediaType: MediaType = MediaType.MOVIE
    override val subtitle: String? = movie.release_date?.take(4) // Extract year
    override val year: Int? = movie.release_date?.take(4)?.toIntOrNull()
    override val rating: Float? = movie.vote_average.toFloat()
    override val progress: Float? = null
    override val isWatched: Boolean = false
    override val isFavorite: Boolean = false
}