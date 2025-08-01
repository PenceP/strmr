package com.strmr.ai.domain.usecase

import android.util.Log
import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TmdbImagesResponse
import com.strmr.ai.ui.theme.StrmrConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FetchLogoUseCase
    @Inject
    constructor(
        private val tmdbApiService: TmdbApiService,
    ) {
        /**
         * Fetches and extracts the best available logo for a given media item
         * @param tmdbId The TMDB ID of the media item
         * @param mediaType The type of media (MOVIE or TV_SHOW)
         * @return Logo URL if available, null otherwise
         */
        suspend fun fetchAndExtractLogo(
            tmdbId: Int,
            mediaType: MediaType,
        ): String? {
            return try {
                // Log.d("FetchLogoUseCase", "📡 Fetching logo from TMDB API for tmdbId=$tmdbId, type=$mediaType")

                val images =
                    withContext(Dispatchers.IO) {
                        when (mediaType) {
                            MediaType.MOVIE -> tmdbApiService.getMovieImages(tmdbId)
                            MediaType.TV_SHOW -> tmdbApiService.getTvShowImages(tmdbId)
                        }
                    }

                val logoUrl = extractLogoUrl(images)
                // Log.d("FetchLogoUseCase", "🎨 Logo URL for tmdbId=$tmdbId: $logoUrl")

                logoUrl
            } catch (e: Exception) {
                Log.w("FetchLogoUseCase", "❌ Error fetching logo for tmdbId=$tmdbId", e)
                null
            }
        }

        /**
         * Extracts the best logo URL from TMDB images response
         * Prioritizes English logos, then any available logo with a valid path
         */
        private fun extractLogoUrl(images: TmdbImagesResponse): String? {
            // Log.d("FetchLogoUseCase", "🔍 Extracting logo from ${images.logos.size} available logos")

            if (images.logos.isEmpty()) {
                Log.d("FetchLogoUseCase", "❌ No logos available")
                return null
            }

            // Log all available logos for debugging
            // images.logos.forEachIndexed { index, logo ->
            // Log.d("FetchLogoUseCase", "🔍 Logo $index: iso=${logo.iso_639_1}, path=${logo.file_path}")
            // }

            // Prefer English logos, then any logo with a valid path
            val selectedLogo =
                images.logos.firstOrNull { it.iso_639_1 == "en" && !it.file_path.isNullOrBlank() }
                    ?: images.logos.firstOrNull { !it.file_path.isNullOrBlank() }

            val logoUrl = selectedLogo?.file_path?.let { StrmrConstants.Api.TMDB_IMAGE_BASE_ORIGINAL + it }

            // Log.d("FetchLogoUseCase", "✅ Selected logo: iso=${selectedLogo?.iso_639_1}, path=${selectedLogo?.file_path}")
            Log.d("FetchLogoUseCase", "✅ Final logo URL: $logoUrl")

            return logoUrl
        }
    }

enum class MediaType {
    MOVIE,
    TV_SHOW,
}
