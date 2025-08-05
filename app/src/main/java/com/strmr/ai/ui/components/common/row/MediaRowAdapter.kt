package com.strmr.ai.ui.components.common.row

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import com.strmr.ai.ui.components.common.events.NavigationThrottle
import com.strmr.ai.ui.components.common.focus.DpadFocusManager
import com.strmr.ai.ui.components.common.loading.PlaceholderCard

/**
 * RecyclerView Adapter for MediaRow using DpadRecyclerView with Compose integration.
 * 
 * This adapter handles:
 * - Focus management and memory restoration
 * - Navigation throttling for Android TV
 * - Placeholder cards during fast scrolling
 * - Event handling for clicks and long presses
 * - Integration with existing Compose UI components
 * 
 * @param T The type of media item
 */
class MediaRowAdapter<T : Any>(
    private val config: MediaRowConfig<T>,
    private val focusManager: DpadFocusManager
) : RecyclerView.Adapter<MediaRowAdapter<T>.MediaCardViewHolder>() {
    
    /**
     * Current list of items being displayed
     */
    private var currentItems: List<T> = config.items
    
    /**
     * Track fast scrolling state for placeholder optimization
     */
    private var isFastScrolling = false
    
    /**
     * Callback for notifying parent about data changes
     */
    var onDataChanged: ((Int) -> Unit)? = null
    
    /**
     * Update the adapter with new items
     */
    fun updateItems(newItems: List<T>) {
        val oldSize = currentItems.size
        currentItems = newItems
        
        // Notify changes efficiently
        when {
            oldSize == 0 && newItems.isNotEmpty() -> {
                notifyItemRangeInserted(0, newItems.size)
            }
            oldSize > 0 && newItems.isEmpty() -> {
                notifyItemRangeRemoved(0, oldSize)
            }
            else -> {
                notifyDataSetChanged() // TODO: Implement more efficient diff updates
            }
        }
        
        onDataChanged?.invoke(newItems.size)
    }
    
    /**
     * Set fast scrolling state for placeholder optimization
     */
    fun setFastScrolling(fastScrolling: Boolean) {
        if (isFastScrolling != fastScrolling) {
            isFastScrolling = fastScrolling
            // Notify visible items to update placeholder state
            notifyDataSetChanged()
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaCardViewHolder {
        val composeView = ComposeView(parent.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        
        return MediaCardViewHolder(composeView)
    }
    
    override fun onBindViewHolder(holder: MediaCardViewHolder, position: Int) {
        val item = currentItems.getOrNull(position)
        if (item != null) {
            holder.bind(item, position)
        }
    }
    
    override fun getItemCount(): Int = currentItems.size
    
    /**
     * ViewHolder that integrates Compose with DpadRecyclerView focus system
     */
    inner class MediaCardViewHolder(
        private val composeView: ComposeView
    ) : RecyclerView.ViewHolder(composeView) {
        
        fun bind(item: T, position: Int) {
            composeView.setContent {
                MediaCardContent(
                    item = item,
                    position = position,
                    config = config,
                    isFastScrolling = isFastScrolling,
                    onItemClick = { clickedItem ->
                        handleItemClick(clickedItem, position)
                    },
                    onItemLongPress = if (config.enableLongPress) { clickedItem ->
                        handleItemLongPress(clickedItem, position)
                    } else null,
                    onItemFocus = { focusedItem ->
                        handleItemFocus(focusedItem, position)
                    },
                    onItemFocusLost = { lostFocusItem ->
                        handleItemFocusLost(lostFocusItem, position)
                    }
                )
            }
        }
        
        /**
         * Handle item click with navigation throttling
         */
        private fun handleItemClick(item: T, position: Int) {
            if (NavigationThrottle.canNavigate()) {
                // Update focus memory before navigation
                focusManager.updateFocus(config.focusMemoryKey, position, getItemId(item))
                
                // Trigger click event
                config.eventHandler.onItemClick(getItemId(item), config.mediaType)
                
                // Track analytics if enabled
                config.analyticsConfig?.let { analytics ->
                    if (analytics.trackClicks) {
                        // TODO: Implement analytics tracking
                        // AnalyticsManager.trackEvent("item_click", analytics.category, ...)
                    }
                }
            }
        }
        
        /**
         * Handle item long press
         */
        private fun handleItemLongPress(item: T, position: Int) {
            if (NavigationThrottle.canNavigate()) {
                config.eventHandler.onItemLongPress(getItemId(item), config.mediaType)
            }
        }
        
        /**
         * Handle item gaining focus
         */
        private fun handleItemFocus(item: T, position: Int) {
            // Update focus manager
            focusManager.updateFocus(config.focusMemoryKey, position, getItemId(item))
            
            // Notify event handler
            config.eventHandler.onItemFocus(getItemId(item), config.mediaType)
            
            // Track analytics if enabled
            config.analyticsConfig?.let { analytics ->
                if (analytics.trackFocus) {
                    // TODO: Implement analytics tracking
                    // AnalyticsManager.trackEvent("item_focus", analytics.category, ...)
                }
            }
        }
        
        /**
         * Handle item losing focus
         */
        private fun handleItemFocusLost(item: T, position: Int) {
            config.eventHandler.onItemFocusLost(getItemId(item), config.mediaType)
        }
        
        /**
         * Extract item ID for focus tracking
         */
        private fun getItemId(item: T): Int {
            return when (item) {
                is MediaRowItem -> item.id.hashCode()
                else -> item.hashCode()
            }
        }
    }
}

/**
 * Composable content for individual media cards
 */
@Composable
private fun <T : Any> MediaCardContent(
    item: T,
    position: Int,
    config: MediaRowConfig<T>,
    isFastScrolling: Boolean,
    onItemClick: (T) -> Unit,
    onItemLongPress: ((T) -> Unit)?,
    onItemFocus: (T) -> Unit,
    onItemFocusLost: (T) -> Unit
) {
    val showPlaceholder = isFastScrolling && config.showPlaceholdersDuringScroll
    
    if (showPlaceholder) {
        // Show placeholder during fast scrolling
        PlaceholderCard(
            title = getItemTitle(item, config),
            width = getCardWidth(config.cardType),
            height = getCardHeight(config.cardType)
        )
    } else {
        // Show actual content
        MediaCard(
            item = item,
            cardType = config.cardType,
            onClick = { onItemClick(item) },
            onLongPress = onItemLongPress?.let { { it(item) } },
            onFocus = { onItemFocus(item) },
            onFocusLost = { onItemFocusLost(item) },
            itemMapper = config.itemMapper
        )
    }
}

/**
 * Individual media card composable
 */
@Composable
private fun <T : Any> MediaCard(
    item: T,
    cardType: CardType,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    onFocus: () -> Unit,
    onFocusLost: () -> Unit,
    itemMapper: ((T) -> MediaRowItem)?
) {
    // Convert item to MediaRowItem for display
    val mediaItem = itemMapper?.invoke(item) ?: (item as? MediaRowItem)
    
    if (mediaItem != null) {
        // TODO: Implement actual MediaCard composable
        // This will be a focused-enabled card that displays:
        // - Image with proper aspect ratio based on cardType
        // - Title and subtitle
        // - Progress indicator if available
        // - Rating if available
        // - Focus indicators and animations
        // - Click and long press handling
        
        // For now, use placeholder
        PlaceholderCard(
            title = mediaItem.title,
            width = getCardWidth(cardType),
            height = getCardHeight(cardType)
        )
    } else {
        // Fallback for non-MediaRowItem types
        PlaceholderCard(
            title = item.toString(),
            width = getCardWidth(cardType),
            height = getCardHeight(cardType)
        )
    }
}

/**
 * Get card width based on card type
 */
private fun getCardWidth(cardType: CardType): Int = when (cardType) {
    CardType.POSTER -> 150
    CardType.LANDSCAPE -> 300
    CardType.SQUARE -> 150
    CardType.CIRCLE -> 120
    CardType.COMPACT -> 120
    CardType.HERO -> 200
}

/**
 * Get card height based on card type
 */
private fun getCardHeight(cardType: CardType): Int = when (cardType) {
    CardType.POSTER -> 225 // 2:3 aspect ratio
    CardType.LANDSCAPE -> 169 // 16:9 aspect ratio
    CardType.SQUARE -> 150 // 1:1 aspect ratio
    CardType.CIRCLE -> 120 // 1:1 aspect ratio
    CardType.COMPACT -> 180
    CardType.HERO -> 300 // 2:3 aspect ratio but larger
}

/**
 * Extract title from item for placeholder display
 */
private fun <T : Any> getItemTitle(item: T, config: MediaRowConfig<T>): String {
    return when {
        config.itemMapper != null -> config.itemMapper.invoke(item).title
        item is MediaRowItem -> item.title
        else -> item.toString()
    }
}