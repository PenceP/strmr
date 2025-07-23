package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.ScraperRepository
import com.strmr.ai.data.models.Stream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreamSelectionViewModel @Inject constructor(
    private val scraperRepository: ScraperRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "StreamSelectionViewModel"
    }
    
    private val _streams = MutableStateFlow<List<Stream>>(emptyList())
    val streams: StateFlow<List<Stream>> = _streams.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun fetchStreams(imdbId: String, type: String, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "🔍 Fetching streams for: $type/$imdbId" + 
                    if (season != null && episode != null) " S${season}E${episode}" else "")
                
                val streamList = scraperRepository.getStreams(
                    imdbId = imdbId,
                    type = type,
                    season = season,
                    episode = episode
                )
                
                Log.d(TAG, "✅ Found ${streamList.size} streams")
                streamList.forEachIndexed { index, stream ->
                    Log.d(TAG, "📺 Stream $index: ${stream.displayName} (${stream.displayQuality}) - ${stream.displaySize}")
                    Log.d(TAG, "🔗 URL: ${stream.url}")
                }
                
                _streams.value = streamList
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching streams", e)
                _error.value = "Failed to fetch streams: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearStreams() {
        _streams.value = emptyList()
        _error.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
}