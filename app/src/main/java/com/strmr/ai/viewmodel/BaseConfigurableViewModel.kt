package com.strmr.ai.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.strmr.ai.config.PageConfiguration
import com.strmr.ai.data.DataSourceConfig
import com.strmr.ai.data.GenericTraktRepository
import com.strmr.ai.data.MediaType
import com.strmr.ai.ui.screens.PagingUiState
import com.strmr.ai.ui.screens.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.strmr.ai.data.OnboardingService

/**
 * Base ViewModel that handles configuration-driven data loading
 * Eliminates the need for hardcoded data source methods
 */
abstract class BaseConfigurableViewModel<T : Any>(
    private val genericRepository: GenericTraktRepository,
    private val mediaType: MediaType,
    private val onboardingService: OnboardingService
) : ViewModel() {
    
    protected val _uiState = MutableStateFlow(UiState<T>())
    val uiState = _uiState.asStateFlow()
    
    private val _pagingUiState = MutableStateFlow(PagingUiState<T>())
    val pagingUiState = _pagingUiState.asStateFlow()
    
    private var pageConfiguration: PageConfiguration? = null
    private var initialLoadComplete = false
    
    // Track which row is currently focused
    private val _focusedRowTitle = MutableStateFlow<String?>(null)
    val focusedRowTitle = _focusedRowTitle.asStateFlow()
    
    // Track current position and total items for each row
    private val _rowPositions = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val rowPositions = _rowPositions.asStateFlow()
    
    /**
     * Update the currently focused row
     */
    fun updateFocusedRow(rowTitle: String?) {
        _focusedRowTitle.value = rowTitle
        Log.d("BaseConfigurableViewModel", "🎯 Row focus updated: $rowTitle")
    }
    
    /**
     * Update the current position and total items for a row
     */
    fun updateRowPosition(rowTitle: String, currentPosition: Int, totalItems: Int) {
        _rowPositions.value = _rowPositions.value + (rowTitle to (currentPosition to totalItems))
        Log.d("BaseConfigurableViewModel", "📍 Row position updated: $rowTitle - Position $currentPosition/$totalItems")
    }
    
    /**
     * Initialize with configuration - this replaces hardcoded init blocks
     */
    fun initializeWithConfiguration(configuration: PageConfiguration) {
        pageConfiguration = configuration
        setupDataSources()
        loadData()
        // Only refresh if cache is empty
        checkAndRefreshEmptyDataSources()
    }
    
    /**
     * Setup data sources dynamically based on JSON configuration
     */
    private fun setupDataSources() {
        pageConfiguration?.let { config ->
            val enabledRows = config.rows.filter { it.enabled }.sortedBy { it.order }
            val pagingFlows = mutableMapOf<String, Flow<PagingData<T>>>()
            
            for (row in enabledRows) {
                val dataSourceConfig = row.toDataSourceConfig()
                if (dataSourceConfig != null) {
                    // Create paging flow for this data source with focus and position checks
                    pagingFlows[row.title] = createPagingFlow(dataSourceConfig, 
                        isRowFocused = { _focusedRowTitle.value == row.title },
                        getCurrentPosition = { 
                            _rowPositions.value[row.title]?.first ?: 0 
                        },
                        getTotalItems = { 
                            _rowPositions.value[row.title]?.second ?: 0 
                        }
                    )
                    Log.d("BaseConfigurableViewModel", "📋 Setup data source: ${row.title} -> ${dataSourceConfig.endpoint}")
                } else {
                    Log.w("BaseConfigurableViewModel", "⚠️ Row missing endpoint/mediaType/cacheKey: ${row.id}")
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
                row.toDataSourceConfig()?.let { dsConfig ->
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
     * Check and refresh only empty data sources (no cached data)
     */
    private fun checkAndRefreshEmptyDataSources() {
        viewModelScope.launch {
            try {
                // Skip if onboarding was completed - data should already be populated
                if (onboardingService.isOnboardingCompleted()) {
                    Log.d("BaseConfigurableViewModel", "✅ Onboarding completed, data should already be populated")
                    initialLoadComplete = true
                    return@launch
                }
                
                pageConfiguration?.let { config ->
                    val enabledRows = config.rows.filter { it.enabled }
                    for (row in enabledRows) {
                        val dataSourceConfig = row.toDataSourceConfig()
                        if (dataSourceConfig != null && isDataSourceEmpty(dataSourceConfig)) {
                            Log.d("BaseConfigurableViewModel", "📥 Loading page 1 for empty data source: ${dataSourceConfig.endpoint}")
                            refreshDataSource(dataSourceConfig)
                        } else {
                            Log.d("BaseConfigurableViewModel", "✅ Data source ${dataSourceConfig?.endpoint} already has cached data")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BaseConfigurableViewModel", "❌ Error checking/refreshing data sources", e)
            } finally {
                initialLoadComplete = true
            }
        }
    }
    
    
    // Abstract methods for subclasses to implement
    abstract fun createPagingFlow(
        config: DataSourceConfig, 
        isRowFocused: () -> Boolean,
        getCurrentPosition: () -> Int,
        getTotalItems: () -> Int
    ): Flow<PagingData<T>>
    abstract suspend fun loadMultipleDataSources(dataSources: List<Pair<String, DataSourceConfig>>)
    abstract suspend fun refreshDataSource(config: DataSourceConfig)
    abstract suspend fun isDataSourceEmpty(config: DataSourceConfig): Boolean
}