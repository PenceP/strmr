package com.strmr.ai.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.strmr.ai.UpdateDownloadWorker
import com.strmr.ai.data.UpdateInfo
import com.strmr.ai.data.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class UpdateUiState(
    val isLoading: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val error: String? = null,
    val downloadProgress: Int = 0,
    val downloadStatus: String? = null,
    val isDownloading: Boolean = false
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var currentDownloadWorkId: String? = null

    init {
        checkForUpdatesOnStartup()
    }

    private fun checkForUpdatesOnStartup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val updateInfo = updateRepository.checkForUpdates()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    updateInfo = updateInfo,
                    error = null
                )
                
                if (updateInfo.hasUpdate) {
                    Log.d(TAG, "Update available: ${updateInfo.latestVersion}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates on startup", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val updateInfo = updateRepository.checkForUpdates()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    updateInfo = updateInfo
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val updateInfo = _uiState.value.updateInfo
        if (updateInfo?.hasUpdate != true || updateInfo.downloadUrl == null) {
            return
        }

        val workId = UpdateDownloadWorker.enqueue(
            context = context,
            downloadUrl = updateInfo.downloadUrl,
            version = updateInfo.latestVersion
        )
        
        currentDownloadWorkId = workId
        _uiState.value = _uiState.value.copy(isDownloading = true)
        
        observeDownloadProgress(UUID.fromString(workId))
    }

    private fun observeDownloadProgress(workId: UUID) {
        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfoByIdLiveData(workId)
                .observeForever { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getInt("progress", 0)
                            val status = workInfo.progress.getString("status")
                            _uiState.value = _uiState.value.copy(
                                downloadProgress = progress,
                                downloadStatus = status,
                                isDownloading = true
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                downloadProgress = 100,
                                downloadStatus = "Installation started"
                            )
                            Log.d(TAG, "Update download completed successfully")
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error") ?: "Download failed"
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                error = error,
                                downloadStatus = null
                            )
                            Log.e(TAG, "Update download failed: $error")
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uiState.value = _uiState.value.copy(
                                isDownloading = false,
                                downloadStatus = null
                            )
                        }
                        else -> {
                            // ENQUEUED, BLOCKED - no action needed
                        }
                    }
                }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun cancelDownload() {
        currentDownloadWorkId?.let { workId ->
            WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workId))
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                downloadProgress = 0,
                downloadStatus = null
            )
        }
    }
}