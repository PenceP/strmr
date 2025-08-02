package com.strmr.ai.ui.utils

import androidx.compose.runtime.Composable

/**
 * Convenience wrapper that provides all focus-related composition locals
 * Use this to wrap any screen that uses UnifiedMediaRow or focus-related components
 */
@Composable
fun WithFocusProviders(
    route: String,
    content: @Composable () -> Unit,
) {
    LocalLastFocusedItemPerDestinationProvider {
        LocalFocusTransferredOnLaunchProvider {
            LocalCurrentRouteProvider(route) {
                content()
            }
        }
    }
}
