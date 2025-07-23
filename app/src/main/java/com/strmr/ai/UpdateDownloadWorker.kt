package com.strmr.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.strmr.ai.data.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@HiltWorker
class UpdateDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val updateRepository: UpdateRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "UpdateDownloadWorker"
        const val KEY_DOWNLOAD_URL = "download_url"
        const val KEY_VERSION = "version"
        
        fun enqueue(context: Context, downloadUrl: String, version: String): String {
            val workRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_DOWNLOAD_URL, downloadUrl)
                        .putString(KEY_VERSION, version)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            return workRequest.id.toString()
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val downloadUrl = inputData.getString(KEY_DOWNLOAD_URL)
                ?: return@withContext Result.failure()
            val version = inputData.getString(KEY_VERSION)
                ?: return@withContext Result.failure()

            Log.d(TAG, "Starting download for version $version from $downloadUrl")

            setProgress(
                Data.Builder()
                    .putString("status", "downloading")
                    .putInt("progress", 0)
                    .build()
            )

            val apkFile = downloadApk(downloadUrl, version)
            
            setProgress(
                Data.Builder()
                    .putString("status", "installing")
                    .putInt("progress", 100)
                    .build()
            )

            installApk(apkFile)

            Log.d(TAG, "Update download and installation initiated successfully")
            return@withContext Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading update", e)
            return@withContext Result.failure(
                Data.Builder()
                    .putString("error", e.message ?: "Unknown error")
                    .build()
            )
        }
    }

    private suspend fun downloadApk(downloadUrl: String, version: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to download APK: ${response.code}")
        }

        val cacheDir = File(applicationContext.cacheDir, "updates")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val apkFile = File(cacheDir, "strmr-$version.apk")
        
        response.body?.byteStream()?.use { inputStream ->
            FileOutputStream(apkFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                val totalSize = response.body?.contentLength() ?: 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    if (totalSize > 0) {
                        val progress = ((totalBytesRead * 100) / totalSize).toInt()
                        setProgress(
                            Data.Builder()
                                .putString("status", "downloading")
                                .putInt("progress", progress)
                                .build()
                        )
                    }
                }
            }
        }

        return@withContext apkFile
    }

    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val apkUri = FileProvider.getUriForFile(
                        applicationContext,
                        "${applicationContext.packageName}.fileprovider",
                        apkFile
                    )
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            applicationContext.startActivity(intent)
            Log.d(TAG, "APK installation intent started")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting APK installation", e)
            throw e
        }
    }
}