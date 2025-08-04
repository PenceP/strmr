package com.strmr.ai.ui.utils

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import com.strmr.ai.utils.safeCall
import kotlinx.coroutines.delay

// Global focus state that survives navigation changes
object GlobalFocusState {
    // Changed to store focus per row within each destination
    private val _lastFocusedItemPerDestinationAndRow =
        mutableMapOf<String, MutableMap<String, String>>()
    private val _focusTransferredOnLaunch = mutableStateOf(false)
    var lastRoute: String? = null

    val lastFocusedItemPerDestinationAndRow: MutableMap<String, MutableMap<String, String>> get() = _lastFocusedItemPerDestinationAndRow
    val focusTransferredOnLaunch: MutableState<Boolean> get() = _focusTransferredOnLaunch

    // Helper functions to manage row-specific focus
    fun getLastFocusedItemForRow(route: String, rowKey: String): String? {
        return _lastFocusedItemPerDestinationAndRow[route]?.get(rowKey)
    }

    fun setLastFocusedItemForRow(route: String, rowKey: String, itemKey: String) {
        if (_lastFocusedItemPerDestinationAndRow[route] == null) {
            _lastFocusedItemPerDestinationAndRow[route] = mutableMapOf()
        }
        _lastFocusedItemPerDestinationAndRow[route]!![rowKey] = itemKey
        Log.d("FocusDebug", "🗂️ Saved focus for route: $route, row: $rowKey, item: $itemKey")
        Log.d("FocusDebug", "📋 Full focus map: $_lastFocusedItemPerDestinationAndRow")
    }

    fun getLastFocusedItemForRoute(route: String): String? {
        // Return the most recently focused item in any row for this route
        return _lastFocusedItemPerDestinationAndRow[route]?.values?.lastOrNull()
    }

    fun hasAnyFocusForRoute(route: String): Boolean {
        return _lastFocusedItemPerDestinationAndRow[route]?.isNotEmpty() == true
    }

    // Backward compatibility helper functions for the old system
    fun getLastFocusedItemKey(route: String): String? {
        // Return the most recently focused item key for any row in this route
        return _lastFocusedItemPerDestinationAndRow[route]?.values?.lastOrNull()
    }

    fun setLastFocusedItemKey(route: String, itemKey: String) {
        // For backward compatibility, extract row key from item key or use default
        val rowKey = when {
            itemKey.contains("trending_row") -> "trending_row"
            itemKey.contains("popular_row") -> "popular_row"
            itemKey.contains("top_rated_row") -> "top_rated_row"
            else -> "default_row"
        }
        setLastFocusedItemForRow(route, rowKey, itemKey)
    }
}

private val LocalLastFocusedItemPerDestinationAndRow =
    compositionLocalOf<MutableMap<String, MutableMap<String, String>>> {
        GlobalFocusState.lastFocusedItemPerDestinationAndRow
    }
private val LocalFocusTransferredOnLaunch =
    compositionLocalOf<MutableState<Boolean>> {
        GlobalFocusState.focusTransferredOnLaunch
    }
private val LocalCurrentRoute =
    compositionLocalOf<String> {
        error("Please wrap your app with LocalCurrentRoute")
    }

@Composable
fun LocalLastFocusedItemPerDestinationAndRowProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalLastFocusedItemPerDestinationAndRow provides GlobalFocusState.lastFocusedItemPerDestinationAndRow,
        content = content
    )
}

@Composable
fun LocalFocusTransferredOnLaunchProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFocusTransferredOnLaunch provides GlobalFocusState.focusTransferredOnLaunch, content = content)
}

@Composable
fun LocalCurrentRouteProvider(
    currentRoute: String,
    content: @Composable () -> Unit,
) {
    val isInitialFocusTransferred = useLocalFocusTransferredOnLaunch()
    val lastFocusedItemPerDestinationAndRow = useLocalLastFocusedItemPerDestinationAndRow()

    // For focus restoration from details pages, we need to detect when we're coming back
    // The key insight: when returning from details, don't reset the focus transfer state
    LaunchedEffect(currentRoute) {
        // Log.d("FocusDebug", "🔄 Route changed to: $currentRoute")

        // Only reset focus transfer state for non-content routes that aren't focus restoration scenarios
        // If we're going TO a sub-page (details/intermediate), always reset (so sub-page starts fresh)
        // If we're going FROM a sub-page to a content page, DON'T reset (preserve focus memory)
        val isSubPage = currentRoute == "details" || currentRoute.startsWith("intermediate_view_")

        if (isSubPage) {
            // Log.d("FocusDebug", "🆕 Navigating to sub-page ($currentRoute) - resetting focus transfer state")
            // Log.d("FocusDebug", "🔓 Setting isInitialFocusTransferred=false for sub-page")
            isInitialFocusTransferred.value = false
        } else {
            // Check if we have saved focus for this route - if yes, we're likely returning from a sub-page
            val hasSavedFocus = GlobalFocusState.hasAnyFocusForRoute(currentRoute)
            Log.d(
                "FocusDebug",
                "📋 Checking route $currentRoute - hasSavedFocus: $hasSavedFocus, isInitialFocusTransferred: ${isInitialFocusTransferred.value}",
            )

            if (hasSavedFocus) {
                // We have saved focus for this route, so we're returning from a sub-page
                // Reset the focus transfer state so focus restoration can work
                Log.d(
                    "FocusDebug",
                    "🔙 Returning to $currentRoute with saved focus - resetting transfer state"
                )
                isInitialFocusTransferred.value = false
            } else if (!isInitialFocusTransferred.value) {
                // No saved focus and no transfer yet - this is initial load
                // Log.d("FocusDebug", "🆕 Initial load of $currentRoute - allowing focus transfer")
            } else {
                // No saved focus but transfer already happened - normal navigation
                // Log.d("FocusDebug", "➡️ Normal navigation to $currentRoute - preserving state")
            }
        }
    }

    CompositionLocalProvider(LocalCurrentRoute provides currentRoute, content = content)
}

// Helper to detect if we're navigating back vs navigating to a new screen
private fun isNavigatingBack(
    previousRoute: String?,
    currentRoute: String,
): Boolean {
    // If we went from details -> movies/home/etc, that's navigating back
    return previousRoute == "details" && currentRoute in listOf("movies", "home", "tvshows", "search")
}

@Composable
fun useLocalLastFocusedItemPerDestinationAndRow() = LocalLastFocusedItemPerDestinationAndRow.current

@Composable
fun useLocalCurrentRoute() = LocalCurrentRoute.current

@Composable
fun useLocalFocusTransferredOnLaunch() = LocalFocusTransferredOnLaunch.current

/**
 * [FocusRequesterModifiers] defines a set of modifiers which can be used for restoring focus and
 * specifying the initially focused item.
 *
 * @param [parentModifier] is added to the parent container.
 * @param [childModifier] is added to the item that needs to first gain focus.
 */
data class FocusRequesterModifiers(
    val parentModifier: Modifier,
    val childModifier: Modifier,
)

/**
 * Returns a set of modifiers [FocusRequesterModifiers] which can be used for restoring focus and
 * specifying the initially focused item.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun createInitialFocusRestorerModifiers(): FocusRequesterModifiers {
    val focusRequester = remember { FocusRequester() }
    val childFocusRequester = remember { FocusRequester() }

    val parentModifier =
        Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                exit = {
                    focusRequester.saveFocusedChild()
                    FocusRequester.Default
                }
                enter = {
                    // Safe call because this one's still bugged.
                    val isRestored =
                        safeCall {
                            focusRequester.restoreFocusedChild()
                        }

                    when (isRestored) {
                        true -> FocusRequester.Cancel
                        null -> FocusRequester.Default // Fail-safe if compose tv acts up
                        else -> childFocusRequester
                    }
                }
            }

    val childModifier = Modifier.focusRequester(childFocusRequester)

    return FocusRequesterModifiers(
        parentModifier = parentModifier,
        childModifier = childModifier,
    )
}

@Composable
fun Modifier.focusOnMount(
    itemKey: String,
    rowKey: String,
    onFocus: (() -> Unit)? = null,
): Modifier {
    val isInitialFocusTransferred = useLocalFocusTransferredOnLaunch()
    val lastFocusedItemPerDestinationAndRow = useLocalLastFocusedItemPerDestinationAndRow()
    val currentRoute = useLocalCurrentRoute()

    val focusRequester = remember { FocusRequester() }

    // Add LaunchedEffect for delayed focus restoration
    LaunchedEffect(currentRoute, itemKey) {
        val lastFocusedKey = GlobalFocusState.getLastFocusedItemForRow(currentRoute, rowKey)

        // Add detailed logging for debugging
        Log.d(
            "FocusDebug",
            "🔍 FocusHelper check - route: $currentRoute, row: $rowKey, itemKey: $itemKey, lastFocusedKey: $lastFocusedKey"
        )
        Log.d(
            "FocusDebug",
            "🗂️ Full focus map: ${GlobalFocusState.lastFocusedItemPerDestinationAndRow}"
        )
        Log.d(
            "FocusDebug",
            "🎯 isInitialFocusTransferred: ${isInitialFocusTransferred.value}"
        )

        // Route-aware focus restoration with delay for better reliability
        if (lastFocusedKey == itemKey && !isInitialFocusTransferred.value) {
            val isTargetRoute = currentRoute in listOf("home", "movies", "tvshows", "search")
            val isIntermediatePage = currentRoute.startsWith("intermediate_view_")

            if (isTargetRoute && !isIntermediatePage) {
                Log.d(
                    "FocusDebug",
                    "🎯 Delayed focus restoration for itemKey: $itemKey in route: $currentRoute, row: $rowKey"
                )

                // Add delay to ensure UI is fully rendered
                delay(150)

                try {
                    focusRequester.requestFocus()
                    isInitialFocusTransferred.value = true
                    Log.d("FocusHelper", "✅ Delayed focus request successful for itemKey: $itemKey")
                } catch (e: IllegalStateException) {
                    Log.w(
                        "FocusHelper",
                        "⚠️ Delayed focus request failed for itemKey: $itemKey - FocusRequester not initialized: ${e.message}"
                    )
                } catch (e: Exception) {
                    Log.e(
                        "FocusHelper",
                        "❌ Unexpected error during delayed focus request for itemKey: $itemKey",
                        e
                    )
                }
            } else {
                Log.d(
                    "FocusDebug",
                    "🚫 Skipping focus request - route: $currentRoute (isTarget=$isTargetRoute, isIntermediate=$isIntermediatePage)"
                )
            }
        } else {
            Log.d(
                "FocusDebug",
                "❌ No focus restoration - lastFocusedKey: $lastFocusedKey, itemKey: $itemKey, match: ${lastFocusedKey == itemKey}, transferred: ${isInitialFocusTransferred.value}"
            )
        }
    }

    return this
        .focusRequester(focusRequester)
        .onGloballyPositioned {
            val lastFocusedKey = GlobalFocusState.getLastFocusedItemForRow(currentRoute, rowKey)
            // Only log when there's a match to restore
            if (lastFocusedKey == itemKey && !isInitialFocusTransferred.value) {
                Log.d(
                    "FocusDebug",
                    "🎯 Match found - route: $currentRoute, row: $rowKey, itemKey: $itemKey, lastFocusedKey: $lastFocusedKey",
                )
            }
        }
        .onFocusChanged {
            if (it.isFocused) {
                Log.d(
                    "FocusDebug",
                    "📍 FOCUS GAINED by itemKey: $itemKey in route: $currentRoute, row: $rowKey"
                )
                onFocus?.invoke()
                GlobalFocusState.setLastFocusedItemForRow(currentRoute, rowKey, itemKey)
                Log.d(
                    "FocusDebug",
                    "🔒 Setting isInitialFocusTransferred=true from onFocusChanged for $itemKey"
                )
                isInitialFocusTransferred.value = true
            }
        }
}

/**
 * Composable that manages row-specific focus restoration.
 * This should be used at the row level to restore focus when a row becomes visible again.
 *
 * @param rowKey Unique identifier for this row (e.g., "trending", "popular", "top_rated")
 * @param isVisible Whether this row is currently visible to the user
 */
@Composable
fun RowFocusManager(
    rowKey: String,
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    val currentRoute = useLocalCurrentRoute()
    val isInitialFocusTransferred = useLocalFocusTransferredOnLaunch()

    // Track when this row becomes visible after being invisible
    LaunchedEffect(isVisible, rowKey, currentRoute) {
        if (isVisible) {
            val lastFocusedInRow = GlobalFocusState.getLastFocusedItemForRow(currentRoute, rowKey)

            Log.d(
                "FocusDebug",
                "👁️ Row '$rowKey' became visible in route '$currentRoute' - lastFocusedInRow: $lastFocusedInRow"
            )

            if (lastFocusedInRow != null && !isInitialFocusTransferred.value) {
                Log.d(
                    "FocusDebug",
                    "🔄 Row '$rowKey' has saved focus item '$lastFocusedInRow' - will attempt restoration"
                )
            } else {
                Log.d(
                    "FocusDebug",
                    "ℹ️ Row '$rowKey' - no focus to restore (lastFocused: $lastFocusedInRow, transferred: ${isInitialFocusTransferred.value})"
                )
            }
        } else {
            Log.d("FocusDebug", "👁️‍🗨️ Row '$rowKey' is no longer visible")
        }
    }

    content()
}

/**
 * [FocusRequesterModifiers] defines a set of modifiers which can be used for restoring focus and
 * specifying the initially focused item.
 *
 * @param [parentModifier] is added to the parent container.
 * @param [childModifier] is added to the item that needs to first gain focus.
 */

@Composable
fun LocalLastFocusedItemPerDestinationProvider(content: @Composable () -> Unit) {
    LocalLastFocusedItemPerDestinationAndRowProvider(content)
}
