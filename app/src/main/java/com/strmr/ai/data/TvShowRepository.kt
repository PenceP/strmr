package com.strmr.ai.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.strmr.ai.data.database.TvShowDao
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.database.TraktRatingsEntity
import com.strmr.ai.data.paging.TvShowsRemoteMediator
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
    traktApiService: TraktApiService,
    tmdbApi: TmdbApiService,
    private val seasonDao: SeasonDao,
    private val episodeDao: EpisodeDao,
    database: StrmrDatabase,
    traktRatingsDao: TraktRatingsDao
) : BaseMediaRepository<TvShowEntity, Show, TrendingShow>(
    traktApiService, tmdbApi, database, traktRatingsDao
) {
    // These are now inherited from BaseMediaRepository

    fun getTrendingTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getTrendingTvShows()
    
    fun getPopularTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getPopularTvShows()
    
    @OptIn(ExperimentalPagingApi::class)
    fun getTrendingTvShowsPager(): Flow<PagingData<TvShowEntity>> {
        return createPager(
            contentType = ContentType.TRENDING,
            remoteMediator = TvShowsRemoteMediator(
                contentType = TvShowsRemoteMediator.ContentType.TRENDING,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                tvShowRepository = this
            ),
            pagingSourceFactory = { tvShowDao.getTrendingTvShowsPagingSource() }
        )
    }
    
    @OptIn(ExperimentalPagingApi::class)
    fun getPopularTvShowsPager(): Flow<PagingData<TvShowEntity>> {
        return createPager(
            contentType = ContentType.POPULAR,
            remoteMediator = TvShowsRemoteMediator(
                contentType = TvShowsRemoteMediator.ContentType.POPULAR,
                database = database,
                traktApi = traktApi,
                tmdbApi = tmdbApi,
                tvShowRepository = this
            ),
            pagingSourceFactory = { tvShowDao.getPopularTvShowsPagingSource() }
        )
    }
    
    suspend fun loadMoreTrendingTvShows() {
        refreshContent(
            contentType = ContentType.TRENDING,
            fetchTrendingFromTrakt = { page, limit -> traktApi.getTrendingTvShows(page, limit) },
            fetchPopularFromTrakt = { _, _ -> emptyList() }, // Not used for trending
            mapTrendingToEntity = { trendingShow, order -> 
                fetchAndMapToEntity(trendingShow.show, trendingOrder = order)
            },
            mapPopularToEntity = { _, _ -> null } // Not used for trending
        )
    }
    
    suspend fun loadMorePopularTvShows() {
        refreshContent(
            contentType = ContentType.POPULAR,
            fetchTrendingFromTrakt = { _, _ -> emptyList() }, // Not used for popular
            fetchPopularFromTrakt = { page, limit -> traktApi.getPopularTvShows(page, limit) },
            mapTrendingToEntity = { _, _ -> null }, // Not used for popular
            mapPopularToEntity = { show, order -> 
                fetchAndMapToEntity(show, popularOrder = order)
            }
        )
    }

    suspend fun refreshTrendingTvShows() = loadMoreTrendingTvShows()
    suspend fun refreshPopularTvShows() = loadMorePopularTvShows()

    suspend fun mapTraktShowToEntity(
        show: Show,
        trendingOrder: Int? = null,
        popularOrder: Int? = null
    ): TvShowEntity? {
        return fetchAndMapToEntity(show, trendingOrder, popularOrder)
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
            val details = tmdbApi.getTvShowDetails(tmdbId)
            val credits = tmdbApi.getTvShowCredits(tmdbId)
            
            // Use cached Trakt ratings instead of direct API call
            val traktRatings = getTraktRatings(traktId)
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
                traktRating = traktRatings?.rating,
                traktVotes = traktRatings?.votes,
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
            val details = tmdbApi.getTvShowDetails(tmdbId)
            val credits = tmdbApi.getTvShowCredits(tmdbId)
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

    // These methods are now inherited from BaseMediaRepository:
    // - getItemByTmdbId() -> getTvShowByTmdbId()
    // - updateItemLogo() -> updateTvShowLogo()  
    // - clearNullLogos()
    
    suspend fun getTvShowByTmdbId(tmdbId: Int): TvShowEntity? = getItemByTmdbId(tmdbId)
    suspend fun updateTvShowLogo(tmdbId: Int, logoUrl: String?) = updateItemLogo(tmdbId, logoUrl)

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
            val details = tmdbApi.getTvShowDetails(tmdbId)
            val credits = tmdbApi.getTvShowCredits(tmdbId)
            val images = tmdbApi.getTvShowImages(tmdbId)
            
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
            tmdbApi.getEpisodeDetails(tvId, seasonNumber, episodeNumber)
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

    suspend fun getOrFetchSeasons(showTmdbId: Int): List<SeasonEntity> {
        val cached = seasonDao.getSeasonsForShow(showTmdbId)
        val now = System.currentTimeMillis()
        if (cached.isNotEmpty() && cached.all { now - it.lastUpdated < detailsExpiryMs }) {
            return cached
        }
        // Fetch from TMDB
        return try {
            val details = tmdbApi.getTvShowDetails(showTmdbId)
            // Try to get the number of seasons from episode_run_time or another field (fallback to 10 if not available)
            val numberOfSeasons = details.episode_run_time?.size ?: 0
            // If episode_run_time is not the right field, you may need to hardcode or fetch from another endpoint
            // For now, let's try 1..numberOfSeasons, fallback to 1..10
            val seasonNumbers = if (numberOfSeasons > 0) 1..numberOfSeasons else 1..10
            val seasonsList = mutableListOf<SeasonEntity>()
            for (seasonNumber in seasonNumbers) {
                try {
                    val seasonDetails = tmdbApi.getSeasonDetails(showTmdbId, seasonNumber)
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
        if (cached.isNotEmpty() && cached.all { now - it.lastUpdated < detailsExpiryMs }) {
            return cached
        }
        // Fetch from TMDB
        return try {
            val seasonDetails = tmdbApi.getSeasonDetails(showTmdbId, seasonNumber)
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
            val similarResponse = tmdbApi.getSimilarTvShows(tmdbId)
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

    // Override getTraktRatings to handle TV show-specific API calls
    suspend override fun getTraktRatings(traktId: Int): TraktRatingsEntity? {
        val cached = super.getTraktRatings(traktId)
        if (cached != null) return cached
        
        // Fetch from API if not cached or expired
        return try {
            val api = traktApi.getShowRatings(traktId)
            saveTraktRatings(traktId, api.rating, api.votes)
            super.getTraktRatings(traktId)
        } catch (e: Exception) {
            null
        }
    }

    // Implement abstract methods from BaseMediaRepository
    override fun getLogTag(): String = "TvShowRepository"
    
    override suspend fun getTmdbId(item: Show): Int? = item.ids.tmdb
    
    override suspend fun getTraktId(item: Show): Int? = item.ids.trakt
    
    override suspend fun getTmdbIdFromTrending(item: TrendingShow): Int? = item.show.ids.tmdb
    
    override suspend fun getTraktIdFromTrending(item: TrendingShow): Int? = item.show.ids.trakt
    
    override suspend fun updateEntityTimestamp(entity: TvShowEntity): TvShowEntity = 
        entity.copy(lastUpdated = System.currentTimeMillis())
    
    // Implement abstract DAO methods from BaseMediaRepository
    override suspend fun insertItems(items: List<TvShowEntity>) = tvShowDao.insertTvShows(items)
    override suspend fun updateTrendingItems(items: List<TvShowEntity>) = tvShowDao.updateTrendingTvShows(items)
    override suspend fun updatePopularItems(items: List<TvShowEntity>) = tvShowDao.updatePopularTvShows(items)
    override suspend fun getItemByTmdbId(tmdbId: Int): TvShowEntity? = tvShowDao.getTvShowByTmdbId(tmdbId)
    override suspend fun updateItemLogo(tmdbId: Int, logoUrl: String?) = tvShowDao.updateTvShowLogo(tmdbId, logoUrl)
    override suspend fun clearNullLogos() = tvShowDao.clearNullLogos()
} 