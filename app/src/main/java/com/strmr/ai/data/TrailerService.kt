package com.strmr.ai.data

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to handle trailer fetching and filtering from TMDB API
 */
@Singleton
class TrailerService @Inject constructor(
    private val tmdbApiService: TmdbApiService
) {
    
    /**
     * Get the best official trailer for a movie
     * Filters for: official=true AND name contains "Trailer"
     * Returns YouTube URL ready for ExoPlayer
     */
    suspend fun getMovieTrailer(movieId: Int): String? {
        return try {
            Log.d("TrailerService", "🎬 Fetching trailers for movie ID: $movieId")
            val videosResponse = tmdbApiService.getMovieVideos(movieId)
            val trailer = findBestTrailer(videosResponse.results)
            
            trailer?.let { video ->
                Log.d("TrailerService", "✅ Found trailer: ${video.name} (${video.site})")
                convertToPlayableUrl(video)
            }
        } catch (e: Exception) {
            Log.e("TrailerService", "❌ Error fetching movie trailer for ID: $movieId", e)
            null
        }
    }
    
    /**
     * Get the best official trailer for a TV show
     * Filters for: official=true AND name contains "Trailer"
     * Returns YouTube URL ready for ExoPlayer
     */
    suspend fun getTvShowTrailer(tvId: Int): String? {
        return try {
            Log.d("TrailerService", "📺 Fetching trailers for TV show ID: $tvId")
            val videosResponse = tmdbApiService.getTvShowVideos(tvId)
            val trailer = findBestTrailer(videosResponse.results)
            
            trailer?.let { video ->
                Log.d("TrailerService", "✅ Found trailer: ${video.name} (${video.site})")
                convertToPlayableUrl(video)
            }
        } catch (e: Exception) {
            Log.e("TrailerService", "❌ Error fetching TV show trailer for ID: $tvId", e)
            null
        }
    }
    
    /**
     * Find the best trailer from a list of videos
     * Priority: official=true AND type="Trailer" AND name contains "Trailer"
     * Secondary: official=true AND type="Trailer"
     * Fallback: type="Trailer"
     */
    private fun findBestTrailer(videos: List<TmdbVideo>): TmdbVideo? {
        Log.d("TrailerService", "🔍 Filtering ${videos.size} videos for best trailer")
        
        // Filter for YouTube videos only (ExoPlayer compatibility)
        val youtubeVideos = videos.filter { it.site.equals("YouTube", ignoreCase = true) }
        Log.d("TrailerService", "📹 Found ${youtubeVideos.size} YouTube videos")
        
        // Priority 1: Official trailers with "Trailer" in name
        var candidates = youtubeVideos.filter { 
            it.official && 
            it.type.equals("Trailer", ignoreCase = true) &&
            it.name.contains("Trailer", ignoreCase = true)
        }
        
        if (candidates.isNotEmpty()) {
            Log.d("TrailerService", "🎯 Found ${candidates.size} official trailers with 'Trailer' in name")
            return candidates.first()
        }
        
        // Priority 2: Official trailers (any name)
        candidates = youtubeVideos.filter { 
            it.official && it.type.equals("Trailer", ignoreCase = true)
        }
        
        if (candidates.isNotEmpty()) {
            Log.d("TrailerService", "🎯 Found ${candidates.size} official trailers")
            return candidates.first()
        }
        
        // Priority 3: Any trailer
        candidates = youtubeVideos.filter { 
            it.type.equals("Trailer", ignoreCase = true)
        }
        
        if (candidates.isNotEmpty()) {
            Log.d("TrailerService", "🎯 Found ${candidates.size} trailers (unofficial)")
            return candidates.first()
        }
        
        Log.w("TrailerService", "⚠️ No suitable trailer found")
        return null
    }
    
    /**
     * Convert TMDB video to playable URL
     * Returns original YouTube URL for WebView-based player
     */
    private suspend fun convertToPlayableUrl(video: TmdbVideo): String? {
        return when (video.site.lowercase()) {
            "youtube" -> {
                val youtubeUrl = "https://www.youtube.com/watch?v=${video.key}"
                Log.d("TrailerService", "🔗 Generated YouTube URL: $youtubeUrl")
                Log.d("TrailerService", "✅ Returning original YouTube URL for WebView player")
                youtubeUrl
            }
            else -> {
                Log.w("TrailerService", "⚠️ Unsupported video site: ${video.site}")
                null
            }
        }
    }
}