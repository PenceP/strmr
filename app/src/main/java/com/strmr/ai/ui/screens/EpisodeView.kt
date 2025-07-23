package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.strmr.ai.utils.DateFormatter
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider

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
    var isSeasonPanelFocused by remember { mutableStateOf(true) }

    val seasonListState = rememberLazyListState()
    val episodeListState = rememberLazyListState()
    val seasonFocusRequester = remember { FocusRequester() }
    val episodeFocusRequester = remember { FocusRequester() }

    // Fetch seasons and episodes
    LaunchedEffect(show.tmdbId) {
        try {
            loading = true
            Log.d("EpisodeView", "📡 Fetching seasons for show: ${show.title}")
            val fetchedSeasons = viewModel.getSeasons(show.tmdbId)
            seasons = fetchedSeasons
            Log.d("EpisodeView", "✅ Fetched ${fetchedSeasons.size} seasons")

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
                    "✅ Fetched ${fetchedEpisodes.size} episodes for season ${selectedSeason.seasonNumber}"
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
            Log.e("EpisodeView", "❌ Error fetching seasons/episodes for show ${show.title}", e)
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
                    "📡 Fetching episodes for season: ${selectedSeason.seasonNumber}"
                )
                val fetchedEpisodes =
                    viewModel.getEpisodes(show.tmdbId, selectedSeason.seasonNumber)
                episodes = fetchedEpisodes
                selectedEpisodeIndex = 0 // Reset to first episode when changing seasons
                Log.d(
                    "EpisodeView",
                    "✅ Fetched ${fetchedEpisodes.size} episodes for season ${selectedSeason.seasonNumber}"
                )
            } catch (e: Exception) {
                Log.e(
                    "EpisodeView",
                    "❌ Error fetching episodes for season ${seasons[selectedSeasonIndex].seasonNumber}",
                    e
                )
                episodes = emptyList()
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
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 12.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp)
            ) {
                // Left side - Seasons
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = show.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    LazyColumn(
                        state = seasonListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .focusRequester(if (isSeasonPanelFocused) seasonFocusRequester else FocusRequester())
                            .onKeyEvent { event ->
                                when (event.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedSeasonIndex > 0) {
                                                selectedSeasonIndex--
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            if (selectedSeasonIndex < seasons.size - 1) {
                                                selectedSeasonIndex++
                                            }
                                        }
                                        true
                                    }

                                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                            isSeasonPanelFocused = false
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
                            }
                    ) {
                        items(seasons) { season ->
                            val isSelected = seasons.indexOf(season) == selectedSeasonIndex
                            SeasonItem(
                                season = season,
                                isSelected = isSelected,
                                isFocused = isSeasonPanelFocused && isSelected,
                                onClick = {
                                    selectedSeasonIndex = seasons.indexOf(season)
                                    isSeasonPanelFocused = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.width(32.dp))

                // Right side - Episodes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    if (seasons.isNotEmpty() && selectedSeasonIndex < seasons.size) {
                        val selectedSeason = seasons[selectedSeasonIndex]
                        Text(
                            text = "Season ${selectedSeason.seasonNumber}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (episodes.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No episodes available",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                state = episodeListState,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .focusRequester(if (!isSeasonPanelFocused) episodeFocusRequester else FocusRequester())
                                    .onKeyEvent { event ->
                                        when (event.nativeKeyEvent.keyCode) {
                                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    if (selectedEpisodeIndex > 0) {
                                                        selectedEpisodeIndex--
                                                    }
                                                }
                                                true
                                            }

                                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    if (selectedEpisodeIndex < episodes.size - 1) {
                                                        selectedEpisodeIndex++
                                                    }
                                                }
                                                true
                                            }

                                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    isSeasonPanelFocused = true
                                                }
                                                true
                                            }

                                            android.view.KeyEvent.KEYCODE_DPAD_CENTER, android.view.KeyEvent.KEYCODE_ENTER -> {
                                                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                                    val selectedSeason =
                                                        seasons[selectedSeasonIndex]
                                                    val selectedEpisode =
                                                        episodes[selectedEpisodeIndex]
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
                                    }
                            ) {
                                items(episodes) { episode ->
                                    val isSelected =
                                        episodes.indexOf(episode) == selectedEpisodeIndex
                                    EpisodeItem(
                                        episode = episode,
                                        isSelected = isSelected,
                                        isFocused = !isSeasonPanelFocused && isSelected,
                                        onClick = {
                                            selectedEpisodeIndex = episodes.indexOf(episode)
                                            val selectedSeason = seasons[selectedSeasonIndex]
                                            onEpisodeClick(
                                                selectedSeason.seasonNumber,
                                                episode.episodeNumber
                                            )
                                        }
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
private fun SeasonItem(
    season: SeasonEntity,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isFocused -> Color.White.copy(alpha = 0.2f)
        isSelected -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "Season ${season.seasonNumber}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
            if (season.episodeCount > 0) {
                Text(
                    text = "${season.episodeCount} episodes",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EpisodeItem(
    episode: EpisodeEntity,
    isSelected: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isFocused -> Color.White.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Episode screenshot
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            if (!episode.stillUrl.isNullOrBlank()) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w300${episode.stillUrl}",
                    contentDescription = episode.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = "E${episode.episodeNumber}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Episode details
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Episode number and name
            Text(
                text = "${episode.episodeNumber}. ${episode.name ?: "Episode ${episode.episodeNumber}"}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Runtime and air date
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                episode.runtime?.let { runtime ->
                    if (runtime > 0) {
                        Text(
                            text = "${runtime}m",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }

                episode.airDate?.let { airDate ->
                    DateFormatter.formatEpisodeDate(airDate)?.let { formattedDate ->
                        Text(
                            text = formattedDate,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Episode overview
            episode.overview?.let { overview ->
                if (overview.isNotBlank()) {
                    Text(
                        text = overview,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}