package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for checking Trakt authorization status
 * Encapsulates the business logic currently in HomeViewModel
 */
class CheckTraktAuthorizationUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Check if Trakt account is properly authorized
     */
    suspend operator fun invoke(): Result<Boolean> {
        return try {
            val isValid = accountRepository.isAccountValid("trakt")
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Refresh Trakt authorization if needed
     */
    suspend fun refreshAuthorization(): Result<Boolean> {
        return try {
            val token = accountRepository.refreshTokenIfNeeded("trakt")
            val isAuthorized = token != null
            Result.success(isAuthorized)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}