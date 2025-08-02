package com.strmr.ai.ui.utils

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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

// Global focus state that survives navigation changes
object GlobalFocusState {
    private val _lastFocusedItemPerDestination = mutableMapOf<String, String>()
    private val _focusTransferredOnLaunch = mutableStateOf(false)
    var lastRoute: String? = null

    val lastFocusedItemPerDestination: MutableMap<String, String> get() = _lastFocusedItemPerDestination
    val focusTransferredOnLaunch: MutableState<Boolean> get() = _focusTransferredOnLaunch
}

private val LocalLastFocusedItemPerDestination =
    compositionLocalOf<MutableMap<String, String>> {
        GlobalFocusState.lastFocusedItemPerDestination
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
fun LocalLastFocusedItemPerDestinationProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLastFocusedItemPerDestination provides GlobalFocusState.lastFocusedItemPerDestination, content = content)
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
    val lastFocusedItemPerDestination = useLocalLastFocusedItemPerDestination()

    // For focus restoration from details pages, we need to detect when we're coming back
    // The key insight: when returning from details, don't reset the focus transfer state
    remember(currentRoute) {
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
            val hasSavedFocus = lastFocusedItemPerDestination[currentRoute] != null
            Log.d(
                "FocusDebug",
                "📋 Checking route $currentRoute - hasSavedFocus: $hasSavedFocus, isInitialFocusTransferred: ${isInitialFocusTransferred.value}",
            )

            if (hasSavedFocus) {
                // We have saved focus for this route, so we're returning from a sub-page
                // Reset the focus transfer state so focus restoration can work
                // Log.d("FocusDebug", "🔙 Returning to $currentRoute with saved focus - enabling restoration")
                // Log.d("FocusDebug", "🔓 Setting isInitialFocusTransferred=false for restoration")
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
fun useLocalLastFocusedItemPerDestination() = LocalLastFocusedItemPerDestination.current

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
    onFocus: (() -> Unit)? = null,
): Modifier {
    val isInitialFocusTransferred = useLocalFocusTransferredOnLaunch()
    val lastFocusedItemPerDestination = useLocalLastFocusedItemPerDestination()
    val currentRoute = useLocalCurrentRoute()

    val focusRequester = remember { FocusRequester() }

    return this
        .focusRequester(focusRequester)
        .onGloballyPositioned {
            val lastFocusedKey = lastFocusedItemPerDestination[currentRoute]
            //Log.d(
            //    "FocusDebug",
            //    "🎯 onGloballyPositioned - route: $currentRoute, itemKey: $itemKey, lastFocusedKey: $lastFocusedKey, isInitialFocusTransferred: ${isInitialFocusTransferred.value}",
            //)

            // Route-aware focus restoration: only restore focus if this item is for the target route
            if (lastFocusedKey == itemKey && !isInitialFocusTransferred.value) {
                // Only restore focus for the destination route (home, movies, etc), not intermediate pages
                val isTargetRoute = currentRoute in listOf("home", "movies", "tvshows", "search")
                val isIntermediatePage = currentRoute.startsWith("intermediate_view_")

                if (isTargetRoute && !isIntermediatePage) {
                    //Log.d("FocusDebug", "🔥 REQUESTING FOCUS for itemKey: $itemKey in route: $currentRoute")
                    try {
                        focusRequester.requestFocus()
                        isInitialFocusTransferred.value = true
                        //Log.d("FocusHelper", "✅ Focus request successful for itemKey: $itemKey")
                    } catch (e: IllegalStateException) {
                        Log.w("FocusHelper", "⚠️ Focus request failed for itemKey: $itemKey - FocusRequester not initialized: ${e.message}")
                        // Don't set isInitialFocusTransferred to true if focus failed
                    } catch (e: Exception) {
                        Log.e("FocusHelper", "❌ Unexpected error during focus request for itemKey: $itemKey", e)
                    }
                } else {
                    //Log.d(
                    //    "FocusDebug",
                    //    "🚫 Skipping focus request - not target route: $currentRoute (isTarget=$isTargetRoute, isIntermediate=$isIntermediatePage)",
                    //)
                }
            }
        }
        .onFocusChanged {
            if (it.isFocused) {
                Log.d("FocusDebug", "📍 FOCUS GAINED by itemKey: $itemKey in route: $currentRoute")
                onFocus?.invoke()
                lastFocusedItemPerDestination[currentRoute] = itemKey
                Log.d("FocusDebug", "🔒 Setting isInitialFocusTransferred=true from onFocusChanged for $itemKey")
                isInitialFocusTransferred.value = true
            }
        }
}
