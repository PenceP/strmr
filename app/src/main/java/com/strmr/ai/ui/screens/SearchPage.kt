package com.strmr.ai.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strmr.ai.R
import com.strmr.ai.data.SearchResultItem
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.MediaCard
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.MediaRowSkeleton
import com.strmr.ai.ui.components.SkeletonCardType
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import com.strmr.ai.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    onNavigateToDetails: ((String, Int) -> Unit)? = null,
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
    val voiceSearchLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
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

    // Local state for managing focus - don't use external isContentFocused for auto-focus
    var localContentFocused by remember { mutableStateOf(false) }
    val searchBarFocusRequester = remember { FocusRequester() }

    WithFocusProviders("search") {
        Box(
            modifier =
                modifier
                    .fillMaxSize(),
        ) {
            // Wallpaper background (fills entire screen)
            Image(
                painter = painterResource(id = R.drawable.wallpaper),
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = navBarWidth),
            ) {
                // Search bar section (fixed at top)
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClear = viewModel::clearSearch,
                    onVoiceSearch = {
                        try {
                            val intent =
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
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
                    searchBarFocusRequester = searchBarFocusRequester,
                    onDownPressed = {
                        // Only transfer focus to results when user explicitly presses DOWN
                        if (searchResults != null) {
                            Log.d("SearchPage", "🎯 User pressed DOWN - transferring focus to results")
                            localContentFocused = true
                            onContentFocusChanged?.invoke(true)
                        }
                    },
                    onFocusReceived = {
                        // Keep focus on search bar, don't auto-transfer to results
                        Log.d("SearchPage", "🎯 Search bar focused - maintaining search bar focus")
                        localContentFocused = false
                        onContentFocusChanged?.invoke(false)
                    },
                    modifier = Modifier.padding(16.dp),
                )

                // Single LazyColumn for all content
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Error message
                    errorMessage?.let { error ->
                        item {
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                            ) {
                                Text(
                                    text = error,
                                    color = Color.Red,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }

                    // Search results
                    when {
                        isLoading -> {
                            item {
                                SearchResultsLoading(
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        searchResults != null -> {
                            searchResults?.let { results ->
                                Log.d(
                                    "SearchPage",
                                    "📊 Search results received: movies=${results.movies.size}, shows=${results.tvShows.size}, people=${results.people.size}",
                                )

                                // Add sections directly as LazyColumn items
                                val sections =
                                    buildList {
                                        if (results.movies.isNotEmpty()) {
                                            add("Movies" to results.movies)
                                        }
                                        if (results.tvShows.isNotEmpty()) {
                                            add("TV Shows" to results.tvShows)
                                        }
                                        if (results.people.isNotEmpty()) {
                                            add("People" to results.people)
                                        }
                                    }

                                sections.forEachIndexed { rowIndex, (title, items) ->
                                    item(key = title) {
                                        UnifiedMediaRow(
                                            config =
                                                MediaRowConfig(
                                                    title = title,
                                                    dataSource = DataSource.RegularList(items),
                                                    cardType = CardType.PORTRAIT,
                                                    itemWidth = 120.dp,
                                                    itemSpacing = 12.dp,
                                                    contentPadding = PaddingValues(horizontal = 48.dp),
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
                                                            },
                                                        )
                                                    },
                                                ),
                                            rowIndex = rowIndex,
                                        )
                                    }
                                }
                            }
                        }
                        searchQuery.length >= 2 -> {
                            // Show empty state
                            item {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "No results found for \"$searchQuery\"",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
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
    searchBarFocusRequester: FocusRequester,
    onDownPressed: () -> Unit = {},
    onFocusReceived: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val voiceFocusRequester = remember { FocusRequester() }
    var isSearchBarFocused by remember { mutableStateOf(false) }
    var isVoiceButtonFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Voice search button - outside the search box on the left
        IconButton(
            onClick = onVoiceSearch,
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        Color.White.copy(alpha = if (isVoiceButtonFocused) 0.2f else 0.1f),
                        RoundedCornerShape(24.dp),
                    )
                    .focusRequester(voiceFocusRequester)
                    .onFocusChanged { isVoiceButtonFocused = it.isFocused }
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    searchBarFocusRequester.requestFocus()
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                    if (hasSearchResults) {
                                        onDownPressed()
                                        true
                                    } else {
                                        false
                                    }
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    },
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Search",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Search input box
        Card(
            modifier =
                Modifier
                    .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .focusRequester(searchBarFocusRequester)
                            .onFocusChanged { focusState ->
                                isSearchBarFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    onFocusReceived()
                                }
                            }
                            .onKeyEvent { event ->
                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    when (event.nativeKeyEvent.keyCode) {
                                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            voiceFocusRequester.requestFocus()
                                            true
                                        }
                                        android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (hasSearchResults) {
                                                Log.d("SearchPage", "🎯 DOWN pressed in search bar - user wants to navigate to results")
                                                onDownPressed()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            },
                    textStyle =
                        TextStyle(
                            color = Color.White,
                            fontSize = 18.sp,
                        ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search,
                        ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search movies, TV shows, people...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 18.sp,
                            )
                        }
                        innerTextField()
                    },
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsLoading(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Movies skeleton
        MediaRowSkeleton(
            title = "Movies",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        // TV Shows skeleton
        MediaRowSkeleton(
            title = "TV Shows",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        // People skeleton
        MediaRowSkeleton(
            title = "People",
            cardCount = 6,
            cardType = SkeletonCardType.PORTRAIT,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

// SearchResultsContent function removed - now handled directly in main LazyColumn

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is SearchResultItem.Movie -> {
            MediaCard(
                title = item.title,
                posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier,
            )
        }
        is SearchResultItem.TvShow -> {
            MediaCard(
                title = item.title,
                posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier,
            )
        }
        is SearchResultItem.Person -> {
            PersonCard(
                person = item,
                isSelected = isSelected,
                onClick = onClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun PersonCard(
    person: SearchResultItem.Person,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseWidth = 120.dp
    val baseHeight = 180.dp
    val targetWidth = if (isSelected) baseWidth * 1.2f else baseWidth
    val targetHeight = if (isSelected) baseHeight * 1.1f else baseHeight

    Card(
        modifier =
            modifier
                .width(targetWidth)
                .height(targetHeight)
                .clickable { onClick() }
                .let { mod ->
                    if (isSelected) {
                        mod.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    } else {
                        mod
                    }
                },
        colors =
            CardDefaults.cardColors(
                containerColor = Color.Gray.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Profile image
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (person.profilePath != null) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w300${person.profilePath}",
                        contentDescription = person.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // Default person icon or placeholder
                    Text(
                        text = person.name.take(2).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
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
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}
