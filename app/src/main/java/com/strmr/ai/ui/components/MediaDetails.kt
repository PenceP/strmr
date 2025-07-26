package com.strmr.ai.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.strmr.ai.R
import com.strmr.ai.utils.DateFormatter
import androidx.compose.runtime.LaunchedEffect
import com.strmr.ai.utils.resolveImageSource

@Composable
fun MediaDetails(
    title: String?,
    logoUrl: String?,
    year: Int?,
    formattedDate: String? = null, // New parameter for formatted date
    runtime: Int?,
    genres: List<String>?,
    rating: Float?,
    overview: String?,
    cast: List<String>?,
    omdbRatings: com.strmr.ai.data.OmdbResponse? = null,
    modifier: Modifier = Modifier,
    extraContent: @Composable (() -> Unit)? = null,
    onFetchLogo: (() -> Unit)? = null // Callback to fetch logo when missing
) {
    // Trigger logo fetching when logo is missing
    LaunchedEffect(logoUrl, title) {
        if (logoUrl.isNullOrBlank() && !title.isNullOrBlank()) {
            onFetchLogo?.invoke()
        }
    }
    
    Column(
        modifier = modifier
            .padding(8.dp)
            .width(400.dp)
    ) {
        // Title/Logo area - always reserve space for consistent layout
        Box(
            modifier = Modifier
                .height(72.dp)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val resolvedLogoSource = resolveImageSource(logoUrl)
            if (resolvedLogoSource != null) {
                AsyncImage(
                    model = resolvedLogoSource,
                    contentDescription = title,
                    modifier = Modifier
                        .height(72.dp)
                        .fillMaxWidth()
                )
            } else {
                // Show title text as placeholder when logo is not available
                Text(
                    text = title ?: "",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // Extra content (e.g., season/episode)
        
        // Year and runtime
        Row(
            modifier = Modifier.padding(bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            //rating?.let { ratingValue ->
              //  Text(
                //    text = "★ ${String.format("%.1f", ratingValue)}",
                //    color = Color.White.copy(alpha = 0.8f),
                //    fontSize = 16.sp
                //)
            //}
            
            runtime?.let { runtimeValue ->
                if (runtimeValue > 0) {
                    Text(
                        text = runtimeValue.toString() + " min",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            // Display formatted date if available, otherwise fall back to year
            (formattedDate ?: year?.toString())?.let { dateValue ->
                Text(
                    text = dateValue,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            //extraContent?.invoke()
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Genres (max 1 line)
        genres?.let { genreList ->
            Text(
                text = genreList.joinToString(", "),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        // Overview
        
        overview?.let { overviewText ->
            Text(
                text = overviewText,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        // Cast/Credits
        cast?.let { castList ->
            if (castList.isNotEmpty()) {
                Column {
                    Text(
                        text = castList.joinToString(", "),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Ratings row (IMDb, Rotten Tomatoes, Metacritic, Trakt)
        omdbRatings?.let { omdb ->
            val imdb = omdb.imdbRating?.takeIf { it.isNotBlank() }
            val rt = omdb.Ratings?.find { it.Source == "Rotten Tomatoes" }?.Value
            val meta = omdb.Metascore?.takeIf { it.isNotBlank() && it != "N/A" }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!imdb.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = R.drawable.imdb_logo),
                        contentDescription = "IMDb",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = imdb,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (!rt.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = R.drawable.rotten_tomatoes),
                        contentDescription = "Rotten Tomatoes",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rt,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (!meta.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = R.drawable.metacritic_logo),
                        contentDescription = "Metacritic",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$meta%",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                // Add Trakt rating
                rating?.let { traktRating ->
                    Image(
                        painter = painterResource(id = R.drawable.trakt1),
                        contentDescription = "Trakt",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", traktRating),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                    )
                }
            }
        } ?: run {
            // Show only Trakt rating if no OMDb ratings available
            rating?.let { traktRating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.trakt1),
                        contentDescription = "Trakt",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", traktRating),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                    )
                }
            }
        }
        
    }
} 