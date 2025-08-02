package com.strmr.ai.ui.screens
// EXTRA MATERIAL3 IMPORTS

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.ui.components.ModernErrorDialog
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.SettingsViewModel
import com.strmr.ai.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch

// Settings categories
enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    TRAKT("Trakt", Icons.Default.AccountCircle),
    PREMIUMIZE("Premiumize", Icons.Default.Cloud),
    REALDEBRID("RealDebrid", Icons.Default.Link),
    USER_INTERFACE("User Interface", Icons.Default.TouchApp),
    PLAYBACK("Playback", Icons.Default.PlayArrow),
    SYSTEM("System", Icons.Default.SystemUpdate),
}

@Composable
fun SettingsPage(
    onLeftBoundary: () -> Unit,
    modifier: Modifier = Modifier,
    onContentFocusChanged: (Boolean) -> Unit = {},
    isContentFocused: Boolean = false,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val traktAuthState by viewModel.traktAuthState.collectAsState()
    val traktUserState by viewModel.traktUserState.collectAsState()

    var selectedCategory by remember { mutableStateOf(SettingsCategory.TRAKT) }
    var focusLevel by remember { mutableStateOf(1) } // Start at level 1 (nav bar focused) when entering settings

    val rightPanelFocusRequester = remember { FocusRequester() }
    val leftPanelFocusRequester = remember { FocusRequester() }

    // Handle when content is focused from navigation bar
    LaunchedEffect(isContentFocused) {
        if (isContentFocused && focusLevel == 1) {
            android.util.Log.d(
                "SettingsPage",
                "🎯 Content focused from nav bar, setting focus level to 2",
            )
            focusLevel = 2
        }
    }

    // Initialize focus when page loads
    LaunchedEffect(Unit) {
        // Start with navigation bar focused, will transition to content when nav bar sends right press
        android.util.Log.d("SettingsPage", "🎯 Settings page initialized, starting at focus level 1")
        onContentFocusChanged(false) // Navigation bar should be red initially
    }

    // Notify MainActivity when focus level changes
    LaunchedEffect(focusLevel) {
        android.util.Log.d("SettingsPage", "🎯 Focus level changed to: $focusLevel")
        when (focusLevel) {
            1 -> {
                // Navigation bar focused - settings icon should be red
                onContentFocusChanged(false)
            }

            2, 3 -> {
                // Content focused - settings icon should be white
                onContentFocusChanged(true)
            }
        }
    }

    // When focus level changes and right panel should be focused
    LaunchedEffect(focusLevel) {
        if (focusLevel == 3) {
            kotlinx.coroutines.delay(50) // Delay to ensure UI is ready
            rightPanelFocusRequester.requestFocus()
        } else if (focusLevel == 2) {
            kotlinx.coroutines.delay(50)
            leftPanelFocusRequester.requestFocus()
        }
    }

    val navBarWidth = StrmrConstants.Dimensions.Components.NAV_BAR_WIDTH

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                StrmrConstants.Colors.BACKGROUND_DARKER,
                                StrmrConstants.Colors.SURFACE_DARK,
                                StrmrConstants.Colors.BACKGROUND_DARK,
                            ),
                    ),
                )
                .padding(start = navBarWidth),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            LeftSettingsPanel(
                selectedCategory = selectedCategory,
                onCategorySelected = { category ->
                    selectedCategory = category
                },
                focusLevel = focusLevel,
                focusRequester = leftPanelFocusRequester,
                onRightPressed = {
                    android.util.Log.d("SettingsPage", "🎯 Left panel right pressed, moving to focus level 3")
                    focusLevel = 3
                },
                onLeftBoundary = {
                    android.util.Log.d("SettingsPage", "🎯 Left panel left pressed, moving to focus level 1 (nav bar)")
                    focusLevel = 1
                    onLeftBoundary()
                },
                traktAuthState = traktAuthState,
            )

            RightSettingsPanel(
                selectedCategory = selectedCategory,
                focusRequester = rightPanelFocusRequester,
                onLeftPressed = {
                    android.util.Log.d("SettingsPage", "🎯 Right panel left pressed, moving to focus level 2")
                    focusLevel = 2
                },
                focusLevel = focusLevel,
                // Pass all the state and callbacks
                traktAuthState = traktAuthState,
                traktUserState = traktUserState,
                syncOnLaunch = true, // Example state
                onSyncOnLaunchChanged = {},
                syncAfterPlayback = false, // Example state
                onSyncAfterPlaybackChanged = {},
                scrollStyle = "Middle", // Example state
                onScrollStyleChanged = {},
                autoPlay = true, // Example state
                onAutoPlayChanged = {},
                nextEpisodeTime = "5", // Example state
                onNextEpisodeTimeChanged = {},
            )
        }
    }
}

@Composable
fun LeftSettingsPanel(
    selectedCategory: SettingsCategory,
    onCategorySelected: (SettingsCategory) -> Unit,
    focusLevel: Int,
    focusRequester: FocusRequester,
    onRightPressed: () -> Unit,
    onLeftBoundary: () -> Unit,
    traktAuthState: com.strmr.ai.viewmodel.TraktAuthState,
    modifier: Modifier = Modifier,
) {
    androidx.compose.runtime.LaunchedEffect(focusLevel) {
        if (focusLevel == 2) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier =
            modifier
                .width(320.dp)
                .fillMaxHeight()
                .focusRequester(focusRequester)
                .focusable()
                .background(
                    StrmrConstants.Colors.SURFACE_DARK.copy(alpha = 0.8f),
                )
                .padding(24.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionRight -> {
                                onRightPressed()
                                return@onKeyEvent true
                            }

                            Key.DirectionDown -> {
                                val currentIndex = SettingsCategory.values().indexOf(selectedCategory)
                                val nextIndex =
                                    (currentIndex + 1).coerceAtMost(SettingsCategory.values().lastIndex)
                                onCategorySelected(SettingsCategory.values()[nextIndex])
                                return@onKeyEvent true
                            }

                            Key.DirectionUp -> {
                                val currentIndex = SettingsCategory.values().indexOf(selectedCategory)
                                val prevIndex = (currentIndex - 1).coerceAtLeast(0)
                                onCategorySelected(SettingsCategory.values()[prevIndex])
                                return@onKeyEvent true
                            }

                            Key.DirectionLeft -> {
                                onLeftBoundary()
                                return@onKeyEvent true
                            }
                        }
                    }
                    false
                }
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        SettingsCategory.values().forEach { category ->
            CategoryItem(
                category = category,
                isSelected = category == selectedCategory,
                isLeftPanelFocused = focusLevel == 2,
                isConnected =
                    when (category) {
                        SettingsCategory.TRAKT -> traktAuthState.isAuthorized
                        // ... other cases
                        else -> null
                    },
                onClick = { onCategorySelected(category) },
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun CategoryItem(
    category: SettingsCategory,
    isSelected: Boolean,
    isLeftPanelFocused: Boolean,
    isConnected: Boolean? = null,
    onClick: () -> Unit,
) {
    // Debug logging for focus states
    android.util.Log.d(
        "CategoryItem",
        "🎯 ${category.displayName} - isSelected: $isSelected, isLeftPanelFocused: $isLeftPanelFocused",
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                .background(
                    when {
                        isSelected && isLeftPanelFocused -> {
                            android.util.Log.d(
                                "CategoryItem",
                                "🟦 ${category.displayName} - Blue background (left panel focused - level 2)",
                            )
                            StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
                        }

                        isSelected && !isLeftPanelFocused -> {
                            android.util.Log.d(
                                "CategoryItem",
                                "🔵 ${category.displayName} - Gray background (nav bar focused level 1 or right panel focused level 3)",
                            )
                            Color.Gray.copy(alpha = 0.5f)
                        }

                        else -> StrmrConstants.Colors.CONTAINER_DARK.copy(alpha = 0.5f)
                    },
                )
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint =
                when {
                    isSelected && isLeftPanelFocused -> StrmrConstants.Colors.PRIMARY_BLUE
                    isSelected && !isLeftPanelFocused -> Color.Gray
                    else -> StrmrConstants.Colors.TEXT_PRIMARY
                },
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.displayName,
            color =
                when {
                    isSelected && isLeftPanelFocused -> StrmrConstants.Colors.PRIMARY_BLUE
                    isSelected && !isLeftPanelFocused -> Color.Gray
                    else -> StrmrConstants.Colors.TEXT_PRIMARY
                },
            fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        isConnected?.let { connected ->
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color =
                                if (connected) {
                                    StrmrConstants.Colors.SUCCESS_GREEN
                                } else {
                                    Color.Gray
                                },
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
            )
        }
    }
}

@Composable
fun RightSettingsPanel(
    selectedCategory: SettingsCategory,
    focusRequester: FocusRequester,
    onLeftPressed: () -> Unit,
    focusLevel: Int,
    // All the state parameters
    traktAuthState: com.strmr.ai.viewmodel.TraktAuthState,
    traktUserState: com.strmr.ai.viewmodel.TraktUserState,
    syncOnLaunch: Boolean,
    onSyncOnLaunchChanged: (Boolean) -> Unit,
    syncAfterPlayback: Boolean,
    onSyncAfterPlaybackChanged: (Boolean) -> Unit,
    scrollStyle: String,
    onScrollStyleChanged: (String) -> Unit,
    autoPlay: Boolean,
    onAutoPlayChanged: (Boolean) -> Unit,
    nextEpisodeTime: String,
    onNextEpisodeTimeChanged: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .padding(32.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.DirectionLeft) {
                        onLeftPressed()
                        return@onKeyEvent true
                    }
                    false
                }
                .verticalScroll(rememberScrollState()),
    ) {
        // Add category title at the top of right panel
        Text(
            text = selectedCategory.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        when (selectedCategory) {
            SettingsCategory.TRAKT ->
                TraktSettingsContent(
                    traktAuthState,
                    traktUserState,
                    syncOnLaunch,
                    onSyncOnLaunchChanged,
                    syncAfterPlayback,
                    onSyncAfterPlaybackChanged,
                    isRightPanelFocused = focusLevel == 3,
                )
            SettingsCategory.PREMIUMIZE ->
                PremiumizeSettingsContent(
                    isRightPanelFocused = focusLevel == 3,
                )

            SettingsCategory.REALDEBRID ->
                RealDebridSettingsContent(
                    isRightPanelFocused = focusLevel == 3,
                )

            SettingsCategory.USER_INTERFACE ->
                UserInterfaceSettingsContent(
                    scrollStyle,
                    onScrollStyleChanged,
                    isRightPanelFocused = focusLevel == 3,
                )

            SettingsCategory.PLAYBACK ->
                PlaybackSettingsContent(
                    autoPlay,
                    onAutoPlayChanged,
                    nextEpisodeTime,
                    onNextEpisodeTimeChanged,
                    isRightPanelFocused = focusLevel == 3,
                )

            SettingsCategory.SYSTEM ->
                SystemSettingsContent(
                    isRightPanelFocused = focusLevel == 3,
                )
        }
    }
}

@Composable
fun TraktSettingsContent(
    traktAuthState: com.strmr.ai.viewmodel.TraktAuthState,
    traktUserState: com.strmr.ai.viewmodel.TraktUserState,
    syncOnLaunch: Boolean,
    onSyncOnLaunchChanged: (Boolean) -> Unit,
    syncAfterPlayback: Boolean,
    onSyncAfterPlaybackChanged: (Boolean) -> Unit,
    isRightPanelFocused: Boolean,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val traktSettingsState by viewModel.traktSettingsState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_LARGE),
    ) {
        // Connection Status Card
        ModernSettingsCard(
            title = "Trakt Account",
            subtitle =
                if (traktAuthState.isAuthorized) {
                    "Connected as ${traktUserState.profile?.username ?: "User"}"
                } else {
                    "Not connected - Click to authorize"
                },
            icon = Icons.Default.AccountCircle,
            isConnected = traktAuthState.isAuthorized,
            onClick =
                if (!traktAuthState.isAuthorized) {
                    { viewModel.startTraktAuth() }
                } else {
                    null
                },
            isRightPanelFocused = isRightPanelFocused,
            showArrow = !traktAuthState.isAuthorized,
        ) {
            if (traktAuthState.isAuthorized) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
                ) {
                    traktUserState.profile?.let { profile ->
                        TraktInfoRow(
                            label = "Username",
                            value = profile.username,
                            icon = Icons.Default.Person,
                        )

                        if (profile.name != null) {
                            TraktInfoRow(
                                label = "Display Name",
                                value = profile.name,
                                icon = Icons.Default.Badge,
                            )
                        }

                        TraktInfoRow(
                            label = "Account Type",
                            value = if (profile.vip) "VIP Member" else "Free Account",
                            icon = if (profile.vip) Icons.Default.Star else Icons.Default.Person,
                            valueColor = if (profile.vip) StrmrConstants.Colors.PRIMARY_BLUE else StrmrConstants.Colors.TEXT_PRIMARY,
                        )
                    }

                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                    Button(
                        onClick = { viewModel.logout() },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = StrmrConstants.Colors.ERROR_RED,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                        Text(
                            text = "Disconnect Account",
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // Sync Settings - Individual Toggle Cards (only if authorized)
        if (traktAuthState.isAuthorized) {
            // Individual card for "Sync on app launch"
            ModernSettingsToggleCard(
                label = "Sync on app launch",
                subtitle = "Automatically sync when opening the app",
                checked = syncOnLaunch,
                onCheckedChange = onSyncOnLaunchChanged,
                isRightPanelFocused = isRightPanelFocused,
            )

            // Individual card for "Sync after playback"
            ModernSettingsToggleCard(
                label = "Sync after playback",
                subtitle = "Update watch status after finishing content",
                checked = syncAfterPlayback,
                onCheckedChange = onSyncAfterPlaybackChanged,
                isRightPanelFocused = isRightPanelFocused,
            )

            // Last sync timestamp card (if available)
            if (traktSettingsState.lastSyncTimestamp > 0) {
                val dateFormat = java.text.SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                val lastSync = dateFormat.format(java.util.Date(traktSettingsState.lastSyncTimestamp))

                ModernSettingsCard(
                    title = "Last Sync",
                    subtitle = lastSync,
                    icon = Icons.Default.Schedule,
                    showArrow = false,
                    isRightPanelFocused = isRightPanelFocused,
                )
            }
        }

        // Statistics Card (only if authorized and stats available)
        if (traktAuthState.isAuthorized && traktUserState.stats != null) {
            ModernSettingsCard(
                title = "Your Statistics",
                subtitle = "Your viewing habits and activity on Trakt",
                icon = Icons.Default.Analytics,
                showArrow = false,
                isRightPanelFocused = isRightPanelFocused,
            ) {
                traktUserState.stats?.let { stats ->
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
                    ) {
                        // Stats Grid
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        StrmrConstants.Colors.CONTAINER_DARK,
                                        StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
                                    )
                                    .padding(StrmrConstants.Dimensions.SPACING_LARGE),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            TraktStatItem(
                                label = "Movies",
                                value = "${stats.movies.watched}",
                                icon = Icons.Default.Movie,
                            )

                            TraktStatItem(
                                label = "Shows",
                                value = "${stats.shows.watched}",
                                icon = Icons.Default.Tv,
                            )

                            TraktStatItem(
                                label = "Episodes",
                                value = "${stats.episodes.watched}",
                                icon = Icons.Default.PlayCircle,
                            )
                        }

                        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                        // Additional Stats
                        TraktInfoRow(
                            label = "Total Watch Time",
                            value = "${(stats.movies.minutes + stats.episodes.minutes) / 60} hours",
                            icon = Icons.Default.Schedule,
                        )

                        TraktInfoRow(
                            label = "Ratings Given",
                            value = "${stats.ratings.total}",
                            icon = Icons.Default.Star,
                        )

                        if (stats.movies.collected > 0 || stats.shows.collected > 0) {
                            TraktInfoRow(
                                label = "Collection Items",
                                value = "${stats.movies.collected + stats.shows.collected}",
                                icon = Icons.Default.Collections,
                            )
                        }
                    }
                }
            }
        }
    }

    // Trakt Authorization Dialog
    if (traktAuthState.isAuthorizing) {
        ModernTraktAuthDialog(
            userCode = traktAuthState.userCode,
            timeLeft = traktAuthState.timeLeft,
            onDismiss = { /* do nothing, let dialog stay open unless user cancels */ },
            onCancel = { viewModel.cancelTraktAuth() },
        )
    }

    // Error Dialog
    (traktAuthState.error ?: traktUserState.error)?.let { error ->
        ModernErrorDialog(
            message = error,
            onDismiss = { viewModel.clearError() },
        )
    }
}

@Composable
fun PremiumizeSettingsContent(isRightPanelFocused: Boolean) {
    val viewModel: com.strmr.ai.viewmodel.PremiumizeSettingsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    // State
    var apiKey by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isConfigured by remember { mutableStateOf(viewModel.isConfigured()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_LARGE),
    ) {
        // Status Card
        ModernSettingsCard(
            title = "Connection Status",
            subtitle = if (isConfigured) "Configured and ready" else "Enter your API key to get started",
            icon = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Cloud,
            isConnected = isConfigured,
            showArrow = false,
            isRightPanelFocused = isRightPanelFocused,
        )

        // API Key Configuration Card
        ModernSettingsCard(
            title = "API Configuration",
            subtitle = "Find your API key at premiumize.me/account",
            icon = Icons.Default.Key,
            showArrow = false,
            isRightPanelFocused = isRightPanelFocused,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
            ) {
                // API Key Input
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        validationError = null
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your Premiumize API key") },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isApiKeyVisible) "Hide API key" else "Show API key",
                                tint = StrmrConstants.Colors.TEXT_SECONDARY,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StrmrConstants.Colors.PRIMARY_BLUE,
                            unfocusedBorderColor = StrmrConstants.Colors.BORDER_DARK,
                            focusedLabelColor = StrmrConstants.Colors.PRIMARY_BLUE,
                            unfocusedLabelColor = StrmrConstants.Colors.TEXT_SECONDARY,
                            focusedTextColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            unfocusedTextColor = StrmrConstants.Colors.TEXT_PRIMARY,
                            cursorColor = StrmrConstants.Colors.PRIMARY_BLUE,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Error message
                validationError?.let { error ->
                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                    Text(
                        text = error,
                        color = StrmrConstants.Colors.ERROR_RED,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                // Save Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isValidating = true
                            validationError = null

                            if (apiKey.isBlank()) {
                                validationError = "Please enter your API key"
                                isValidating = false
                                return@launch
                            }

                            val isValid = viewModel.validateAndSaveApiKey(apiKey)
                            if (isValid) {
                                isConfigured = true
                                validationError = null
                            } else {
                                validationError = "Invalid API key. Please check and try again."
                            }
                            isValidating = false
                        }
                    },
                    enabled = !isValidating && apiKey.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = StrmrConstants.Colors.PRIMARY_BLUE,
                            disabledContainerColor = StrmrConstants.Colors.BORDER_DARK,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                            strokeWidth = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2,
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                        Text("Validating...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                        Text("Save API Key")
                    }
                }

                // Clear Button (if configured)
                if (isConfigured) {
                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))

                    OutlinedButton(
                        onClick = {
                            viewModel.clearApiKey()
                            apiKey = ""
                            isConfigured = false
                            validationError = null
                        },
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = StrmrConstants.Colors.ERROR_RED,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                        Text("Clear API Key")
                    }
                }
            }
        }
    }
}

@Composable
fun RealDebridSettingsContent(isRightPanelFocused: Boolean) {
    ModernSettingsCard(
        title = "RealDebrid Integration",
        subtitle = "RealDebrid integration coming soon!",
        icon = Icons.Default.Link,
        showArrow = false,
        isRightPanelFocused = isRightPanelFocused,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
        ) {
            Text(
                text = "🚧 Under Development",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = StrmrConstants.Colors.TEXT_PRIMARY,
                modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SMALL),
            )

            Text(
                text = "RealDebrid support is currently being developed and will be available in a future update.",
                style = MaterialTheme.typography.bodyMedium,
                color = StrmrConstants.Colors.TEXT_SECONDARY,
            )
        }
    }
}

@Composable
fun UserInterfaceSettingsContent(
    scrollStyle: String,
    onScrollStyleChanged: (String) -> Unit,
    isRightPanelFocused: Boolean,
) {
    ModernSettingsCard(
        title = "Scroll Style",
        subtitle = "Choose how content scrolls",
        icon = Icons.Default.TouchApp,
        showArrow = false,
        isRightPanelFocused = isRightPanelFocused,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        ) {
            SettingsRadioGroup(
                title = "Content Alignment",
                options = listOf("Middle", "Left"),
                selectedOption = scrollStyle,
                onOptionSelected = { onScrollStyleChanged(it) },
                descriptions =
                    mapOf(
                        "Middle" to "Center content on screen",
                        "Left" to "Align content to the left",
                    ),
                isRightPanelFocused = isRightPanelFocused,
            )
        }
    }
}

@Composable
fun PlaybackSettingsContent(
    autoPlay: Boolean,
    onAutoPlayChanged: (Boolean) -> Unit,
    nextEpisodeTime: String,
    onNextEpisodeTimeChanged: (String) -> Unit,
    isRightPanelFocused: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StrmrConstants.Dimensions.SPACING_LARGE),
    ) {
        // Individual card for Auto Play toggle
        ModernSettingsToggleCard(
            label = "Enable Auto Play",
            subtitle = "Automatically play the next episode",
            checked = autoPlay,
            onCheckedChange = onAutoPlayChanged,
            isRightPanelFocused = isRightPanelFocused,
        )

        // Next Episode Countdown card (only if autoPlay is enabled)
        if (autoPlay) {
            ModernSettingsCard(
                title = "Next Episode Countdown",
                subtitle = "Choose countdown time before next episode",
                icon = Icons.Default.Schedule,
                showArrow = false,
                isRightPanelFocused = isRightPanelFocused,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                ) {
                    SettingsRadioGroup(
                        title = "Countdown Duration",
                        options = listOf("3", "5", "10", "15"),
                        selectedOption = nextEpisodeTime,
                        onOptionSelected = onNextEpisodeTimeChanged,
                        descriptions =
                            mapOf(
                                "3" to "3 seconds",
                                "5" to "5 seconds",
                                "10" to "10 seconds",
                                "15" to "15 seconds",
                            ),
                        isRightPanelFocused = isRightPanelFocused,
                    )
                }
            }
        }
    }
}

@Composable
fun SystemSettingsContent(isRightPanelFocused: Boolean) {
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    ModernSettingsCard(
        title = "App Updates",
        subtitle =
            updateState.updateInfo?.let { updateInfo ->
                if (updateInfo.hasUpdate) {
                    "Version ${updateInfo.latestVersion} available"
                } else {
                    "Version ${updateInfo.currentVersion} - Up to date"
                }
            } ?: "Version Unknown - Up to date",
        icon = Icons.Default.SystemUpdate,
        isConnected = updateState.updateInfo?.hasUpdate,
        showArrow = false,
        isRightPanelFocused = isRightPanelFocused,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        ) {
            when {
                updateState.isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                            strokeWidth = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2,
                            color = StrmrConstants.Colors.PRIMARY_BLUE,
                        )
                        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))
                        Text(
                            text = "Checking for updates...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrmrConstants.Colors.TEXT_SECONDARY,
                        )
                    }
                }

                updateState.isDownloading -> {
                    Column {
                        Text(
                            text = "Downloading update...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                        )
                        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                        LinearProgressIndicator(
                            progress = { updateState.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = StrmrConstants.Colors.PRIMARY_BLUE,
                            trackColor = StrmrConstants.Colors.BORDER_DARK,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${updateState.downloadProgress}% - ${updateState.downloadStatus ?: "Preparing..."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StrmrConstants.Colors.TEXT_SECONDARY,
                        )
                    }
                }

                updateState.updateInfo?.hasUpdate == true -> {
                    updateState.updateInfo?.let { updateInfo ->
                        Column {
                            Text(
                                text = "New version available: ${updateInfo.latestVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = StrmrConstants.Colors.PRIMARY_BLUE,
                            )

                            updateInfo.releaseNotes?.let { releaseNotes ->
                                if (releaseNotes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                                    Text(
                                        text = "What's new:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                                    )
                                    Text(
                                        text = releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                                        maxLines = 3,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                            Row {
                                Button(
                                    onClick = { updateViewModel.downloadAndInstallUpdate() },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = StrmrConstants.Colors.PRIMARY_BLUE,
                                        ),
                                ) {
                                    Text("Update Now")
                                }

                                Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))

                                TextButton(
                                    onClick = { updateViewModel.checkForUpdates() },
                                ) {
                                    Text(
                                        text = "Check Again",
                                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                                    )
                                }
                            }
                        }
                    }
                }

                updateState.error != null -> {
                    updateState.error?.let { error ->
                        Column {
                            Text(
                                text = "Error checking for updates",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = StrmrConstants.Colors.ERROR_RED,
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = StrmrConstants.Colors.TEXT_SECONDARY,
                            )

                            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                            TextButton(
                                onClick = {
                                    updateViewModel.dismissError()
                                    updateViewModel.checkForUpdates()
                                },
                            ) {
                                Text(
                                    text = "Retry",
                                    color = StrmrConstants.Colors.PRIMARY_BLUE,
                                )
                            }
                        }
                    }
                }

                else -> {
                    Row {
                        Text(
                            text = "✅ You're up to date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrmrConstants.Colors.SUCCESS_GREEN,
                            modifier = Modifier.weight(1f),
                        )

                        TextButton(
                            onClick = { updateViewModel.checkForUpdates() },
                        ) {
                            Text(
                                text = "Check Now",
                                color = StrmrConstants.Colors.TEXT_SECONDARY,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isConnected: Boolean? = null,
    showArrow: Boolean = true,
    onClick: (() -> Unit)? = null,
    isRightPanelFocused: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = StrmrConstants.Colors.SURFACE_DARK,
            ),
        shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = StrmrConstants.Dimensions.Elevation.STANDARD,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .clickable { onClick?.invoke() }
                    .padding(StrmrConstants.Dimensions.SPACING_LARGE),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StrmrConstants.Colors.TEXT_PRIMARY,
                    modifier = Modifier.size(StrmrConstants.Dimensions.Icons.STANDARD),
                )

                Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                    )

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                        modifier = Modifier.padding(top = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2),
                    )
                }

                isConnected?.let { connected ->
                    Box(
                        modifier =
                            Modifier
                                .size(StrmrConstants.Dimensions.SPACING_MEDIUM)
                                .background(
                                    color = if (connected) StrmrConstants.Colors.PRIMARY_BLUE else Color.Gray,
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                ),
                    )
                    Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))
                }

                if (showArrow && onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = StrmrConstants.Colors.TEXT_TERTIARY,
                        modifier = Modifier.size(StrmrConstants.Dimensions.SPACING_LARGE),
                    )
                }
            }

            content?.invoke()
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isRightPanelFocused: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusable()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }
                .background(
                    if (isFocused) {
                        StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    },
                    StrmrConstants.Shapes.CORNER_RADIUS_STANDARD,
                )
                .padding(12.dp)
                .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color =
                    if (isFocused) {
                        StrmrConstants.Colors.PRIMARY_BLUE
                    } else {
                        Color.White
                    },
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = StrmrConstants.Colors.TEXT_SECONDARY,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = StrmrConstants.Colors.TEXT_PRIMARY,
                    checkedTrackColor = StrmrConstants.Colors.PRIMARY_BLUE,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = StrmrConstants.Colors.BORDER_DARK,
                ),
        )
    }
}

@Composable
fun ModernSettingsToggleCard(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isRightPanelFocused: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .focusable()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isFocused) {
                        StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
                    } else {
                        StrmrConstants.Colors.SURFACE_DARK
                    },
            ),
        shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = StrmrConstants.Dimensions.Elevation.STANDARD,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(StrmrConstants.Dimensions.SPACING_LARGE)
                    .fillMaxWidth()
                    .clickable { onCheckedChange(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (isFocused) {
                            StrmrConstants.Colors.PRIMARY_BLUE
                        } else {
                            StrmrConstants.Colors.TEXT_PRIMARY
                        },
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StrmrConstants.Colors.TEXT_SECONDARY,
                    modifier = Modifier.padding(top = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = StrmrConstants.Colors.TEXT_PRIMARY,
                        checkedTrackColor = StrmrConstants.Colors.PRIMARY_BLUE,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = StrmrConstants.Colors.BORDER_DARK,
                    ),
            )
        }
    }
}

@Composable
fun SettingsRadioGroup(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    descriptions: Map<String, String> = emptyMap(),
    isRightPanelFocused: Boolean,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_MEDIUM),
        )

        options.forEach { option ->
            var isFocused by remember { mutableStateOf(false) }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusable()
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .background(
                            if (isFocused) {
                                StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            },
                            StrmrConstants.Shapes.CORNER_RADIUS_STANDARD,
                        )
                        .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                        .clickable { onOptionSelected(option) }
                        .padding(StrmrConstants.Dimensions.SPACING_MEDIUM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    colors =
                        RadioButtonDefaults.colors(
                            selectedColor = StrmrConstants.Colors.PRIMARY_BLUE,
                            unselectedColor = Color.Gray,
                        ),
                )

                Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))

                Column {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color =
                            if (isFocused) {
                                StrmrConstants.Colors.PRIMARY_BLUE
                            } else {
                                StrmrConstants.Colors.TEXT_PRIMARY
                            },
                    )

                    descriptions[option]?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = StrmrConstants.Colors.TEXT_SECONDARY,
                            modifier = Modifier.padding(top = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsDisplay(
    movies: Int,
    shows: Int,
    lastSync: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    StrmrConstants.Colors.CONTAINER_DARK,
                    StrmrConstants.Shapes.CORNER_RADIUS_STANDARD,
                )
                .padding(StrmrConstants.Dimensions.SPACING_STANDARD),
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SMALL),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatItem("Movies", movies.toString())
            StatItem("Shows", shows.toString())
        }

        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))

        Text(
            text = "Last sync: $lastSync",
            style = MaterialTheme.typography.bodySmall,
            color = StrmrConstants.Colors.TEXT_SECONDARY,
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.PRIMARY_BLUE,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = StrmrConstants.Colors.TEXT_SECONDARY,
        )
    }
}
