package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strmr.ai.data.models.Stream
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.StreamSelectionViewModel

@Composable
fun StreamSelectionPage(
    mediaTitle: String,
    imdbId: String,
    type: String,
    backdropUrl: String? = null,
    logoUrl: String? = null,
    season: Int? = null,
    episode: Int? = null,
    onBackPressed: () -> Unit,
    onStreamSelected: (Stream) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: StreamSelectionViewModel = hiltViewModel()

    val streams by viewModel.streams.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Fetch streams when the page loads
    LaunchedEffect(imdbId, type, season, episode) {
        viewModel.fetchStreams(imdbId, type, season, episode)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // Backdrop with single transparency layer
        backdropUrl?.let {
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(StrmrConstants.Dimensions.SPACING_SECTION),
        ) {
            // Header with logo
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(StrmrConstants.Dimensions.Components.HEADER_HEIGHT),
            ) {
                // Back button positioned absolutely
                // IconButton(
                //    onClick = onBackPressed,
                //    modifier = Modifier
                //        .size(48.dp)
                //        .align(Alignment.CenterStart)
                // ) {
                //    Icon(
                //        imageVector = Icons.Default.ArrowBack,
                //        contentDescription = "Back",
                //        tint = Color.White,
                //        modifier = Modifier.size(24.dp)
                //    )
                // }

                // Logo centered in the box
                logoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = mediaTitle,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(StrmrConstants.Dimensions.Components.LOGO_HEIGHT)
                                .align(Alignment.Center),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center,
                    )
                } ?: run {
                    // Fallback if no logo URL
                    Text(
                        text = mediaTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }

            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                color = StrmrConstants.Colors.PRIMARY_BLUE,
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.EXTRA_LARGE),
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                            Text(
                                text = "Searching for streams...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = StrmrConstants.Colors.ERROR_RED,
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.EXTRA_LARGE),
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = StrmrConstants.Colors.ERROR_RED,
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                            Button(
                                onClick = { viewModel.fetchStreams(imdbId, type, season, episode) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = StrmrConstants.Colors.PRIMARY_BLUE,
                                    ),
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }

                streams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = StrmrConstants.Colors.TEXT_SECONDARY,
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.EXTRA_LARGE),
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                            Text(
                                text = "No streams found",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = StrmrConstants.Colors.TEXT_PRIMARY,
                            )
                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                            Text(
                                text = "Try again later or check your Premiumize account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StrmrConstants.Colors.TEXT_SECONDARY,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_MEDIUM),
                    ) {
                        items(streams) { stream ->
                            StreamItem(
                                stream = stream,
                                onClick = { onStreamSelected(stream) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamItem(
    stream: Stream,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .focusable(interactionSource = interactionSource)
                .clickable { onClick() }
                .border(
                    width = StrmrConstants.Dimensions.Components.BORDER_WIDTH,
                    color =
                        if (isFocused) {
                            Color.Transparent
                        } else {
                            StrmrConstants.Colors.TEXT_PRIMARY.copy(
                                alpha = StrmrConstants.Colors.Alpha.SUBTLE,
                            )
                        },
                    shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isFocused) {
                        StrmrConstants.Colors.TEXT_PRIMARY.copy(alpha = StrmrConstants.Colors.Alpha.FOCUS)
                    } else {
                        Color.Black.copy(alpha = StrmrConstants.Colors.Alpha.MEDIUM)
                    },
            ),
        shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
        elevation = CardDefaults.cardElevation(defaultElevation = StrmrConstants.Dimensions.Elevation.NONE),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(StrmrConstants.Dimensions.SPACING_LARGE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Quality indicator
            Box(
                modifier =
                    Modifier
                        .background(
                            color =
                                when (stream.displayQuality) {
                                    "4K" -> StrmrConstants.Colors.Quality.GOLD_4K
                                    "1080p" -> StrmrConstants.Colors.Quality.BLUE_1080P
                                    "720p" -> StrmrConstants.Colors.Quality.RED_720P
                                    else -> StrmrConstants.Colors.Quality.GRAY_UNKNOWN
                                },
                            shape = StrmrConstants.Shapes.CORNER_RADIUS_SMALL,
                        )
                        .padding(horizontal = StrmrConstants.Dimensions.SPACING_SMALL, vertical = StrmrConstants.Dimensions.SPACING_TINY),
            ) {
                Text(
                    text = stream.displayQuality,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StrmrConstants.Colors.TEXT_PRIMARY,
                    fontSize = StrmrConstants.Typography.TEXT_SIZE_CAPTION,
                )
            }

            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))

            // Stream info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // Horizontally scrollable title text
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stream.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isFocused) FontWeight.Medium else FontWeight.SemiBold,
                        color = if (isFocused) Color.Black else StrmrConstants.Colors.TEXT_PRIMARY,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_TINY))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stream.displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (isFocused) {
                                Color.Black.copy(
                                    alpha = StrmrConstants.Colors.Alpha.MEDIUM,
                                )
                            } else {
                                StrmrConstants.Colors.TEXT_SECONDARY
                            },
                    )

                    if (stream.seeders != null && stream.seeders > 0) {
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (isFocused) StrmrConstants.Colors.SUCCESS_GREEN_DARK else StrmrConstants.Colors.SUCCESS_GREEN,
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.TINY),
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_TINY))
                        Text(
                            text = "${stream.seeders} seeders",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (isFocused) {
                                    Color.Black.copy(
                                        alpha = StrmrConstants.Colors.Alpha.MEDIUM,
                                    )
                                } else {
                                    StrmrConstants.Colors.TEXT_SECONDARY
                                },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))

            // Play icon
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isFocused) Color.Black else StrmrConstants.Colors.PRIMARY_BLUE,
                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.STANDARD),
            )
        }
    }
}
