package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.RetrofitInstance
import com.strmr.ai.data.AccountRepository
import com.strmr.ai.data.DeviceCodeResponse
import com.strmr.ai.data.TokenResponse
import com.strmr.ai.data.TraktUserProfile
import com.strmr.ai.data.TraktUserStats
import com.strmr.ai.data.TraktSyncSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async

data class TraktAuthState(
    val isAuthorizing: Boolean = false,
    val userCode: String = "",
    val expiresIn: Int = 0,
    val timeLeft: Int = 0,
    val isAuthorized: Boolean = false,
    val error: String? = null
)

data class TraktUserState(
    val profile: TraktUserProfile? = null,
    val stats: TraktUserStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TraktSettingsState(
    val syncOnLaunch: Boolean = true,
    val syncAfterPlayback: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

class SettingsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {
    private val _traktAuthState = MutableStateFlow(TraktAuthState())
    val traktAuthState = _traktAuthState.asStateFlow()

    private val _traktUserState = MutableStateFlow(TraktUserState())
    val traktUserState = _traktUserState.asStateFlow()

    private val _traktSettingsState = MutableStateFlow(TraktSettingsState())
    val traktSettingsState = _traktSettingsState.asStateFlow()

    private var deviceCode: String = ""
    private var pollingJob: Job? = null
    private var isPollingActive = false

    init {
        // Check for existing Trakt account on initialization
        checkExistingTraktAccount()
    }

    private fun checkExistingTraktAccount() {
        viewModelScope.launch {
            try {
                val account = accountRepository.getAccount("trakt")
                if (account != null && accountRepository.isAccountValid("trakt")) {
                    Log.d("SettingsViewModel", "✅ Found valid Trakt account: ${account.username}")
                    _traktAuthState.value = _traktAuthState.value.copy(
                        isAuthorized = true
                    )
                    // Load user data with stored token
                    loadUserData(account.accessToken)
                } else {
                    Log.d("SettingsViewModel", "❌ No valid Trakt account found")
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Error checking existing Trakt account", e)
            }
        }
    }

    fun startTraktAuth() {
        viewModelScope.launch {
            try {
                _traktAuthState.value = _traktAuthState.value.copy(
                    isAuthorizing = true,
                    error = null
                )

                Log.d("SettingsViewModel", "🔐 Starting Trakt device authorization...")
                
                // Get device code from Trakt
                val deviceCodeResponse = RetrofitInstance.traktAuthManager.startDeviceAuth()
                
                deviceCode = deviceCodeResponse.device_code
                
                _traktAuthState.value = _traktAuthState.value.copy(
                    userCode = deviceCodeResponse.user_code,
                    expiresIn = deviceCodeResponse.expires_in,
                    timeLeft = deviceCodeResponse.expires_in
                )

                Log.d("SettingsViewModel", "📱 Device code received: ${deviceCodeResponse.user_code}")
                Log.d("SettingsViewModel", "⏰ Expires in: ${deviceCodeResponse.expires_in} seconds")
                Log.d("SettingsViewModel", "🔄 Polling interval: ${deviceCodeResponse.interval} seconds")

                // Start polling for authorization
                startPolling(deviceCode, deviceCodeResponse.interval, deviceCodeResponse.expires_in)

            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to start Trakt auth", e)
                _traktAuthState.value = _traktAuthState.value.copy(
                    isAuthorizing = false,
                    error = "Failed to start authorization: ${e.message}"
                )
            }
        }
    }

    private fun startPolling(deviceCode: String, interval: Int, expiresIn: Int) {
        isPollingActive = true
        pollingJob = viewModelScope.launch {
            try {
                val tokenResponse = RetrofitInstance.traktAuthManager.pollForToken(deviceCode, interval)
                
                if (tokenResponse != null && isPollingActive) {
                    Log.d("SettingsViewModel", "✅ Trakt authorization successful!")
                    
                    // Save credentials to database
                    accountRepository.saveAccount(
                        accountType = "trakt",
                        accessToken = tokenResponse.access_token,
                        refreshToken = tokenResponse.refresh_token,
                        expiresIn = tokenResponse.expires_in,
                        createdAt = tokenResponse.created_at // <-- pass created_at
                    )
                    
                    _traktAuthState.value = _traktAuthState.value.copy(
                        isAuthorizing = false,
                        isAuthorized = true,
                        error = null
                    )
                    isPollingActive = false
                    
                    // Load user data after successful authorization
                    loadUserData(tokenResponse.access_token)
                } else if (isPollingActive) {
                    Log.w("SettingsViewModel", "⏰ Trakt authorization timed out")
                    _traktAuthState.value = _traktAuthState.value.copy(
                        isAuthorizing = false,
                        error = "Authorization timed out. Please try again."
                    )
                    isPollingActive = false
                }
            } catch (e: Exception) {
                if (isPollingActive) {
                    Log.e("SettingsViewModel", "❌ Polling failed", e)
                    _traktAuthState.value = _traktAuthState.value.copy(
                        isAuthorizing = false,
                        error = "Authorization failed: ${e.message}"
                    )
                    isPollingActive = false
                }
            }
        }
    }

    private fun loadUserData(accessToken: String) {
        if (accessToken.isEmpty()) {
            Log.w("SettingsViewModel", "⚠️ No access token available for user data")
            return
        }
        
        viewModelScope.launch {
            try {
                _traktUserState.value = _traktUserState.value.copy(isLoading = true, error = null)
                
                Log.d("SettingsViewModel", "👤 Loading Trakt user data...")
                
                // Create authenticated service
                val authenticatedService = RetrofitInstance.createAuthenticatedTraktService(accessToken)
                
                // Load user profile and stats in parallel
                val profileDeferred = async { 
                    try {
                        Log.d("SettingsViewModel", "📡 Fetching user profile...")
                        authenticatedService.getUserProfile()
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "❌ Failed to fetch user profile", e)
                        throw e
                    }
                }
                val statsDeferred = async { 
                    try {
                        Log.d("SettingsViewModel", "📊 Fetching user stats...")
                        authenticatedService.getUserStats()
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "❌ Failed to fetch user stats", e)
                        throw e
                    }
                }
                
                val profile = profileDeferred.await()
                val stats = statsDeferred.await()
                
                Log.d("SettingsViewModel", "✅ Successfully fetched user data: ${profile.username}")
                
                // Update account with username (preserve existing tokens)
                try {
                    val existingAccount = accountRepository.getAccount("trakt")
                    if (existingAccount != null) {
                        val expiresIn = ((existingAccount.expiresAt - System.currentTimeMillis()) / 1000).toInt()
                        accountRepository.saveAccount(
                            accountType = "trakt",
                            accessToken = existingAccount.accessToken,
                            refreshToken = existingAccount.refreshToken,
                            expiresIn = if (expiresIn > 0) expiresIn else 3600, // Default to 1 hour if calculation fails
                            createdAt = (existingAccount.expiresAt / 1000L) - if (expiresIn > 0) expiresIn else 3600, // Approximate original createdAt
                            username = profile.username
                        )
                        Log.d("SettingsViewModel", "💾 Updated account with username: ${profile.username}")
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "❌ Failed to update account with username", e)
                    // Don't fail the entire operation if account update fails
                }
                
                _traktUserState.value = _traktUserState.value.copy(
                    profile = profile,
                    stats = stats,
                    isLoading = false
                )
                
                Log.d("SettingsViewModel", "✅ User data loaded successfully: ${profile.username}")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to load user data", e)
                _traktUserState.value = _traktUserState.value.copy(
                    isLoading = false,
                    error = "Failed to load user data: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                Log.d("SettingsViewModel", "🚪 Logging out from Trakt...")
                
                // Delete account from database
                accountRepository.deleteAccount("trakt")
                
                _traktAuthState.value = TraktAuthState()
                _traktUserState.value = TraktUserState()
                
                Log.d("SettingsViewModel", "✅ Logged out successfully")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "❌ Failed to logout", e)
            }
        }
    }

    fun toggleSyncOnLaunch() {
        _traktSettingsState.value = _traktSettingsState.value.copy(
            syncOnLaunch = !_traktSettingsState.value.syncOnLaunch
        )
        Log.d("SettingsViewModel", "🔄 Sync on launch: ${_traktSettingsState.value.syncOnLaunch}")
    }

    fun toggleSyncAfterPlayback() {
        _traktSettingsState.value = _traktSettingsState.value.copy(
            syncAfterPlayback = !_traktSettingsState.value.syncAfterPlayback
        )
        Log.d("SettingsViewModel", "🔄 Sync after playback: ${_traktSettingsState.value.syncAfterPlayback}")
    }

    fun cancelTraktAuth() {
        Log.d("SettingsViewModel", "🚫 Cancelling Trakt authorization...")
        isPollingActive = false
        pollingJob?.cancel()
        pollingJob = null
        
        _traktAuthState.value = _traktAuthState.value.copy(
            isAuthorizing = false,
            error = null
        )
        Log.d("SettingsViewModel", "🚫 Trakt authorization cancelled")
    }

    fun clearError() {
        _traktAuthState.value = _traktAuthState.value.copy(error = null)
        _traktUserState.value = _traktUserState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        isPollingActive = false
        pollingJob?.cancel()
    }
} 