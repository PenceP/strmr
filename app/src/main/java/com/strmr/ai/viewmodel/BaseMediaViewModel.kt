package com.strmr.ai.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.RetrofitInstance
import com.strmr.ai.data.TmdbApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseMediaViewModel<T : Any>(
    application: Application
) : AndroidViewModel(application) {
    
    protected val tmdbService: TmdbApiService = RetrofitInstance.tmdb.create(TmdbApiService::class.java)
    
    protected abstract val _uiState: MutableStateFlow<MediaUiState<T>>
    val uiState: StateFlow<MediaUiState<T>> = _uiState
    
    protected abstract suspend fun refreshData()
    protected abstract suspend fun fetchLogoForItem(item: T)
    protected abstract fun updateItemWithLogo(tmdbId: Int, logoUrl: String?)
    protected abstract fun getTmdbId(item: T): Int
    protected abstract fun getTitle(item: T): String
    
    protected fun refreshDataWithErrorHandling() {
        viewModelScope.launch {
            try {
                Log.d("BaseMediaViewModel", "🔄 Starting to refresh data from API...")
                _uiState.value = MediaUiState.Loading
                refreshData()
                Log.d("BaseMediaViewModel", "✅ Finished refreshing data from API")
            } catch (e: Exception) {
                Log.e("BaseMediaViewModel", "❌ Error refreshing data", e)
                _uiState.value = MediaUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
    
    protected fun onItemSelected(selectedIndex: Int, items: List<T>) {
        if (selectedIndex < items.size) {
            val selectedItem = items[selectedIndex]
            Log.d("BaseMediaViewModel", "🎯 Item selected: '${getTitle(selectedItem)}' (index: $selectedIndex)")
            if (getTmdbId(selectedItem) != 0) {
                Log.d("BaseMediaViewModel", "🔍 Fetching logo for '${getTitle(selectedItem)}' (TMDB: ${getTmdbId(selectedItem)})")
                viewModelScope.launch {
                    fetchLogoForItem(selectedItem)
                }
            } else {
                Log.d("BaseMediaViewModel", "✅ Logo already cached for '${getTitle(selectedItem)}'")
            }
        }
    }
    
    protected suspend fun fetchLogoWithErrorHandling(
        tmdbId: Int,
        title: String,
        fetchImages: suspend () -> Any,
        updateLogo: suspend (Int, String?) -> Unit
    ) {
        try {
            Log.d("BaseMediaViewModel", "📡 Fetching logo from TMDB API for '$title' (TMDB: $tmdbId)")
            val images = withContext(Dispatchers.IO) {
                fetchImages()
            }
            
            // Extract logo URL from images response
            val logoUrl = extractLogoUrl(images)
            Log.d("BaseMediaViewModel", "🎨 Logo URL for '$title': $logoUrl")
            
            // Save logo URL to database
            updateLogo(tmdbId, logoUrl)
            
            // Update UI state
            updateItemWithLogo(tmdbId, logoUrl)
        } catch (e: Exception) {
            Log.w("BaseMediaViewModel", "❌ Error fetching logo for tmdbId=$tmdbId", e)
            // Save null to database to avoid repeated failed attempts
            updateLogo(tmdbId, null)
        }
    }
    
    private fun extractLogoUrl(images: Any): String? {
        // This is a simplified version - in practice, you'd need to handle the specific response types
        // For now, we'll return null and let the concrete implementations handle this
        return null
    }
}

sealed class MediaUiState<out T> {
    object Loading : MediaUiState<Nothing>()
    data class Success<T>(val items: List<T>) : MediaUiState<T>()
    data class Error(val message: String) : MediaUiState<Nothing>()
} 