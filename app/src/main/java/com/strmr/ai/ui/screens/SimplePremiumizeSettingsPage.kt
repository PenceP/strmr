package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.viewmodel.PremiumizeSettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SimplePremiumizeSettingsPage(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: PremiumizeSettingsViewModel = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()
    
    // State
    var apiKey by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isConfigured by remember { mutableStateOf(viewModel.isConfigured()) }
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Premiumize",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Text(
                        text = "Configure your Premiumize API key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a1a1a)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isConfigured) Color(0xFF4CAF50) else Color(0xFF888888),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = if (isConfigured) "Configured" else "Not Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        
                        Text(
                            text = if (isConfigured) "Ready to fetch streams" else "Enter your API key below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // API Key Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a1a1a)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "Find your API key at premiumize.me/account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
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
                                    tint = Color(0xFF888888)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedLabelColor = Color(0xFF007AFF),
                            unfocusedLabelColor = Color(0xFF888888),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF007AFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Error message
                    validationError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFFF3B30),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            disabledContainerColor = Color(0xFF333333)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Validating...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save API Key")
                        }
                    }
                    
                    // Clear Button (if configured)
                    if (isConfigured) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.clearApiKey()
                                apiKey = ""
                                isConfigured = false
                                validationError = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF3B30)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear API Key")
                        }
                    }
                }
            }
        }
    }
}