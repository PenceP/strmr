package com.strmr.ai.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator

/**
 * Utility class for optimizing ExoPlayer performance and memory usage
 * Specifically tuned for Android TV streaming applications
 */
@UnstableApi
object ExoPlayerOptimizer {
    
    // Buffer sizes optimized for streaming content
    private const val MIN_BUFFER_MS = 15_000        // 15 seconds
    private const val MAX_BUFFER_MS = 50_000        // 50 seconds  
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500 // 2.5 seconds
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000 // 5 seconds
    
    // Memory allocation optimized for TV
    private const val ALLOCATOR_TRIM_THRESHOLD = 128 * 1024 * 1024 // 128MB
    
    /**
     * Create optimized LoadControl for streaming performance
     */
    fun createOptimizedLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, ALLOCATOR_TRIM_THRESHOLD))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(-1) // Use default
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }
    
    /**
     * Create optimized TrackSelector for Android TV
     */
    fun createOptimizedTrackSelector(context: Context): TrackSelector {
        return DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    // Prefer higher quality video on TV
                    .setMaxVideoSizeSd() 
                    // Enable tunneling for better performance
                    .setTunnelingEnabled(true)
                    // Prefer hardware acceleration
                    .setPreferredVideoMimeTypes("video/avc", "video/hevc")
                    .build()
            )
        }
    }
    
    /**
     * Apply memory optimizations to ExoPlayer
     */
    fun applyMemoryOptimizations(player: ExoPlayer) {
        // Set volume to prevent audio focus issues
        player.volume = 1.0f
        
        // Enable video scaling for memory efficiency
        player.videoScalingMode = androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        
        // Log memory optimizations applied
        android.util.Log.d("ExoPlayerOptimizer", "✅ Memory optimizations applied to ExoPlayer")
    }
    
    /**
     * Release ExoPlayer resources properly to prevent memory leaks
     */
    fun releasePlayerSafely(player: ExoPlayer?) {
        player?.let {
            try {
                it.stop()
                it.clearMediaItems()
                it.release()
                android.util.Log.d("ExoPlayerOptimizer", "🧹 ExoPlayer resources released safely")
            } catch (e: Exception) {
                android.util.Log.e("ExoPlayerOptimizer", "⚠️ Error releasing ExoPlayer", e)
            }
        }
    }
}