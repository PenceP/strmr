package com.strmr.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.strmr.ai.viewmodel.MoviesViewModel
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.OmdbRepository
import androidx.compose.ui.focus.FocusRequester
import android.util.Log
import com.strmr.ai.ui.components.rememberSelectionManager

@Composable
fun MoviesPage(
    movieRepository: MovieRepository,
    tmdbApiService: TmdbApiService,
    omdbRepository: OmdbRepository,
    isContentFocused: Boolean,
    onContentFocusChanged: ((Boolean) -> Unit)?,
    onNavigateToDetails: ((Int) -> Unit)?
) {
    val viewModel: MoviesViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MoviesViewModel(movieRepository, tmdbApiService)
            }
        }
    )
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Use the new SelectionManager
    val selectionManager = rememberSelectionManager()
    
    val rowTitles = uiState.mediaRows.keys.toList()
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
        MediaPage(
            uiState = uiState,
            selectedRowIndex = selectionManager.selectedRowIndex,
            selectedItemIndex = selectionManager.selectedItemIndex,
            onItemSelected = { rowIdx, itemIdx ->
                selectionManager.updateSelection(rowIdx, itemIdx)
                val rowTitles = uiState.mediaRows.keys.toList()
                val rowTitle = rowTitles.getOrNull(rowIdx) ?: return@MediaPage
                val movies = uiState.mediaRows[rowTitle] ?: return@MediaPage
                val selectedMovie = movies.getOrNull(itemIdx) ?: return@MediaPage
                viewModel.onMovieSelected(rowIdx, itemIdx)
                // Do NOT navigate here
            },
            onSelectionChanged = { newIndex ->
                selectionManager.updateSelection(selectionManager.selectedRowIndex, newIndex)
            },
            // Pass the paging function here:
            onCheckForMoreItems = { rowIdx, itemIdx, totalItems ->
                viewModel.loadMore(rowIdx, itemIdx, totalItems)
            },
            modifier = Modifier,
            focusRequester = if (selectionManager.selectedRowIndex < focusRequesters.size && selectionManager.isContentFocused) focusRequesters[selectionManager.selectedRowIndex] else null,
            onUpDown = { direction ->
                val newRowIndex = selectionManager.selectedRowIndex + direction
                if (newRowIndex >= 0 && newRowIndex < rowCount) {
                    selectionManager.updateSelection(newRowIndex, 0)
                }
            },
            isContentFocused = selectionManager.isContentFocused,
            onContentFocusChanged = { focused ->
                selectionManager.updateContentFocus(focused)
                onContentFocusChanged?.invoke(focused)
            },
            omdbRepository = omdbRepository,
            onItemClick = { movie ->
                onNavigateToDetails?.invoke((movie as MovieEntity).tmdbId)
            }
        )
    }
} 