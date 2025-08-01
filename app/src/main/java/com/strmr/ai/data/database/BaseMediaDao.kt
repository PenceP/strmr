package com.strmr.ai.data.database

/**
 * Base interface for media DAOs that provides common database operations
 * This interface should be implemented by MovieDao and TvShowDao
 */
interface BaseMediaDao<T : Any> {
    suspend fun insertItems(items: List<T>)

    suspend fun updateTrendingItems(items: List<T>)

    suspend fun updatePopularItems(items: List<T>)

    suspend fun getItemByTmdbId(tmdbId: Int): T?

    suspend fun updateItemLogo(
        tmdbId: Int,
        logoUrl: String?,
    )

    suspend fun clearNullLogos()
}
