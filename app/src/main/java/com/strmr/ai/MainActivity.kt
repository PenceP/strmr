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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.AccountRepository
import com.strmr.ai.data.RetrofitInstance
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TraktApiService
import com.strmr.ai.data.TvShowRepository
import com.strmr.ai.data.database.StrmrDatabase
import com.strmr.ai.ui.navigation.NavigationBar
import com.strmr.ai.ui.screens.DebridCloudPage
import com.strmr.ai.ui.screens.HomePage
import com.strmr.ai.ui.screens.MoviesPage
import com.strmr.ai.ui.screens.PlaceholderPage
import com.strmr.ai.ui.screens.SettingsPage
import com.strmr.ai.ui.screens.TvShowsPage
import com.strmr.ai.ui.screens.TraktSettingsPage
import com.strmr.ai.ui.screens.PremiumizeSettingsPage
import com.strmr.ai.ui.screens.RealDebridSettingsPage
import com.strmr.ai.ui.theme.StrmrTheme
import com.strmr.ai.viewmodel.HomeViewModel
import com.strmr.ai.viewmodel.MoviesViewModel
import com.strmr.ai.viewmodel.TvShowsViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusDirection
import com.strmr.ai.data.HomeRepository
import androidx.compose.ui.zIndex
import com.strmr.ai.data.OmdbRepository
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.ui.screens.DetailsPage
import com.strmr.ai.ui.screens.MediaDetailsType
import android.util.Log

class MainActivity : ComponentActivity() {
    // Dependencies
    private lateinit var movieRepository: MovieRepository
    private lateinit var tvShowRepository: TvShowRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var tmdbApiService: TmdbApiService
    private lateinit var homeRepository: HomeRepository
    private lateinit var omdbRepository: OmdbRepository
    
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("FontLoading", "🚀 MainActivity onCreate started")
        
        // Initialize dependencies
        initializeDependencies()
        
        setContent {
            android.util.Log.d("FontLoading", "🎨 Setting content with StrmrTheme")
            StrmrTheme {
                android.util.Log.d("FontLoading", "✅ StrmrTheme applied, creating Surface")
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    MainScreen(
                        movieRepository = movieRepository,
                        tvShowRepository = tvShowRepository,
                        accountRepository = accountRepository,
                        tmdbApiService = tmdbApiService,
                        homeRepository = homeRepository,
                        omdbRepository = omdbRepository
                    )
                }
            }
        }
    }
    
    private fun initializeDependencies() {
        // Initialize API services
        val traktApiService = RetrofitInstance.trakt.create(TraktApiService::class.java)
        tmdbApiService = RetrofitInstance.tmdb.create(TmdbApiService::class.java)
        
        // Initialize database
        val database = (application as StrmrApplication).database
        
        // Initialize repositories
        homeRepository = HomeRepository(
            this, // context
            database.playbackDao(),
            database.traktUserProfileDao(),
            database.traktUserStatsDao()
        )
        movieRepository = MovieRepository(database.movieDao(), traktApiService, tmdbApiService)
        tvShowRepository = TvShowRepository(
            database.tvShowDao(),
            traktApiService,
            tmdbApiService,
            database.seasonDao(),
            database.episodeDao()
        )
        accountRepository = AccountRepository(database.accountDao(), traktApiService)
        omdbRepository = OmdbRepository(database.omdbRatingsDao(), RetrofitInstance.omdbApiService)
    }
}

@Composable
fun MainScreen(
    movieRepository: MovieRepository,
    tvShowRepository: TvShowRepository,
    accountRepository: AccountRepository,
    tmdbApiService: TmdbApiService,
    homeRepository: HomeRepository,
    omdbRepository: OmdbRepository
) {
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
                    PlaceholderPage("Search")
                }
                composable("home") {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                HomeViewModel(accountRepository, movieRepository, tvShowRepository, homeRepository)
                            }
                        }
                    )
                    HomePage(
                        viewModel = homeViewModel,
                        omdbRepository = omdbRepository,
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
                        movieRepository = movieRepository,
                        tmdbApiService = tmdbApiService,
                        omdbRepository = omdbRepository, // <-- added
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
                        tvShowRepository = tvShowRepository,
                        tmdbApiService = tmdbApiService,
                        omdbRepository = omdbRepository, // <-- added
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
                        accountRepository = accountRepository,
                        onNavigateToTraktSettings = { navController.navigate("trakt_settings") },
                        onNavigateToPremiumizeSettings = { navController.navigate("premiumize_settings") },
                        onNavigateToRealDebridSettings = { navController.navigate("realdebrid_settings") }
                    )
                }
                composable("trakt_settings") {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = viewModelFactory {
                            initializer {
                                HomeViewModel(accountRepository, movieRepository, tvShowRepository, homeRepository)
                            }
                        }
                    )
                    TraktSettingsPage(
                        accountRepository = accountRepository,
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
                    var movie by remember { mutableStateOf<MovieEntity?>(null) }
                    var show by remember { mutableStateOf<TvShowEntity?>(null) }
                    LaunchedEffect(mediaType, tmdbId) {
                        if (mediaType == "movie" && tmdbId != null) {
                            movie = movieRepository.getMovieByTmdbId(tmdbId)
                        } else if (mediaType == "tvshow" && tmdbId != null) {
                            show = tvShowRepository.getTvShowByTmdbId(tmdbId)
                        }
                    }
                    when (mediaType) {
                        "movie" -> DetailsPage(
                            mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                            omdbRepository = omdbRepository,
                            movieRepository = movieRepository,
                            onNavigateToSimilar = { mediaType, tmdbId ->
                                val route = "details/$mediaType/$tmdbId"
                                navController.navigate(route)
                            }
                        )
                        "tvshow" -> DetailsPage(
                            mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                            omdbRepository = omdbRepository,
                            tvShowRepository = tvShowRepository,
                            onNavigateToSimilar = { mediaType, tmdbId ->
                                val route = "details/$mediaType/$tmdbId"
                                navController.navigate(route)
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
                    var movie by remember { mutableStateOf<MovieEntity?>(null) }
                    var show by remember { mutableStateOf<TvShowEntity?>(null) }
                    LaunchedEffect(mediaType, tmdbId) {
                        if (mediaType == "movie" && tmdbId != null) {
                            movie = movieRepository.getMovieByTmdbId(tmdbId)
                        } else if (mediaType == "tvshow" && tmdbId != null) {
                            show = tvShowRepository.getTvShowByTmdbId(tmdbId)
                        }
                    }
                    when (mediaType) {
                        "movie" -> DetailsPage(
                            mediaDetails = movie?.let { MediaDetailsType.Movie(it) },
                            omdbRepository = omdbRepository,
                            movieRepository = movieRepository,
                            onNavigateToSimilar = { mediaType, tmdbId ->
                                val route = "details/$mediaType/$tmdbId"
                                navController.navigate(route)
                            }
                        )
                        "tvshow" -> DetailsPage(
                            mediaDetails = show?.let { MediaDetailsType.TvShow(it) },
                            omdbRepository = omdbRepository,
                            tvShowRepository = tvShowRepository,
                            cachedSeason = season,
                            cachedEpisode = episode
                        )
                    }
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