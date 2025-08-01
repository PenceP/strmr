package com.strmr.ai.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.strmr.ai.data.NetworkInfo
import com.strmr.ai.viewmodel.HomeMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Loads row configurations from JSON files in assets
 */
class ConfigurationLoader(private val context: Context) {
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .create()

    /**
     * Load configuration for a specific page
     */
    suspend fun loadPageConfiguration(page: String): PageConfiguration? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "${page.uppercase()}.json"
                val inputStream = context.assets.open("config/$fileName")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                gson.fromJson(jsonString, PageConfiguration::class.java)
            } catch (e: IOException) {
                android.util.Log.e("ConfigurationLoader", "Error loading configuration for page: $page", e)
                null
            } catch (e: Exception) {
                android.util.Log.e("ConfigurationLoader", "Error parsing configuration for page: $page", e)
                null
            }
        }
    }

    /**
     * Get collections from configuration as HomeMediaItem.Collection objects
     */
    fun getCollectionsFromConfig(config: PageConfiguration): List<HomeMediaItem.Collection> {
        return config.collections?.map { collectionConfig ->
            HomeMediaItem.Collection(
                id = collectionConfig.id,
                name = collectionConfig.name,
                backgroundImageUrl = collectionConfig.backgroundImageUrl,
                nameDisplayMode = collectionConfig.nameDisplayMode,
                dataUrl = collectionConfig.dataUrl,
            )
        } ?: emptyList()
    }

    /**
     * Get networks from configuration as NetworkInfo objects
     */
    fun getNetworksFromConfig(config: PageConfiguration): List<NetworkInfo> {
        return config.networks?.map { networkConfig ->
            NetworkInfo(
                id = networkConfig.id,
                name = networkConfig.name,
                posterUrl = networkConfig.backgroundImageUrl,
                dataUrl = networkConfig.dataUrl,
            )
        } ?: emptyList()
    }

    /**
     * Get directors from configuration as HomeMediaItem.Collection objects
     */
    fun getDirectorsFromConfig(config: PageConfiguration): List<HomeMediaItem.Collection> {
        return config.directors?.map { directorConfig ->
            HomeMediaItem.Collection(
                id = directorConfig.id,
                name = directorConfig.name,
                backgroundImageUrl = directorConfig.backgroundImageUrl,
                nameDisplayMode = directorConfig.nameDisplayMode,
                dataUrl = directorConfig.dataUrl,
            )
        } ?: emptyList()
    }

    /**
     * Get nested rows from configuration as complete row configurations
     */
    fun getNestedRowsFromConfig(rowConfig: RowConfig): List<RowConfig> {
        return rowConfig.nestedRows?.filter { it.enabled } ?: emptyList()
    }

    /**
     * Get nested items from configuration as NetworkInfo objects
     */
    fun getNestedItemsFromConfig(rowConfig: RowConfig): List<NetworkInfo> {
        return rowConfig.nestedItems?.map { nestedItemConfig ->
            NetworkInfo(
                id = nestedItemConfig.id,
                name = nestedItemConfig.name,
                posterUrl = nestedItemConfig.backgroundImageUrl,
                dataUrl = nestedItemConfig.dataUrl,
            )
        } ?: emptyList()
    }

    /**
     * Get enabled rows sorted by order
     */
    fun getEnabledRowsSortedByOrder(config: PageConfiguration): List<RowConfig> {
        return config.rows
            .filter { it.enabled }
            .sortedBy { it.order }
    }

    /**
     * Get row configuration by ID
     */
    fun getRowConfigById(
        config: PageConfiguration,
        id: String,
    ): RowConfig? {
        return config.rows.find { it.id == id }
    }

    /**
     * Check if a row should show hero section
     */
    fun shouldShowHero(
        config: PageConfiguration,
        rowId: String,
    ): Boolean {
        return getRowConfigById(config, rowId)?.showHero == true
    }

    /**
     * Check if a row should show loading state
     */
    fun shouldShowLoading(
        config: PageConfiguration,
        rowId: String,
    ): Boolean {
        return getRowConfigById(config, rowId)?.showLoading == true
    }

    /**
     * Get card height for a row
     */
    fun getCardHeight(
        config: PageConfiguration,
        rowId: String,
    ): Int {
        return getRowConfigById(config, rowId)?.cardHeight ?: 140
    }

    /**
     * Get card type for a row
     */
    fun getCardType(
        config: PageConfiguration,
        rowId: String,
    ): CardType {
        val rowConfig = getRowConfigById(config, rowId)
        return rowConfig?.let { CardType.fromString(it.cardType) } ?: CardType.PORTRAIT
    }

    /**
     * Check if a row should show overlays
     */
    fun shouldShowOverlays(
        config: PageConfiguration,
        rowId: String,
    ): Boolean {
        return getRowConfigById(config, rowId)?.displayOptions?.showOverlays == true
    }

    /**
     * Check if a row is clickable
     */
    fun isRowClickable(
        config: PageConfiguration,
        rowId: String,
    ): Boolean {
        return getRowConfigById(config, rowId)?.displayOptions?.clickable == true
    }
}
