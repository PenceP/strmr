package com.strmr.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.viewmodel.MoviesViewModel
import com.strmr.ai.data.database.MovieEntity
import androidx.compose.ui.focus.FocusRequester
import android.util.Log
import com.strmr.ai.ui.components.rememberSelectionManager
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.CenteredMediaRow
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.data.OmdbResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MoviesPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onNavigateToDetails: ((Int) -> Unit)?
) {
    val context = LocalContext.current
    val viewModel: MoviesViewModel = hiltViewModel()
    
    // Load configuration
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("MOVIES")
    }
    
    val pagingUiState by viewModel.pagingUiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()
    
    // Get all available rows from uiState (now includes all data)
    val allRows = uiState.mediaRows
    val allRowTitles = allRows.keys.toList()
    val rowCount = allRowTitles.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Update local content focus when external focus changes
    LaunchedEffect(isContentFocused) {
        Log.d("MoviesPage", "🎯 External focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    // Get selected item for hero section
    val validRowIndex = if (selectionManager.selectedRowIndex >= rowCount) 0 else selectionManager.selectedRowIndex
    val currentRowTitle = allRowTitles.getOrNull(validRowIndex) ?: ""
    val selectedRow = allRows[currentRowTitle] ?: emptyList()
    val selectedItem = selectedRow.getOrNull(selectionManager.selectedItemIndex)

    // Check if current row should show hero based on configuration
    val shouldShowHero = pageConfiguration?.let { config ->
        currentRowTitle.let { title ->
            val rowConfig = config.rows.find { it.title == title }
            rowConfig?.showHero == true
        }
    } ?: false

    // Get backdrop URL for background
    val backdropUrl = if (shouldShowHero) {
        (selectedItem as? MovieEntity)?.backdropUrl
    } else null

    // OMDb ratings for hero section
    var omdbRatings by remember(selectedItem) { mutableStateOf<OmdbResponse?>(null) }
    LaunchedEffect(selectedItem) {
        if (shouldShowHero && selectedItem is MovieEntity) {
            val imdbId = selectedItem.imdbId
            if (!imdbId.isNullOrBlank()) {
                omdbRatings = withContext(Dispatchers.IO) {
                    viewModel.getOmdbRatings(imdbId)
                }
            } else {
                omdbRatings = null
            }
        } else {
            omdbRatings = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Backdrop image as the main background
        if (shouldShowHero && !backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    },
                contentScale = ContentScale.Crop,
                alpha = 1f
            )
            
            // Gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.3f)
                            ),
                            startX = 0f,
                            endX = 2200f
                        )
                    )
            )
        }

        // Wide, soft horizontal gradient overlay from left edge (behind nav bar) to main area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = -navBarWidthPx,
                        endX = 1200f
                    )
                )
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = navBarWidth)
        ) {
            // Hero section (based on configuration)
            if (shouldShowHero && selectedItem is MovieEntity) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.49f)
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = selectedItem.title,
                                logoUrl = selectedItem.logoUrl,
                                year = selectedItem.year,
                                formattedDate = selectedItem.releaseDate,
                                runtime = selectedItem.runtime,
                                genres = selectedItem.genres,
                                rating = selectedItem.rating,
                                overview = selectedItem.overview,
                                cast = selectedItem.cast.map { it.name ?: "" },
                                omdbRatings = omdbRatings,
                                onFetchLogo = {
                                    viewModel.fetchAndUpdateLogo(selectedItem)
                                }
                            )
                        }
                    )
                }
            }

            // Active row section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (shouldShowHero) 0.51f else 1f)
            ) {
                // Render the selected row
                val rowItems = selectedRow
                
                if (rowItems.isNotEmpty()) {
                    CenteredMediaRow(
                        title = currentRowTitle,
                        mediaItems = rowItems,
                        selectedIndex = selectionManager.selectedItemIndex,
                        isRowSelected = true,
                        onSelectionChanged = { newIndex ->
                            selectionManager.updateSelection(validRowIndex, newIndex)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        focusRequester = if (selectionManager.isContentFocused) focusRequesters.getOrNull(validRowIndex) else null,
                        onUpDown = { direction ->
                            val newRowIndex = validRowIndex + direction
                            if (newRowIndex >= 0 && newRowIndex < rowCount) {
                                Log.d("MoviesPage", "🎯 Row navigation: $validRowIndex -> $newRowIndex, maintaining focus")
                                selectionManager.updateSelection(newRowIndex, 0)
                                selectionManager.updateContentFocus(true)
                            }
                        },
                        isContentFocused = selectionManager.isContentFocused,
                        onContentFocusChanged = { focused ->
                            selectionManager.updateContentFocus(focused)
                            onContentFocusChanged?.invoke(focused)
                        },
                        currentRowIndex = validRowIndex,
                        totalRowCount = rowCount,
                        onItemClick = { movie ->
                            onNavigateToDetails?.invoke((movie as MovieEntity).tmdbId)
                        },
                        itemContent = { movie, isSelected ->
                            MediaCard(
                                title = (movie as MovieEntity).title,
                                posterUrl = movie.posterUrl,
                                isSelected = isSelected,
                                onClick = {
                                    onNavigateToDetails?.invoke(movie.tmdbId)
                                }
                            )
                        }
                    )
                } else {
                    // Show skeleton when no items loaded yet
                    MediaRowSkeleton(
                        title = currentRowTitle,
                        cardCount = 8,
                        cardType = SkeletonCardType.PORTRAIT,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
} 