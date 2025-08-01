package com.strmr.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.SettingsViewModel
import com.strmr.ai.viewmodel.UpdateViewModel
import java.util.*

@Composable
fun SettingsPage(
    onNavigateToTraktSettings: () -> Unit,
    onNavigateToPremiumizeSettings: () -> Unit,
    onNavigateToRealDebridSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val traktAuthState by viewModel.traktAuthState.collectAsState()
    val traktUserState by viewModel.traktUserState.collectAsState()

    // Settings state - these would be hooked up to actual preferences
    var syncOnLaunch by remember { mutableStateOf(true) }
    var syncAfterPlayback by remember { mutableStateOf(false) }
    var scrollStyle by remember { mutableStateOf("Middle") }
    var autoPlay by remember { mutableStateOf(true) }
    var nextEpisodeTime by remember { mutableStateOf("5") }

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
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Left Panel - Settings Navigation
            Column(
                modifier =
                    Modifier
                        .width(StrmrConstants.Dimensions.Components.SETTINGS_PANEL_WIDTH)
                        .fillMaxHeight()
                        .background(
                            StrmrConstants.Colors.SURFACE_DARK.copy(alpha = 0.8f),
                            StrmrConstants.Shapes.SETTINGS_PANEL_SHAPE,
                        )
                        .padding(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE)
                        .verticalScroll(rememberScrollState()),
            ) {
                // Header
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = StrmrConstants.Colors.TEXT_PRIMARY,
                    modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SECTION),
                )

                // Navigation Sections
                SettingsNavSection(
                    title = "Accounts",
                    items =
                        listOf(
                            SettingsNavItem("Trakt", Icons.Default.AccountCircle, isConnected = traktAuthState.isAuthorized),
                            SettingsNavItem("Premiumize", Icons.Default.Cloud, isConnected = false),
                            SettingsNavItem("RealDebrid", Icons.Default.Link, isConnected = false),
                        ),
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                SettingsNavSection(
                    title = "User Interface",
                    items =
                        listOf(
                            SettingsNavItem("Scroll Style", Icons.Default.TouchApp, showArrow = false),
                        ),
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                SettingsNavSection(
                    title = "Content",
                    items =
                        listOf(
                            SettingsNavItem("Home", Icons.Default.Home),
                            SettingsNavItem("Movies", Icons.Default.Movie),
                            SettingsNavItem("TV Shows", Icons.Default.Tv),
                            SettingsNavItem("Debrid Cloud", Icons.Default.CloudQueue),
                        ),
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                SettingsNavSection(
                    title = "Playback",
                    items =
                        listOf(
                            SettingsNavItem("Auto Play", Icons.Default.PlayArrow, showArrow = false),
                        ),
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                SettingsNavSection(
                    title = "System",
                    items =
                        listOf(
                            SettingsNavItem("Updates", Icons.Default.SystemUpdate),
                        ),
                )
            }

            // Right Panel - Settings Content
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(StrmrConstants.Dimensions.SPACING_SECTION)
                        .verticalScroll(rememberScrollState()),
            ) {
                // Account Settings Section
                SettingsContentSection(
                    title = "Account Settings",
                    subtitle = "Manage your streaming and tracking accounts",
                ) {
                    // Trakt Account
                    ModernSettingsCard(
                        title = "Trakt",
                        subtitle =
                            if (traktAuthState.isAuthorized) {
                                "Connected as ${traktUserState.profile?.username ?: "User"}"
                            } else {
                                "Connect to sync your watch history and ratings"
                            },
                        icon = Icons.Default.AccountCircle,
                        isConnected = traktAuthState.isAuthorized,
                        onClick = onNavigateToTraktSettings,
                    ) {
                        if (traktAuthState.isAuthorized) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = StrmrConstants.Dimensions.SPACING_STANDARD),
                            ) {
                                SettingsToggleRow(
                                    label = "Sync on app launch",
                                    subtitle = "Automatically sync when opening the app",
                                    checked = syncOnLaunch,
                                    onCheckedChange = { syncOnLaunch = it },
                                )

                                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                                SettingsToggleRow(
                                    label = "Sync after playback",
                                    subtitle = "Update watch status after finishing content",
                                    checked = syncAfterPlayback,
                                    onCheckedChange = { syncAfterPlayback = it },
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

                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                    // Premiumize Account
                    ModernSettingsCard(
                        title = "Premiumize",
                        subtitle = "Connect to access your cloud storage",
                        icon = Icons.Default.Cloud,
                        isConnected = false,
                        onClick = onNavigateToPremiumizeSettings,
                    )

                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                    // RealDebrid Account
                    ModernSettingsCard(
                        title = "RealDebrid",
                        subtitle = "Connect to access your cloud storage",
                        icon = Icons.Default.Link,
                        isConnected = false,
                        onClick = onNavigateToRealDebridSettings,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // User Interface Settings
                SettingsContentSection(
                    title = "User Interface",
                    subtitle = "Customize your viewing experience",
                ) {
                    ModernSettingsCard(
                        title = "Scroll Style",
                        subtitle = "Choose how content scrolls",
                        icon = Icons.Default.TouchApp,
                        showArrow = false,
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
                                onOptionSelected = { scrollStyle = it },
                                descriptions =
                                    mapOf(
                                        "Middle" to "Center content on screen",
                                        "Left" to "Align content to the left",
                                    ),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Playback Settings
                SettingsContentSection(
                    title = "Playback Settings",
                    subtitle = "Control how content plays",
                ) {
                    ModernSettingsCard(
                        title = "Auto Play",
                        subtitle = "Automatically play next episode",
                        icon = Icons.Default.PlayArrow,
                        showArrow = false,
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                        ) {
                            SettingsToggleRow(
                                label = "Enable Auto Play",
                                subtitle = "Automatically play the next episode",
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it },
                            )

                            if (autoPlay) {
                                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))
                                SettingsRadioGroup(
                                    title = "Next Episode Countdown",
                                    options = listOf("3", "5", "10", "15"),
                                    selectedOption = nextEpisodeTime,
                                    onOptionSelected = { nextEpisodeTime = it },
                                    descriptions =
                                        mapOf(
                                            "3" to "3 seconds",
                                            "5" to "5 seconds",
                                            "10" to "10 seconds",
                                            "15" to "15 seconds",
                                        ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // System Settings
                SystemSettingsSection()
            }
        }
    }
}

@Composable
fun SystemSettingsSection() {
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    SettingsContentSection(
        title = "System",
        subtitle = "App updates and system settings",
    ) {
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
}

@Composable
fun SettingsNavSection(
    title: String,
    items: List<SettingsNavItem>,
) {
    Text(
        text = title,
        fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
        fontWeight = FontWeight.SemiBold,
        color = StrmrConstants.Colors.TEXT_SECONDARY,
        modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_MEDIUM),
    )

    items.forEach { item ->
        SettingsNavItemRow(item)
        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
    }
}

@Composable
fun SettingsNavItemRow(item: SettingsNavItem) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                .background(StrmrConstants.Colors.CONTAINER_DARK)
                .padding(StrmrConstants.Dimensions.SPACING_MEDIUM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.size(StrmrConstants.Dimensions.SPACING_LARGE),
        )

        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))

        Text(
            text = item.title,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )

        if (item.isConnected != null) {
            Box(
                modifier =
                    Modifier
                        .size(StrmrConstants.Dimensions.SPACING_SMALL)
                        .background(
                            color = if (item.isConnected) StrmrConstants.Colors.PRIMARY_BLUE else Color.Gray,
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
            )
            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
        }

        if (item.showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StrmrConstants.Colors.TEXT_TERTIARY,
                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
            )
        }
    }
}

@Composable
fun SettingsContentSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.TEXT_PRIMARY,
            modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_TINY),
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = StrmrConstants.Colors.TEXT_SECONDARY,
            modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_LARGE),
        )

        content()
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
    content: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = StrmrConstants.Colors.SURFACE_DARK,
            ),
        shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
        elevation = CardDefaults.cardElevation(defaultElevation = StrmrConstants.Dimensions.Elevation.STANDARD),
    ) {
        Column(
            modifier = Modifier.padding(StrmrConstants.Dimensions.SPACING_LARGE),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (onClick != null) {
                                Modifier.clickable { onClick() }
                            } else {
                                Modifier
                            },
                        ),
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

                if (isConnected != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(StrmrConstants.Dimensions.SPACING_MEDIUM)
                                .background(
                                    color = if (isConnected) StrmrConstants.Colors.PRIMARY_BLUE else Color.Gray,
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White,
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
fun SettingsRadioGroup(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    descriptions: Map<String, String> = emptyMap(),
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
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
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
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
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

data class SettingsNavItem(
    val title: String,
    val icon: ImageVector,
    val isConnected: Boolean? = null,
    val showArrow: Boolean = true,
)
