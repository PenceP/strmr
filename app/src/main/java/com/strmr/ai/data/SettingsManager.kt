package com.strmr.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val YOUTUBE_PLAYBACK_MODE = stringPreferencesKey("youtube_playback_mode")
        private val PREFERRED_VIDEO_QUALITY = stringPreferencesKey("preferred_video_quality")
        private val AUTO_PLAY_TRAILERS = booleanPreferencesKey("auto_play_trailers")
    }
    
    enum class YouTubePlaybackMode {
        EXTERNAL_APP,     // Open in YouTube app (default, most reliable)
        IN_APP_WEBVIEW,   // Try WebView embed
        IN_APP_EXTRACT    // Try extraction (experimental)
    }
    
    val youtubePlaybackMode: Flow<YouTubePlaybackMode> = context.dataStore.data
        .map { preferences ->
            val mode = preferences[YOUTUBE_PLAYBACK_MODE] ?: YouTubePlaybackMode.EXTERNAL_APP.name
            try {
                YouTubePlaybackMode.valueOf(mode)
            } catch (e: Exception) {
                YouTubePlaybackMode.EXTERNAL_APP
            }
        }
    
    val preferredVideoQuality: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PREFERRED_VIDEO_QUALITY] ?: "720p"
        }
    
    val autoPlayTrailers: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_PLAY_TRAILERS] ?: false
        }
    
    suspend fun setYouTubePlaybackMode(mode: YouTubePlaybackMode) {
        context.dataStore.edit { preferences ->
            preferences[YOUTUBE_PLAYBACK_MODE] = mode.name
        }
    }
    
    suspend fun setPreferredVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_VIDEO_QUALITY] = quality
        }
    }
    
    suspend fun setAutoPlayTrailers(autoPlay: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_PLAY_TRAILERS] = autoPlay
        }
    }
}