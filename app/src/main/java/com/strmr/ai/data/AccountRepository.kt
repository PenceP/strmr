package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.AccountDao
import com.strmr.ai.data.database.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import com.strmr.ai.data.RetrofitInstance
import okhttp3.logging.HttpLoggingInterceptor

class AccountRepository(
    private val accountDao: AccountDao,
    private val traktApiService: TraktApiService
) {
    fun getAccountFlow(accountType: String): Flow<AccountEntity?> = accountDao.getAccountFlow(accountType)
    
    fun getAllAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun saveAccount(
        accountType: String,
        accessToken: String,
        refreshToken: String,
        expiresIn: Int,
        createdAt: Long, // <-- add this param
        username: String? = null
    ) {
        try {
            val expiresAt = (createdAt * 1000L) + (expiresIn * 1000L)
            val account = AccountEntity(
                accountType = accountType,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAt = expiresAt,
                username = username
            )
            accountDao.insertAccount(account)
            Log.d("AccountRepository", "✅ Saved $accountType account credentials")
        } catch (e: Exception) {
            Log.e("AccountRepository", "❌ Failed to save $accountType account", e)
            throw e
        }
    }

    suspend fun getAccount(accountType: String): AccountEntity? {
        return try {
            accountDao.getAccount(accountType)
        } catch (e: Exception) {
            Log.e("AccountRepository", "❌ Failed to get $accountType account", e)
            null
        }
    }

    suspend fun isAccountValid(accountType: String): Boolean {
        val account = getAccount(accountType) ?: return false
        return System.currentTimeMillis() < account.expiresAt
    }

    suspend fun refreshTokenIfNeeded(accountType: String): String? {
        val account = getAccount(accountType) ?: return null
        Log.d("AccountRepository", "🔑 Current $accountType token: "+
            "${account.accessToken.take(8)}... expiresAt=${account.expiresAt}, now=${System.currentTimeMillis()}")
        // Check if token expires within the next 5 minutes
        val fiveMinutesFromNow = System.currentTimeMillis() + (5 * 60 * 1000L)
        if (account.expiresAt <= fiveMinutesFromNow) {
            Log.d("AccountRepository", "🔄 Token for $accountType expires soon, refreshing... (refreshToken=${account.refreshToken.take(8)}...)")
            val response = RetrofitInstance.traktAuthManager.refreshAccessToken(account.refreshToken)
            if (response != null) {
                saveAccount(
                    accountType,
                    response.access_token,
                    response.refresh_token,
                    response.expires_in,
                    response.created_at, // <-- use created_at from response
                    account.username
                )
                Log.d("AccountRepository", "✅ Token refreshed for $accountType: ${response.access_token.take(8)}...")
                return response.access_token
            } else {
                Log.e("AccountRepository", "❌ Failed to refresh token for $accountType")
                return null
            }
        }
        return account.accessToken
    }

    suspend fun deleteAccount(accountType: String) {
        try {
            accountDao.deleteAccount(accountType)
            Log.d("AccountRepository", "🗑️ Deleted $accountType account")
        } catch (e: Exception) {
            Log.e("AccountRepository", "❌ Failed to delete $accountType account", e)
            throw e
        }
    }

    suspend fun updateLastSync(accountType: String) {
        try {
            accountDao.updateLastSync(accountType, System.currentTimeMillis())
            Log.d("AccountRepository", "🕒 Updated last sync for $accountType")
        } catch (e: Exception) {
            Log.e("AccountRepository", "❌ Failed to update last sync for $accountType", e)
        }
    }

    suspend fun getContinueWatching(): List<ContinueWatchingItem> {
        // Always get a valid, refreshed token
        val accessToken = refreshTokenIfNeeded("trakt")?.trim()
        Log.d("AccountRepository", "🔑 Using access token for continue watching: ${accessToken?.take(8)}...")
        if (!accessToken.isNullOrEmpty()) {
            try {
                // Create authenticated service
                val authService = RetrofitInstance.createAuthenticatedTraktService(accessToken)
                val continueWatchingService = ContinueWatchingService()
                val result = continueWatchingService.getContinueWatching(authService)
                Log.d("AccountRepository", "✅ Continue watching returned ${result.size} items")
                return result
            } catch (e: retrofit2.HttpException) {
                Log.e("AccountRepository", "❌ HTTP error fetching continue watching: ${e.code()} ${e.message()}")
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    Log.e("AccountRepository", "❌ Error body: $errorBody")
                } catch (_: Exception) {}
            } catch (e: Exception) {
                Log.e("AccountRepository", "Error fetching continue watching", e)
            }
        } else {
            Log.e("AccountRepository", "No valid Trakt access token available for continue watching.")
        }
        return emptyList()
    }

    suspend fun saveTraktTokens(accessToken: String, refreshToken: String, expiresIn: Int, createdAt: Long) {
        val accountEntity = AccountEntity(
            accountType = "trakt",
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = (createdAt * 1000L) + (expiresIn * 1000L)
        )
        accountDao.insertAccount(accountEntity)
    }

    suspend fun clearTraktTokens() {
        accountDao.deleteAccount("trakt")
    }
} 