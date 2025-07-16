package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.TvShowDao
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.strmr.ai.data.Show
import com.strmr.ai.data.database.SeasonDao
import com.strmr.ai.data.database.EpisodeDao
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.EpisodeEntity
import com.strmr.ai.utils.DateFormatter

class TvShowRepository(
    private val tvShowDao: TvShowDao,
    private val traktApiService: TraktApiService,
    private val tmdbApiService: TmdbApiService,
    private val seasonDao: SeasonDao,
    private val episodeDao: EpisodeDao
) {
    private val detailsExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days
    private var currentTrendingPage = 0
    private var currentPopularPage = 0
    private val pageSize = 20

    fun getTrendingTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getTrendingTvShows()
    
    fun getPopularTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getPopularTvShows()
    
    fun getTrendingTvShowsPagingSource() = tvShowDao.getTvShowsPagingSource()
    
    suspend fun loadMoreTrendingTvShows() {
        currentTrendingPage++
        refreshTrendingTvShows()
    }
    
    suspend fun loadMorePopularTvShows() {
        currentPopularPage++
        refreshPopularTvShows()
    }

    suspend fun refreshTrendingTvShows() {
        withContext(Dispatchers.IO) {
            val limit = pageSize
            val page = currentTrendingPage + 1 // Trakt API uses 1-based page numbers
            val trending = traktApiService.getTrendingTvShows(page = page, limit = limit).mapIndexedNotNull { index, trendingShow ->
                val tmdbId = trendingShow.show.ids.tmdb ?: return@mapIndexedNotNull null
                val traktId = trendingShow.show.ids.trakt ?: return@mapIndexedNotNull null
                val cached = tvShowDao.getTvShowByTmdbId(tmdbId)
                val now = System.currentTimeMillis()
                val actualIndex = currentTrendingPage * pageSize + index
                if (cached == null || now - cached.lastUpdated > detailsExpiryMs) {
                    fetchAndMapToEntity(trendingShow.show, trendingOrder = actualIndex)?.copy(lastUpdated = now)
                } else {
                    cached.copy(trendingOrder = actualIndex)
                }
            }
            Log.d("TvShowRepository", "Fetched trending page $page, got IDs: ${trending.map { it.tmdbId }}")
            if (currentTrendingPage == 0) {
                tvShowDao.updateTrendingTvShows(trending)
            } else {
                tvShowDao.insertTvShows(trending)
            }
        }
    }
    
    suspend fun refreshPopularTvShows() {
        withContext(Dispatchers.IO) {
            val limit = pageSize
            val page = currentPopularPage + 1 // Trakt API uses 1-based page numbers
            val popular = traktApiService.getPopularTvShows(page = page, limit = limit).mapIndexedNotNull { index, show ->
                val tmdbId = show.ids.tmdb ?: return@mapIndexedNotNull null
                val traktId = show.ids.trakt ?: return@mapIndexedNotNull null
                val cached = tvShowDao.getTvShowByTmdbId(tmdbId)
                val now = System.currentTimeMillis()
                val actualIndex = currentPopularPage * pageSize + index
                if (cached == null || now - cached.lastUpdated > detailsExpiryMs) {
                    fetchAndMapToEntity(show, popularOrder = actualIndex)?.copy(lastUpdated = now)
                } else {
                    cached.copy(popularOrder = actualIndex)
                }
            }
            Log.d("TvShowRepository", "Fetched popular page $page, got IDs: ${popular.map { it.tmdbId }}")
            if (currentPopularPage == 0) {
                tvShowDao.updatePopularTvShows(popular)
            } else {
                tvShowDao.insertTvShows(popular)
            }
        }
    }

    private suspend fun fetchAndMapToEntity(
        show: Show,
        trendingOrder: Int? = null,
        popularOrder: Int? = null
    ): TvShowEntity? {
        val tmdbId = show.ids.tmdb ?: return null
        val traktId = show.ids.trakt ?: return null
        val imdbId = show.ids.imdb
        return try {
            val details = tmdbApiService.getTvShowDetails(tmdbId)
            val credits = tmdbApiService.getTvShowCredits(tmdbId)
            val traktRatings = traktApiService.getShowRatings(traktId)
            val cached = tvShowDao.getTvShowByTmdbId(tmdbId)
            TvShowEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.name ?: show.title,
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = cached?.logoUrl,
                traktRating = traktRatings.rating,
                traktVotes = traktRatings.votes,
                year = DateFormatter.extractYear(details.first_air_date),
                firstAirDate = details.first_air_date,
                lastAirDate = details.last_air_date,
                runtime = details.episode_run_time?.firstOrNull(),
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                trendingOrder = trendingOrder ?: cached?.trendingOrder,
                popularOrder = popularOrder ?: cached?.popularOrder
            )
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching details for ${show.title}", e)
            null
        }
    }

    suspend fun getOrFetchTvShow(tmdbId: Int): TvShowEntity? {
        val cachedTvShow = tvShowDao.getTvShowByTmdbId(tmdbId)
        if (cachedTvShow != null) {
            return cachedTvShow
        }

        return try {
            val details = tmdbApiService.getTvShowDetails(tmdbId)
            val credits = tmdbApiService.getTvShowCredits(tmdbId)
            val imdbId = details.imdb_id
            val entity = TvShowEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.name ?: "",
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = null, // Will be fetched on demand
                traktRating = 0f, // No Trakt rating available here
                traktVotes = 0,   // No Trakt votes available here
                year = DateFormatter.extractYear(details.first_air_date),
                firstAirDate = details.first_air_date,
                lastAirDate = details.last_air_date,
                runtime = details.episode_run_time?.firstOrNull(),
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                }
            )
            tvShowDao.insertTvShows(listOf(entity))
            entity
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching tv show with tmdbId $tmdbId", e)
            null
        }
    }

    suspend fun getTvShowByTmdbId(tmdbId: Int): TvShowEntity? {
        return tvShowDao.getTvShowByTmdbId(tmdbId)
    }

    suspend fun updateTvShowLogo(tmdbId: Int, logoUrl: String?) {
        tvShowDao.updateTvShowLogo(tmdbId, logoUrl)
    }

    suspend fun getOrFetchTvShowWithLogo(tmdbId: Int): TvShowEntity? {
        Log.d("TvShowRepository", "🔍 getOrFetchTvShowWithLogo called for tmdbId=$tmdbId")
        val cachedShow = tvShowDao.getTvShowByTmdbId(tmdbId)
        Log.d("TvShowRepository", "🔍 Cached show for tmdbId=$tmdbId: ${cachedShow?.title}, hasLogo=${!cachedShow?.logoUrl.isNullOrBlank()}")
        
        if (cachedShow != null && !cachedShow.logoUrl.isNullOrBlank()) {
            Log.d("TvShowRepository", "✅ Returning cached show with logo for tmdbId=$tmdbId")
            return cachedShow
        }
        
        // Fetch logo from TMDB if missing
        return try {
            Log.d("TvShowRepository", "🌐 Fetching logo from TMDB for tmdbId=$tmdbId")
            val details = tmdbApiService.getTvShowDetails(tmdbId)
            val credits = tmdbApiService.getTvShowCredits(tmdbId)
            val images = tmdbApiService.getTvShowImages(tmdbId)
            
            Log.d("TvShowRepository", "🌐 TMDB images response for tmdbId=$tmdbId: ${images.logos.size} logos available")
            images.logos.forEachIndexed { index, logo ->
                Log.d("TvShowRepository", "🌐 Logo $index: iso=${logo.iso_639_1}, path=${logo.file_path}")
            }
            
            // Prefer English PNG logo, then any PNG, then any English logo, then any logo
            val logo = images.logos.firstOrNull { it.iso_639_1 == "en" && it.file_path?.endsWith(".png") == true }
                ?: images.logos.firstOrNull { it.file_path?.endsWith(".png") == true }
                ?: images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
                ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }
            
            Log.d("TvShowRepository", "🌐 Selected logo for tmdbId=$tmdbId: iso=${logo?.iso_639_1}, path=${logo?.file_path}")
            
            val logoUrl = logo?.file_path?.let { "https://image.tmdb.org/t/p/w500$it" }
            Log.d("TvShowRepository", "🌐 Final logo URL for tmdbId=$tmdbId: $logoUrl")
            
            val imdbId = details.imdb_id
            if (!logoUrl.isNullOrBlank()) {
                Log.d("TvShowRepository", "💾 Updating logo in database for tmdbId=$tmdbId")
                tvShowDao.updateTvShowLogo(tmdbId, logoUrl)
            }
            
            val entity = TvShowEntity(
                tmdbId = tmdbId,
                imdbId = imdbId,
                title = details.name ?: "",
                posterUrl = details.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                backdropUrl = details.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                overview = details.overview,
                rating = details.vote_average,
                logoUrl = logoUrl,
                traktRating = cachedShow?.traktRating,
                traktVotes = cachedShow?.traktVotes,
                year = DateFormatter.extractYear(details.first_air_date),
                firstAirDate = details.first_air_date,
                lastAirDate = details.last_air_date,
                runtime = details.episode_run_time?.firstOrNull(),
                genres = details.genres.map { it.name },
                cast = credits.cast.take(15).map { 
                    Actor(
                        id = it.id,
                        name = it.name,
                        character = it.character,
                        profilePath = it.profile_path
                    )
                },
                trendingOrder = cachedShow?.trendingOrder,
                popularOrder = cachedShow?.popularOrder
            )
            tvShowDao.insertTvShows(listOf(entity))
            Log.d("TvShowRepository", "✅ Created/updated entity for tmdbId=$tmdbId with logoUrl=$logoUrl")
            entity
        } catch (e: Exception) {
            Log.e("TvShowRepository", "❌ Error fetching logo for show $tmdbId", e)
            cachedShow
        }
    }

    suspend fun getEpisodeDetails(tvId: Int, seasonNumber: Int, episodeNumber: Int): TmdbEpisodeDetails? {
        return try {
            tmdbApiService.getEpisodeDetails(tvId, seasonNumber, episodeNumber)
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching episode details for tvId=$tvId, season=$seasonNumber, episode=$episodeNumber", e)
            null
        }
    }

    suspend fun getEpisodeByDetails(showTmdbId: Int, seasonNumber: Int, episodeNumber: Int): EpisodeEntity? {
        return try {
            episodeDao.getEpisodeByDetails(showTmdbId, seasonNumber, episodeNumber)
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching episode from database for showTmdbId=$showTmdbId, season=$seasonNumber, episode=$episodeNumber", e)
            null
        }
    }

    private val cacheExpiryMs = 7 * 24 * 60 * 60 * 1000L // 7 days

    suspend fun getOrFetchSeasons(showTmdbId: Int): List<SeasonEntity> {
        val cached = seasonDao.getSeasonsForShow(showTmdbId)
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && cached.all { now - it.lastUpdated < cacheExpiryMs }) {
            return cached
        }
        // Fetch from TMDB
        return try {
            val details = tmdbApiService.getTvShowDetails(showTmdbId)
            // Try to get the number of seasons from episode_run_time or another field (fallback to 10 if not available)
            val numberOfSeasons = details.episode_run_time?.size ?: 0
            // If episode_run_time is not the right field, you may need to hardcode or fetch from another endpoint
            // For now, let's try 1..numberOfSeasons, fallback to 1..10
            val seasonNumbers = if (numberOfSeasons > 0) 1..numberOfSeasons else 1..10
            val seasonsList = mutableListOf<SeasonEntity>()
            for (seasonNumber in seasonNumbers) {
                try {
                    val seasonDetails = tmdbApiService.getSeasonDetails(showTmdbId, seasonNumber)
                    seasonsList.add(
                        SeasonEntity(
                            showTmdbId = showTmdbId,
                            seasonNumber = seasonDetails.season_number,
                            name = seasonDetails.name,
                            overview = seasonDetails.overview,
                            posterUrl = seasonDetails.poster_path?.let { path -> "https://image.tmdb.org/t/p/w300$path" },
                            episodeCount = seasonDetails.episodes?.size ?: 0,
                            airDate = seasonDetails.air_date,
                            lastUpdated = now
                        )
                    )
                } catch (_: Exception) { /* skip missing/bad seasons */ }
            }
            seasonDao.deleteSeasonsForShow(showTmdbId)
            seasonDao.insertSeasons(seasonsList)
            seasonsList
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching seasons for show $showTmdbId", e)
            cached
        }
    }

    suspend fun getOrFetchEpisodes(showTmdbId: Int, seasonNumber: Int): List<EpisodeEntity> {
        val cached = episodeDao.getEpisodesForSeason(showTmdbId, seasonNumber)
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && cached.all { now - it.lastUpdated < cacheExpiryMs }) {
            return cached
        }
        // Fetch from TMDB
        return try {
            val seasonDetails = tmdbApiService.getSeasonDetails(showTmdbId, seasonNumber)
            val episodesList = seasonDetails.episodes?.map { episode ->
                EpisodeEntity(
                    showTmdbId = showTmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episode.episode_number,
                    name = episode.name,
                    overview = episode.overview,
                    stillUrl = episode.still_path?.let { path -> "https://image.tmdb.org/t/p/w300$path" },
                    airDate = episode.air_date,
                    runtime = episode.runtime,
                    lastUpdated = now
                )
            } ?: emptyList()
            episodeDao.deleteEpisodesForSeason(showTmdbId, seasonNumber)
            episodeDao.insertEpisodes(episodesList)
            episodesList
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching episodes for show $showTmdbId season $seasonNumber", e)
            cached
        }
    }

    suspend fun getOrFetchSimilarTvShows(tmdbId: Int): List<SimilarContent> {
        val cachedShow = tvShowDao.getTvShowByTmdbId(tmdbId)
        if (cachedShow != null && cachedShow.similar.isNotEmpty()) {
            return cachedShow.similar
        }
        
        return try {
            val similarResponse = tmdbApiService.getSimilarTvShows(tmdbId)
            val similarContent = similarResponse.results.take(10).map { item ->
                SimilarContent(
                    tmdbId = item.id,
                    title = item.name ?: "",
                    posterUrl = item.poster_path?.let { "https://image.tmdb.org/t/p/w500$it" },
                    backdropUrl = item.backdrop_path?.let { "https://image.tmdb.org/t/p/w780$it" },
                    rating = item.vote_average,
                    year = DateFormatter.extractYear(item.first_air_date),
                    mediaType = "tv"
                )
            }
            
            // Update the TV show entity with similar content
            cachedShow?.let { show ->
                val updatedShow = show.copy(similar = similarContent)
                tvShowDao.insertTvShows(listOf(updatedShow))
            }
            
            similarContent
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching similar TV shows for $tmdbId", e)
            emptyList()
        }
    }
} 