package com.strmr.ai.domain.usecase

import com.strmr.ai.data.TmdbApiService
import com.strmr.ai.data.TmdbImagesResponse
import com.strmr.ai.data.TmdbLogo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FetchLogoUseCaseTest {
    private lateinit var tmdbApiService: TmdbApiService
    private lateinit var fetchLogoUseCase: FetchLogoUseCase

    @Before
    fun setup() {
        tmdbApiService = mock()
        fetchLogoUseCase = FetchLogoUseCase(tmdbApiService)
    }

    @Test
    fun `fetchAndExtractLogo returns English logo when available`() =
        runTest {
            // Given
            val tmdbId = 123
            val expectedLogoPath = "/english_logo.png"
            val mockResponse =
                TmdbImagesResponse(
                    backdrops = emptyList(),
                    logos =
                        listOf(
                            TmdbLogo(file_path = "/other_logo.png", iso_639_1 = "fr"),
                            TmdbLogo(file_path = expectedLogoPath, iso_639_1 = "en"),
                            TmdbLogo(file_path = "/another_logo.png", iso_639_1 = null),
                        ),
                )

            whenever(tmdbApiService.getMovieImages(tmdbId)).thenReturn(mockResponse)

            // When
            val result = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, MediaType.MOVIE)

            // Then
            assertEquals("https://image.tmdb.org/t/p/original$expectedLogoPath", result)
        }

    @Test
    fun `fetchAndExtractLogo returns first available logo when no English logo`() =
        runTest {
            // Given
            val tmdbId = 123
            val expectedLogoPath = "/first_logo.png"
            val mockResponse =
                TmdbImagesResponse(
                    backdrops = emptyList(),
                    logos =
                        listOf(
                            TmdbLogo(file_path = expectedLogoPath, iso_639_1 = "fr"),
                            TmdbLogo(file_path = "/second_logo.png", iso_639_1 = "de"),
                        ),
                )

            whenever(tmdbApiService.getMovieImages(tmdbId)).thenReturn(mockResponse)

            // When
            val result = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, MediaType.MOVIE)

            // Then
            assertEquals("https://image.tmdb.org/t/p/original$expectedLogoPath", result)
        }

    @Test
    fun `fetchAndExtractLogo returns null when no logos available`() =
        runTest {
            // Given
            val tmdbId = 123
            val mockResponse =
                TmdbImagesResponse(
                    backdrops = emptyList(),
                    logos = emptyList(),
                )

            whenever(tmdbApiService.getMovieImages(tmdbId)).thenReturn(mockResponse)

            // When
            val result = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, MediaType.MOVIE)

            // Then
            assertNull(result)
        }

    @Test
    fun `fetchAndExtractLogo returns null when API throws exception`() =
        runTest {
            // Given
            val tmdbId = 123
            whenever(tmdbApiService.getMovieImages(tmdbId)).thenThrow(RuntimeException("API Error"))

            // When
            val result = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, MediaType.MOVIE)

            // Then
            assertNull(result)
        }

    @Test
    fun `fetchAndExtractLogo calls correct API method for TV shows`() =
        runTest {
            // Given
            val tmdbId = 123
            val mockResponse =
                TmdbImagesResponse(
                    backdrops = emptyList(),
                    logos = listOf(TmdbLogo(file_path = "/tv_logo.png", iso_639_1 = "en")),
                )

            whenever(tmdbApiService.getTvShowImages(tmdbId)).thenReturn(mockResponse)

            // When
            val result = fetchLogoUseCase.fetchAndExtractLogo(tmdbId, MediaType.TV_SHOW)

            // Then
            assertEquals("https://image.tmdb.org/t/p/original/tv_logo.png", result)
        }
}
