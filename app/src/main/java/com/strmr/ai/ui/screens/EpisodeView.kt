package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.EpisodeEntity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.key.onKeyEvent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.strmr.ai.utils.DateFormatter
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import com.strmr.ai.ui.theme.StrmrConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.isActive
import com.strmr.ai.utils.resolveImageSource

@Composable
fun EpisodeView(
    show: TvShowEntity,
    viewModel: com.strmr.ai.viewmodel.DetailsViewModel,
    onEpisodeClick: (season: Int, episode: Int) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    initialSeason: Int? = null,
    initialEpisode: Int? = null
) {
    var seasons by remember { mutableStateOf<List<SeasonEntity>>(emptyList()) }
    var selectedSeasonIndex by remember { mutableStateOf(0) }
    var selectedEpisodeIndex by remember { mutableStateOf(0) }
    var episodes by remember { mutableStateOf<List<EpisodeEntity>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var isSeasonSelectorFocused by remember { mutableStateOf(false) }
    var isEpisodeRowFocused by remember { mutableStateOf(true) }

    val episodeListState = rememberLazyListState()
    val seasonListState = rememberLazyListState()
    val seasonFocusRequester = remember { FocusRequester() }
    val episodeFocusRequester = remember { FocusRequester() }

    // Fetch seasons and episodes
    LaunchedEffect(show.tmdbId) {
        try {
            loading = true
            Log.d(
                "EpisodeView",
                " Fetching seasons for show: ${show.title} (tmdbId: ${show.tmdbId})"
            )
            val fetchedSeasons = viewModel.getSeasons(show.tmdbId)
            seasons = fetchedSeasons
            Log.d("EpisodeView", " Fetched ${fetchedSeasons.size} seasons for ${show.title}")
            fetchedSeasons.forEachIndexed { index, season ->
                Log.d(
                    "EpisodeView",
                    "Season $index: Season ${season.seasonNumber} - ${season.name} (${season.episodeCount} episodes)"
                )
            }

            // Set initial season
            if (initialSeason != null) {
                val seasonIndex = fetchedSeasons.indexOfFirst { it.seasonNumber == initialSeason }
                if (seasonIndex >= 0) {
                    selectedSeasonIndex = seasonIndex
                }
            }

            // Fetch episodes for selected season
            if (fetchedSeasons.isNotEmpty()) {
                val selectedSeason = fetchedSeasons[selectedSeasonIndex]
                val fetchedEpisodes =
                    viewModel.getEpisodes(show.tmdbId, selectedSeason.seasonNumber)
                episodes = fetchedEpisodes
                Log.d(
                    "EpisodeView",
                    " Fetched ${fetchedEpisodes.size} episodes for season ${selectedSeason.seasonNumber}"
                )

                // Set initial episode
                if (initialEpisode != null) {
                    val episodeIndex =
                        fetchedEpisodes.indexOfFirst { it.episodeNumber == initialEpisode }
                    if (episodeIndex >= 0) {
                        selectedEpisodeIndex = episodeIndex
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EpisodeView", " Error fetching seasons/episodes for show ${show.title}", e)
        } finally {
            loading = false
        }
    }

    // Fetch episodes when season selection changes
    LaunchedEffect(selectedSeasonIndex) {
        if (seasons.isNotEmpty() && selectedSeasonIndex < seasons.size) {
            try {
                val selectedSeason = seasons[selectedSeasonIndex]
                Log.d(
                    "EpisodeView",
                    " Fetching episodes for season: ${selectedSeason.seasonNumber}"
                )

                // Use the current coroutine scope for the API call
                val fetchedEpisodes = withContext(Dispatchers.IO) {
                    viewModel.getEpisodes(show.tmdbId, selectedSeason.seasonNumber)
                }

                // Only update if this coroutine wasn't cancelled
                if (this@LaunchedEffect.isActive) {
                    episodes = fetchedEpisodes
                    selectedEpisodeIndex = 0 // Reset to first episode when changing seasons
                    Log.d(
                        "EpisodeView",
                        " Fetched ${fetchedEpisodes.size} episodes for season ${selectedSeason.seasonNumber}"
                    )
                }
            } catch (e: Exception) {
                if (this@LaunchedEffect.isActive) { // Only log if not cancelled
                    Log.e(
                        "EpisodeView",
                        " Error fetching episodes for season ${seasons[selectedSeasonIndex].seasonNumber}",
                        e
                    )
                    episodes = emptyList()
                }
            }
        }
    }

    // Auto-scroll to selected episode
    LaunchedEffect(selectedEpisodeIndex) {
        if (episodes.isNotEmpty() && selectedEpisodeIndex in 0 until episodes.size) {
            episodeListState.animateScrollToItem(selectedEpisodeIndex)
        }
    }

    // Auto-scroll to selected season
    LaunchedEffect(selectedSeasonIndex) {
        if (seasons.isNotEmpty() && selectedSeasonIndex in 0 until seasons.size) {
            seasonListState.animateScrollToItem(selectedSeasonIndex)
        }
    }

    /**
     * Focus management: Safely request focus only after FocusRequesters are initialized and composition is complete.
     */
    LaunchedEffect(episodes, isSeasonSelectorFocused, isEpisodeRowFocused) {
        // Add a small delay to ensure composables are fully initialized
        kotlinx.coroutines.delay(100)

        if (episodes.isNotEmpty() && !isSeasonSelectorFocused && isEpisodeRowFocused) {
            try {
                episodeFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w("EpisodeView", "Failed to request episode focus: ${e.message}")
            }
        }
    }

    LaunchedEffect(seasons, isSeasonSelectorFocused) {
        // Add a small delay to ensure composables are fully initialized
        kotlinx.coroutines.delay(100)

        if (seasons.isNotEmpty() && isSeasonSelectorFocused) {
            try {
                seasonFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.w("EpisodeView", "Failed to request season focus: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StrmrConstants.Colors.BACKGROUND_DARK)
    ) {
        // Backdrop
        show.backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = StrmrConstants.Blur.RADIUS_STANDARD),
                contentScale = ContentScale.Crop,
                alpha = StrmrConstants.Colors.Alpha.LIGHT
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StrmrConstants.Colors.TEXT_PRIMARY)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 0.dp,
                        end = 0.dp,
                        top = 40.dp
                    )
            ) {
                // Header with show title and season count
                Column(
                    modifier = Modifier.padding(
                        start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                        bottom = 40.dp
                    )
                ) {
                    // Show title or logo
                    val resolvedLogoSource = resolveImageSource(show.logoUrl)
                    if (resolvedLogoSource != null) {
                        AsyncImage(
                            model = resolvedLogoSource,
                            contentDescription = show.title,
                            modifier = Modifier
                                .height(72.dp)
                                .padding(bottom = 8.dp)
                        )
                    } else {
                        Text(
                            text = show.title,
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = "${seasons.size} Seasons ${episodes.size} Episodes",
                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                // Season selector - horizontal scrolling aligned with episodes
                if (seasons.isNotEmpty()) {
                    LazyRow(
                        state = seasonListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                            .focusRequester(seasonFocusRequester)
                            .focusable()
                            .onKeyEvent { event ->
                                when (event.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedSeasonIndex > 0) {
                                                selectedSeasonIndex--
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedSeasonIndex < seasons.size - 1) {
                                                selectedSeasonIndex++
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            isSeasonSelectorFocused = false
                                            isEpisodeRowFocused = true
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_BACK -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            onBack()
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            },
                        contentPadding = PaddingValues(
                            start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                            end = 40.dp
                        )
                    ) {
                        itemsIndexed(seasons) { index, season ->
                            SeasonButton(
                                season = season,
                                isSelected = index == selectedSeasonIndex,
                                isFocused = isSeasonSelectorFocused && index == selectedSeasonIndex,
                                onClick = {
                                    selectedSeasonIndex = index
                                    isSeasonSelectorFocused = true
                                    isEpisodeRowFocused = false
                                }
                            )
                        }
                    }
                }

                // Episodes section
                // Removed the "Episodes" text label

                // Episodes row - full width
                Column {
                    LazyRow(
                        state = episodeListState,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(if (isEpisodeRowFocused) episodeFocusRequester else FocusRequester())
                            .focusable()
                            .onKeyEvent { event ->
                                when (event.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedEpisodeIndex > 0) {
                                                selectedEpisodeIndex--
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedEpisodeIndex < episodes.size - 1) {
                                                selectedEpisodeIndex++
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            isEpisodeRowFocused = false
                                            isSeasonSelectorFocused = true
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            val selectedSeason = seasons[selectedSeasonIndex]
                                            val selectedEpisode = episodes[selectedEpisodeIndex]
                                            onEpisodeClick(
                                                selectedSeason.seasonNumber,
                                                selectedEpisode.episodeNumber
                                            )
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_BACK -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            onBack()
                                        }
                                        true
                                    }

                                    else -> false
                                }
                            },
                        contentPadding = PaddingValues(
                            start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                            end = 40.dp
                        )
                    ) {
                        itemsIndexed(episodes) { index, episode ->
                            EpisodeCardCompact(
                                episode = episode,
                                isSelected = index == selectedEpisodeIndex,
                                isFocused = isEpisodeRowFocused && index == selectedEpisodeIndex,
                                onClick = {
                                    selectedEpisodeIndex = index
                                    isEpisodeRowFocused = true
                                    isSeasonSelectorFocused = false
                                }
                            )
                        }
                    }

                    // Description area for focused episode only
                    Spacer(modifier = Modifier.height(16.dp))
                    if (episodes.isNotEmpty() && selectedEpisodeIndex < episodes.size) {
                        val selectedEpisode = episodes[selectedEpisodeIndex]
                        selectedEpisode.overview?.let { overview ->
                            if (overview.isNotBlank()) {
                                Text(
                                    text = overview,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(
                                        start = StrmrConstants.Dimensions.Icons.EXTRA_LARGE,
                                        end = StrmrConstants.Dimensions.Icons.EXTRA_LARGE
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonButton(
    season: SeasonEntity,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonIsFocused by interactionSource.collectIsFocusedAsState()
    val actuallyFocused = isFocused || buttonIsFocused

    val backgroundColor = when {
        isSelected && actuallyFocused -> StrmrConstants.Colors.TEXT_PRIMARY // Full white when selected and focused
        isSelected -> StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = 0.7f) // Less grey when selected but not focused
        actuallyFocused -> StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = 0.2f)
        else -> Color.Gray.copy(alpha = 0.4f) // Grey color for non-selected buttons
    }
    
    val textColor = if (isSelected) StrmrConstants.Colors.BACKGROUND_DARK else StrmrConstants.Colors.TEXT_PRIMARY

    Box(
        modifier = Modifier
            .width(100.dp)
            .height(40.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .graphicsLayer {
                alpha = if (actuallyFocused) 1f else 0.65f
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Season ${season.seasonNumber}",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun EpisodeCardCompact(
    episode: EpisodeEntity,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val cardIsFocused by interactionSource.collectIsFocusedAsState()
    val actuallyFocused = isFocused || cardIsFocused

    Column(
        modifier = Modifier
            .width(200.dp)
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .padding(end = 20.dp)
            .graphicsLayer {
                alpha = if (actuallyFocused) 1f else 0.65f
            }
    ) {
        // Episode thumbnail with play icon and runtime
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = 0.6f))
                .let { modifier ->
                    if (actuallyFocused) {
                        modifier.border(
                            width = 3.dp,
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        modifier
                    }
                }
        ) {
            // Episode still/thumbnail
            if (!episode.stillUrl.isNullOrBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w500${episode.stillUrl}",
                    contentDescription = episode.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Play icon overlay
            if (actuallyFocused) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(
                            color = StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = 0.9f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = StrmrConstants.Colors.BACKGROUND_DARK,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Runtime overlay
            episode.runtime?.let { runtime ->
                if (runtime > 0 && actuallyFocused) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(
                                color = StrmrConstants.Colors.BACKGROUND_DARK.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${runtime}m",
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Episode number and title
        Text(
            text = "${episode.episodeNumber}. ${episode.name ?: "Episode ${episode.episodeNumber}"}",
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Air date and rating row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Air date (left side)
            episode.airDate?.let { airDate ->
                DateFormatter.formatEpisodeDate(airDate)?.let { formattedDate ->
                    Text(
                        text = formattedDate,
                        color = if (actuallyFocused) Color(0xFFFAFAFA) else StrmrConstants.Colors.TEXT_SECONDARY,
                        fontSize = 12.sp
                    )
                }
            } ?: Spacer(modifier = Modifier.width(1.dp)) // Placeholder when no air date

            // Rating (right side, aligned with thumbnail edge)
            episode.rating?.let { rating ->
                if (rating > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = String.format("%.1f", rating),
                            color = if (actuallyFocused) Color(0xFFFAFAFA) else StrmrConstants.Colors.TEXT_SECONDARY,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "★",
                            color = if (actuallyFocused) Color(0xFFFAFAFA) else StrmrConstants.Colors.TEXT_SECONDARY.copy(
                                alpha = 0.7f
                            ),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}