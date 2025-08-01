package com.strmr.ai.data

/**
 * Represents the current state of app onboarding and database initialization
 */
enum class OnboardingState {
    NOT_STARTED,
    INITIALIZING,
    LOADING_MOVIES,
    LOADING_TV_SHOWS,
    LOADING_LISTS,
    FINALIZING,
    COMPLETED,
    ERROR,
}

/**
 * Progress information for the onboarding process
 */
data class OnboardingProgress(
    val state: OnboardingState,
    val message: String,
    val progress: Float, // 0.0 to 1.0
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val currentTask: String? = null,
    val error: String? = null,
)

/**
 * Fun loading messages similar to Discord's approach
 */
object OnboardingMessages {
    val loadingMessages =
        listOf(
            "Hacking the mainframe...",
            "Populating the database...",
            "Downloading movie posters...",
            "Connecting to the Trakt API...",
            "Enriching with TMDB data...",
            "Loading trending content...",
            "Fetching popular movies...",
            "Synchronizing user lists...",
            "Optimizing for Android TV...",
            "Preparing your experience...",
            "Loading cast information...",
            "Generating recommendations...",
            "Caching poster images...",
            "Building media index...",
            "Establishing secure connections...",
            "Calibrating flux capacitor...",
            "Reticulating splines...",
            "Loading awesome content...",
            "Brewing fresh coffee...",
            "Consulting the movie gods...",
            "Summoning the perfect UI...",
            "Training recommendation AI...",
            "Polishing the interface...",
            "Adding final touches...",
        )

    fun getRandomMessage(): String = loadingMessages.random()

    fun getMessageForState(state: OnboardingState): String =
        when (state) {
            OnboardingState.NOT_STARTED -> "Welcome to Strmr!"
            OnboardingState.INITIALIZING -> "Initializing application..."
            OnboardingState.LOADING_MOVIES -> "Loading trending movies..."
            OnboardingState.LOADING_TV_SHOWS -> "Loading popular TV shows..."
            OnboardingState.LOADING_LISTS -> "Loading curated lists..."
            OnboardingState.FINALIZING -> "Adding final touches..."
            OnboardingState.COMPLETED -> "Ready to stream!"
            OnboardingState.ERROR -> "Something went wrong..."
        }
}
