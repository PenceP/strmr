package com.strmr.ai.ui.screens

data class UiState<T>(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val mediaRows: Map<String, List<T>> = emptyMap()
) 