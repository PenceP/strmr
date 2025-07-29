package com.strmr.ai.data.repository

import android.util.Log
import com.strmr.ai.data.AccountRepository as LegacyAccountRepository
import com.strmr.ai.data.mapper.AccountMapper
import com.strmr.ai.domain.repository.AccountRepository
import com.strmr.ai.domain.repository.UserAccount
import com.strmr.ai.domain.repository.ContinueWatchingItem
import com.strmr.ai.viewmodel.HomeMediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Clean architecture implementation of AccountRepository
 * This wraps the existing AccountRepository and applies our domain mappers
 * 
 * Performance improvements:
 * - Uses domain models to eliminate database entity leakage
 * - Proper error handling with Result types
 * - Centralized mapping logic
 */
class AccountRepositoryImpl @Inject constructor(
    private val legacyRepository: LegacyAccountRepository,
    private val accountMapper: AccountMapper
) : AccountRepository {

    companion object {
        private const val TAG = "AccountRepositoryImpl"
    }

    override fun getAccountFlow(accountType: String): Flow<UserAccount?> {
        Log.d(TAG, "👤 Getting account flow for type: $accountType")
        return legacyRepository.getAccountFlow(accountType).map { entity ->
            entity?.let { 
                accountMapper.mapAccountToDomain(it).also {
                    Log.d(TAG, "✅ Successfully mapped account: ${it.username}")
                }
            }
        }
    }

    override fun getAllAccounts(): Flow<List<UserAccount>> {
        Log.d(TAG, "👥 Getting all accounts flow")
        return legacyRepository.getAllAccounts().map { entityList ->
            Log.d(TAG, "🔄 Mapping ${entityList.size} accounts to domain models")
            entityList.map { accountMapper.mapAccountToDomain(it) }
        }
    }

    override suspend fun getAccount(accountType: String): UserAccount? {
        return try {
            Log.d(TAG, "👤 Getting account for type: $accountType")
            val entity = legacyRepository.getAccount(accountType)
            entity?.let { 
                accountMapper.mapAccountToDomain(it).also {
                    Log.d(TAG, "✅ Successfully mapped account: ${it.username}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting account $accountType", e)
            null
        }
    }

    override suspend fun saveAccount(account: UserAccount): Result<Unit> {
        return try {
            Log.d(TAG, "💾 Saving account: ${account.username} (${account.accountType})")
            
            // Get existing account to preserve tokens
            val existingEntity = legacyRepository.getAccount(account.accountType)
            val entity = accountMapper.mapAccountToEntity(
                domain = account,
                accessToken = existingEntity?.accessToken ?: "",
                refreshToken = existingEntity?.refreshToken ?: "",
                expiresAt = existingEntity?.expiresAt ?: 0L
            )
            
            legacyRepository.saveAccount(
                accountType = entity.accountType,
                accessToken = entity.accessToken,
                refreshToken = entity.refreshToken,
                expiresIn = ((entity.expiresAt - System.currentTimeMillis()) / 1000L).toInt(),
                createdAt = System.currentTimeMillis(),
                username = entity.username
            )
            
            Log.d(TAG, "✅ Successfully saved account: ${account.username}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving account", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(accountType: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting account: $accountType")
            legacyRepository.deleteAccount(accountType)
            Log.d(TAG, "✅ Successfully deleted account: $accountType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting account $accountType", e)
            Result.failure(e)
        }
    }

    override suspend fun isAccountValid(accountType: String): Boolean {
        return try {
            Log.d(TAG, "✅ Checking account validity: $accountType")
            legacyRepository.isAccountValid(accountType).also { isValid ->
                Log.d(TAG, "🔍 Account $accountType is ${if (isValid) "valid" else "invalid"}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking account validity", e)
            false
        }
    }

    override suspend fun refreshTokenIfNeeded(accountType: String): String? {
        return try {
            Log.d(TAG, "🔄 Refreshing token if needed: $accountType")
            legacyRepository.refreshTokenIfNeeded(accountType).also { token ->
                if (token != null) {
                    Log.d(TAG, "✅ Successfully refreshed token for $accountType")
                } else {
                    Log.d(TAG, "ℹ️ No token refresh needed for $accountType")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing token", e)
            null
        }
    }

    override suspend fun getContinueWatching(): List<ContinueWatchingItem> {
        return try {
            Log.d(TAG, "📺 Getting continue watching items")
            val continueWatchingData = legacyRepository.getContinueWatching()
            
            // The legacy method returns HomeMediaItems, we need to convert them
            continueWatchingData.mapNotNull { homeMediaItem ->
                try {
                    when (homeMediaItem) {
                        is HomeMediaItem.Movie -> ContinueWatchingItem(
                            mediaId = homeMediaItem.movie.tmdbId,
                            mediaType = "movie",
                            title = homeMediaItem.movie.title,
                            posterUrl = homeMediaItem.movie.posterUrl,
                            progress = homeMediaItem.progress ?: 0f,
                            lastWatched = System.currentTimeMillis()
                        )
                        is HomeMediaItem.TvShow -> ContinueWatchingItem(
                            mediaId = homeMediaItem.show.tmdbId,
                            mediaType = "tv",
                            title = homeMediaItem.show.title,
                            posterUrl = homeMediaItem.show.posterUrl,
                            progress = homeMediaItem.progress ?: 0f,
                            lastWatched = System.currentTimeMillis()
                        )
                        else -> {
                            Log.w(TAG, "⚠️ Unsupported HomeMediaItem type: ${homeMediaItem::class.simpleName}")
                            null
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to convert continue watching item", e)
                    null
                }
            }.also {
                Log.d(TAG, "✅ Mapped ${it.size} continue watching items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting continue watching items", e)
            emptyList()
        }
    }

    override suspend fun updateLastSync(accountType: String): Result<Unit> {
        return try {
            Log.d(TAG, "⏰ Updating last sync for: $accountType")
            legacyRepository.updateLastSync(accountType)
            Log.d(TAG, "✅ Successfully updated last sync for $accountType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating last sync", e)
            Result.failure(e)
        }
    }

    override suspend fun saveTraktTokens(accessToken: String, refreshToken: String, expiresIn: Int): Result<Unit> {
        return try {
            Log.d(TAG, "🔐 Saving Trakt tokens")
            val createdAt = System.currentTimeMillis()
            legacyRepository.saveTraktTokens(accessToken, refreshToken, expiresIn, createdAt)
            Log.d(TAG, "✅ Successfully saved Trakt tokens")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving Trakt tokens", e)
            Result.failure(e)
        }
    }

    override suspend fun clearTraktTokens(): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Clearing Trakt tokens")
            legacyRepository.clearTraktTokens()
            Log.d(TAG, "✅ Successfully cleared Trakt tokens")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing Trakt tokens", e)
            Result.failure(e)
        }
    }
}