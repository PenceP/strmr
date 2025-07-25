package com.strmr.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.util.Log
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.TmdbEnrichmentService

data class IntermediateViewUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val mediaItems: List<HomeMediaItem> = emptyList(),
    val itemName: String = "",
    val itemBackgroundUrl: String? = null,
    val isLoadingMore: Boolean = false
)

@HiltViewModel
class IntermediateViewViewModel @Inject constructor(
    private val intermediateViewRepository: com.strmr.ai.data.IntermediateViewRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(IntermediateViewUiState())
    val uiState: StateFlow<IntermediateViewUiState> = _uiState.asStateFlow()
    
    // Store current parameters for pagination
    private var currentViewType: String = ""
    private var currentItemId: String = ""
    private var currentItemName: String = ""
    private var currentItemBackgroundUrl: String? = null
    private var currentDataUrl: String? = null
    
    fun loadContent(
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?
    ) {
        Log.d("IntermediateViewVM", "🎬 Loading content: type=$viewType, id=$itemId, name=$itemName")
        
        // Store parameters for pagination
        currentViewType = viewType
        currentItemId = itemId
        currentItemName = itemName
        currentItemBackgroundUrl = itemBackgroundUrl
        currentDataUrl = dataUrl
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false,
            errorMessage = null,
            itemName = itemName,
            itemBackgroundUrl = itemBackgroundUrl
        )
        
        viewModelScope.launch {
            try {
                val mediaItems = intermediateViewRepository.getIntermediateViewData(
                    viewType = viewType,
                    itemId = itemId,
                    itemName = itemName,
                    itemBackgroundUrl = itemBackgroundUrl,
                    dataUrl = dataUrl
                )
                
                Log.d("IntermediateViewVM", "✅ Loaded ${mediaItems.size} items for $viewType")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mediaItems = mediaItems
                )
            } catch (e: Exception) {
                Log.e("IntermediateViewVM", "❌ Error loading content", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.message ?: "Failed to load content"
                )
            }
        }
    }
    
    /**
     * Force refresh the current content (bypasses cache)
     */
    fun refreshContent(
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?
    ) {
        Log.d("IntermediateViewVM", "🔄 Force refreshing content: type=$viewType, id=$itemId")
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            isError = false,
            errorMessage = null
        )
        
        viewModelScope.launch {
            try {
                val mediaItems = intermediateViewRepository.refreshIntermediateView(
                    viewType = viewType,
                    itemId = itemId,
                    itemName = itemName,
                    itemBackgroundUrl = itemBackgroundUrl,
                    dataUrl = dataUrl
                )
                
                Log.d("IntermediateViewVM", "✅ Refreshed ${mediaItems.size} items for $viewType")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mediaItems = mediaItems
                )
            } catch (e: Exception) {
                Log.e("IntermediateViewVM", "❌ Error refreshing content", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isError = true,
                    errorMessage = e.message ?: "Failed to refresh content"
                )
            }
        }
    }
    
    /**
     * Load more items for pagination
     */
    fun loadMore() {
        if (_uiState.value.isLoadingMore) {
            Log.d("IntermediateViewVM", "⚠️ Already loading more items, skipping")
            return
        }
        
        Log.d("IntermediateViewVM", "📄 Loading more items for $currentViewType")
        
        _uiState.value = _uiState.value.copy(isLoadingMore = true)
        
        viewModelScope.launch {
            try {
                val newItems = intermediateViewRepository.loadMoreItems(
                    viewType = currentViewType,
                    itemId = currentItemId,
                    itemName = currentItemName,
                    itemBackgroundUrl = currentItemBackgroundUrl,
                    dataUrl = currentDataUrl
                )
                
                Log.d("IntermediateViewVM", "✅ Loaded ${newItems.size} additional items")
                
                // Append new items to existing list
                val currentItems = _uiState.value.mediaItems
                val updatedItems = currentItems + newItems
                
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    mediaItems = updatedItems
                )
            } catch (e: Exception) {
                Log.e("IntermediateViewVM", "❌ Error loading more items", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false
                )
            }
        }
    }
}