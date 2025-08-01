package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.EpisodeDao
import com.strmr.ai.data.database.EpisodeEntity
import com.strmr.ai.data.database.SeasonDao
import com.strmr.ai.data.database.SeasonEntity
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.database.TraktRatingsEntity
import com.strmr.ai.data.database.TvShowDao
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.utils.DateFormatter
import kotlinx.coroutines.flow.Flow

class TvShowRepository(
    private val tvShowDao: TvShowDao,
    traktApiService: TraktApiService,
    tmdbApi: TmdbApiService,
    private val seasonDao: SeasonDao,
    private val episodeDao: EpisodeDao,
    database: StrmrDatabase,
    traktRatingsDao: TraktRatingsDao,
    private val trailerService: TrailerService,
    private val tmdbEnrichmentService: TmdbEnrichmentService,
) : BaseMediaRepository<TvShowEntity, Show, TrendingShow>(
        traktApiService,
        tmdbApi,
        database,
        traktRatingsDao,
    ) {
    // These are now inherited from BaseMediaRepository

    fun getTrendingTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getTrendingTvShows()

    fun getPopularTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getPopularTvShows()

    suspend fun mapTraktShowToEntity(
        show: Show,
        trendingOrder: Int? = null,
        popularOrder: Int? = null,
    ): TvShowEntity? {
        return fetchAndMapToEntity(show, trendingOrder, popularOrder)
    }

    private suspend fun fetchAndMapToEntity(
        show: Show,
        trendingOrder: Int? = null,
        popularOrder: Int? = null,
    ): TvShowEntity? {
        val dataSourceId =
            when {
                trendingOrder != null -> "trending"
                popularOrder != null -> "popular"
                else -> null
            }
        val orderValue = trendingOrder ?: popularOrder

        return tmdbEnrichmentService.enrichTvShow(
            show = show,
            dataSourceId = dataSourceId,
            orderValue = orderValue,
        )
    }

    suspend fun getOrFetchTvShow(tmdbId: Int): TvShowEntity? {
        val cachedTvShow = tvShowDao.getTvShowByTmdbId(tmdbId)
        if (cachedTvShow != null) {
            return cachedTvShow
        }

        return try {
            // Create a minimal Show object for enrichment
            val basicShow =
                Show(
                    title = "", // Will be filled by enrichment service
                    year = null,
                    ids = ShowIds(tmdb = tmdbId, trakt = null, imdb = null, slug = null),
                )

            val entity = tmdbEnrichmentService.enrichTvShow(basicShow)
            entity?.let {
                tvShowDao.insertTvShows(listOf(it))
            }
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

    suspend fun updateTvShowLogo(
        tmdbId: Int,
        logoUrl: String?,
    ) = updateItemLogo(tmdbId, logoUrl)

    suspend fun getOrFetchTvShowWithLogo(tmdbId: Int): TvShowEntity? {
        Log.d("TvShowRepository", "🔍 getOrFetchTvShowWithLogo called for tmdbId=$tmdbId")
        val cachedShow = tvShowDao.getTvShowByTmdbId(tmdbId)
        Log.d(
            "TvShowRepository",
            "🔍 Cached show for tmdbId=$tmdbId: ${cachedShow?.title}, hasLogo=${!cachedShow?.logoUrl.isNullOrBlank()}",
        )

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
            val logo =
                images.logos.firstOrNull { it.iso_639_1 == "en" && it.file_path?.endsWith(".png") == true }
                    ?: images.logos.firstOrNull { it.file_path?.endsWith(".png") == true }
                    ?: images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
                    ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }

            Log.d("TvShowRepository", "🌐 Selected logo for tmdbId=$tmdbId: iso=${logo?.iso_639_1}, path=${logo?.file_path}")

            val logoUrl = logo?.file_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it }
            Log.d("TvShowRepository", "🌐 Final logo URL for tmdbId=$tmdbId: $logoUrl")

            val imdbId = details.imdb_id
            if (!logoUrl.isNullOrBlank()) {
                Log.d("TvShowRepository", "💾 Updating logo in database for tmdbId=$tmdbId")
                tvShowDao.updateTvShowLogo(tmdbId, logoUrl)
            }

            val entity =
                TvShowEntity(
                    tmdbId = tmdbId,
                    imdbId = imdbId,
                    title = details.name ?: "",
                    posterUrl = details.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                    backdropUrl = details.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
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
                    cast =
                        credits.cast.take(StrmrConstants.UI.MAX_CAST_ITEMS).map {
                            Actor(
                                id = it.id,
                                name = it.name,
                                character = it.character,
                                profilePath = it.profile_path,
                            )
                        },
                    trendingOrder = cachedShow?.trendingOrder,
                    popularOrder = cachedShow?.popularOrder,
                )
            tvShowDao.insertTvShows(listOf(entity))
            Log.d("TvShowRepository", "✅ Created/updated entity for tmdbId=$tmdbId with logoUrl=$logoUrl")
            entity
        } catch (e: Exception) {
            Log.e("TvShowRepository", "❌ Error fetching logo for show $tmdbId", e)
            cachedShow
        }
    }

    suspend fun getEpisodeDetails(
        tvId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): TmdbEpisodeDetails? {
        return try {
            tmdbApi.getEpisodeDetails(tvId, seasonNumber, episodeNumber)
        } catch (e: Exception) {
            Log.e("TvShowRepository", "Error fetching episode details for tvId=$tvId, season=$seasonNumber, episode=$episodeNumber", e)
            null
        }
    }

    suspend fun getEpisodeByDetails(
        showTmdbId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeEntity? {
        return try {
            episodeDao.getEpisodeByDetails(showTmdbId, seasonNumber, episodeNumber)
        } catch (e: Exception) {
            Log.e(
                "TvShowRepository",
                "Error fetching episode from database for showTmdbId=$showTmdbId, season=$seasonNumber, episode=$episodeNumber",
                e,
            )
            null
        }
    }

    suspend fun getOrFetchSeasons(showTmdbId: Int): List<SeasonEntity> {
        // Always fetch fresh data from TMDB to ensure new seasons/episodes are shown
        return try {
            Log.d("TvShowRepository", "🌐 Fetching seasons from TMDB for show $showTmdbId")
            val details = tmdbApi.getTvShowDetails(showTmdbId)
            Log.d(
                "TvShowRepository",
                "📡 TMDB TV Show Details for $showTmdbId: seasons=${details.seasons?.size}, number_of_seasons=${details.number_of_seasons}",
            )

            // Log the raw seasons data from TMDB
            details.seasons?.forEachIndexed { index, season ->
                Log.d(
                    "TvShowRepository",
                    "📺 Raw Season $index: number=${season.season_number}, name='${season.name}', episodes=${season.episode_count}",
                )
            }

            val seasonsList = mutableListOf<SeasonEntity>()
            val now = System.currentTimeMillis()

            // Use the seasons array from TMDB if available, otherwise fallback to number_of_seasons
            if (!details.seasons.isNullOrEmpty()) {
                Log.d(
                    "TvShowRepository",
                    "✅ Using seasons array from TMDB (${details.seasons.size} seasons)",
                )
                for (seasonSummary in details.seasons) {
                    Log.d(
                        "TvShowRepository",
                        "🔍 Processing Season ${seasonSummary.season_number}: '${seasonSummary.name}' (${seasonSummary.episode_count} episodes)",
                    )

                    // Skip Season 0 (specials) entirely
                    if (seasonSummary.season_number == 0) {
                        Log.d("TvShowRepository", "⏭️ Skipping Season 0 (specials)")
                        continue
                    }

                    try {
                        val seasonEntity =
                            SeasonEntity(
                                showTmdbId = showTmdbId,
                                seasonNumber = seasonSummary.season_number,
                                name = seasonSummary.name,
                                overview = seasonSummary.overview,
                                posterUrl = seasonSummary.poster_path?.let { path -> StrmrConstants.Api.TMDB_IMAGE_BASE_W300 + path },
                                episodeCount = seasonSummary.episode_count,
                                airDate = seasonSummary.air_date,
                                lastUpdated = now,
                            )
                        seasonsList.add(seasonEntity)
                        Log.d(
                            "TvShowRepository",
                            "✅ Added Season ${seasonSummary.season_number}: ${seasonSummary.name} (${seasonSummary.episode_count} episodes)",
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "TvShowRepository",
                            "⚠️ Failed to process season ${seasonSummary.season_number}: ${e.message}",
                        )
                    }
                }
            } else if (details.number_of_seasons != null && details.number_of_seasons > 0) {
                Log.d(
                    "TvShowRepository",
                    "🔄 Fallback: Using number_of_seasons=${details.number_of_seasons}",
                )
                // Fallback: fetch each season individually
                for (seasonNumber in 1..details.number_of_seasons) {
                    try {
                        val seasonDetails = tmdbApi.getSeasonDetails(showTmdbId, seasonNumber)
                        seasonsList.add(
                            SeasonEntity(
                                showTmdbId = showTmdbId,
                                seasonNumber = seasonDetails.season_number,
                                name = seasonDetails.name,
                                overview = seasonDetails.overview,
                                posterUrl = seasonDetails.poster_path?.let { path -> StrmrConstants.Api.TMDB_IMAGE_BASE_W300 + path },
                                episodeCount = seasonDetails.episodes?.size ?: 0,
                                airDate = seasonDetails.air_date,
                                lastUpdated = now,
                            ),
                        )
                        Log.d(
                            "TvShowRepository",
                            "✅ Fetched Season $seasonNumber: ${seasonDetails.name}",
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "TvShowRepository",
                            "⚠️ Failed to fetch season $seasonNumber: ${e.message}",
                        )
                    }
                }
            } else {
                Log.w(
                    "TvShowRepository",
                    "⚠️ No season information available from TMDB for show $showTmdbId",
                )
            }

            Log.d("TvShowRepository", "📊 Final seasonsList size: ${seasonsList.size}")
            seasonsList.forEachIndexed { index, season ->
                Log.d(
                    "TvShowRepository",
                    "📊 Final Season $index: ${season.seasonNumber} - ${season.name}",
                )
            }

            // Still save to database for performance, but don't use cache for retrieval
            if (seasonsList.isNotEmpty()) {
                seasonDao.deleteSeasonsForShow(showTmdbId)
                seasonDao.insertSeasons(seasonsList)
                Log.d(
                    "TvShowRepository",
                    "💾 Saved ${seasonsList.size} seasons to database (for performance only)",
                )
            }

            seasonsList
        } catch (e: Exception) {
            Log.e("TvShowRepository", "❌ Error fetching seasons for show $showTmdbId", e)
            // Fallback to cached data only if API fails
            val cached = seasonDao.getSeasonsForShow(showTmdbId)
            Log.d("TvShowRepository", "🔄 API failed, falling back to ${cached.size} cached seasons")
            cached
        }
    }

    suspend fun getOrFetchEpisodes(
        showTmdbId: Int,
        seasonNumber: Int,
    ): List<EpisodeEntity> {
        // Always fetch fresh episode data from TMDB to ensure new episodes are shown
        return try {
            Log.d(
                "TvShowRepository",
                "🌐 Fetching episodes from TMDB for show $showTmdbId season $seasonNumber",
            )
            val seasonDetails = tmdbApi.getSeasonDetails(showTmdbId, seasonNumber)
            val now = System.currentTimeMillis()

            val episodesList =
                seasonDetails.episodes?.map { episode ->
                    EpisodeEntity(
                        showTmdbId = showTmdbId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episode.episode_number,
                        name = episode.name,
                        overview = episode.overview,
                        stillUrl = episode.still_path?.let { path -> StrmrConstants.Api.TMDB_IMAGE_BASE_W300 + path },
                        airDate = episode.air_date,
                        runtime = episode.runtime,
                        rating = episode.vote_average, // Added episode rating
                        lastUpdated = now,
                    )
                } ?: emptyList()

            Log.d(
                "TvShowRepository",
                "✅ Fetched ${episodesList.size} fresh episodes for show $showTmdbId season $seasonNumber",
            )

            // Still save to database for performance, but don't use cache for retrieval
            episodeDao.deleteEpisodesForSeason(showTmdbId, seasonNumber)
            episodeDao.insertEpisodes(episodesList)

            episodesList
        } catch (e: Exception) {
            Log.e(
                "TvShowRepository",
                "❌ Error fetching episodes for show $showTmdbId season $seasonNumber",
                e,
            )
            // Fallback to cached data only if API fails
            val cached = episodeDao.getEpisodesForSeason(showTmdbId, seasonNumber)
            Log.d(
                "TvShowRepository",
                "🔄 API failed, falling back to ${cached.size} cached episodes",
            )
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
            val similarContent =
                similarResponse.results.take(StrmrConstants.UI.MAX_SIMILAR_ITEMS).map { item ->
                    SimilarContent(
                        tmdbId = item.id,
                        title = item.name ?: "",
                        posterUrl = item.poster_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W500 + it },
                        backdropUrl = item.backdrop_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_W780 + it },
                        rating = item.vote_average,
                        year = DateFormatter.extractYear(item.first_air_date),
                        mediaType = "tv",
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
    override suspend fun getTraktRatings(traktId: Int): TraktRatingsEntity? {
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

    override suspend fun updateEntityTimestamp(entity: TvShowEntity): TvShowEntity = entity.copy(lastUpdated = System.currentTimeMillis())

    // Implement abstract DAO methods from BaseMediaRepository
    override suspend fun insertItems(items: List<TvShowEntity>) = tvShowDao.insertTvShows(items)

    override suspend fun updateTrendingItems(items: List<TvShowEntity>) = tvShowDao.updateTrendingTvShows(items)

    override suspend fun updatePopularItems(items: List<TvShowEntity>) = tvShowDao.updatePopularTvShows(items)

    override suspend fun getItemByTmdbId(tmdbId: Int): TvShowEntity? = tvShowDao.getTvShowByTmdbId(tmdbId)

    override suspend fun updateItemLogo(
        tmdbId: Int,
        logoUrl: String?,
    ) = tvShowDao.updateTvShowLogo(tmdbId, logoUrl)

    override suspend fun clearNullLogos() = tvShowDao.clearNullLogos()

    suspend fun getTvShowTrailer(tmdbId: Int): String? {
        return trailerService.getTvShowTrailer(tmdbId)
    }
}
