package com.strmr.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.data.CollectionMovie
import com.strmr.ai.ui.components.common.row.MediaRow
import com.strmr.ai.ui.components.common.row.MediaRowConfigs
import com.strmr.ai.ui.components.common.events.EventHandler
import com.strmr.ai.ui.components.common.focus.DpadFocusManager
import com.strmr.ai.ui.components.common.focus.rememberDpadFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border

/**
 * NEW IMPLEMENTATION: CollectionRow using the unified MediaRow system
 */
@Composable
fun CollectionRowNew(
    collectionMovies: List<CollectionMovie>,
    onItemClick: (CollectionMovie) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null
) {
    if (collectionMovies.isEmpty()) return
    
    // Create event handler that maps CollectionMovie clicks
    val eventHandler = object : EventHandler {
        override fun onItemClick(itemId: Int, mediaType: com.strmr.ai.ui.components.common.events.MediaType) {
            // Find the movie by ID and trigger the click
            val movie = collectionMovies.find { it.id == itemId }
            movie?.let { onItemClick(it) }
        }
        
        override fun onItemLongPress(itemId: Int, mediaType: com.strmr.ai.ui.components.common.events.MediaType) {
            // No long press handling for collection movies currently
        }
        
        override fun onItemFocus(itemId: Int, mediaType: com.strmr.ai.ui.components.common.events.MediaType) {
            // No specific focus handling needed
        }
        
        override fun onItemFocusLost(itemId: Int, mediaType: com.strmr.ai.ui.components.common.events.MediaType) {
            // No specific focus lost handling needed
        }
    }
    
    MediaRow(
        config = MediaRowConfigs.collectionMovieRow(
            items = collectionMovies,
            focusKey = "collection_row_${collectionMovies.firstOrNull()?.id ?: 0}",
            eventHandler = eventHandler
        ),
        modifier = modifier
    )
}

/**
 * DEPRECATED: Original CollectionRow implementation for comparison
 * This will be removed after migration is verified
 */
@Composable
@Deprecated("Use CollectionRowNew instead", ReplaceWith("CollectionRowNew"))
fun CollectionRowOld(
    collectionMovies: List<CollectionMovie>,
    onItemClick: (CollectionMovie) -> Unit,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null
) {
    if (collectionMovies.isEmpty()) return
    
    val title = "Part of Collection"
    
    UnifiedMediaRow(
        config = MediaRowConfig(
            title = title,
            dataSource = DataSource.RegularList(collectionMovies.take(10)),
            selectedIndex = selectedIndex,
            isRowSelected = isRowSelected,
            onSelectionChanged = onSelectionChanged,
            onUpDown = onUpDown,
            onItemClick = { movie -> onItemClick(movie) },
            focusRequester = focusRequester,
            onContentFocusChanged = onContentFocusChanged,
            cardType = CardType.PORTRAIT,
            itemWidth = 120.dp,
            itemSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 48.dp),
            itemContent = { movie, isSelected ->
                CollectionMovieCard(
                    movie = movie,
                    onClick = { onItemClick(movie) },
                    isSelected = isSelected
                )
            }
        ),
        modifier = modifier
    )
}

// Note: CollectionMovieCard is already defined in CollectionRow.kt
// The new MediaRow system will use the MediaCard composable with the itemMapper
// This provides better separation of concerns and reusability