package com.strmr.ai.data

import android.util.Log
import com.google.gson.Gson
import com.strmr.ai.BuildConfig
import com.strmr.ai.data.database.OmdbRatingsDao
import com.strmr.ai.data.database.OmdbRatingsEntity

class OmdbRepository(
    private val omdbRatingsDao: OmdbRatingsDao,
    private val omdbApiService: OmdbApiService,
    private val gson: Gson = Gson(),
    private val cacheExpiryMs: Long = 7 * 24 * 60 * 60 * 1000L, // 7 days
) {
    suspend fun getOmdbRatings(imdbId: String): OmdbResponse? {
        Log.d("OmdbRepository", "🔍 getOmdbRatings called for IMDB ID: $imdbId")
        try {
            val cached = omdbRatingsDao.getOmdbRatings(imdbId)
            val now = System.currentTimeMillis()
            Log.d("OmdbRepository", "📦 Cached data found: ${cached != null}")
            if (cached != null) {
                Log.d("OmdbRepository", "📦 Cache age: ${now - cached.lastFetched}ms")
                Log.d("OmdbRepository", "📦 Cache expiry: ${cacheExpiryMs}ms")
            }

            if (cached != null && now - cached.lastFetched < cacheExpiryMs) {
                Log.d("OmdbRepository", "✅ Using cached data for $imdbId")
                val response = gson.fromJson(cached.omdbJson, OmdbResponse::class.java)
                Log.d("OmdbRepository", "✅ Cached response: $response")
                return response
            }

            // Fetch from API
            Log.d("OmdbRepository", "📡 Fetching from API for $imdbId")
            if (BuildConfig.DEBUG) {
                Log.d("OmdbRepository", "📡 API Key: ${BuildConfig.OMDB_API_KEY.take(5)}...")
            }
            val response = omdbApiService.getOmdbRatings(apiKey = BuildConfig.OMDB_API_KEY, imdbId = imdbId)
            Log.d("OmdbRepository", "✅ API response received: $response")

            Log.d("OmdbRepository", "💾 Caching response for $imdbId")
            omdbRatingsDao.insertOmdbRatings(
                OmdbRatingsEntity(
                    imdbId = imdbId,
                    omdbJson = gson.toJson(response),
                    lastFetched = now,
                ),
            )
            Log.d("OmdbRepository", "✅ Response cached successfully")
            return response
        } catch (e: Exception) {
            Log.e("OmdbRepository", "❌ Error fetching OMDb ratings for $imdbId", e)
            // Fallback to cache if available
            Log.d("OmdbRepository", "🔄 Attempting fallback to cached data")
            val cached = omdbRatingsDao.getOmdbRatings(imdbId)
            val fallbackResponse = cached?.let { gson.fromJson(it.omdbJson, OmdbResponse::class.java) }
            Log.d("OmdbRepository", "🔄 Fallback response: $fallbackResponse")
            return fallbackResponse
        }
    }
}
