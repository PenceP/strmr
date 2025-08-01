package com.strmr.ai.utils

import android.os.Build
import android.util.Log

object DeviceCapabilities {
    /**
     * Check if the device is an emulator
     */
    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.PRODUCT.contains("sdk_google") ||
            Build.PRODUCT.contains("google_sdk") ||
            Build.PRODUCT.contains("sdk") ||
            Build.PRODUCT.contains("sdk_x86") ||
            Build.PRODUCT.contains("vbox86p") ||
            Build.PRODUCT.contains("emulator") ||
            Build.PRODUCT.contains("simulator")
    }

    /**
     * Check if the device is a known high-end Android TV device
     */
    fun isHighEndAndroidTV(): Boolean {
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()

        return model.contains("shield") || // Nvidia Shield
            model.contains("chromecast") || // Chromecast with Google TV 4K
            device.contains("sabrina") || // Chromecast codename
            model.contains("mibox") || // Xiaomi Mi Box S
            model.contains("fire tv cube") || // Amazon Fire TV Cube
            model.contains("onn 4k") // Walmart Onn 4K
    }

    /**
     * Check if device likely supports advanced codecs
     */
    fun supportsAdvancedCodecs(): Boolean {
        // Emulators generally don't support advanced codecs well
        if (isEmulator()) {
            Log.d("DeviceCapabilities", "🖥️ Running on emulator - limited codec support")
            return false
        }

        // High-end Android TV devices support most codecs
        if (isHighEndAndroidTV()) {
            Log.d("DeviceCapabilities", "📺 High-end Android TV detected - full codec support")
            return true
        }

        // For other devices, check API level (newer = better codec support)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q // Android 10+
    }

    /**
     * Get device info for logging
     */
    fun getDeviceInfo(): String {
        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(" (${Build.DEVICE})")
            append(", API ${Build.VERSION.SDK_INT}")
            if (isEmulator()) append(" [EMULATOR]")
            if (isHighEndAndroidTV()) append(" [HIGH-END TV]")
        }
    }
}
