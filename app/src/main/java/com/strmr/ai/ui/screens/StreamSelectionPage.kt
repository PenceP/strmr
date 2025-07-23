package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.strmr.ai.data.models.Stream
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
    modifier: Modifier = Modifier
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
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Backdrop with single transparency layer
        backdropUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 8.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Header with logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Back button positioned absolutely
                //IconButton(
                //    onClick = onBackPressed,
                //    modifier = Modifier
                //        .size(48.dp)
                //        .align(Alignment.CenterStart)
                //) {
                //    Icon(
                //        imageVector = Icons.Default.ArrowBack,
                //        contentDescription = "Back",
                //        tint = Color.White,
                //        modifier = Modifier.size(24.dp)
                //    )
                //}
                
                // Logo centered in the box
                logoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = mediaTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(75.dp)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.Center
                    )
                } ?: run {
                    // Fallback if no logo URL
                    Text(
                        text = mediaTitle,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF007AFF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching for streams...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFFF3B30),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error ?: "Unknown error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchStreams(imdbId, type, season, episode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF007AFF)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                
                streams.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No streams found",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try again later or check your Premiumize account",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(streams) { stream ->
                            StreamItem(
                                stream = stream,
                                onClick = { onStreamSelected(stream) }
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
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .focusable(interactionSource = interactionSource)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isFocused) Color.Transparent else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) 
                Color.White.copy(alpha = 0.95f) 
            else 
                Color.Black.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality indicator
            Box(
                modifier = Modifier
                    .background(
                        color = when (stream.displayQuality) {
                            "4K" -> Color(0xFFEFC700)      // Gold
                            "1080p" -> Color(0xFF2196F3)   // Blue
                            "720p" -> Color(0xFFE53E3E)    // Red
                            else -> Color(0xFF9E9E9E)      // Gray for Unknown/CAM/etc
                        },
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stream.displayQuality,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Stream info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Horizontally scrollable title text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stream.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isFocused) FontWeight.Medium else FontWeight.SemiBold,
                        color = if (isFocused) Color.Black else Color.White,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stream.displaySize,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isFocused) Color.Black.copy(alpha = 0.7f) else Color(0xFF888888)
                    )
                    
                    if (stream.seeders != null && stream.seeders > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = if (isFocused) Color(0xFF2E7D32) else Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stream.seeders} seeders",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFocused) Color.Black.copy(alpha = 0.7f) else Color(0xFF888888)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Play icon
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = if (isFocused) Color.Black else Color(0xFF007AFF),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}