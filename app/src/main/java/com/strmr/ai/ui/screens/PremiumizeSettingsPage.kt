package com.strmr.ai.ui.screens

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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.ui.components.AuthStep
import com.strmr.ai.ui.components.ModernErrorDialog
import com.strmr.ai.viewmodel.DebridSettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*

@Composable
fun PremiumizeSettingsPage(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: DebridSettingsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    
    // Collect states
    val authState by viewModel.premiumizeAuthState.collectAsState()
    val userState by viewModel.premiumizeUserState.collectAsState()
    
    // State management
    val scrollState = rememberScrollState()
    var selectedNavSection by remember { mutableStateOf("connection") }
    val sectionPositions = remember { mutableMapOf<String, Int>() }
    
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
                    text = "Premiumize",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "High-speed streaming & downloads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                // Navigation Items
                PremiumizeNavSection(
                    title = "Account",
                    items = listOf(
                        PremiumizeNavItem("Connection Status", Icons.Default.CloudQueue, sectionId = "connection", isConnected = authState.isAuthorized),
                        PremiumizeNavItem("Storage & Usage", Icons.Default.Storage, sectionId = "storage", isVisible = authState.isAuthorized),
                        PremiumizeNavItem("Account Info", Icons.Default.Person, sectionId = "account", isVisible = authState.isAuthorized && userState.account != null)
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
                
                PremiumizeNavSection(
                    title = "Scraper Settings",
                    items = listOf(
                        PremiumizeNavItem("Torrentio Config", Icons.Default.Settings, sectionId = "torrentio", isVisible = authState.isAuthorized),
                        PremiumizeNavItem("Quality Preferences", Icons.Default.HighQuality, sectionId = "quality", isVisible = authState.isAuthorized)
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
                PremiumizeContentSection(
                    title = "Connection Status",
                    subtitle = if (authState.isAuthorized) "Your Premiumize account is connected" else "Connect your Premiumize account for high-speed streaming",
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        sectionPositions["connection"] = coordinates.positionInParent().y.toInt()
                    }
                ) {
                    ModernPremiumizeCard(
                        title = "Premiumize Account",
                        subtitle = if (authState.isAuthorized) {
                            "Connected as ${userState.account?.email ?: "User"}"
                        } else {
                            "Not connected - Click to authorize"
                        },
                        icon = Icons.Default.CloudQueue,
                        isConnected = authState.isAuthorized,
                        onClick = if (!authState.isAuthorized) {
                            { viewModel.startPremiumizeAuth() }
                        } else null
                    ) {
                        if (authState.isAuthorized) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                userState.account?.let { account ->
                                    PremiumizeInfoRow(
                                        label = "Email",
                                        value = account.email,
                                        icon = Icons.Default.Email
                                    )
                                    
                                    val daysRemaining = ((account.premiumUntil - System.currentTimeMillis() / 1000) / 86400).toInt()
                                    PremiumizeInfoRow(
                                        label = "Premium Status",
                                        value = if (daysRemaining > 0) "$daysRemaining days remaining" else "Expired",
                                        icon = Icons.Default.CalendarToday,
                                        valueColor = if (daysRemaining > 0) Color(0xFF4CAF50) else Color(0xFFFF3B30)
                                    )
                                    
                                    PremiumizeInfoRow(
                                        label = "Account Status",
                                        value = account.status.capitalize(),
                                        icon = Icons.Default.CheckCircle,
                                        valueColor = if (account.status == "active") Color(0xFF4CAF50) else Color(0xFFFF9500)
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
                
                if (authState.isAuthorized && userState.account != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Storage Section
                    PremiumizeContentSection(
                        title = "Storage & Usage",
                        subtitle = "Monitor your cloud storage and download usage",
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["storage"] = coordinates.positionInParent().y.toInt()
                        }
                    ) {
                        ModernPremiumizeCard(
                            title = "Cloud Storage",
                            subtitle = "Your Premiumize storage usage",
                            icon = Icons.Default.Storage,
                            showArrow = false
                        ) {
                            userState.account?.let { account ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    // Storage Bar
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF222222),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(20.dp)
                                    ) {
                                        val totalGB = (account.spaceLimit ?: 1099511627776L) / 1073741824.0
                                        val usedGB = account.spaceUsed / 1073741824.0
                                        val percentUsed = (usedGB / totalGB).toFloat()
                                        val df = DecimalFormat("#.##")
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Used: ${df.format(usedGB)} GB",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Total: ${df.format(totalGB)} GB",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = percentUsed,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = when {
                                                percentUsed > 0.9f -> Color(0xFFFF3B30)
                                                percentUsed > 0.7f -> Color(0xFFFF9500)
                                                else -> Color(0xFF007AFF)
                                            },
                                            trackColor = Color(0xFF333333)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = "${(percentUsed * 100).toInt()}% used",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Download stats
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFF222222),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val df = DecimalFormat("#.##")
                                            Text(
                                                text = "${df.format(account.limitUsed / 1073741824.0)} GB",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Downloaded",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Torrentio Configuration Section
                    PremiumizeContentSection(
                        title = "Scraper Configuration",
                        subtitle = "Your account is automatically configured with Torrentio",
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["torrentio"] = coordinates.positionInParent().y.toInt()
                        }
                    ) {
                        ModernPremiumizeCard(
                            title = "Torrentio Integration",
                            subtitle = "Premiumize is configured for instant streaming",
                            icon = Icons.Default.Link,
                            showArrow = false
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF222222)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Active Configuration",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text(
                                            text = "Your Premiumize OAuth token is automatically included in Torrentio requests for:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF888888),
                                            lineHeight = 20.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Column {
                                            ConfigBenefit("Instant playback of cached torrents")
                                            ConfigBenefit("No waiting for downloads")
                                            ConfigBenefit("Access to premium sources")
                                            ConfigBenefit("Higher quality streams")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Quality Preferences Section
                    PremiumizeContentSection(
                        title = "Quality Preferences",
                        subtitle = "Set your preferred streaming quality",
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            sectionPositions["quality"] = coordinates.positionInParent().y.toInt()
                        }
                    ) {
                        ModernPremiumizeCard(
                            title = "Streaming Quality",
                            subtitle = "Choose your default quality preference",
                            icon = Icons.Default.HighQuality,
                            showArrow = false
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                var selectedQuality by remember { mutableStateOf(viewModel.getQualityPreference()) }
                                
                                val qualities = listOf("4K", "1080p", "720p", "Auto")
                                
                                qualities.forEach { quality ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (selectedQuality == quality) 
                                                    Color(0xFF007AFF).copy(alpha = 0.15f) 
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                selectedQuality = quality
                                                viewModel.setQualityPreference(quality)
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectedQuality == quality,
                                            onClick = {
                                                selectedQuality = quality
                                                viewModel.setQualityPreference(quality)
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color(0xFF007AFF),
                                                unselectedColor = Color(0xFF666666)
                                            )
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(
                                                text = quality,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = when(quality) {
                                                    "4K" -> "Best quality (requires fast connection)"
                                                    "1080p" -> "High quality (recommended)"
                                                    "720p" -> "Good quality (saves bandwidth)"
                                                    "Auto" -> "Automatically select based on connection"
                                                    else -> ""
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF888888)
                                            )
                                        }
                                    }
                                    
                                    if (quality != qualities.last()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Premiumize Authorization Dialog
        if (authState.isAuthorizing) {
            PremiumizeAuthDialog(
                userCode = authState.userCode,
                verificationUrl = authState.verificationUrl,
                timeLeft = authState.timeLeft,
                onDismiss = { /* Keep dialog open */ },
                onCancel = { viewModel.cancelPremiumizeAuth() }
            )
        }
        
        // Error Dialog
        (authState.error ?: userState.error)?.let { error ->
            ModernErrorDialog(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
fun PremiumizeAuthDialog(
    userCode: String,
    verificationUrl: String,
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
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Authorize Premiumize",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "To connect your Premiumize account:",
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
                        text = "Go to $verificationUrl"
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
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = userCode,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
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
fun PremiumizeNavSection(
    title: String,
    items: List<PremiumizeNavItem>,
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
        PremiumizeNavItemRow(
            item = item,
            isSelected = selectedSection == item.sectionId,
            onClick = { onSectionClick(item.sectionId) }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PremiumizeNavItemRow(
    item: PremiumizeNavItem,
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
                        color = if (item.isConnected) Color(0xFF4CAF50) else Color.Gray,
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
fun PremiumizeContentSection(
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
fun ModernPremiumizeCard(
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
                                color = if (isConnected) Color(0xFF4CAF50) else Color.Gray,
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
fun PremiumizeInfoRow(
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
fun ConfigBenefit(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4CAF50),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            lineHeight = 18.sp
        )
    }
}



data class PremiumizeNavItem(
    val title: String,
    val icon: ImageVector,
    val sectionId: String,
    val isConnected: Boolean? = null,
    val isVisible: Boolean = true,
    val showArrow: Boolean = true
)