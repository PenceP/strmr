package com.strmr.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.ui.components.AuthStep
import com.strmr.ai.ui.components.ModernErrorDialog
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TraktSettingsPage(
    onBackPressed: () -> Unit,
    onTraktAuthorized: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val traktAuthState by viewModel.traktAuthState.collectAsState()
    val traktUserState by viewModel.traktUserState.collectAsState()
    val traktSettingsState by viewModel.traktSettingsState.collectAsState()

    // Navigation and scroll state
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var selectedNavSection by remember { mutableStateOf("connection") }

    // Define section positions (approximate, will be calculated in real usage)
    val sectionPositions = remember { mutableMapOf<String, Int>() }

    val prevIsAuthorized = remember { mutableStateOf(traktAuthState.isAuthorized) }
    LaunchedEffect(traktAuthState.isAuthorized) {
        if (!prevIsAuthorized.value && traktAuthState.isAuthorized) {
            onTraktAuthorized?.invoke()
        }
        prevIsAuthorized.value = traktAuthState.isAuthorized
    }

    // Update selected section based on scroll position
    LaunchedEffect(scrollState.value) {
        val currentScroll = scrollState.value
        var closestSection = "connection"
        var minDistance = Int.MAX_VALUE

        sectionPositions.forEach { (section, position) ->
            val distance = kotlin.math.abs(currentScroll - position)
            if (distance < minDistance) {
                minDistance = distance
                closestSection = section
            }
        }

        if (selectedNavSection != closestSection) {
            selectedNavSection = closestSection
        }
    }

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
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Left Panel - Navigation
            Column(
                modifier =
                    Modifier
                        .width(StrmrConstants.Dimensions.Components.SETTINGS_PANEL_WIDTH - StrmrConstants.Dimensions.SPACING_LARGE)
                        .fillMaxHeight()
                        .background(
                            StrmrConstants.Colors.SURFACE_DARK.copy(alpha = 0.8f),
                            StrmrConstants.Shapes.SETTINGS_PANEL_SHAPE,
                        )
                        .padding(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE),
            ) {
                // Back Button
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(StrmrConstants.Shapes.CORNER_RADIUS_STANDARD)
                            .clickable { onBackPressed() }
                            .background(StrmrConstants.Colors.CONTAINER_DARK)
                            .padding(StrmrConstants.Dimensions.SPACING_MEDIUM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = StrmrConstants.Colors.TEXT_PRIMARY,
                        modifier = Modifier.size(StrmrConstants.Dimensions.SPACING_LARGE),
                    )
                    Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))
                    Text(
                        text = "Back to Settings",
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                        fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SECTION))

                // Header
                Text(
                    text = "Trakt",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = StrmrConstants.Colors.TEXT_PRIMARY,
                    modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SMALL),
                )

                Text(
                    text = "Sync your watch history and ratings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StrmrConstants.Colors.TEXT_SECONDARY,
                    modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SECTION),
                )

                // Navigation Items
                TraktNavSection(
                    title = "Account",
                    items =
                        listOf(
                            TraktNavItem(
                                "Connection Status",
                                Icons.Default.AccountCircle,
                                sectionId = "connection",
                                isConnected = traktAuthState.isAuthorized,
                            ),
                            TraktNavItem(
                                "User Profile",
                                Icons.Default.Person,
                                sectionId = "profile",
                                isVisible = traktAuthState.isAuthorized,
                            ),
                            TraktNavItem(
                                "Statistics",
                                Icons.Default.Analytics,
                                sectionId = "statistics",
                                isVisible = traktAuthState.isAuthorized && traktUserState.stats != null,
                            ),
                        ),
                    selectedSection = selectedNavSection,
                    onSectionClick = { sectionId ->
                        selectedNavSection = sectionId
                        coroutineScope.launch {
                            sectionPositions[sectionId]?.let { position ->
                                scrollState.animateScrollTo(position)
                            }
                        }
                    },
                )

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                TraktNavSection(
                    title = "Sync Settings",
                    items =
                        listOf(
                            TraktNavItem("Sync Options", Icons.Default.Sync, sectionId = "sync", showArrow = false),
                            TraktNavItem(
                                "Last Sync",
                                Icons.Default.Schedule,
                                sectionId = "sync",
                                isVisible = traktSettingsState.lastSyncTimestamp > 0,
                                showArrow = false,
                            ),
                        ),
                    selectedSection = selectedNavSection,
                    onSectionClick = { sectionId ->
                        selectedNavSection = sectionId
                        coroutineScope.launch {
                            sectionPositions[sectionId]?.let { position ->
                                scrollState.animateScrollTo(position)
                            }
                        }
                    },
                )
            }

            // Right Panel - Content
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(StrmrConstants.Dimensions.SPACING_SECTION)
                        .verticalScroll(scrollState),
            ) {
                // Connection Status Section
                TraktContentSection(
                    title = "Connection Status",
                    subtitle = if (traktAuthState.isAuthorized) "Your Trakt account is connected" else "Connect your Trakt account to sync your data",
                    modifier =
                        Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["connection"] = coordinates.positionInParent().y.toInt()
                        },
                ) {
                    ModernTraktCard(
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
                    ) {
                        if (traktAuthState.isAuthorized) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
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
                }

                if (traktAuthState.isAuthorized && traktUserState.stats != null) {
                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SECTION))

                    // Statistics Section
                    TraktContentSection(
                        title = "Your Statistics",
                        subtitle = "Your viewing habits and activity on Trakt",
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                sectionPositions["statistics"] = coordinates.positionInParent().y.toInt()
                            },
                    ) {
                        ModernTraktCard(
                            title = "Viewing Statistics",
                            subtitle = "Track your movies, shows, and total watch time",
                            icon = Icons.Default.Analytics,
                            showArrow = false,
                        ) {
                            traktUserState.stats?.let { stats ->
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp),
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

                if (traktAuthState.isAuthorized) {
                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SECTION))

                    // Sync Settings Section
                    TraktContentSection(
                        title = "Sync Settings",
                        subtitle = "Control when and how your data syncs with Trakt",
                        modifier =
                            Modifier.onGloballyPositioned { coordinates ->
                                sectionPositions["sync"] = coordinates.positionInParent().y.toInt()
                            },
                    ) {
                        ModernTraktCard(
                            title = "Automatic Sync",
                            subtitle = "Configure when to sync your watch progress",
                            icon = Icons.Default.Sync,
                            showArrow = false,
                        ) {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                            ) {
                                TraktToggleRow(
                                    label = "Sync on app launch",
                                    subtitle = "Automatically sync when opening the app",
                                    checked = traktSettingsState.syncOnLaunch,
                                    onCheckedChange = { viewModel.toggleSyncOnLaunch() },
                                    icon = Icons.Default.Launch,
                                )

                                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                                TraktToggleRow(
                                    label = "Sync after playback",
                                    subtitle = "Update watch status after finishing content",
                                    checked = traktSettingsState.syncAfterPlayback,
                                    onCheckedChange = { viewModel.toggleSyncAfterPlayback() },
                                    icon = Icons.Default.PlayArrow,
                                )

                                if (traktSettingsState.lastSyncTimestamp > 0) {
                                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

                                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                    val lastSync = dateFormat.format(Date(traktSettingsState.lastSyncTimestamp))

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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = StrmrConstants.Colors.TEXT_SECONDARY,
                                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                                            )
                                            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                                            Text(
                                                text = "Last sync:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = StrmrConstants.Colors.TEXT_SECONDARY,
                                            )
                                        }

                                        Text(
                                            text = lastSync,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                                            modifier = Modifier.padding(top = StrmrConstants.Dimensions.SPACING_TINY),
                                        )
                                    }
                                }
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
}

@Composable
fun TraktNavSection(
    title: String,
    items: List<TraktNavItem>,
    selectedSection: String = "",
    onSectionClick: (String) -> Unit = {},
) {
    Text(
        text = title,
        fontSize = StrmrConstants.Typography.TEXT_SIZE_BODY,
        fontWeight = FontWeight.SemiBold,
        color = StrmrConstants.Colors.TEXT_SECONDARY,
        modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_MEDIUM),
    )

    items.filter { it.isVisible }.forEach { item ->
        TraktNavItemRow(
            item = item,
            isSelected = selectedSection == item.sectionId,
            onClick = { onSectionClick(item.sectionId) },
        )
        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
    }
}

@Composable
fun TraktNavItemRow(
    item: TraktNavItem,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isSelected) {
                        StrmrConstants.Colors.PRIMARY_BLUE.copy(
                            alpha = StrmrConstants.Colors.Alpha.SUBTLE,
                        )
                    } else {
                        StrmrConstants.Colors.CONTAINER_DARK
                    },
                )
                .clickable { onClick() }
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
fun TraktContentSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
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
fun ModernTraktCard(
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
fun TraktInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = StrmrConstants.Colors.TEXT_PRIMARY,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = StrmrConstants.Dimensions.SPACING_TINY * 1.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StrmrConstants.Colors.TEXT_SECONDARY,
            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
        )

        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = StrmrConstants.Colors.TEXT_SECONDARY,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun TraktToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StrmrConstants.Colors.TEXT_SECONDARY,
            modifier = Modifier.size(StrmrConstants.Dimensions.SPACING_LARGE),
        )

        Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_MEDIUM))

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
                modifier = Modifier.padding(top = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2),
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StrmrConstants.Colors.PRIMARY_BLUE,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = StrmrConstants.Colors.BORDER_DARK,
                ),
        )
    }
}

@Composable
fun TraktStatItem(
    label: String,
    value: String,
    icon: ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StrmrConstants.Colors.PRIMARY_BLUE,
            modifier = Modifier.size(StrmrConstants.Dimensions.Icons.STANDARD),
        )

        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))

        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = StrmrConstants.Colors.PRIMARY_BLUE,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888),
        )
    }
}

@Composable
fun ModernTraktAuthDialog(
    userCode: String,
    timeLeft: Int,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    var currentTimeLeft by remember { mutableStateOf(timeLeft) }

    // Countdown timer
    LaunchedEffect(timeLeft) {
        currentTimeLeft = timeLeft
        while (currentTimeLeft > 0) {
            delay(1000)
            currentTimeLeft--
        }
        if (currentTimeLeft <= 0) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(StrmrConstants.Dimensions.SPACING_SECTION),
            colors =
                CardDefaults.cardColors(
                    containerColor = StrmrConstants.Colors.SURFACE_DARK,
                ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(StrmrConstants.Dimensions.SPACING_SECTION),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header with icon
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = StrmrConstants.Colors.PRIMARY_BLUE,
                    modifier =
                        Modifier
                            .size(StrmrConstants.Dimensions.Icons.EXTRA_LARGE)
                            .padding(bottom = StrmrConstants.Dimensions.SPACING_STANDARD),
                )

                Text(
                    text = "Authorize Trakt",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SMALL),
                )

                Text(
                    text = "To connect your Trakt account:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StrmrConstants.Colors.TEXT_SECONDARY,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_EXTRA_LARGE),
                )

                // Steps
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                StrmrConstants.Colors.CONTAINER_DARK,
                                StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
                            )
                            .padding(StrmrConstants.Dimensions.SPACING_LARGE),
                ) {
                    AuthStep(
                        number = "1",
                        text = "Go to trakt.tv/activate",
                    )

                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_MEDIUM))

                    AuthStep(
                        number = "2",
                        text = "Enter the code below",
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                // Auth Code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = StrmrConstants.Colors.PRIMARY_BLUE.copy(alpha = StrmrConstants.Colors.Alpha.SUBTLE),
                        ),
                    shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
                ) {
                    Text(
                        text = userCode,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = StrmrConstants.Colors.PRIMARY_BLUE,
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE),
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                // Countdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (currentTimeLeft <= StrmrConstants.Time.TRAKT_WARNING_THRESHOLD_SECONDS) StrmrConstants.Colors.ERROR_RED else StrmrConstants.Colors.TEXT_SECONDARY,
                        modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                    )

                    Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))

                    Text(
                        text = "Expires in ${currentTimeLeft}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentTimeLeft <= StrmrConstants.Time.TRAKT_WARNING_THRESHOLD_SECONDS) StrmrConstants.Colors.ERROR_RED else StrmrConstants.Colors.TEXT_SECONDARY,
                    )
                }

                Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_EXTRA_LARGE))

                // Cancel button
                Button(
                    onClick = onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = StrmrConstants.Colors.BORDER_DARK,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

data class TraktNavItem(
    val title: String,
    val icon: ImageVector,
    val sectionId: String,
    val isConnected: Boolean? = null,
    val isVisible: Boolean = true,
    val showArrow: Boolean = true,
)
