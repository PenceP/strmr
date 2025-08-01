package com.strmr.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.PremiumizeSettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SimplePremiumizeSettingsPage(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(StrmrConstants.Dimensions.SPACING_SECTION),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(StrmrConstants.Dimensions.Components.BUTTON_HEIGHT),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = StrmrConstants.Colors.TEXT_PRIMARY,
                        modifier = Modifier.size(StrmrConstants.Dimensions.Icons.STANDARD),
                    )
                }

                Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))

                Column {
                    Text(
                        text = "Premiumize",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                    )

                    Text(
                        text = "Configure your Premiumize API key",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                    )
                }
            }

            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.Icons.EXTRA_LARGE))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = StrmrConstants.Colors.SURFACE_DARK,
                    ),
                shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(StrmrConstants.Dimensions.SPACING_LARGE),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isConfigured) StrmrConstants.Colors.SUCCESS_GREEN else StrmrConstants.Colors.TEXT_SECONDARY,
                        modifier = Modifier.size(StrmrConstants.Dimensions.Icons.STANDARD),
                    )

                    Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_STANDARD))

                    Column {
                        Text(
                            text = if (isConfigured) "Configured" else "Not Configured",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StrmrConstants.Colors.TEXT_PRIMARY,
                        )

                        Text(
                            text = if (isConfigured) "Ready to fetch streams" else "Enter your API key below",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StrmrConstants.Colors.TEXT_SECONDARY,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SECTION))

            // API Key Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = StrmrConstants.Colors.SURFACE_DARK,
                    ),
                shape = StrmrConstants.Shapes.CORNER_RADIUS_MEDIUM,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(StrmrConstants.Dimensions.SPACING_LARGE),
                ) {
                    Text(
                        text = "API Key",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StrmrConstants.Colors.TEXT_PRIMARY,
                        modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_SMALL),
                    )

                    Text(
                        text = "Find your API key at premiumize.me/account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StrmrConstants.Colors.TEXT_SECONDARY,
                        modifier = Modifier.padding(bottom = StrmrConstants.Dimensions.SPACING_STANDARD),
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
                                    tint = StrmrConstants.Colors.TEXT_SECONDARY,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StrmrConstants.Colors.PRIMARY_BLUE,
                                unfocusedBorderColor = StrmrConstants.Colors.BORDER_DARK,
                                focusedLabelColor = StrmrConstants.Colors.PRIMARY_BLUE,
                                unfocusedLabelColor = StrmrConstants.Colors.TEXT_SECONDARY,
                                focusedTextColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                unfocusedTextColor = StrmrConstants.Colors.TEXT_PRIMARY,
                                cursorColor = StrmrConstants.Colors.PRIMARY_BLUE,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Error message
                    validationError?.let { error ->
                        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))
                        Text(
                            text = error,
                            color = StrmrConstants.Colors.ERROR_RED,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_STANDARD))

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
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = StrmrConstants.Colors.PRIMARY_BLUE,
                                disabledContainerColor = StrmrConstants.Colors.BORDER_DARK,
                            ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                                color = StrmrConstants.Colors.TEXT_PRIMARY,
                                strokeWidth = StrmrConstants.Dimensions.Components.BORDER_WIDTH * 2,
                            )
                            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                            Text("Validating...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                            )
                            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                            Text("Save API Key")
                        }
                    }

                    // Clear Button (if configured)
                    if (isConfigured) {
                        Spacer(modifier = Modifier.height(StrmrConstants.Dimensions.SPACING_SMALL))

                        OutlinedButton(
                            onClick = {
                                viewModel.clearApiKey()
                                apiKey = ""
                                isConfigured = false
                                validationError = null
                            },
                            colors =
                                ButtonDefaults.outlinedButtonColors(
                                    contentColor = StrmrConstants.Colors.ERROR_RED,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(StrmrConstants.Dimensions.Icons.SMALL),
                            )
                            Spacer(modifier = Modifier.width(StrmrConstants.Dimensions.SPACING_SMALL))
                            Text("Clear API Key")
                        }
                    }
                }
            }
        }
    }
}
