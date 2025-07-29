package com.strmr.ai.presentation.state

import com.strmr.ai.domain.repository.ContinueWatchingItem
import com.strmr.ai.viewmodel.HomeMediaItem

/**
 * Unified UI state for the Home screen
 * This replaces the scattered StateFlow properties in HomeViewModel
 */
data class HomeUiState(
    val continueWatching: ContinueWatchingState = ContinueWatchingState.Loading,
    val networks: NetworksState = NetworksState.Loading,
    val traktLists: TraktListsState = TraktListsState.NotLoaded,
    val traktAuthorization: TraktAuthState = TraktAuthState.Unknown
) {
    /**
     * Check if any section is currently loading
     */
    val isAnyLoading: Boolean
        get() = continueWatching is ContinueWatchingState.Loading ||
                networks is NetworksState.Loading ||
                traktLists is TraktListsState.Loading

    /**
     * Get all error messages for display
     */
    val errorMessages: List<String>
        get() = listOfNotNull(
            (continueWatching as? ContinueWatchingState.Error)?.message,
            (networks as? NetworksState.Error)?.message,
            (traktLists as? TraktListsState.Error)?.message
        )
}

/**
 * State for continue watching section
 */
sealed class ContinueWatchingState {
    object Loading : ContinueWatchingState()
    data class Success(val items: List<ContinueWatchingItem>) : ContinueWatchingState()
    data class Error(val message: String) : ContinueWatchingState()
}

/**
 * State for networks section
 */
sealed class NetworksState {
    object Loading : NetworksState()
    data class Success(val networks: List<NetworkInfoState>) : NetworksState()
    data class Error(val message: String) : NetworksState()
}

/**
 * State for Trakt lists section
 */
sealed class TraktListsState {
    object NotLoaded : TraktListsState()
    object Loading : TraktListsState()
    data class Success(val lists: List<HomeMediaItem.Collection>) : TraktListsState()
    data class Error(val message: String) : TraktListsState()
}

/**
 * State for Trakt authorization
 */
sealed class TraktAuthState {
    object Unknown : TraktAuthState()
    object Authorized : TraktAuthState()
    object NotAuthorized : TraktAuthState()
    object Checking : TraktAuthState()
}

/**
 * Network info for the networks section
 * TODO: Move this to domain model when needed
 */
data class NetworkInfoState(
    val id: String,
    val name: String,
    val logoUrl: String? = null
)