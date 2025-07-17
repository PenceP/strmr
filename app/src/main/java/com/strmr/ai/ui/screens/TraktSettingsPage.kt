package com.strmr.ai.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
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
    
    val prevIsAuthorized = remember { mutableStateOf(traktAuthState.isAuthorized) }
    LaunchedEffect(traktAuthState.isAuthorized) {
        if (!prevIsAuthorized.value && traktAuthState.isAuthorized) {
            onTraktAuthorized?.invoke()
        }
        prevIsAuthorized.value = traktAuthState.isAuthorized
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a1a),
                        Color(0xFF0d0d0d)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBackPressed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2a2a2a)
                    ),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Text(
                    text = "Trakt Settings",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Account Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2a2a2a)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬",
                            fontSize = 32.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Account Status",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (traktAuthState.isAuthorized) {
                                    traktUserState.profile?.username ?: "Connected"
                                } else {
                                    "Not connected"
                                },
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        // Status indicator
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(
                                    color = if (traktAuthState.isAuthorized) Color(0xFF00ff88) else Color.Gray,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    
                    if (traktAuthState.isAuthorized) {
                        Divider(
                            color = Color(0xFF404040),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        // User info
                        traktUserState.profile?.let { profile ->
                            SettingsRow(
                                label = "Username",
                                value = profile.username,
                                icon = "👤"
                            )
                            
                            if (profile.name != null) {
                                SettingsRow(
                                    label = "Name",
                                    value = profile.name,
                                    icon = "📝"
                                )
                            }
                            
                            SettingsRow(
                                label = "VIP Status",
                                value = if (profile.vip) "VIP Member" else "Free Account",
                                icon = if (profile.vip) "👑" else "👤"
                            )
                        }
                    }
                }
            }
            
            // Statistics Card
            if (traktAuthState.isAuthorized && traktUserState.stats != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2a2a2a)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "📊 Statistics",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        traktUserState.stats?.let { stats ->
                            SettingsRow(
                                label = "Movies Watched",
                                value = "${stats.movies.watched}",
                                icon = "🎬"
                            )
                            
                            SettingsRow(
                                label = "Shows Watched",
                                value = "${stats.shows.watched}",
                                icon = "📺"
                            )
                            
                            SettingsRow(
                                label = "Total Minutes",
                                value = "${stats.movies.minutes + stats.episodes.minutes}",
                                icon = "⏱️"
                            )
                            
                            SettingsRow(
                                label = "Total Ratings",
                                value = "${stats.ratings.total}",
                                icon = "⭐"
                            )
                        }
                    }
                }
            }
            
            // Sync Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2a2a2a)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "🔄 Sync Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    SettingsToggleRow(
                        label = "Sync on app launch",
                        checked = traktSettingsState.syncOnLaunch,
                        onCheckedChange = { viewModel.toggleSyncOnLaunch() },
                        icon = "🚀"
                    )
                    
                    SettingsToggleRow(
                        label = "Sync after playback",
                        checked = traktSettingsState.syncAfterPlayback,
                        onCheckedChange = { viewModel.toggleSyncAfterPlayback() },
                        icon = "▶️"
                    )
                    
                    if (traktSettingsState.lastSyncTimestamp > 0) {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        val lastSync = dateFormat.format(Date(traktSettingsState.lastSyncTimestamp))
                        
                        SettingsRow(
                            label = "Last Sync",
                            value = lastSync,
                            icon = "🕒"
                        )
                    }
                }
            }
            
            // Action Buttons
            if (traktAuthState.isAuthorized) {
                Button(
                    onClick = { viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFff4757)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Logout from Trakt",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Button(
                    onClick = { viewModel.startTraktAuth() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00ff88)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Connect Trakt Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }
        }
        
        // Trakt Authorization Dialog
        if (traktAuthState.isAuthorizing) {
            TraktAuthDialog(
                userCode = traktAuthState.userCode,
                timeLeft = traktAuthState.timeLeft,
                onDismiss = { /* do nothing, let dialog stay open unless user cancels */ },
                onCancel = { viewModel.cancelTraktAuth() }
            )
        }
        
        // Error Dialog
        (traktAuthState.error ?: traktUserState.error)?.let { error ->
            ErrorDialog(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00ff88),
                checkedTrackColor = Color(0xFF00ff88).copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun TraktAuthDialog(
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
                containerColor = Color(0xFF2a2a2a)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "🔐 Trakt Authorization",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Instructions
                Text(
                    text = "To authorize Strmr with your Trakt account:",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Steps
                Text(
                    text = "1. Go to trakt.tv/activate",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "2. Enter the code below",
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Auth Code
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1a1a1a)
                    )
                ) {
                    Text(
                        text = userCode,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00ff88),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
                
                // Countdown
                Text(
                    text = "Time remaining: ${currentTimeLeft}s",
                    fontSize = 14.sp,
                    color = if (currentTimeLeft <= 30) Color(0xFFFF6B6B) else Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Cancel button
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFff4757)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
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
                containerColor = Color(0xFF2a2a2a)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❌ Error",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B6B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFff4757)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "OK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
} 