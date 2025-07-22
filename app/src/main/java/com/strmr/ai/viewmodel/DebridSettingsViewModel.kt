package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.PremiumizeAuthService
import com.strmr.ai.data.ScraperRepository
import com.strmr.ai.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DebridSettingsViewModel @Inject constructor(
    private val scraperRepository: ScraperRepository,
    private val premiumizeAuthService: PremiumizeAuthService
) : ViewModel() {
    
    companion object {
        private const val TAG = "DebridSettingsViewModel"
    }
    
    // Auth state
    private val _premiumizeAuthState = MutableStateFlow(PremiumizeAuthState())
    val premiumizeAuthState: StateFlow<PremiumizeAuthState> = _premiumizeAuthState.asStateFlow()
    
    // User state
    private val _premiumizeUserState = MutableStateFlow(PremiumizeUserState())
    val premiumizeUserState: StateFlow<PremiumizeUserState> = _premiumizeUserState.asStateFlow()
    
    private var pollingJob: Job? = null
    
    init {
        // Check if already authenticated
        checkExistingAuth()
    }
    
    private fun checkExistingAuth() {
        viewModelScope.launch {
            val config = scraperRepository.getDebridConfiguration()
            if (config != null && config.provider.id == "premiumize" && !config.apiKey.isNullOrBlank()) {
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    isAuthorized = true,
                    accessToken = config.apiKey
                )
                // Fetch user info
                fetchUserInfo(config.apiKey)
            }
        }
    }
    
    fun startPremiumizeAuth() {
        viewModelScope.launch {
            try {
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    isAuthorizing = true,
                    error = null
                )
                
                // Get device code
                val deviceCodeResponse = premiumizeAuthService.getDeviceCode()
                
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    userCode = deviceCodeResponse.userCode,
                    deviceCode = deviceCodeResponse.deviceCode,
                    verificationUrl = deviceCodeResponse.verificationUrl,
                    timeLeft = deviceCodeResponse.expiresIn
                )
                
                // Start polling for authorization
                startPolling(deviceCodeResponse)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Premiumize auth", e)
                val errorMessage = when {
                    e.message?.contains("HTTP 400") == true -> 
                        "Invalid client configuration. Please contact support to register this app with Premiumize."
                    e.message?.contains("HTTP 401") == true -> 
                        "Authentication failed. Invalid client credentials."
                    e.message?.contains("HTTP 403") == true -> 
                        "Access denied. Client not authorized."
                    else -> "Failed to start authorization: ${e.message}"
                }
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    isAuthorizing = false,
                    error = errorMessage
                )
            }
        }
    }
    
    private fun startPolling(deviceCodeResponse: PremiumizeDeviceCodeResponse) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = deviceCodeResponse.expiresIn / deviceCodeResponse.interval
            
            while (attempts < maxAttempts && isActive) {
                delay(deviceCodeResponse.interval * 1000L)
                
                // Update time left
                val timeLeft = deviceCodeResponse.expiresIn - (attempts * deviceCodeResponse.interval)
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    timeLeft = timeLeft
                )
                
                try {
                    val tokenResponse = premiumizeAuthService.getAccessToken(
                        deviceCode = deviceCodeResponse.deviceCode
                    )
                    
                    // Success! Save the token
                    scraperRepository.saveDebridConfiguration("premiumize", tokenResponse.accessToken)
                    
                    _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                        isAuthorizing = false,
                        isAuthorized = true,
                        accessToken = tokenResponse.accessToken,
                        userCode = "",
                        deviceCode = ""
                    )
                    
                    // Fetch user info
                    fetchUserInfo(tokenResponse.accessToken)
                    
                    break
                    
                } catch (e: Exception) {
                    // Authorization pending, continue polling
                    Log.d(TAG, "Authorization pending, continuing to poll...")
                }
                
                attempts++
            }
            
            if (attempts >= maxAttempts) {
                _premiumizeAuthState.value = _premiumizeAuthState.value.copy(
                    isAuthorizing = false,
                    error = "Authorization timed out"
                )
            }
        }
    }
    
    private suspend fun fetchUserInfo(accessToken: String) {
        try {
            val accountInfo = premiumizeAuthService.getAccountInfo("Bearer $accessToken")
            
            _premiumizeUserState.value = _premiumizeUserState.value.copy(
                account = accountInfo,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Premiumize user info", e)
            _premiumizeUserState.value = _premiumizeUserState.value.copy(
                error = "Failed to fetch user info"
            )
        }
    }
    
    fun cancelPremiumizeAuth() {
        pollingJob?.cancel()
        _premiumizeAuthState.value = PremiumizeAuthState()
    }
    
    fun logout() {
        pollingJob?.cancel()
        scraperRepository.saveDebridConfiguration("", "")
        _premiumizeAuthState.value = PremiumizeAuthState()
        _premiumizeUserState.value = PremiumizeUserState()
    }
    
    fun clearError() {
        _premiumizeAuthState.value = _premiumizeAuthState.value.copy(error = null)
        _premiumizeUserState.value = _premiumizeUserState.value.copy(error = null)
    }
    
    fun getDebridConfiguration(): DebridConfiguration? {
        return scraperRepository.getDebridConfiguration()
    }
    
    fun setPreferredScraper(scraper: String) {
        scraperRepository.setPreferredScraper(scraper)
    }
    
    fun setQualityPreference(quality: String) {
        scraperRepository.setQualityPreference(quality)
    }
    
    fun getQualityPreference(): String {
        return scraperRepository.getQualityPreference()
    }
}

// State classes
data class PremiumizeAuthState(
    val isAuthorizing: Boolean = false,
    val isAuthorized: Boolean = false,
    val userCode: String = "",
    val deviceCode: String = "",
    val verificationUrl: String = "",
    val accessToken: String? = null,
    val timeLeft: Int = 0,
    val error: String? = null
)

data class PremiumizeUserState(
    val account: PremiumizeAccount? = null,
    val error: String? = null
)