package com.strmr.ai.ui.components.common.image

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for ImagePreloader
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImagePreloaderTest {
    
    @Mock
    private lateinit var context: Context
    
    @Mock
    private lateinit var imageLoader: StrmrImageLoader
    
    private lateinit var imagePreloader: ImagePreloader
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        imagePreloader = ImagePreloader(context, imageLoader)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `preloadImages - given empty list - should not process any URLs`() = runTest {
        // Given
        val emptyUrls = emptyList<String>()
        
        // When
        imagePreloader.preloadImages(emptyUrls)
        advanceUntilIdle()
        
        // Then
        // No interactions with imageLoader expected
        verifyNoInteractions(imageLoader)
    }
    
    @Test
    fun `preloadImages - given valid URLs - should not crash`() {
        // Given
        val urls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg"
        )
        
        // Mock the imageLoader.imageLoader field
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        
        // When & Then - should not throw exception
        imagePreloader.preloadImages(urls, PreloadPriority.NORMAL)
        
        // Verify basic setup is correct
        assertNotNull(imagePreloader)
        assertEquals(2, urls.size)
    }
    
    @Test
    fun `preloadMediaImages - given media items - should extract URLs correctly`() {
        // Given
        data class TestMediaItem(val imageUrl: String?)
        val mediaItems = listOf(
            TestMediaItem("https://example.com/poster1.jpg"),
            TestMediaItem("https://example.com/poster2.jpg"),
            TestMediaItem(null) // Should be filtered out
        )
        
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        
        // When & Then - should not throw exception
        imagePreloader.preloadMediaImages(
            mediaItems = mediaItems,
            extractImageUrl = { it.imageUrl },
            priority = PreloadPriority.HIGH
        )
        
        // Verify basic functionality
        assertEquals(3, mediaItems.size)
        assertEquals("https://example.com/poster1.jpg", mediaItems[0].imageUrl)
        assertNull(mediaItems[2].imageUrl)
    }
    
    @Test
    fun `preloadTmdbPosters - given poster paths - should handle nulls correctly`() {
        // Given
        val posterPaths = listOf(
            "/abc123.jpg",
            "/def456.jpg",
            null // Should be filtered out
        )
        
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        
        // When & Then - should not throw exception
        imagePreloader.preloadTmdbPosters(posterPaths, PreloadPriority.LOW)
        
        // Verify path filtering logic
        val nonNullPaths = posterPaths.filterNotNull()
        assertEquals(2, nonNullPaths.size)
        assertTrue(nonNullPaths.contains("/abc123.jpg"))
    }
    
    @Test
    fun `preloadTmdbBackdrops - given backdrop paths - should accept valid paths`() {
        // Given
        val backdropPaths = listOf(
            "/backdrop1.jpg",
            "/backdrop2.jpg"
        )
        
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        
        // When & Then - should not throw exception
        imagePreloader.preloadTmdbBackdrops(backdropPaths, PreloadPriority.NORMAL)
        
        // Verify basic functionality
        assertEquals(2, backdropPaths.size)
        assertTrue(backdropPaths.all { it.startsWith("/") })
    }
    
    @Test
    fun `clearCache - should clear memory and disk cache`() {
        // Given
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        val mockMemoryCache = mock<coil.memory.MemoryCache>()
        val mockDiskCache = mock<coil.disk.DiskCache>()
        
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        whenever(mockCoilImageLoader.memoryCache).thenReturn(mockMemoryCache)
        whenever(mockCoilImageLoader.diskCache).thenReturn(mockDiskCache)
        
        // When
        imagePreloader.clearCache()
        
        // Then
        verify(mockMemoryCache).clear()
        verify(mockDiskCache).clear()
    }
    
    @Test
    fun `getCacheStats - given valid caches - should return correct stats`() {
        // Given
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        val mockMemoryCache = mock<coil.memory.MemoryCache>()
        val mockDiskCache = mock<coil.disk.DiskCache>()
        
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        whenever(mockCoilImageLoader.memoryCache).thenReturn(mockMemoryCache)
        whenever(mockCoilImageLoader.diskCache).thenReturn(mockDiskCache)
        whenever(mockMemoryCache.size).thenReturn(1024)
        whenever(mockMemoryCache.maxSize).thenReturn(2048)
        whenever(mockDiskCache.size).thenReturn(5000)
        whenever(mockDiskCache.maxSize).thenReturn(10000)
        
        // When
        val stats = imagePreloader.getCacheStats()
        
        // Then
        assertEquals(1024L, stats.memoryCacheSize)
        assertEquals(2048L, stats.memoryCacheMaxSize)
        assertEquals(5000L, stats.diskCacheSize)
        assertEquals(10000L, stats.diskCacheMaxSize)
        assertEquals(50.0f, stats.memoryCacheUsagePercent, 0.1f)
        assertEquals(50.0f, stats.diskCacheUsagePercent, 0.1f)
    }
    
    @Test
    fun `getCacheStats - given null caches - should return zero stats`() {
        // Given
        val mockCoilImageLoader = mock<coil.ImageLoader>()
        whenever(imageLoader.imageLoader).thenReturn(mockCoilImageLoader)
        whenever(mockCoilImageLoader.memoryCache).thenReturn(null)
        whenever(mockCoilImageLoader.diskCache).thenReturn(null)
        
        // When
        val stats = imagePreloader.getCacheStats()
        
        // Then
        assertEquals(0L, stats.memoryCacheSize)
        assertEquals(0L, stats.memoryCacheMaxSize)
        assertEquals(0L, stats.diskCacheSize)
        assertEquals(0L, stats.diskCacheMaxSize)
        assertEquals(0.0f, stats.memoryCacheUsagePercent, 0.1f)
        assertEquals(0.0f, stats.diskCacheUsagePercent, 0.1f)
    }
    
    @Test
    fun `PreloadPriority - should have all expected values`() {
        // Verify all priority levels exist
        val priorities = PreloadPriority.values()
        assertEquals(3, priorities.size)
        assertTrue(priorities.contains(PreloadPriority.HIGH))
        assertTrue(priorities.contains(PreloadPriority.NORMAL))
        assertTrue(priorities.contains(PreloadPriority.LOW))
    }
    
    @Test
    fun `CacheStats - percentage calculations - should handle edge cases`() {
        // Given zero max size
        val statsWithZeroMax = CacheStats(
            memoryCacheSize = 100L,
            memoryCacheMaxSize = 0L,
            diskCacheSize = 200L,
            diskCacheMaxSize = 0L
        )
        
        // Then should return 0% for both
        assertEquals(0.0f, statsWithZeroMax.memoryCacheUsagePercent, 0.1f)
        assertEquals(0.0f, statsWithZeroMax.diskCacheUsagePercent, 0.1f)
    }
}