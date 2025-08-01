package com.strmr.ai.ui.screens

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

data class PagingUiState<T : Any>(
    val isLoading: Boolean = false,
    val mediaRows: Map<String, Flow<PagingData<T>>> = emptyMap(),
    val error: String? = null,
)
