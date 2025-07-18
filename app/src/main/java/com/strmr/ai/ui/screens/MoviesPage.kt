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

@Composable
fun MoviesPage(
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onNavigateToDetails: ((Int) -> Unit)?
) {
    val viewModel: MoviesViewModel = hiltViewModel()
    
    val pagingUiState by viewModel.pagingUiState.collectAsState()
    
    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()
    
    val rowTitles = pagingUiState.mediaRows.keys.toList()
    val rowCount = rowTitles.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Update local content focus when external focus changes
    LaunchedEffect(isContentFocused) {
        Log.d("MoviesPage", "🎯 External focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }

    val navBarWidth = 56.dp

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MediaPagingPage(
            pagingUiState = pagingUiState,
            selectedRowIndex = selectionManager.selectedRowIndex,
            selectedItemIndex = selectionManager.selectedItemIndex,
            onItemSelected = { rowIdx, itemIdx ->
                selectionManager.updateSelection(rowIdx, itemIdx)
                viewModel.onMovieSelected(rowIdx, itemIdx)
            },
            onSelectionChanged = { newIndex ->
                selectionManager.updateSelection(selectionManager.selectedRowIndex, newIndex)
            },
            modifier = Modifier,
            focusRequester = if (selectionManager.selectedRowIndex < focusRequesters.size && selectionManager.isContentFocused) focusRequesters[selectionManager.selectedRowIndex] else null,
            onUpDown = { direction ->
                val newRowIndex = selectionManager.selectedRowIndex + direction
                if (newRowIndex >= 0 && newRowIndex < rowCount) {
                    Log.d("MoviesPage", "🎯 Row navigation: ${selectionManager.selectedRowIndex} -> $newRowIndex, maintaining focus")
                    selectionManager.updateSelection(newRowIndex, 0)
                    // Ensure content focus is maintained during row transitions
                    selectionManager.updateContentFocus(true)
                }
            },
            isContentFocused = selectionManager.isContentFocused,
            onContentFocusChanged = { focused ->
                selectionManager.updateContentFocus(focused)
                onContentFocusChanged?.invoke(focused)
            },
            onItemClick = { movie ->
                onNavigateToDetails?.invoke((movie as MovieEntity).tmdbId)
            },
            getOmdbRatings = { imdbId ->
                viewModel.getOmdbRatings(imdbId)
            }
        )
    }
} 