package com.strmr.ai.ui.components.common.row

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import com.rubensousa.dpadrecyclerview.DpadRecyclerView
import com.strmr.ai.ui.components.common.focus.DpadFocusManager
import com.strmr.ai.ui.components.common.focus.rememberDpadFocusManager

/**
 * MediaRow - A unified row component for displaying media content using DpadRecyclerView.
 * 
 * This component replaces all existing row implementations (UnifiedMediaRow, CollectionRow, 
 * SimilarContentRow) with a single, configurable component that provides:
 * 
 * - Consistent focus management and memory
 * - Optimized scrolling performance
 * - Placeholder cards during fast scrolling
 * - Event handling for Android TV navigation
 * - Support for both static and paged data
 * 
 * @param config Configuration object defining the row's behavior and data
 * @param modifier Compose modifier for styling
 * @param focusManager Optional custom focus manager (uses default if not provided)
 */
@Composable
fun <T : Any> MediaRow(
    config: MediaRowConfig<T>,
    modifier: Modifier = Modifier,
    focusManager: DpadFocusManager = rememberDpadFocusManager()
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row title
        if (config.title.isNotEmpty()) {
            MediaRowTitle(
                title = config.title,
                itemCount = config.items.size,
                maxItemsToShow = config.maxItemsToShow
            )
        }
        
        // Media content row
        MediaRowContent(
            config = config,
            focusManager = focusManager
        )
    }
}

/**
 * Row title with optional item count and "View All" indicator
 */
@Composable
private fun MediaRowTitle(
    title: String,
    itemCount: Int,
    maxItemsToShow: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Show item count and "View All" hint if applicable
        if (maxItemsToShow != null && itemCount > maxItemsToShow) {
            Text(
                text = "$itemCount items",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * The actual media content using DpadRecyclerView
 */
@Composable
private fun <T : Any> MediaRowContent(
    config: MediaRowConfig<T>,
    focusManager: DpadFocusManager
) {
    val context = LocalContext.current
    
    // State for adapter management
    var adapter by remember { mutableStateOf<MediaRowAdapter<T>?>(null) }
    var recyclerView by remember { mutableStateOf<DpadRecyclerView?>(null) }
    
    // Initialize adapter when config changes
    LaunchedEffect(config) {
        adapter = MediaRowAdapter(config, focusManager).apply {
            onDataChanged = { itemCount ->
                // Handle data change notifications
                // Could trigger analytics or UI updates
            }
        }
    }
    
    // Handle focus restoration
    LaunchedEffect(config.focusMemoryKey) {
        val lastFocusedIndex = focusManager.getLastFocusedIndex(config.focusMemoryKey)
        if (lastFocusedIndex > 0 && recyclerView != null) {
            // Restore focus position after a short delay to ensure layout is complete
            kotlinx.coroutines.delay(100)
            recyclerView?.scrollToPosition(lastFocusedIndex)
        }
    }
    
    // DpadRecyclerView wrapped in AndroidView
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(getRowHeight(config.cardType)),
        factory = { context ->
            DpadRecyclerView(context).apply {
                // Configure layout manager
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                ).apply {
                    // Optimize for TV scrolling
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = 4
                }
                
                // Configure DpadRecyclerView specific settings
                setHasFixedSize(true)
                setLayoutWhileScrollingEnabled(false)
                
                // Set spacing between items
                addItemDecoration(MediaRowItemDecoration(16.dp.value.toInt()))
                
                // Configure focus behavior
                isFocusable = true
                isFocusableInTouchMode = false
                
                // Set padding for edge spacing
                setPadding(48.dp.value.toInt(), 0, 48.dp.value.toInt(), 0)
                clipToPadding = false
                
                // Store reference for focus restoration
                recyclerView = this
                
                // Set up scroll listeners for fast scrolling detection
                addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                    private var isScrolling = false
                    private val scrollThreshold = 10 // pixels per frame
                    
                    override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        
                        val wasFastScrolling = isScrolling
                        isScrolling = newState != androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
                        
                        // Update adapter fast scrolling state
                        if (wasFastScrolling != isScrolling) {
                            adapter?.setFastScrolling(isScrolling && config.enableFastScrolling)
                        }
                    }
                })
            }
        },
        update = { dpadRecyclerView ->
            // Update adapter when it changes
            adapter?.let { newAdapter ->
                if (dpadRecyclerView.adapter != newAdapter) {
                    dpadRecyclerView.adapter = newAdapter
                }
            }
        }
    )
}

/**
 * Custom ItemDecoration for consistent spacing between items
 */
private class MediaRowItemDecoration(
    private val spacing: Int
) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
    
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: android.view.View,
        parent: androidx.recyclerview.widget.RecyclerView,
        state: androidx.recyclerview.widget.RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0
        
        // Add spacing to the right of all items except the last one
        if (position < itemCount - 1) {
            outRect.right = spacing
        }
    }
}

/**
 * Get appropriate row height based on card type
 */
private fun getRowHeight(cardType: CardType): androidx.compose.ui.unit.Dp = when (cardType) {
    CardType.POSTER -> 280.dp        // Height for poster cards + title space
    CardType.LANDSCAPE -> 220.dp     // Height for landscape cards + title space  
    CardType.SQUARE -> 200.dp        // Height for square cards + title space
    CardType.CIRCLE -> 170.dp        // Height for circular cards + title space
    CardType.COMPACT -> 230.dp       // Height for compact cards + title space
    CardType.HERO -> 350.dp          // Height for hero cards + title space
}

/**
 * Preview functions for development
 */
// TODO: Add @Preview composables for different MediaRow configurations
// This would be useful for development and design validation

/**
 * Factory functions for common MediaRow usage patterns
 */
object MediaRows {
    
    /**
     * Create a standard movie poster row
     */
    @Composable
    fun <T : Any> MovieRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: com.strmr.ai.ui.components.common.events.EventHandler,
        modifier: Modifier = Modifier,
        focusManager: DpadFocusManager = rememberDpadFocusManager()
    ) {
        MediaRow(
            config = MediaRowConfigs.movieRow(title, items, focusKey, eventHandler),
            modifier = modifier,
            focusManager = focusManager
        )
    }
    
    /**
     * Create a TV show poster row
     */
    @Composable
    fun <T : Any> TvShowRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: com.strmr.ai.ui.components.common.events.EventHandler,
        modifier: Modifier = Modifier,
        focusManager: DpadFocusManager = rememberDpadFocusManager()
    ) {
        MediaRow(
            config = MediaRowConfigs.tvShowRow(title, items, focusKey, eventHandler),
            modifier = modifier,
            focusManager = focusManager
        )
    }
    
    /**
     * Create a continue watching row with landscape cards
     */
    @Composable
    fun <T : Any> ContinueWatchingRow(
        items: List<T>,
        focusKey: String,
        eventHandler: com.strmr.ai.ui.components.common.events.EventHandler,
        modifier: Modifier = Modifier,
        focusManager: DpadFocusManager = rememberDpadFocusManager()
    ) {
        MediaRow(
            config = MediaRowConfigs.continueWatchingRow(items, focusKey, eventHandler),
            modifier = modifier,
            focusManager = focusManager
        )
    }
    
    /**
     * Create a collection row with limited items and "View All" support
     */
    @Composable
    fun <T : Any> CollectionRow(
        title: String,
        items: List<T>,
        focusKey: String,
        eventHandler: com.strmr.ai.ui.components.common.events.EventHandler,
        maxItems: Int = 10,
        modifier: Modifier = Modifier,
        focusManager: DpadFocusManager = rememberDpadFocusManager()
    ) {
        MediaRow(
            config = MediaRowConfigs.collectionRow(title, items, focusKey, eventHandler, maxItems),
            modifier = modifier,
            focusManager = focusManager
        )
    }
}