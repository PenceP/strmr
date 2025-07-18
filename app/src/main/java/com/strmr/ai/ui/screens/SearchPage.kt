package com.strmr.ai.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.strmr.ai.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.strmr.ai.data.SearchResultItem
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.CenteredMediaRow
import com.strmr.ai.viewmodel.SearchViewModel
import java.util.Locale

@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    onNavigateToDetails: ((String, Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val popularSearches by viewModel.popularSearches.collectAsState()
    
    // Voice search launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.let { results ->
                if (results.isNotEmpty()) {
                    viewModel.onVoiceSearchResult(results[0])
                }
            }
        }
    }
    
    val navBarWidth = 56.dp
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Wallpaper background (fills entire screen)
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = navBarWidth)
                .verticalScroll(rememberScrollState())
        ) {
            // Search bar section
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onClear = viewModel::clearSearch,
                onVoiceSearch = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for movies, TV shows, or people")
                        }
                        voiceSearchLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.e("SearchPage", "Voice recognition not available", e)
                    }
                },
                hasSearchResults = searchResults != null,
                modifier = Modifier.padding(16.dp)
            )
            
            // Show suggestions or recent/popular searches when query is empty or short
            if (searchQuery.length < 2) {
                SearchSuggestions(
                    recentSearches = recentSearches,
                    popularSearches = popularSearches,
                    onSuggestionClick = { suggestion ->
                        viewModel.selectSuggestion(suggestion)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (searchSuggestions.isNotEmpty()) {
                // Show search suggestions
                SearchSuggestionsList(
                    suggestions = searchSuggestions,
                    onSuggestionClick = { suggestion ->
                        viewModel.selectSuggestion(suggestion)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Search results
            when {
                isLoading -> {
                    SearchResultsLoading(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                searchResults != null -> {
                    searchResults?.let { results ->
                        Log.d("SearchPage", "📊 Search results received: movies=${results.movies.size}, shows=${results.tvShows.size}, people=${results.people.size}")
                        SearchResultsContent(
                            searchResults = results,
                            onNavigateToDetails = onNavigateToDetails,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                searchQuery.length >= 2 -> {
                    // Show empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onVoiceSearch: () -> Unit,
    hasSearchResults: Boolean,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                    event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
                    hasSearchResults) {
                    // This will be handled by the SearchResultsContent
                    false
                } else {
                    false
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            
            
            IconButton(
                onClick = onVoiceSearch,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(20.dp)
                    )
                    .focusable()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            //Icon(
            //    imageVector = Icons.Default.Search,
            //    contentDescription = "Search",
            //    tint = Color.White.copy(alpha = 0.9f),
            //    modifier = Modifier.size(24.dp)
            //)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 18.sp
                ),
                cursorBrush = SolidColor(Color.White),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search movies, TV shows, people...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 18.sp
                        )
                    }
                    innerTextField()
                }
            )
            
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestions(
    recentSearches: List<String>,
    popularSearches: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (recentSearches.isNotEmpty()) {
            Text(
                text = "Recent Searches",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recentSearches) { search ->
                    SuggestionChip(
                        text = search,
                        onClick = { onSuggestionClick(search) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        if (popularSearches.isNotEmpty()) {
            Text(
                text = "Popular Searches",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularSearches) { search ->
                    SuggestionChip(
                        text = search,
                        onClick = { onSuggestionClick(search) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionsList(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(suggestions) { suggestion ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = suggestion,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .clickable { onClick() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected || isFocused) {
                Color(0xFFF0F0F0) // Light light gray when selected or focused
            } else {
                Color(0xFF101010) // Dark gray when not selected
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected || isFocused) {
                Color.Black // Black text on light gray background
            } else {
                Color.White // White text on dark gray background
            },
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SearchResultsLoading(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Movies skeleton
        MediaRowSkeleton(
            title = "Movies",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // TV Shows skeleton
        MediaRowSkeleton(
            title = "TV Shows",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // People skeleton
        MediaRowSkeleton(
            title = "People",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun SearchResultsContent(
    searchResults: com.strmr.ai.data.SearchResults,
    onNavigateToDetails: ((String, Int) -> Unit)?,
    modifier: Modifier = Modifier
) {
    // Build list of available sections - always include sections if they have data
    val sections = buildList {
        if (searchResults.movies.isNotEmpty()) {
            add("Movies" to searchResults.movies)
        }
        if (searchResults.tvShows.isNotEmpty()) {
            add("TV Shows" to searchResults.tvShows)
        }
        if (searchResults.people.isNotEmpty()) {
            add("People" to searchResults.people)
        }
    }
    
    // Debug logging
    Log.d("SearchPage", "🔍 Search results sections: ${sections.size}")
    sections.forEachIndexed { index, (title, items) ->
        Log.d("SearchPage", "  Section $index: $title (${items.size} items)")
    }
    
    // State management similar to MediaPage
    var selectedRowIndex by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf(0) }
    var isContentFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    
    // Reset selection when sections change
    LaunchedEffect(sections.size) {
        selectedRowIndex = 0
        selectedItemIndex = 0
        isContentFocused = false
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // Navigate to first row if not already focused on content
                            if (!isContentFocused && sections.isNotEmpty()) {
                                isContentFocused = true
                                selectedRowIndex = 0
                                selectedItemIndex = 0
                                true
                            } else {
                                false // Let individual rows handle navigation
                            }
                        }
                        else -> false
                    }
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sections.forEachIndexed { rowIndex, (title, items) ->
            CenteredMediaRow(
                title = title,
                mediaItems = items,
                selectedIndex = if (rowIndex == selectedRowIndex) selectedItemIndex else 0,
                isRowSelected = rowIndex == selectedRowIndex,
                onSelectionChanged = { newIndex ->
                    if (rowIndex == selectedRowIndex) {
                        selectedItemIndex = newIndex
                    }
                },
                onUpDown = { direction ->
                    val newRowIndex = selectedRowIndex + direction
                    if (newRowIndex in 0 until sections.size) {
                        selectedRowIndex = newRowIndex
                        selectedItemIndex = 0
                    } else if (newRowIndex < 0) {
                        // Go back to search bar
                        isContentFocused = false
                    }
                },
                focusRequester = if (rowIndex == selectedRowIndex) focusRequester else null,
                isContentFocused = isContentFocused,
                onContentFocusChanged = { focused ->
                    isContentFocused = focused
                },
                onItemClick = { item ->
                    when (item) {
                        is SearchResultItem.Movie -> {
                            onNavigateToDetails?.invoke("movie", item.tmdbId ?: item.id)
                        }
                        is SearchResultItem.TvShow -> {
                            onNavigateToDetails?.invoke("tvshow", item.tmdbId ?: item.id)
                        }
                        is SearchResultItem.Person -> {
                            Log.d("SearchPage", "Person clicked: ${item.name}")
                        }
                    }
                },
                itemContent = { item, isSelected ->
                    SearchResultCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = { 
                            when (item) {
                                is SearchResultItem.Movie -> {
                                    onNavigateToDetails?.invoke("movie", item.tmdbId ?: item.id)
                                }
                                is SearchResultItem.TvShow -> {
                                    onNavigateToDetails?.invoke("tvshow", item.tmdbId ?: item.id)
                                }
                                is SearchResultItem.Person -> {
                                    Log.d("SearchPage", "Person clicked: ${item.name}")
                                }
                            }
                        }
                    )
                }
            )
        }
    }
}


@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (item) {
        is SearchResultItem.Movie -> {
            MediaCard(
                title = item.title,
                posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier
            )
        }
        is SearchResultItem.TvShow -> {
            MediaCard(
                title = item.title,
                posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier
            )
        }
        is SearchResultItem.Person -> {
            PersonCard(
                person = item,
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PersonCard(
    person: SearchResultItem.Person,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseWidth = 120.dp
    val baseHeight = 180.dp
    val targetWidth = if (isSelected) baseWidth * 1.2f else baseWidth
    val targetHeight = if (isSelected) baseHeight * 1.1f else baseHeight
    
    Card(
        modifier = modifier
            .width(targetWidth)
            .height(targetHeight)
            .clickable { onClick() }
            .focusable()
            .let { mod ->
                if (isSelected) {
                    mod.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                } else {
                    mod
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (person.profilePath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w300${person.profilePath}",
                        contentDescription = person.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Default person icon or placeholder
                    Text(
                        text = person.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Name
            Text(
                text = person.name,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}