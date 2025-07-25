package com.strmr.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.strmr.ai.ui.navigation.NavigationBar
import com.strmr.ai.ui.screens.DebridCloudPage
import com.strmr.ai.ui.screens.HomePage
import com.strmr.ai.ui.screens.MoviesPage
import com.strmr.ai.ui.screens.PlaceholderPage
import com.strmr.ai.ui.screens.SearchPage
import com.strmr.ai.ui.screens.SettingsPage
import com.strmr.ai.ui.screens.StreamSelectionPage
import com.strmr.ai.ui.screens.TvShowsPage
import com.strmr.ai.ui.screens.TraktSettingsPage
import com.strmr.ai.ui.screens.SimplePremiumizeSettingsPage
import com.strmr.ai.ui.screens.RealDebridSettingsPage
import com.strmr.ai.ui.screens.SplashScreen
import com.strmr.ai.ui.screens.OnboardingScreen
import com.strmr.ai.ui.theme.StrmrTheme
import androidx.compose.ui.focus.FocusRequester
import com.strmr.ai.data.OnboardingService
import com.strmr.ai.viewmodel.OnboardingViewModel
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.screens.DetailsPage
import com.strmr.ai.ui.screens.MediaDetailsType
import com.strmr.ai.ui.screens.VideoPlayerScreen
import com.strmr.ai.ui.screens.EpisodeView
import com.strmr.ai.ui.screens.IntermediateViewPage
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import com.strmr.ai.viewmodel.HomeViewModel
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.YouTubeExtractor
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import androidx.media3.common.util.UnstableApi

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var movieRepository: MovieRepository
    
    @Inject
    lateinit var tvShowRepository: TvShowRepository
    
    @Inject
    lateinit var youtubeExtractor: YouTubeExtractor
    
    @Inject
    lateinit var onboardingService: OnboardingService

    @OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("FontLoading", "🚀 MainActivity onCreate started")
        
        // Clear null logos on app start to allow retries
        clearNullLogos()
        
        setContent {
            android.util.Log.d("FontLoading", "🎨 Setting content with StrmrTheme")
            StrmrTheme {
                android.util.Log.d("FontLoading", "✅ StrmrTheme applied, creating Surface")
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showOnboarding by remember { mutableStateOf(false) }
                    
                    // Check onboarding status after splash completes
                    LaunchedEffect(showSplash) {
                        if (!showSplash) {
                            showOnboarding = !onboardingService.isOnboardingCompleted()
                        }
                    }
                    
                    when {
                        showSplash -> {
                            SplashScreen(
                                onSplashComplete = { showSplash = false }
                            )
                        }
                        showOnboarding -> {
                            OnboardingScreen(
                                onOnboardingComplete = { showOnboarding = false }
                            )
                        }
                        else -> {
                            MainScreen(youtubeExtractor = youtubeExtractor)
                        }
                    }
                }
            }
        }
    }
    
    private fun clearNullLogos() {
        // Use a coroutine to clear null logos asynchronously with proper error handling
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Batch these operations for better performance
                val movieJob = launch { movieRepository.clearNullLogos() }
                val tvShowJob = launch { tvShowRepository.clearNullLogos() }
                
                // Wait for both to complete
                movieJob.join()
                tvShowJob.join()
                
                Log.d("MainActivity", "✅ Cleared null logos for retry")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error clearing null logos", e)
            }
        }
    }
}

@Composable
fun MainScreen(youtubeExtractor: YouTubeExtractor) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainDestinations = setOf("search", "home", "movies", "tvshows", "debridcloud", "settings")
    
    // Create focus requesters for navigation and main content
    val navFocusRequester = remember { FocusRequester() }
    val mainContentFocusRequester = remember { FocusRequester() }
    
    // Track content focus state
    var isContentFocused by remember { mutableStateOf(false) }
    
    // Reset content focus when route changes with debouncing
    LaunchedEffect(currentRoute) {
        isContentFocused = false
        // Add a small delay to prevent focus conflicts during navigation
        kotlinx.coroutines.delay(50)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main Content Area with focus scope (fills the box)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(mainContentFocusRequester)
        ) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("search") {
                    SearchPage(
                        onNavigateToDetails = { mediaType, tmdbId ->
                            navController.navigate("details/$mediaType/$tmdbId")
                        }
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
                        onNavigateToDetails = { mediaType, tmdbId, season, episode ->
                            val route = if (season != null && episode != null) {
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
                        }
                    )
                }
                composable("movies") {
                    MoviesPage(
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onNavigateToDetails = { tmdbId ->
                            navController.navigate("details/movie/$tmdbId")
                        }
                    )
                }
                composable("tvshows") {
                    TvShowsPage(
                        isContentFocused = isContentFocused,
                        onContentFocusChanged = { focused ->
                            isContentFocused = focused
                        },
                        onNavigateToDetails = { tmdbId ->
                            navController.navigate("details/tvshow/$tmdbId")
                        }
                    )
                }
                composable("debridcloud") {
                    DebridCloudPage()
                }
                composable("settings") {
                    SettingsPage(
                        onNavigateToTraktSettings = { navController.navigate("trakt_settings") },
                        onNavigateToPremiumizeSettings = { navController.navigate("premiumize_settings") },
                        onNavigateToRealDebridSettings = { navController.navigate("realdebrid_settings") }
                    )
                }
                composable("trakt_settings") {
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    TraktSettingsPage(
                        onBackPressed = { navController.popBackStack() },
                        onTraktAuthorized = { homeViewModel.refreshContinueWatching() }
                    )
                }
                composable("premiumize_settings") {
                    SimplePremiumizeSettingsPage(
                        onBackPressed = { navController.popBackStack() }
                    )
                }
                composable("realdebrid_settings") {
                    RealDebridSettingsPage(
                        onBackPressed = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "details/{mediaType}/{tmdbId}",
                    arguments = listOf(
                        navArgument("mediaType") { type = NavType.StringType },
                        navArgument("tmdbId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val mediaType = backStackEntry.arguments?.getString("mediaType")
                    val tmdbId = backStackEntry.arguments?.getInt("tmdbId")
                    val season: Int? = null
                    val episode: Int? = null
                    
                    // Debug logging for navigation parameters
                    Log.d("MainActivity", "🎯 DEBUG: Navigation to details - mediaType: $mediaType, tmdbId: $tmdbId, season: $season, episode: $episode")
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
                        "movie" -> DetailsPage(
                            mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                            viewModel = detailsViewModel,
                            onPlay = { _, _ ->
                                movie?.let { movieEntity ->
                                    val imdbId = movieEntity.imdbId ?: ""
                                    val title = movieEntity.title ?: "Unknown Movie"
                                    val backdrop = movieEntity.backdropUrl ?: ""
                                    val logo = movieEntity.logoUrl ?: ""
                                    Log.d("MainActivity", "🎬 Play button clicked for movie: $title (IMDB: $imdbId)")
                                    val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                    val encodedLogo = java.net.URLEncoder.encode(logo, "UTF-8")
                                    navController.navigate("stream_selection/movie/$imdbId/$title/$encodedBackdrop/$encodedLogo")
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
                            }
                        )
                        "tvshow" -> DetailsPage(
                            mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                            viewModel = detailsViewModel,
                            onPlay = { selectedSeason, selectedEpisode ->
                                show?.let { showEntity ->
                                    val imdbId = showEntity.imdbId ?: ""
                                    val title = showEntity.title ?: "Unknown TV Show"
                                    val backdrop = showEntity.backdropUrl ?: ""
                                    val logo = showEntity.logoUrl ?: ""
                                    Log.d("MainActivity", "📺 Play button clicked for TV show: $title (IMDB: $imdbId) S${selectedSeason}E${selectedEpisode}")
                                    val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                    val encodedLogo = java.net.URLEncoder.encode(logo, "UTF-8")
                                    navController.navigate("stream_selection/tvshow/$imdbId/$title/$encodedBackdrop/$encodedLogo/$selectedSeason/$selectedEpisode")
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
                                navController.navigate("episode_view/$tmdbId/${season ?: 1}/${episode ?: 1}")
                            }
                        )
                    }
                }
                composable(
                    route = "details/{mediaType}/{tmdbId}/{season}/{episode}",
                    arguments = listOf(
                        navArgument("mediaType") { type = NavType.StringType },
                        navArgument("tmdbId") { type = NavType.IntType },
                        navArgument("season") { type = NavType.IntType },
                        navArgument("episode") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val mediaType = backStackEntry.arguments?.getString("mediaType")
                    val tmdbId = backStackEntry.arguments?.getInt("tmdbId")
                    val season = backStackEntry.arguments?.getInt("season")
                    val episode = backStackEntry.arguments?.getInt("episode")
                    
                    // Debug logging for navigation parameters
                    Log.d("MainActivity", "🎯 DEBUG: Navigation to details - mediaType: $mediaType, tmdbId: $tmdbId, season: $season, episode: $episode")
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
                        "movie" -> DetailsPage(
                            mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                            viewModel = detailsViewModel,
                            onPlay = { _, _ ->
                                movie?.let { movieEntity ->
                                    val imdbId = movieEntity.imdbId ?: ""
                                    val title = movieEntity.title ?: "Unknown Movie"
                                    val backdrop = movieEntity.backdropUrl ?: ""
                                    val logo = movieEntity.logoUrl ?: ""
                                    Log.d("MainActivity", "🎬 Play button clicked for movie: $title (IMDB: $imdbId)")
                                    val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                    val encodedLogo = java.net.URLEncoder.encode(logo, "UTF-8")
                                    navController.navigate("stream_selection/movie/$imdbId/$title/$encodedBackdrop/$encodedLogo")
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
                            }
                        )
                        "tvshow" -> DetailsPage(
                            mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                            viewModel = detailsViewModel,
                            onPlay = { selectedSeason, selectedEpisode ->
                                show?.let { showEntity ->
                                    val imdbId = showEntity.imdbId ?: ""
                                    val title = showEntity.title ?: "Unknown TV Show"
                                    val backdrop = showEntity.backdropUrl ?: ""
                                    val logo = showEntity.logoUrl ?: ""
                                    Log.d("MainActivity", "📺 Play button clicked for TV show: $title (IMDB: $imdbId) S${selectedSeason}E${selectedEpisode}")
                                    val encodedBackdrop = java.net.URLEncoder.encode(backdrop, "UTF-8")
                                    val encodedLogo = java.net.URLEncoder.encode(logo, "UTF-8")
                                    navController.navigate("stream_selection/tvshow/$imdbId/$title/$encodedBackdrop/$encodedLogo/$selectedSeason/$selectedEpisode")
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
                                navController.navigate("episode_view/$tmdbId/${season ?: 1}/${episode ?: 1}")
                            }
                        )
                    }
                }
                // Stream Selection Routes
                composable(
                    route = "stream_selection/movie/{imdbId}/{title}/{backdrop}/{logo}",
                    arguments = listOf(
                        navArgument("imdbId") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("backdrop") { type = NavType.StringType },
                        navArgument("logo") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Unknown Movie"
                    val encodedBackdrop = backStackEntry.arguments?.getString("backdrop") ?: ""
                    val encodedLogo = backStackEntry.arguments?.getString("logo") ?: ""
                    val backdropUrl = if (encodedBackdrop.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedBackdrop, "UTF-8")
                    } else null
                    val logoUrl = if (encodedLogo.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedLogo, "UTF-8")
                    } else null
                    
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
                        }
                    )
                }
                composable(
                    route = "stream_selection/tvshow/{imdbId}/{title}/{backdrop}/{logo}/{season}/{episode}",
                    arguments = listOf(
                        navArgument("imdbId") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType },
                        navArgument("backdrop") { type = NavType.StringType },
                        navArgument("logo") { type = NavType.StringType },
                        navArgument("season") { type = NavType.IntType },
                        navArgument("episode") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val imdbId = backStackEntry.arguments?.getString("imdbId") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Unknown TV Show"
                    val encodedBackdrop = backStackEntry.arguments?.getString("backdrop") ?: ""
                    val encodedLogo = backStackEntry.arguments?.getString("logo") ?: ""
                    val backdropUrl = if (encodedBackdrop.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedBackdrop, "UTF-8")
                    } else null
                    val logoUrl = if (encodedLogo.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedLogo, "UTF-8")
                    } else null
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
                                val streamTitle = "$title S${season}E${episode}"
                                val encodedTitle = java.net.URLEncoder.encode(streamTitle, "UTF-8")
                                navController.navigate("video_player/$encodedUrl/$encodedTitle")
                            }
                        }
                    )
                }
                composable(
                    route = "video_player/{videoUrl}/{title}",
                    arguments = listOf(
                        navArgument("videoUrl") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
                    val title = backStackEntry.arguments?.getString("title") ?: "Trailer"
                    
                    // Decode URL if needed
                    val decodedUrl = java.net.URLDecoder.decode(videoUrl, "UTF-8")
                    
                    VideoPlayerScreen(
                        videoUrl = decodedUrl,
                        title = title,
                        onBack = { navController.popBackStack() },
                        youtubeExtractor = youtubeExtractor
                    )
                }
                composable(
                    route = "episode_view/{tmdbId}/{season}/{episode}",
                    arguments = listOf(
                        navArgument("tmdbId") { type = NavType.IntType },
                        navArgument("season") { type = NavType.IntType },
                        navArgument("episode") { type = NavType.IntType }
                    )
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

                    show?.let {
                        EpisodeView(
                            show = it,
                            viewModel = detailsViewModel,
                            onEpisodeClick = { selectedSeason, selectedEpisode ->
                                navController.navigate("details/tvshow/$tmdbId/$selectedSeason/$selectedEpisode")
                            },
                            onBack = { navController.popBackStack() },
                            initialSeason = season,
                            initialEpisode = episode
                        )
                    }
                }
                composable(
                    route = "intermediate_view/{viewType}/{itemId}/{itemName}/{itemBackgroundUrl}/{dataUrl}",
                    arguments = listOf(
                        navArgument("viewType") { type = NavType.StringType },
                        navArgument("itemId") { type = NavType.StringType },
                        navArgument("itemName") { type = NavType.StringType },
                        navArgument("itemBackgroundUrl") { type = NavType.StringType },
                        navArgument("dataUrl") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val viewType = backStackEntry.arguments?.getString("viewType") ?: ""
                    val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                    val encodedName = backStackEntry.arguments?.getString("itemName") ?: ""
                    val encodedBackgroundUrl = backStackEntry.arguments?.getString("itemBackgroundUrl") ?: ""
                    val encodedDataUrl = backStackEntry.arguments?.getString("dataUrl") ?: ""
                    
                    val itemName = java.net.URLDecoder.decode(encodedName, "UTF-8")
                    val itemBackgroundUrl = if (encodedBackgroundUrl.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedBackgroundUrl, "UTF-8")
                    } else null
                    val dataUrl = if (encodedDataUrl.isNotEmpty()) {
                        java.net.URLDecoder.decode(encodedDataUrl, "UTF-8")
                    } else null
                    
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
                        }
                    )
                }
            }
        }
        // Overlay NavigationBar only on main destinations
        if (currentRoute in mainDestinations) {
            NavigationBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(1f),
                onRightPressed = {
                    android.util.Log.d("MainActivity", "🎯 NavigationBar right pressed, setting isContentFocused = true")
                    isContentFocused = true
                }
            )
        }
    }
}