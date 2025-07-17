package com.strmr.ai

import android.app.Application
import android.content.Context
import android.util.Log
import com.strmr.ai.data.database.StrmrDatabase
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.strmr.ai.BackgroundSyncWorker
import dagger.hilt.android.HiltAndroidApp
import androidx.hilt.work.HiltWorkerFactory
import javax.inject.Inject

@HiltAndroidApp
class StrmrApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory
    
    val database: StrmrDatabase by lazy {
        StrmrDatabase.getDatabase(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("FontLoading", "🏁 StrmrApplication onCreate started")
        
        // Verify font resources are accessible
        verifyFontResources()
        
        // Schedule background sync once per day
        val workRequest = PeriodicWorkRequestBuilder<BackgroundSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "strmr_background_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    private fun verifyFontResources() {
        try {
            // Check if font files exist in resources
            val regularFontId = com.strmr.ai.R.font.figtree_variablefont_wght
            val italicFontId = com.strmr.ai.R.font.figtree_italic_variablefont_wght
            
            Log.d("FontLoading", "📁 Font resource IDs - Regular: $regularFontId, Italic: $italicFontId")
            
            // Try to get the font resource names
            val regularFontName = resources.getResourceEntryName(regularFontId)
            val italicFontName = resources.getResourceEntryName(italicFontId)
            
            Log.d("FontLoading", "✅ Font resources found - Regular: $regularFontName, Italic: $italicFontName")
            
        } catch (e: Exception) {
            Log.e("FontLoading", "❌ Error verifying font resources: ${e.message}")
            e.printStackTrace()
        }
    }
} 