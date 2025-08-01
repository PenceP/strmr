package com.strmr.ai.utils

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility functions for handling image URLs and drawable resources
 * Now supports remote loading from GitHub to reduce APK size
 */
@Singleton
class ImageUtils
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val remoteResourceLoader: RemoteResourceLoader,
    ) {
        /**
         * Legacy method for backwards compatibility
         * @deprecated Use RemoteResourceLoader.resolveImageSource() instead
         */
        @Deprecated("Use RemoteResourceLoader for remote loading capability")
        fun resolveImageSource(
            url: String?,
            context: Context,
        ): Any? {
            return when {
                url == null -> null
                url.startsWith("drawable://") -> {
                    val resourceName = url.removePrefix("drawable://")
                    val resourceId =
                        context.resources.getIdentifier(
                            resourceName,
                            "drawable",
                            context.packageName,
                        )
                    if (resourceId != 0) resourceId else null
                }
                else -> url
            }
        }

        /**
         * Resolve image source with remote loading capability
         */
        suspend fun resolveImageSourceRemote(url: String?): Any? {
            return remoteResourceLoader.resolveImageSource(url)
        }
    }

/**
 * Composable helper to resolve image source with remote loading
 * Downloads from GitHub and caches locally to reduce APK size by ~75MB
 */
@Composable
fun resolveImageSource(url: String?): Any? {
    val context = LocalContext.current
    var resolvedSource by remember(url) { mutableStateOf<Any?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // For remote resources, we need to handle async loading
    LaunchedEffect(url) {
        if (url?.startsWith("drawable://") == true) {
            // Use the new remote loading system
            try {
                // Get RemoteResourceLoader through DI or create instance
                val remoteLoader = RemoteResourceLoader(context)
                resolvedSource = remoteLoader.resolveImageSource(url)
            } catch (e: Exception) {
                // Fallback to legacy method
                resolvedSource = legacyResolveImageSource(url, context)
            }
        } else {
            resolvedSource = url
        }
    }

    return resolvedSource
}

/**
 * Legacy image resolution for fallback
 */
private fun legacyResolveImageSource(
    url: String?,
    context: Context,
): Any? {
    return when {
        url == null -> null
        url.startsWith("drawable://") -> {
            val resourceName = url.removePrefix("drawable://")
            val resourceId =
                context.resources.getIdentifier(
                    resourceName,
                    "drawable",
                    context.packageName,
                )
            if (resourceId != 0) resourceId else null
        }
        else -> url
    }
}
