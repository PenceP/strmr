package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.SearchRepository
import com.strmr.ai.data.SearchResults
import com.strmr.ai.data.SearchResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResults?>(null)
    val searchResults: StateFlow<SearchResults?> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Search suggestions (could be enhanced with recent searches, popular searches, etc.)
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Recent searches
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // Popular searches (could be loaded from backend)
    private val _popularSearches = MutableStateFlow(
        listOf(
            "Marvel", "Batman", "Star Wars", "Harry Potter", "Breaking Bad",
            "Game of Thrones", "The Office", "Stranger Things", "Friends", "Avengers"
        )
    )
    val popularSearches: StateFlow<List<String>> = _popularSearches.asStateFlow()

    init {
        // Set up debounced search
        _searchQuery
            .debounce(300) // Wait 300ms after user stops typing
            .filter { it.length >= 2 } // Only search for queries with 2+ characters
            .distinctUntilChanged() // Don't search if query hasn't changed
            .onEach { query ->
                Log.d("SearchViewModel", "🔍 Performing search for: '$query'")
                performSearch(query)
            }
            .launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
        
        // Clear results if query is too short
        if (query.length < 2) {
            _searchResults.value = null
            _isLoading.value = false
        }
        
        // Update suggestions based on query
        updateSuggestions(query)
    }

    private fun updateSuggestions(query: String) {
        if (query.isEmpty()) {
            _searchSuggestions.value = emptyList()
            return
        }
        
        val suggestions = mutableListOf<String>()
        
        // Add matching recent searches
        suggestions.addAll(
            _recentSearches.value.filter { 
                it.contains(query, ignoreCase = true) 
            }.take(3)
        )
        
        // Add matching popular searches
        suggestions.addAll(
            _popularSearches.value.filter { 
                it.contains(query, ignoreCase = true) 
            }.take(7)
        )
        
        _searchSuggestions.value = suggestions.distinct().take(10)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val results = searchRepository.searchMultiSource(query)
                _searchResults.value = results
                
                // Add to recent searches
                addToRecentSearches(query)
                
                Log.d("SearchViewModel", "✅ Search completed: ${results.movies.size} movies, ${results.tvShows.size} shows, ${results.people.size} people")
                
            } catch (e: Exception) {
                Log.e("SearchViewModel", "❌ Search failed: ${e.message}", e)
                _errorMessage.value = "Search failed. Please try again."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addToRecentSearches(query: String) {
        val currentRecent = _recentSearches.value.toMutableList()
        
        // Remove if already exists
        currentRecent.remove(query)
        
        // Add to beginning
        currentRecent.add(0, query)
        
        // Keep only last 10 searches
        _recentSearches.value = currentRecent.take(10)
    }

    fun selectSuggestion(suggestion: String) {
        _searchQuery.value = suggestion
        _searchSuggestions.value = emptyList()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = null
        _searchSuggestions.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Voice search functionality
    fun onVoiceSearchResult(voiceQuery: String) {
        Log.d("SearchViewModel", "🎤 Voice search result: '$voiceQuery'")
        updateSearchQuery(voiceQuery)
    }

    // For analytics and debugging
    fun logSearchInteraction(action: String, details: String = "") {
        Log.d("SearchViewModel", "📊 Search interaction: $action $details")
    }
}