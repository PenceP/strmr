package com.strmr.ai.data

import android.util.Log
import com.strmr.ai.data.database.*
import com.strmr.ai.ui.theme.StrmrConstants
import com.strmr.ai.viewmodel.HomeMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for caching and retrieving intermediate view data
 * Follows the same caching patterns as other repositories in the app
 */
@Singleton
class IntermediateViewRepository @Inject constructor(
    private val database: StrmrDatabase,
    private val traktApiService: TraktApiService,
    private val tmdbEnrichmentService: TmdbEnrichmentService
) {
    
    companion object {
        private const val TAG = "IntermediateViewRepo"
        private const val CACHE_EXPIRY_HOURS = 24 // Cache valid for 24 hours
        private const val CACHE_EXPIRY_MS = CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
    }
    
    private val intermediateViewDao = database.intermediateViewDao()
    private val movieDao = database.movieDao()
    private val tvShowDao = database.tvShowDao()
    
    /**
     * Get cached data for an intermediate view, or fetch from API if cache is expired/missing
     */
    suspend fun getIntermediateViewData(
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?
    ): List<HomeMediaItem> = withContext(Dispatchers.IO) {
        val cacheId = "$viewType:$itemId"
        val now = System.currentTimeMillis()
        val expiry = now - CACHE_EXPIRY_MS
        
        Log.d(TAG, "🎯 Getting data for $cacheId")
        
        // Check if we have valid cached data
        val isValidCache = intermediateViewDao.isViewCachedAndValid(cacheId, expiry)
        
        if (isValidCache) {
            Log.d(TAG, "✅ Using cached data for $cacheId")
            return@withContext getCachedData(cacheId)
        } else {
            Log.d(TAG, "🌐 Cache miss/expired for $cacheId, fetching from API")
            return@withContext fetchAndCacheData(
                cacheId, viewType, itemId, itemName, 
                itemBackgroundUrl, dataUrl, now
            )
        }
    }
    
    /**
     * Get cached data from database
     */
    private suspend fun getCachedData(cacheId: String): List<HomeMediaItem> {
        val movies = intermediateViewDao.getCachedMoviesForView(cacheId)
        val tvShows = intermediateViewDao.getCachedTvShowsForView(cacheId)
        
        val homeMediaItems = mutableListOf<HomeMediaItem>()
        
        // Add movies
        movies.forEach { movie ->
            homeMediaItems.add(HomeMediaItem.Movie(movie, null))
        }
        
        // Add TV shows
        tvShows.forEach { show ->
            homeMediaItems.add(HomeMediaItem.TvShow(show, null))
        }
        
        Log.d(TAG, "📦 Retrieved ${homeMediaItems.size} cached items for $cacheId")
        
        // Debug log for cache state
        val viewInfo = intermediateViewDao.getIntermediateView(cacheId)
        Log.d(TAG, "📊 Cache info for $cacheId: page=${viewInfo?.page}, totalItems=${viewInfo?.totalItems}")
        
        return homeMediaItems
    }
    
    /**
     * Fetch data from API and cache it
     */
    private suspend fun fetchAndCacheData(
        cacheId: String,
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?,
        timestamp: Long
    ): List<HomeMediaItem> {
        val homeMediaItems = when (viewType) {
            "network" -> loadNetworkContent(itemId, dataUrl)
            "collection" -> {
                // Collections now use Trakt lists just like networks and directors
                if (!dataUrl.isNullOrBlank()) {
                    loadTraktListContent(dataUrl)
                } else {
                    Log.w(TAG, "⚠️ No data URL provided for collection $itemId")
                    emptyList()
                }
            }
            "director" -> {
                // Directors use Trakt lists just like networks
                if (!dataUrl.isNullOrBlank()) {
                    loadTraktListContent(dataUrl)
                } else {
                    Log.w(TAG, "⚠️ No data URL provided for director $itemId")
                    emptyList()
                }
            }
            "trakt_list" -> loadTraktListContent(dataUrl)
            else -> {
                Log.w(TAG, "⚠️ Unknown view type: $viewType")
                emptyList()
            }
        }
        
        if (homeMediaItems.isNotEmpty()) {
            // Cache the data
            cacheIntermediateViewData(
                cacheId, viewType, itemId, itemName, 
                itemBackgroundUrl, dataUrl, timestamp, homeMediaItems
            )
            Log.d(TAG, "💾 Cached ${homeMediaItems.size} items for $cacheId")
        }
        
        return homeMediaItems
    }
    
    /**
     * Cache intermediate view data to database
     */
    private suspend fun cacheIntermediateViewData(
        cacheId: String,
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?,
        timestamp: Long,
        items: List<HomeMediaItem>
    ) {
        // Save the intermediate view metadata
        val viewEntity = IntermediateViewEntity(
            id = cacheId,
            viewType = viewType,
            itemId = itemId,
            itemName = itemName,
            itemBackgroundUrl = itemBackgroundUrl,
            dataUrl = dataUrl,
            lastUpdated = timestamp,
            totalItems = items.size,
            page = 1,
            pageSize = StrmrConstants.Api.DEFAULT_PAGE_SIZE
        )
        
        intermediateViewDao.insertIntermediateView(viewEntity)
        
        // Clear existing items for this view
        intermediateViewDao.deleteIntermediateViewItems(cacheId)
        
        // Save the items
        val itemEntities = items.mapIndexedNotNull { index, item ->
            when (item) {
                is HomeMediaItem.Movie -> IntermediateViewItemEntity(
                    intermediateViewId = cacheId,
                    mediaType = "movie",
                    tmdbId = item.movie.tmdbId,
                    orderIndex = index,
                    addedAt = timestamp
                )
                is HomeMediaItem.TvShow -> IntermediateViewItemEntity(
                    intermediateViewId = cacheId,
                    mediaType = "show",
                    tmdbId = item.show.tmdbId,
                    orderIndex = index,
                    addedAt = timestamp
                )
                is HomeMediaItem.Collection -> {
                    // Collections don't have tmdbId, skip caching them for now
                    Log.w(TAG, "⚠️ Skipping collection item caching: ${item.name}")
                    null
                }
            }
        }
        
        intermediateViewDao.insertIntermediateViewItems(itemEntities)
    }
    
    // ======================== Data Loading Methods ========================
    
    private suspend fun loadNetworkContent(networkId: String, dataUrl: String?): List<HomeMediaItem> {
        return if (!dataUrl.isNullOrBlank()) {
            loadTraktListContent(dataUrl)
        } else {
            Log.w(TAG, "⚠️ No data URL provided for network $networkId")
            emptyList()
        }
    }
    
    private suspend fun loadCollectionContent(collectionId: String): List<HomeMediaItem> {
        // This method is deprecated - collections now use Trakt lists via loadTraktListContent
        Log.w(TAG, "⚠️ Collection content should use dataUrl and loadTraktListContent for $collectionId")
        return emptyList()
    }
    
    private suspend fun loadDirectorContent(directorId: String): List<HomeMediaItem> {
        // Directors don't have their own content, this should not be called
        Log.w(TAG, "⚠️ Director content requires dataUrl, but was called without it for $directorId")
        return emptyList()
    }
    
    private suspend fun loadTraktListContent(dataUrl: String?): List<HomeMediaItem> {
        if (dataUrl.isNullOrBlank()) {
            Log.w(TAG, "⚠️ No data URL provided for Trakt list")
            return emptyList()
        }
        
        return when {
            dataUrl.contains("/sync/collection/movies") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/watchlist/movies") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/collection/shows") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/watchlist/shows") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("trakt.tv/users/") && dataUrl.contains("/lists/") -> {
                loadExternalTraktList(dataUrl)
            }
            else -> {
                Log.w(TAG, "⚠️ Unsupported Trakt URL: $dataUrl")
                emptyList()
            }
        }
    }
    
    private suspend fun loadExternalTraktList(listUrl: String, page: Int = 1): List<HomeMediaItem> {
        Log.d(TAG, "🌐 Loading external Trakt list: $listUrl")
        
        return try {
            // Parse the Trakt URL to extract username and list slug
            val urlRegex = """https://trakt\.tv/users/([^/]+)/lists/([^/?]+)""".toRegex()
            val matchResult = urlRegex.find(listUrl)
            
            if (matchResult == null) {
                Log.e(TAG, "❌ Invalid Trakt list URL format: $listUrl")
                return emptyList()
            }
            
            val username = matchResult.groupValues[1]
            val listSlug = matchResult.groupValues[2]
            
            Log.d(TAG, "📋 Fetching list: username=$username, slug=$listSlug")
            
            // Call the Trakt API with pagination
            val listItems = traktApiService.getUserListItems(
                username = username,
                listSlug = listSlug,
                page = page,
                limit = StrmrConstants.Api.DEFAULT_PAGE_SIZE
            )
            Log.d(TAG, "✅ Received ${listItems.size} items from Trakt list (page $page)")
            
            // Convert Trakt list items to HomeMediaItems and enrich with TMDB data
            val homeMediaItems = mutableListOf<HomeMediaItem>()
            val moviesToSave = mutableListOf<MovieEntity>()
            val showsToSave = mutableListOf<TvShowEntity>()
            
            for (item in listItems) {
                when (item.type) {
                    "movie" -> {
                        item.movie?.let { traktMovie ->
                            val enrichedMovie = tmdbEnrichmentService.enrichMovieWithLogo(traktMovie)
                            enrichedMovie?.let { movie ->
                                homeMediaItems.add(HomeMediaItem.Movie(movie, null))
                                moviesToSave.add(movie)
                            }
                        }
                    }
                    "show" -> {
                        item.show?.let { traktShow ->
                            val enrichedShow = tmdbEnrichmentService.enrichTvShowWithLogo(traktShow)
                            enrichedShow?.let { show ->
                                homeMediaItems.add(HomeMediaItem.TvShow(show, null))
                                showsToSave.add(show)
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Unknown item type: ${item.type}")
                    }
                }
            }
            
            // Save enriched entities to database
            if (moviesToSave.isNotEmpty()) {
                movieDao.insertMovies(moviesToSave)
                Log.d(TAG, "💾 Saved ${moviesToSave.size} movies to database")
            }
            if (showsToSave.isNotEmpty()) {
                tvShowDao.insertTvShows(showsToSave)
                Log.d(TAG, "💾 Saved ${showsToSave.size} TV shows to database")
            }
            
            Log.d(TAG, "🎬 Converted ${homeMediaItems.size} items successfully")
            homeMediaItems
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading external Trakt list", e)
            throw e
        }
    }
    
    // ======================== Cache Management ========================
    
    /**
     * Clear expired cache entries
     */
    suspend fun cleanupExpiredCache() = withContext(Dispatchers.IO) {
        val expiry = System.currentTimeMillis() - CACHE_EXPIRY_MS
        intermediateViewDao.deleteExpiredViews(expiry)
        intermediateViewDao.deleteExpiredItems(expiry)
        intermediateViewDao.deleteOrphanedViews()
        Log.d(TAG, "🧹 Cleaned up expired cache entries")
    }
    
    /**
     * Force refresh a specific intermediate view
     */
    suspend fun refreshIntermediateView(
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?
    ): List<HomeMediaItem> = withContext(Dispatchers.IO) {
        val cacheId = "$viewType:$itemId"
        
        // Delete existing cache
        intermediateViewDao.deleteIntermediateView(cacheId)
        intermediateViewDao.deleteIntermediateViewItems(cacheId)
        
        // Fetch fresh data
        return@withContext fetchAndCacheData(
            cacheId, viewType, itemId, itemName,
            itemBackgroundUrl, dataUrl, System.currentTimeMillis()
        )
    }
    
    /**
     * Load more items for pagination (next page)
     */
    suspend fun loadMoreItems(
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?
    ): List<HomeMediaItem> = withContext(Dispatchers.IO) {
        val cacheId = "$viewType:$itemId"
        Log.d(TAG, "📄 Loading more items for $cacheId")
        
        // Get current cached view to determine next page
        // Calculate next page based on total cached items, not stored page number
        val cachedItemCount = intermediateViewDao.getCachedItemCount(cacheId)
        val nextPage = (cachedItemCount / StrmrConstants.Api.DEFAULT_PAGE_SIZE) + 1
        
        Log.d(TAG, "📄 Page calculation for $cacheId: cachedItems=$cachedItemCount, nextPage=$nextPage")
        
        return@withContext fetchAndCacheMoreData(
            cacheId, viewType, itemId, itemName,
            itemBackgroundUrl, dataUrl, nextPage
        )
    }
    
    /**
     * Fetch additional page of data and append to cache
     */
    private suspend fun fetchAndCacheMoreData(
        cacheId: String,
        viewType: String,
        itemId: String,
        itemName: String,
        itemBackgroundUrl: String?,
        dataUrl: String?,
        page: Int
    ): List<HomeMediaItem> {
        val newItems = when (viewType) {
            "network" -> loadNetworkContentPage(itemId, dataUrl, page)
            "collection" -> {
                // Collections now use Trakt lists just like networks and directors
                if (!dataUrl.isNullOrBlank()) {
                    loadTraktListContentPage(dataUrl, page)
                } else {
                    Log.w(TAG, "⚠️ No data URL provided for collection $itemId pagination")
                    emptyList()
                }
            }
            "director" -> {
                // Directors use Trakt lists just like networks
                if (!dataUrl.isNullOrBlank()) {
                    loadTraktListContentPage(dataUrl, page)
                } else {
                    Log.w(TAG, "⚠️ No data URL provided for director $itemId pagination")
                    emptyList()
                }
            }
            "trakt_list" -> loadTraktListContentPage(dataUrl, page)
            else -> {
                Log.w(TAG, "⚠️ Unknown view type: $viewType")
                emptyList()
            }
        }
        
        if (newItems.isNotEmpty()) {
            // Get current cached items count for ordering
            val currentCount = intermediateViewDao.getCachedItemCount(cacheId)
            
            // Update view metadata with new page info
            val existingView = intermediateViewDao.getIntermediateView(cacheId)
            existingView?.let { view ->
                val updatedView = view.copy(
                    page = page,
                    totalItems = view.totalItems + newItems.size,
                    lastUpdated = System.currentTimeMillis()
                )
                intermediateViewDao.insertIntermediateView(updatedView)
            }
            
            // Add new items to cache with proper ordering
            val itemEntities = newItems.mapIndexedNotNull { index, item ->
                when (item) {
                    is HomeMediaItem.Movie -> IntermediateViewItemEntity(
                        intermediateViewId = cacheId,
                        mediaType = "movie",
                        tmdbId = item.movie.tmdbId,
                        orderIndex = currentCount + index,
                        addedAt = System.currentTimeMillis()
                    )
                    is HomeMediaItem.TvShow -> IntermediateViewItemEntity(
                        intermediateViewId = cacheId,
                        mediaType = "show",
                        tmdbId = item.show.tmdbId,
                        orderIndex = currentCount + index,
                        addedAt = System.currentTimeMillis()
                    )
                    else -> null
                }
            }
            
            intermediateViewDao.insertIntermediateViewItems(itemEntities)
            Log.d(TAG, "📄 Cached ${newItems.size} additional items for $cacheId (page $page)")
        }
        
        return newItems
    }
    
    // Helper methods for loading specific pages
    private suspend fun loadNetworkContentPage(networkId: String, dataUrl: String?, page: Int): List<HomeMediaItem> {
        return if (!dataUrl.isNullOrBlank()) {
            loadTraktListContentPage(dataUrl, page)
        } else {
            Log.w(TAG, "⚠️ No data URL provided for network $networkId")
            emptyList()
        }
    }
    
    private suspend fun loadTraktListContentPage(dataUrl: String?, page: Int): List<HomeMediaItem> {
        if (dataUrl.isNullOrBlank()) {
            Log.w(TAG, "⚠️ No data URL provided for Trakt list")
            return emptyList()
        }
        
        return when {
            dataUrl.contains("/sync/collection/movies") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist pagination not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/watchlist/movies") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist pagination not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/collection/shows") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist pagination not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("/sync/watchlist/shows") -> {
                Log.w(TAG, "⚠️ Sync collection/watchlist pagination not yet implemented for: $dataUrl")
                emptyList()
            }
            dataUrl.contains("trakt.tv/users/") && dataUrl.contains("/lists/") -> {
                loadExternalTraktList(dataUrl, page)
            }
            else -> {
                Log.w(TAG, "⚠️ Unsupported Trakt URL: $dataUrl")
                emptyList()
            }
        }
    }
}