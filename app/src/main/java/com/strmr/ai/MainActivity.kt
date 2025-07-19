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
import com.strmr.ai.ui.screens.TvShowsPage
import com.strmr.ai.ui.screens.TraktSettingsPage
import com.strmr.ai.ui.screens.PremiumizeSettingsPage
import com.strmr.ai.ui.screens.RealDebridSettingsPage
import com.strmr.ai.ui.theme.StrmrTheme
import androidx.compose.ui.focus.FocusRequester
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
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import com.strmr.ai.viewmodel.HomeViewModel
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TvShowRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var movieRepository: MovieRepository
    
    @Inject
    lateinit var tvShowRepository: TvShowRepository
    
    @OptIn(ExperimentalTvMaterial3Api::class)
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
                    MainScreen()
                }
            }
        }
    }
    
    private fun clearNullLogos() {
        // Use a coroutine to clear null logos asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                movieRepository.clearNullLogos()
                tvShowRepository.clearNullLogos()
                
                Log.d("MainActivity", "✅ Cleared null logos for retry")
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error clearing null logos", e)
            }
        }
    }
}

@Composable
fun MainScreen() {
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
                    PremiumizeSettingsPage(
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
                            cachedEpisode = episode
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
                            onTrailer = { videoUrl, title ->
                                val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
                                val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                                navController.navigate("video_player/$encodedUrl/$encodedTitle")
                            },
                            cachedSeason = season,
                            cachedEpisode = episode
                        )
                    }
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
                        onBack = { navController.popBackStack() }
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