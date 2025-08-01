package com.strmr.ai.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and caches remote resources (images) from GitHub repository
 * Replaces local drawable resources to reduce APK size by ~75MB
 */
@Singleton
class RemoteResourceLoader
    @Inject
    constructor(
        private val context: Context,
    ) {
        companion object {
            private const val TAG = "RemoteResourceLoader"
            private const val GITHUB_RAW_BASE_URL = "https://raw.githubusercontent.com/PenceP/strmr/refs/heads/main/app/src/main/res/drawable/"
            private const val CACHE_DIR_NAME = "remote_resources"
            private const val CACHE_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        }

        private val cacheDir: File by lazy {
            File(context.cacheDir, CACHE_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
        }

        /**
         * Resolve image source with fallback chain:
         * 1. Try local cache
         * 2. Download from GitHub and cache
         * 3. Fallback to local drawable (if exists)
         * 4. Return null
         */
        suspend fun resolveImageSource(url: String?): Any? =
            withContext(Dispatchers.IO) {
                when {
                    url == null -> null
                    url.startsWith("drawable://") -> {
                        val resourceName = url.removePrefix("drawable://")
                        resolveDrawableResource(resourceName)
                    }
                    else -> url // Regular URL, return as-is
                }
            }

        private suspend fun resolveDrawableResource(resourceName: String): Any? {
            try {
                Log.d(TAG, "🔍 Resolving drawable resource: $resourceName")

                // 1. Check local cache first
                val cachedFile = getCachedFile(resourceName)
                if (cachedFile.exists() && isCacheValid(cachedFile)) {
                    Log.d(TAG, "✅ Using cached resource: $resourceName")
                    return cachedFile.absolutePath
                }

                // 2. Try to download from GitHub
                val remoteUrl = "$GITHUB_RAW_BASE_URL$resourceName.png"
                Log.d(TAG, "📡 Downloading from GitHub: $remoteUrl")

                if (downloadAndCache(remoteUrl, cachedFile)) {
                    Log.d(TAG, "✅ Downloaded and cached: $resourceName")
                    return cachedFile.absolutePath
                }

                // 3. Fallback to local drawable (if exists)
                val resourceId =
                    context.resources.getIdentifier(
                        resourceName,
                        "drawable",
                        context.packageName,
                    )
                if (resourceId != 0) {
                    Log.d(TAG, "⚠️ Using local fallback for: $resourceName")
                    return resourceId
                }

                // 4. Resource not found anywhere
                Log.w(TAG, "❌ Resource not found: $resourceName")
                return null
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error resolving resource: $resourceName", e)

                // Try local fallback on error
                val resourceId =
                    context.resources.getIdentifier(
                        resourceName,
                        "drawable",
                        context.packageName,
                    )
                return if (resourceId != 0) resourceId else null
            }
        }

        private suspend fun downloadAndCache(
            url: String,
            targetFile: File,
        ): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(url).openConnection()
                    connection.connectTimeout = 10000
                    connection.readTimeout = 30000

                    connection.inputStream.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    return@withContext true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to download: $url", e)
                    // Clean up partial file
                    if (targetFile.exists()) targetFile.delete()
                    return@withContext false
                }
            }

        private fun getCachedFile(resourceName: String): File {
            return File(cacheDir, "$resourceName.png")
        }

        private fun isCacheValid(file: File): Boolean {
            val ageMs = System.currentTimeMillis() - file.lastModified()
            return ageMs < CACHE_EXPIRY_MS
        }

        /**
         * Clear all cached resources (useful for testing or cache management)
         */
        fun clearCache() {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                        Log.d(TAG, "🗑️ Deleted cached file: ${file.name}")
                    }
                }
                Log.d(TAG, "✅ Cache cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing cache", e)
            }
        }

        /**
         * Get cache statistics for debugging
         */
        fun getCacheStats(): CacheStats {
            val files = cacheDir.listFiles()?.filter { it.isFile } ?: emptyList()
            val totalSize = files.sumOf { it.length() }

            return CacheStats(
                fileCount = files.size,
                totalSizeBytes = totalSize,
                totalSizeMB = totalSize / (1024 * 1024).toDouble(),
            )
        }

        data class CacheStats(
            val fileCount: Int,
            val totalSizeBytes: Long,
            val totalSizeMB: Double,
        )
    }
