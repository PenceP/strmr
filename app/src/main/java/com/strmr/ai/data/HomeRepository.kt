package com.strmr.ai.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.PlaybackDao
import com.strmr.ai.data.database.ContinueWatchingDao
import com.strmr.ai.data.database.TraktUserProfileDao
import com.strmr.ai.data.database.TraktUserStatsDao
import com.strmr.ai.data.database.TraktUserProfileEntity
import com.strmr.ai.data.database.TraktUserStatsEntity
import kotlinx.coroutines.flow.Flow
import com.strmr.ai.data.database.PlaybackEntity
import com.strmr.ai.data.database.ContinueWatchingEntity
import kotlinx.coroutines.flow.firstOrNull

class HomeRepository(
    private val context: Context,
    private val playbackDao: PlaybackDao,
    private val continueWatchingDao: ContinueWatchingDao,
    private val traktUserProfileDao: TraktUserProfileDao,
    private val traktUserStatsDao: TraktUserStatsDao
) {

    private var homeConfig: HomeConfig? = null

    fun getHomeConfig(): HomeConfig? {
        if (homeConfig != null) {
            return homeConfig
        }

        return try {
            val jsonString = context.assets.open("HOME.json").bufferedReader().use { it.readText() }
            val config = Gson().fromJson(jsonString, HomeConfig::class.java)
            homeConfig = config
            config
        } catch (e: Exception) {
            Log.e("HomeRepository", "Error reading HOME.json", e)
            null
        }
    }

    fun getContinueWatching(): Flow<List<ContinueWatchingEntity>> = continueWatchingDao.getContinueWatchingItems()

    suspend fun refreshContinueWatching(accountRepository: AccountRepository) {
        val continueWatchingItems = accountRepository.getContinueWatching()
        Log.d("HomeRepository", "🎬 Received ${continueWatchingItems.size} continue watching items from Trakt")
        
        val entities = continueWatchingItems.mapIndexed { index, item ->
            val id = when (item.type) {
                "movie" -> "movie_${item.movie?.ids?.tmdb ?: index}"
                "episode" -> {
                    val showId = item.show?.ids?.tmdb ?: index
                    val season = item.season ?: 0
                    val episode = item.episodeNumber ?: 0
                    "episode_${showId}_s${season}e${episode}"
                }
                else -> "item_$index"
            }
            
            ContinueWatchingEntity(
                id = id,
                type = item.type,
                lastWatchedAt = item.lastWatchedAt,
                progress = item.progress,
                movieTitle = item.movie?.title,
                movieTmdbId = item.movie?.ids?.tmdb,
                movieTraktId = item.movie?.ids?.trakt,
                movieYear = item.movie?.year,
                showTitle = item.show?.title,
                showTmdbId = item.show?.ids?.tmdb,
                showTraktId = item.show?.ids?.trakt,
                showYear = item.show?.year,
                episodeTitle = item.currentEpisode?.title ?: item.nextEpisode?.title,
                episodeSeason = item.season,
                episodeNumber = item.episodeNumber,
                episodeTmdbId = item.currentEpisode?.ids?.tmdb ?: item.nextEpisode?.ids?.tmdb,
                episodeTraktId = item.currentEpisode?.ids?.trakt ?: item.nextEpisode?.ids?.trakt,
                isNextEpisode = item.nextEpisode != null,
                isInProgress = item.progress != null && item.progress > 0f
            )
        }
        
        Log.d("HomeRepository", "✅ Created ${entities.size} continue watching entities")
        
        continueWatchingDao.clearContinueWatching()
        continueWatchingDao.insertContinueWatchingItems(entities)
    }

    fun getUserProfile(username: String): Flow<TraktUserProfileEntity?> =
        traktUserProfileDao.getUserProfile(username)

    suspend fun refreshUserProfile(accountRepository: AccountRepository) {
        val accessToken = accountRepository.refreshTokenIfNeeded("trakt")?.trim()
        if (!accessToken.isNullOrEmpty()) {
            val authService = RetrofitInstance.createAuthenticatedTraktService(accessToken)
            val profile = authService.getUserProfile()
            val entity = TraktUserProfileEntity(
                username = profile.username,
                private = profile.private,
                name = profile.name,
                vip = profile.vip,
                vipEp = profile.vip_ep,
                slug = profile.ids.slug,
                uuid = profile.ids.uuid
            )
            traktUserProfileDao.insertUserProfile(entity)
        }
    }

    fun getUserStats(username: String): Flow<TraktUserStatsEntity?> =
        traktUserStatsDao.getUserStats(username)

    suspend fun refreshUserStats(accountRepository: AccountRepository) {
        val accessToken = accountRepository.refreshTokenIfNeeded("trakt")?.trim()
        if (!accessToken.isNullOrEmpty()) {
            val authService = RetrofitInstance.createAuthenticatedTraktService(accessToken)
            val stats = authService.getUserStats()
            val gson = Gson()
            val username = traktUserProfileDao.getUserProfile("").firstOrNull()?.username ?: "me"
            val entity = TraktUserStatsEntity(
                username = username,
                moviesJson = gson.toJson(stats.movies),
                showsJson = gson.toJson(stats.shows),
                seasonsJson = gson.toJson(stats.seasons),
                episodesJson = gson.toJson(stats.episodes),
                networkJson = gson.toJson(stats.network),
                ratingsJson = gson.toJson(stats.ratings)
            )
            traktUserStatsDao.insertUserStats(entity)
        }
    }
} 