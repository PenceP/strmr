package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.strmr.ai.data.ScraperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PremiumizeSettingsViewModel
    @Inject
    constructor(
        private val scraperRepository: ScraperRepository,
    ) : ViewModel() {
        companion object {
            private const val TAG = "PremiumizeSettingsViewModel"
        }

        fun getApiKey(): String? {
            return scraperRepository.getPremiumizeApiKey()
        }

        fun isConfigured(): Boolean {
            return scraperRepository.isPremiumizeConfigured()
        }

        suspend fun validateAndSaveApiKey(apiKey: String): Boolean {
            return try {
                val isValid = scraperRepository.validatePremiumizeKey(apiKey)
                if (isValid) {
                    scraperRepository.savePremiumizeApiKey(apiKey)
                    Log.d(TAG, "✅ API key validated and saved")
                } else {
                    Log.w(TAG, "❌ API key validation failed")
                }
                isValid
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error validating API key", e)
                false
            }
        }

        fun clearApiKey() {
            scraperRepository.clearPremiumizeApiKey()
            Log.d(TAG, "🗑️ API key cleared")
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
