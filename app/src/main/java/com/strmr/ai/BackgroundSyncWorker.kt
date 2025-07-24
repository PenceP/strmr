package com.strmr.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TvShowRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class BackgroundSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // TODO: Update to use GenericTraktRepository for background sync
            // For now, just return success to prevent build errors
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 