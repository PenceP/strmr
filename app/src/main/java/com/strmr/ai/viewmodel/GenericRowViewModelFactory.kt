package com.strmr.ai.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.strmr.ai.config.GenericRowConfiguration
import com.strmr.ai.data.GenericTraktRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating GenericRowViewModel instances with specific configurations
 * Uses a simpler approach compatible with Hilt
 */
class GenericRowViewModelFactory @Inject constructor(
    private val genericRepository: GenericTraktRepository
) {
    
    /**
     * Create a GenericRowViewModel with the specified configuration
     */
    fun create(configuration: GenericRowConfiguration): GenericRowViewModel {
        return GenericRowViewModel(genericRepository, configuration)
    }
}

/**
 * Store to manage GenericRowViewModel instances by configuration ID
 * This allows us to maintain ViewModel lifecycle properly
 */
@Singleton
class GenericRowViewModelStore @Inject constructor(
    private val factory: GenericRowViewModelFactory
) {
    private val viewModels = mutableMapOf<String, GenericRowViewModel>()
    
    fun getOrCreate(configuration: GenericRowConfiguration): GenericRowViewModel {
        return viewModels.getOrPut(configuration.id) {
            factory.create(configuration)
        }
    }
    
    fun clear(configId: String) {
        viewModels.remove(configId)
    }
    
    fun clearAll() {
        viewModels.clear()
    }
}

/**
 * Helper function for easy ViewModel creation in Composables
 */
@Composable
fun rememberGenericRowViewModel(
    configuration: GenericRowConfiguration
): GenericRowViewModel {
    val factory: GenericRowViewModelFactory = hiltViewModel()
    return remember(configuration.id) {
        factory.create(configuration)
    }
}