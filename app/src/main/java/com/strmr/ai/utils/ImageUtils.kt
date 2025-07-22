package com.strmr.ai.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Utility functions for handling image URLs and drawable resources
 */
object ImageUtils {
    
    /**
     * Convert a URL string that may contain "drawable://" scheme to appropriate resource
     * @param url The URL or drawable resource identifier
     * @param context Android context for resource access
     * @return Resource ID if drawable:// scheme, otherwise returns the original URL
     */
    fun resolveImageSource(url: String?, context: Context): Any? {
        return when {
            url == null -> null
            url.startsWith("drawable://") -> {
                val resourceName = url.removePrefix("drawable://")
                val resourceId = context.resources.getIdentifier(
                    resourceName, 
                    "drawable", 
                    context.packageName
                )
                if (resourceId != 0) resourceId else null
            }
            else -> url
        }
    }
}

/**
 * Composable helper to resolve image source for AsyncImage
 */
@Composable
fun resolveImageSource(url: String?): Any? {
    val context = LocalContext.current
    return ImageUtils.resolveImageSource(url, context)
}