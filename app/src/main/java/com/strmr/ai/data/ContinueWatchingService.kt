package com.strmr.ai.data

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that implements proper Continue Watching logic according to Trakt API best practices.
 * 
 * Uses combination of:
 * - /sync/playback - for items currently being watched (with progress)
 * - /sync/history - for recent watch activity  
 * - /shows/:id/progress/watched - for finding next episodes
 * 
 * Returns movies with progress + TV shows with progress + TV shows with next episodes,
 * all ordered by most recently watched.
 */
@Singleton
class ContinueWatchingService @Inject constructor() {
    
    /**
     * Get comprehensive continue watching list
     */
    suspend fun getContinueWatching(authService: TraktAuthenticatedApiService): List<ContinueWatchingItem> {
        try {
            Log.d("ContinueWatching", "🎬 Building comprehensive continue watching list...")
            
            // 1. Get currently in-progress items
            val playbackItems = authService.getPlayback()
            Log.d("ContinueWatching", "📺 Found ${playbackItems.size} in-progress items")
            
            // 2. Get recent watch history to find potential next episodes
            val historyItems = authService.getHistory(limit = 100)
            Log.d("ContinueWatching", "📅 Found ${historyItems.size} recent history items")
            
            val continueWatchingItems = mutableListOf<ContinueWatchingItem>()
            val processedShows = mutableSetOf<Int>() // Track shows we've already processed
            
            // 3. Process in-progress items first (highest priority)
            playbackItems.forEach { playback ->
                when (playback.type) {
                    "movie" -> {
                        playback.movie?.let { movie ->
                            continueWatchingItems.add(
                                ContinueWatchingItem(
                                    type = "movie",
                                    lastWatchedAt = playback.paused_at,
                                    progress = playback.progress,
                                    movie = movie
                                )
                            )
                            Log.d("ContinueWatching", "🎬 Added in-progress movie: ${movie.title}")
                        }
                    }
                    "episode" -> {
                        playback.show?.let { show ->
                            playback.episode?.let { episode ->
                                show.ids.trakt?.let { showId ->
                                    processedShows.add(showId)
                                }
                                continueWatchingItems.add(
                                    ContinueWatchingItem(
                                        type = "episode",
                                        lastWatchedAt = playback.paused_at,
                                        progress = playback.progress,
                                        show = show,
                                        currentEpisode = episode,
                                        season = episode.season,
                                        episodeNumber = episode.number
                                    )
                                )
                                Log.d("ContinueWatching", "📺 Added in-progress episode: ${show.title} S${episode.season}E${episode.number}")
                            }
                        }
                    }
                }
            }
            
            // 4. Process recent history to find shows with next episodes
            val recentShows = historyItems
                .filter { it.type == "episode" && it.show != null }
                .groupBy { it.show!!.ids.trakt }
                .mapNotNull { (showId, episodes) ->
                    showId?.let { id ->
                        val mostRecent = episodes.maxByOrNull { it.watched_at }
                        mostRecent?.let { id to it }
                    }
                }
                .toMap()
            
            Log.d("ContinueWatching", "🔍 Found ${recentShows.size} recently watched shows")
            
            // 5. For each recently watched show, check if there's a next episode
            recentShows.forEach { (showId, recentEpisode) ->
                if (!processedShows.contains(showId)) { // Don't duplicate in-progress shows
                    try {
                        val progress = authService.getShowProgress(showId)
                        progress.next_episode?.let { nextEp ->
                            recentEpisode.show?.let { show ->
                                continueWatchingItems.add(
                                    ContinueWatchingItem(
                                        type = "episode",
                                        lastWatchedAt = recentEpisode.watched_at,
                                        progress = null, // Next episode, not in progress
                                        show = show,
                                        nextEpisode = nextEp,
                                        season = nextEp.season,
                                        episodeNumber = nextEp.number
                                    )
                                )
                                Log.d("ContinueWatching", "▶️ Added next episode: ${show.title} S${nextEp.season}E${nextEp.number}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("ContinueWatching", "⚠️ Failed to get progress for show $showId: ${e.message}")
                    }
                }
            }
            
            // 6. Sort by most recently watched and limit results
            val sortedItems = continueWatchingItems
                .sortedByDescending { parseDateTime(it.lastWatchedAt) }
                .take(20) // Reasonable limit
            
            Log.d("ContinueWatching", "✅ Built continue watching list with ${sortedItems.size} items")
            return sortedItems
            
        } catch (e: Exception) {
            Log.e("ContinueWatching", "❌ Error building continue watching list", e)
            return emptyList()
        }
    }
    
    /**
     * Parse Trakt datetime strings for sorting
     */
    private fun parseDateTime(dateTimeStr: String): Long {
        return try {
            val zonedDateTime = ZonedDateTime.parse(dateTimeStr)
            zonedDateTime.toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.w("ContinueWatching", "Failed to parse datetime: $dateTimeStr")
            0L
        }
    }
}