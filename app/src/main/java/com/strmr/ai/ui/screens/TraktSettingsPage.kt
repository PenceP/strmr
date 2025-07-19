package com.strmr.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TraktSettingsPage(
    onBackPressed: () -> Unit,
    onTraktAuthorized: (() -> Unit)? = null,
    modifier: Modifier = Modifier
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
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left Panel - Navigation
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(
                        Color(0xFF151515),
                        RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
                    .padding(24.dp)
            ) {
                // Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBackPressed() }
                        .background(Color(0xFF1f1f1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Back to Settings",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Header
                Text(
                    text = "Trakt",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Sync your watch history and ratings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Navigation Items
                TraktNavSection(
                    title = "Account",
                    items = listOf(
                        TraktNavItem("Connection Status", Icons.Default.AccountCircle, sectionId = "connection", isConnected = traktAuthState.isAuthorized),
                        TraktNavItem("User Profile", Icons.Default.Person, sectionId = "profile", isVisible = traktAuthState.isAuthorized),
                        TraktNavItem("Statistics", Icons.Default.Analytics, sectionId = "statistics", isVisible = traktAuthState.isAuthorized && traktUserState.stats != null)
                    ),
                    selectedSection = selectedNavSection,
                    onSectionClick = { sectionId ->
                        selectedNavSection = sectionId
                        coroutineScope.launch {
                            sectionPositions[sectionId]?.let { position ->
                                scrollState.animateScrollTo(position)
                            }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                TraktNavSection(
                    title = "Sync Settings",
                    items = listOf(
                        TraktNavItem("Sync Options", Icons.Default.Sync, sectionId = "sync", showArrow = false),
                        TraktNavItem("Last Sync", Icons.Default.Schedule, sectionId = "sync", isVisible = traktSettingsState.lastSyncTimestamp > 0, showArrow = false)
                    ),
                    selectedSection = selectedNavSection,
                    onSectionClick = { sectionId ->
                        selectedNavSection = sectionId
                        coroutineScope.launch {
                            sectionPositions[sectionId]?.let { position ->
                                scrollState.animateScrollTo(position)
                            }
                        }
                    }
                )
            }
            
            // Right Panel - Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(scrollState)
            ) {
                // Connection Status Section
                TraktContentSection(
                    title = "Connection Status",
                    subtitle = if (traktAuthState.isAuthorized) "Your Trakt account is connected" else "Connect your Trakt account to sync your data",
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        sectionPositions["connection"] = coordinates.positionInParent().y.toInt()
                    }
                ) {
                    ModernTraktCard(
                        title = "Trakt Account",
                        subtitle = if (traktAuthState.isAuthorized) {
                            "Connected as ${traktUserState.profile?.username ?: "User"}"
                        } else {
                            "Not connected - Click to authorize"
                        },
                        icon = Icons.Default.AccountCircle,
                        isConnected = traktAuthState.isAuthorized,
                        onClick = if (!traktAuthState.isAuthorized) {
                            { viewModel.startTraktAuth() }
                        } else null
                    ) {
                        if (traktAuthState.isAuthorized) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                traktUserState.profile?.let { profile ->
                                    TraktInfoRow(
                                        label = "Username",
                                        value = profile.username,
                                        icon = Icons.Default.Person
                                    )
                                    
                                    if (profile.name != null) {
                                        TraktInfoRow(
                                            label = "Display Name",
                                            value = profile.name,
                                            icon = Icons.Default.Badge
                                        )
                                    }
                                    
                                    TraktInfoRow(
                                        label = "Account Type",
                                        value = if (profile.vip) "VIP Member" else "Free Account",
                                        icon = if (profile.vip) Icons.Default.Star else Icons.Default.Person,
                                        valueColor = if (profile.vip) Color(0xFF007AFF) else Color.White
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { viewModel.logout() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF3B30)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Disconnect Account",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (traktAuthState.isAuthorized && traktUserState.stats != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Statistics Section
                    TraktContentSection(
                        title = "Your Statistics",
                        subtitle = "Your viewing habits and activity on Trakt",
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["statistics"] = coordinates.positionInParent().y.toInt()
                        }
                    ) {
                        ModernTraktCard(
                            title = "Viewing Statistics",
                            subtitle = "Track your movies, shows, and total watch time",
                            icon = Icons.Default.Analytics,
                            showArrow = false
                        ) {
                            traktUserState.stats?.let { stats ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    // Stats Grid
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF222222),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        TraktStatItem(
                                            label = "Movies",
                                            value = "${stats.movies.watched}",
                                            icon = Icons.Default.Movie
                                        )
                                        
                                        TraktStatItem(
                                            label = "Shows",
                                            value = "${stats.shows.watched}",
                                            icon = Icons.Default.Tv
                                        )
                                        
                                        TraktStatItem(
                                            label = "Episodes",
                                            value = "${stats.episodes.watched}",
                                            icon = Icons.Default.PlayCircle
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Additional Stats
                                    TraktInfoRow(
                                        label = "Total Watch Time",
                                        value = "${(stats.movies.minutes + stats.episodes.minutes) / 60} hours",
                                        icon = Icons.Default.Schedule
                                    )
                                    
                                    TraktInfoRow(
                                        label = "Ratings Given",
                                        value = "${stats.ratings.total}",
                                        icon = Icons.Default.Star
                                    )
                                    
                                    if (stats.movies.collected > 0 || stats.shows.collected > 0) {
                                        TraktInfoRow(
                                            label = "Collection Items",
                                            value = "${stats.movies.collected + stats.shows.collected}",
                                            icon = Icons.Default.Collections
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (traktAuthState.isAuthorized) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Sync Settings Section
                    TraktContentSection(
                        title = "Sync Settings",
                        subtitle = "Control when and how your data syncs with Trakt",
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["sync"] = coordinates.positionInParent().y.toInt()
                        }
                    ) {
                        ModernTraktCard(
                            title = "Automatic Sync",
                            subtitle = "Configure when to sync your watch progress",
                            icon = Icons.Default.Sync,
                            showArrow = false
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                TraktToggleRow(
                                    label = "Sync on app launch",
                                    subtitle = "Automatically sync when opening the app",
                                    checked = traktSettingsState.syncOnLaunch,
                                    onCheckedChange = { viewModel.toggleSyncOnLaunch() },
                                    icon = Icons.Default.Launch
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                TraktToggleRow(
                                    label = "Sync after playback",
                                    subtitle = "Update watch status after finishing content",
                                    checked = traktSettingsState.syncAfterPlayback,
                                    onCheckedChange = { viewModel.toggleSyncAfterPlayback() },
                                    icon = Icons.Default.PlayArrow
                                )
                                
                                if (traktSettingsState.lastSyncTimestamp > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                                    val lastSync = dateFormat.format(Date(traktSettingsState.lastSyncTimestamp))
                                    
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF222222),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFF888888),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Last sync:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                        
                                        Text(
                                            text = lastSync,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 4.dp)
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
                onCancel = { viewModel.cancelTraktAuth() }
            )
        }
        
        // Error Dialog
        (traktAuthState.error ?: traktUserState.error)?.let { error ->
            ModernErrorDialog(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
fun TraktNavSection(
    title: String,
    items: List<TraktNavItem>,
    selectedSection: String = "",
    onSectionClick: (String) -> Unit = {}
) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF888888),
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    items.filter { it.isVisible }.forEach { item ->
        TraktNavItemRow(
            item = item,
            isSelected = selectedSection == item.sectionId,
            onClick = { onSectionClick(item.sectionId) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun TraktNavItemRow(
    item: TraktNavItem,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF007AFF).copy(alpha = 0.15f) else Color(0xFF1f1f1f)
            )
            .clickable { onClick() }
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
fun TraktContentSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
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
fun ModernTraktCard(
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
fun TraktInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TraktToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
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
fun TraktStatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF007AFF),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
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

@Composable
fun ModernTraktAuthDialog(
    userCode: String,
    timeLeft: Int,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
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
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1a1a1a)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Authorize Trakt",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "To connect your Trakt account:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Steps
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF222222),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp)
                ) {
                    AuthStep(
                        number = "1",
                        text = "Go to trakt.tv/activate"
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AuthStep(
                        number = "2",
                        text = "Enter the code below"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Auth Code
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF007AFF).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = userCode,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007AFF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Countdown
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (currentTimeLeft <= 30) Color(0xFFFF3B30) else Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Expires in ${currentTimeLeft}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (currentTimeLeft <= 30) Color(0xFFFF3B30) else Color(0xFF888888)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Cancel button
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AuthStep(
    number: String,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    Color(0xFF007AFF),
                    androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
fun ModernErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1a1a1a)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF3B30),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "OK",
                        fontWeight = FontWeight.Medium
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
    val showArrow: Boolean = true
)