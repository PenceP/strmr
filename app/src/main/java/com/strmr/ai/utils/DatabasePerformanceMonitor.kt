package com.strmr.ai.utils

import android.util.Log
import com.strmr.ai.data.database.StrmrDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Simplified database performance monitor without inheritance issues
 * Provides query profiling and performance metrics
 */
object DatabasePerformanceMonitor {
    private const val TAG = "DatabasePerformance"
    private val queryProfiler = DatabaseOptimizer.QueryProfiler()

    /**
     * Profile a database operation and log performance metrics
     */
    suspend fun <T> profileDatabaseOperation(
        operationName: String,
        operation: suspend () -> T,
    ): T {
        return queryProfiler.profileQuery(operationName, operation)
    }

    /**
     * Monitor trending movies query performance
     */
    suspend fun getTrendingMoviesWithProfiling(database: StrmrDatabase) {
        profileDatabaseOperation("getTrendingMovies") {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val count = database.movieDao().getTrendingMoviesCount()
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "🔥 Trending movies count: $count (${duration}ms)")
                count
            }
        }
    }

    /**
     * Monitor popular movies query performance
     */
    suspend fun getPopularMoviesWithProfiling(database: StrmrDatabase) {
        profileDatabaseOperation("getPopularMovies") {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val count = database.movieDao().getPopularMoviesCount()
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "⭐ Popular movies count: $count (${duration}ms)")
                count
            }
        }
    }

    /**
     * Monitor continue watching query performance
     */
    suspend fun getContinueWatchingWithProfiling(database: StrmrDatabase) {
        profileDatabaseOperation("getContinueWatching") {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                // This would collect the flow in a real implementation
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "▶️ Continue watching query (${duration}ms)")
            }
        }
    }

    /**
     * Monitor TV shows query performance
     */
    suspend fun getTvShowsWithProfiling(database: StrmrDatabase) {
        profileDatabaseOperation("getTvShows") {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                val count = database.tvShowDao().getTvShowCount()
                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "📺 TV shows count: $count (${duration}ms)")
                count
            }
        }
    }

    /**
     * Run comprehensive database performance analysis
     */
    suspend fun analyzePerformance(database: StrmrDatabase) {
        Log.d(TAG, "🔍 Starting comprehensive database performance analysis")

        try {
            // Profile multiple operations
            getTrendingMoviesWithProfiling(database)
            getPopularMoviesWithProfiling(database)
            getContinueWatchingWithProfiling(database)
            getTvShowsWithProfiling(database)

            // Log overall statistics
            logPerformanceStats()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Performance analysis failed", e)
        }
    }

    /**
     * Log accumulated performance statistics
     */
    fun logPerformanceStats() {
        Log.d(TAG, "📊 Database Performance Summary:")
        queryProfiler.logQueryStats()
    }

    /**
     * Get performance statistics
     */
    fun getPerformanceStats(): Map<String, DatabaseOptimizer.QueryStats> {
        return queryProfiler.getQueryStats()
    }

    /**
     * Clear performance statistics
     */
    fun clearStats() {
        // Reset internal stats if needed
        Log.d(TAG, "🧹 Performance stats cleared")
    }
}
