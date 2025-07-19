package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.DataSourceRegistry
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.ui.screens.PagingUiState
import com.strmr.ai.ui.screens.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel that handles configuration-driven data loading
 * Eliminates the need for hardcoded data source methods
 */
abstract class BaseConfigurableViewModel<T : Any>(
    private val genericRepository: GenericTraktRepository,
    private val mediaType: MediaType
) : ViewModel() {
    
    protected val _uiState = MutableStateFlow(UiState<T>())
    val uiState = _uiState.asStateFlow()
    
    private val _pagingUiState = MutableStateFlow(PagingUiState<T>())
    val pagingUiState = _pagingUiState.asStateFlow()
    
    private var pageConfiguration: PageConfiguration? = null
    private var initialLoadComplete = false
    
    /**
     * Initialize with configuration - this replaces hardcoded init blocks
     */
    fun initializeWithConfiguration(configuration: PageConfiguration) {
        pageConfiguration = configuration
        setupDataSources()
        loadData()
        refreshAllData()
    }
    
    /**
     * Setup data sources dynamically based on JSON configuration
     */
    private fun setupDataSources() {
        pageConfiguration?.let { config ->
            val enabledRows = config.rows.filter { it.enabled }.sortedBy { it.order }
            val pagingFlows = mutableMapOf<String, Flow<PagingData<T>>>()
            
            for (row in enabledRows) {
                val dataSourceConfig = getDataSourceConfig(row.id)
                if (dataSourceConfig != null) {
                    // Create paging flow for this data source
                    pagingFlows[row.title] = createPagingFlow(dataSourceConfig)
                    Log.d("BaseConfigurableViewModel", "📋 Setup data source: ${row.title} -> ${dataSourceConfig.endpoint}")
                } else {
                    Log.w("BaseConfigurableViewModel", "⚠️ Unknown data source: ${row.id}")
                }
            }
            
            _pagingUiState.value = PagingUiState(mediaRows = pagingFlows)
        }
    }
    
    /**
     * Load data dynamically based on configuration
     */
    private fun loadData() {
        pageConfiguration?.let { config ->
            val enabledRows = config.rows.filter { it.enabled }.sortedBy { it.order }
            val dataSourceConfigs = enabledRows.mapNotNull { row ->
                getDataSourceConfig(row.id)?.let { dsConfig ->
                    row.title to dsConfig
                }
            }
            
            if (dataSourceConfigs.isNotEmpty()) {
                viewModelScope.launch {
                    loadMultipleDataSources(dataSourceConfigs)
                }
            }
        }
    }
    
    /**
     * Refresh all data sources dynamically
     */
    private fun refreshAllData() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                pageConfiguration?.let { config ->
                    val enabledRows = config.rows.filter { it.enabled }
                    for (row in enabledRows) {
                        val dataSourceConfig = getDataSourceConfig(row.id)
                        if (dataSourceConfig != null) {
                            refreshDataSource(dataSourceConfig)
                            Log.d("BaseConfigurableViewModel", "🔄 Refreshed: ${dataSourceConfig.endpoint}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BaseConfigurableViewModel", "❌ Error refreshing data sources", e)
            } finally {
                initialLoadComplete = true
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * Get data source config based on media type
     */
    private fun getDataSourceConfig(id: String): DataSourceConfig? {
        return when (mediaType) {
            MediaType.MOVIE -> DataSourceRegistry.getMovieDataSource(id)
            MediaType.TV_SHOW -> DataSourceRegistry.getTvDataSource(id)
        }
    }
    
    // Abstract methods for subclasses to implement
    abstract fun createPagingFlow(config: DataSourceConfig): Flow<PagingData<T>>
    abstract suspend fun loadMultipleDataSources(dataSources: List<Pair<String, DataSourceConfig>>)
    abstract suspend fun refreshDataSource(config: DataSourceConfig)
}