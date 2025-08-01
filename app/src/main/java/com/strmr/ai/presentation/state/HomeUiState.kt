package com.strmr.ai.presentation.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.strmr.ai.domain.repository.ContinueWatchingItem
import com.strmr.ai.viewmodel.HomeMediaItem

/**
 * Unified UI state for the Home screen
 * This replaces the scattered StateFlow properties in HomeViewModel
 */
@Stable
data class HomeUiState(
    val continueWatching: ContinueWatchingState = ContinueWatchingState.Loading,
    val networks: NetworksState = NetworksState.Loading,
    val traktLists: TraktListsState = TraktListsState.NotLoaded,
    val traktAuthorization: TraktAuthState = TraktAuthState.Unknown,
) {
    /**
     * Check if any section is currently loading
     */
    val isAnyLoading: Boolean
        get() =
            continueWatching is ContinueWatchingState.Loading ||
                networks is NetworksState.Loading ||
                traktLists is TraktListsState.Loading

    /**
     * Get all error messages for display
     */
    val errorMessages: List<String>
        get() =
            listOfNotNull(
                (continueWatching as? ContinueWatchingState.Error)?.message,
                (networks as? NetworksState.Error)?.message,
                (traktLists as? TraktListsState.Error)?.message,
            )
}

/**
 * State for continue watching section
 */
@Stable
sealed class ContinueWatchingState {
    @Immutable
    object Loading : ContinueWatchingState()

    @Immutable
    data class Success(val items: List<ContinueWatchingItem>) : ContinueWatchingState()

    @Immutable
    data class Error(val message: String) : ContinueWatchingState()
}

/**
 * State for networks section
 */
@Stable
sealed class NetworksState {
    @Immutable
    object Loading : NetworksState()

    @Immutable
    data class Success(val networks: List<NetworkInfoState>) : NetworksState()

    @Immutable
    data class Error(val message: String) : NetworksState()
}

/**
 * State for Trakt lists section
 */
@Stable
sealed class TraktListsState {
    @Immutable
    object NotLoaded : TraktListsState()

    @Immutable
    object Loading : TraktListsState()

    @Immutable
    data class Success(val lists: List<HomeMediaItem.Collection>) : TraktListsState()

    @Immutable
    data class Error(val message: String) : TraktListsState()
}

/**
 * State for Trakt authorization
 */
@Stable
sealed class TraktAuthState {
    @Immutable
    object Unknown : TraktAuthState()

    @Immutable
    object Authorized : TraktAuthState()

    @Immutable
    object NotAuthorized : TraktAuthState()

    @Immutable
    object Checking : TraktAuthState()
}

/**
 * Network info for the networks section
 * TODO: Move this to domain model when needed
 */
@Immutable
data class NetworkInfoState(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
)
