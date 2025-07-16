@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import com.strmr.ai.ui.theme.Purple40
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.data.OmdbResponse
import com.strmr.ai.data.OmdbRepository
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.EpisodeEntity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import com.strmr.ai.R
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.focusable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.strmr.ai.data.Actor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import com.strmr.ai.ui.components.MediaHero
import com.strmr.ai.ui.components.MediaDetails
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import com.strmr.ai.ui.screens.MediaDetailsType
import com.strmr.ai.utils.DateFormatter
import androidx.compose.material.icons.filled.Visibility

sealed class MediaDetailsType {
    data class Movie(val movie: MovieEntity) : MediaDetailsType()
    data class TvShow(val show: TvShowEntity) : MediaDetailsType()
}

@Composable
fun DetailsPage(
    mediaDetails: MediaDetailsType?,
    omdbRepository: OmdbRepository,
    tvShowRepository: TvShowRepository? = null, // Only needed for TV shows
    onPlay: (season: Int?, episode: Int?) -> Unit = { _, _ -> },
    onAddToCollection: () -> Unit = {},
    cachedSeason: Int? = null,
    cachedEpisode: Int? = null
) {
    if (mediaDetails == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    when (mediaDetails) {
        is MediaDetailsType.Movie -> MovieDetailsView(mediaDetails.movie, omdbRepository, onPlay, onAddToCollection)
        is MediaDetailsType.TvShow -> {
            requireNotNull(tvShowRepository) { "tvShowRepository required for TV shows" }
            TvShowDetailsView(mediaDetails.show, omdbRepository, tvShowRepository, onPlay, onAddToCollection, cachedSeason, cachedEpisode)
        }
    }
}

@Composable
fun MovieDetailsView(
    movie: MovieEntity,
    omdbRepository: OmdbRepository,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onAddToCollection: () -> Unit
) {
    val playButtonFocusRequester = remember { FocusRequester() }
    var omdbRatings by remember(movie.imdbId) { mutableStateOf<OmdbResponse?>(null) }
    var logoUrl by remember { mutableStateOf(movie.logoUrl) }
    val scrollState = rememberScrollState()

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
                omdbRatings = withContext(Dispatchers.IO) {
                    val response = omdbRepository.getOmdbRatings(movie.imdbId)
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

    // Log when omdbRatings state changes
    LaunchedEffect(omdbRatings) {
        Log.d("MovieDetailsView", "🔄 omdbRatings state changed to: $omdbRatings")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Backdrop
        movie.backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero section (top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(start = 48.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
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
                                omdbRatings = omdbRatings
                            )
                        }
                    )
                    RatingsRow(omdbRatings = omdbRatings, traktRating = movie.rating)
                }
            }

            // Lower section (scrollable if needed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                // Buttons row
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth(0.4f).align(Alignment.CenterStart)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val playButtonInteractionSource = remember { MutableInteractionSource() }
                            val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { onPlay(null, null) },
                                modifier = Modifier.weight(1f).height(48.dp).focusRequester(playButtonFocusRequester),
                                interactionSource = playButtonInteractionSource,
                                isFocused = playButtonIsFocused,
                                text = "Play",
                                textColor = Color.White
                            )
                            val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                            val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Trailer */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = trailerButtonInteractionSource,
                                isFocused = trailerButtonIsFocused,
                                text = "Trailer",
                                textColor = Color.White
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                            val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = onAddToCollection,
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = collectionButtonInteractionSource,
                                isFocused = collectionButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark, 
                                    contentDescription = "Collection", 
                                    tint = if (collectionButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watchlist */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = watchlistButtonInteractionSource,
                                isFocused = watchlistButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Queue, 
                                    contentDescription = "Watchlist", 
                                    tint = if (watchlistButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            // --- New Watched/Unwatched Button ---
                            val watchedButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchedButtonIsFocused by watchedButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watched/Unwatched */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = watchedButtonInteractionSource,
                                isFocused = watchedButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = "Watched/Unwatched",
                                    tint = if (watchedButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            // --- End New Button ---
                            val moreButtonInteractionSource = remember { MutableInteractionSource() }
                            val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: More */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = moreButtonInteractionSource,
                                isFocused = moreButtonIsFocused,
                                text = "...",
                                textColor = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                // Actors row
                if (movie.cast.isNotEmpty()) {
                    ActorsRow(
                        actors = movie.cast,
                        modifier = Modifier.fillMaxWidth()
                    )
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
    onAddToCollection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(Modifier.height(24.dp))
        // 1. Logo/title
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = movie.title,
                modifier = Modifier.height(72.dp).padding(bottom = 16.dp)
            )
        } else {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 48.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        // 2. Plot/description
        Text(
            text = movie.overview ?: "",
            color = Color.White,
            fontSize = 16.sp,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Info row (runtime, date, genres)
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("${movie.runtime ?: ""} min", color = Color.Gray, fontSize = 16.sp)
            Spacer(Modifier.width(16.dp))
            Text(DateFormatter.formatMovieDate(movie.releaseDate) ?: "${movie.year}", color = Color.Gray, fontSize = 16.sp)
            Spacer(Modifier.width(16.dp))
            Text(movie.genres?.joinToString() ?: "", color = Color.Gray, fontSize = 16.sp)
        }
        
        // 3. Ratings row
        val rt = omdbRatings?.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value
        val meta = omdbRatings?.Metascore?.takeIf { !it.isNullOrBlank() && it != "N/A" }
        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            if (omdbRatings?.imdbRating != null) {
                Image(painter = painterResource(id = R.drawable.imdb_logo), contentDescription = "IMDb", modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(4.dp))
                Text(omdbRatings?.imdbRating ?: "-", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.width(16.dp))
            }
            if (!meta.isNullOrBlank()) {
                Image(painter = painterResource(id = R.drawable.metacritic_logo), contentDescription = "Metacritic", modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(4.dp))
                Text("$meta%", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.width(16.dp))
            }
            if (!rt.isNullOrBlank()) {
                Image(painter = painterResource(id = R.drawable.rotten_tomatoes), contentDescription = "Rotten Tomatoes", modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(4.dp))
                Text(rt, color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.width(16.dp))
            }
            Image(painter = painterResource(id = R.drawable.trakt1), contentDescription = "Trakt", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(4.dp))
            Text(movie.rating?.let { String.format("%.1f", it) } ?: "-", color = Color.White, fontSize = 18.sp)
        }
        
        // 4. Buttons (max width 40% of screen)
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(0.4f).align(Alignment.CenterStart)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val playButtonInteractionSource = remember { MutableInteractionSource() }
                    val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { onPlay(null, null) },
                        modifier = Modifier.weight(1f).height(48.dp).focusRequester(playButtonFocusRequester),
                        interactionSource = playButtonInteractionSource,
                        isFocused = playButtonIsFocused,
                        text = "Play",
                        textColor = Color.Black.copy(alpha = 0.87f)
                    )
                    val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                    val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: Trailer */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        interactionSource = trailerButtonInteractionSource,
                        isFocused = trailerButtonIsFocused,
                        text = "Trailer",
                        textColor = Color.Black.copy(alpha = 0.87f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                    val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = onAddToCollection,
                        modifier = Modifier.weight(1f).height(48.dp),
                        interactionSource = collectionButtonInteractionSource,
                        isFocused = collectionButtonIsFocused,
                        text = "Collection",
                        textColor = Color.Black.copy(alpha = 0.87f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Bookmark, 
                                contentDescription = "Collection", 
                                tint = if (collectionButtonIsFocused) Color.Black else Color.White
                            )
                            //Spacer(Modifier.width(8.dp))
                            //Text(
                             //   "Collection", 
                             //   fontSize = 16.sp, 
                             //   color = if (collectionButtonIsFocused) Color.Black else Color.White,
                             //   fontWeight = if (collectionButtonIsFocused) FontWeight.Medium else FontWeight.Normal
                            //)
                        }
                    }
                    val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                    val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: Watchlist */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        interactionSource = watchlistButtonInteractionSource,
                        isFocused = watchlistButtonIsFocused,
                        text = "Watchlist",
                        textColor = Color.Black.copy(alpha = 0.87f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Queue, 
                                contentDescription = "Watchlist", 
                                tint = if (watchlistButtonIsFocused) Color.Black else Color.White
                            )
                           //Spacer(Modifier.width(8.dp))
                           //Text(
                            //    "Watchlist", 
                             //   fontSize = 16.sp, 
                             //   color = if (watchlistButtonIsFocused) Color.Black else Color.White,
                             //   fontWeight = if (watchlistButtonIsFocused) FontWeight.Medium else FontWeight.Normal
                            //)
                        }
                    }
                    val moreButtonInteractionSource = remember { MutableInteractionSource() }
                    val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                    FrostedGlassButton(
                        onClick = { /* TODO: More */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        interactionSource = moreButtonInteractionSource,
                        isFocused = moreButtonIsFocused,
                        text = "...",
                        textColor = Color.Black.copy(alpha = 0.87f)
                    )
                }
            }
        }
    }
}

@Composable
fun TvShowDetailsView(
    show: TvShowEntity,
    omdbRepository: OmdbRepository,
    tvShowRepository: TvShowRepository,
    onPlay: (season: Int?, episode: Int?) -> Unit,
    onAddToCollection: () -> Unit,
    cachedSeason: Int? = null,
    cachedEpisode: Int? = null
) {
    val playButtonFocusRequester = remember { FocusRequester() }
    var omdbRatings by remember(show.imdbId) { mutableStateOf<OmdbResponse?>(null) }
    var seasons by remember { mutableStateOf<List<SeasonEntity>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf<Int?>(cachedSeason) }
    var episodes by remember { mutableStateOf<List<EpisodeEntity>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<EpisodeEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

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
                omdbRatings = withContext(Dispatchers.IO) {
                    val response = omdbRepository.getOmdbRatings(show.imdbId)
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

    // Log when omdbRatings state changes
    LaunchedEffect(omdbRatings) {
        Log.d("TvShowDetailsView", "🔄 omdbRatings state changed to: $omdbRatings")
    }

    // Fetch seasons and episodes
    LaunchedEffect(show.tmdbId) {
        try {
            loading = true
            Log.d("TvShowDetailsView", "📡 Fetching seasons for show: ${show.title}")
            val fetchedSeasons = tvShowRepository.getOrFetchSeasons(show.tmdbId)
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
                val fetchedEpisodes = tvShowRepository.getOrFetchEpisodes(show.tmdbId, selectedSeason!!)
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
                val fetchedEpisodes = tvShowRepository.getOrFetchEpisodes(show.tmdbId, selectedSeason!!)
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Backdrop
        show.backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero section (top half)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(start = 48.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
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
                                omdbRatings = omdbRatings
                            )
                        }
                    )
                    RatingsRow(omdbRatings = omdbRatings, traktRating = show.rating)
                }
            }

            // Lower section (scrollable if needed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 48.dp, vertical = 32.dp)
            ) {
                // Buttons row
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth(0.4f).align(Alignment.CenterStart)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val playButtonInteractionSource = remember { MutableInteractionSource() }
                            val playButtonIsFocused by playButtonInteractionSource.collectIsFocusedAsState()
                            val episodeNumber = selectedEpisode?.episodeNumber
                            val playButtonText = if (selectedSeason != null && episodeNumber != null) "Play S${selectedSeason}: E${episodeNumber}" else "Play"
                            Log.d("TvShowDetailsView", "🎯 DEBUG: Play button text: '$playButtonText' (selectedSeason: $selectedSeason, episodeNumber: $episodeNumber)")
                            FrostedGlassButton(
                                onClick = { onPlay(selectedSeason, episodeNumber) },
                                modifier = Modifier.weight(1f).height(48.dp).focusRequester(playButtonFocusRequester),
                                interactionSource = playButtonInteractionSource,
                                isFocused = playButtonIsFocused,
                                text = playButtonText,
                                textColor = Color.White
                            )
                            val trailerButtonInteractionSource = remember { MutableInteractionSource() }
                            val trailerButtonIsFocused by trailerButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Trailer */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = trailerButtonInteractionSource,
                                isFocused = trailerButtonIsFocused,
                                text = "Trailer",
                                textColor = Color.White
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val collectionButtonInteractionSource = remember { MutableInteractionSource() }
                            val collectionButtonIsFocused by collectionButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = onAddToCollection,
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = collectionButtonInteractionSource,
                                isFocused = collectionButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Bookmark, 
                                    contentDescription = "Collection", 
                                    tint = if (collectionButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            val watchlistButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchlistButtonIsFocused by watchlistButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watchlist */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = watchlistButtonInteractionSource,
                                isFocused = watchlistButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Queue, 
                                    contentDescription = "Watchlist", 
                                    tint = if (watchlistButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            // --- New Watched/Unwatched Button ---
                            val watchedButtonInteractionSource = remember { MutableInteractionSource() }
                            val watchedButtonIsFocused by watchedButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: Watched/Unwatched */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = watchedButtonInteractionSource,
                                isFocused = watchedButtonIsFocused,
                                text = "",
                                textColor = Color.White,
                                hasCustomContent = true
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    contentDescription = "Watched/Unwatched",
                                    tint = if (watchedButtonIsFocused) Color.Black else Color.White
                                )
                            }
                            // --- End New Button ---
                            val moreButtonInteractionSource = remember { MutableInteractionSource() }
                            val moreButtonIsFocused by moreButtonInteractionSource.collectIsFocusedAsState()
                            FrostedGlassButton(
                                onClick = { /* TODO: More */ },
                                modifier = Modifier.weight(1f).height(48.dp),
                                interactionSource = moreButtonInteractionSource,
                                isFocused = moreButtonIsFocused,
                                text = "...",
                                textColor = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                // Season/Episode selection UI
                if (loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (seasons.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Season & Episode",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Season selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Season:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                            seasons.forEach { season ->
                                val isSelected = selectedSeason == season.seasonNumber
                                Text(
                                    text = "${season.seasonNumber}",
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clickable { selectedSeason = season.seasonNumber }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .background(
                                            if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Episode selection
                        if (episodes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Episode:",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 16.sp
                                )
                                episodes.take(10).forEach { episode -> // Limit to first 10 episodes for UI
                                    val isSelected = selectedEpisode?.episodeNumber == episode.episodeNumber
                                    Text(
                                        text = "${episode.episodeNumber}",
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clickable { selectedEpisode = episode }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                                if (episodes.size > 10) {
                                    Text(
                                        text = "...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                // Actors row
                if (show.cast.isNotEmpty()) {
                    ActorsRow(
                        actors = show.cast,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ActorsRow(
    actors: List<Actor>,
    modifier: Modifier = Modifier
) {
    if (actors.isEmpty()) return
    Spacer(Modifier.height(14.dp))
    Column(modifier = modifier) {
        Text(
            text = "Actors",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(120.dp)
        ) {
            items(actors.take(15)) { actor ->
                ActorCard(actor = actor)
            }
        }
        
    }
}

@Composable
fun ActorCard(
    actor: Actor,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 200)
    )
    var parentBounds by remember { mutableStateOf<Rect?>(null) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(isFocused, parentBounds) {
        if (isFocused && parentBounds != null) {
            coroutineScope.launch {
                val extra = with(density) { 40.dp.toPx() }
                val rect = Rect(
                    parentBounds!!.left,
                    parentBounds!!.top,
                    parentBounds!!.right,
                    parentBounds!!.bottom + extra
                )
                bringIntoViewRequester.bringIntoView(rect)
            }
        }
    }

    Column(
        modifier = modifier
            .width(80.dp)
            .focusable(interactionSource = interactionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .bringIntoViewRequester(bringIntoViewRequester)
            .onGloballyPositioned { coordinates ->
                parentBounds = coordinates.boundsInParent()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Actor image
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(6.dp)
                )
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!actor.profilePath.isNullOrBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${actor.profilePath}",
                    contentDescription = actor.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = actor.name?.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Actor name
        Text(
            text = actor.name ?: "Unknown",
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // Character name (always shown as a separate line, if available)
        Text(
            text = actor.character ?: "",
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun RatingsRow(
    omdbRatings: OmdbResponse?,
    traktRating: Float?
) {
    // Add comprehensive logging for debugging
    Log.d("RatingsRow", "⭐ RatingsRow composable called")
    Log.d("RatingsRow", "⭐ omdbRatings: $omdbRatings")
    Log.d("RatingsRow", "⭐ traktRating: $traktRating")
    
    val rt = omdbRatings?.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value
    val meta = omdbRatings?.Metascore?.takeIf { !it.isNullOrBlank() && it != "N/A" }
    
    Log.d("RatingsRow", "⭐ IMDb rating: ${omdbRatings?.imdbRating}")
    Log.d("RatingsRow", "⭐ Rotten Tomatoes rating: $rt")
    Log.d("RatingsRow", "⭐ Metacritic rating: $meta")
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (omdbRatings?.imdbRating != null) {
            Log.d("RatingsRow", "⭐ Rendering IMDb rating: ${omdbRatings.imdbRating}")
            Image(painter = painterResource(id = R.drawable.imdb_logo), contentDescription = "IMDb", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(4.dp))
            Text(omdbRatings.imdbRating, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.width(16.dp))
        } else {
            Log.d("RatingsRow", "⭐ IMDb rating is null, not rendering")
        }
        if (!meta.isNullOrBlank()) {
            Log.d("RatingsRow", "⭐ Rendering Metacritic rating: $meta")
            Image(painter = painterResource(id = R.drawable.metacritic_logo), contentDescription = "Metacritic", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(4.dp))
            Text("$meta%", color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.width(16.dp))
        } else {
            Log.d("RatingsRow", "⭐ Metacritic rating is null/blank, not rendering")
        }
        if (!rt.isNullOrBlank()) {
            Log.d("RatingsRow", "⭐ Rendering Rotten Tomatoes rating: $rt")
            Image(painter = painterResource(id = R.drawable.rotten_tomatoes), contentDescription = "Rotten Tomatoes", modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(4.dp))
            Text(rt, color = Color.White, fontSize = 18.sp)
            Spacer(Modifier.width(16.dp))
        } else {
            Log.d("RatingsRow", "⭐ Rotten Tomatoes rating is null/blank, not rendering")
        }
        Log.d("RatingsRow", "⭐ Rendering Trakt rating: $traktRating")
        Image(painter = painterResource(id = R.drawable.trakt1), contentDescription = "Trakt", modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(4.dp))
        Text(traktRating?.let { String.format("%.1f", it) } ?: "-", color = Color.White, fontSize = 18.sp)
    }
} 

@Composable
fun FrostedGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isFocused: Boolean = false,
    text: String = "",
    textColor: Color = Color.White,
    hasCustomContent: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    val cornerRadius = 8.dp
    
    Box(
        modifier = modifier
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Frosted glass background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (isFocused) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(cornerRadius)
                )
        )
        
        // Sharp content layer
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (hasCustomContent) {
                content()
            } else if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = if (isFocused) Color.Black else Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isFocused) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
} 