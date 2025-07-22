package com.strmr.ai.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strmr.ai.viewmodel.SettingsViewModel
import com.strmr.ai.viewmodel.UpdateViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsPage(
    onNavigateToTraktSettings: () -> Unit,
    onNavigateToPremiumizeSettings: () -> Unit,
    onNavigateToRealDebridSettings: () -> Unit,
    modifier: Modifier = Modifier
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
    
    val navBarWidth = 56.dp
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0a0a0a),
                        Color(0xFF1a1a1a),
                        Color(0xFF0f0f0f)
                    )
                )
            )
            .padding(start = navBarWidth)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left Panel - Settings Navigation
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(
                        Color(0xFF151515),
                        RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Navigation Sections
                SettingsNavSection(
                    title = "Accounts",
                    items = listOf(
                        SettingsNavItem("Trakt", Icons.Default.AccountCircle, isConnected = traktAuthState.isAuthorized),
                        SettingsNavItem("Premiumize", Icons.Default.Cloud, isConnected = false),
                        SettingsNavItem("RealDebrid", Icons.Default.Link, isConnected = false)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsNavSection(
                    title = "User Interface",
                    items = listOf(
                        SettingsNavItem("Scroll Style", Icons.Default.TouchApp, showArrow = false)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsNavSection(
                    title = "Content",
                    items = listOf(
                        SettingsNavItem("Home", Icons.Default.Home),
                        SettingsNavItem("Movies", Icons.Default.Movie),
                        SettingsNavItem("TV Shows", Icons.Default.Tv),
                        SettingsNavItem("Debrid Cloud", Icons.Default.CloudQueue)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsNavSection(
                    title = "Playback",
                    items = listOf(
                        SettingsNavItem("Auto Play", Icons.Default.PlayArrow, showArrow = false)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsNavSection(
                    title = "System",
                    items = listOf(
                        SettingsNavItem("Updates", Icons.Default.SystemUpdate)
                    )
                )
            }
            
            // Right Panel - Settings Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Account Settings Section
                SettingsContentSection(
                    title = "Account Settings",
                    subtitle = "Manage your streaming and tracking accounts"
                ) {
                    // Trakt Account
                    ModernSettingsCard(
                        title = "Trakt",
                        subtitle = if (traktAuthState.isAuthorized) {
                            "Connected as ${traktUserState.profile?.username ?: "User"}"
                        } else {
                            "Connect to sync your watch history and ratings"
                        },
                        icon = Icons.Default.AccountCircle,
                        isConnected = traktAuthState.isAuthorized,
                        onClick = onNavigateToTraktSettings
                    ) {
                        if (traktAuthState.isAuthorized) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                SettingsToggleRow(
                                    label = "Sync on app launch",
                                    subtitle = "Automatically sync when opening the app",
                                    checked = syncOnLaunch,
                                    onCheckedChange = { syncOnLaunch = it }
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                SettingsToggleRow(
                                    label = "Sync after playback",
                                    subtitle = "Update watch status after finishing content",
                                    checked = syncAfterPlayback,
                                    onCheckedChange = { syncAfterPlayback = it }
                                )
                                
                                if (traktUserState.stats != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    StatsDisplay(
                                        movies = traktUserState.stats?.movies?.watched ?: 0,
                                        shows = traktUserState.stats?.shows?.watched ?: 0,
                                        lastSync = "2 hours ago" // This would be actual timestamp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Premiumize Account
                    ModernSettingsCard(
                        title = "Premiumize",
                        subtitle = "Connect to access your cloud storage",
                        icon = Icons.Default.Cloud,
                        isConnected = false,
                        onClick = onNavigateToPremiumizeSettings
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // RealDebrid Account
                    ModernSettingsCard(
                        title = "RealDebrid",
                        subtitle = "Connect to access your cloud storage",
                        icon = Icons.Default.Link,
                        isConnected = false,
                        onClick = onNavigateToRealDebridSettings
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // User Interface Settings
                SettingsContentSection(
                    title = "User Interface",
                    subtitle = "Customize your viewing experience"
                ) {
                    ModernSettingsCard(
                        title = "Scroll Style",
                        subtitle = "Choose how content scrolls",
                        icon = Icons.Default.TouchApp,
                        showArrow = false
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            SettingsRadioGroup(
                                title = "Content Alignment",
                                options = listOf("Middle", "Left"),
                                selectedOption = scrollStyle,
                                onOptionSelected = { scrollStyle = it },
                                descriptions = mapOf(
                                    "Middle" to "Center content on screen",
                                    "Left" to "Align content to the left"
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Playback Settings
                SettingsContentSection(
                    title = "Playback Settings",
                    subtitle = "Control how content plays"
                ) {
                    ModernSettingsCard(
                        title = "Auto Play",
                        subtitle = "Automatically play next episode",
                        icon = Icons.Default.PlayArrow,
                        showArrow = false
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            SettingsToggleRow(
                                label = "Enable Auto Play",
                                subtitle = "Automatically play the next episode",
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it }
                            )
                            
                            if (autoPlay) {
                                Spacer(modifier = Modifier.height(16.dp))
                                SettingsRadioGroup(
                                    title = "Next Episode Countdown",
                                    options = listOf("3", "5", "10", "15"),
                                    selectedOption = nextEpisodeTime,
                                    onOptionSelected = { nextEpisodeTime = it },
                                    descriptions = mapOf(
                                        "3" to "3 seconds",
                                        "5" to "5 seconds",
                                        "10" to "10 seconds",
                                        "15" to "15 seconds"
                                    )
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
        subtitle = "App updates and system settings"
    ) {
        ModernSettingsCard(
            title = "App Updates",
            subtitle = updateState.updateInfo?.let { updateInfo ->
                if (updateInfo.hasUpdate) {
                    "Version ${updateInfo.latestVersion} available"
                } else {
                    "Version ${updateInfo.currentVersion} - Up to date"
                }
            } ?: "Version Unknown - Up to date",
            icon = Icons.Default.SystemUpdate,
            isConnected = updateState.updateInfo?.hasUpdate,
            showArrow = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                when {
                    updateState.isLoading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF007AFF)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Checking for updates...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF888888)
                            )
                        }
                    }
                    
                    updateState.isDownloading -> {
                        Column {
                            Text(
                                text = "Downloading update...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { updateState.downloadProgress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF007AFF),
                                trackColor = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${updateState.downloadProgress}% - ${updateState.downloadStatus ?: "Preparing..."}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888)
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
                                    color = Color(0xFF007AFF)
                                )
                                
                                updateInfo.releaseNotes?.let { releaseNotes ->
                                    if (releaseNotes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "What's new:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = releaseNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF888888),
                                            maxLines = 3
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row {
                                    Button(
                                        onClick = { updateViewModel.downloadAndInstallUpdate() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF007AFF)
                                        )
                                    ) {
                                        Text("Update Now")
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    TextButton(
                                        onClick = { updateViewModel.checkForUpdates() }
                                    ) {
                                        Text(
                                            text = "Check Again",
                                            color = Color(0xFF888888)
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
                                    color = Color(0xFFFF6B6B)
                                )
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF888888)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                TextButton(
                                    onClick = { 
                                        updateViewModel.dismissError()
                                        updateViewModel.checkForUpdates() 
                                    }
                                ) {
                                    Text(
                                        text = "Retry",
                                        color = Color(0xFF007AFF)
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
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            
                            TextButton(
                                onClick = { updateViewModel.checkForUpdates() }
                            ) {
                                Text(
                                    text = "Check Now",
                                    color = Color(0xFF888888)
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
    items: List<SettingsNavItem>
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF888888),
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    items.forEach { item ->
        SettingsNavItemRow(item)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsNavItemRow(item: SettingsNavItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1f1f1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        
        if (item.isConnected != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (item.isConnected) Color(0xFF007AFF) else Color.Gray,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        if (item.showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SettingsContentSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888),
            modifier = Modifier.padding(bottom = 20.dp)
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
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable { onClick() }
                        } else Modifier
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                if (isConnected != null) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isConnected) Color(0xFF007AFF) else Color.Gray,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                
                if (showArrow && onClick != null) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(20.dp)
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
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
fun SettingsRadioGroup(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    descriptions: Map<String, String> = emptyMap()
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOptionSelected(option) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF007AFF),
                        unselectedColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    
                    descriptions[option]?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888),
                            modifier = Modifier.padding(top = 2.dp)
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
    lastSync: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF222222),
                RoundedCornerShape(8.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Statistics",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Movies", movies.toString())
            StatItem("Shows", shows.toString())
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Last sync: $lastSync",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF007AFF)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888)
        )
    }
}

data class SettingsNavItem(
    val title: String,
    val icon: ImageVector,
    val isConnected: Boolean? = null,
    val showArrow: Boolean = true
)