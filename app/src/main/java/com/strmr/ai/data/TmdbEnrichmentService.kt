package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.utils.DateFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for enriching basic movie/TV show data with TMDB details
 * 
 * Single Responsibility: TMDB data transformation and enrichment
 * Consolidates duplicate enrichment logic from multiple repositories
 */
@Singleton
class TmdbEnrichmentService @Inject constructor(
    private val tmdbApiService: TmdbApiService,
    private val database: StrmrDatabase
) {
    
    companion object {
        private const val TAG = "TmdbEnrichmentService"
    }
    
    /**
     * Enrich a basic Movie with TMDB data to create MovieEntity
     * Consolidates logic from GenericTraktRepository, MovieRepository, TvShowRepository
     */
    suspend fun enrichMovie(
        movie: Movie,
        dataSourceId: String? = null,
        orderValue: Int? = null
    ): MovieEntity? {
        val tmdbId = movie.ids.tmdb ?: return null
        val traktId = movie.ids.trakt
        val imdbId = movie.ids.imdb
        
        return try {
            Log.d(TAG, "🎬 Enriching movie: ${movie.title} (TMDB: $tmdbId)")
            
            // Fetch TMDB data
            val details = tmdbApiService.getMovieDetails(tmdbId)
            val credits = tmdbApiService.getMovieCredits(tmdbId)
            
            // Get existing entity to preserve data from other sources
            val existing = database.movieDao().getMovieByTmdbId(tmdbId)
            
            MovieEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.title ?: movie.title,
                posterUrl = details.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                backdropUrl = details.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = existing?.logoUrl, // Preserve existing logo
                traktRating = existing?.traktRating, // Preserve existing Trakt data
                traktVotes = existing?.traktVotes,
                year = DateFormatter.extractYear(details.release_date),
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres.map { it.name },
                cast = credits.cast.take(StrmrConstants.UI.MAX_CAST_ITEMS).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                belongsToCollection = details.belongs_to_collection,
                // Set order based on data source
                trendingOrder = if (dataSourceId == "trending") orderValue else existing?.trendingOrder,
                popularOrder = if (dataSourceId == "popular") orderValue else existing?.popularOrder,
                topMoviesWeekOrder = if (dataSourceId == "top_movies_week") orderValue else existing?.topMoviesWeekOrder,
                similar = existing?.similar ?: emptyList(), // Preserve existing similar content
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enriching movie ${movie.title}", e)
            null
        }
    }
    
    /**
     * Enrich a basic Show with TMDB data to create TvShowEntity
     * Consolidates logic from GenericTraktRepository, TvShowRepository
     */
    suspend fun enrichTvShow(
        show: Show,
        dataSourceId: String? = null,
        orderValue: Int? = null
    ): TvShowEntity? {
        val tmdbId = show.ids.tmdb ?: return null
        val traktId = show.ids.trakt
        val imdbId = show.ids.imdb
        
        return try {
            Log.d(TAG, "📺 Enriching TV show: ${show.title} (TMDB: $tmdbId)")
            
            // Fetch TMDB data
            val details = tmdbApiService.getTvShowDetails(tmdbId)
            val credits = tmdbApiService.getTvShowCredits(tmdbId)
            
            // Get existing entity to preserve data from other sources
            val existing = database.tvShowDao().getTvShowByTmdbId(tmdbId)
            
            TvShowEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.name ?: show.title,
                posterUrl = details.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                backdropUrl = details.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = existing?.logoUrl, // Preserve existing logo
                traktRating = existing?.traktRating, // Preserve existing Trakt data
                traktVotes = existing?.traktVotes,
                year = DateFormatter.extractYear(details.first_air_date),
                firstAirDate = details.first_air_date,
                runtime = details.episode_run_time?.firstOrNull(),
                genres = details.genres.map { it.name },
                cast = credits.cast.take(StrmrConstants.UI.MAX_CAST_ITEMS).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                // Set order based on data source
                trendingOrder = if (dataSourceId == "trending") orderValue else existing?.trendingOrder,
                popularOrder = if (dataSourceId == "popular") orderValue else existing?.popularOrder,
                similar = existing?.similar ?: emptyList(), // Preserve existing similar content
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enriching TV show ${show.title}", e)
            null
        }
    }
    
    /**
     * Enrich movie with logo data specifically
     * Used when we need to fetch/update logo information
     */
    suspend fun enrichMovieWithLogo(tmdbId: Int): MovieEntity? {
        return try {
            val details = tmdbApiService.getMovieDetails(tmdbId)
            val credits = tmdbApiService.getMovieCredits(tmdbId)
            val images = tmdbApiService.getMovieImages(tmdbId)
            
            val logo = images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
                ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }
            val logoUrl = logo?.file_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it }
            
            val existing = database.movieDao().getMovieByTmdbId(tmdbId)
            
            MovieEntity(
                tmdbId = tmdbId,
                imdbId = details.imdb_id,
                title = details.title ?: "",
                posterUrl = details.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                backdropUrl = details.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = logoUrl,
                traktRating = existing?.traktRating,
                traktVotes = existing?.traktVotes,
                year = DateFormatter.extractYear(details.release_date),
                releaseDate = details.release_date,
                runtime = details.runtime,
                genres = details.genres.map { it.name },
                cast = credits.cast.take(StrmrConstants.UI.MAX_CAST_ITEMS).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                belongsToCollection = details.belongs_to_collection,
                trendingOrder = existing?.trendingOrder,
                popularOrder = existing?.popularOrder,
                topMoviesWeekOrder = existing?.topMoviesWeekOrder,
                similar = existing?.similar ?: emptyList(),
                lastUpdated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enriching movie with logo for tmdbId $tmdbId", e)
            null
        }
    }
}