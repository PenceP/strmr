package com.strmr.ai.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain model for user accounts (simplified from entity)
 */
data class UserAccount(
    val accountType: String,
    val username: String,
    val displayName: String? = null,
    val isActive: Boolean = true,
    val lastSyncTime: Long = 0L
)

/**
 * Domain model for continue watching items
 */
data class ContinueWatchingItem(
    val mediaId: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String,
    val posterUrl: String? = null,
    val progress: Float = 0f, // 0.0 to 1.0
    val lastWatched: Long = 0L
)

/**
 * Clean domain repository interface for user accounts
 */
interface AccountRepository {
    
    // Account management
    fun getAccountFlow(accountType: String): Flow<UserAccount?>
    fun getAllAccounts(): Flow<List<UserAccount>>
    suspend fun getAccount(accountType: String): UserAccount?
    suspend fun saveAccount(account: UserAccount): Result<Unit>
    suspend fun deleteAccount(accountType: String): Result<Unit>
    
    // Account validation and auth
    suspend fun isAccountValid(accountType: String): Boolean
    suspend fun refreshTokenIfNeeded(accountType: String): String?
    
    // User data
    suspend fun getContinueWatching(): List<ContinueWatchingItem>
    suspend fun updateLastSync(accountType: String): Result<Unit>
    
    // Trakt-specific (can be abstracted later)
    suspend fun saveTraktTokens(accessToken: String, refreshToken: String, expiresIn: Int): Result<Unit>
    suspend fun clearTraktTokens(): Result<Unit>
}