package com.strmr.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.strmr.ai.data.MovieRepository
import com.strmr.ai.data.TvShowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackgroundSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            movieRepository.refreshTrendingMovies()
            movieRepository.refreshPopularMovies()
            tvShowRepository.refreshTrendingTvShows()
            tvShowRepository.refreshPopularTvShows()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 