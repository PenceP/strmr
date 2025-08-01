package com.strmr.ai.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import com.strmr.ai.ui.components.rememberSelectionManager
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.SearchViewModel
import kotlinx.coroutines.CoroutineScope
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

            // Scrollable content area
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 0.dp),
            ) {
                // Error message
                errorMessage?.let { error ->
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

                // Search results
                when {
                    isLoading -> {
                        SearchResultsLoading(
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    searchResults != null -> {
                        searchResults?.let { results ->
                            Log.d(
                                "SearchPage",
                                "📊 Search results received: movies=${results.movies.size}, shows=${results.tvShows.size}, people=${results.people.size}",
                            )
                            SearchResultsContent(
                                searchResults = results,
                                onNavigateToDetails = onNavigateToDetails,
                                onFocusReturnToSearchBar = {
                                    // Return focus to search bar when UP pressed on first row
                                    searchBarFocusRequester.requestFocus()
                                    localContentFocused = false
                                    onContentFocusChanged?.invoke(false)
                                },
                                isContentFocused = localContentFocused,
                                onContentFocusChanged = { focused ->
                                    localContentFocused = focused
                                    onContentFocusChanged?.invoke(focused)
                                },
                                onLeftBoundary = onLeftBoundary,
                                scrollState = scrollState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    searchQuery.length >= 2 -> {
                        // Show empty state
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

@Composable
private fun SearchResultsContent(
    searchResults: com.strmr.ai.data.SearchResults,
    onNavigateToDetails: ((String, Int) -> Unit)?,
    onFocusReturnToSearchBar: () -> Unit = {},
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
    onLeftBoundary: (() -> Unit)? = null,
    scrollState: ScrollState? = null,
    coroutineScope: CoroutineScope? = null,
    modifier: Modifier = Modifier,
) {
    // Build list of available sections - always include sections if they have data
    val sections =
        buildList {
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

    // Use the unified SelectionManager like HomePage and DetailsPage
    val selectionManager = rememberSelectionManager()

    // Update SelectionManager with external focus state - but only when user explicitly focuses
    LaunchedEffect(isContentFocused) {
        Log.d("SearchPage", "🎯 Content focus changed: isContentFocused=$isContentFocused")
        selectionManager.updateContentFocus(isContentFocused)
    }

    // Row position memory - tracks last position in each row by row index
    val rowPositionMemory = remember { mutableMapOf<Int, Int>() }

    // Build rows array based on available sections
    val rows =
        remember(sections) {
            sections.map { it.first } // Extract section titles
        }

    val rowCount = rows.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Initialize selection state only once
    LaunchedEffect(sections.size) {
        Log.d("SearchPage", "🎯 Initializing selection state for ${sections.size} sections")
        if (sections.isNotEmpty() && selectionManager.selectedRowIndex == -1) {
            selectionManager.updateSelection(0, 0)
        }
    }

    // Handle focus changes when user explicitly navigates to content
    LaunchedEffect(selectionManager.selectedRowIndex, isContentFocused) {
        val index = selectionManager.selectedRowIndex
        // Only request focus if user has explicitly focused content and we have valid row
        if (index >= 0 && index < focusRequesters.size && index < rows.size && isContentFocused) {
            try {
                kotlinx.coroutines.delay(100)
                focusRequesters[index].requestFocus()
                Log.d("SearchPage", "🎯 Successfully requested focus on row $index (${rows[index]})")
            } catch (e: Exception) {
                Log.w("SearchPage", "🚨 Failed to request focus on row $index: ${e.message}")
            }
        }
    }

    // Auto-scroll to bring focused row into view when row changes
    LaunchedEffect(selectionManager.selectedRowIndex) {
        val index = selectionManager.selectedRowIndex
        Log.d("SearchPage", "🎯 Row selection changed to $index, isContentFocused=$isContentFocused, scrollState=${scrollState != null}")

        if (scrollState != null && index >= 0 && index < rows.size) {
            try {
                // Wait for focus request to complete first
                kotlinx.coroutines.delay(250)

                // Calculate scroll position to bring row into view
                // Using actual measured scroll positions when rows are properly visible
                val scrollOffset =
                    when (index) {
                        0 -> 0 // Movies - already at top
                        1 -> 641 // TV Shows - exact position when fully visible
                        2 -> 1342 // People - exact position when fully visible
                        else -> index * 641 // Fallback for additional rows
                    }

                Log.d("SearchPage", "🎯 Attempting to scroll to row $index (${rows[index]}) at offset $scrollOffset")
                scrollState.animateScrollTo(scrollOffset)
                Log.d("SearchPage", "🎯 Successfully auto-scrolled to row $index (${rows[index]}) at offset $scrollOffset")
            } catch (e: Exception) {
                Log.w("SearchPage", "🚨 Failed to auto-scroll to row $index: ${e.message}")
            }
        } else {
            Log.d("SearchPage", "🚨 Auto-scroll skipped: scrollState=${scrollState != null}, index=$index, rows.size=${rows.size}")
        }
    }

    // Log scroll position when horizontal navigation occurs (to capture desired positions)
    LaunchedEffect(selectionManager.selectedItemIndex, selectionManager.selectedRowIndex) {
        if (scrollState != null && selectionManager.selectedRowIndex >= 0) {
            val currentScrollPosition = scrollState.value
            val rowIndex = selectionManager.selectedRowIndex
            val itemIndex = selectionManager.selectedItemIndex
            val rowName = rows.getOrNull(rowIndex) ?: "Unknown"

            Log.d(
                "SearchPage",
                "📍 POSITION LOG - Row: $rowIndex ($rowName), Item: $itemIndex, Current Scroll Position: $currentScrollPosition",
            )
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        sections.forEachIndexed { rowIndex, (title, items) ->
            UnifiedMediaRow(
                config =
                    MediaRowConfig(
                        title = title,
                        dataSource = DataSource.RegularList(items),
                        selectedIndex = if (selectionManager.selectedRowIndex == rowIndex) selectionManager.selectedItemIndex else 0,
                        isRowSelected = selectionManager.selectedRowIndex == rowIndex,
                        onSelectionChanged = { newIndex ->
                            if (selectionManager.selectedRowIndex == rowIndex) {
                                selectionManager.updateSelection(rowIndex, newIndex)
                                rowPositionMemory[rowIndex] = newIndex
                                Log.d("SearchPage", "💾 Updated position $newIndex for row $rowIndex ($title)")
                            }
                        },
                        onUpDown = { direction ->
                            val newRowIndex = selectionManager.selectedRowIndex + direction
                            if (newRowIndex >= 0 && newRowIndex < sections.size) {
                                // Save current position
                                rowPositionMemory[selectionManager.selectedRowIndex] = selectionManager.selectedItemIndex

                                // Get target position from memory or use default
                                val newItemIndex = rowPositionMemory[newRowIndex] ?: 0

                                Log.d(
                                    "SearchPage",
                                    "🎯 Row navigation: ${selectionManager.selectedRowIndex}(${rows[selectionManager.selectedRowIndex]}) -> $newRowIndex(${rows[newRowIndex]}), direction=$direction",
                                )
                                selectionManager.updateSelection(newRowIndex, newItemIndex)
                            } else if (newRowIndex < 0) {
                                // Go back to search bar
                                onFocusReturnToSearchBar()
                            }
                        },
                        focusRequester =
                            if (selectionManager.selectedRowIndex == rowIndex && isContentFocused) {
                                focusRequesters.getOrNull(
                                    rowIndex,
                                )
                            } else {
                                null
                            },
                        onContentFocusChanged = { focused ->
                            selectionManager.updateContentFocus(focused)
                            onContentFocusChanged?.invoke(focused)
                        },
                        onLeftBoundary = if (rowIndex == selectionManager.selectedRowIndex) onLeftBoundary else null,
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
            )
        }
    }
}

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
