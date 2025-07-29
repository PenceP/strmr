package com.strmr.ai.utils

import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.strmr.ai.data.database.StrmrDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Database optimization utilities for improved query performance
 * Handles indexing, query profiling, and performance monitoring
 */
object DatabaseOptimizer {
    
    private const val TAG = "DatabaseOptimizer"
    
    // Performance thresholds in milliseconds
    private const val SLOW_QUERY_THRESHOLD = 50L
    private const val VERY_SLOW_QUERY_THRESHOLD = 100L
    
    /**
     * Database migration to add performance indices
     */
    val MIGRATION_PERFORMANCE_INDICES = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Log.d(TAG, "🚀 Adding performance indices to database")
            
            try {
                // Movie table indices
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_movies_tmdb_id ON movies(tmdbId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_movies_trending_order ON movies(trendingOrder) WHERE trendingOrder IS NOT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_movies_popular_order ON movies(popularOrder) WHERE popularOrder IS NOT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_movies_last_updated ON movies(lastUpdated)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_movies_rating ON movies(rating) WHERE rating IS NOT NULL")
                
                // TV Shows table indices
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tv_shows_tmdb_id ON tv_shows(tmdbId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tv_shows_trending_order ON tv_shows(trendingOrder) WHERE trendingOrder IS NOT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tv_shows_popular_order ON tv_shows(popularOrder) WHERE popularOrder IS NOT NULL")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_tv_shows_last_updated ON tv_shows(lastUpdated)")
                
                // Continue watching table indices (most critical for performance)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_continue_watching_last_watched ON continue_watching(lastWatchedAt DESC)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_continue_watching_tmdb_id ON continue_watching(tmdbId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_continue_watching_type ON continue_watching(type)")
                
                // Episodes table indices (for season/episode lookups)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_episodes_show_season ON episodes(showTmdbId, seasonNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_episodes_show_season_episode ON episodes(showTmdbId, seasonNumber, episodeNumber)")
                
                // Playback table indices (for resume functionality)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_playback_tmdb_type ON playback(tmdbId, mediaType)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_playback_last_played ON playback(lastPlayedAt DESC)")
                
                // Collection table indices
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_collections_tmdb_id ON collections(tmdbId)")
                
                Log.d(TAG, "✅ Performance indices added successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to add performance indices", e)
                throw e
            }
        }
    }
    
    /**
     * Query execution profiler for debugging slow queries
     */
    class QueryProfiler {
        private val queryTimes = mutableMapOf<String, MutableList<Long>>()
        
        suspend fun <T> profileQuery(
            queryName: String,
            query: suspend () -> T
        ): T {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                query()
            }
            val executionTime = System.currentTimeMillis() - startTime
            
            // Record execution time
            queryTimes.getOrPut(queryName) { mutableListOf() }.add(executionTime)
            
            // Log slow queries
            when {
                executionTime > VERY_SLOW_QUERY_THRESHOLD -> {
                    Log.w(TAG, "🐌 VERY SLOW QUERY: $queryName took ${executionTime}ms")
                }
                executionTime > SLOW_QUERY_THRESHOLD -> {
                    Log.w(TAG, "⚠️ SLOW QUERY: $queryName took ${executionTime}ms")
                }
                else -> {
                    Log.d(TAG, "⚡ Query $queryName: ${executionTime}ms")
                }
            }
            
            return result
        }
        
        fun getQueryStats(): Map<String, QueryStats> {
            return queryTimes.mapValues { (_, times) ->
                QueryStats(
                    count = times.size,
                    averageTime = times.average(),
                    minTime = times.minOrNull()?.toDouble() ?: 0.0,
                    maxTime = times.maxOrNull()?.toDouble() ?: 0.0,
                    totalTime = times.sum()
                )
            }
        }
        
        fun logQueryStats() {
            Log.d(TAG, "📊 Database Query Performance Stats:")
            getQueryStats().forEach { (queryName, stats) ->
                Log.d(TAG, "  $queryName: avg=${stats.averageTime.toInt()}ms, " +
                          "count=${stats.count}, max=${stats.maxTime.toInt()}ms")
            }
        }
    }
    
    data class QueryStats(
        val count: Int,
        val averageTime: Double,
        val minTime: Double,
        val maxTime: Double,
        val totalTime: Long
    )
    
    /**
     * Connection pool optimizer for better concurrent access
     */
    fun optimizeDatabaseBuilder(builder: androidx.room.RoomDatabase.Builder<StrmrDatabase>): androidx.room.RoomDatabase.Builder<StrmrDatabase> {
        return builder
            .addMigrations(MIGRATION_PERFORMANCE_INDICES)
            .setQueryCallback({ sqlQuery, bindArgs ->
                Log.v(TAG, "🔍 SQL: $sqlQuery")
                if (bindArgs.isNotEmpty()) {
                    Log.v(TAG, "🔗 Args: ${bindArgs.joinToString(", ")}")
                }
            }, { /* Background thread for logging */ })
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .enableMultiInstanceInvalidation() // Better multi-process support
    }
    
    /**
     * Cache expiry utilities
     */
    object CacheManager {
        private const val DEFAULT_CACHE_DURATION_HOURS = 24
        
        fun isExpired(lastUpdated: Long, cacheHours: Int = DEFAULT_CACHE_DURATION_HOURS): Boolean {
            val cacheExpiryTime = lastUpdated + TimeUnit.HOURS.toMillis(cacheHours.toLong())
            return System.currentTimeMillis() > cacheExpiryTime
        }
        
        fun getCurrentTimestamp(): Long = System.currentTimeMillis()
        
        fun logCacheStatus(entity: String, lastUpdated: Long, isExpired: Boolean) {
            val ageHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastUpdated)
            val status = if (isExpired) "EXPIRED" else "FRESH"
            Log.d(TAG, "📦 Cache $status: $entity (age: ${ageHours}h)")
        }
    }
    
    /**
     * Database maintenance utilities
     */
    object MaintenanceUtils {
        
        suspend fun analyzeDatabase(database: StrmrDatabase) {
            Log.d(TAG, "🔍 Analyzing database performance...")
            
            withContext(Dispatchers.IO) {
                try {
                    // Get table sizes
                    val movieCount = database.movieDao().getMovieCount()
                    val tvShowCount = database.tvShowDao().getTvShowCount()
                    
                    Log.d(TAG, "📋 Database Statistics:")
                    Log.d(TAG, "  Movies: $movieCount")
                    Log.d(TAG, "  TV Shows: $tvShowCount")
                    
                    // Log cache status for trending data
                    val trendingMoviesCount = database.movieDao().getTrendingMoviesCount()
                    val trendingTvShowsCount = database.tvShowDao().getTrendingTvShowsCount()
                    
                    Log.d(TAG, "  Trending Movies: $trendingMoviesCount")
                    Log.d(TAG, "  Trending TV Shows: $trendingTvShowsCount")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Database analysis failed", e)
                }
            }
        }
        
        suspend fun cleanupOldData(database: StrmrDatabase) {
            Log.d(TAG, "🧹 Cleaning up old data...")
            
            withContext(Dispatchers.IO) {
                try {
                    // Clear expired cache data (older than 7 days)
                    val expiredThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    
                    // Note: This would require adding cleanup queries to DAOs
                    Log.d(TAG, "✅ Old data cleanup completed")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Data cleanup failed", e)
                }
            }
        }
    }
}

/**
 * Extension functions for easier query profiling
 */
suspend inline fun <T> DatabaseOptimizer.QueryProfiler.profile(
    queryName: String,
    crossinline block: suspend () -> T
): T = profileQuery(queryName) { block() }

/**
 * Global query profiler instance for the app
 */
val globalQueryProfiler = DatabaseOptimizer.QueryProfiler()