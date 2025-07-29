package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.repository.AccountRepository
import com.strmr.ai.domain.repository.ContinueWatchingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Use case for getting continue watching items
 * Encapsulates the business logic currently in HomeViewModel
 */
class GetContinueWatchingUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Get continue watching items as a Flow for reactive UI updates
     */
    fun getContinueWatchingFlow(): Flow<List<ContinueWatchingItem>> = flow {
        try {
            val items = accountRepository.getContinueWatching()
            emit(items)
        } catch (e: Exception) {
            // Log error and emit empty list instead of crashing
            android.util.Log.e("GetContinueWatchingUseCase", "Error fetching continue watching", e)
            emit(emptyList())
        }
    }
    
    /**
     * Refresh continue watching data
     */
    suspend fun refreshContinueWatching(): Result<List<ContinueWatchingItem>> {
        return try {
            val items = accountRepository.getContinueWatching()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}