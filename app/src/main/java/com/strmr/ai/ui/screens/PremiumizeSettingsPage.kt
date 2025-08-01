package com.strmr.ai.ui.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PremiumizeSettingsPage(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF1a1a1a),
                                Color(0xFF0d0d0d),
                            ),
                    ),
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            // Header with back button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onBackPressed,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2a2a2a),
                        ),
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                Text(
                    text = "Premiumize Settings",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            // Placeholder content
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color(0xFF2a2a2a),
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                ) {
                    Text(
                        text = "☁️ Premiumize Integration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )

                    Text(
                        text = "Premiumize integration coming soon!",
                        fontSize = 16.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}
