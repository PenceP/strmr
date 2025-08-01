package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.strmr.ai.config.ConfigurationLoader
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.rememberSelectionManager
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.GenericTvShowsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TvShowsPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((Int) -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: GenericTvShowsViewModel = hiltViewModel()

    // Load configuration
    var pageConfiguration by remember { mutableStateOf<PageConfiguration?>(null) }
    LaunchedEffect(Unit) {
        val configLoader = ConfigurationLoader(context)
        pageConfiguration = configLoader.loadPageConfiguration("TV")
        pageConfiguration?.let { config ->
            viewModel.initializeWithConfiguration(config)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val pagingUiState by viewModel.pagingUiState.collectAsState()
    val logoUrls by viewModel.logoUrls.collectAsState()

    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()

    // For now, get row titles from regular uiState for navigation
    // but we'll use paging data for actual display
    val allRowTitles = uiState.mediaRows.keys.toList()
    val rowCount = allRowTitles.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Update local content focus when external focus changes
    LaunchedEffect(isContentFocused) {
        Log.d("TvShowsPage", "🎯 External focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }

    val navBarWidth = 56.dp
    val navBarWidthPx = with(LocalDensity.current) { navBarWidth.toPx() }

    // Get selected item for hero section
    val validRowIndex = if (selectionManager.selectedRowIndex >= rowCount) 0 else selectionManager.selectedRowIndex
    val currentRowTitle = allRowTitles.getOrNull(validRowIndex) ?: ""

    // Get the paging flow for the current row
    val currentPagingFlow = pagingUiState.mediaRows[currentRowTitle]

    // Collect paging items for the current row
    val pagingItems = currentPagingFlow?.collectAsLazyPagingItems()

    // Get selected item from paging data if available, otherwise from regular data
    val selectedRow = uiState.mediaRows[currentRowTitle] ?: emptyList()
    val selectedItem =
        if (pagingItems != null && pagingItems.itemCount > selectionManager.selectedItemIndex) {
            pagingItems[selectionManager.selectedItemIndex]
        } else {
            selectedRow.getOrNull(selectionManager.selectedItemIndex)
        }

    // Update focused row in ViewModel when it changes
    LaunchedEffect(currentRowTitle) {
        if (currentRowTitle.isNotEmpty()) {
            viewModel.updateFocusedRow(currentRowTitle)
        }
    }

    // Check if current row should show hero based on configuration
    val shouldShowHero =
        pageConfiguration?.let { config ->
            currentRowTitle.let { title ->
                val rowConfig = config.rows.find { it.title == title }
                rowConfig?.showHero == true
            }
        } ?: false

    // Get backdrop URL for background
    val backdropUrl =
        if (shouldShowHero) {
            (selectedItem as? TvShowEntity)?.backdropUrl
        } else {
            null
        }

    // OMDb ratings for hero section
    var omdbRatings by remember(selectedItem) { mutableStateOf<OmdbResponse?>(null) }
    LaunchedEffect(selectedItem) {
        if (shouldShowHero && selectedItem is TvShowEntity) {
            val imdbId = selectedItem.imdbId
            if (!imdbId.isNullOrBlank()) {
                omdbRatings =
                    withContext(Dispatchers.IO) {
                        viewModel.fetchOmdbRatings(imdbId)
                    }
            } else {
                omdbRatings = null
            }
        } else {
            omdbRatings = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // Backdrop image as the main background
        if (shouldShowHero && !backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.1f
                            scaleY = 1.1f
                        }
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
                alpha = 1f,
            )

            // Gradient overlay for readability
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Black.copy(alpha = 0.3f),
                                    ),
                                startX = 0f,
                                endX = 2200f,
                            ),
                        ),
            )
        }

        // Wide, soft horizontal gradient overlay from left edge (behind nav bar) to main area
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    Color.Black,
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                ),
                            startX = -navBarWidthPx,
                            endX = 1200f,
                        ),
                    ),
        )

        // Main content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 1.dp),
        ) {
            // Hero section (based on configuration)
            if (shouldShowHero && selectedItem is TvShowEntity) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(0.49f)
                            .padding(start = navBarWidth - 2.dp),
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = selectedItem.title,
                                logoUrl = logoUrls[selectedItem.tmdbId] ?: selectedItem.logoUrl,
                                year = selectedItem.year,
                                formattedDate = selectedItem.firstAirDate,
                                runtime = null, // TV shows don't have runtime
                                genres = selectedItem.genres,
                                rating = selectedItem.rating,
                                overview = selectedItem.overview,
                                cast = selectedItem.cast.map { it.name ?: "" },
                                omdbRatings = omdbRatings,
                                onFetchLogo = {
                                    viewModel.fetchAndUpdateLogo(selectedItem)
                                },
                            )
                        },
                    )
                }
            }

            // Active row section
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(if (shouldShowHero) 0.51f else 1f),
            ) {
                // Check if we have paging data for this row
                if (currentPagingFlow != null && pagingItems != null) {
                    // Use UnifiedMediaRow with paging data source
                    UnifiedMediaRow(
                        config =
                            MediaRowConfig(
                                title = currentRowTitle,
                                dataSource = DataSource.PagingList(pagingItems),
                                selectedIndex = selectionManager.selectedItemIndex,
                                isRowSelected = true,
                                onSelectionChanged = { newIndex ->
                                    selectionManager.updateSelection(validRowIndex, newIndex)
                                    // Update row position in view model
                                    if (pagingItems.itemCount > 0) {
                                        viewModel.updateRowPosition(currentRowTitle, newIndex, pagingItems.itemCount)
                                    }
                                },
                                onUpDown = { direction ->
                                    val newRowIndex = validRowIndex + direction
                                    if (newRowIndex >= 0 && newRowIndex < rowCount) {
                                        Log.d("TvShowsPage", "🎯 Row navigation: $validRowIndex -> $newRowIndex, maintaining focus")
                                        selectionManager.updateSelection(newRowIndex, 0)
                                        selectionManager.updateContentFocus(true)
                                    }
                                },
                                onLeftBoundary = onLeftBoundary,
                                onContentFocusChanged = { focused ->
                                    selectionManager.updateContentFocus(focused)
                                    onContentFocusChanged?.invoke(focused)
                                },
                                focusRequester = if (selectionManager.isContentFocused) focusRequesters.getOrNull(validRowIndex) else null,
                                cardType = CardType.PORTRAIT,
                                itemWidth = 120.dp,
                                itemSpacing = 12.dp,
                                isContentFocused = selectionManager.isContentFocused,
                                // contentPadding = PaddingValues(horizontal = 48.dp),
                                onItemClick = { show ->
                                    if (show is TvShowEntity) {
                                        onNavigateToDetails?.invoke(show.tmdbId)
                                    }
                                },
                                itemContent = { show, isSelected ->
                                    if (show is TvShowEntity) {
                                        MediaCard(
                                            title = show.title,
                                            posterUrl = show.posterUrl,
                                            isSelected = isSelected,
                                            onClick = {
                                                onNavigateToDetails?.invoke(show.tmdbId)
                                            },
                                        )
                                    }
                                },
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // Fallback to regular row if no paging data
                    val rowItems = selectedRow

                    if (rowItems.isNotEmpty()) {
                        UnifiedMediaRow(
                            config =
                                MediaRowConfig(
                                    title = currentRowTitle,
                                    dataSource = DataSource.RegularList(rowItems),
                                    selectedIndex = selectionManager.selectedItemIndex,
                                    isRowSelected = true,
                                    onSelectionChanged = { newIndex ->
                                        selectionManager.updateSelection(validRowIndex, newIndex)
                                    },
                                    onUpDown = { direction ->
                                        val newRowIndex = validRowIndex + direction
                                        if (newRowIndex >= 0 && newRowIndex < rowCount) {
                                            Log.d("TvShowsPage", "🎯 Row navigation: $validRowIndex -> $newRowIndex, maintaining focus")
                                            selectionManager.updateSelection(newRowIndex, 0)
                                            selectionManager.updateContentFocus(true)
                                        }
                                    },
                                    onLeftBoundary = onLeftBoundary,
                                    onContentFocusChanged = { focused ->
                                        selectionManager.updateContentFocus(focused)
                                        onContentFocusChanged?.invoke(focused)
                                    },
                                    focusRequester =
                                        if (selectionManager.isContentFocused) {
                                            focusRequesters.getOrNull(
                                                validRowIndex,
                                            )
                                        } else {
                                            null
                                        },
                                    cardType = CardType.PORTRAIT,
                                    itemWidth = 120.dp,
                                    itemSpacing = 12.dp,
                                    isContentFocused = selectionManager.isContentFocused,
                                    // contentPadding = PaddingValues(horizontal = 48.dp),
                                    onItemClick = { show ->
                                        if (show is TvShowEntity) {
                                            onNavigateToDetails?.invoke(show.tmdbId)
                                        }
                                    },
                                    itemContent = { show, isSelected ->
                                        if (show is TvShowEntity) {
                                            MediaCard(
                                                title = show.title,
                                                posterUrl = show.posterUrl,
                                                isSelected = isSelected,
                                                onClick = {
                                                    onNavigateToDetails?.invoke(show.tmdbId)
                                                },
                                            )
                                        }
                                    },
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        // Show skeleton when no items loaded yet
                        MediaRowSkeleton(
                            title = currentRowTitle,
                            cardCount = 8,
                            cardType = SkeletonCardType.PORTRAIT,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // Up arrow (shown when there are rows above current row)
        if (validRowIndex > 0) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Navigate up",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // Down arrow (shown when there are rows below current row)
        if (validRowIndex < rowCount - 1) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Navigate down",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
