package com.strmr.ai.data

import com.strmr.ai.data.youtube.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * Unit tests for YouTubeExtractor
 */
class YouTubeExtractorTest {
    
    @Mock
    private lateinit var innerTubeClient: YouTubeInnerTubeClient
    
    @Mock
    private lateinit var formatSelector: YouTubeFormatSelector
    
    @Mock
    private lateinit var streamUrlResolver: YouTubeStreamUrlResolver
    
    @Mock
    private lateinit var proxyExtractor: YouTubeProxyExtractor
    
    @Mock
    private lateinit var nParamTransformer: YouTubeNParamTransformer
    
    private lateinit var extractor: YouTubeExtractor
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        extractor = YouTubeExtractor(innerTubeClient, formatSelector, streamUrlResolver, proxyExtractor)
    }
    
    @Test
    fun testVideoIdExtraction() {
        // Test various YouTube URL formats
        val testCases = mapOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ" to "dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
            "https://www.youtube.com/v/dQw4w9WgXcQ" to "dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42s" to "dQw4w9WgXcQ",
            "not-a-youtube-url" to null,
            "" to null
        )
        
        testCases.forEach { (url, expectedId) ->
            val actualId = extractor.extractVideoId(url)
            assertEquals("Failed for URL: $url", expectedId, actualId)
        }
    }
    
    @Test
    fun testIsYouTubeUrl() {
        val youtubeUrls = listOf(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtube.com/watch?v=abc123",
            "http://www.youtube.com/embed/test"
        )
        
        val nonYoutubeUrls = listOf(
            "https://vimeo.com/12345",
            "https://example.com/video",
            "not-a-url",
            ""
        )
        
        youtubeUrls.forEach { url ->
            assertTrue("Should identify $url as YouTube URL", extractor.isYouTubeUrl(url))
        }
        
        nonYoutubeUrls.forEach { url ->
            assertFalse("Should NOT identify $url as YouTube URL", extractor.isYouTubeUrl(url))
        }
    }
    
    @Test
    fun testThumbnailUrl() {
        val videoId = "dQw4w9WgXcQ"
        val expectedUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
        val actualUrl = extractor.getThumbnailUrl(videoId)
        assertEquals(expectedUrl, actualUrl)
    }
    
    @Test
    fun testWatchUrl() {
        val videoId = "dQw4w9WgXcQ"
        val expectedUrl = "https://www.youtube.com/watch?v=$videoId"
        val actualUrl = extractor.getWatchUrl(videoId)
        assertEquals(expectedUrl, actualUrl)
    }
    
    @Test
    fun testDirectUrlExtraction() = runBlocking {
        // Test simulating actual YouTube extraction behavior
        val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        
        // In the real implementation, it will try to extract and likely fail
        // because we're using mocks, so it should return null
        val result = extractor.extractDirectUrl(testUrl)
        
        // With mocked dependencies that return null by default,
        // the extractor should gracefully return null
        assertNull("Should return null when mocked dependencies return null", result)
        println("✅ Extraction returned null as expected with mock dependencies")
    }
    
    @Test
    fun testDirectUrlExtractionWithInvalidUrl() = runBlocking {
        val result = extractor.extractDirectUrl("not-a-youtube-url")
        assertNull("Should return null for invalid URL", result)
    }
    
    @Test
    fun testDirectUrlExtractionWithEmptyUrl() = runBlocking {
        val result = extractor.extractDirectUrl("")
        assertNull("Should return null for empty URL", result)
    }
    
    
    @Test
    fun testDirectUrlExtractionWhenNoFormatsAvailable() = runBlocking {
        val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val videoId = "dQw4w9WgXcQ"
        
        val mockPlayerResponse = PlayerResponse(
            streamingData = StreamingData(
                adaptiveFormats = listOf(),
                formats = listOf(),
                expiresInSeconds = null,
                hlsManifestUrl = null,
                dashManifestUrl = null
            ),
            videoDetails = null
        )
        
        whenever(innerTubeClient.getPlayerResponse(videoId)).thenReturn(mockPlayerResponse)
        whenever(formatSelector.hasPlayableFormats(any())).thenReturn(false)
        
        val result = extractor.extractDirectUrl(testUrl)
        assertNull("Should return null when no formats available", result)
    }
    
    @Test
    fun testDirectUrlExtractionWithProxyFallback() = runBlocking {
        val testUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val videoId = "dQw4w9WgXcQ"
        val proxyUrl = "https://invidious.fdn.fr/latest_version?id=$videoId&itag=22"
        
        // Mock InnerTube failure
        whenever(innerTubeClient.getPlayerResponse(videoId)).thenReturn(null)
        
        // Mock proxy extractor success
        whenever(proxyExtractor.extractVideoUrl(testUrl)).thenReturn(proxyUrl)
        
        val result = extractor.extractDirectUrl(testUrl)
        assertEquals("Should return proxy URL when InnerTube fails", proxyUrl, result)
    }
}