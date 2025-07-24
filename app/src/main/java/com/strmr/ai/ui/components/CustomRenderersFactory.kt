package com.strmr.ai.ui.components

import android.content.Context
import android.os.Handler
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.strmr.ai.utils.DeviceCapabilities
import android.util.Log

/**
 * Custom renderer factory optimized for Android TV
 * Following CloudStream's approach to prevent crashes and optimize performance
 * 
 * Single Responsibility: Creates optimized renderers for different device types
 */
@UnstableApi
class CustomRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    
    companion object {
        private const val TAG = "CustomRenderersFactory"
    }
    
    private val isAndroidTV = DeviceCapabilities.isHighEndAndroidTV()
    private val isEmulator = DeviceCapabilities.isEmulator()
    
    init {
        Log.d(TAG, "🏭 Creating custom renderer factory - TV: $isAndroidTV, Emulator: $isEmulator")
        
        // Android TV optimizations following CloudStream pattern
        if (isAndroidTV) {
            // Disable decoder fallback on TV to prevent crashes
            setEnableDecoderFallback(false)
            Log.d(TAG, "📺 Android TV: Disabled decoder fallback")
        }
        
        if (isEmulator) {
            // Enable decoder fallback for emulators
            setEnableDecoderFallback(true)
            Log.d(TAG, "🖥️ Emulator: Enabled decoder fallback")
        }
    }
    
    override fun buildVideoRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        eventHandler: Handler,
        eventListener: VideoRendererEventListener,
        allowedVideoJoiningTimeMs: Long,
        out: ArrayList<Renderer>
    ) {
        val actualEnableDecoderFallback = if (isAndroidTV) {
            // Force disable decoder fallback on Android TV to prevent MediaCodec crashes
            false
        } else {
            enableDecoderFallback
        }
        
        Log.d(TAG, "🎥 Building video renderers - fallback enabled: $actualEnableDecoderFallback")
        
        super.buildVideoRenderers(
            context,
            extensionRendererMode,
            mediaCodecSelector,
            actualEnableDecoderFallback,
            eventHandler,
            eventListener,
            allowedVideoJoiningTimeMs,
            out
        )
    }
    
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        // Optimize audio sink for TV devices
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .build()
            .also {
                Log.d(TAG, "🔊 Built optimized audio sink")
            }
    }
}