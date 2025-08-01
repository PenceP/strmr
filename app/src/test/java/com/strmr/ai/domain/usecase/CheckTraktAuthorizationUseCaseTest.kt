package com.strmr.ai.domain.usecase

import com.strmr.ai.domain.repository.AccountRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CheckTraktAuthorizationUseCaseTest {
    private lateinit var accountRepository: AccountRepository
    private lateinit var checkTraktAuthorizationUseCase: CheckTraktAuthorizationUseCase

    @Before
    fun setup() {
        accountRepository = mock()
        checkTraktAuthorizationUseCase = CheckTraktAuthorizationUseCase(accountRepository)
    }

    @Test
    fun `invoke returns success with true when account is valid`() =
        runTest {
            // Given
            whenever(accountRepository.isAccountValid("trakt")).thenReturn(true)

            // When
            val result = checkTraktAuthorizationUseCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
        }

    @Test
    fun `invoke returns success with false when account is invalid`() =
        runTest {
            // Given
            whenever(accountRepository.isAccountValid("trakt")).thenReturn(false)

            // When
            val result = checkTraktAuthorizationUseCase()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun `invoke returns failure when repository throws exception`() =
        runTest {
            // Given
            val exception = RuntimeException("Repository error")
            whenever(accountRepository.isAccountValid("trakt")).thenThrow(exception)

            // When
            val result = checkTraktAuthorizationUseCase()

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    @Test
    fun `invoke calls repository with correct account type`() =
        runTest {
            // Given
            whenever(accountRepository.isAccountValid("trakt")).thenReturn(true)

            // When
            checkTraktAuthorizationUseCase()

            // Then
            org.mockito.kotlin.verify(accountRepository).isAccountValid("trakt")
        }
}
