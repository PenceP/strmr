package com.strmr.ai.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.SettingsViewModel
import com.strmr.ai.viewmodel.UpdateViewModel

// Settings categories
enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    TRAKT("Trakt", Icons.Default.AccountCircle),
    PREMIUMIZE("Premiumize", Icons.Default.Cloud),
    REALDEBRID("RealDebrid", Icons.Default.Link),
    USER_INTERFACE("User Interface", Icons.Default.TouchApp),
    PLAYBACK("Playback", Icons.Default.PlayArrow),
    SYSTEM("System", Icons.Default.SystemUpdate)
}

@Composable
fun SettingsPage(
    onNavigateToTraktSettings: () -> Unit,
    onNavigateToPremiumizeSettings: () -> Unit,
    onNavigateToRealDebridSettings: () -> Unit,
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
                "🎯 Content focused from nav bar, setting focus level to 2"
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
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
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
                traktAuthState = traktAuthState
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
                onNavigateToTraktSettings = onNavigateToTraktSettings,
                onNavigateToPremiumizeSettings = onNavigateToPremiumizeSettings,
                onNavigateToRealDebridSettings = onNavigateToRealDebridSettings
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
    modifier: Modifier = Modifier
) {
    androidx.compose.runtime.LaunchedEffect(focusLevel) {
        if (focusLevel == 2) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .focusRequester(focusRequester)
            .focusable()
            .background(
                StrmrConstants.Colors.SURFACE_DARK.copy(alpha = 0.8f)
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
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SettingsCategory.values().forEach { category ->
            CategoryItem(
                category = category,
                isSelected = category == selectedCategory,
                isLeftPanelFocused = focusLevel == 2,
                isConnected = when (category) {
                    SettingsCategory.TRAKT -> traktAuthState.isAuthorized
                    // ... other cases
                    else -> null
                },
                onClick = { onCategorySelected(category) }
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
    onClick: () -> Unit
) {
    // Debug logging for focus states
    android.util.Log.d(
        "CategoryItem",
        "🎯 ${category.displayName} - isSelected: $isSelected, isLeftPanelFocused: $isLeftPanelFocused"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
            .background(
                when {
                    isSelected && isLeftPanelFocused -> {
                        android.util.Log.d(
                            "CategoryItem",
                            "🟦 ${category.displayName} - Blue background (left panel focused - level 2)"
                        )
                        StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
                    }

                    isSelected && !isLeftPanelFocused -> {
                        android.util.Log.d(
                            "CategoryItem",
                            "🔵 ${category.displayName} - Gray background (nav bar focused level 1 or right panel focused level 3)"
                        )
                        Color.Gray.copy(alpha = 0.5f)
                    }

                    else -> StrmrConstants.Colors.CONTAINER_DARK.copy(alpha = 0.5f)
                }
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = when {
                isSelected && isLeftPanelFocused -> StrmrConstants.Colors.PRIMARY_BLUE
                isSelected && !isLeftPanelFocused -> Color.Gray
                else -> StrmrConstants.Colors.TEXT_PRIMARY
            },
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.displayName,
            color = when {
                isSelected && isLeftPanelFocused -> StrmrConstants.Colors.PRIMARY_BLUE
                isSelected && !isLeftPanelFocused -> Color.Gray
                else -> StrmrConstants.Colors.TEXT_PRIMARY
            },
            fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        isConnected?.let { connected ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (connected)
                            StrmrConstants.Colors.SUCCESS_GREEN
                        else
                            Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
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
    onNavigateToTraktSettings: () -> Unit,
    onNavigateToPremiumizeSettings: () -> Unit,
    onNavigateToRealDebridSettings: () -> Unit
) {
    Column(
        modifier = Modifier
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
            .verticalScroll(rememberScrollState())
    ) {
        // Add category title at the top of right panel
        Text(
            text = selectedCategory.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        when (selectedCategory) {
            SettingsCategory.TRAKT -> TraktSettingsContent(
                traktAuthState, traktUserState, syncOnLaunch, onSyncOnLaunchChanged,
                syncAfterPlayback, onSyncAfterPlaybackChanged, onNavigateToTraktSettings,
                isRightPanelFocused = focusLevel == 3
            )
            SettingsCategory.PREMIUMIZE -> PremiumizeSettingsContent(
                onNavigateToPremiumizeSettings,
                isRightPanelFocused = focusLevel == 3
            )

            SettingsCategory.REALDEBRID -> RealDebridSettingsContent(
                onNavigateToRealDebridSettings,
                isRightPanelFocused = focusLevel == 3
            )

            SettingsCategory.USER_INTERFACE -> UserInterfaceSettingsContent(
                scrollStyle, onScrollStyleChanged,
                isRightPanelFocused = focusLevel == 3
            )

            SettingsCategory.PLAYBACK -> PlaybackSettingsContent(
                autoPlay, onAutoPlayChanged, nextEpisodeTime, onNextEpisodeTimeChanged,
                isRightPanelFocused = focusLevel == 3
            )

            SettingsCategory.SYSTEM -> SystemSettingsContent(
                isRightPanelFocused = focusLevel == 3
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
    onNavigateToTraktSettings: () -> Unit,
    isRightPanelFocused: Boolean,
) {
    ModernSettingsCard(
        title = "Trakt",
        subtitle = if (traktAuthState.isAuthorized) {
            "Connected as ${traktUserState.profile?.username ?: "User"}"
        } else {
            "Connect to sync your watch history and ratings"
        },
        icon = Icons.Default.AccountCircle,
        isConnected = traktAuthState.isAuthorized,
        onClick = onNavigateToTraktSettings,
        isRightPanelFocused = isRightPanelFocused,
    ) {
        if (traktAuthState.isAuthorized) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
            ) {
                SettingsToggleRow(
                    label = "Sync on app launch",
                    subtitle = "Automatically sync when opening the app",
                    checked = syncOnLaunch,
                    onCheckedChange = onSyncOnLaunchChanged,
                    isRightPanelFocused = isRightPanelFocused,
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                SettingsToggleRow(
                    label = "Sync after playback",
                    subtitle = "Update watch status after finishing content",
                    checked = syncAfterPlayback,
                    onCheckedChange = onSyncAfterPlaybackChanged,
                    isRightPanelFocused = isRightPanelFocused,
                )

                if (traktUserState.stats != null) {
                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                    StatsDisplay(
                        movies = traktUserState.stats?.movies?.watched ?: 0,
                        shows = traktUserState.stats?.shows?.watched ?: 0,
                        lastSync = "2 hours ago", // This would be actual timestamp
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumizeSettingsContent(
    onNavigateToPremiumizeSettings: () -> Unit,
    isRightPanelFocused: Boolean,
) {
    ModernSettingsCard(
        title = "Premiumize",
        subtitle = "Connect to access your cloud storage",
        icon = Icons.Default.Cloud,
        onClick = onNavigateToPremiumizeSettings,
        isRightPanelFocused = isRightPanelFocused,
    )
}

@Composable
fun RealDebridSettingsContent(
    onNavigateToRealDebridSettings: () -> Unit,
    isRightPanelFocused: Boolean,
) {
    ModernSettingsCard(
        title = "RealDebrid",
        subtitle = "Connect to access your cloud storage",
        icon = Icons.Default.Link,
        onClick = onNavigateToRealDebridSettings,
        isRightPanelFocused = isRightPanelFocused,
    )
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
            modifier = Modifier
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
    ModernSettingsCard(
        title = "Auto Play",
        subtitle = "Automatically play next episode",
        icon = Icons.Default.PlayArrow,
        showArrow = false,
        isRightPanelFocused = isRightPanelFocused,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            SettingsToggleRow(
                label = "Enable Auto Play",
                subtitle = "Automatically play the next episode",
                checked = autoPlay,
                onCheckedChange = onAutoPlayChanged,
                isRightPanelFocused = isRightPanelFocused,
            )

            if (autoPlay) {
                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                SettingsRadioGroup(
                    title = "Next Episode Countdown",
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

@Composable
fun SystemSettingsContent(
    isRightPanelFocused: Boolean,
) {
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
            modifier = Modifier
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
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .focusTarget(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused || isRightPanelFocused)
                StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.3f)
            else
                StrmrConstants.Colors.SURFACE_DARK
        ),
        shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFocused || isRightPanelFocused) {
                StrmrConstants.Dimensions.Elevation.STANDARD * 2
            } else {
                StrmrConstants.Dimensions.Elevation.STANDARD
            }
        ),
    ) {
        Column(
            modifier = Modifier
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
                        modifier = Modifier
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
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .background(
                if (isFocused || isRightPanelFocused)
                    StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.2f)
                else
                    Color.Transparent,
                StrmrConstants.Shapes.CORNER_RADIUS_STANDARD
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
                color = if (isFocused || isRightPanelFocused)
                    StrmrConstants.Colors.PRIMARY_BLUE
                else
                    Color.White,
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = StrmrConstants.Colors.TEXT_PRIMARY,
                checkedTrackColor = StrmrConstants.Colors.PRIMARY_BLUE,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = StrmrConstants.Colors.BORDER_DARK,
            ),
        )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusable()
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    }
                    .background(
                        if (isFocused || isRightPanelFocused)
                            StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = 0.2f)
                        else
                            Color.Transparent,
                        StrmrConstants.Shapes.CORNER_RADIUS_STANDARD
                    )
                    .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                    .clickable { onOptionSelected(option) }
                    .padding(StrmrConstants.Dimensions.SPACING_MEDIUM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    colors = RadioButtonDefaults.colors(
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
                        color = if (isFocused || isRightPanelFocused)
                            StrmrConstants.Colors.PRIMARY_BLUE
                        else
                            StrmrConstants.Colors.TEXT_PRIMARY,
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
