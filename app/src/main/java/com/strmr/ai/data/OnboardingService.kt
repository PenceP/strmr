package com.strmr.ai.data

import android.content.Context
import android.util.Log
import com.strmr.ai.data.database.StrmrDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsible for managing the onboarding experience and initial data population
 * 
 * Features:
 * - Pre-loads all page 1 data for configured rows
 * - Shows progress with fun loading messages
 * - Tracks onboarding completion state
 * - Provides elegant first-launch experience
 */
@Singleton
class OnboardingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: StrmrDatabase,
    private val genericRepository: GenericTraktRepository
) {
    
    companion object {
        private const val TAG = "OnboardingService"
        private const val PREFS_NAME = "strmr_onboarding"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ONBOARDING_VERSION = "onboarding_version"
        private const val CURRENT_ONBOARDING_VERSION = 1
    }
    
    private val _onboardingProgress = MutableStateFlow(
        OnboardingProgress(
            state = OnboardingState.NOT_STARTED,
            message = "Welcome to Strmr!",
            progress = 0.0f
        )
    )
    val onboardingProgress: StateFlow<OnboardingProgress> = _onboardingProgress.asStateFlow()
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Check if onboarding has been completed for the current version
     */
    fun isOnboardingCompleted(): Boolean {
        val completed = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        val version = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
        return completed && version >= CURRENT_ONBOARDING_VERSION
    }
    
    /**
     * Mark onboarding as completed
     */
    private fun markOnboardingCompleted() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putInt(KEY_ONBOARDING_VERSION, CURRENT_ONBOARDING_VERSION)
            .apply()
    }
    
    /**
     * Reset onboarding state (for testing or force refresh)
     */
    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .putInt(KEY_ONBOARDING_VERSION, 0)
            .apply()
        
        _onboardingProgress.value = OnboardingProgress(
            state = OnboardingState.NOT_STARTED,
            message = "Welcome to Strmr!",
            progress = 0.0f
        )
    }
    
    /**
     * Run the complete onboarding process
     */
    suspend fun runOnboarding(): Boolean {
        if (isOnboardingCompleted()) {
            Log.d(TAG, "✅ Onboarding already completed, skipping")
            _onboardingProgress.value = OnboardingProgress(
                state = OnboardingState.COMPLETED,
                message = "Ready to stream!",
                progress = 1.0f
            )
            return true
        }
        
        return try {
            Log.d(TAG, "🚀 Starting onboarding process")
            
            // Step 1: Initialize
            updateProgress(OnboardingState.INITIALIZING, "Initializing application...", 0.1f)
            delay(1000) // Give user time to see the message
            
            // Step 2: Load movie configurations and data
            updateProgress(OnboardingState.LOADING_MOVIES, "Loading movie data...", 0.2f)
            loadMovieData()
            
            // Step 3: Load TV show configurations and data  
            updateProgress(OnboardingState.LOADING_TV_SHOWS, "Loading TV show data...", 0.6f)
            loadTvShowData()
            
            // Step 4: Load special lists
            updateProgress(OnboardingState.LOADING_LISTS, "Loading curated lists...", 0.8f)
            loadSpecialLists()
            
            // Step 5: Finalize
            updateProgress(OnboardingState.FINALIZING, "Adding final touches...", 0.95f)
            delay(1000)
            
            // Complete
            updateProgress(OnboardingState.COMPLETED, "Ready to stream!", 1.0f)
            markOnboardingCompleted()
            
            Log.d(TAG, "✅ Onboarding completed successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Onboarding failed", e)
            updateProgress(
                OnboardingState.ERROR, 
                "Failed to load data: ${e.message}", 
                0.0f,
                error = e.message
            )
            false
        }
    }
    
    /**
     * Load all movie data sources (page 1)
     */
    private suspend fun loadMovieData() {
        val movieConfigs = getMovieConfigurations()
        val totalConfigs = movieConfigs.size
        
        movieConfigs.forEachIndexed { index, config ->
            val progress = 0.2f + (0.4f * (index.toFloat() / totalConfigs))
            val message = "Loading ${config.title}..."
            updateProgress(OnboardingState.LOADING_MOVIES, message, progress)
            
            try {
                Log.d(TAG, "📥 Loading movie data: ${config.title}")
                genericRepository.loadMovieDataSourcePageForOnboarding(config, page = 1, limit = 7)
                delay(500) // Small delay for smooth progress
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to load ${config.title}: ${e.message}")
                // Continue with other configs even if one fails
            }
        }
    }
    
    /**
     * Load all TV show data sources (page 1)
     */
    private suspend fun loadTvShowData() {
        val tvConfigs = getTvShowConfigurations()
        val totalConfigs = tvConfigs.size
        
        tvConfigs.forEachIndexed { index, config ->
            val progress = 0.6f + (0.2f * (index.toFloat() / totalConfigs))
            val message = "Loading ${config.title}..."
            updateProgress(OnboardingState.LOADING_TV_SHOWS, message, progress)
            
            try {
                Log.d(TAG, "📺 Loading TV show data: ${config.title}")
                genericRepository.loadTvDataSourcePageForOnboarding(config, page = 1, limit = 7)
                delay(500) // Small delay for smooth progress
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to load ${config.title}: ${e.message}")
                // Continue with other configs even if one fails
            }
        }
    }
    
    /**
     * Load special lists and user-curated content
     */
    private suspend fun loadSpecialLists() {
        // This could load additional curated lists, recommendations, etc.
        updateProgress(OnboardingState.LOADING_LISTS, "Loading Top Movies of the Week...", 0.85f)
        delay(1000)
        
        updateProgress(OnboardingState.LOADING_LISTS, "Generating recommendations...", 0.9f)
        delay(1000)
    }
    
    /**
     * Update progress state with optional fun message
     */
    private fun updateProgress(
        state: OnboardingState, 
        message: String, 
        progress: Float,
        error: String? = null
    ) {
        val funMessage = if (state == OnboardingState.LOADING_MOVIES || 
                               state == OnboardingState.LOADING_TV_SHOWS ||
                               state == OnboardingState.LOADING_LISTS) {
            OnboardingMessages.getRandomMessage()
        } else {
            message
        }
        
        _onboardingProgress.value = OnboardingProgress(
            state = state,
            message = funMessage,
            progress = progress,
            currentTask = message,
            error = error
        )
        
        Log.d(TAG, "📊 Progress: ${(progress * 100).toInt()}% - $funMessage")
    }
    
    /**
     * Get movie data source configurations
     */
    private fun getMovieConfigurations(): List<DataSourceConfig> {
        // This would ideally read from the JSON config files
        // For now, return the known configurations
        return listOf(
            DataSourceConfig(
                id = "trending",
                title = "Trending",
                endpoint = "movies/trending",
                mediaType = MediaType.MOVIE,
                cacheKey = "trending_movies",
                enabled = true,
                order = 1
            ),
            DataSourceConfig(
                id = "popular", 
                title = "Popular",
                endpoint = "movies/popular",
                mediaType = MediaType.MOVIE, 
                cacheKey = "popular_movies",
                enabled = true,
                order = 2
            ),
            DataSourceConfig(
                id = "top_movies_week",
                title = "Top Movies of the Week", 
                endpoint = "users/garycrawfordgc/lists/top-movies-of-the-week/items",
                mediaType = MediaType.MOVIE,
                cacheKey = "top_movies_week",
                enabled = true,
                order = 3
            )
        )
    }
    
    /**
     * Get TV show data source configurations  
     */
    private fun getTvShowConfigurations(): List<DataSourceConfig> {
        return listOf(
            DataSourceConfig(
                id = "trending",
                title = "Trending Shows",
                endpoint = "shows/trending", 
                mediaType = MediaType.TV_SHOW,
                cacheKey = "trending_shows",
                enabled = true,
                order = 1
            ),
            DataSourceConfig(
                id = "popular",
                title = "Popular Shows",
                endpoint = "shows/popular",
                mediaType = MediaType.TV_SHOW, 
                cacheKey = "popular_shows",
                enabled = true,
                order = 2
            )
        )
    }
}