package com.strmr.ai

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.OnboardingService
import com.strmr.ai.data.ScraperRepository
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.YouTubeExtractor
import com.strmr.ai.domain.usecase.ClearObsoleteDataUseCase
import com.strmr.ai.ui.navigation.NavigationBar
import com.strmr.ai.ui.screens.DebridCloudPage
import com.strmr.ai.ui.screens.DetailsPage
import com.strmr.ai.ui.screens.EpisodeView
import com.strmr.ai.ui.screens.HomePage
import com.strmr.ai.ui.screens.IntermediateViewPage
import com.strmr.ai.ui.screens.MediaDetailsType
import com.strmr.ai.ui.screens.MoviesPage
import com.strmr.ai.ui.screens.OnboardingScreen
import com.strmr.ai.ui.screens.RealDebridSettingsPage
import com.strmr.ai.ui.screens.SearchPage
import com.strmr.ai.ui.screens.SettingsPage
import com.strmr.ai.ui.screens.SimplePremiumizeSettingsPage
import com.strmr.ai.ui.screens.SplashScreen
import com.strmr.ai.ui.screens.StreamSelectionPage
import com.strmr.ai.ui.screens.TraktSettingsPage
import com.strmr.ai.ui.screens.TvShowsPage
import com.strmr.ai.ui.screens.VideoPlayerScreen
import com.strmr.ai.ui.theme.StrmrTheme
import com.strmr.ai.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

// Focus memory data classes
data class FocusMemory(
    val lastFocusedItem: String = "",
    val additionalData: Map<String, Any> = emptyMap(),
)

// Focus memory manager
class FocusMemoryManager {
    private val memories = mutableMapOf<String, FocusMemory>()

    fun storeFocus(
        screenKey: String,
        focusedItem: String,
        additionalData: Map<String, Any> = emptyMap(),
    ) {
        try {
            if (screenKey.isNotBlank() && focusedItem.isNotBlank()) {
                memories[screenKey] = FocusMemory(focusedItem, additionalData)
                Log.d("FocusMemory", " Stored focus for $screenKey: $focusedItem")
            }
        } catch (e: Exception) {
            Log.e("FocusMemory", " Error storing focus for $screenKey", e)
        }
    }

    fun getFocus(screenKey: String): FocusMemory? {
        return try {
            if (screenKey.isNotBlank()) {
                val memory = memories[screenKey]
                Log.d("FocusMemory", " Retrieved focus for $screenKey: ${memory?.lastFocusedItem}")
                memory
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FocusMemory", " Error retrieving focus for $screenKey", e)
            null
        }
    }

    fun clearFocus(screenKey: String) {
        try {
            if (screenKey.isNotBlank()) {
                memories.remove(screenKey)
                Log.d("FocusMemory", " Cleared focus for $screenKey")
            }
        } catch (e: Exception) {
            Log.e("FocusMemory", " Error clearing focus for $screenKey", e)
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var movieRepository: MovieRepository

    @Inject
    lateinit var tvShowRepository: TvShowRepository

    @Inject
    lateinit var youtubeExtractor: YouTubeExtractor

    // Clean architecture use case
    @Inject
    lateinit var clearObsoleteDataUseCase: ClearObsoleteDataUseCase

    @Inject
    lateinit var onboardingService: OnboardingService

    @Inject
    lateinit var scraperRepository: ScraperRepository

    private val focusMemoryManager = FocusMemoryManager()

    @OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("FontLoading", "🚀 MainActivity onCreate started")

        setContent {
            android.util.Log.d("FontLoading", "🎨 Setting content with StrmrTheme")
            StrmrTheme {
                android.util.Log.d("FontLoading", "✅ StrmrTheme applied, creating Surface")
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showOnboarding by remember { mutableStateOf(false) }

                    // Move heavy initialization to background after UI is set up
                    LaunchedEffect(Unit) {
                        try {
                            // Clear null logos on background thread
                            clearNullLogos()
                        } catch (e: Exception) {
                            Log.e("MainActivity", "❌ Error during background initialization", e)
                        }
                    }

                    // Check onboarding status after splash completes
                    LaunchedEffect(showSplash) {
                        if (!showSplash) {
                            try {
                                showOnboarding = !onboardingService.isOnboardingCompleted()
                            } catch (e: Exception) {
                                Log.e("MainActivity", "❌ Error checking onboarding status", e)
                                showOnboarding = false // Default to false on error
                            }
                        }
                    }

                    when {
                        showSplash -> {
                            SplashScreen(
                                onSplashComplete = { showSplash = false },
                            )
                        }
                        showOnboarding -> {
                            OnboardingScreen(
                                onOnboardingComplete = { showOnboarding = false },
                            )
                        }
                        else -> {
                            @OptIn(UnstableApi::class)
                            MainScreen(
                                youtubeExtractor = youtubeExtractor,
                                scraperRepository = scraperRepository,
                                focusMemoryManager = focusMemoryManager,
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun clearNullLogos() {
        // Use clean architecture use case to clear null logos
        try {
            Log.d("MainActivity", "🏗️ Clearing obsolete data with clean architecture")

            clearObsoleteDataUseCase()
                .onSuccess {
                    Log.d("MainActivity", "✅ Successfully cleared obsolete data")
                }
                .onFailure { exception ->
                    Log.e("MainActivity", "❌ Error clearing obsolete data", exception)
                }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Exception in clearNullLogos", e)
        }
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                // Backspace key - treat as back button
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    youtubeExtractor: YouTubeExtractor,
    scraperRepository: ScraperRepository,
    focusMemoryManager: FocusMemoryManager,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainDestinations = setOf("search", "home", "movies", "tvshows", "debridcloud", "settings")

    // State for debrid authorization dialog
    var showDebridDialog by remember { mutableStateOf(false) }

    // Create focus requesters for navigation and main content
    val navFocusRequester = remember { FocusRequester() }
    val mainContentFocusRequester = remember { FocusRequester() }

    // Track content focus state
    var isContentFocused by remember { mutableStateOf(false) }

    // Helper function to handle navigation back to sidebar
    val handleReturnToNavigation: () -> Unit = {
        try {
            android.util.Log.d("MainActivity", "🎯 Returning focus to navigation bar")
            // Immediately set content focus to false for instant navbar color update
            isContentFocused = false
            // Request focus on navigation bar
            navFocusRequester.requestFocus()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error in handleReturnToNavigation", e)
            // Fallback: set content focus to false
            isContentFocused = false
        }
    }

    // Track navigation state to prevent focus conflicts
    var isNavigating by remember { mutableStateOf(false) }
    
    // Reset content focus when route changes with improved debouncing
    LaunchedEffect(currentRoute) {
        isNavigating = true
        isContentFocused = false
        
        Log.d("MainActivity", "🚀 Navigation to route: $currentRoute")
        
        // Longer delay for navigation transitions, especially back navigation
        kotlinx.coroutines.delay(200)
        isNavigating = false
        
        Log.d("MainActivity", "✅ Navigation completed to route: $currentRoute")
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        // Main Content Area with focus scope (fills the box)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .focusRequester(mainContentFocusRequester),
        ) {
            NavHost(
                navController = navController,
                startDestination = "home",
            ) {
                composable("search") {
                    SearchPage(
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onLeftBoundary = handleReturnToNavigation,
                        onNavigateToDetails = { mediaType, tmdbId ->
                            navController.navigate("details/$mediaType/$tmdbId")
                        },
                    )
                }
                composable("home") {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    HomePage(
                        viewModel = homeViewModel,
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onLeftBoundary = handleReturnToNavigation,
                        onNavigateToDetails = { mediaType, tmdbId, season, episode ->
                            val route =
                                if (season != null && episode != null) {
                                    "details/$mediaType/$tmdbId/$season/$episode"
                                } else {
                                    "details/$mediaType/$tmdbId"
                                }
                            navController.navigate(route)
                        },
                        onNavigateToIntermediateView = { viewType, itemId, itemName, itemBackgroundUrl, dataUrl ->
                            val encodedName = java.net.URLEncoder.encode(itemName, "UTF-8")
                            val encodedBackgroundUrl = itemBackgroundUrl?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                            val encodedDataUrl = dataUrl?.let { java.net.URLEncoder.encode(it, "UTF-8") } ?: ""
                            navController.navigate("intermediate_view/$viewType/$itemId/$encodedName/$encodedBackgroundUrl/$encodedDataUrl")
                        },
                    )
                }
                composable("movies") {
                    MoviesPage(
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onLeftBoundary = handleReturnToNavigation,
                        onNavigateToDetails = { tmdbId ->
                            navController.navigate("details/movie/$tmdbId")
                        },
                    )
                }
                composable("tvshows") {
                    TvShowsPage(
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onLeftBoundary = handleReturnToNavigation,
                        onNavigateToDetails = { tmdbId ->
                            navController.navigate("details/tvshow/$tmdbId")
                        },
                    )
                }
                composable("debridcloud") {
                    DebridCloudPage()
                }
                composable("settings") {
                    SettingsPage(
                        onLeftBoundary = handleReturnToNavigation,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        isContentFocused = isContentFocused,
                    )
                }
                composable("trakt_settings") {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val scope = rememberCoroutineScope()
                    TraktSettingsPage(
                        onBackPressed = { navController.popBackStack() },
                        onTraktAuthorized = {
                            // Add small delay to ensure tokens are fully saved
                            scope.launch {
                                delay(100) // Small delay to ensure token persistence
                                homeViewModel.loadContinueWatching()
                                homeViewModel.refreshTraktLists()
                            }
                        },
                    )
                }
                composable("premiumize_settings") {
                    SimplePremiumizeSettingsPage(
                        onBackPressed = { navController.popBackStack() },
                    )
                }
                composable("realdebrid_settings") {
                    RealDebridSettingsPage(
                        onBackPressed = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "details/{mediaType}/{tmdbId}",
                    arguments =
                        listOf(
                            navArgument("mediaType") { type = NavType.StringType },
                            navArgument("tmdbId") { type = NavType.IntType },
                        ),
                ) { backStackEntry ->
                    val mediaType = backStackEntry.arguments?.getString("mediaType")
                    val tmdbId = backStackEntry.arguments?.getInt("tmdbId")
                    val season: Int? = null
                    val episode: Int? = null

                    // Debug logging for navigation parameters
                    Log.d(
                        "MainActivity",
                        "🎯 DEBUG: Navigation to details - mediaType: $mediaType, tmdbId: $tmdbId, season: $season, episode: $episode",
                    )
                    Log.d("MainActivity", "🎯 Current navigation stack: ${navController.currentBackStackEntry?.destination?.route}")
                    val detailsViewModel: com.strmr.ai.viewmodel.DetailsViewModel = hiltViewModel()
                    val movie by detailsViewModel.movie.collectAsState()
                    val show by detailsViewModel.tvShow.collectAsState()

                    LaunchedEffect(mediaType, tmdbId) {
                        if (mediaType == "movie" && tmdbId != null) {
                            detailsViewModel.loadMovie(tmdbId)
                        } else if (mediaType == "tvshow" && tmdbId != null) {
                            detailsViewModel.loadTvShow(tmdbId)
                        }
                    }
                    when (mediaType) {
                        "movie" ->
                            DetailsPage(
                                mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                                viewModel = detailsViewModel,
                                focusMemoryManager = focusMemoryManager,
                                screenKey = "details_movie_$tmdbId",
                                onPlay = { _, _ ->
                                    movie?.let { movieEntity ->
                                        if (!scraperRepository.isPremiumizeConfigured()) {
                                            showDebridDialog = true
                                        } else {
                                            val imdbId = movieEntity.imdbId ?: ""
                                            val title = movieEntity.title ?: "Unknown Movie"
                                            val backdrop = movieEntity.backdropUrl ?: ""
                                            val logo = movieEntity.logoUrl ?: ""
                                            Log.d("MainActivity", "🎬 Play button clicked for movie: $title (IMDB: $imdbId)")
                                            val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                            val encodedLogo = java.net.URLEncoder.encode(logo.ifEmpty { "EMPTY" }, "UTF-8")
                                            navController.navigate("stream_selection/movie/$imdbId/$title/$encodedBackdrop/$encodedLogo")
                                        }
                                    }
                                },
                                onNavigateToSimilar = { mediaType, tmdbId ->
                                    val route = "details/$mediaType/$tmdbId"
                                    navController.navigate(route)
                                },
                                onTrailer = { videoUrl, title ->
                                    val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("video_player/$encodedUrl/$encodedTitle")
                                },
                            )
                        "tvshow" ->
                            DetailsPage(
                                mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                                viewModel = detailsViewModel,
                                focusMemoryManager = focusMemoryManager,
                                screenKey = "details_tvshow_$tmdbId",
                                onPlay = { selectedSeason, selectedEpisode ->
                                    show?.let { showEntity ->
                                        if (!scraperRepository.isPremiumizeConfigured()) {
                                            showDebridDialog = true
                                        } else {
                                            val imdbId = showEntity.imdbId ?: ""
                                            val title = showEntity.title ?: "Unknown TV Show"
                                            val backdrop = showEntity.backdropUrl ?: ""
                                            val logo = showEntity.logoUrl ?: ""
                                            Log.d(
                                                "MainActivity",
                                                "📺 Play button clicked for TV show: $title (IMDB: $imdbId) S${selectedSeason}E$selectedEpisode",
                                            )
                                            val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                            val encodedLogo = java.net.URLEncoder.encode(logo.ifEmpty { "EMPTY" }, "UTF-8")
                                            navController.navigate(
                                                "stream_selection/tvshow/$imdbId/$title/$encodedBackdrop/$encodedLogo/$selectedSeason/$selectedEpisode",
                                            )
                                        }
                                    }
                                },
                                onNavigateToSimilar = { mediaType, tmdbId ->
                                    val route = "details/$mediaType/$tmdbId"
                                    navController.navigate(route)
                                },
                                onTrailer = { videoUrl, title ->
                                    val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("video_player/$encodedUrl/$encodedTitle")
                                },
                                cachedSeason = season,
                                cachedEpisode = episode,
                                onMoreEpisodes = {
                                    // Store focus on "more episodes" button before navigating
                                    focusMemoryManager.storeFocus(
                                        "details_tvshow_$tmdbId",
                                        "more_episodes",
                                    )
                                    navController.navigate("episode_view/$tmdbId/${season ?: 1}/${episode ?: 1}")
                                },
                            )
                    }
                }
                composable(
                    route = "details/{mediaType}/{tmdbId}/{season}/{episode}",
                    arguments =
                        listOf(
                            navArgument("mediaType") { type = NavType.StringType },
                            navArgument("tmdbId") { type = NavType.IntType },
                            navArgument("season") { type = NavType.IntType },
                            navArgument("episode") { type = NavType.IntType },
                        ),
                ) { backStackEntry ->
                    val mediaType = backStackEntry.arguments?.getString("mediaType")
                    val tmdbId = backStackEntry.arguments?.getInt("tmdbId")
                    val season = backStackEntry.arguments?.getInt("season")
                    val episode = backStackEntry.arguments?.getInt("episode")

                    // Debug logging for navigation parameters
                    Log.d(
                        "MainActivity",
                        "🎯 DEBUG: Navigation to details - mediaType: $mediaType, tmdbId: $tmdbId, season: $season, episode: $episode",
                    )
                    val detailsViewModel: com.strmr.ai.viewmodel.DetailsViewModel = hiltViewModel()
                    val movie by detailsViewModel.movie.collectAsState()
                    val show by detailsViewModel.tvShow.collectAsState()

                    LaunchedEffect(mediaType, tmdbId) {
                        if (mediaType == "movie" && tmdbId != null) {
                            detailsViewModel.loadMovie(tmdbId)
                        } else if (mediaType == "tvshow" && tmdbId != null) {
                            detailsViewModel.loadTvShow(tmdbId)
                        }
                    }
                    when (mediaType) {
                        "movie" ->
                            DetailsPage(
                                mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                                viewModel = detailsViewModel,
                                focusMemoryManager = focusMemoryManager,
                                screenKey = "details_movie_$tmdbId",
                                onPlay = { _, _ ->
                                    movie?.let { movieEntity ->
                                        if (!scraperRepository.isPremiumizeConfigured()) {
                                            showDebridDialog = true
                                        } else {
                                            val imdbId = movieEntity.imdbId ?: ""
                                            val title = movieEntity.title ?: "Unknown Movie"
                                            val backdrop = movieEntity.backdropUrl ?: ""
                                            val logo = movieEntity.logoUrl ?: ""
                                            Log.d("MainActivity", "🎬 Play button clicked for movie: $title (IMDB: $imdbId)")
                                            val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                            val encodedLogo = java.net.URLEncoder.encode(logo.ifEmpty { "EMPTY" }, "UTF-8")
                                            navController.navigate("stream_selection/movie/$imdbId/$title/$encodedBackdrop/$encodedLogo")
                                        }
                                    }
                                },
                                onNavigateToSimilar = { mediaType, tmdbId ->
                                    val route = "details/$mediaType/$tmdbId"
                                    navController.navigate(route)
                                },
                                onTrailer = { videoUrl, title ->
                                    val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("video_player/$encodedUrl/$encodedTitle")
                                },
                            )
                        "tvshow" ->
                            DetailsPage(
                                mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                                viewModel = detailsViewModel,
                                focusMemoryManager = focusMemoryManager,
                                screenKey = "details_tvshow_$tmdbId",
                                onPlay = { selectedSeason, selectedEpisode ->
                                    show?.let { showEntity ->
                                        if (!scraperRepository.isPremiumizeConfigured()) {
                                            showDebridDialog = true
                                        } else {
                                            val imdbId = showEntity.imdbId ?: ""
                                            val title = showEntity.title ?: "Unknown TV Show"
                                            val backdrop = showEntity.backdropUrl ?: ""
                                            val logo = showEntity.logoUrl ?: ""
                                            Log.d(
                                                "MainActivity",
                                                "📺 Play button clicked for TV show: $title (IMDB: $imdbId) S${selectedSeason}E$selectedEpisode",
                                            )
                                            val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                            val encodedLogo = java.net.URLEncoder.encode(logo.ifEmpty { "EMPTY" }, "UTF-8")
                                            navController.navigate(
                                                "stream_selection/tvshow/$imdbId/$title/$encodedBackdrop/$encodedLogo/$selectedSeason/$selectedEpisode",
                                            )
                                        }
                                    }
                                },
                                onTrailer = { videoUrl, title ->
                                    val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                    navController.navigate("video_player/$encodedUrl/$encodedTitle")
                                },
                                cachedSeason = season,
                                cachedEpisode = episode,
                                onMoreEpisodes = {
                                    // Store focus on "more episodes" button before navigating
                                    focusMemoryManager.storeFocus(
                                        "details_tvshow_$tmdbId",
                                        "more_episodes",
                                    )
                                    navController.navigate("episode_view/$tmdbId/${season ?: 1}/${episode ?: 1}")
                                },
                            )
                    }
                }
                // Stream Selection Routes
                composable(
                    route = "stream_selection/movie/{imdbId}/{title}/{backdrop}/{logo}",
                    arguments =
                        listOf(
                            navArgument("imdbId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("backdrop") { type = NavType.StringType },
                            navArgument("logo") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Unknown Movie"
                    val encodedBackdrop = backStackEntry.arguments?.getString("backdrop") ?: ""
                    val encodedLogo = backStackEntry.arguments?.getString("logo") ?: ""
                    val backdropUrl =
                        if (encodedBackdrop.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedBackdrop, "UTF-8")
                        } else {
                            null
                        }
                    val logoUrl =
                        if (encodedLogo.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedLogo, "UTF-8")
                        } else {
                            null
                        }

                    StreamSelectionPage(
                        mediaTitle = title,
                        imdbId = imdbId,
                        type = "movie",
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl,
                        onBackPressed = { navController.popBackStack() },
                        onStreamSelected = { stream ->
                            stream.url?.let { url ->
                                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                navController.navigate("video_player/$encodedUrl/$encodedTitle")
                            }
                        },
                    )
                }
                composable(
                    route = "stream_selection/tvshow/{imdbId}/{title}/{backdrop}/{logo}/{season}/{episode}",
                    arguments =
                        listOf(
                            navArgument("imdbId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("backdrop") { type = NavType.StringType },
                            navArgument("logo") { type = NavType.StringType },
                            navArgument("season") { type = NavType.IntType },
                            navArgument("episode") { type = NavType.IntType },
                        ),
                ) { backStackEntry ->
                    val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Unknown TV Show"
                    val encodedBackdrop = backStackEntry.arguments?.getString("backdrop") ?: ""
                    val encodedLogo = backStackEntry.arguments?.getString("logo") ?: ""
                    val backdropUrl =
                        if (encodedBackdrop.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedBackdrop, "UTF-8")
                        } else {
                            null
                        }
                    val logoUrl =
                        if (encodedLogo.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedLogo, "UTF-8")
                        } else {
                            null
                        }
                    val season = backStackEntry.arguments?.getInt("season") ?: 1
                    val episode = backStackEntry.arguments?.getInt("episode") ?: 1

                    StreamSelectionPage(
                        mediaTitle = title,
                        imdbId = imdbId,
                        type = "tvshow",
                        backdropUrl = backdropUrl,
                        logoUrl = logoUrl,
                        season = season,
                        episode = episode,
                        onBackPressed = { navController.popBackStack() },
                        onStreamSelected = { stream ->
                            stream.url?.let { url ->
                                val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
                                val streamTitle = "$title S${season}E$episode"
                                val encodedTitle = java.net.URLEncoder.encode(streamTitle, "UTF-8")
                                navController.navigate("video_player/$encodedUrl/$encodedTitle")
                            }
                        },
                    )
                }
                @OptIn(UnstableApi::class)
                composable(
                    route = "video_player/{videoUrl}/{title}",
                    arguments =
                        listOf(
                            navArgument("videoUrl") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Trailer"

                    // Decode URL if needed
                    val decodedUrl = java.net.URLDecoder.decode(videoUrl, "UTF-8")

                    VideoPlayerScreen(
                        videoUrl = decodedUrl,
                        title = title,
                        onBack = { navController.popBackStack() },
                        youtubeExtractor = youtubeExtractor,
                    )
                }
                composable(
                    route = "episode_view/{tmdbId}/{season}/{episode}",
                    arguments =
                        listOf(
                            navArgument("tmdbId") { type = NavType.IntType },
                            navArgument("season") { type = NavType.IntType },
                            navArgument("episode") { type = NavType.IntType },
                        ),
                ) { backStackEntry ->
                    val tmdbId = backStackEntry.arguments?.getInt("tmdbId")
                    val season = backStackEntry.arguments?.getInt("season")
                    val episode = backStackEntry.arguments?.getInt("episode")

                    val detailsViewModel: com.strmr.ai.viewmodel.DetailsViewModel = hiltViewModel()
                    val show by detailsViewModel.tvShow.collectAsState()

                    LaunchedEffect(tmdbId) {
                        if (tmdbId != null) {
                            detailsViewModel.loadTvShow(tmdbId)
                        }
                    }

                    if (show == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        val alpha = remember { Animatable(0f) }
                        LaunchedEffect(Unit) {
                            alpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 420),
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { this.alpha = alpha.value },
                        ) {
                            EpisodeView(
                                show = show!!,
                                viewModel = detailsViewModel,
                                onEpisodeClick = { selectedSeason, selectedEpisode ->
                                    navController.navigate("details/tvshow/$tmdbId/$selectedSeason/$selectedEpisode")
                                },
                                onBack = { navController.popBackStack() },
                                initialSeason = season,
                                initialEpisode = episode,
                            )
                        }
                    }
                }
                composable(
                    route = "intermediate_view/{viewType}/{itemId}/{itemName}/{itemBackgroundUrl}/{dataUrl}",
                    arguments =
                        listOf(
                            navArgument("viewType") { type = NavType.StringType },
                            navArgument("itemId") { type = NavType.StringType },
                            navArgument("itemName") { type = NavType.StringType },
                            navArgument("itemBackgroundUrl") { type = NavType.StringType },
                            navArgument("dataUrl") { type = NavType.StringType },
                        ),
                ) { backStackEntry ->
                    val viewType = backStackEntry.arguments?.getString("viewType") ?: ""
                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                    val encodedName = backStackEntry.arguments?.getString("itemName") ?: ""
                    val encodedBackgroundUrl = backStackEntry.arguments?.getString("itemBackgroundUrl") ?: ""
                    val encodedDataUrl = backStackEntry.arguments?.getString("dataUrl") ?: ""

                    val itemName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                    val itemBackgroundUrl =
                        if (encodedBackgroundUrl.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedBackgroundUrl, "UTF-8")
                        } else {
                            null
                        }
                    val dataUrl =
                        if (encodedDataUrl.isNotEmpty()) {
                            java.net.URLDecoder.decode(encodedDataUrl, "UTF-8")
                        } else {
                            null
                        }

                    IntermediateViewPage(
                        viewType = viewType,
                        itemId = itemId,
                        itemName = itemName,
                        itemBackgroundUrl = itemBackgroundUrl,
                        dataUrl = dataUrl,
                        onNavigateToDetails = { mediaType, tmdbId ->
                            navController.navigate("details/$mediaType/$tmdbId")
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
        // Overlay NavigationBar only on main destinations
        if (currentRoute in mainDestinations) {
            NavigationBar(
                navController = navController,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(1f)
                        .focusRequester(navFocusRequester),
                isContentFocused = isContentFocused,
                onRightPressed = {
                    android.util.Log.d("MainActivity", "🎯 NavigationBar right pressed, setting isContentFocused = true")
                    isContentFocused = true
                },
                onFocusReceived = {
                    android.util.Log.d(
                        "MainActivity",
                        "🎯 NavigationBar focus received, setting isContentFocused = false",
                    )
                    isContentFocused = false
                },
            )
        }

        // Debrid authorization dialog
        if (showDebridDialog) {
            AlertDialog(
                onDismissRequest = { showDebridDialog = false },
                title = {
                    Text(
                        text = "Debrid Not Authorized",
                        color = Color.White,
                    )
                },
                text = {
                    Text(
                        text = "Debrid not authorized. Please authorize in settings.",
                        color = Color.White,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showDebridDialog = false },
                    ) {
                        Text(
                            text = "Okay",
                            color = Color.White,
                        )
                    }
                },
                containerColor = Color.Black.copy(alpha = 0.9f),
                tonalElevation = 8.dp,
            )
        }
    }
}
