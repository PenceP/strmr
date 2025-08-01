package com.strmr.ai.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("prerelease") val prerelease: Boolean,
    @SerializedName("draft") val draft: Boolean,
    @SerializedName("assets") val assets: List<GitHubAsset>,
    @SerializedName("published_at") val publishedAt: String,
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("size") val size: Long,
    @SerializedName("content_type") val contentType: String,
)

data class UpdateInfo(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val assetSize: Long = 0L,
)

@Singleton
class UpdateRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val okHttpClient: OkHttpClient,
        private val gson: Gson,
    ) {
        companion object {
            private const val TAG = "UpdateRepository"
            private const val GITHUB_REPO_OWNER = "PenceP"
            private const val GITHUB_REPO_NAME = "strmr"
            private const val GITHUB_API_BASE = "https://api.github.com"
        }

        suspend fun checkForUpdates(): UpdateInfo =
            withContext(Dispatchers.IO) {
                try {
                    val currentVersion = getCurrentVersion()
                    Log.d(TAG, "Current version: $currentVersion")

                    val latestRelease = getLatestRelease()
                    val latestVersion = latestRelease.tagName.removePrefix("v")

                    Log.d(TAG, "Latest version: $latestVersion")

                    val hasUpdate = compareVersions(currentVersion, latestVersion) < 0

                    if (hasUpdate) {
                        val apkAsset =
                            latestRelease.assets.find {
                                it.name.endsWith(".apk") && it.contentType == "application/vnd.android.package-archive"
                            }

                        return@withContext UpdateInfo(
                            hasUpdate = true,
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                            downloadUrl = apkAsset?.downloadUrl,
                            releaseNotes = latestRelease.body,
                            assetSize = apkAsset?.size ?: 0L,
                        )
                    } else {
                        return@withContext UpdateInfo(
                            hasUpdate = false,
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking for updates", e)
                    return@withContext UpdateInfo(
                        hasUpdate = false,
                        currentVersion = getCurrentVersion(),
                        latestVersion = getCurrentVersion(),
                    )
                }
            }

        private suspend fun getLatestRelease(): GitHubRelease =
            withContext(Dispatchers.IO) {
                val request =
                    Request.Builder()
                        .url("$GITHUB_API_BASE/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest")
                        .addHeader("Accept", "application/vnd.github.v3+json")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch latest release: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                return@withContext gson.fromJson(responseBody, GitHubRelease::class.java)
            }

        private fun getCurrentVersion(): String {
            return try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "1.0"
            } catch (e: Exception) {
                Log.e(TAG, "Error getting current version", e)
                "1.0"
            }
        }

        private fun compareVersions(
            version1: String,
            version2: String,
        ): Int {
            val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
            val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLength = maxOf(parts1.size, parts2.size)

            for (i in 0 until maxLength) {
                val part1 = parts1.getOrElse(i) { 0 }
                val part2 = parts2.getOrElse(i) { 0 }

                when {
                    part1 < part2 -> return -1
                    part1 > part2 -> return 1
                }
            }

            return 0
        }
    }
