package com.strmr.ai.domain.usecase

import android.util.Log
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.domain.repository.ContinueWatchingItem
import com.strmr.ai.viewmodel.HomeMediaItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Use case to convert domain ContinueWatchingItems to HomeMediaItems for the UI
 * This bridges the gap between clean architecture domain models and UI models
 */
class ConvertContinueWatchingToHomeMediaItemsUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository
) {
    companion object {
        private const val TAG = "ConvertContinueWatchingUseCase"
    }

    /**
     * Convert a list of domain ContinueWatchingItems to HomeMediaItems
     * Fetches additional data from repositories to enrich the UI models
     */
    suspend operator fun invoke(items: List<ContinueWatchingItem>): List<HomeMediaItem> = coroutineScope {
        Log.d(TAG, "🔄 Converting ${items.size} continue watching items to HomeMediaItems")
        
        items.mapNotNull { item ->
            try {
                when (item.mediaType) {
                    "movie" -> async {
                        convertMovieItem(item)
                    }
                    "tv" -> async {
                        convertTvShowItem(item)
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Unsupported media type: ${item.mediaType}")
                        null
                    }
                }?.await()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error converting item: ${item.title}", e)
                null
            }
        }.also { result ->
            Log.d(TAG, "✅ Successfully converted ${result.size} items")
        }
    }

    private suspend fun convertMovieItem(item: ContinueWatchingItem): HomeMediaItem.Movie? {
        val movieEntity = movieRepository.getOrFetchMovieWithLogo(item.mediaId)
        return if (movieEntity != null) {
            // Try to get alternative backdrop
            val altBackdropUrl = try {
                val images = movieRepository.getMovieImages(item.mediaId)
                images?.backdrops?.getOrNull(1)?.file_path?.let { path -> 
                    "https://image.tmdb.org/t/p/w780$path" 
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load alt backdrop for movie ${item.mediaId}", e)
                null
            }
            
            HomeMediaItem.Movie(
                movie = movieEntity,
                progress = item.progress,
                altBackdropUrl = altBackdropUrl
            )
        } else {
            Log.w(TAG, "⚠️ Movie not found: ${item.mediaId} - ${item.title}")
            null
        }
    }

    private suspend fun convertTvShowItem(item: ContinueWatchingItem): HomeMediaItem.TvShow? {
        val tvShowEntity = tvShowRepository.getOrFetchTvShowWithLogo(item.mediaId)
        return if (tvShowEntity != null) {
            // For TV shows, we need to determine which episode the user was watching
            // This information should ideally come from the ContinueWatchingItem
            // For now, we'll create a basic TV show item without episode details
            HomeMediaItem.TvShow(
                show = tvShowEntity,
                progress = item.progress,
                episodeImageUrl = null,
                season = null,
                episode = null,
                episodeOverview = null,
                episodeAirDate = null,
                isNextEpisode = false
            )
        } else {
            Log.w(TAG, "⚠️ TV show not found: ${item.mediaId} - ${item.title}")
            null
        }
    }
}