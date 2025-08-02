@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.strmr.ai.R
import com.strmr.ai.data.Actor
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.database.EpisodeEntity
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.components.CardType
import com.strmr.ai.ui.components.DataSource
import com.strmr.ai.ui.components.DetailsContentData
import com.strmr.ai.ui.components.DetailsContentRow
import com.strmr.ai.ui.components.MediaDetails
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaRowConfig
import com.strmr.ai.ui.components.UnifiedMediaRow
import com.strmr.ai.ui.components.rememberSelectionManager
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.ui.utils.WithFocusProviders
import com.strmr.ai.ui.utils.safeRequestFocus
import com.strmr.ai.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class MediaDetailsType {
    data class Movie(val movie: MovieEntity) : MediaDetailsType()

    data class TvShow(val show: TvShowEntity) : MediaDetailsType()
}

@Composable
fun DetailsPage(
    mediaDetails: MediaDetailsType?,
    viewModel: com.strmr.ai.viewmodel.DetailsViewModel,
    focusMemoryManager: com.strmr.ai.FocusMemoryManager? = null,
    screenKey: String = "",
    onPlay: (season: Int?, episode: Int?) -> Unit = { _, _ -> },
    onAddToCollection: () -> Unit = {},
    onNavigateToSimilar: (String, Int) -> Unit = { _, _ -> }, // New navigation callback
    onTrailer: (String, String) -> Unit = { _, _ -> }, // Trailer navigation callback
    onMoreEpisodes: () -> Unit = {},
    cachedSeason: Int? = null,
    cachedEpisode: Int? = null,
) {
    Log.d(
        "DetailsPage",
        "🎬 DetailsPage composable called with mediaDetails: ${when (mediaDetails) {
            is MediaDetailsType.Movie -> "Movie: ${mediaDetails.movie.title}"
            is MediaDetailsType.TvShow -> "TvShow: ${mediaDetails.show.title}"
            null -> "null"
        }}",
    )

    WithFocusProviders("details") {
        if (mediaDetails == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@WithFocusProviders
        }
        when (mediaDetails) {
            is MediaDetailsType.Movie ->
                MovieDetailsView(
                    mediaDetails.movie,
                    viewModel,
                    onPlay,
                    onAddToCollection,
                    onNavigateToSimilar,
                    onTrailer,
                )
            is MediaDetailsType.TvShow -> {
                TvShowDetailsView(
                    mediaDetails.show,
                    viewModel,
                    onPlay,
                    onAddToCollection,
                    onNavigateToSimilar,
                    onTrailer,
                    onMoreEpisodes,
                    cachedSeason,
                    cachedEpisode,
                    focusMemoryManager,
                    screenKey,
                )
            }
        }
    }
}

@Composable
fun MovieDetailsView(
    movie: MovieEntity,
    viewModel: com.strmr.ai.viewmodel.DetailsViewModel,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onAddToCollection: () -> Unit,
    onNavigateToSimilar: (String, Int) -> Unit,
    onTrailer: (String, String) -> Unit,
) {
    var omdbRatings by remember(movie.imdbId) { mutableStateOf<OmdbResponse?>(null) }
    var logoUrl by remember { mutableStateOf(movie.logoUrl) }
    var similarContent by remember { mutableStateOf<List<SimilarContent>>(emptyList()) }
    val scrollState = rememberScrollState()
    var collection by remember { mutableStateOf<com.strmr.ai.data.database.CollectionEntity?>(null) }

    // Use the unified SelectionManager like HomePage
    val selectionManager = rememberSelectionManager()

    // Row position memory - tracks last position in each row by row index
    val rowPositionMemory = remember { mutableMapOf<Int, Int>() }

    // Build rows array dynamically based on available content
    val rows =
        remember(movie.cast, collection, similarContent) {
            mutableListOf<String>().apply {
                add("buttons") // Row 0: Always present
                if (movie.cast.isNotEmpty()) add("actors") // Row 1: Actors if available
                if (collection != null && collection!!.parts.size > 1) add("collection") // Row 2: Collection if available
                if (similarContent.isNotEmpty()) add("similar") // Row 3: Similar content if available
            }.toList()
        }

    val rowCount = rows.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Initialize focus state - start with buttons row (index 0)
    LaunchedEffect(Unit) {
        Log.d("MovieDetailsView", "🎯 Initializing selection state")
        if (selectionManager.selectedRowIndex != 0) {
            selectionManager.updateSelection(0, 0)
        }
    }

    // Handle focus changes when selectedRowIndex changes
    LaunchedEffect(selectionManager.selectedRowIndex, focusRequesters.size) {
        val index = selectionManager.selectedRowIndex
        if (index >= 0 && index < focusRequesters.size && index < rows.size) {
            try {
                kotlinx.coroutines.delay(100)
                focusRequesters[index].safeRequestFocus("DetailsPage-Row$index")
                Log.d("MovieDetailsView", "🎯 Successfully requested focus on row $index (${rows[index]})")
            } catch (e: Exception) {
                Log.w("MovieDetailsView", "🚨 Failed to request focus on row $index: ${e.message}")
            }
        }
    }

    // Fetch collection if movie belongs to one
    LaunchedEffect(movie.belongsToCollection?.id) {
        val collectionId = movie.belongsToCollection?.id
        Log.d("MovieDetailsView", "🎬 Collection LaunchedEffect triggered")
        Log.d("MovieDetailsView", "🎬 Movie: ${movie.title} (TMDB ID: ${movie.tmdbId})")
        Log.d("MovieDetailsView", "🎬 Movie belongsToCollection: ${movie.belongsToCollection}")
        Log.d("MovieDetailsView", "🎬 Collection ID: $collectionId")
        Log.d("MovieDetailsView", "🎬 Collection Name: ${movie.belongsToCollection?.name}")
        Log.d("MovieDetailsView", "🎬 DetailsViewModel available: true")

        if (collectionId != null) {
            try {
                Log.d("MovieDetailsView", "📡 Fetching collection for ID: $collectionId")
                collection =
                    withContext(Dispatchers.IO) {
                        val fetchedCollection = viewModel.fetchMovieCollection(collectionId)
                        Log.d("MovieDetailsView", "✅ Collection fetched: $fetchedCollection")
                        Log.d("MovieDetailsView", "✅ Collection parts count: ${fetchedCollection?.parts?.size}")
                        fetchedCollection
                    }
                Log.d("MovieDetailsView", "✅ Collection state updated: $collection")
            } catch (e: Exception) {
                Log.e("MovieDetailsView", "❌ Error fetching collection for ID: $collectionId", e)
                collection = null
            }
        } else {
            Log.d("MovieDetailsView", "⚠️ Skipping collection fetch - ID: $collectionId")
            Log.d("MovieDetailsView", "⚠️ Movie belongs_to_collection is null or has no ID")
        }
    }

    // Add comprehensive logging for debugging
    Log.d("MovieDetailsView", "🎬 MovieDetailsView initialized for movie: ${movie.title}")
    Log.d("MovieDetailsView", "🎬 Movie IMDB ID: ${movie.imdbId}")
    Log.d("MovieDetailsView", "🎬 Current omdbRatings state: $omdbRatings")

    // Add LaunchedEffect to fetch OMDb ratings (missing from original implementation)
    LaunchedEffect(movie.imdbId) {
        Log.d("MovieDetailsView", "🚀 LaunchedEffect triggered for IMDB ID: ${movie.imdbId}")
        if (!movie.imdbId.isNullOrBlank()) {
            try {
                Log.d("MovieDetailsView", "📡 Fetching OMDb ratings for: ${movie.imdbId}")
                omdbRatings =
                    withContext(Dispatchers.IO) {
                        val response = viewModel.fetchOmdbRatings(movie.imdbId)
                        Log.d("MovieDetailsView", "✅ OMDb API response received: $response")
                        response
                    }
                Log.d("MovieDetailsView", "✅ OMDb ratings updated in state: $omdbRatings")
            } catch (e: Exception) {
                Log.e("MovieDetailsView", "❌ Error fetching OMDb ratings for ${movie.imdbId}", e)
                omdbRatings = null
            }
        } else {
            Log.w("MovieDetailsView", "⚠️ Movie IMDB ID is null or blank: ${movie.imdbId}")
            omdbRatings = null
        }
    }

    // Fetch similar content
    LaunchedEffect(movie.tmdbId) {
        try {
            Log.d("MovieDetailsView", "📡 Fetching similar movies for: ${movie.title}")
            similarContent =
                withContext(Dispatchers.IO) {
                    val similar = viewModel.fetchSimilarMovies(movie.tmdbId)
                    Log.d("MovieDetailsView", "✅ Similar movies fetched: ${similar.size} items")
                    similar
                }
        } catch (e: Exception) {
            Log.e("MovieDetailsView", "❌ Error fetching similar movies for ${movie.title}", e)
            similarContent = emptyList()
        }
    }

    // Log when omdbRatings state changes
    LaunchedEffect(omdbRatings) {
        Log.d("MovieDetailsView", "🔄 omdbRatings state changed to: $omdbRatings")
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(StrmrConstants.Colors.BACKGROUND_DARK),
    ) {
        // Backdrop
        movie.backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
                alpha = StrmrConstants.Colors.Alpha.LIGHT,
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Hero section (top half)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = movie.title,
                                logoUrl = logoUrl,
                                year = movie.year,
                                runtime = movie.runtime,
                                genres = movie.genres,
                                rating = movie.rating,
                                overview = movie.overview,
                                cast = movie.cast.mapNotNull { it.name },
                                omdbRatings = omdbRatings,
                            )
                        },
                    )
                    RatingsRow(omdbRatings = omdbRatings, traktRating = movie.rating)
                }
            }

            // Lower section (scrollable if needed)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                            vertical = StrmrConstants.Dimensions.SPACING_SECTION,
                        ),
            ) {
                // Buttons row
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.4f)
                                .align(Alignment.CenterStart),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_STANDARD),
                        ) {
                            val playButtonInteractionSource = remember { MutableInteractionSource() }
                            val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { onPlay(null, null) },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = playButtonInteractionSource,
                                isFocused = playButtonIsFocused,
                                text = "Play",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                            val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        try {
                                            val trailerUrl = viewModel.fetchMovieTrailer(movie.tmdbId)
                                            if (trailerUrl != null) {
                                                onTrailer(trailerUrl, movie.title)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MovieDetailsView", "❌ Error fetching trailer", e)
                                        }
                                    }
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                                interactionSource = trailerButtonInteractionSource,
                                isFocused = trailerButtonIsFocused,
                                text = "Trailer",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                        }
                        Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                            event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
                                            selectionManager.selectedRowIndex == 0 && rows.size > 1
                                        ) {
                                            // Navigate from buttons to next available row
                                            val newRowIndex = 1
                                            val newItemIndex = rowPositionMemory[newRowIndex] ?: 0
                                            selectionManager.updateSelection(newRowIndex, newItemIndex)
                                            Log.d(
                                                "MovieDetailsView",
                                                "🎯 Button navigation: moving to row $newRowIndex (${rows[newRowIndex]})",
                                            )
                                            true
                                        } else {
                                            false
                                        }
                                    },
                            horizontalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_STANDARD),
                        ) {
                            val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                            val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = onAddToCollection,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                                interactionSource = collectionButtonInteractionSource,
                                isFocused = collectionButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = "Collection",
                                    tint = if (collectionButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watchlist */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                                interactionSource = watchlistButtonInteractionSource,
                                isFocused = watchlistButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Queue,
                                    contentDescription = "Watchlist",
                                    tint = if (watchlistButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            // --- New Watched/Unwatched Button ---
                            val watchedButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchedButtonIsFocused by watchedButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watched/Unwatched */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                                interactionSource = watchedButtonInteractionSource,
                                isFocused = watchedButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = "Watched/Unwatched",
                                    tint = if (watchedButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            // --- End New Button ---
                            val moreButtonInteractionSource = remember { MutableInteractionSource() }
                            val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: More */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                                interactionSource = moreButtonInteractionSource,
                                isFocused = moreButtonIsFocused,
                                text = "...",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                        }
                    }
                }

                // Focus management is now handled by the unified LaunchedEffect above

                Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                // Render rows dynamically based on the rows array
                for ((index, rowType) in rows.withIndex()) {
                    when (rowType) {
                        "actors" -> {
                            val actorsRowIndex = index
                            ActorsRow(
                                actors = movie.cast,
                                modifier = Modifier.fillMaxWidth(),
                                selectedIndex = if (selectionManager.selectedRowIndex == actorsRowIndex) selectionManager.selectedItemIndex else 0,
                                isRowSelected = selectionManager.selectedRowIndex == actorsRowIndex,
                                onSelectionChanged = { newIndex ->
                                    if (selectionManager.selectedRowIndex == actorsRowIndex) {
                                        selectionManager.updateSelection(actorsRowIndex, newIndex)
                                        rowPositionMemory[actorsRowIndex] = newIndex
                                        Log.d("MovieDetailsView", "💾 Updated position $newIndex for actors row")
                                    }
                                },
                                onUpDown = { direction ->
                                    val newRowIndex = actorsRowIndex + direction
                                    if (newRowIndex >= 0 && newRowIndex < rows.size) {
                                        // Save current position
                                        rowPositionMemory[actorsRowIndex] = selectionManager.selectedItemIndex

                                        // Get target position from memory or use default
                                        val newItemIndex = rowPositionMemory[newRowIndex] ?: 0

                                        Log.d(
                                            "MovieDetailsView",
                                            "🎯 Actor row navigation: $actorsRowIndex(${rows[actorsRowIndex]}) -> $newRowIndex(${rows[newRowIndex]}), direction=$direction",
                                        )
                                        selectionManager.updateSelection(newRowIndex, newItemIndex)
                                    }
                                },
                                focusRequester =
                                    if (selectionManager.selectedRowIndex == actorsRowIndex) {
                                        focusRequesters.getOrNull(
                                            actorsRowIndex,
                                        )
                                    } else {
                                        null
                                    },
                                isContentFocused = selectionManager.selectedRowIndex == actorsRowIndex,
                                onContentFocusChanged = { /* Handled by selectionManager */ },
                            )
                        }
                        "collection" -> {
                            val collectionRowIndex = index
                            val currentCollection = collection
                            Log.d("MovieDetailsView", "🎬 Rendering collection row")
                            if (currentCollection != null && currentCollection.parts.size > 1) {
                                DetailsContentRow(
                                    title = "Part of Collection",
                                    items = currentCollection.parts,
                                    onItemClick = { /* TODO: handle collection movie click */ },
                                    contentMapper = { movie ->
                                        DetailsContentData(
                                            title = movie.title,
                                            posterUrl = if (!movie.poster_path.isNullOrBlank()) "https://image.tmdb.org/t/p/w500${movie.poster_path}" else null,
                                            subtitle = movie.release_date?.take(4), // Extract year
                                            rating = String.format("%.1f", movie.vote_average),
                                        )
                                    },
                                )
                            }
                        }
                        "similar" -> {
                            val similarRowIndex = index
                            DetailsContentRow(
                                title = "Similar ${if (similarContent.firstOrNull()?.mediaType == "movie") "Movies" else "TV Shows"}",
                                items = similarContent,
                                onItemClick = { content ->
                                    onNavigateToSimilar(content.mediaType, content.tmdbId)
                                },
                                contentMapper = { content ->
                                    DetailsContentData(
                                        title = content.title,
                                        posterUrl = content.posterUrl,
                                        subtitle = content.year?.toString(),
                                        rating = content.rating?.let { String.format("%.1f", it) },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    movie: MovieEntity,
    logoUrl: String?,
    omdbRatings: OmdbResponse?,
    playButtonFocusRequester: FocusRequester,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onAddToCollection: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                    end = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                    bottom = StrmrConstants.Dimensions.SPACING_SECTION,
                ),
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
        // 1. Logo/title
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = movie.title,
                modifier =
                    Modifier
                        .height(StrmrConstants.Dimensions.Icons.HUGE)
                        .padding(bottom = StrmrConstants.Dimensions.SPACING_STANDARD),
            )
        } else {
            Text(
                text = movie.title,
                color = StrmrConstants.Colors.TEXT_PRIMARY,
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        // 2. Plot/description
        Text(
            text = movie.overview ?: "",
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            fontSize = 16.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_MEDIUM),
        )

        // Info row (runtime, date, genres)
        Row(modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_STANDARD)) {
            Text("${movie.runtime ?: ""} min", color = StrmrConstants.Colors.TEXT_SECONDARY, fontSize = 16.sp)
            Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
            Text(
                DateFormatter.formatMovieDate(movie.releaseDate) ?: "${movie.year}",
                color = StrmrConstants.Colors.TEXT_SECONDARY,
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
            Text(movie.genres?.joinToString() ?: "", color = StrmrConstants.Colors.TEXT_SECONDARY, fontSize = 16.sp)
        }

        // 3. Ratings row
        val rt = omdbRatings?.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value
        val meta = omdbRatings?.Metascore?.takeIf { !it.isNullOrBlank() && it != "N/A" }
        Row(modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_STANDARD)) {
            if (omdbRatings?.imdbRating != null) {
                Image(
                    painter = painterResource(id = R.drawable.imdb_logo),
                    contentDescription = "IMDb",
                    modifier = Modifier.size(StrmrConstants.Dimensions.Icons.LARGE),
                )
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_TINY))
                Text(omdbRatings?.imdbRating ?: "-", color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
            }
            if (!meta.isNullOrBlank()) {
                Image(
                    painter = painterResource(id = R.drawable.metacritic_logo),
                    contentDescription = "Metacritic",
                    modifier = Modifier.size(StrmrConstants.Dimensions.Icons.LARGE),
                )
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_TINY))
                Text("$meta%", color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
            }
            if (!rt.isNullOrBlank()) {
                Image(
                    painter = painterResource(id = R.drawable.rotten_tomatoes),
                    contentDescription = "Rotten Tomatoes",
                    modifier = Modifier.size(StrmrConstants.Dimensions.Icons.LARGE),
                )
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_TINY))
                Text(rt, color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
                Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
            }
            Image(
                painter = painterResource(id = R.drawable.trakt1),
                contentDescription = "Trakt",
                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.LARGE),
            )
            Spacer(Modifier.width(4.dp))
            Text(movie.rating?.let { String.format("%.1f", it) } ?: "-", color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
        }

        // 4. Buttons (max width 40% of screen)
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(0.4f)
                        .align(Alignment.CenterStart),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val playButtonInteractionSource = remember { MutableInteractionSource() }
                    val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { onPlay(null, null) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .focusRequester(playButtonFocusRequester),
                        interactionSource = playButtonInteractionSource,
                        isFocused = playButtonIsFocused,
                        text = "Play",
                        textColor = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = StrmrConstants.Colors.Alpha.HEAVY),
                    )
                    val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                    val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: Trailer */ },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        interactionSource = trailerButtonInteractionSource,
                        isFocused = trailerButtonIsFocused,
                        text = "Trailer",
                        textColor = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = StrmrConstants.Colors.Alpha.HEAVY),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                    val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = onAddToCollection,
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        interactionSource = collectionButtonInteractionSource,
                        isFocused = collectionButtonIsFocused,
                        text = "Collection",
                        textColor = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = StrmrConstants.Colors.Alpha.HEAVY),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Bookmark,
                                contentDescription = "Collection",
                                tint = if (collectionButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            // Spacer(Modifier.width(8.dp))
                            // Text(
                            //   "Collection",
                            //   fontSize = 16.sp,
                            //   color = if (collectionButtonIsFocused) Color.Black else Color.White,
                            //   fontWeight = if (collectionButtonIsFocused) FontWeight.Medium else FontWeight.Normal
                            // )
                        }
                    }
                    val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                    val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: Watchlist */ },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        interactionSource = watchlistButtonInteractionSource,
                        isFocused = watchlistButtonIsFocused,
                        text = "Watchlist",
                        textColor = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = StrmrConstants.Colors.Alpha.HEAVY),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Queue,
                                contentDescription = "Watchlist",
                                tint = if (watchlistButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            // Spacer(Modifier.width(8.dp))
                            // Text(
                            //    "Watchlist",
                            //   fontSize = 16.sp,
                            //   color = if (watchlistButtonIsFocused) Color.Black else Color.White,
                            //   fontWeight = if (watchlistButtonIsFocused) FontWeight.Medium else FontWeight.Normal
                            // )
                        }
                    }
                    val moreButtonInteractionSource = remember { MutableInteractionSource() }
                    val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: More */ },
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(48.dp),
                        interactionSource = moreButtonInteractionSource,
                        isFocused = moreButtonIsFocused,
                        text = "...",
                        textColor = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = StrmrConstants.Colors.Alpha.HEAVY),
                    )
                }
            }
        }
    }
}

@Composable
fun TvShowDetailsView(
    show: TvShowEntity,
    viewModel: com.strmr.ai.viewmodel.DetailsViewModel,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onAddToCollection: () -> Unit,
    onNavigateToSimilar: (String, Int) -> Unit,
    onTrailer: (String, String) -> Unit,
    onMoreEpisodes: () -> Unit = {},
    cachedSeason: Int? = null,
    cachedEpisode: Int? = null,
    focusMemoryManager: com.strmr.ai.FocusMemoryManager? = null,
    screenKey: String = "",
) {
    // Use the unified SelectionManager like HomePage
    val selectionManager = rememberSelectionManager()

    // Row position memory - tracks last position in each row by row index
    val rowPositionMemory = remember { mutableMapOf<Int, Int>() }
    var omdbRatings by remember(show.imdbId) { mutableStateOf<OmdbResponse?>(null) }
    var seasons by remember { mutableStateOf<List<SeasonEntity>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<Int?>(cachedSeason) }
    var episodes by remember { mutableStateOf<List<EpisodeEntity>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<EpisodeEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var similarContent by remember { mutableStateOf<List<SimilarContent>>(emptyList()) }
    val scrollState = rememberScrollState()

    // Build rows array dynamically based on available content
    val rows =
        remember(show.cast, similarContent) {
            mutableListOf<String>().apply {
                add("buttons") // Row 0: Always present
                if (show.cast.isNotEmpty()) add("actors") // Row 1: Actors if available
                if (similarContent.isNotEmpty()) add("similar") // Row 2: Similar content if available
            }.toList()
        }

    val rowCount = rows.size
    val focusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }

    // Initialize focus state - start with buttons row (index 0)
    LaunchedEffect(Unit) {
        Log.d("TvShowDetailsView", "🎯 Initializing selection state")
        if (selectionManager.selectedRowIndex != 0) {
            selectionManager.updateSelection(0, 0)
        }
    }

    // Handle focus changes when selectedRowIndex changes
    LaunchedEffect(selectionManager.selectedRowIndex, focusRequesters.size) {
        val index = selectionManager.selectedRowIndex
        if (index >= 0 && index < focusRequesters.size && index < rows.size) {
            try {
                kotlinx.coroutines.delay(100)
                focusRequesters[index].safeRequestFocus("DetailsPage-Row$index")
                Log.d("TvShowDetailsView", "🎯 Successfully requested focus on row $index (${rows[index]})")
            } catch (e: Exception) {
                Log.w("TvShowDetailsView", "🚨 Failed to request focus on row $index: ${e.message}")
            }
        }
    }

    // Restore focus when coming back from another screen
    LaunchedEffect(screenKey) {
        if (focusMemoryManager != null && screenKey.isNotEmpty()) {
            val focusMemory = focusMemoryManager.getFocus(screenKey)
            focusMemory?.let { memory ->
                // Use the unified focus system instead of individual requesters
                Log.d("TvShowDetailsView", "Restoring focus from memory: ${memory.lastFocusedItem}")
                focusRequesters.getOrNull(0)?.safeRequestFocus("DetailsPage-DefaultRow") // Default to buttons row
                // Clear the memory after restoring focus
                focusMemoryManager.clearFocus(screenKey)
            }
        }
    }

    // Debug logging for cached values
    Log.d("TvShowDetailsView", "🎯 DEBUG: Show: ${show.title} (TMDB: ${show.tmdbId})")
    Log.d("TvShowDetailsView", "🎯 DEBUG: cachedSeason: $cachedSeason")
    Log.d("TvShowDetailsView", "🎯 DEBUG: cachedEpisode: $cachedEpisode")
    Log.d("TvShowDetailsView", "🎯 DEBUG: selectedSeason: $selectedSeason")
    Log.d("TvShowDetailsView", "🎯 DEBUG: selectedEpisode: ${selectedEpisode?.episodeNumber}")

    // Add comprehensive logging for debugging
    Log.d("TvShowDetailsView", "📺 TvShowDetailsView initialized for show: ${show.title}")
    Log.d("TvShowDetailsView", "📺 Show IMDB ID: ${show.imdbId}")
    Log.d("TvShowDetailsView", "📺 Current omdbRatings state: $omdbRatings")

    // Add LaunchedEffect to fetch OMDb ratings (missing from original implementation)
    LaunchedEffect(show.imdbId) {
        Log.d("TvShowDetailsView", "🚀 LaunchedEffect triggered for IMDB ID: ${show.imdbId}")
        if (!show.imdbId.isNullOrBlank()) {
            try {
                Log.d("TvShowDetailsView", "📡 Fetching OMDb ratings for: ${show.imdbId}")
                omdbRatings =
                    withContext(Dispatchers.IO) {
                        val response = viewModel.fetchOmdbRatings(show.imdbId)
                        Log.d("TvShowDetailsView", "✅ OMDb API response received: $response")
                        response
                    }
                Log.d("TvShowDetailsView", "✅ OMDb ratings updated in state: $omdbRatings")
            } catch (e: Exception) {
                Log.e("TvShowDetailsView", "❌ Error fetching OMDb ratings for ${show.imdbId}", e)
                omdbRatings = null
            }
        } else {
            Log.w("TvShowDetailsView", "⚠️ Show IMDB ID is null or blank: ${show.imdbId}")
            omdbRatings = null
        }
    }

    // Fetch similar content
    LaunchedEffect(show.tmdbId) {
        try {
            Log.d("TvShowDetailsView", "📡 Fetching similar TV shows for: ${show.title}")
            similarContent =
                withContext(Dispatchers.IO) {
                    val similar = viewModel.fetchSimilarTvShows(show.tmdbId)
                    Log.d("TvShowDetailsView", "✅ Similar TV shows fetched: ${similar.size} items")
                    similar
                }
        } catch (e: Exception) {
            Log.e("TvShowDetailsView", "❌ Error fetching similar TV shows for ${show.title}", e)
            similarContent = emptyList()
        }
    }

    // Log when omdbRatings state changes
    LaunchedEffect(omdbRatings) {
        Log.d("TvShowDetailsView", "🔄 omdbRatings state changed to: $omdbRatings")
    }

    // Fetch seasons and episodes
    LaunchedEffect(show.tmdbId) {
        try {
            loading = true
            Log.d("TvShowDetailsView", "📡 Fetching seasons for show: ${show.title}")
            val fetchedSeasons = viewModel.fetchTvShowSeasons(show.tmdbId)
            seasons = fetchedSeasons
            Log.d("TvShowDetailsView", "✅ Fetched ${fetchedSeasons.size} seasons")
            Log.d("TvShowDetailsView", "🎯 DEBUG: Available seasons: ${fetchedSeasons.map { it.seasonNumber }}")

            // Use cached season if available, otherwise auto-select first season
            if (cachedSeason != null && fetchedSeasons.any { it.seasonNumber == cachedSeason }) {
                selectedSeason = cachedSeason
                Log.d("TvShowDetailsView", "🎯 Using cached season: $cachedSeason")
            } else if (fetchedSeasons.isNotEmpty()) {
                selectedSeason = fetchedSeasons.first().seasonNumber
                Log.d("TvShowDetailsView", "🎯 Auto-selected season: $selectedSeason")
            }

            // Fetch episodes for the selected season
            if (selectedSeason != null) {
                val fetchedEpisodes = viewModel.fetchTvShowEpisodes(show.tmdbId, selectedSeason!!)
                episodes = fetchedEpisodes
                Log.d("TvShowDetailsView", "✅ Fetched ${fetchedEpisodes.size} episodes for season $selectedSeason")
                Log.d("TvShowDetailsView", "🎯 DEBUG: Available episodes: ${fetchedEpisodes.map { it.episodeNumber }}")

                // Use cached episode if available, otherwise auto-select first episode
                if (cachedEpisode != null && fetchedEpisodes.any { it.episodeNumber == cachedEpisode }) {
                    selectedEpisode = fetchedEpisodes.find { it.episodeNumber == cachedEpisode }
                    Log.d("TvShowDetailsView", "🎯 Using cached episode: $cachedEpisode")
                } else if (fetchedEpisodes.isNotEmpty()) {
                    selectedEpisode = fetchedEpisodes.first()
                    Log.d("TvShowDetailsView", "🎯 Auto-selected episode: ${selectedEpisode?.episodeNumber}")
                }
            }
        } catch (e: Exception) {
            Log.e("TvShowDetailsView", "❌ Error fetching seasons/episodes for show ${show.title}", e)
        } finally {
            loading = false
        }
    }

    // Fetch episodes when season changes
    LaunchedEffect(selectedSeason) {
        if (selectedSeason != null) {
            try {
                Log.d("TvShowDetailsView", "📡 Fetching episodes for season: $selectedSeason")
                val fetchedEpisodes = viewModel.fetchTvShowEpisodes(show.tmdbId, selectedSeason!!)
                episodes = fetchedEpisodes
                Log.d("TvShowDetailsView", "✅ Fetched ${fetchedEpisodes.size} episodes for season $selectedSeason")

                // Auto-select first episode if available
                if (fetchedEpisodes.isNotEmpty()) {
                    selectedEpisode = fetchedEpisodes.first()
                    Log.d("TvShowDetailsView", "🎯 Auto-selected episode: ${selectedEpisode?.episodeNumber}")
                } else {
                    selectedEpisode = null
                }
            } catch (e: Exception) {
                Log.e("TvShowDetailsView", "❌ Error fetching episodes for season $selectedSeason", e)
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(StrmrConstants.Colors.BACKGROUND_DARK),
    ) {
        // Backdrop
        show.backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
                alpha = StrmrConstants.Colors.Alpha.LIGHT,
            )
        }
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Hero section (top half)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    MediaHero(
                        mediaDetails = {
                            MediaDetails(
                                title = show.title,
                                logoUrl = show.logoUrl,
                                year = show.year,
                                formattedDate = DateFormatter.formatTvShowDateRange(show.firstAirDate, show.lastAirDate),
                                runtime = show.runtime,
                                genres = show.genres,
                                rating = show.rating,
                                overview = selectedEpisode?.overview?.takeIf { !it.isNullOrBlank() } ?: show.overview,
                                cast = show.cast.mapNotNull { it.name },
                                omdbRatings = omdbRatings,
                            )
                        },
                    )
                    RatingsRow(omdbRatings = omdbRatings, traktRating = show.rating)
                }
            }

            // Lower section (scrollable if needed)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                            vertical = StrmrConstants.Dimensions.SPACING_SECTION,
                        ),
            ) {
                // Buttons row
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.4f)
                                .align(Alignment.CenterStart),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_STANDARD),
                        ) {
                            val playButtonInteractionSource = remember { MutableInteractionSource() }
                            val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                            val episodeNumber = selectedEpisode?.episodeNumber
                            val playButtonText = if (selectedSeason != null && episodeNumber != null) "Play S$selectedSeason: E$episodeNumber" else "Play"
                            Log.d(
                                "TvShowDetailsView",
                                "🎯 DEBUG: Play button text: '$playButtonText' (selectedSeason: $selectedSeason, episodeNumber: $episodeNumber)",
                            )
                            FrostedGlassButton(
                                onClick = { onPlay(selectedSeason, episodeNumber) },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = playButtonInteractionSource,
                                isFocused = playButtonIsFocused,
                                text = playButtonText,
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                            val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = {
                                    viewModel.viewModelScope.launch {
                                        try {
                                            val trailerUrl = viewModel.fetchTvShowTrailer(show.tmdbId)
                                            if (trailerUrl != null) {
                                                onTrailer(trailerUrl, show.title)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("TvShowDetailsView", "❌ Error fetching trailer", e)
                                        }
                                    }
                                },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = trailerButtonInteractionSource,
                                isFocused = trailerButtonIsFocused,
                                text = "Trailer",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            val moreEpisodesButtonInteractionSource = remember { MutableInteractionSource() }
                            val moreEpisodesButtonIsFocused by moreEpisodesButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = onMoreEpisodes,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = moreEpisodesButtonInteractionSource,
                                isFocused = moreEpisodesButtonIsFocused,
                                text = "More Episodes",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                        }
                        Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .onKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN &&
                                            event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
                                            selectionManager.selectedRowIndex == 0 && rows.size > 1
                                        ) {
                                            // Navigate from buttons to next available row
                                            val newRowIndex = 1
                                            val newItemIndex = rowPositionMemory[newRowIndex] ?: 0
                                            selectionManager.updateSelection(newRowIndex, newItemIndex)
                                            Log.d(
                                                "TvShowDetailsView",
                                                "🎯 Button navigation: moving to row $newRowIndex (${rows[newRowIndex]})",
                                            )
                                            true
                                        } else {
                                            false
                                        }
                                    },
                            horizontalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_STANDARD),
                        ) {
                            val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                            val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = onAddToCollection,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = collectionButtonInteractionSource,
                                isFocused = collectionButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark,
                                    contentDescription = "Collection",
                                    tint = if (collectionButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watchlist */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = watchlistButtonInteractionSource,
                                isFocused = watchlistButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Queue,
                                    contentDescription = "Watchlist",
                                    tint = if (watchlistButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            // --- New Watched/Unwatched Button ---
                            val watchedButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchedButtonIsFocused by watchedButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watched/Unwatched */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = watchedButtonInteractionSource,
                                isFocused = watchedButtonIsFocused,
                                text = "",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                hasCustomContent = true,
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = "Watched/Unwatched",
                                    tint = if (watchedButtonIsFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                                )
                            }
                            // --- End New Button ---
                            val moreButtonInteractionSource = remember { MutableInteractionSource() }
                            val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: More */ },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT)
                                        .focusRequester(focusRequesters.getOrNull(0) ?: remember { FocusRequester() }),
                                interactionSource = moreButtonInteractionSource,
                                isFocused = moreButtonIsFocused,
                                text = "...",
                                textColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                        }
                    }
                }

                // Focus management is now handled by the unified LaunchedEffect above

                Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                // Render rows dynamically based on the rows array
                for ((index, rowType) in rows.withIndex()) {
                    when (rowType) {
                        "actors" -> {
                            val actorsRowIndex = index
                            ActorsRow(
                                actors = show.cast,
                                modifier = Modifier.fillMaxWidth(),
                                selectedIndex = if (selectionManager.selectedRowIndex == actorsRowIndex) selectionManager.selectedItemIndex else 0,
                                isRowSelected = selectionManager.selectedRowIndex == actorsRowIndex,
                                onSelectionChanged = { newIndex ->
                                    if (selectionManager.selectedRowIndex == actorsRowIndex) {
                                        selectionManager.updateSelection(actorsRowIndex, newIndex)
                                        rowPositionMemory[actorsRowIndex] = newIndex
                                        Log.d("TvShowDetailsView", "💾 Updated position $newIndex for actors row")
                                    }
                                },
                                onUpDown = { direction ->
                                    val newRowIndex = actorsRowIndex + direction
                                    if (newRowIndex >= 0 && newRowIndex < rows.size) {
                                        // Save current position
                                        rowPositionMemory[actorsRowIndex] = selectionManager.selectedItemIndex

                                        // Get target position from memory or use default
                                        val newItemIndex = rowPositionMemory[newRowIndex] ?: 0

                                        Log.d(
                                            "TvShowDetailsView",
                                            "🎯 Actor row navigation: $actorsRowIndex(${rows[actorsRowIndex]}) -> $newRowIndex(${rows[newRowIndex]}), direction=$direction",
                                        )
                                        selectionManager.updateSelection(newRowIndex, newItemIndex)
                                    }
                                },
                                focusRequester =
                                    if (selectionManager.selectedRowIndex == actorsRowIndex) {
                                        focusRequesters.getOrNull(
                                            actorsRowIndex,
                                        )
                                    } else {
                                        null
                                    },
                                isContentFocused = selectionManager.selectedRowIndex == actorsRowIndex,
                                onContentFocusChanged = { /* Handled by selectionManager */ },
                            )
                        }
                        "similar" -> {
                            val similarRowIndex = index
                            Spacer(Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                            DetailsContentRow(
                                title = "Similar ${if (similarContent.firstOrNull()?.mediaType == "movie") "Movies" else "TV Shows"}",
                                items = similarContent,
                                onItemClick = { content ->
                                    onNavigateToSimilar(content.mediaType, content.tmdbId)
                                },
                                contentMapper = { content ->
                                    DetailsContentData(
                                        title = content.title,
                                        posterUrl = content.posterUrl,
                                        subtitle = content.year?.toString(),
                                        rating = content.rating?.let { String.format("%.1f", it) },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActorsRow(
    actors: List<Actor>,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    isRowSelected: Boolean = false,
    onSelectionChanged: (Int) -> Unit = {},
    onUpDown: ((Int) -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    isContentFocused: Boolean = false,
    onContentFocusChanged: ((Boolean) -> Unit)? = null,
) {
    if (actors.isEmpty()) return

    // Simplified ActorsRow using new UnifiedMediaRow pattern
    UnifiedMediaRow(
        config =
            MediaRowConfig(
                title = "Actors",
                dataSource = DataSource.RegularList(actors.take(StrmrConstants.UI.MAX_CAST_ITEMS)),
                cardType = CardType.PORTRAIT,
                itemWidth = 90.dp, // Keep as 90.dp since it's specific for actors
                itemSpacing = StrmrConstants.Dimensions.SPACING_MEDIUM,
                contentPadding = PaddingValues(horizontal = 48.dp),
                itemContent = { actor, isSelected ->
                    ActorCard(actor = actor, isSelected = isSelected)
                },
            ),
        rowIndex = 0, // Actors row is typically the first row in details
        modifier = modifier,
    )
}

@Composable
fun ActorCard(
    actor: Actor,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val baseWidth = StrmrConstants.Dimensions.Cards.BASE_WIDTH
    val baseHeight = StrmrConstants.Dimensions.Cards.BASE_HEIGHT
    val targetWidth = if (isSelected) baseWidth * 1.1f else baseWidth
    val targetHeight = if (isSelected) baseHeight * 1.1f else baseHeight
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = tween(durationMillis = StrmrConstants.Animation.DURATION_INSTANT),
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = StrmrConstants.Animation.DURATION_INSTANT),
    )

    Column(
        modifier =
            modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .border(
                    width = if (isSelected) StrmrConstants.Dimensions.Components.BORDER_WIDTH else 0.dp,
                    color = Color.Transparent,
                    shape = StrmrConstants.Shapes.CORNER_RADIUS_STANDARD,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Actor image
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                    .background(StrmrConstants.Colors.TEXT_SECONDARY),
            contentAlignment = Alignment.Center,
        ) {
            if (!actor.profilePath.isNullOrBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${actor.profilePath}",
                    contentDescription = actor.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = actor.name?.firstOrNull()?.uppercase() ?: "?",
                    color = StrmrConstants.Colors.TEXT_PRIMARY,
                    fontSize = 20.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))

        // Actor name
        Text(
            text = actor.name ?: "Unknown",
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        // Character name
        if (!actor.character.isNullOrBlank()) {
            Text(
                text = actor.character,
                color = StrmrConstants.Colors.TEXT_SECONDARY,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RatingsRow(
    omdbRatings: OmdbResponse?,
    traktRating: Float?,
) {
    // Add comprehensive logging for debugging
    // Log.d("RatingsRow", "⭐ RatingsRow composable called")
    // Log.d("RatingsRow", "⭐ omdbRatings: $omdbRatings")
    // Log.d("RatingsRow", "⭐ traktRating: $traktRating")

    val rt = omdbRatings?.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value
    val meta = omdbRatings?.Metascore?.takeIf { !it.isNullOrBlank() && it != "N/A" }

    // Log.d("RatingsRow", "⭐ IMDb rating: ${omdbRatings?.imdbRating}")
    // Log.d("RatingsRow", "⭐ Rotten Tomatoes rating: $rt")
    // Log.d("RatingsRow", "⭐ Metacritic rating: $meta")

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (omdbRatings?.imdbRating != null) {
            // Log.d("RatingsRow", "⭐ Rendering IMDb rating: ${omdbRatings.imdbRating}")
            Image(painter = painterResource(id = R.drawable.imdb_logo), contentDescription = "IMDb", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(4.dp))
            Text(omdbRatings.imdbRating, color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
            Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
        } else {
            // Log.d("RatingsRow", "⭐ IMDb rating is null, not rendering")
        }
        if (!meta.isNullOrBlank()) {
            // Log.d("RatingsRow", "⭐ Rendering Metacritic rating: $meta")
            Image(
                painter = painterResource(id = R.drawable.metacritic_logo),
                contentDescription = "Metacritic",
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("$meta%", color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
            Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
        } else {
            // Log.d("RatingsRow", "⭐ Metacritic rating is null/blank, not rendering")
        }
        if (!rt.isNullOrBlank()) {
            // Log.d("RatingsRow", "⭐ Rendering Rotten Tomatoes rating: $rt")
            Image(
                painter = painterResource(id = R.drawable.rotten_tomatoes),
                contentDescription = "Rotten Tomatoes",
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(rt, color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
            Spacer(Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))
        } else {
            // Log.d("RatingsRow", "⭐ Rotten Tomatoes rating is null/blank, not rendering")
        }
        // Log.d("RatingsRow", "⭐ Rendering Trakt rating: $traktRating")
        Image(painter = painterResource(id = R.drawable.trakt1), contentDescription = "Trakt", modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(4.dp))
        Text(traktRating?.let { String.format("%.1f", it) } ?: "-", color = StrmrConstants.Colors.TEXT_PRIMARY, fontSize = 18.sp)
    }
}

@Composable
fun FrostedGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isFocused: Boolean = false,
    text: String = "",
    textColor: Color = StrmrConstants.Colors.TEXT_PRIMARY,
    hasCustomContent: Boolean = false,
    content: @Composable () -> Unit = {},
) {
    val cornerRadius = StrmrConstants.Dimensions.SPACING_SMALL

    Box(
        modifier =
            modifier
                .focusable(interactionSource = interactionSource)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // Frosted glass background
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        color =
                            if (isFocused) {
                                StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = StrmrConstants.Colors.Alpha.FOCUS)
                            } else {
                                StrmrConstants.Colors.BACKGROUND_DARK.copy(
                                    alpha = StrmrConstants.Colors.Alpha.MEDIUM,
                                )
                            },
                        shape = RoundedCornerShape(cornerRadius),
                    ),
        )

        // Sharp content layer
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCustomContent) {
                content()
            } else if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = if (isFocused) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY,
                    fontSize = 16.sp,
                    fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}
