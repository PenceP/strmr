package com.strmr.ai.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.data.database.PlaybackDao
import com.strmr.ai.data.database.TraktUserProfileDao
import com.strmr.ai.data.database.TraktUserStatsDao
import com.strmr.ai.data.database.TraktUserProfileEntity
import com.strmr.ai.data.database.TraktUserStatsEntity
import kotlinx.coroutines.flow.Flow
import com.strmr.ai.data.database.PlaybackEntity
import kotlinx.coroutines.flow.firstOrNull

class HomeRepository(
    private val context: Context,
    private val playbackDao: PlaybackDao,
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

    fun getContinueWatching(): Flow<List<PlaybackEntity>> = playbackDao.getPlaybackItems()

    suspend fun refreshContinueWatching(accountRepository: AccountRepository) {
        val playbackItems = accountRepository.getContinueWatching()
        Log.d("HomeRepository", "🎯 DEBUG: Received ${playbackItems.size} playback items from Trakt")
        
        val entities = playbackItems.map { item ->
            // Debug logging for each playback item
            Log.d("HomeRepository", "🎯 DEBUG: PlaybackItem - type: ${item.type}, show: ${item.show?.title}, episode: ${item.episode?.title}")
            Log.d("HomeRepository", "🎯 DEBUG: Episode data - season: ${item.episode?.season}, number: ${item.episode?.number}")
            
            PlaybackEntity(
                id = item.id,
                progress = item.progress,
                pausedAt = item.paused_at,
                type = item.type,
                movieTitle = item.movie?.title,
                movieTmdbId = item.movie?.ids?.tmdb,
                showTitle = item.show?.title,
                showTmdbId = item.show?.ids?.tmdb,
                episodeTitle = item.episode?.title,
                episodeSeason = item.episode?.season,
                episodeNumber = item.episode?.number,
                episodeTmdbId = item.episode?.ids?.tmdb
            )
        }
        
        // Debug logging for created entities
        entities.forEach { entity ->
            Log.d("HomeRepository", "🎯 DEBUG: Created PlaybackEntity - type: ${entity.type}, show: ${entity.showTitle}, season: ${entity.episodeSeason}, episode: ${entity.episodeNumber}")
        }
        
        playbackDao.clearPlaybackItems()
        playbackDao.insertPlaybackItems(entities)
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