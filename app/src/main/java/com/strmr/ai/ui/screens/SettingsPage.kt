package com.strmr.ai.ui.screens

import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.strmr.ai.data.AccountRepository
import com.strmr.ai.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsPage(
    accountRepository: AccountRepository,
    onNavigateToTraktSettings: () -> Unit,
    onNavigateToPremiumizeSettings: () -> Unit,
    onNavigateToRealDebridSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel { 
        SettingsViewModel(accountRepository) 
    }
    val traktAuthState by viewModel.traktAuthState.collectAsState()
    val traktUserState by viewModel.traktUserState.collectAsState()
    
    val navBarWidth = 56.dp
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black,
                        Color(0xFF1a1a1a)
                    )
                )
            )
            .padding(start = navBarWidth)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Accounts Section
            SettingsSection(
                title = "Accounts",
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Trakt Account
                SettingsCard(
                    title = "Trakt",
                    subtitle = if (traktAuthState.isAuthorized) {
                        traktUserState.profile?.username ?: "Connected"
                    } else {
                        "Sync your watch history and ratings"
                    },
                    icon = "🎬",
                    onClick = onNavigateToTraktSettings,
                    modifier = Modifier.padding(bottom = 16.dp),
                    statusIndicator = if (traktAuthState.isAuthorized) Color(0xFF00ff88) else Color.Gray
                )
                
                // Premiumize Account
                SettingsCard(
                    title = "Premiumize",
                    subtitle = "Access your cloud storage",
                    icon = "☁️",
                    onClick = onNavigateToPremiumizeSettings,
                    modifier = Modifier.padding(bottom = 16.dp),
                    statusIndicator = Color.Gray
                )
                
                // RealDebrid Account
                SettingsCard(
                    title = "RealDebrid",
                    subtitle = "Access your cloud storage",
                    icon = "🔗",
                    onClick = onNavigateToRealDebridSettings,
                    modifier = Modifier.padding(bottom = 16.dp),
                    statusIndicator = Color.Gray
                )
            }
            
            // Content Section
            SettingsSection(
                title = "Content",
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                SettingsCard(
                    title = "Home",
                    subtitle = "Customize your home screen",
                    icon = "🏠",
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                SettingsCard(
                    title = "Movies",
                    subtitle = "Movie preferences and filters",
                    icon = "🎬",
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                SettingsCard(
                    title = "TV Shows",
                    subtitle = "TV show preferences and filters",
                    icon = "📺",
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                SettingsCard(
                    title = "Debrid Cloud",
                    subtitle = "Cloud storage settings",
                    icon = "☁️",
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        content()
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusIndicator: Color? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2a2a2a)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Status indicator
            statusIndicator?.let { color ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = color,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(end = 8.dp)
                )
            }
            
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
} 