package com.strmr.ai.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.strmr.ai.ui.utils.safeRequestFocus

@Composable
fun NavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isContentFocused: Boolean = false,
    onRightPressed: (() -> Unit)? = null,
    onFocusReceived: (() -> Unit)? = null,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"
    var focusedIndex by remember { mutableStateOf(1) } // Default to Home (index 1)
    val navFocusRequester = remember { FocusRequester() }
    var initialLoad by remember { mutableStateOf(true) }
    var hasNavBarFocus by remember { mutableStateOf(false) }
    var forceRecomposition by remember { mutableStateOf(0) }

    // Auto-focus the NavigationBar on app start
    LaunchedEffect(Unit) {
        // Only auto-focus on app start if content is not focused
        if (!isContentFocused) {
            navFocusRequester.safeRequestFocus("NavigationBar-AppStart")
            hasNavBarFocus = true // Ensure navbar focus state is set on app start
        }
    }

    // Update navbar focus state when content focus changes
    LaunchedEffect(isContentFocused) {
        hasNavBarFocus = !isContentFocused
        android.util.Log.d("NavigationBar", "🎯 Content focus changed: isContentFocused=$isContentFocused, hasNavBarFocus=$hasNavBarFocus")
        
        // Don't auto-request focus if content should be focused (e.g., when returning from details)
        if (!isContentFocused && !hasNavBarFocus) {
            // Only request navbar focus if content is not focused and navbar doesn't have focus
            navFocusRequester.safeRequestFocus("NavigationBar-ContentFocusChange")
        }
        
        // Force recomposition when focus state changes
        forceRecomposition++
    }

    // Update focused index when route changes
    LaunchedEffect(currentRoute) {
        focusedIndex =
            when (currentRoute) {
                "search" -> 0
                "home" -> 1
                "movies" -> 2
                "tvshows" -> 3
                "debridcloud" -> 4
                "settings" -> 5
                else -> 1
            }
        // Only auto-focus when route changes if this is the initial load or a major navigation
        // Don't auto-focus for every route change to prevent conflicts with manual focus requests
        if (initialLoad) {
            navFocusRequester.safeRequestFocus("NavigationBar-RouteChange")
            initialLoad = false
        }
    }

    // Auto-navigate when focused index changes
    LaunchedEffect(focusedIndex) {
        val route =
            when (focusedIndex) {
                0 -> "search"
                1 -> "home"
                2 -> "movies"
                3 -> "tvshows"
                4 -> "debridcloud"
                5 -> "settings"
                else -> "home"
            }
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            // Add a small delay to ensure navigation completes before requesting focus
            kotlinx.coroutines.delay(100)
            navFocusRequester.safeRequestFocus("NavigationBar-FocusChange")
        }
    }

    Column(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(vertical = 22.dp)
                .focusRequester(navFocusRequester)
                .focusable()
                .onFocusChanged { focusState ->
                    hasNavBarFocus = focusState.hasFocus
                    if (focusState.hasFocus) {
                        android.util.Log.d(
                            "NavigationBar",
                            "🎯 NavigationBar gained focus, triggering onFocusReceived callback",
                        )
                        // Update focusedIndex to match current route when navbar gains focus
                        focusedIndex =
                            when (currentRoute) {
                                "search" -> 0
                                "home" -> 1
                                "movies" -> 2
                                "tvshows" -> 3
                                "debridcloud" -> 4
                                "settings" -> 5
                                else -> 1
                            }
                        // Force recomposition to update colors
                        forceRecomposition++
                        onFocusReceived?.invoke()
                    }
                }
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        when (event.nativeKeyEvent.keyCode) {
                            android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                if (focusedIndex > 0) {
                                    focusedIndex--
                                }
                                // Don't wrap to bottom - do nothing if already at top
                                true // Consume the event
                            }

                            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (focusedIndex < 5) {
                                    focusedIndex++
                                }
                                // Don't wrap to top - do nothing if already at bottom
                                true // Consume the event
                            }

                            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                                // Prevent left navigation
                                true
                            }

                            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                // Allow right navigation to main content and notify
                                android.util.Log.d(
                                    "NavigationBar",
                                    "🎯 Right arrow pressed, triggering onRightPressed callback",
                                )
                                onRightPressed?.invoke()
                                false
                            }

                            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                            android.view.KeyEvent.KEYCODE_ENTER,
                                -> {
                                // Navigate to the selected item
                                val route =
                                    when (focusedIndex) {
                                        0 -> "search"
                                        1 -> "home"
                                        2 -> "movies"
                                        3 -> "tvshows"
                                        4 -> "debridcloud"
                                        5 -> "settings"
                                        else -> "home"
                                    }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                true
                            }

                            else -> false
                        }
                    } else {
                        false
                    }
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        NavigationItem(
            icon = Icons.Filled.Search,
            label = "Search",
            route = "search",
            isSelected = currentRoute == "search",
            isFocused = focusedIndex == 0,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 0
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationItem(
            icon = Icons.Filled.Home,
            label = "Home",
            route = "home",
            isSelected = currentRoute == "home",
            isFocused = focusedIndex == 1,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 1
            },
        )

        Spacer(modifier = Modifier.height(22.dp))

        NavigationItem(
            icon = Icons.Filled.Theaters,
            label = "Movies",
            route = "movies",
            isSelected = currentRoute == "movies",
            isFocused = focusedIndex == 2,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 2
            },
        )

        Spacer(modifier = Modifier.height(22.dp))

        NavigationItem(
            icon = Icons.Filled.Tv,
            label = "TV Shows",
            route = "tvshows",
            isSelected = currentRoute == "tvshows",
            isFocused = focusedIndex == 3,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 3
            },
        )

        Spacer(modifier = Modifier.height(22.dp))

        NavigationItem(
            icon = Icons.Filled.Cloud,
            label = "Debrid Cloud",
            route = "debridcloud",
            isSelected = currentRoute == "debridcloud",
            isFocused = focusedIndex == 4,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 4
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            route = "settings",
            isSelected = currentRoute == "settings",
            isFocused = focusedIndex == 5,
            hasNavBarFocus = hasNavBarFocus,
            recompositionTrigger = forceRecomposition,
            onClick = {
                focusedIndex = 5
            },
        )
    }
}

@Composable
private fun NavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    route: String,
    isSelected: Boolean,
    isFocused: Boolean,
    hasNavBarFocus: Boolean,
    recompositionTrigger: Int = 0,
    onClick: () -> Unit,
) {
    // Debug logging
    android.util.Log.d(
        "NavigationItem",
        "🎯 $label - isSelected: $isSelected, isFocused: $isFocused, hasNavBarFocus: $hasNavBarFocus",
    )

    Box(
        modifier =
            Modifier
                .size(38.dp)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint =
                when {
                    // Show red when navbar has focus AND (this item is focused OR it's the selected route)
                    hasNavBarFocus && (isFocused || isSelected) -> {
                        android.util.Log.d(
                            "NavigationItem",
                            "🔴 Item $label is actively focused with navbar focus - should be RED",
                        )
                        Color(0xFFFF0000) // Explicit bright red
                    }

                    isSelected -> {
                        android.util.Log.d(
                            "NavigationItem",
                            "⚪ Item $label is current page - should be WHITE",
                        )
                        Color.White
                    }

                    else -> {
                        android.util.Log.d(
                            "NavigationItem",
                            "⚫ Item $label is inactive - should be GRAY",
                        )
                        Color.Gray
                    }
                },
            modifier = Modifier.size(24.dp),
        )
    }
}
