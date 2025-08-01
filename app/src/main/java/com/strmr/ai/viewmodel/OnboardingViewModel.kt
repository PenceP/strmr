package com.strmr.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strmr.ai.data.OnboardingProgress
import com.strmr.ai.data.OnboardingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing the onboarding experience
 */
@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val onboardingService: OnboardingService,
    ) : ViewModel() {
        val onboardingProgress: StateFlow<OnboardingProgress> = onboardingService.onboardingProgress

        /**
         * Start the onboarding process
         */
        fun startOnboarding() {
            viewModelScope.launch {
                onboardingService.runOnboarding()
            }
        }

        /**
         * Reset onboarding state (for testing/debugging)
         */
        fun resetOnboarding() {
            onboardingService.resetOnboarding()
        }

        /**
         * Check if onboarding is completed
         */
        fun isOnboardingCompleted(): Boolean {
            return onboardingService.isOnboardingCompleted()
        }
    }
